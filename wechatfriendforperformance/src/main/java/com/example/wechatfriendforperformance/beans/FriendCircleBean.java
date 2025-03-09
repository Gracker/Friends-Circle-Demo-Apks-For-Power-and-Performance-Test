package com.example.wechatfriendforperformance.beans;

import android.text.SpannableStringBuilder;

import java.util.List;

/**
 * 朋友圈数据模型类
 */
public class FriendCircleBean {
    
    private int viewType;
    private String content;
    private UserBean userBean;
    private OtherInfoBean otherInfoBean;
    private List<String> imageUrls;
    private List<CommentBean> commentBeans;
    private List<PraiseBean> praiseBeans;
    private SpannableStringBuilder praiseSpan;
    
    public int getViewType() {
        return viewType;
    }
    
    public void setViewType(int viewType) {
        this.viewType = viewType;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public UserBean getUserBean() {
        return userBean;
    }
    
    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }
    
    public OtherInfoBean getOtherInfoBean() {
        return otherInfoBean;
    }
    
    public void setOtherInfoBean(OtherInfoBean otherInfoBean) {
        this.otherInfoBean = otherInfoBean;
    }
    
    public List<String> getImageUrls() {
        return imageUrls;
    }
    
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    public List<CommentBean> getCommentBeans() {
        return commentBeans;
    }
    
    public void setCommentBeans(List<CommentBean> commentBeans) {
        this.commentBeans = commentBeans;
    }
    
    public List<PraiseBean> getPraiseBeans() {
        return praiseBeans;
    }
    
    public void setPraiseBeans(List<PraiseBean> praiseBeans) {
        this.praiseBeans = praiseBeans;
    }
    
    public SpannableStringBuilder getPraiseSpan() {
        return praiseSpan;
    }
    
    public void setPraiseSpan(SpannableStringBuilder praiseSpan) {
        this.praiseSpan = praiseSpan;
    }
} 