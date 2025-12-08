package com.example.wechatfriendforperformance.beans;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.example.wechatfriendforperformance.utils.PerformanceSpanUtils;

/**
 * 评论数据Bean
 */
public class CommentBean {

    private static final String TAG = "CommentBean";

    private UserBean childUserBean;
    private UserBean parentUserBean;
    private String content;
    private String childUserName;
    private String parentUserName;
    private String commentContent;
    private SpannableStringBuilder commentContentSpan;
    private Context mContext;

    public CommentBean() {
    }

    public CommentBean(Context context) {
        this.mContext = context;
    }

    public UserBean getChildUserBean() {
        return childUserBean;
    }

    public void setChildUserBean(UserBean childUserBean) {
        this.childUserBean = childUserBean;
    }

    public UserBean getParentUserBean() {
        return parentUserBean;
    }

    public void setParentUserBean(UserBean parentUserBean) {
        this.parentUserBean = parentUserBean;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
        if (commentContentSpan == null) {
            build();
        }
        return commentContentSpan;
    }

    public void setCommentContentSpan(SpannableStringBuilder commentContentSpan) {
        this.commentContentSpan = commentContentSpan;
    }

    /**
     * 构建评论文本
     */
    public void build() {
        // 如果已经构建过，直接返回
        if (commentContentSpan != null) {
            return;
        }
        
        // 确保 Context 不为空
        if (mContext == null) {
            // 没有 Context 无法创建带颜色的span
            commentContentSpan = new SpannableStringBuilder(
                    TextUtils.isEmpty(childUserName) ? "用户" : childUserName);
            return;
        }
        
        // 获取评论人用户名
        if (TextUtils.isEmpty(childUserName) && childUserBean != null) {
            childUserName = childUserBean.getUserName();
        }
        
        if (TextUtils.isEmpty(childUserName)) {
            childUserName = "用户";
        }
        
        // 获取被评论人用户名
        if (TextUtils.isEmpty(parentUserName) && parentUserBean != null) {
            parentUserName = parentUserBean.getUserName();
        }
        
        // 获取评论内容
        if (TextUtils.isEmpty(commentContent)) {
            commentContent = content;
        }
        
        // 构建评论文本
        if (TextUtils.isEmpty(parentUserName)) {
            // 普通评论
            commentContentSpan = PerformanceSpanUtils.makeSingleCommentSpan(
                    mContext, childUserName, commentContent);
        } else {
            // 回复评论
            commentContentSpan = PerformanceSpanUtils.makeReplyCommentSpan(
                    mContext, childUserName, parentUserName, commentContent);
        }
    }

    /**
     * 构建评论文本
     * @param context Context
     */
    public void build(Context context) {
        this.mContext = context;
        build();
    }
} 