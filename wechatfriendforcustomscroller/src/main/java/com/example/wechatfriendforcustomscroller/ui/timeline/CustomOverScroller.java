package com.example.wechatfriendforcustomscroller.ui.timeline;

import android.content.Context;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;

/**
 * 更接近 AOSP OverScroller 的自定义实现，仅处理纵向滚动但复用了相同的 Spline 物理模型。
 */
public class CustomOverScroller {

    private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private static final float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
    private static final float START_TENSION = 0.5f;
    private static final float END_TENSION = 1.0f;
    private static final float P1 = START_TENSION * INFLEXION;
    private static final float P2 = 1.0f - END_TENSION * (1.0f - INFLEXION);
    private static final int NB_SAMPLES = 100;
    private static final float[] SPLINE_POSITION = new float[NB_SAMPLES + 1];
    private static final float[] SPLINE_TIME = new float[NB_SAMPLES + 1];

    static {
        for (int i = 0; i <= NB_SAMPLES; i++) {
            float t = (float) i / NB_SAMPLES;

            float xMin = 0f;
            float xMax = 1f;
            float x = 0f;
            while (true) {
                x = (xMin + xMax) * 0.5f;
                float coef = 3.0f * x * (1.0f - x);
                float tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x;
                if (Math.abs(tx - t) < 1e-5f) {
                    break;
                }
                if (tx > t) {
                    xMax = x;
                } else {
                    xMin = x;
                }
            }
            SPLINE_POSITION[i] = 3.0f * x * (1.0f - x) * ((1.0f - x) * START_TENSION + x * END_TENSION) + x * x * x;

            float yMin = 0f;
            float yMax = 1f;
            float y = 0f;
            while (true) {
                y = (yMin + yMax) * 0.5f;
                float coef = 3.0f * y * (1.0f - y);
                float ty = coef * ((1.0f - y) * START_TENSION + y * END_TENSION) + y * y * y;
                if (Math.abs(ty - t) < 1e-5f) {
                    break;
                }
                if (ty > t) {
                    yMax = y;
                } else {
                    yMin = y;
                }
            }
            SPLINE_TIME[i] = 3.0f * y * (1.0f - y) * ((1.0f - y) * P1 + y * P2) + y * y * y;
        }
        SPLINE_POSITION[NB_SAMPLES] = SPLINE_TIME[NB_SAMPLES] = 1.0f;
    }

    private final float mPhysicalCoeff;
    private final float mFlingFriction;

    private int mStart;
    private int mFinal;
    private int mMin;
    private int mMax;
    private int mCurr;
    private float mDistance;
    private int mDuration;
    private long mStartTime;
    private boolean mFinished = true;

    public CustomOverScroller(Context context) {
        this(context, ViewConfiguration.getScrollFriction());
    }

    public CustomOverScroller(Context context, float friction) {
        mFlingFriction = friction;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        // 0.84f is empirically derived in AOSP to approximate drag on a screen.
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH * 39.37f * metrics.density * 160f * 0.84f;
    }

    public void fling(int startY, int velocityY, int minY, int maxY) {
        mFinished = false;
        mStart = startY;
        mCurr = startY;
        mMin = minY;
        mMax = maxY;
        mStartTime = AnimationUtils.currentAnimationTimeMillis();

        if (Math.abs(velocityY) < 1) {
            mDuration = 0;
            mDistance = 0;
            mFinal = startY;
            mFinished = true;
            return;
        }

        mDuration = getSplineFlingDuration(velocityY);
        double distance = getSplineFlingDistance(velocityY);
        mFinal = startY + (int) Math.round(Math.signum(velocityY) * distance);
        if (mFinal < mMin) {
            mFinal = mMin;
        } else if (mFinal > mMax) {
            mFinal = mMax;
        }
        mDistance = mFinal - mStart;
        if (mDistance == 0) {
            mFinished = true;
        }
    }

    public boolean computeScrollOffset() {
        if (mFinished) {
            return false;
        }
        final int timePassed = (int) (AnimationUtils.currentAnimationTimeMillis() - mStartTime);
        if (timePassed >= mDuration) {
            mCurr = mFinal;
            mFinished = true;
            return true;
        }

        float t = timePassed / (float) mDuration;
        int index = (int) (NB_SAMPLES * t);
        float distanceCoef = 1f;
        if (index < NB_SAMPLES) {
            float tInf = (float) index / NB_SAMPLES;
            float tSup = (float) (index + 1) / NB_SAMPLES;
            float dInf = SPLINE_POSITION[index];
            float dSup = SPLINE_POSITION[index + 1];
            float alpha = (t - tInf) / (tSup - tInf);
            distanceCoef = dInf + alpha * (dSup - dInf);
        }
        mCurr = mStart + Math.round(distanceCoef * mDistance);
        if (mCurr < mMin) {
            mCurr = mMin;
            mFinished = true;
        } else if (mCurr > mMax) {
            mCurr = mMax;
            mFinished = true;
        }
        return true;
    }

    public int getCurrY() {
        return mCurr;
    }

    public boolean isFinished() {
        return mFinished;
    }

    public void forceFinished() {
        mFinished = true;
    }

    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private int getSplineFlingDuration(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * Math.exp(l / decelMinusOne));
    }

    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }
}

