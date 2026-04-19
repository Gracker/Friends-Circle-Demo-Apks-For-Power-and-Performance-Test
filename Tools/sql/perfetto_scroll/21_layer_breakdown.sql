WITH by_layer AS (
  SELECT
    layer_name,
    COUNT(DISTINCT CAST(name AS INTEGER)) AS total_frames,
    COUNT(
      DISTINCT CASE
        WHEN (present_type = 'Dropped Frame' OR jank_type GLOB '*Dropped Frame*')
        THEN CAST(name AS INTEGER)
      END
    ) AS dropped_frames
  FROM __CONSUMER_TABLE__
  GROUP BY layer_name
  ORDER BY total_frames DESC
  LIMIT 50
)
SELECT
  COALESCE(
    json_group_array(
      json_object(
        'layer_name', layer_name,
        'total_frames', total_frames,
        'dropped_frames', dropped_frames
      )
    ),
    '[]'
  ) AS payload
FROM by_layer;
