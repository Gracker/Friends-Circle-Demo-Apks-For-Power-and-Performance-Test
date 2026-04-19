-- App consumer perspective: frames scheduled by the app process, clipped to window.
-- Reads from hpfc_app_frames_raw (actual_frame_timeline_slice joined by target upid).
DROP VIEW IF EXISTS hpfc_consumer_frames_app;
CREATE PERFETTO VIEW hpfc_consumer_frames_app AS
SELECT af.*
FROM hpfc_app_frames_raw af
JOIN hpfc_window w
WHERE af.ts >= w.ts_start
  AND af.ts <= w.ts_end;
