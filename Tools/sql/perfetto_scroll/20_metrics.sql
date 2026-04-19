DROP VIEW IF EXISTS hpfc_metric_row;
CREATE PERFETTO VIEW hpfc_metric_row AS
WITH base AS (
  SELECT
    CAST(name AS INTEGER) AS frame_id,
    CASE
      WHEN display_frame_token IS NOT NULL THEN 'dft:' || CAST(display_frame_token AS TEXT) || ':ts:' || CAST(ts AS TEXT)
      WHEN surface_frame_token IS NOT NULL THEN 'sft:' || CAST(surface_frame_token AS TEXT) || ':ts:' || CAST(ts AS TEXT)
      WHEN CAST(name AS INTEGER) >= 0 THEN 'id:' || CAST(name AS TEXT) || ':ts:' || CAST(ts AS TEXT)
      ELSE 'row:' || CAST(id AS TEXT)
    END AS frame_key,
    ts,
    dur,
    layer_name,
    present_type,
    jank_type,
    (present_type = 'Dropped Frame' OR jank_type GLOB '*Dropped Frame*') AS is_dropped,
    android_is_app_jank_type(jank_type) AS is_app_jank,
    android_is_sf_jank_type(jank_type) AS is_sf_jank,
    android_is_missed_frame_type(jank_type) AS is_missed
  FROM __CONSUMER_TABLE__
),
frame_level AS (
  SELECT
    MIN(frame_id) AS frame_id,
    frame_key,
    MIN(ts) AS ts,
    MAX(dur) AS dur_ns,
    MAX(is_dropped) AS is_dropped,
    MAX(is_app_jank) AS is_app_jank,
    MAX(is_sf_jank) AS is_sf_jank,
    MAX(is_missed) AS is_missed
  FROM base
  GROUP BY frame_key
),
raw_counts AS (
  SELECT
    COUNT(*) AS consumer_rows,
    COALESCE(SUM(CASE WHEN frame_id < 0 THEN 1 ELSE 0 END), 0) AS non_numeric_or_interpolated_rows
  FROM base
),
counts AS (
  SELECT
    COUNT(*) AS total_frames,
    COALESCE(SUM(CASE WHEN frame_id < 0 THEN 1 ELSE 0 END), 0) AS non_numeric_or_interpolated_frames,
    COALESCE(SUM(CASE WHEN is_dropped THEN 1 ELSE 0 END), 0) AS dropped_frames,
    COALESCE(SUM(CASE WHEN NOT is_dropped THEN 1 ELSE 0 END), 0) AS presented_frames,
    COALESCE(SUM(CASE WHEN is_missed THEN 1 ELSE 0 END), 0) AS missed_frames,
    COALESCE(SUM(CASE WHEN is_app_jank THEN 1 ELSE 0 END), 0) AS app_jank_frames,
    COALESCE(SUM(CASE WHEN is_sf_jank THEN 1 ELSE 0 END), 0) AS sf_jank_frames,
    MIN(ts) AS ts_min,
    MAX(ts + dur_ns) AS ts_max
  FROM frame_level
),
-- BufferTX track selection.
-- Base: track name is 'BufferTX - <layer-name>#<bufferId>'. Filter by package first,
-- then (when __LAYER_GLOB__ != '*') also require the layer name to match so the
-- numerator (BufferTX consumed frames) is from the same layer as the frame-timeline
-- denominator.
buffertx_tracks AS (
  SELECT id
  FROM track
  WHERE name GLOB 'BufferTX -*__PACKAGE_NAME__*'
    AND ('__LAYER_GLOB__' = '*' OR name GLOB 'BufferTX -*__LAYER_GLOB__*')
),
buffertx_points AS (
  SELECT
    c.track_id,
    c.ts,
    c.value
  FROM counter c
  JOIN buffertx_tracks bt ON bt.id = c.track_id
  JOIN hpfc_window w
  WHERE c.ts >= w.ts_start
    AND c.ts <= w.ts_end
),
buffertx_deltas AS (
  SELECT
    track_id,
    ts,
    LAG(value) OVER (PARTITION BY track_id ORDER BY ts) AS prev_value,
    value,
    value - LAG(value) OVER (PARTITION BY track_id ORDER BY ts) AS delta
  FROM buffertx_points
),
buffertx_counts AS (
  SELECT
    COALESCE(SUM(CASE WHEN prev_value > 0 AND delta < 0 THEN 1 ELSE 0 END), 0.0) AS consumed_frames_raw,
    COALESCE(SUM(CASE WHEN delta > 0 THEN delta ELSE 0 END), 0.0) AS produced_frames_raw,
    COALESCE(SUM(CASE WHEN prev_value > 0 AND delta < -1 THEN (-delta - 1) ELSE 0 END), 0.0) AS discarded_frames_raw,
    COALESCE(SUM(CASE WHEN prev_value > 0 AND delta < -1 THEN 1 ELSE 0 END), 0) AS large_negative_jump_events,
    COALESCE(SUM(CASE WHEN prev_value > 0 AND delta < -1 THEN (-delta - 1) ELSE 0 END), 0.0) AS large_negative_jump_frames,
    COALESCE(MAX(CASE WHEN prev_value > 0 AND delta < 0 THEN -delta ELSE 0 END), 0.0) AS max_single_dequeue_frames,
    COALESCE(COUNT(*), 0) AS point_count,
    MIN(ts) AS ts_min,
    MAX(ts) AS ts_max
  FROM buffertx_deltas
  WHERE delta IS NOT NULL
),
-- PrevFramePending tracks are SurfaceFlinger vsync-tick counters. Android emits
-- one per layer, so scoping to package/layer avoids accidentally picking an
-- unrelated display's track just because it has more ticks.
prevpending_tracks AS (
  SELECT
    id,
    CASE
      WHEN '__LAYER_GLOB__' != '*' AND name GLOB 'PrevFramePending*__LAYER_GLOB__*' THEN 2
      WHEN name GLOB 'PrevFramePending*__PACKAGE_NAME__*' THEN 1
      ELSE 0
    END AS scope_rank
  FROM track
  WHERE name GLOB 'PrevFramePending*'
),
prevpending_points AS (
  SELECT
    c.track_id,
    pt.scope_rank,
    c.ts,
    c.value
  FROM counter c
  JOIN prevpending_tracks pt ON pt.id = c.track_id
  JOIN hpfc_window w
  CROSS JOIN buffertx_counts b
  WHERE c.ts >= COALESCE(b.ts_min, w.ts_start)
    AND c.ts <= COALESCE(b.ts_max, w.ts_end)
),
prevpending_track_counts AS (
  SELECT
    track_id,
    MAX(scope_rank) AS scope_rank,
    COUNT(*) AS tick_count
  FROM prevpending_points
  GROUP BY track_id
),
-- Prefer app/layer-scoped tracks (rank > 0) over global tracks; within same rank
-- tier, pick the one with the most ticks in the window.
selected_prevpending_track AS (
  SELECT track_id
  FROM prevpending_track_counts
  ORDER BY scope_rank DESC, tick_count DESC
  LIMIT 1
),
sf_vsync_points AS (
  SELECT p.track_id, p.ts, p.value
  FROM prevpending_points p
  JOIN selected_prevpending_track s ON p.track_id = s.track_id
),
sf_vsync_counts AS (
  SELECT
    COUNT(*) AS sf_vsync_ticks,
    COALESCE(SUM(CASE WHEN value > 0 THEN value ELSE 0 END), 0.0) AS sf_vsync_pending_nonzero_ticks
  FROM sf_vsync_points
),
selected_counts AS (
  SELECT
    c.total_frames AS timeline_total_frames,
    c.presented_frames AS timeline_presented_frames,
    c.dropped_frames AS timeline_dropped_frames,
    c.ts_min AS timeline_ts_min,
    c.ts_max AS timeline_ts_max,
    CAST(ROUND(b.consumed_frames_raw) AS INTEGER) AS buffertx_consumed_frames,
    CAST(ROUND(b.produced_frames_raw) AS INTEGER) AS buffertx_produced_frames,
    CAST(ROUND(b.discarded_frames_raw) AS INTEGER) AS buffertx_discarded_frames,
    CAST(b.large_negative_jump_events AS INTEGER) AS buffertx_large_negative_jump_events,
    CAST(ROUND(b.large_negative_jump_frames) AS INTEGER) AS buffertx_large_negative_jump_frames,
    CAST(ROUND(b.max_single_dequeue_frames) AS INTEGER) AS buffertx_max_single_dequeue_frames,
    b.point_count AS buffertx_point_count,
    b.ts_min AS buffertx_ts_min,
    b.ts_max AS buffertx_ts_max,
    sv.sf_vsync_ticks,
    CAST(ROUND(sv.sf_vsync_pending_nonzero_ticks) AS INTEGER) AS sf_vsync_pending_nonzero_ticks,
    CASE
      WHEN b.consumed_frames_raw > 0 AND sv.sf_vsync_ticks > 0 THEN 'buffertx_vsync'
      WHEN b.consumed_frames_raw > 0 THEN 'buffertx_consumed'
      ELSE 'frame_timeline'
    END AS fps_source,
    CASE
      WHEN b.consumed_frames_raw > 0 AND sv.sf_vsync_ticks > 0
      THEN MIN(CAST(ROUND(b.consumed_frames_raw) AS INTEGER), sv.sf_vsync_ticks)
      WHEN b.consumed_frames_raw > 0 THEN CAST(ROUND(b.consumed_frames_raw) AS INTEGER)
      ELSE c.presented_frames
    END AS presented_frames_selected,
    CASE
      WHEN b.consumed_frames_raw > 0 AND sv.sf_vsync_ticks > 0
      THEN MAX(sv.sf_vsync_ticks - CAST(ROUND(b.consumed_frames_raw) AS INTEGER), 0)
      ELSE c.dropped_frames
    END AS dropped_frames_vsync_gap,
    CASE
      WHEN b.consumed_frames_raw > 0 THEN CAST(ROUND(b.discarded_frames_raw) AS INTEGER)
      ELSE 0
    END AS dropped_frames_buffertx_jump,
    CASE
      WHEN b.consumed_frames_raw > 0 AND sv.sf_vsync_ticks > 0
      THEN sv.sf_vsync_ticks + CAST(ROUND(b.discarded_frames_raw) AS INTEGER)
      WHEN b.consumed_frames_raw > 0
      THEN CAST(ROUND(b.consumed_frames_raw) AS INTEGER)
         + c.dropped_frames
         + CAST(ROUND(b.discarded_frames_raw) AS INTEGER)
      ELSE c.total_frames
    END AS total_frames_selected,
    CASE
      WHEN b.consumed_frames_raw > 0 AND sv.sf_vsync_ticks > 0
      THEN MAX(sv.sf_vsync_ticks - CAST(ROUND(b.consumed_frames_raw) AS INTEGER), 0)
         + CAST(ROUND(b.discarded_frames_raw) AS INTEGER)
      WHEN b.consumed_frames_raw > 0
      THEN c.dropped_frames + CAST(ROUND(b.discarded_frames_raw) AS INTEGER)
      ELSE c.dropped_frames
    END AS dropped_frames_selected,
    CASE
      WHEN b.consumed_frames_raw > 0 THEN b.ts_min
      ELSE c.ts_min
    END AS ts_min_selected,
    CASE
      WHEN b.consumed_frames_raw > 0 THEN b.ts_max
      ELSE c.ts_max
    END AS ts_max_selected
  FROM counts c
  CROSS JOIN buffertx_counts b
  CROSS JOIN sf_vsync_counts sv
),
dur_stats AS (
  SELECT
    COALESCE(PERCENTILE(CASE WHEN is_dropped THEN NULL ELSE dur_ns END, 50) / 1e6, 0.0) AS frame_dur_ms_p50,
    COALESCE(PERCENTILE(CASE WHEN is_dropped THEN NULL ELSE dur_ns END, 90) / 1e6, 0.0) AS frame_dur_ms_p90,
    COALESCE(PERCENTILE(CASE WHEN is_dropped THEN NULL ELSE dur_ns END, 95) / 1e6, 0.0) AS frame_dur_ms_p95,
    COALESCE(PERCENTILE(CASE WHEN is_dropped THEN NULL ELSE dur_ns END, 99) / 1e6, 0.0) AS frame_dur_ms_p99,
    COALESCE(AVG(CASE WHEN is_dropped THEN NULL ELSE dur_ns END) / 1e6, 0.0) AS frame_dur_ms_avg
  FROM frame_level
)
SELECT
  sc.total_frames_selected AS total_frames,
  sc.timeline_total_frames,
  c.non_numeric_or_interpolated_frames,
  r.consumer_rows,
  r.non_numeric_or_interpolated_rows,
  sc.presented_frames_selected AS presented_frames,
  sc.timeline_presented_frames,
  sc.timeline_dropped_frames,
  sc.buffertx_consumed_frames,
  sc.buffertx_produced_frames,
  sc.buffertx_discarded_frames,
  sc.buffertx_large_negative_jump_events,
  sc.buffertx_large_negative_jump_frames,
  sc.buffertx_max_single_dequeue_frames,
  sc.buffertx_point_count,
  sc.sf_vsync_ticks,
  sc.sf_vsync_pending_nonzero_ticks,
  sc.fps_source,
  sc.dropped_frames_selected AS dropped_frames,
  sc.dropped_frames_vsync_gap,
  sc.dropped_frames_buffertx_jump,
  CASE
    WHEN sc.fps_source = 'buffertx_vsync' THEN sc.dropped_frames_vsync_gap
    ELSE 0
  END AS dropped_frames_vsync_no_consume,
  c.missed_frames,
  c.app_jank_frames,
  c.sf_jank_frames,
  ROUND(
    CASE
      WHEN sc.total_frames_selected > 0 THEN CAST(sc.dropped_frames_selected AS DOUBLE) / sc.total_frames_selected
      ELSE 0.0
    END,
    6
  ) AS drop_rate,
  ROUND(
    CASE
      WHEN sc.total_frames_selected > 0 THEN CAST(c.missed_frames AS DOUBLE) / sc.total_frames_selected
      ELSE 0.0
    END,
    6
  ) AS missed_rate,
  ROUND(CAST(sc.presented_frames_selected AS DOUBLE) / CAST(__TRACE_SECONDS__ AS DOUBLE), 3) AS fps_window_norm,
  ROUND(
    CASE
      WHEN sc.ts_min_selected IS NOT NULL AND sc.ts_max_selected IS NOT NULL AND sc.ts_max_selected > sc.ts_min_selected
      THEN CAST(sc.presented_frames_selected AS DOUBLE) / ((sc.ts_max_selected - sc.ts_min_selected) / 1e9)
      ELSE CAST(sc.presented_frames_selected AS DOUBLE) / CAST(__TRACE_SECONDS__ AS DOUBLE)
    END,
    3
  ) AS fps_effective,
  ROUND(
    CASE
      WHEN sc.ts_min_selected IS NOT NULL AND sc.ts_max_selected IS NOT NULL AND sc.ts_max_selected > sc.ts_min_selected
      THEN CAST(sc.presented_frames_selected AS DOUBLE) / ((sc.ts_max_selected - sc.ts_min_selected) / 1e9)
      ELSE CAST(sc.presented_frames_selected AS DOUBLE) / CAST(__TRACE_SECONDS__ AS DOUBLE)
    END,
    3
  ) AS fps,
  ROUND(
    CASE
      WHEN sc.ts_min_selected IS NOT NULL AND sc.ts_max_selected IS NOT NULL AND sc.ts_max_selected > sc.ts_min_selected
      THEN (sc.ts_max_selected - sc.ts_min_selected) / 1e9
      ELSE CAST(__TRACE_SECONDS__ AS DOUBLE)
    END,
    6
  ) AS effective_window_sec,
  d.frame_dur_ms_p50,
  d.frame_dur_ms_p90,
  d.frame_dur_ms_p95,
  d.frame_dur_ms_p99,
  d.frame_dur_ms_avg
