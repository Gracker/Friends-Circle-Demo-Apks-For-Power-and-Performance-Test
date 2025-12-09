package com.example.wechatfriendforpurerenderthread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.OverScroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadType;

/**
 * Pure RenderThread List View - all rendering happens on a separate thread.
 * UI Thread only handles touch events and passes scroll commands to render thread.
 * 
 * Key characteristics:
 * - SurfaceView provides a separate Surface for rendering
 * - Dedicated render thread handles all drawing operations
 * - UI Thread is kept free from rendering work
 * - Simulates WeChat Moments-style list items
 */
public class PureRenderListView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "PureRenderListView";
    
    // Rendering thread
    private Thread renderThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private SurfaceHolder holder;
    
    // Scroll state (accessed from both threads, needs synchronization)
    private volatile float scrollY = 0;
    private volatile float targetScrollY = 0;
    private OverScroller scroller;
    private GestureDetector gestureDetector;
    
    // Item configuration
    private static final int ITEM_HEIGHT = 300;
    private static final int ITEM_COUNT = 100;
    private static final int AVATAR_SIZE = 80;
    private static final int PADDING = 20;
    
    // Paints
    private Paint itemBgPaint;
    private Paint avatarPaint;
    private Paint namePaint;
    private Paint contentPaint;
    private Paint timePaint;
    private Paint dividerPaint;
    private Paint imagePaint;
    
    // Load simulation
    private int loadType = LoadType.MINIMAL;
    private final Random random = new Random(12345L);
    
    // Data
    private final List<ListItem> items = new ArrayList<>();
    
    // Pre-generated content
    private static final String[] NAMES = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry"};
    private static final String[] CONTENTS = {
            "Just had an amazing breakfast!",
            "Working from home today...",
            "Beautiful sunset tonight!",
            "New project is exciting!",
            "Weekend vibes!",
            "Coffee time ☕",
            "Learning something new!",
            "Great meeting today!"
    };
    private static final String[] TIMES = {"Just now", "5 min ago", "10 min ago", "30 min ago", "1 hour ago", "2 hours ago"};

    public PureRenderListView(Context context) {
        super(context);
        init(context);
    }

    public PureRenderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PureRenderListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        holder = getHolder();
        holder.addCallback(this);
        
        scroller = new OverScroller(context);
        
        // Initialize paints
        itemBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        itemBgPaint.setColor(Color.WHITE);
        
        avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarPaint.setColor(Color.parseColor("#2196F3"));
        
        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.parseColor("#1976D2"));
        namePaint.setTextSize(40);
        namePaint.setFakeBoldText(true);
        
        contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setColor(Color.parseColor("#333333"));
        contentPaint.setTextSize(36);
        
        timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(Color.parseColor("#999999"));
        timePaint.setTextSize(28);
        
        dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#E0E0E0"));
        
        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        // Initialize gesture detector
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                scroller.forceFinished(true);
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                scrollY += distanceY;
                scrollY = Math.max(0, Math.min(scrollY, getMaxScrollY()));
                return true;
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                scroller.fling(
                    0, (int) scrollY,
                    0, (int) -velocityY,
                    0, 0,
                    0, getMaxScrollY()
                );
                return true;
            }
        });
        
        // Generate items
        generateItems();
    }
    
    private void generateItems() {
        Random r = new Random(LoadConfig.DATA_GENERATION_SEED);
        for (int i = 0; i < ITEM_COUNT; i++) {
            ListItem item = new ListItem();
            item.name = NAMES[r.nextInt(NAMES.length)];
            item.content = CONTENTS[r.nextInt(CONTENTS.length)];
            item.time = TIMES[r.nextInt(TIMES.length)];
            item.avatarColor = Color.HSVToColor(new float[]{r.nextFloat() * 360, 0.5f, 0.8f});
            item.hasImage = r.nextFloat() > 0.5f;
            if (item.hasImage) {
                item.imageColor = Color.HSVToColor(new float[]{r.nextFloat() * 360, 0.3f, 0.9f});
            }
            items.add(item);
        }
    }
    
    private int getMaxScrollY() {
        return Math.max(0, ITEM_COUNT * ITEM_HEIGHT - getHeight());
    }
    
    public void setLoadType(@LoadType.Type int loadType) {
        this.loadType = loadType;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Touch events are handled on UI Thread, only passing scroll state to render thread
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning.set(true);
        renderThread = new Thread(this, "PureRenderThread");
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning.set(false);
        try {
            renderThread.join(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping render thread", e);
        }
    }

    @Override
    public void run() {
        Log.i(TAG, "Render thread started - UI Thread is free from rendering");
        
        while (isRunning.get()) {
            long frameStart = System.nanoTime();
            
            // Update scroller on render thread
            if (scroller.computeScrollOffset()) {
                scrollY = scroller.getCurrY();
            }
            
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    Trace.beginSection("PureRenderThread_draw");
                    drawList(canvas);
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
        
        Log.i(TAG, "Render thread stopped");
    }
    
    private void drawList(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        // Clear background
        canvas.drawColor(Color.parseColor("#F5F5F5"));
        
        // Execute load simulation
        executeLoad();
        
        // Calculate visible items
        int firstVisibleItem = (int) (scrollY / ITEM_HEIGHT);
        int lastVisibleItem = (int) ((scrollY + height) / ITEM_HEIGHT) + 1;
        
        firstVisibleItem = Math.max(0, firstVisibleItem);
        lastVisibleItem = Math.min(ITEM_COUNT - 1, lastVisibleItem);
        
        // Draw visible items
        for (int i = firstVisibleItem; i <= lastVisibleItem; i++) {
            float itemTop = i * ITEM_HEIGHT - scrollY;
            drawItem(canvas, items.get(i), 0, itemTop, width, ITEM_HEIGHT);
        }
        
        // Draw header with load info
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRect(0, 0, width, 80, headerPaint);
        
        Paint headerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerTextPaint.setColor(Color.WHITE);
        headerTextPaint.setTextSize(36);
        headerTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Pure RenderThread - " + LoadType.toLabel(loadType), width / 2f, 52, headerTextPaint);
    }
    
    private void drawItem(Canvas canvas, ListItem item, float x, float y, float width, float height) {
        // Item background
        RectF itemRect = new RectF(x + 10, y + 5, x + width - 10, y + height - 5);
        canvas.drawRoundRect(itemRect, 8, 8, itemBgPaint);
        
        // Avatar
        avatarPaint.setColor(item.avatarColor);
        canvas.drawCircle(x + PADDING + AVATAR_SIZE / 2f, y + PADDING + AVATAR_SIZE / 2f, AVATAR_SIZE / 2f, avatarPaint);
        
        // Name
        canvas.drawText(item.name, x + PADDING + AVATAR_SIZE + 20, y + PADDING + 40, namePaint);
        
        // Time
        float timeWidth = timePaint.measureText(item.time);
        canvas.drawText(item.time, x + width - PADDING - 10 - timeWidth, y + PADDING + 40, timePaint);
        
        // Content
        canvas.drawText(item.content, x + PADDING + AVATAR_SIZE + 20, y + PADDING + 90, contentPaint);
        
        // Image placeholder
        if (item.hasImage) {
            imagePaint.setColor(item.imageColor);
            RectF imageRect = new RectF(
                x + PADDING + AVATAR_SIZE + 20,
                y + PADDING + 110,
                x + PADDING + AVATAR_SIZE + 220,
                y + PADDING + 250
            );
            canvas.drawRoundRect(imageRect, 8, 8, imagePaint);
        }
        
        // Divider
        canvas.drawRect(x + PADDING, y + height - 1, x + width - PADDING, y + height, dividerPaint);
    }
    
    private void executeLoad() {
        // 使用统一的 LoadConfig 获取负载强度
        int iterations = LoadConfig.getInFrameIntensity(loadType);
        
        if (iterations == 0) return;
        
        Trace.beginSection("PureRenderThread_executeLoad");
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        Trace.endSection();
    }
    
    private static class ListItem {
        String name;
        String content;
        String time;
        int avatarColor;
        boolean hasImage;
        int imageColor;
    }
}


