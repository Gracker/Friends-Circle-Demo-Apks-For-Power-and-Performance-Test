package com.android.wechatfriendforpower.beans;

import android.text.SpannableStringBuilder;

import com.android.wechatfriendforpower.PowerUtils;
import com.android.wechatfriendforpower.enums.TranslationState;
import com.example.scrolling.common.beans.FriendCircleBean;

import java.util.List;

/**
 * Power module's extended FriendCircleBean with additional fields
 * for expanded state, comment/praise visibility, and translation.
 */
public class PowerFriendCircleBean extends FriendCircleBean {

    private List<CommentBean> powerCommentBeans;

    private boolean isShowPraise;

    private boolean isExpanded = false;

    private boolean showComment = false;

    private boolean showPraise = false;

    private boolean showCheckAll = false;

    private TranslationState translationState = TranslationState.START;

    private SpannableStringBuilder contentSpan;

    public void setTranslationState(TranslationState translationState) {
        this.translationState = translationState;
    }

    public TranslationState getTranslationState() {
        return translationState;
    }

    public boolean isShowComment() {
        return showComment;
    }

    public void setShowComment(boolean showComment) {
        this.showComment = showComment;
    }

    public boolean isShowPraise() {
        return showPraise;
    }

    public void setShowPraise(boolean showPraise) {
        this.showPraise = showPraise;
    }

    public boolean isShowCheckAll() {
        return showCheckAll;
    }

    public void setShowCheckAll(boolean showCheckAll) {
        this.showCheckAll = showCheckAll;
    }

    public SpannableStringBuilder getContentSpan() {
        return contentSpan;
    }

    public void setContentSpan(SpannableStringBuilder contentSpan) {
        this.contentSpan = contentSpan;
        this.showCheckAll = PowerUtils.calculateShowCheckAllText(contentSpan.toString());
    }

    @Override
    public void setContent(String content) {
        super.setContent(content);
        setContentSpan(new SpannableStringBuilder(content));
    }

    public List<CommentBean> getPowerCommentBeans() {
        return powerCommentBeans;
    }

    public void setPowerCommentBeans(List<CommentBean> commentBeans) {
        showComment = commentBeans != null && commentBeans.size() > 0;
        this.powerCommentBeans = commentBeans;
    }

    @Override
    public void setPraiseBeans(List<com.example.scrolling.common.beans.PraiseBean> praiseBeans) {
        showPraise = praiseBeans != null && praiseBeans.size() > 0;
        super.setPraiseBeans(praiseBeans);
    }
}
