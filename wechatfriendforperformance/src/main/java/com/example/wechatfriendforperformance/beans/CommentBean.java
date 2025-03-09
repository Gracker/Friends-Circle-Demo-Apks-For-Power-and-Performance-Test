package com.example.wechatfriendforperformance.beans;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.example.wechatfriendforperformance.PerformanceConstants;

/**
 * 评论数据模型类
 */
public class CommentBean {
    
    private int commentType;
    private String childUserName;
    private String parentUserName;
    private String commentContent;
    private SpannableStringBuilder commentContentSpan;
    
    public int getCommentType() {
        return commentType;
    }
    
    public void setCommentType(int commentType) {
        this.commentType = commentType;
    }
    
    public String getChildUserName() {
        return childUserName;
    }
    
    public void setChildUserName(String childUserName) {
        this.childUserName = childUserName;
    }
    
    public String getParentUserName() {
        return parentUserName;
    }
    
    public void setParentUserName(String parentUserName) {
        this.parentUserName = parentUserName;
    }
    
    public String getCommentContent() {
        return commentContent;
    }
    
    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }
    
    public SpannableStringBuilder getCommentContentSpan() {
        return commentContentSpan;
    }
    
    /**
     * 构建评论内容样式
     * @param context 上下文
     */
    public void build(Context context) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        // 添加评论者用户名
        int start = builder.length();
        builder.append(childUserName);
        int end = builder.length();
        
        // 设置评论者用户名颜色
        ForegroundColorSpan childColorSpan = new ForegroundColorSpan(Color.parseColor(PerformanceConstants.BLUE));
        builder.setSpan(childColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // 如果是回复类型，添加被回复者用户名
        if (commentType == PerformanceConstants.CommentType.COMMENT_TYPE_REPLY && parentUserName != null) {
            builder.append(" 回复 ");
            
            start = builder.length();
            builder.append(parentUserName);
            end = builder.length();
            
            // 设置被回复者用户名颜色
            ForegroundColorSpan parentColorSpan = new ForegroundColorSpan(Color.parseColor(PerformanceConstants.BLUE));
            builder.setSpan(parentColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // 添加评论内容
        builder.append("：").append(commentContent);
        
        this.commentContentSpan = builder;
    }
} 