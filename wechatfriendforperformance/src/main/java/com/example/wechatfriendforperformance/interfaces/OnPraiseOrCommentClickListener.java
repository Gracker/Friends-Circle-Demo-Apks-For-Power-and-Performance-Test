package com.example.wechatfriendforperformance.interfaces;

import android.view.View;

/**
 * Interface for handling praise and comment click events
 */
public interface OnPraiseOrCommentClickListener {
    
    /**
     * Called when the praise button is clicked
     * @param position Position of the item
     */
    void onPraiseClick(int position);
    
    /**
     * Called when the comment button is clicked
     * @param view The view that was clicked
     * @param position Position of the item
     */
    void onCommentClick(View view, int position);
} 