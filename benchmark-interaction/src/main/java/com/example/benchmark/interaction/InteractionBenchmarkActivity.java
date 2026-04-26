package com.example.benchmark.interaction;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class InteractionBenchmarkActivity extends Activity {
    private static final String TAG = "HPFCInteraction";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new TapBenchmarkView(this));
    }

    private static final class TapBenchmarkView extends View {
        private final int[] colors = {
                Color.rgb(16, 185, 129),
                Color.rgb(59, 130, 246),
                Color.rgb(239, 68, 68),
                Color.rgb(245, 158, 11)
        };
        private int colorIndex = 0;
        private long lastEventMs = -1L;
        private long lastHandleMs = -1L;
        private int tapCount = 0;
        private boolean pendingDraw = false;

        TapBenchmarkView(Activity activity) {
            super(activity);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
                return true;
            }
            Trace.beginSection("HPFC_TAP_ACTION_DOWN");
            tapCount++;
            lastEventMs = event.getEventTime();
            lastHandleMs = SystemClock.uptimeMillis();
            pendingDraw = true;
            colorIndex = (colorIndex + 1) % colors.length;
            Log.d(TAG, "ACTION_DOWN tap=" + tapCount
                    + " event_ms=" + lastEventMs
                    + " handle_ms=" + lastHandleMs
                    + " dispatch_latency_ms=" + (lastHandleMs - lastEventMs));
            Choreographer.getInstance().postFrameCallback(frameTimeNanos -> {
                long nowMs = SystemClock.uptimeMillis();
                Log.d(TAG, "FRAME_CALLBACK tap=" + tapCount
                        + " frame_time_ms=" + (frameTimeNanos / 1000000L)
                        + " uptime_ms=" + nowMs
                        + " latency_from_event_ms=" + (nowMs - lastEventMs)
                        + " latency_from_handle_ms=" + (nowMs - lastHandleMs));
            });
            invalidate();
            Trace.endSection();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Trace.beginSection("HPFC_TAP_ON_DRAW");
            canvas.drawColor(colors[colorIndex]);
            if (pendingDraw && lastEventMs > 0) {
                long nowMs = SystemClock.uptimeMillis();
                Log.d(TAG, "ON_DRAW tap=" + tapCount
                        + " uptime_ms=" + nowMs
                        + " latency_from_event_ms=" + (nowMs - lastEventMs)
                        + " latency_from_handle_ms=" + (nowMs - lastHandleMs));
                pendingDraw = false;
            }
            Trace.endSection();
        }
    }
}
