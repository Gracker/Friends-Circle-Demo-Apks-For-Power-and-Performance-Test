package com.example.wechatfriendfordualwindow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Trace;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import java.util.Random;

/**
 * Second window view that continuously animates to trigger its own RenderThread drawFrame.
 * This creates the dual-window rendering pattern visible in systrace.
 */
public class SecondWindowView extends View implements Choreographer.FrameCallback {
    
    private Paint backgroundPaint;
    private Paint circlePaint;
    private Paint textPaint;
    private Paint itemPaint;
    
    private float animationProgress = 0f;
    private boolean isAnimating = false;
    private Choreographer choreographer;
    
    private int loadType = LoadProfile.LOAD_TYPE_MINIMAL;
    private final Random random = new Random(12345L);
    
    // Simulated list items
    private static final int ITEM_COUNT = 8;
    private float scrollOffset = 0f;

    public SecondWindowView(Context context) {
        super(context);
        init();
    }

    public SecondWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SecondWindowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#1E88E5"));
        
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#FFFFFF"));
        circlePaint.setAlpha(80);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        itemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        itemPaint.setColor(Color.WHITE);
        itemPaint.setAlpha(60);
        
        choreographer = Choreographer.getInstance();
    }
    
    public void setLoadType(@LoadProfile.LoadType int loadType) {
        this.loadType = loadType;
    }
    
    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            choreographer.postFrameCallback(this);
        }
    }
    
    public void stopAnimation() {
        isAnimating = false;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isAnimating) return;
        
        Trace.beginSection("SecondWindow_doFrame");
        
        // Update animation
        animationProgress += 0.02f;
        if (animationProgress > 1f) animationProgress = 0f;
        
        // Update scroll simulation
        scrollOffset += 2f;
        if (scrollOffset > 100f) scrollOffset = 0f;
        
        // Execute load
        executeLoad();
        
        // Request redraw - this triggers RenderThread drawFrame for this window
        invalidate();
        
        Trace.endSection();
        
        // Schedule next frame
        choreographer.postFrameCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        Trace.beginSection("SecondWindow_onDraw");
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw gradient background
        canvas.drawColor(Color.parseColor("#1565C0"));
        
        // Draw animated circles
        float centerX = width / 2f;
        float centerY = height / 3f;
        float radius = 60 + 40 * (float) Math.sin(animationProgress * Math.PI * 2);
        
        for (int i = 0; i < 3; i++) {
            float offsetX = (float) Math.cos(animationProgress * Math.PI * 2 + i * 2.1f) * 80;
            float offsetY = (float) Math.sin(animationProgress * Math.PI * 2 + i * 2.1f) * 80;
            circlePaint.setAlpha(60 + i * 20);
            canvas.drawCircle(centerX + offsetX, centerY + offsetY, radius - i * 15, circlePaint);
        }
        
        // Draw title
        canvas.drawText("Window 2 (Overlay)", centerX, 50, textPaint);
        canvas.drawText(LoadProfile.toLabel(loadType), centerX, 90, textPaint);
        
        // Draw simulated list items
        float itemHeight = 80;
        float startY = height / 2f - scrollOffset;
        
        for (int i = 0; i < ITEM_COUNT; i++) {
            float y = startY + i * (itemHeight + 10);
            if (y > height / 2f - 50 && y < height - 50) {
                RectF rect = new RectF(20, y, width - 20, y + itemHeight);
                canvas.drawRoundRect(rect, 10, 10, itemPaint);
                
                // Draw item content
                textPaint.setTextSize(28);
                canvas.drawText("Item " + (i + 1), centerX, y + itemHeight / 2 + 10, textPaint);
            }
        }
        
        // Draw frame indicator
        textPaint.setTextSize(20);
        canvas.drawText("Frame: " + String.format("%.2f", animationProgress), centerX, height - 30, textPaint);
        
        Trace.endSection();
    }
    
    private void executeLoad() {
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MINIMAL:
                iterations = 0;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT:
            case LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                iterations = 50;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM:
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                iterations = 250;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                iterations = 1000;
                break;
            default:
                iterations = 0;
        }
        
        if (iterations == 0) return;
        
        Trace.beginSection("SecondWindow_executeLoad");
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        Trace.endSection();
    }
}

