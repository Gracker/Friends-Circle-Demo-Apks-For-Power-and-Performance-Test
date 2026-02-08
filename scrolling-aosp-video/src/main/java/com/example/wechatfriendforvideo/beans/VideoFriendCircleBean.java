package com.example.wechatfriendforvideo.beans;

import com.example.scrolling.common.beans.FriendCircleBean;

/**
 * Video module's extended FriendCircleBean with video media support
 */
public class VideoFriendCircleBean extends FriendCircleBean {

    public static final int MEDIA_TYPE_IMAGE = 0;
    public static final int MEDIA_TYPE_VIDEO = 1;

    private int mediaType = MEDIA_TYPE_IMAGE;
    private String videoResName;
    private int videoResId;

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
        if (videoResId != 0) {
            this.mediaType = MEDIA_TYPE_VIDEO;
        }
    }
}
