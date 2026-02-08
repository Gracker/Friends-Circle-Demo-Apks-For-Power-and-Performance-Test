package com.example.scrolling.common.beans;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.example.scrolling.common.utils.SpanUtils;

/**
 * Comment data model class
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
     * Build comment spannable text
     */
    public void build() {
        if (commentContentSpan != null) {
            return;
        }

        if (mContext == null) {
            commentContentSpan = new SpannableStringBuilder(
                    TextUtils.isEmpty(childUserName) ? "用户" : childUserName);
            return;
        }

        if (TextUtils.isEmpty(childUserName) && childUserBean != null) {
            childUserName = childUserBean.getUserName();
        }

        if (TextUtils.isEmpty(childUserName)) {
            childUserName = "用户";
        }

        if (TextUtils.isEmpty(parentUserName) && parentUserBean != null) {
            parentUserName = parentUserBean.getUserName();
        }

        if (TextUtils.isEmpty(commentContent)) {
            commentContent = content;
        }

        if (TextUtils.isEmpty(parentUserName)) {
            commentContentSpan = SpanUtils.makeSingleCommentSpan(
                    mContext, childUserName, commentContent);
        } else {
            commentContentSpan = SpanUtils.makeReplyCommentSpan(
                    mContext, childUserName, parentUserName, commentContent);
        }
    }

    /**
     * Build comment spannable text with context
     * @param context Context
     */
    public void build(Context context) {
        this.mContext = context;
        build();
    }
}
