package com.example.wechatfriendforvideo.beans;

/**
 * 朋友圈其他信息数据模型（时间、来源、位置等）
 */
public class OtherInfoBean {

    private String time;
    private String source;
    private String location;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

