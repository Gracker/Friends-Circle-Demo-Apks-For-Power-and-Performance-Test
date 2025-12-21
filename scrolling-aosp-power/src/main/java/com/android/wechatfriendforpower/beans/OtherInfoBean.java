package com.android.wechatfriendforpower.beans;

/**
 * 其他信息Bean类
 */
public class OtherInfoBean {
    private String time; // 时间
    private String source; // 来源
    private String location; // 地理位置

    public OtherInfoBean() {
    }

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