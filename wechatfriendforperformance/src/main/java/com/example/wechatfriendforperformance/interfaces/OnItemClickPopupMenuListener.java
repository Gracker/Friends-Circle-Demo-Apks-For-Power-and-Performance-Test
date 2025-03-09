package com.example.wechatfriendforperformance.interfaces;

/**
 * 项目点击弹出菜单监听器接口
 */
public interface OnItemClickPopupMenuListener {
    
    /**
     * 项目点击弹出菜单
     * @param position 数据位置
     * @param itemId 菜单项ID
     */
    void onItemClickPopupMenu(int position, int itemId);
} 