package com.example.wechatfriendforsurfacemap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.OverScroller;

import java.util.Random;

/**
 * Custom SurfaceView that renders a map-like grid interface.
 * Simulates map tile rendering with scrolling support.
 */
public class MapSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "MapSurfaceView";
    
    // Rendering
    private Thread renderThread;
    private volatile boolean isRunning = false;
    private SurfaceHolder holder;
    
    // Paints
    private Paint gridPaint;
    private Paint roadPaint;
    private Paint waterPaint;
    private Paint parkPaint;
    private Paint buildingPaint;
    private Paint textPaint;
    
    // Scroll state
    private float offsetX = 0;
    private float offsetY = 0;
    private OverScroller scroller;
    private GestureDetector gestureDetector;
    
    // Map configuration
    private static final int GRID_SIZE = 100;
    private static final int TILE_SIZE = 200;
    
    // Load simulation
    private int loadType = LoadProfile.LOAD_TYPE_MINIMAL;
    private final Random random = new Random(12345L);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isBackgroundTaskRunning = false;
    
    // Between-frame task scheduling
    private static final int MIN_TASK_INTERVAL_MS = 16;
    private static final int MAX_TASK_INTERVAL_MS = 83;

    public MapSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public MapSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MapSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        holder = getHolder();
        holder.addCallback(this);
        
        scroller = new OverScroller(context);
        
        // Initialize paints
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#CCCCCC"));
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);
        
        roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        roadPaint.setColor(Color.WHITE);
        roadPaint.setStrokeWidth(8);
        roadPaint.setStyle(Paint.Style.STROKE);
        roadPaint.setStrokeCap(Paint.Cap.ROUND);
        
        waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waterPaint.setColor(Color.parseColor("#A5D6F7"));
        waterPaint.setStyle(Paint.Style.FILL);
        
        parkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        parkPaint.setColor(Color.parseColor("#C8E6C9"));
        parkPaint.setStyle(Paint.Style.FILL);
        
        buildingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buildingPaint.setColor(Color.parseColor("#E0E0E0"));
        buildingPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#666666"));
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // Gesture detector
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                scroller.forceFinished(true);
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                offsetX += distanceX;
                offsetY += distanceY;
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                scroller.fling(
                    (int) offsetX, (int) offsetY,
                    (int) -velocityX, (int) -velocityY,
                    Integer.MIN_VALUE, Integer.MAX_VALUE,
                    Integer.MIN_VALUE, Integer.MAX_VALUE
                );
                return true;
            }
        });
    }
    
    public void setLoadType(@LoadProfile.LoadType int loadType) {
        this.loadType = loadType;
        
        // Start background tasks if needed
        if (LoadProfile.isBetweenFramesLoad(loadType) || LoadProfile.isMixedLoad(loadType)) {
            startBackgroundTasks();
        }
    }
    
    private void startBackgroundTasks() {
        if (isBackgroundTaskRunning) return;
        isBackgroundTaskRunning = true;
        scheduleNextBetweenFrameTask();
    }
    
    private void stopBackgroundTasks() {
        isBackgroundTaskRunning = false;
        mainHandler.removeCallbacksAndMessages(null);
    }
    
    private void scheduleNextBetweenFrameTask() {
        if (!isBackgroundTaskRunning) return;
        
        int intervalMs = MIN_TASK_INTERVAL_MS + random.nextInt(MAX_TASK_INTERVAL_MS - MIN_TASK_INTERVAL_MS);
        
        mainHandler.postDelayed(() -> {
            if (!isBackgroundTaskRunning) return;
            
            Trace.beginSection("MapSurface_betweenFrameLoad");
            executeBetweenFrameLoad();
            Trace.endSection();
            
            scheduleNextBetweenFrameTask();
        }, intervalMs);
    }
    
    private void executeBetweenFrameLoad() {
        int intensity;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                intensity = 200;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                intensity = 400;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                intensity = 800;
                break;
            default:
                return;
        }
        
        double sum = 0;
        for (int i = 1; i <= intensity; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        renderThread = new Thread(this);
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Handle size changes if needed
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        stopBackgroundTasks();
        try {
            renderThread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping render thread", e);
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            // Update scroller
            if (scroller.computeScrollOffset()) {
                offsetX = scroller.getCurrX();
                offsetY = scroller.getCurrY();
            }
            
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    Trace.beginSection("MapSurface_draw");
                    drawMap(canvas);
                    Trace.endSection();
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            
            // Frame rate limiting (~60fps)
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void drawMap(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        // Clear background
        canvas.drawColor(Color.parseColor("#E8E8E8"));
        
        // Calculate visible tile range
        int startTileX = (int) (offsetX / TILE_SIZE) - 1;
        int startTileY = (int) (offsetY / TILE_SIZE) - 1;
        int endTileX = startTileX + (width / TILE_SIZE) + 3;
        int endTileY = startTileY + (height / TILE_SIZE) + 3;
        
        // Execute in-frame load
        executeInFrameLoad();
        
        // Draw tiles
        for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
                float x = tileX * TILE_SIZE - offsetX;
                float y = tileY * TILE_SIZE - offsetY;
                
                drawTile(canvas, tileX, tileY, x, y);
            }
        }
        
        // Draw grid lines
        drawGrid(canvas, width, height);
        
        // Draw load indicator
        textPaint.setTextSize(32);
        canvas.drawText(LoadProfile.toLabel(loadType), width / 2f, 50, textPaint);
    }
    
    private void drawTile(Canvas canvas, int tileX, int tileY, float x, float y) {
        // Use tile coordinates to generate consistent "features"
        int seed = tileX * 1000 + tileY;
        Random tileRandom = new Random(seed);
        
        // Draw some map features based on seed
        int featureType = tileRandom.nextInt(10);
        
        RectF rect = new RectF(x + 10, y + 10, x + TILE_SIZE - 10, y + TILE_SIZE - 10);
        
        if (featureType < 2) {
            // Water
            canvas.drawRoundRect(rect, 20, 20, waterPaint);
        } else if (featureType < 4) {
            // Park
            canvas.drawRoundRect(rect, 20, 20, parkPaint);
        } else if (featureType < 6) {
            // Building blocks
            for (int i = 0; i < 4; i++) {
                float bx = x + 20 + (i % 2) * 80;
                float by = y + 20 + (i / 2) * 80;
                canvas.drawRect(bx, by, bx + 70, by + 70, buildingPaint);
            }
        }
        
        // Draw roads
        if (tileX % 2 == 0) {
            canvas.drawLine(x + TILE_SIZE / 2f, y, x + TILE_SIZE / 2f, y + TILE_SIZE, roadPaint);
        }
        if (tileY % 2 == 0) {
            canvas.drawLine(x, y + TILE_SIZE / 2f, x + TILE_SIZE, y + TILE_SIZE / 2f, roadPaint);
        }
    }
    
    private void drawGrid(Canvas canvas, int width, int height) {
        // Draw faint grid overlay
        for (int x = 0; x < width + GRID_SIZE; x += GRID_SIZE) {
            float drawX = x - (offsetX % GRID_SIZE);
            canvas.drawLine(drawX, 0, drawX, height, gridPaint);
        }
        for (int y = 0; y < height + GRID_SIZE; y += GRID_SIZE) {
            float drawY = y - (offsetY % GRID_SIZE);
            canvas.drawLine(0, drawY, width, drawY, gridPaint);
        }
    }
    
    private void executeInFrameLoad() {
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MINIMAL:
                iterations = 0;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT:
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                iterations = 100;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM:
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                iterations = 500;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                iterations = 2000;
                break;
            default:
                iterations = 0;
        }
        
        if (iterations == 0) return;
        
        Trace.beginSection("MapSurface_inFrameLoad");
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        Trace.endSection();
    }
}


