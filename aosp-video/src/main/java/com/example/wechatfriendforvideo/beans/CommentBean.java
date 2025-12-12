package com.example.wechatfriendforvideo.beans;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.wechatfriendforvideo.R;

/**
 * 评论数据模型
 */
public class CommentBean {

    private UserBean childUserBean;  // 评论发起人
    private UserBean parentUserBean; // 被回复的用户（可为null）
    private String content;          // 评论内容
    private SpannableStringBuilder commentContentSpan; // 格式化后的评论内容
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

    public SpannableStringBuilder getCommentContentSpan() {
        return commentContentSpan;
    }

    public void setCommentContentSpan(SpannableStringBuilder commentContentSpan) {
        this.commentContentSpan = commentContentSpan;
    }

    /**
     * 构建格式化的评论内容
     */
    public void build() {
        build(mContext);
    }

    /**
     * 构建格式化的评论内容
     */
    public void build(Context context) {
        if (context == null) {
            // 如果没有context，创建简单的文本
            commentContentSpan = new SpannableStringBuilder();
            if (childUserBean != null) {
                commentContentSpan.append(childUserBean.getUserName());
                if (parentUserBean != null) {
                    commentContentSpan.append(" 回复 ");
                    commentContentSpan.append(parentUserBean.getUserName());
                }
                commentContentSpan.append(": ");
            }
            commentContentSpan.append(content != null ? content : "");
            return;
        }

        commentContentSpan = new SpannableStringBuilder();

        int praiseTextColor = ContextCompat.getColor(context, R.color.praise_item_text);

        // 添加评论发起人
        if (childUserBean != null) {
            String childName = childUserBean.getUserName();
            int childStart = commentContentSpan.length();
            commentContentSpan.append(childName);
            int childEnd = commentContentSpan.length();

            // 设置点击样式
            commentContentSpan.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // 点击用户名的处理
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(praiseTextColor);
                    ds.setUnderlineText(false);
                }
            }, childStart, childEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 如果有被回复的用户
        if (parentUserBean != null) {
            commentContentSpan.append(" 回复 ");

            String parentName = parentUserBean.getUserName();
            int parentStart = commentContentSpan.length();
            commentContentSpan.append(parentName);
            int parentEnd = commentContentSpan.length();

            // 设置点击样式
            commentContentSpan.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // 点击用户名的处理
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(praiseTextColor);
                    ds.setUnderlineText(false);
                }
            }, parentStart, parentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 添加评论内容
        commentContentSpan.append(": ");
        commentContentSpan.append(content != null ? content : "");
    }
}

