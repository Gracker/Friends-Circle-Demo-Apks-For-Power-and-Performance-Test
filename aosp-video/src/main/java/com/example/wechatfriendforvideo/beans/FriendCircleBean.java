package com.example.wechatfriendforvideo.beans;

import android.text.SpannableStringBuilder;

import java.util.List;

/**
 * 朋友圈数据模型类
 * 支持图片和视频两种媒体类型
 */
public class FriendCircleBean {

    // 媒体类型常量
    public static final int MEDIA_TYPE_IMAGE = 0;  // 图片类型
    public static final int MEDIA_TYPE_VIDEO = 1;  // 视频类型

    private int viewType;
    private String content;
    private UserBean userBean;
    private OtherInfoBean otherInfoBean;
    private List<String> imageUrls;
    private List<CommentBean> commentBeans;
    private List<PraiseBean> praiseBeans;
    private SpannableStringBuilder praiseSpan;

    // 视频相关字段
    private int mediaType = MEDIA_TYPE_IMAGE;  // 默认为图片类型
    private String videoResName;                // 视频资源名称（对应raw目录下的文件名，不含扩展名）
    private int videoResId;                     // 视频资源ID

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

    // 视频相关getter/setter
    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public boolean isVideoType() {
        return mediaType == MEDIA_TYPE_VIDEO;
    }

    public String getVideoResName() {
        return videoResName;
    }

    public void setVideoResName(String videoResName) {
        this.videoResName = videoResName;
    }

    public int getVideoResId() {
        return videoResId;
    }

    public void setVideoResId(int videoResId) {
        this.videoResId = videoResId;
        // 设置视频资源ID时自动切换为视频类型
        if (videoResId != 0) {
            this.mediaType = MEDIA_TYPE_VIDEO;
        }
    }
}

