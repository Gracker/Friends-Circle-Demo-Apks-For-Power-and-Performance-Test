package com.example.wechatfriendforpurerenderthread;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Trace;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.OverScroller;

import androidx.core.content.ContextCompat;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.scrolling.common.beans.FriendCircleBean;
import com.example.scrolling.common.beans.OtherInfoBean;
import com.example.scrolling.common.beans.UserBean;
import com.example.scrolling.common.model.MomentsDataFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pure RenderThread List View - all rendering happens on a separate thread.
 * UI Thread only handles touch events and passes scroll commands to render thread.
 */
public class PureRenderListView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "PureRenderListView";

    // Rendering thread
    private Thread renderThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private SurfaceHolder holder;

    // Scroll state (accessed from both threads)
    private volatile float scrollY = 0;
    private OverScroller scroller;
    private GestureDetector gestureDetector;
    private volatile boolean isFlinging = false;

    // Item configuration
    private static final int ITEM_HEIGHT = 360;
    private static final int ITEM_COUNT = 2700;
    private static final int AVATAR_SIZE = 88;
    private static final int PADDING = 24;

    // Shared color palette
    private int colorBg;
    private int colorItemBg;
    private int colorName;
    private int colorContent;
    private int colorTime;
    private int colorDivider;

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
    private LoadSimulator mLoadSimulator;

    // Data
    private volatile List<ListItem> items = new ArrayList<>();

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
        initColors(context);
        initPaints();
        initGestureDetector(context);

        // Initialize data with default load type.
        generateItems();

        // Initialize load simulator
        mLoadSimulator = new LoadSimulator();
    }

    private void initColors(Context context) {
        colorBg = ContextCompat.getColor(context, com.example.scrolling.common.R.color.base_F2F2F2);
        colorItemBg = ContextCompat.getColor(context, com.example.scrolling.common.R.color.base_FFFFFF);
        colorName = ContextCompat.getColor(context, com.example.scrolling.common.R.color.base_697A9F);
        colorContent = ContextCompat.getColor(context, com.example.scrolling.common.R.color.base_333333);
        colorTime = ContextCompat.getColor(context, com.example.scrolling.common.R.color.base_999999);
        colorDivider = ContextCompat.getColor(context, com.example.scrolling.common.R.color.base_DCDCDC);
    }

    private void initPaints() {
        itemBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        itemBgPaint.setColor(colorItemBg);

        avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(colorName);
        namePaint.setTextSize(36);
        namePaint.setFakeBoldText(true);

        contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setColor(colorContent);
        contentPaint.setTextSize(32);

        timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(colorTime);
        timePaint.setTextSize(26);

        dividerPaint = new Paint();
        dividerPaint.setColor(colorDivider);

        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    private void initGestureDetector(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                isFlinging = false;
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
                isFlinging = !scroller.isFinished();
                return true;
            }
        });
    }

    private void generateItems() {
        List<FriendCircleBean> beans = MomentsDataFactory.create(loadType, ITEM_COUNT);
        List<ListItem> generated = new ArrayList<>(beans.size());

        for (int i = 0; i < beans.size(); i++) {
            FriendCircleBean bean = beans.get(i);
            if (bean == null) {
                continue;
            }

            UserBean userBean = bean.getUserBean();
            OtherInfoBean otherInfoBean = bean.getOtherInfoBean();

            ListItem item = new ListItem();
            item.name = userBean != null && !TextUtils.isEmpty(userBean.getUserName())
                    ? userBean.getUserName() : "微信用户";
            item.content = !TextUtils.isEmpty(bean.getContent()) ? bean.getContent() : "";
            item.time = otherInfoBean != null && !TextUtils.isEmpty(otherInfoBean.getTime())
                    ? otherInfoBean.getTime() : "刚刚";
            item.source = otherInfoBean != null ? otherInfoBean.getSource() : null;
            item.hasImage = bean.getImageUrls() != null && !bean.getImageUrls().isEmpty();
            item.avatarColor = Color.HSVToColor(new float[]{
                    Math.abs((item.name + i).hashCode()) % 360, 0.45f, 0.88f
            });
            if (item.hasImage) {
                item.imageColor = Color.HSVToColor(new float[]{
                        Math.abs((item.content + i).hashCode()) % 360, 0.25f, 0.95f
                });
            }
            generated.add(item);
        }

        items = generated;
        Log.i(TAG, "Generated item count: " + generated.size() + ", loadType=" + LoadType.toLabel(loadType));
        scrollY = Math.max(0, Math.min(scrollY, getMaxScrollY()));
    }

    private int getMaxScrollY() {
        return Math.max(0, items.size() * ITEM_HEIGHT - getHeight());
    }

    public void setLoadType(@LoadType.Type int loadType) {
        if (this.loadType != loadType || items.isEmpty()) {
            this.loadType = loadType;
            generateItems();
        }
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

            if (scroller.computeScrollOffset()) {
                scrollY = scroller.getCurrY();
                isFlinging = true;
            } else if (isFlinging) {
                isFlinging = false;
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

        canvas.drawColor(colorBg);
        executeLoad();

        List<ListItem> currentItems = items;
        int itemCount = currentItems.size();
        if (itemCount <= 0) {
            return;
        }

        int firstVisibleItem = (int) (scrollY / ITEM_HEIGHT);
        int lastVisibleItem = (int) ((scrollY + height) / ITEM_HEIGHT) + 1;

        firstVisibleItem = Math.max(0, firstVisibleItem);
        lastVisibleItem = Math.min(itemCount - 1, lastVisibleItem);

        for (int i = firstVisibleItem; i <= lastVisibleItem; i++) {
            float itemTop = i * ITEM_HEIGHT - scrollY;
            drawItem(canvas, currentItems.get(i), 0, itemTop, width, ITEM_HEIGHT);
        }

        Paint loadTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        loadTextPaint.setColor(Color.parseColor("#66000000"));
        loadTextPaint.setTextSize(24);
        canvas.drawText("Load: " + LoadType.toLabel(loadType), 16, 34, loadTextPaint);
        canvas.drawText("Items: " + itemCount, 16, 66, loadTextPaint);
    }

    private void drawItem(Canvas canvas, ListItem item, float x, float y, float width, float height) {
        RectF itemRect = new RectF(x, y, x + width, y + height);
        canvas.drawRect(itemRect, itemBgPaint);

        avatarPaint.setColor(item.avatarColor);
        float avatarCenterX = x + PADDING + AVATAR_SIZE / 2f;
        float avatarCenterY = y + PADDING + AVATAR_SIZE / 2f;
        canvas.drawCircle(avatarCenterX, avatarCenterY, AVATAR_SIZE / 2f, avatarPaint);

        float contentStartX = x + PADDING + AVATAR_SIZE + 20;
        float contentMaxWidth = width - contentStartX - PADDING;

        canvas.drawText(item.name, contentStartX, y + PADDING + 38, namePaint);

        float contentTop = y + PADDING + 84;
        float contentBottom = drawMultilineText(
                canvas,
                item.content,
                contentStartX,
                contentTop,
                contentPaint,
                contentMaxWidth,
                3
        );

        if (item.hasImage) {
            imagePaint.setColor(item.imageColor);
            RectF imageRect = new RectF(
                    contentStartX,
                    contentBottom + 10,
                    contentStartX + 230,
                    contentBottom + 170
            );
            canvas.drawRoundRect(imageRect, 8, 8, imagePaint);
        }

        float timeY = y + height - 26;
        canvas.drawText(item.time, contentStartX, timeY, timePaint);
        if (!TextUtils.isEmpty(item.source)) {
            float timeWidth = timePaint.measureText(item.time);
            canvas.drawText(item.source, contentStartX + timeWidth + 16, timeY, timePaint);
        }

        canvas.drawRect(x, y + height - 1, x + width, y + height, dividerPaint);
    }

    private float drawMultilineText(
            Canvas canvas,
            String text,
            float x,
            float startY,
            Paint paint,
            float maxWidth,
            int maxLines
    ) {
        if (TextUtils.isEmpty(text)) {
            return startY;
        }

        int start = 0;
        int textLength = text.length();
        float y = startY;
        float lineHeight = paint.getTextSize() + 10;

        for (int line = 0; line < maxLines && start < textLength; line++) {
            int count = paint.breakText(text, start, textLength, true, maxWidth, null);
            if (count <= 0) {
                break;
            }

            int end = start + count;
            boolean isLastLine = (line == maxLines - 1);
            if (isLastLine && end < textLength && count > 1) {
                String ellipsisLine = text.substring(start, end - 1) + "…";
                canvas.drawText(ellipsisLine, x, y, paint);
            } else {
                canvas.drawText(text, start, end, x, y, paint);
            }

            start = end;
            y += lineHeight;
        }

        return y;
    }

    private void executeLoad() {
        if (mLoadSimulator != null && isFlinging) {
            mLoadSimulator.executeInFrameLoad(loadType, "PureRenderThread_doFrameLoad");
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

    private static class ListItem {
        String name;
        String content;
        String time;
        String source;
        int avatarColor;
        boolean hasImage;
        int imageColor;
    }
}
