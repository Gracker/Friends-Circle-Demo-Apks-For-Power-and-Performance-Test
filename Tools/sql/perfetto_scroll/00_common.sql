INCLUDE PERFETTO MODULE android.frames.jank_type;
INCLUDE PERFETTO MODULE android.surfaceflinger;

DROP VIEW IF EXISTS hpfc_target_app_upid;
CREATE PERFETTO VIEW hpfc_target_app_upid AS
SELECT upid, name
FROM process
WHERE name = '__PACKAGE_NAME__'
   OR name GLOB '__PACKAGE_NAME__:*'
   OR name GLOB '__PACKAGE_NAME__:*:*';

DROP VIEW IF EXISTS hpfc_app_frames_raw;
CREATE PERFETTO VIEW hpfc_app_frames_raw AS
SELECT af.*
FROM actual_frame_timeline_slice af
JOIN hpfc_target_app_upid t ON af.upid = t.upid
WHERE ('__LAYER_GLOB__' = '*' OR af.layer_name GLOB '__LAYER_GLOB__');

DROP VIEW IF EXISTS hpfc_sf_frames_raw;
CREATE PERFETTO VIEW hpfc_sf_frames_raw AS
WITH mapped AS (
  SELECT DISTINCT
    m.sf_upid,
    m.sf_vsync
  FROM android_app_to_sf_frame_timeline_match m
  JOIN hpfc_target_app_upid t ON m.app_upid = t.upid
),
mapped_sf AS (
  SELECT sf.*
  FROM actual_frame_timeline_slice sf
  JOIN mapped mp
    ON sf.upid = mp.sf_upid
   AND CAST(sf.name AS INTEGER) = mp.sf_vsync
  WHERE ('__LAYER_GLOB__' = '*' OR sf.layer_name GLOB '__LAYER_GLOB__')
),
layer_matched_sf AS (
  SELECT sf.*
  FROM actual_frame_timeline_slice sf
  WHERE sf.layer_name GLOB '*__PACKAGE_NAME__*'
    AND ('__LAYER_GLOB__' = '*' OR sf.layer_name GLOB '__LAYER_GLOB__')
)
SELECT DISTINCT *
FROM (
  SELECT * FROM mapped_sf
  UNION ALL
  SELECT * FROM layer_matched_sf
);

-- hpfc_window: the time window we attribute metrics to.
-- ts_end = last frame timestamp MINUS __TRAILING_GRACE_NS__ so frames emitted
--   after the formal scroll loop (post_scroll_wait_ms, trace-stop latency,
--   background renderers) don't pull the window forward.
-- ts_start = ts_end - __TRACE_SECONDS__*1e9 so the window covers exactly the
--   formal scroll duration.
DROP VIEW IF EXISTS hpfc_window;
CREATE PERFETTO VIEW hpfc_window AS
WITH frame_timestamps AS (
  SELECT ts FROM hpfc_sf_frames_raw
  UNION ALL
  SELECT ts FROM hpfc_app_frames_raw
  UNION ALL
  SELECT ts FROM actual_frame_timeline_slice
),
trace_bounds AS (
  SELECT MAX(ts) AS ts_last
  FROM frame_timestamps
),
resolved_end AS (
  SELECT
    CASE
      WHEN ts_last - CAST(__TRAILING_GRACE_NS__ AS LONG) IS NULL
        OR ts_last - CAST(__TRAILING_GRACE_NS__ AS LONG) <= 0
      THEN ts_last
      ELSE ts_last - CAST(__TRAILING_GRACE_NS__ AS LONG)
    END AS ts_end
  FROM trace_bounds
)
SELECT
  ts_end,
  ts_end - CAST(__TRACE_SECONDS__ * 1000000000 AS LONG) AS ts_start
FROM resolved_end;
