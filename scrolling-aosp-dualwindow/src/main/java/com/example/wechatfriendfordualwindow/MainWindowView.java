package com.example.wechatfriendfordualwindow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Trace;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

import java.util.Random;

import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

/**
 * Main window view that renders a scrollable list.
 * Combined with SecondWindowView, creates dual-window rendering.
 */
public class MainWindowView extends View implements Choreographer.FrameCallback {

    private Paint backgroundPaint;
    private Paint itemPaint;
    private Paint avatarPaint;
    private Paint textPaint;
    private Paint subtextPaint;
    private Paint headerPaint;

    private float scrollY = 0;
    private OverScroller scroller;
    private GestureDetector gestureDetector;
    private Choreographer choreographer;
    private boolean isAnimating = false;

    private int loadType = LoadType.MINIMAL;
    private final Random random = new Random(12345L);
    private LoadSimulator mLoadSimulator;

    // Callback for scroll state changes
    public interface ScrollStateListener {
        void onScrollStart();
        void onScrollStop();
    }
    private ScrollStateListener scrollStateListener;

    private static final int ITEM_COUNT = 50;
    private static final int ITEM_HEIGHT = 120;

    private static final String[] NAMES = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry"};
    private static final String[] CONTENTS = {"Working on new features...", "Great day today!", "Meeting at 3pm", "Coffee break ☕"};

    public MainWindowView(Context context) {
        super(context);
        init(context);
    }

    public MainWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scroller = new OverScroller(context);
        choreographer = Choreographer.getInstance();

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#F5F5F5"));

        itemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        itemPaint.setColor(Color.WHITE);

        avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(36);

        subtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtextPaint.setColor(Color.parseColor("#666666"));
        subtextPaint.setTextSize(28);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#4CAF50"));

        // Initialize load simulator
        mLoadSimulator = new LoadSimulator();

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
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                scroller.fling(0, (int) scrollY, 0, (int) -velocityY, 0, 0, 0, getMaxScrollY());
                startAnimation();
                return true;
            }
        });
    }

    private int getMaxScrollY() {
        return Math.max(0, ITEM_COUNT * ITEM_HEIGHT - getHeight() + 100);
    }

    public void setLoadType(@LoadType.Type int loadType) {
        this.loadType = loadType;
    }

    public void setScrollStateListener(ScrollStateListener listener) {
        this.scrollStateListener = listener;
    }

    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            choreographer.postFrameCallback(this);
            if (scrollStateListener != null) {
                scrollStateListener.onScrollStart();
            }
        }
    }

    public void stopAnimation() {
        isAnimating = false;
        if (scrollStateListener != null) {
            scrollStateListener.onScrollStop();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isAnimating) return;

        Trace.beginSection("MainWindow_doFrame");

        if (scroller.computeScrollOffset()) {
            scrollY = scroller.getCurrY();
            executeLoad();
            invalidate();
            choreographer.postFrameCallback(this);
        } else {
            stopAnimation();
        }

        Trace.endSection();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Trace.beginSection("MainWindow_onDraw");

        int width = getWidth();
        int height = getHeight();

        // Background
        canvas.drawColor(Color.parseColor("#F5F5F5"));

        // Header
        canvas.drawRect(0, 0, width, 80, headerPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Window 1 (Main) - " + LoadType.toLabel(loadType), width / 2f, 52, textPaint);
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextAlign(Paint.Align.LEFT);

        // Calculate visible items
        int firstVisible = (int) (scrollY / ITEM_HEIGHT);
        int lastVisible = (int) ((scrollY + height) / ITEM_HEIGHT) + 1;
        firstVisible = Math.max(0, firstVisible);
        lastVisible = Math.min(ITEM_COUNT - 1, lastVisible);

        // Draw items
        Random itemRandom = new Random(LoadConfig.DATA_GENERATION_SEED);
        for (int i = firstVisible; i <= lastVisible; i++) {
            float y = 90 + i * ITEM_HEIGHT - scrollY;
            drawItem(canvas, i, 10, y, width - 20, ITEM_HEIGHT - 10, itemRandom);
        }

        Trace.endSection();
    }

    private void drawItem(Canvas canvas, int index, float x, float y, float width, float height, Random r) {
        // Item background
        RectF rect = new RectF(x, y, x + width, y + height);
        canvas.drawRoundRect(rect, 8, 8, itemPaint);

        // Avatar
        int avatarColor = Color.HSVToColor(new float[]{(index * 37) % 360, 0.5f, 0.8f});
        avatarPaint.setColor(avatarColor);
        canvas.drawCircle(x + 50, y + height / 2, 35, avatarPaint);

        // Name
        String name = NAMES[index % NAMES.length];
        canvas.drawText(name, x + 100, y + 45, textPaint);

        // Content
        String content = CONTENTS[index % CONTENTS.length];
        canvas.drawText(content, x + 100, y + 85, subtextPaint);
    }

    private void executeLoad() {
        // 使用统一的负载中心执行负载
        if (mLoadSimulator != null) {
            mLoadSimulator.executeInFrameLoad(loadType, "MainWindow_doFrameLoad");
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
    }
}

