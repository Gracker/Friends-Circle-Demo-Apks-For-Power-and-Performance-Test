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
        
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                renderer.setOffset(-distanceX, distanceY);
                requestRender();
                return true;
            }
        });
        
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                renderer.setZoom(detector.getScaleFactor());
                requestRender();
                return true;
            }
        });
    }
    
    public void setLoadType(@LoadType.Type int loadType) {
        renderer.setLoadType(loadType);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }
}

