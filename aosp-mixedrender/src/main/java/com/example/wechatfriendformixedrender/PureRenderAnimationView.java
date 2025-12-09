package com.example.wechatfriendformixedrender;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadType;

/**
 * Pure RenderThread animation view using SurfaceView.
 * All rendering happens on a dedicated render thread, not the UI Thread or standard RenderThread.
 * This simulates video playback or game rendering scenarios.
 */
public class PureRenderAnimationView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "PureRenderAnimation";
    
    private Thread renderThread;
    private volatile boolean isRunning = false;
    private SurfaceHolder holder;
    
    private Paint wavePaint;
    private Paint circlePaint;
    private Paint textPaint;
    
    private float animationTime = 0f;
    private int loadType = LoadType.MINIMAL;
    private final Random random = new Random(12345L);

    public PureRenderAnimationView(Context context) {
        super(context);
        init();
    }

    public PureRenderAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
        
        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(Color.parseColor("#E91E63"));
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(4);
        
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    public void setLoadType(@LoadType.Type int loadType) {
        this.loadType = loadType;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        renderThread = new Thread(this, "PureRenderAnimationThread");
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        try {
            renderThread.join(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping render thread", e);
        }
    }

    @Override
    public void run() {
        Log.i(TAG, "Pure render animation thread started");
        
        while (isRunning) {
            long frameStart = System.nanoTime();
            
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    Trace.beginSection("PureRenderAnimation_draw");
                    drawAnimation(canvas);
                    Trace.endSection();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error drawing", e);
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        Log.e(TAG, "Error posting canvas", e);
                    }
                }
            }
            
            // Execute load on render thread
            executeLoad();
            
            // Update animation time
            animationTime += 0.03f;
            
            // Frame rate limiting (~60fps)
            long frameTime = System.nanoTime() - frameStart;
            long sleepTime = (16_666_666 - frameTime) / 1_000_000;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        Log.i(TAG, "Pure render animation thread stopped");
    }
    
    private void drawAnimation(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        // Background gradient simulation
        canvas.drawColor(Color.parseColor("#1A237E"));
        
        // Draw animated wave patterns
        for (int i = 0; i < 5; i++) {
            float phase = animationTime + i * 0.5f;
            int alpha = 100 + i * 30;
            wavePaint.setAlpha(alpha);
            
            Path path = new Path();
            path.moveTo(0, height / 2f);
            
            for (int x = 0; x <= width; x += 10) {
                float y = height / 2f + 
                          (float) Math.sin((x * 0.02f) + phase) * 30 +
                          (float) Math.sin((x * 0.01f) + phase * 0.7f) * 20;
                path.lineTo(x, y);
            }
            
            canvas.drawPath(path, wavePaint);
        }
        
        // Draw animated circles
        int circleCount = 8;
        for (int i = 0; i < circleCount; i++) {
            float angle = (float) (animationTime * 0.5f + i * Math.PI * 2 / circleCount);
            float radius = 80 + 20 * (float) Math.sin(animationTime * 2 + i);
            float x = width / 2f + (float) Math.cos(angle) * 100;
            float y = height / 2f + (float) Math.sin(angle) * 50;
            
            int hue = (int) ((animationTime * 50 + i * 45) % 360);
            circlePaint.setColor(Color.HSVToColor(150, new float[]{hue, 0.7f, 0.9f}));
            
            canvas.drawCircle(x, y, radius / 4, circlePaint);
        }
        
        // Draw label
        canvas.drawText("Pure RenderThread Animation", width / 2f, height - 20, textPaint);
    }
    
    private void executeLoad() {
        // 使用统一的 LoadConfig 获取负载强度
        int iterations = LoadConfig.getInFrameIntensity(loadType);
        
        if (iterations == 0) return;
        
        Trace.beginSection("PureRenderAnimation_executeLoad");
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        Trace.endSection();
    }
}

