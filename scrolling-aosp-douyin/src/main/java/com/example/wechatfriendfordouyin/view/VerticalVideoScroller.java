package com.example.wechatfriendfordouyin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 抖音风格垂直视频滚动容器
 * 特点：
 * 1. 全屏翻页式滚动，每次只显示一个视频
 * 2. 使用自定义Scroller控制滑动动画
 * 3. 滑动时间与滑动速度相关，范围200ms-600ms
 * 4. 滑动过半自动切换到下一个/上一个视频
 */
public class VerticalVideoScroller extends ViewGroup {
    private static final String TAG = "VerticalVideoScroller";

    // 触摸相关
    private VelocityTracker mVelocityTracker;
    private int mActivePointerId = -1;
    private float mLastMotionY;
    private boolean mIsBeingDragged;
    private final int mTouchSlop;
    private final int mMinimumVelocity;
    private final int mMaximumVelocity;

    // 滚动相关
    private final VideoOverScroller mScroller;
    private int mScrollOffset = 0; // 当前滚动偏移量
    private int mCurrentPage = 0; // 当前页面索引
    private int mPageHeight = 0; // 单页高度（等于View高度）

    // 页面切换监听
    private OnPageChangeListener mPageChangeListener;
    // 滚动监听
    private OnScrollListener mScrollListener;

    // 视频数据
    private final List<VideoItem> mVideoItems = new ArrayList<>();
    private VideoPageAdapter mAdapter;

    // 目标页面（用于滚动结束时的回调）
    private int mTargetPage = 0;
    private boolean mNeedNotifyPageChange = false;

