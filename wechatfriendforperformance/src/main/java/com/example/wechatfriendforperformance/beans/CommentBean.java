package com.example.wechatfriendforperformance.beans;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.example.wechatfriendforperformance.PerformanceConstants;

/**
 * 评论数据模型类
 */
public class CommentBean {
    
    private static final String TAG = "CommentBean";
    
    private int commentType;
    private String childUserName;
    private String parentUserName;
    private String commentContent;
    private SpannableStringBuilder commentContentSpan;
    
    // Add UserBean fields for From and To users
    private UserBean fromUserBean;
    private UserBean toUserBean;
    private String content;
    
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
        // 如果还没有创建评论内容样式，则先构建
        if (commentContentSpan == null) {
            Log.d(TAG, "getCommentContentSpan: 评论文本未构建，尝试自动构建");
            build(null);
        }
        return commentContentSpan;
    }
    
    /**
     * Get the from user bean
     */
    public UserBean getFromUserBean() {
        return fromUserBean;
    }
    
    /**
     * Set the from user bean
     */
    public void setFromUserBean(UserBean fromUserBean) {
        this.fromUserBean = fromUserBean;
        // 自动设置childUserName
        if (fromUserBean != null && (childUserName == null || childUserName.isEmpty())) {
            setChildUserName(fromUserBean.getUserName());
        }
    }
    
    /**
     * Get the to user bean
     */
    public UserBean getToUserBean() {
        return toUserBean;
    }
    
    /**
     * Set the to user bean
     */
    public void setToUserBean(UserBean toUserBean) {
        this.toUserBean = toUserBean;
        // 自动设置parentUserName
        if (toUserBean != null && (parentUserName == null || parentUserName.isEmpty())) {
            setParentUserName(toUserBean.getUserName());
        }
    }
    
    /**
     * Get the content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Set the content
     */
    public void setContent(String content) {
        this.content = content;
        // 自动设置commentContent
        if (content != null && (commentContent == null || commentContent.isEmpty())) {
            setCommentContent(content);
        }
    }
    
    /**
     * 构建评论内容样式
     * @param context 上下文
     */
    public void build(Context context) {
        Log.d(TAG, "build: 开始构建评论文本 childUserName=" + childUserName + ", parentUserName=" + 
              parentUserName + ", commentType=" + commentType + ", content=" + 
              (commentContent != null ? commentContent : content));
              
        // 确保必要的字段已设置
        if (childUserName == null || childUserName.isEmpty()) {
            if (fromUserBean != null && fromUserBean.getUserName() != null) {
                childUserName = fromUserBean.getUserName();
                Log.d(TAG, "build: 自动设置childUserName=" + childUserName);
            } else {
                childUserName = "未知用户";
                Log.d(TAG, "build: 使用默认childUserName");
            }
        }
        
        if (commentType == PerformanceConstants.CommentType.COMMENT_TYPE_REPLY && 
            (parentUserName == null || parentUserName.isEmpty())) {
            if (toUserBean != null && toUserBean.getUserName() != null) {
                parentUserName = toUserBean.getUserName();
                Log.d(TAG, "build: 自动设置parentUserName=" + parentUserName);
            } else {
                parentUserName = "未知用户";
                Log.d(TAG, "build: 使用默认parentUserName");
            }
        }
        
        if (commentContent == null || commentContent.isEmpty()) {
            commentContent = content;
            Log.d(TAG, "build: 使用content作为commentContent");
        }
              
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
        builder.append("：").append(commentContent != null ? commentContent : content);
        
        this.commentContentSpan = builder;
        Log.d(TAG, "build: 评论文本构建完成: " + (commentContentSpan != null ? commentContentSpan.toString() : "null"));
    }
} 