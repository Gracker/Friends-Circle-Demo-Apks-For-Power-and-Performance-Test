package com.example.wechatfriendforglmap;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.example.loadconfig.LoadType;

/**
 * GLSurfaceView for map rendering with touch support.
 * Supports pan and pinch-to-zoom gestures.
 */
public class GLMapView extends GLSurfaceView {

    private GLMapRenderer renderer;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private android.widget.Scroller scroller;

    public GLMapView(Context context) {
        super(context);
        init(context);
    }

    public GLMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);

        renderer = new GLMapRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        scroller = new android.widget.Scroller(context);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // Fix inverted Y direction by passing negative distanceY
                // Also track scroll state for LongFrame load
                renderer.onScrollStart();
                renderer.setOffset(-distanceX, -distanceY);
                requestRender();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                renderer.onScrollStart(); // Ensure scroll state (long frame) is active during fling

                // Fling with the scroller
                // We are scrolling the "viewport" over the map.
                // If we fling UP (positive velocityY), we want the camera to move UP (positive
                // Y),
                // so the map appears to move DOWN.
                // The renderer uses offsetX/Y where +Y moves the map UP relative to camera (or
                // camera down).
                // Let's rely on standard scroll mechanics:
                // startX, startY, velocityX, velocityY, minX, maxX, minY, maxY
                // Since our map is effectively infinite/large, we use large bounds.

                scroller.fling(0, 0, (int) velocityX, (int) velocityY,
                        Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);

                requestRender(); // Trigger computeScroll via draw or invalidation logic,
                                 // GLSurfaceView doesn't standard invalidate like View, but we can hook into
                                 // computeScroll
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        renderer.setZoom(detector.getScaleFactor());
                        requestRender();
                        return true;
                    }
                });
    }

    public void setLoadType(@LoadType.Type int loadType) {
        if (renderer != null) {
            renderer.setLoadType(loadType);
        }
    }

    // We need to keep track of last fling position to calculate deltas
    private int mLastFlingX;
    private int mLastFlingY;

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            int currX = scroller.getCurrX();
            int currY = scroller.getCurrY();

            // Initialize mLastFlingX/Y on the first computeScroll call after a fling
            if (mLastFlingX == 0 && mLastFlingY == 0 && currX != 0 && currY != 0) {
                mLastFlingX = currX;
                mLastFlingY = currY;
            }

            float dx = currX - mLastFlingX;
            float dy = currY - mLastFlingY;

            mLastFlingX = currX;
            mLastFlingY = currY;

            // renderer.setOffset takes "distance" as user finger movement.
            // Positive velocityX means finger moved right? No, fling right.
            // OnScroll: distanceX = start - current.
            // Scroller velocity is pixels/sec.
            // If we fling UP (positive velocityY), the content should move UP?
            // Wait, Standard view: fling UP (finger moves UP) -> content moves DOWN?
            // No, finger moves UP, content follows finger (moves UP).
            // Then on fling (release), content CONTINUES moving UP.

            // onScroll: distanceY = e1.y - e2.y.
            // If I drag DOWN (e2 > e1), distance is negative.
            // renderer.setOffset(-x, -y). So Drag DOWN -> setOffset(pos, pos).

            // Scroller: fling with velocityY.
            // If I fling DOWN (positive velocityY), I want same effect as Drag DOWN.
            // So we should pass the delta directly effectively?

            // onScroll uses -distanceY.
            // Scroller.fling gives us absolute positions.
            // dx = curr - last. Positive if increasing.
            // If velocity is positive (fling down?), curr increases. dx is positive.
            // We want same direction as drag.

            renderer.setOffset(dx, dy);
            requestRender();

            // Keep animating
            postInvalidateOnAnimation();
        } else {
            // Fling finished
            if (scroller.isFinished()) {
                renderer.onScrollStop();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = scaleGestureDetector.onTouchEvent(event);
        result = gestureDetector.onTouchEvent(event) || result;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Stop existing fling
            if (!scroller.isFinished()) {
                scroller.forceFinished(true);
            }
            mLastFlingX = 0;
            mLastFlingY = 0;
        }

        // Stop scrolling state when touch ends AND not flinging
        if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
            if (scroller.isFinished()) {
                renderer.onScrollStop();
            }
        }
        return true;
    }
}
