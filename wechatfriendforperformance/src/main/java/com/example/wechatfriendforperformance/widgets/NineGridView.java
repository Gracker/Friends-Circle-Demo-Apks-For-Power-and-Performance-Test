package com.example.wechatfriendforperformance.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 九宫格图片控件，与原项目保持一致
 */
public class NineGridView extends ViewGroup {

    private static final int MAX_COLUMN_COUNT = 3; // 最大列数
    private static final int ITEM_GAP = 6; // 图片之间的间距，与原项目保持一致
    
    private NineGridAdapter<?> mAdapter;
    private int mColumnCount;
    private int mItemCount;
    private int mItemSize;
    
    public NineGridView(Context context) {
        super(context);
        init(context);
    }

    public NineGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NineGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        // 空实现，由setAdapter时设置
    }
    
    public void setAdapter(NineGridAdapter<?> adapter) {
        if (adapter == null) {
            return;
        }
        
        // 清除旧的视图
        removeAllViews();
        
        mAdapter = adapter;
        mItemCount = adapter.getCount();
        
        // 根据图片数量确定列数
        if (mItemCount == 1) {
            mColumnCount = 1;
        } else if (mItemCount == 2 || mItemCount == 4) {
            mColumnCount = 2;
        } else {
            mColumnCount = Math.min(mItemCount, MAX_COLUMN_COUNT);
        }
        
        // 添加子视图
        for (int i = 0; i < mItemCount; i++) {
            View itemView = adapter.getView(i, null);
            addView(itemView);
        }
        
        // 请求重新布局
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        if (mAdapter == null || mItemCount == 0) {
            setMeasuredDimension(0, 0);
            return;
        }
        
        // 获取控件宽度（不包括padding）
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        
        // 计算每个图片的宽度
        mItemSize = (width - ITEM_GAP * (mColumnCount - 1)) / mColumnCount;
        
        // 计算行数
        int rowCount = (int) Math.ceil(mItemCount * 1.0 / mColumnCount);
        
        // 计算总高度
        int totalHeight = rowCount * mItemSize + (rowCount - 1) * ITEM_GAP;
        
        // 设置新的测量尺寸
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            totalHeight + getPaddingTop() + getPaddingBottom()
        );
        
        // 测量子视图
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int childWidthSpec = MeasureSpec.makeMeasureSpec(mItemSize, MeasureSpec.EXACTLY);
            int childHeightSpec = MeasureSpec.makeMeasureSpec(mItemSize, MeasureSpec.EXACTLY);
            child.measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }
        
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            
            // 计算子视图在网格中的行列位置
            int row = i / mColumnCount;
            int col = i % mColumnCount;
            
            // 计算子视图的位置
            int childLeft = getPaddingLeft() + col * (mItemSize + ITEM_GAP);
            int childTop = getPaddingTop() + row * (mItemSize + ITEM_GAP);
            int childRight = childLeft + mItemSize;
            int childBottom = childTop + mItemSize;
            
            // 布局子视图
            child.layout(childLeft, childTop, childRight, childBottom);
        }
    }
    
    /**
     * dp转px
     */
    private int dpToPx(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
    
    /**
     * 九宫格适配器接口，与原项目保持一致
     */
    public interface NineGridAdapter<T> {
        int getCount();
        T getItem(int position);
        View getView(int position, View itemView);
    }
} 