FROM counts c
CROSS JOIN selected_counts sc
CROSS JOIN raw_counts r
CROSS JOIN dur_stats d;

SELECT
  json_object(
    'total_frames', total_frames,
    'timeline_total_frames', timeline_total_frames,
    'non_numeric_or_interpolated_frames', non_numeric_or_interpolated_frames,
    'consumer_rows', consumer_rows,
    'non_numeric_or_interpolated_rows', non_numeric_or_interpolated_rows,
    'presented_frames', presented_frames,
    'timeline_presented_frames', timeline_presented_frames,
    'timeline_dropped_frames', timeline_dropped_frames,
    'buffertx_consumed_frames', buffertx_consumed_frames,
    'buffertx_produced_frames', buffertx_produced_frames,
    'buffertx_discarded_frames', buffertx_discarded_frames,
    'buffertx_large_negative_jump_events', buffertx_large_negative_jump_events,
    'buffertx_large_negative_jump_frames', buffertx_large_negative_jump_frames,
    'buffertx_max_single_dequeue_frames', buffertx_max_single_dequeue_frames,
    'buffertx_point_count', buffertx_point_count,
    'sf_vsync_ticks', sf_vsync_ticks,
    'sf_vsync_pending_nonzero_ticks', sf_vsync_pending_nonzero_ticks,
    'fps_source', fps_source,
    'dropped_frames', dropped_frames,
    'dropped_frames_vsync_gap', dropped_frames_vsync_gap,
    'dropped_frames_buffertx_jump', dropped_frames_buffertx_jump,
    'dropped_frames_vsync_no_consume', dropped_frames_vsync_no_consume,
    'drop_rate', drop_rate,
    'missed_frames', missed_frames,
    'missed_rate', missed_rate,
    'fps_window_norm', fps_window_norm,
    'fps_20s_norm', fps_window_norm,
    'fps_effective', fps_effective,
    'fps', fps,
    'effective_window_sec', effective_window_sec,
    'app_jank_frames', app_jank_frames,
    'sf_jank_frames', sf_jank_frames,
    'frame_dur_ms_p50', frame_dur_ms_p50,
    'frame_dur_ms_p90', frame_dur_ms_p90,
    'frame_dur_ms_p95', frame_dur_ms_p95,
    'frame_dur_ms_p99', frame_dur_ms_p99,
    'frame_dur_ms_avg', frame_dur_ms_avg
  ) AS payload
FROM hpfc_metric_row;