    // 滑动动画Runnable
    private final Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                scrollToOffset(mScroller.getCurrY());
                ViewCompat.postOnAnimation(VerticalVideoScroller.this, this);
            } else {
                // 滚动结束，确保对齐到页面并通知
                mCurrentPage = mTargetPage;
                if (mNeedNotifyPageChange) {
                    mNeedNotifyPageChange = false;
                    notifyPageChanged();
                }
            }
        }
    };

    public VerticalVideoScroller(@NonNull Context context) {
        this(context, null);
    }

    public VerticalVideoScroller(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalVideoScroller(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mScroller = new VideoOverScroller(context);
        setClipToPadding(false);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    /**
     * 设置视频数据适配器
     */
    public void setAdapter(VideoPageAdapter adapter) {
        mAdapter = adapter;
        if (adapter != null) {
            rebuildChildren();
        }
    }

    /**
     * 设置页面切换监听器
     */
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mPageChangeListener = listener;
    }

    /**
     * 设置滚动监听器
     */
    public void setOnScrollListener(OnScrollListener listener) {
        mScrollListener = listener;
    }

    /**
     * 获取当前页面索引
     */
    public int getCurrentPage() {
        return mCurrentPage;
    }

    /**
     * 重新构建子View
     */
    private void rebuildChildren() {
        removeAllViews();
        if (mAdapter == null) {
            return;
        }

        int count = mAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            View child = mAdapter.createView(this, i);
            mAdapter.bindView(child, i);
            addView(child);
        }

        mScrollOffset = 0;
        mCurrentPage = 0;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mPageHeight = height;

        // 每个子View都是全屏大小
        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(childWidthSpec, childHeightSpec);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int top = -mScrollOffset;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(0, top, child.getMeasuredWidth(), top + mPageHeight);
            top += mPageHeight;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsBeingDragged = false;
            mActivePointerId = -1;
            recycleVelocityTracker();
            return false;
        }

        if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = !mScroller.isFinished();
                mScroller.forceFinished();
                removeCallbacks(mScrollRunnable);
                initVelocityTracker();
                mVelocityTracker.addMovement(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                final int index = ev.findPointerIndex(mActivePointerId);
                if (index < 0) {
                    break;
                }
                float y = ev.getY(index);
                float dy = Math.abs(y - mLastMotionY);
                if (dy > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                    initVelocityTracker();
                    mVelocityTracker.addMovement(ev);
                    return true;
                }
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTracker();
        mVelocityTracker.addMovement(event);

        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScroller.forceFinished();
                removeCallbacks(mScrollRunnable);
                mActivePointerId = event.getPointerId(0);
                mLastMotionY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                final int index = event.findPointerIndex(mActivePointerId);
                if (index < 0) {
                    break;
                }
                float y = event.getY(index);
                float dy = mLastMotionY - y;

                if (!mIsBeingDragged && Math.abs(dy) > mTouchSlop) {
                    mIsBeingDragged = true;
                }

                if (mIsBeingDragged) {
                    scrollByInternal((int) dy);
                    mLastMotionY = y;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    float velocityY = mVelocityTracker.getYVelocity(mActivePointerId);
                    handleScrollEnd((int) velocityY);
                }
                endDrag();
                break;

            case MotionEvent.ACTION_CANCEL:
                // 取消时滚动回当前页
                smoothScrollToPage(mCurrentPage, 0);
                endDrag();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;
        }
        return true;
    }

    /**
     * 处理滑动结束，决定切换到哪个页面
     */
    private void handleScrollEnd(int velocityY) {
        if (getChildCount() == 0 || mPageHeight == 0) {
            return;
        }

        int currentPageOffset = mCurrentPage * mPageHeight;
        int deltaFromPage = mScrollOffset - currentPageOffset;
        int targetPage = mCurrentPage;

        // 判断是否需要切换页面
        // 1. 滑动距离超过一半页面高度
        // 2. 或者滑动速度足够快（速度方向与位移方向一致）
        boolean shouldSwitch = false;

        if (Math.abs(deltaFromPage) > mPageHeight / 2) {
            // 滑动超过一半
            shouldSwitch = true;
        } else if (Math.abs(velocityY) > mMinimumVelocity) {
            // 速度足够快
            if (velocityY < 0 && deltaFromPage > 0) {
                // 向上滑（速度负），且已经向下滑了一点
                shouldSwitch = true;
            } else if (velocityY > 0 && deltaFromPage < 0) {
                // 向下滑（速度正），且已经向上滑了一点
                shouldSwitch = true;
            }
        }

        if (shouldSwitch) {
            if (deltaFromPage > 0) {
                // 向下滑，切换到下一页
                targetPage = Math.min(mCurrentPage + 1, getChildCount() - 1);
            } else if (deltaFromPage < 0) {
                // 向上滑，切换到上一页
                targetPage = Math.max(mCurrentPage - 1, 0);
            }
        }

        smoothScrollToPage(targetPage, Math.abs(velocityY));
    }

    /**
     * 平滑滚动到指定页面
     */
    private void smoothScrollToPage(int page, int velocity) {
        int targetOffset = page * mPageHeight;
        int dy = targetOffset - mScrollOffset;

        // 记录是否需要通知页面变化
        mTargetPage = page;
        mNeedNotifyPageChange = (page != mCurrentPage);

        if (dy == 0) {
            // 没有滚动距离，直接通知
            if (mNeedNotifyPageChange) {
                mCurrentPage = page;
                mNeedNotifyPageChange = false;
                notifyPageChanged();
            }
            return;
        }

        mScroller.startScroll(mScrollOffset, dy, velocity);
        ViewCompat.postOnAnimation(this, mScrollRunnable);
    }

    /**
     * 滚动处理
     */
    private void scrollByInternal(int deltaY) {
        int newOffset = mScrollOffset + deltaY;

        // 限制滚动范围
        int maxScroll = Math.max(0, (getChildCount() - 1) * mPageHeight);
        newOffset = Math.max(0, Math.min(newOffset, maxScroll));

        scrollToOffset(newOffset);
    }

    /**
     * 滚动到指定偏移量
     */
    private void scrollToOffset(int offset) {
        if (mScrollOffset != offset) {
            mScrollOffset = offset;
            requestLayout();
            if (mScrollListener != null) {
                mScrollListener.onScrollChanged(offset);
            }
        }
    }

    /**
     * 通知页面切换
     */
    private void notifyPageChanged() {
        Log.d(TAG, "Page changed to: " + mCurrentPage);
        if (mPageChangeListener != null) {
            mPageChangeListener.onPageSelected(mCurrentPage);
        }
    }

    /**
     * 获取最大滚动范围
     */
    private int getMaxScrollRange() {
        return Math.max(0, (getChildCount() - 1) * mPageHeight);
    }

    private void endDrag() {
        mIsBeingDragged = false;
        mActivePointerId = -1;
        recycleVelocityTracker();
    }

    private void onSecondaryPointerUp(MotionEvent event) {
        final int pointerIndex = event.getActionIndex();
        final int pointerId = event.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            int newIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = event.getPointerId(newIndex);
            mLastMotionY = event.getY(newIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void initVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mScrollRunnable);
        mScroller.forceFinished();
        recycleVelocityTracker();
    }

    /**
     * 页面切换监听器接口
     */
    public interface OnPageChangeListener {
        void onPageSelected(int position);
    }

    /**
     * 视频页面适配器接口
     */
    public interface VideoPageAdapter {
        int getItemCount();

        View createView(ViewGroup parent, int position);

        void bindView(View view, int position);
    }

    /**
     * 滚动监听器接口
     */
    public interface OnScrollListener {
        void onScrollChanged(int scrollY);
    }

    /**
     * 视频数据项
     */
    public static class VideoItem {
        public String videoPath;
        public String author;
        public String description;
        public int likeCount;
        public int commentCount;
        public int favoriteCount;
        public int shareCount;

        public VideoItem(String videoPath, String author, String description,
                int likeCount, int commentCount, int favoriteCount, int shareCount) {
            this.videoPath = videoPath;
            this.author = author;
            this.description = description;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
            this.favoriteCount = favoriteCount;
            this.shareCount = shareCount;
        }
    }
}
