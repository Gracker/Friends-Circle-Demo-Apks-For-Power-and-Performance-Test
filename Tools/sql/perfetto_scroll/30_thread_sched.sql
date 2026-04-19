WITH
win AS (
  SELECT ts_start, ts_end
  FROM hpfc_window
),
-- Restrict upids to processes whose lifetime overlaps the window. This avoids
-- double-counting when the app is restarted mid-trace: old instance's PID lies
-- entirely before ts_start and is filtered out.
windowed_app_upids AS (
  SELECT DISTINCT p.upid
  FROM process p
  JOIN hpfc_target_app_upid t ON t.upid = p.upid
  CROSS JOIN win w
  WHERE (p.start_ts IS NULL OR p.start_ts <= w.ts_end)
    AND (p.end_ts IS NULL OR p.end_ts >= w.ts_start)
),
target_threads AS (
  SELECT t.utid, 'main' AS thread_kind
  FROM thread t
  JOIN windowed_app_upids p ON t.upid = p.upid
  WHERE t.is_main_thread = 1

  UNION ALL

  -- Android's HWUI render thread is exactly 'RenderThread'. Avoid the broader
  -- glob which would also pick up hwuiRenderThread / RenderThreadPool-* and
  -- inflate running_dur.
  SELECT t.utid, 'render' AS thread_kind
  FROM thread t
  JOIN windowed_app_upids p ON t.upid = p.upid
  WHERE t.name = 'RenderThread'
),
thread_kinds AS (
  SELECT 'main' AS thread_kind
  UNION ALL
  SELECT 'render' AS thread_kind
),
running_raw AS (
  SELECT
    tt.thread_kind,
    s.utid,
    s.cpu,
    s.priority,
    s.ts,
    CASE WHEN s.dur = -1 THEN w.ts_end ELSE s.ts + s.dur END AS ts_end
  FROM sched s
  JOIN target_threads tt USING (utid)
  CROSS JOIN win w
  WHERE s.ts < w.ts_end
    AND (CASE WHEN s.dur = -1 THEN w.ts_end ELSE s.ts + s.dur END) > w.ts_start
),
running AS (
  SELECT
    rr.thread_kind,
    rr.utid,
    rr.cpu,
    rr.priority,
    MAX(rr.ts, w.ts_start) AS ts,
    MIN(rr.ts_end, w.ts_end) AS ts_end,
    MIN(rr.ts_end, w.ts_end) - MAX(rr.ts, w.ts_start) AS dur
  FROM running_raw rr
  CROSS JOIN win w
  WHERE MIN(rr.ts_end, w.ts_end) > MAX(rr.ts, w.ts_start)
),
runnable_raw AS (
  SELECT
    tt.thread_kind,
    ts.utid,
    ts.ts,
    CASE WHEN ts.dur = -1 THEN w.ts_end ELSE ts.ts + ts.dur END AS ts_end,
    ts.state,
    ts.cpu
  FROM thread_state ts
  JOIN target_threads tt USING (utid)
  CROSS JOIN win w
  WHERE ts.ts < w.ts_end
    AND (CASE WHEN ts.dur = -1 THEN w.ts_end ELSE ts.ts + ts.dur END) > w.ts_start
    AND (ts.state = 'R' OR ts.state = 'Runnable' OR ts.state GLOB 'R*')
    AND ts.state != 'Running'
    AND (ts.cpu IS NULL OR ts.cpu < 0)
),
runnable AS (
  SELECT
    rr.thread_kind,
    rr.utid,
    MAX(rr.ts, w.ts_start) AS ts,
    MIN(rr.ts_end, w.ts_end) AS ts_end,
    MIN(rr.ts_end, w.ts_end) - MAX(rr.ts, w.ts_start) AS dur
  FROM runnable_raw rr
  CROSS JOIN win w
  WHERE MIN(rr.ts_end, w.ts_end) > MAX(rr.ts, w.ts_start)
),
freq_events AS (
  SELECT cct.cpu, c.ts, c.value AS freq
  FROM cpu_counter_track cct
  JOIN counter c ON c.track_id = cct.id
  WHERE cct.name = 'cpufreq'
    AND cct.type = 'cpu_frequency'
),
freq_before_start AS (
  SELECT fe.cpu, MAX(fe.ts) AS ts
  FROM freq_events fe
  CROSS JOIN win w
  WHERE fe.ts <= w.ts_start
  GROUP BY fe.cpu
),
freq_at_start AS (
  SELECT fbs.cpu, w.ts_start AS ts, fe.freq
  FROM freq_before_start fbs
  JOIN freq_events fe ON fe.cpu = fbs.cpu AND fe.ts = fbs.ts
  CROSS JOIN win w
),
freq_in_window AS (
  SELECT fe.cpu, fe.ts, fe.freq
  FROM freq_events fe
  CROSS JOIN win w
  WHERE fe.ts >= w.ts_start AND fe.ts < w.ts_end
  UNION ALL
  SELECT cpu, ts, freq FROM freq_at_start
),
freq_spans AS (
  SELECT
    fiw.cpu,
    fiw.ts,
    LEAD(fiw.ts) OVER (PARTITION BY fiw.cpu ORDER BY fiw.ts) AS ts_end,
    fiw.freq
  FROM freq_in_window fiw
),
freq_spans_clip AS (
  SELECT
    fs.cpu,
    MAX(fs.ts, w.ts_start) AS ts,
    MIN(COALESCE(fs.ts_end, w.ts_end), w.ts_end) AS ts_end,
    fs.freq
  FROM freq_spans fs
  CROSS JOIN win w
  WHERE COALESCE(fs.ts_end, w.ts_end) > w.ts_start
    AND fs.ts < w.ts_end
    AND MIN(COALESCE(fs.ts_end, w.ts_end), w.ts_end) > MAX(fs.ts, w.ts_start)
),
cpu_maxfreq AS (
  SELECT cpu, MAX(freq) AS max_freq
  FROM freq_events
  GROUP BY cpu
),
tier_boundaries AS (
  WITH distinct_levels AS (
    SELECT DISTINCT max_freq
    FROM cpu_maxfreq
    WHERE max_freq IS NOT NULL
  ),
  sorted AS (
    SELECT
      max_freq,
      LAG(max_freq) OVER (ORDER BY max_freq) AS prev_freq
    FROM distinct_levels
  )
  SELECT max_freq AS upper_freq
  FROM sorted
  WHERE prev_freq IS NOT NULL
    AND CAST(max_freq AS DOUBLE) / CAST(prev_freq AS DOUBLE) >= 1.10
),
cpu_tier AS (
  SELECT
    cm.cpu,
    cm.max_freq,
    1 + COALESCE(SUM(CASE WHEN cm.max_freq >= tb.upper_freq THEN 1 ELSE 0 END), 0) AS tier_rank
  FROM cpu_maxfreq cm
  LEFT JOIN tier_boundaries tb ON 1 = 1
  GROUP BY cm.cpu, cm.max_freq
),
tier_count AS (
  SELECT COALESCE(MAX(tier_rank), 0) AS tier_count
  FROM cpu_tier
),
cpu_cluster AS (
  SELECT
    ct.cpu,
    ct.tier_rank,
    tc.tier_count,
    CASE
      WHEN tc.tier_count <= 1 THEN 'mid'
      WHEN ct.tier_rank = 1 THEN 'little'
      WHEN ct.tier_rank = tc.tier_count THEN 'big'
      ELSE 'mid'
    END AS cluster3
  FROM cpu_tier ct
  CROSS JOIN tier_count tc
),
running_with_cluster AS (
  SELECT
    r.thread_kind,
    r.utid,
    r.cpu,
    r.priority,
    r.ts,
    r.ts_end,
    r.dur,
    COALESCE(cc.cluster3, 'unknown') AS cluster3
  FROM running r
  LEFT JOIN cpu_cluster cc USING (cpu)
),
running_totals AS (
  SELECT
    thread_kind,
    SUM(dur) AS running_dur,
    SUM(CASE WHEN priority IS NOT NULL THEN dur * priority ELSE 0 END)
      / NULLIF(SUM(CASE WHEN priority IS NOT NULL THEN dur ELSE 0 END), 0) AS avg_running_priority,
    MIN(priority) AS min_running_priority,
    MAX(priority) AS max_running_priority
  FROM running_with_cluster
  GROUP BY thread_kind
),
runnable_totals AS (
  SELECT
    thread_kind,
    SUM(dur) AS runnable_dur
  FROM runnable
  GROUP BY thread_kind
),
running_by_cluster AS (
  SELECT
    thread_kind,
    cluster3,
    SUM(dur) AS running_dur
  FROM running_with_cluster
  GROUP BY thread_kind, cluster3
),
run_freq_overlap AS (
  SELECT
    r.thread_kind,
    r.cluster3,
    MAX(r.ts, f.ts) AS ts,
    MIN(r.ts_end, f.ts_end) AS ts_end,
    MIN(r.ts_end, f.ts_end) - MAX(r.ts, f.ts) AS dur,
    f.freq
  FROM running_with_cluster r
  JOIN freq_spans_clip f
    ON f.cpu = r.cpu
   AND r.ts < f.ts_end
   AND r.ts_end > f.ts
  WHERE MIN(r.ts_end, f.ts_end) > MAX(r.ts, f.ts)
),
avg_running_freq_by_cluster AS (
  SELECT
    thread_kind,
    cluster3,
    SUM(dur * freq) / NULLIF(SUM(dur), 0) AS avg_running_freq
  FROM run_freq_overlap
  GROUP BY thread_kind, cluster3
),
kind_metrics AS (
  SELECT
    tk.thread_kind,
    COALESCE(rt.running_dur, 0) AS running_dur,
    COALESCE(rbt.runnable_dur, 0) AS runnable_dur,
    rt.avg_running_priority,
    rt.min_running_priority,
    rt.max_running_priority,
    COALESCE(SUM(CASE WHEN rbc.cluster3 = 'little' THEN rbc.running_dur ELSE 0 END), 0)
      / NULLIF(COALESCE(rt.running_dur, 0), 0) AS running_share_little,
    COALESCE(SUM(CASE WHEN rbc.cluster3 = 'mid' THEN rbc.running_dur ELSE 0 END), 0)
      / NULLIF(COALESCE(rt.running_dur, 0), 0) AS running_share_mid,
    COALESCE(SUM(CASE WHEN rbc.cluster3 = 'big' THEN rbc.running_dur ELSE 0 END), 0)
      / NULLIF(COALESCE(rt.running_dur, 0), 0) AS running_share_big,
    COALESCE(SUM(CASE WHEN rbc.cluster3 = 'unknown' THEN rbc.running_dur ELSE 0 END), 0)
      / NULLIF(COALESCE(rt.running_dur, 0), 0) AS running_share_unknown,
    MAX(CASE WHEN arf.cluster3 = 'little' THEN arf.avg_running_freq END) AS avg_running_freq_little,
    MAX(CASE WHEN arf.cluster3 = 'mid' THEN arf.avg_running_freq END) AS avg_running_freq_mid,
    MAX(CASE WHEN arf.cluster3 = 'big' THEN arf.avg_running_freq END) AS avg_running_freq_big,
    MAX(CASE WHEN arf.cluster3 = 'unknown' THEN arf.avg_running_freq END) AS avg_running_freq_unknown
  FROM thread_kinds tk
  LEFT JOIN running_totals rt ON rt.thread_kind = tk.thread_kind
  LEFT JOIN runnable_totals rbt ON rbt.thread_kind = tk.thread_kind
  LEFT JOIN running_by_cluster rbc ON rbc.thread_kind = tk.thread_kind
  LEFT JOIN avg_running_freq_by_cluster arf ON arf.thread_kind = tk.thread_kind
  GROUP BY
    tk.thread_kind,
    rt.running_dur,
    rt.avg_running_priority,
    rt.min_running_priority,
    rt.max_running_priority,
    rbt.runnable_dur
)
SELECT json_object(
  'main', (
    SELECT json_object(
      'running_time_ms', ROUND(running_dur / 1e6, 3),
      'runnable_time_ms', ROUND(runnable_dur / 1e6, 3),
      'avg_running_priority', ROUND(avg_running_priority, 4),
      'min_running_priority', min_running_priority,
      'max_running_priority', max_running_priority,
      'running_share_little', ROUND(running_share_little, 6),
      'running_share_mid', ROUND(running_share_mid, 6),
      'running_share_big', ROUND(running_share_big, 6),
      'running_share_unknown', ROUND(running_share_unknown, 6),
      'avg_running_freq_little', ROUND(avg_running_freq_little, 3),
      'avg_running_freq_mid', ROUND(avg_running_freq_mid, 3),
      'avg_running_freq_big', ROUND(avg_running_freq_big, 3),
      'avg_running_freq_unknown', ROUND(avg_running_freq_unknown, 3)
    )
    FROM kind_metrics
    WHERE thread_kind = 'main'
  ),
  'render', (
    SELECT json_object(
      'running_time_ms', ROUND(running_dur / 1e6, 3),
      'runnable_time_ms', ROUND(runnable_dur / 1e6, 3),
      'avg_running_priority', ROUND(avg_running_priority, 4),
      'min_running_priority', min_running_priority,
      'max_running_priority', max_running_priority,
      'running_share_little', ROUND(running_share_little, 6),
      'running_share_mid', ROUND(running_share_mid, 6),
      'running_share_big', ROUND(running_share_big, 6),
      'running_share_unknown', ROUND(running_share_unknown, 6),
      'avg_running_freq_little', ROUND(avg_running_freq_little, 3),
      'avg_running_freq_mid', ROUND(avg_running_freq_mid, 3),
      'avg_running_freq_big', ROUND(avg_running_freq_big, 3),
      'avg_running_freq_unknown', ROUND(avg_running_freq_unknown, 3)
    )
    FROM kind_metrics
    WHERE thread_kind = 'render'
  ),
  'cluster_tier_count', (SELECT tier_count FROM tier_count),
  'cluster_cpu_count', (SELECT COUNT(*) FROM cpu_tier)
) AS payload;
