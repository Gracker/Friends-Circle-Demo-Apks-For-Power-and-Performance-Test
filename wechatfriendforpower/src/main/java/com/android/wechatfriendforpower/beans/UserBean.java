package com.android.wechatfriendforpower.beans;

/**
 * 用户信息Bean类
 */
public class UserBean {
    private String userId;  // 用户ID (使用String而不是int类型，以匹配测试)
    private String userName; // 用户名
    private String userAvatarUrl; // 用户头像URL

    public UserBean() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }
} 