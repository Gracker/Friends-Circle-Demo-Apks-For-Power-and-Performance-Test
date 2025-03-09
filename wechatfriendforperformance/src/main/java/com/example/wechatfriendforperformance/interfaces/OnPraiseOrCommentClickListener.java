package com.example.wechatfriendforperformance.interfaces;

/**
 * 点赞和评论点击监听器接口
 */
public interface OnPraiseOrCommentClickListener {
    
    /**
     * 点赞按钮点击
     * @param position 数据位置
     */
    void onPraiseClick(int position);
    
    /**
     * 评论按钮点击
     * @param position 数据位置
     */
    void onCommentClick(int position);
} 