package com.android.wechatfriendforpower.beans;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.android.wechatfriendforpower.PowerConstants;
import com.android.wechatfriendforpower.enums.TranslationState;
import com.android.wechatfriendforpower.utils.PowerSpanUtils;

/**
 * 评论信息Bean类
 */
public class CommentBean {

    private int commentType;

    private String parentUserName;

    private String childUserName;

    private int parentUserId;

    private int childUserId;

    private String commentContent;

    private TranslationState translationState = TranslationState.START;

    private UserBean childUserBean; // 发表评论的用户
    private UserBean parentUserBean; // 被回复的用户（如果是回复评论）

    public CommentBean() {
    }

    public void setTranslationState(TranslationState translationState) {
        this.translationState = translationState;
    }

    public TranslationState getTranslationState() {
        return translationState;
    }

    public int getCommentType() {
        return commentType;
    }

    public void setCommentType(int commentType) {
        this.commentType = commentType;
    }

    public String getParentUserName() {
        return parentUserName;
    }

    public void setParentUserName(String parentUserName) {
        this.parentUserName = parentUserName;
    }

    public String getChildUserName() {
        return childUserName;
    }

    public void setChildUserName(String childUserName) {
        this.childUserName = childUserName;
    }

    public int getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(int parentUserId) {
        this.parentUserId = parentUserId;
    }

    public int getChildUserId() {
        return childUserId;
    }

    public void setChildUserId(int childUserId) {
        this.childUserId = childUserId;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    /**
     * 富文本内容
     */
    private SpannableStringBuilder commentContentSpan;

    public SpannableStringBuilder getCommentContentSpan() {
        return commentContentSpan;
    }

    public void build(Context context) {
        if (commentType == PowerConstants.CommentType.COMMENT_TYPE_SINGLE) {
            commentContentSpan = PowerSpanUtils.makeSingleCommentSpan(context, childUserName, commentContent);
        } else {
            commentContentSpan = PowerSpanUtils.makeReplyCommentSpan(context, parentUserName, childUserName, commentContent);
        }
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
        return commentContent;
    }

    public void setContent(String content) {
        this.commentContent = content;
    }
} 