package com.example.wechatfriendforperformance.beans;

/**
 * 点赞数据模型类
 */
public class PraiseBean {
    
    private String praiseUserId;
    private String praiseUserName;
    
    // Add UserBean field
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
    
    /**
     * Get the user bean
     * @return UserBean
     */
    public UserBean getUserBean() {
        return userBean;
    }
    
    /**
     * Set the user bean
     * @param userBean User bean
     */
    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }
} 