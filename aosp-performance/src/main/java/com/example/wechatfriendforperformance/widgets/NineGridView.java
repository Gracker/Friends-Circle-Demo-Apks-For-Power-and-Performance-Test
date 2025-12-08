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
            final View itemView = adapter.getView(i, null, this);
            final int position = i;
            
            // Set click listener for each item
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
        
        // Request layout
        requestLayout();
    }
    
    /**
     * Set item click listener
     * @param listener The listener to be notified when an item is clicked
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }
    
    /**
     * Interface definition for a callback to be invoked when an item in this view has been clicked
     */
    public interface OnItemClickListener {
        /**
         * Callback method to be invoked when an item in this view has been clicked
         * @param view The view within the adapter that was clicked
         * @param position The position of the view in the adapter
         */
        void onItemClick(View view, int position);
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
    
    /**
     * Nine Grid Adapter interface for providing views to NineGridView
     */
    public interface NineGridAdapter<T> {
        /**
         * Get the number of items in the adapter
         * 
         * @return Item count
         */
        int getCount();
        
        /**
         * Get the item at the specified position
         * 
         * @param position Position of the item
         * @return The item at the position
         */
        T getItem(int position);
        
        /**
         * Get a View that displays the data at the specified position
         * 
         * @param position Position of the item
         * @param convertView Old view to reuse, if possible
         * @param parent The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the position
         */
        View getView(int position, View convertView, ViewGroup parent);
    }
} 