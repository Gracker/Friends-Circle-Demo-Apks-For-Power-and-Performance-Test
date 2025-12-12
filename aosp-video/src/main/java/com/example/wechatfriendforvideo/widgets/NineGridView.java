package com.example.wechatfriendforvideo.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.wechatfriendforvideo.adapters.NineImageAdapter;

/**
 * 九宫格图片显示控件
 */
public class NineGridView extends ViewGroup {

    private static final int MAX_COLUMNS = 3;
    private static final int GAP_DP = 4;

    private int mGap;
    private int mSingleImageSize;
    private int mMultiImageSize;
    private NineImageAdapter mAdapter;

    public NineGridView(Context context) {
        super(context);
        init();
    }

    public NineGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NineGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        mGap = (int) (GAP_DP * density);
        mSingleImageSize = (int) (200 * density);
        mMultiImageSize = (int) (100 * density);
    }

    public void setAdapter(NineImageAdapter adapter) {
        this.mAdapter = adapter;
        updateViews();
    }

    private void updateViews() {
        removeAllViews();

        if (mAdapter == null || mAdapter.getCount() == 0) {
            return;
        }

        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            ImageView imageView = mAdapter.getView(i, null, this);
            addView(imageView);
        }

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int childCount = getChildCount();

        if (childCount == 0) {
            setMeasuredDimension(width, 0);
            return;
        }

        int columns = getColumns(childCount);
        int rows = getRows(childCount, columns);

        int childSize;
        if (childCount == 1) {
            childSize = mSingleImageSize;
        } else {
            childSize = (width - mGap * (columns - 1)) / columns;
        }

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.measure(
                    MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY)
            );
        }

        int height = childSize * rows + mGap * (rows - 1);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }

        int columns = getColumns(childCount);
        int childSize = getChildAt(0).getMeasuredWidth();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int row = i / columns;
            int col = i % columns;

            int left = col * (childSize + mGap);
            int top = row * (childSize + mGap);
            int right = left + childSize;
            int bottom = top + childSize;

            child.layout(left, top, right, bottom);
        }
    }

    private int getColumns(int count) {
        if (count == 1) {
            return 1;
        } else if (count == 2 || count == 4) {
            return 2;
        } else {
            return MAX_COLUMNS;
        }
    }

    private int getRows(int count, int columns) {
        return (int) Math.ceil((double) count / columns);
    }
}

