package com.example.wechatfriendforglmap;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import android.widget.OverScroller;

import com.example.loadconfig.LoadType;

/**
 * GLSurfaceView for map rendering with touch support.
 * Supports pan and pinch-to-zoom gestures with smooth fling effect.
 */
public class GLMapView extends GLSurfaceView {

    private GLMapRenderer renderer;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private OverScroller scroller;
    private boolean flingStarted = false;

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

        scroller = new OverScroller(context);

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

                // Reset fling tracking state
                mLastFlingX = 0;
                mLastFlingY = 0;
                flingStarted = true;

                // Start fling from 0,0 - we only care about deltas
                scroller.fling(0, 0, (int) velocityX, (int) velocityY,
                        Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);

                // Trigger animation loop
                postInvalidateOnAnimation();
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

    public void zoomIn() {
        queueEvent(() -> renderer.setZoom(1.2f));
        requestRender();
    }

    public void zoomOut() {
        queueEvent(() -> renderer.setZoom(1f / 1.2f));
        requestRender();
    }

    public void resetView() {
        // Cancel any active fling animation to prevent it from overriding the reset
        if (!scroller.isFinished()) {
            scroller.forceFinished(true);
        }
        flingStarted = false;
        queueEvent(renderer::resetView);
        requestRender();
    }

    public void toggleMarkers() {
        queueEvent(renderer::toggleMarkers);
        requestRender();
    }

    // We need to keep track of last fling position to calculate deltas
    private int mLastFlingX;
    private int mLastFlingY;

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            int currX = scroller.getCurrX();
            int currY = scroller.getCurrY();

            // Skip the first frame to initialize tracking values
            if (flingStarted) {
                mLastFlingX = currX;
                mLastFlingY = currY;
                flingStarted = false;
                postInvalidateOnAnimation();
                return;
            }

            float dx = currX - mLastFlingX;
            float dy = currY - mLastFlingY;

            mLastFlingX = currX;
            mLastFlingY = currY;

            // Apply delta to renderer
            renderer.setOffset(dx, dy);
            requestRender();

            // Keep animating
            postInvalidateOnAnimation();
        } else {
            // Fling finished
            flingStarted = false;
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
            flingStarted = false;
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
