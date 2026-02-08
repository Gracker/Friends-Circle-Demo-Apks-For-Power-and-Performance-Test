package com.example.scrolling.common.beans;

/**
 * Praise (like) data model class
 */
public class PraiseBean {

    private String praiseUserId;
    private String praiseUserName;
    private UserBean userBean;

    public String getPraiseUserId() {
        return praiseUserId;
    }

    public void setPraiseUserId(String praiseUserId) {
        this.praiseUserId = praiseUserId;
    }

    public String getPraiseUserName() {
        return praiseUserName;
    }

    public void setPraiseUserName(String praiseUserName) {
        this.praiseUserName = praiseUserName;
    }

    public UserBean getUserBean() {
        return userBean;
    }

    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }
}
