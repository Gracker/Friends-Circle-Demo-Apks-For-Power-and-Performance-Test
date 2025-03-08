package com.android.wechatfriendforpower.interfaces;

/**
 * 点赞或评论点击监听器
 */
public interface OnPraiseOrCommentClickListener {
    void onPraiseClick(int position);
    void onCommentClick(int position);
} 