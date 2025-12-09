package com.example.wechatfriendforcustomscroller.ui.timeline;

import android.content.Context;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.example.wechatfriendforcustomscroller.LoadProfile;
import com.example.wechatfriendforcustomscroller.beans.FriendCircleBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 彻底摆脱RecyclerView/OverScroller的自定义列表容器。
 */
public class CustomTimelineView extends ViewGroup {

    private final List<FriendCircleBean> data = new ArrayList<>();
    private FriendCircleItemRenderer itemRenderer;
    private View headerView;
    private int scrollOffset;
    private int contentHeight;
    private int loadProfile = LoadProfile.LOAD_TYPE_LIGHT;

    private final CustomOverScroller customOverScroller;
    private final Handler loadHandler = new Handler(Looper.getMainLooper());
    private final Runnable flingRunnable = new Runnable() {
        @Override
        public void run() {
            if (customOverScroller.computeScrollOffset()) {
                scrollToOffset(customOverScroller.getCurrY());
                ViewCompat.postOnAnimation(CustomTimelineView.this, this);
            } else {
                stopContinuousLoadIfIdle();
            }
        }
    };
    private final Runnable continuousLoadRunnable = new Runnable() {
        @Override
        public void run() {
            if (!continuousLoadRunning) {
                return;
            }
            LoadStressSimulator.runContinuousLoad(loadProfile);
            loadHandler.postDelayed(this, 16);
        }
    };

    private VelocityTracker velocityTracker;
    private int activePointerId = -1;
    private float lastMotionY;
    private boolean isBeingDragged;
    private final int touchSlop;
    private final int minimumVelocity;
    private final int maximumVelocity;
    private boolean continuousLoadRunning;

    public CustomTimelineView(@NonNull Context context) {
        this(context, null);
    }

    public CustomTimelineView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTimelineView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        customOverScroller = new CustomOverScroller(context);
        setClipToPadding(false);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public void setItemRenderer(FriendCircleItemRenderer renderer) {
        this.itemRenderer = renderer;
        if (!data.isEmpty()) {
            rebuildChildren();
        }
    }

    public void setHeaderView(View view) {
        if (headerView == view) {
            return;
        }
        if (headerView != null) {
            removeView(headerView);
        }
        headerView = view;
        if (headerView != null) {
            if (headerView.getParent() instanceof ViewGroup) {
                ((ViewGroup) headerView.getParent()).removeView(headerView);
            }
            addView(headerView, 0);
        }
    }

    public void setLoadProfile(@LoadProfile.LoadType int loadProfile) {
        this.loadProfile = loadProfile;
        if (loadProfile == LoadProfile.LOAD_TYPE_LIGHT) {
            stopContinuousLoad();
        }
    }

    public void submitData(List<FriendCircleBean> beans, @LoadProfile.LoadType int loadType) {
        loadProfile = loadType;
        if (loadProfile == LoadProfile.LOAD_TYPE_LIGHT) {
            stopContinuousLoad();
        }
        data.clear();
        if (beans != null) {
            data.addAll(beans);
        }
        rebuildChildren();
    }

