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

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

/**
 * Custom SurfaceView that renders a map-like grid interface.
 * Simulates map tile rendering with scrolling support.
 * Uses LoadConfig for unified load configuration.
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
    private int loadType = LoadType.MINIMAL;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LoadSimulator mLoadSimulator;

    // Long frame state
    private long scrollStartTime = 0;
    private int longFrameTriggerCount = 0;
    private int currentLongFrameIndex = 0;
    private long[] longFrameTriggerTimes;
    private long lastLongFrameTime = 0;
    private boolean isScrolling = false;

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

        // Initialize load simulator
        mLoadSimulator = new LoadSimulator();
    }

    public void setLoadType(@LoadType.Type int loadType) {
        this.loadType = loadType;
    }

    private void stopBackgroundTasks() {
        mainHandler.removeCallbacksAndMessages(null);
        resetLongFrameState();
    }

    public void onScrollStateChanged(boolean scrolling) {
        if (scrolling && !isScrolling) {
            isScrolling = true;
            if (LoadType.isLongFrameLoad(loadType)) {
                startLongFrameCycle();
            }
        } else if (!scrolling && isScrolling) {
            isScrolling = false;
            resetLongFrameState();
        }
    }

    private void startLongFrameCycle() {
        scrollStartTime = System.currentTimeMillis();
        longFrameTriggerCount = LoadConfig.getLongFrameTriggerCount();
        longFrameTriggerTimes = LoadConfig.getLongFrameTriggerTimes(longFrameTriggerCount);
        currentLongFrameIndex = 0;
        lastLongFrameTime = 0;
    }

    private void resetLongFrameState() {
        scrollStartTime = 0;
        currentLongFrameIndex = 0;
        longFrameTriggerTimes = null;
    }

    private void checkAndExecuteLongFrame() {
        if (currentLongFrameIndex >= longFrameTriggerCount || longFrameTriggerTimes == null) return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - scrollStartTime;

        if (elapsedTime >= longFrameTriggerTimes[currentLongFrameIndex]) {
            if (currentTime - lastLongFrameTime >= LoadConfig.LONG_FRAME_MIN_INTERVAL_MS) {
                Trace.beginSection("MapSurface_longFrameLoad_" + (currentLongFrameIndex + 1));
                executeLongFrameLoad();
                Trace.endSection();
                lastLongFrameTime = currentTime;
                currentLongFrameIndex++;
            }
        }

        if (elapsedTime >= LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS) {
            startLongFrameCycle();
        }
    }

    private void executeLongFrameLoad() {
        if (mLoadSimulator != null) {
            mLoadSimulator.executeLongFrameLoad("MapSurface_longFrameLoad");
        }
    }

    /**
     * 执行帧间负载（每帧调用，由 LoadSimulator 统一控制伪随机帧间隔）
     */
    private void executeBetweenFrameLoad() {
        if (mLoadSimulator == null) return;
        if (LoadType.isLongFrameLoad(loadType)) return;

        if (LoadType.isBetweenFramesLoad(loadType)) {
            mLoadSimulator.executePureBetweenFrameLoad(loadType, "MapSurface_betweenFrameLoad");
        } else if (LoadType.isMixedLoad(loadType)) {
            mLoadSimulator.executeBetweenFrameLoad(loadType, "MapSurface_betweenFrameLoad");
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
            // 添加超时限制，防止ANR
            if (renderThread != null) {
                renderThread.join(1000); // 最多等待1秒
                if (renderThread.isAlive()) {
                    Log.w(TAG, "Render thread did not stop in time, interrupting");
                    renderThread.interrupt();
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping render thread", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
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

            // 每帧调用帧间负载（由 LoadSimulator 统一控制伪随机帧间隔）
            executeBetweenFrameLoad();

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

        // Execute long frame load if applicable
        if (LoadType.isLongFrameLoad(loadType) && isScrolling) {
            checkAndExecuteLongFrame();
        }

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
        canvas.drawText(LoadType.toLabel(loadType), width / 2f, 50, textPaint);
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
        // 使用统一的负载中心执行负载
        if (mLoadSimulator != null) {
            mLoadSimulator.executeInFrameLoad(loadType, "MapSurface_doFrameLoad");
        }
    }
}


