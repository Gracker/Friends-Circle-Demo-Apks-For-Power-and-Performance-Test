package com.android.wechatfriendforpower.interfaces;

/**
 * 弹出菜单点击监听器
 */
public interface OnItemClickPopupMenuListener {
    void onItemClickCopy(int position);
    void onItemClickCollection(int position);
    void onItemClickTranslation(int position);
    void onItemClickHideTranslation(int position);
} 