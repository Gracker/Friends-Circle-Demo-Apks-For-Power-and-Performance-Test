package com.example.wechatfriendfordouyin.view;

import android.content.Context;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;

/**
 * 自定义视频滚动器，专为抖音风格的全屏视频切换设计
 * 滑动时间与滑动速度相关，范围200ms - 600ms
 */
public class VideoOverScroller {

    // 滑动时间范围
    private static final int MIN_SCROLL_DURATION = 200;  // 最小滑动时间 200ms
    private static final int MAX_SCROLL_DURATION = 600;  // 最大滑动时间 600ms
    
    // 速度阈值
    private static final int MIN_VELOCITY = 500;   // 最小速度阈值
    private static final int MAX_VELOCITY = 8000;  // 最大速度阈值

    private final DecelerateInterpolator mInterpolator;
    
    private int mStartY;
    private int mFinalY;
    private int mCurrY;
    private int mDuration;
    private long mStartTime;
    private boolean mFinished = true;

    public VideoOverScroller(Context context) {
        mInterpolator = new DecelerateInterpolator(1.5f);
    }

    /**
     * 开始滚动到指定位置
     * @param startY 起始位置
     * @param dy 滚动距离
     * @param velocityY 滑动速度（用于计算滑动时间）
     */
    public void startScroll(int startY, int dy, int velocityY) {
        mFinished = false;
        mStartY = startY;
        mFinalY = startY + dy;
        mCurrY = startY;
        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        
        // 根据速度计算滑动时间
        mDuration = calculateDuration(Math.abs(velocityY));
    }

    /**
     * 根据滑动速度计算滑动时间
     * 速度越快，时间越短；速度越慢，时间越长
     * 范围限制在200ms - 600ms之间
     */
    private int calculateDuration(int velocity) {
        if (velocity <= MIN_VELOCITY) {
            return MAX_SCROLL_DURATION;
        }
        if (velocity >= MAX_VELOCITY) {
            return MIN_SCROLL_DURATION;
        }
        
        // 线性插值：速度越快时间越短
        float ratio = (float) (velocity - MIN_VELOCITY) / (MAX_VELOCITY - MIN_VELOCITY);
        int duration = (int) (MAX_SCROLL_DURATION - ratio * (MAX_SCROLL_DURATION - MIN_SCROLL_DURATION));
        
        return Math.max(MIN_SCROLL_DURATION, Math.min(MAX_SCROLL_DURATION, duration));
    }

    /**
     * 使用默认时间滚动
     */
    public void startScroll(int startY, int dy, int duration, boolean useDuration) {
        mFinished = false;
        mStartY = startY;
        mFinalY = startY + dy;
        mCurrY = startY;
        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mDuration = Math.max(MIN_SCROLL_DURATION, Math.min(MAX_SCROLL_DURATION, duration));
    }

    /**
     * 计算当前滚动位置
     * @return 如果滚动还在进行中返回true，否则返回false
     */
    public boolean computeScrollOffset() {
        if (mFinished) {
            return false;
        }

        final int timePassed = (int) (AnimationUtils.currentAnimationTimeMillis() - mStartTime);
        
        if (timePassed >= mDuration) {
            mCurrY = mFinalY;
            mFinished = true;
            return true;
        }

        // 使用减速插值器计算当前位置
        float t = (float) timePassed / mDuration;
        float interpolatedT = mInterpolator.getInterpolation(t);
        
        mCurrY = mStartY + Math.round((mFinalY - mStartY) * interpolatedT);
        
        return true;
    }

    public int getCurrY() {
        return mCurrY;
    }

    public int getFinalY() {
        return mFinalY;
    }

    public boolean isFinished() {
        return mFinished;
    }

    public void forceFinished() {
        mFinished = true;
    }

    public void abortAnimation() {
        mFinished = true;
        mCurrY = mFinalY;
    }

    public int getDuration() {
        return mDuration;
    }
}

