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

import com.example.wechatfriendforperformance.adapters.NineImageAdapter;

import java.util.List;

/**
 * NineGridView for displaying images in a grid layout similar to WeChat Moments
 */
public class NineGridView extends ViewGroup {

    private static final int MAX_COLUMN_COUNT = 3; // Maximum number of columns
    private static final int ITEM_GAP = 6; // Gap between images, consistent with the original project
    
    private NineImageAdapter mAdapter;
    private int mColumns;
    private int mRows;
    private int mSingleWidth;
    private int mItemCount;
    
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
    public void setAdapter(@NonNull NineImageAdapter adapter) {
        // Empty implementation, set by adapter time
        this.mAdapter = adapter;
        List<?> imageList = adapter.getImageData();
        if (imageList == null) {
            return;
        }
        
        // Clear old views
        removeAllViews();
        mItemCount = imageList.size();
        if (mItemCount == 0) {
            return;
        }
        
        // Determine number of columns based on image count
        mColumns = mItemCount == 4 ? 2 : Math.min(MAX_COLUMN_COUNT, mItemCount);
        mRows = (int) Math.ceil(mItemCount * 1.0 / mColumns);
        
        // Add child views
        for (int i = 0; i < mItemCount; i++) {
            View itemView = adapter.getView(i, null, this);
            addView(itemView);
        }
        
        // Request layout
        requestLayout();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mItemCount == 0) {
            setMeasuredDimension(0, 0);
            return;
        }
        
        // Get view width (excluding padding)
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        
        // Calculate width for each image
        mSingleWidth = (width - (mColumns - 1) * dpToPx(ITEM_GAP)) / mColumns;
        
        // Calculate number of rows
        int height = mRows * mSingleWidth + (mRows - 1) * dpToPx(ITEM_GAP);
        
        // Set new measurement dimensions
        setMeasuredDimension(width, height);
        
        // Measure child views
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            measureChild(child, mSingleWidth, mSingleWidth);
        }
    }
    
    private void measureChild(View child, int width, int height) {
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
            // Calculate child view's row and column position in the grid
            int row = i / mColumns;
            int col = i % mColumns;
            
            // Calculate child view's position
            View child = getChildAt(i);
            int l = col * (mSingleWidth + dpToPx(ITEM_GAP));
            int t = row * (mSingleWidth + dpToPx(ITEM_GAP));
            
            // Layout child view
            child.layout(l, t, l + mSingleWidth, t + mSingleWidth);
        }
    }
    
    /**
     * Convert dp to px
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
} 