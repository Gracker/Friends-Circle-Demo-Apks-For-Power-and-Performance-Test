package com.example.wechatfriendforvideo.beans;

/**
 * 点赞数据模型
 */
public class PraiseBean {
    
    private UserBean userBean;
    private String praiseUserName;
    
    public UserBean getUserBean() {
        return userBean;
    }
    
    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }
    
    public String getPraiseUserName() {
        return praiseUserName;
    }
    
    public void setPraiseUserName(String praiseUserName) {
        this.praiseUserName = praiseUserName;
    }
}

