package com.example.scrolling.common.interfaces;

/**
 * Interface for popup menu item click events
 */
public interface OnItemClickPopupMenuListener {

    /**
     * Called when a popup menu item is clicked
     * @param position Data position
     * @param itemId Menu item ID
     */
    void onItemClickPopupMenu(int position, int itemId);
}
