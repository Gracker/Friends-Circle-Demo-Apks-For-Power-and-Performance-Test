# perfetto_scroll SQL templates

These templates are rendered by `Tools/perfetto_release_scroll_benchmark.py`.

Placeholder tokens:

- `__PACKAGE_NAME__`: target app process name prefix.
- `__LAYER_GLOB__`: layer name filter glob (`*` for all).
- `__TRACE_SECONDS__`: benchmark window size in seconds.
- `__CONSUMER_TABLE__`: resolved frame source table (`hpfc_consumer_frames_app` or `hpfc_consumer_frames_sf`).

Template pipeline:

1. `00_common.sql` creates app frame scope and benchmark window.
2. `10_consumer_app.sql` or `11_consumer_sf.sql` selects consumer perspective.
3. `20_metrics.sql` computes FPS/drop/missed/jank metrics and emits one JSON payload row.
4. `21_layer_breakdown.sql` emits layer-level dropped-frame breakdown as JSON array.
5. `30_thread_sched.sql` computes target app main/render thread scheduling metrics
   (running/runnable time, priority, cluster running share, cluster avg running freq).
