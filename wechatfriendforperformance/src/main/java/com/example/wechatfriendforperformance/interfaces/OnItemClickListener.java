package com.example.wechatfriendforperformance.interfaces;

/**
 * Interface for handling item click events in RecyclerView
 */
public interface OnItemClickListener {
    /**
     * Called when an item in the list is clicked
     * 
     * @param position Position of the clicked item
     */
    void onItemClick(int position);
} 