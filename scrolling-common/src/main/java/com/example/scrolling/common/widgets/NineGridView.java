package com.example.scrolling.common.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * NineGridView for displaying images in a grid layout similar to WeChat Moments
 */
public class NineGridView extends ViewGroup {

    private static final int MAX_COLUMN_COUNT = 3;
    private static final int ITEM_GAP = 6;

    private int mColumns;
    private int mRows;
    private int mSingleWidth;
    private int mItemCount;
    private OnItemClickListener mOnItemClickListener;

    public NineGridView(Context context) {
        super(context);
    }

    public NineGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NineGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Set adapter for the grid view
     * @param adapter The adapter to provide images
     */
    public <T> void setAdapter(@NonNull NineGridAdapter<T> adapter) {
        removeAllViews();

        mItemCount = adapter.getCount();
        if (mItemCount == 0) {
            return;
        }

        mColumns = mItemCount == 4 ? 2 : Math.min(MAX_COLUMN_COUNT, mItemCount);
        mRows = (int) Math.ceil(mItemCount * 1.0 / mColumns);

        for (int i = 0; i < mItemCount; i++) {
            final View itemView = adapter.getView(i, null, this);
            final int position = i;

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v, position);
                    }
                }
            });

            addView(itemView);
        }

        requestLayout();
    }

    /**
     * Set item click listener
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    /**
     * Interface for item click callbacks
     */
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mItemCount == 0) {
            setMeasuredDimension(0, 0);
            return;
        }

        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        mSingleWidth = (width - (mColumns - 1) * dpToPx(ITEM_GAP)) / mColumns;
        int height = mRows * mSingleWidth + (mRows - 1) * dpToPx(ITEM_GAP);

        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            measureChildWithMargins(child, mSingleWidth, mSingleWidth);
        }
    }

    protected void measureChildWithMargins(View child, int width, int height) {
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        child.measure(widthSpec, heightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mItemCount == 0) {
            return;
        }

        for (int i = 0; i < mItemCount; i++) {
            int row = i / mColumns;
            int col = i % mColumns;

            View child = getChildAt(i);
            int l = col * (mSingleWidth + dpToPx(ITEM_GAP));
            int t = row * (mSingleWidth + dpToPx(ITEM_GAP));

            child.layout(l, t, l + mSingleWidth, t + mSingleWidth);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Nine Grid Adapter interface for providing views to NineGridView
     */
    public interface NineGridAdapter<T> {
        int getCount();
        T getItem(int position);
        View getView(int position, View convertView, ViewGroup parent);
    }
}
