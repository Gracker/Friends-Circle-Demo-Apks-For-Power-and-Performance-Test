DROP VIEW IF EXISTS hpfc_consumer_frames_sf;
CREATE PERFETTO VIEW hpfc_consumer_frames_sf AS
SELECT sf.*
FROM hpfc_sf_frames_raw sf
JOIN hpfc_window w
WHERE sf.ts >= w.ts_start
  AND sf.ts <= w.ts_end;