    private void rebuildChildren() {
        removeAllViews();
        if (headerView != null) {
            addView(headerView);
        }
        if (itemRenderer == null) {
            return;
        }
        for (int i = 0; i < data.size(); i++) {
            View child = itemRenderer.createItemView(this);
            itemRenderer.bind(child, data.get(i), i, loadProfile);
            addView(child);
        }
        scrollOffset = 0;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int total = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            child.measure(childWidthSpec, childHeightSpec);
            total += child.getMeasuredHeight();
        }
        contentHeight = total;
        setMeasuredDimension(width, height);
        clampScrollOffset();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int top = -scrollOffset;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childHeight = child.getMeasuredHeight();
            child.layout(0, top, child.getMeasuredWidth(), top + childHeight);
            top += childHeight;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isBeingDragged = false;
            activePointerId = -1;
            recycleVelocityTracker();
            return false;
        }

        if (action == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastMotionY = ev.getY();
                activePointerId = ev.getPointerId(0);
                isBeingDragged = !customOverScroller.isFinished();
                customOverScroller.forceFinished();
                removeCallbacks(flingRunnable);
                startContinuousLoadIfNeeded();
                initVelocityTracker();
                velocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                final int index = ev.findPointerIndex(activePointerId);
                if (index < 0) {
                    break;
                }
                float y = ev.getY(index);
                float dy = Math.abs(y - lastMotionY);
                if (dy > touchSlop) {
                    isBeingDragged = true;
                    lastMotionY = y;
                    initVelocityTracker();
                    velocityTracker.addMovement(ev);
                    startContinuousLoadIfNeeded();
                    return true;
                }
                break;
        }
        return isBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTracker();
        velocityTracker.addMovement(event);

        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                customOverScroller.forceFinished();
                removeCallbacks(flingRunnable);
                activePointerId = event.getPointerId(0);
                lastMotionY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final int index = event.findPointerIndex(activePointerId);
                if (index < 0) {
                    break;
                }
                float y = event.getY(index);
                float dy = lastMotionY - y;
                if (!isBeingDragged && Math.abs(dy) > touchSlop) {
                    isBeingDragged = true;
                    startContinuousLoadIfNeeded();
                }
                if (isBeingDragged) {
                    scrollByInternal((int) dy);
                    lastMotionY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isBeingDragged) {
                    velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                    float vY = velocityTracker.getYVelocity(activePointerId);
                    if (Math.abs(vY) > minimumVelocity) {
                        startFling((int) -vY);
                    }
                }
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                endDrag();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
        }
        return true;
    }

    private void scrollByInternal(int deltaY) {
        scrollToOffset(scrollOffset + deltaY);
    }

    private void scrollToOffset(int newOffset) {
        int range = getMaxScrollRange();
        if (range <= 0) {
            scrollOffset = 0;
        } else {
            scrollOffset = Math.max(0, Math.min(newOffset, range));
        }
        requestLayout();
        stopContinuousLoadIfIdle();
    }

    private void startFling(int velocityY) {
        if (getChildCount() == 0) {
            return;
        }
        customOverScroller.fling(scrollOffset, velocityY, 0, getMaxScrollRange());
        startContinuousLoadIfNeeded();
        ViewCompat.postOnAnimation(this, flingRunnable);
    }

    private void endDrag() {
        isBeingDragged = false;
        activePointerId = -1;
        recycleVelocityTracker();
        stopContinuousLoadIfIdle();
    }

    private void onSecondaryPointerUp(MotionEvent event) {
        final int pointerIndex = event.getActionIndex();
        final int pointerId = event.getPointerId(pointerIndex);
        if (pointerId == activePointerId) {
            int newIndex = pointerIndex == 0 ? 1 : 0;
            activePointerId = event.getPointerId(newIndex);
            lastMotionY = event.getY(newIndex);
            if (velocityTracker != null) {
                velocityTracker.clear();
            }
        }
    }

    private int getMaxScrollRange() {
        return Math.max(0, contentHeight - getHeight());
    }

    private void clampScrollOffset() {
        int range = getMaxScrollRange();
        if (scrollOffset > range) {
            scrollOffset = range;
        }
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
    }

    private void initVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(flingRunnable);
        customOverScroller.forceFinished();
        recycleVelocityTracker();
        stopContinuousLoad();
    }

    private void startContinuousLoadIfNeeded() {
        if (continuousLoadRunning || loadProfile == LoadProfile.LOAD_TYPE_LIGHT) {
            return;
        }
        continuousLoadRunning = true;
        loadHandler.post(continuousLoadRunnable);
        // 通知 LoadStressSimulator 开始滚动
        LoadStressSimulator.getInstance().onScrollStart();
    }

    private void stopContinuousLoad() {
        if (!continuousLoadRunning) {
            return;
        }
        continuousLoadRunning = false;
        loadHandler.removeCallbacks(continuousLoadRunnable);
        // 通知 LoadStressSimulator 停止滚动
        LoadStressSimulator.getInstance().onScrollStop();
    }

    private void stopContinuousLoadIfIdle() {
        if (!isBeingDragged && customOverScroller.isFinished()) {
            stopContinuousLoad();
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p);
    }
}

