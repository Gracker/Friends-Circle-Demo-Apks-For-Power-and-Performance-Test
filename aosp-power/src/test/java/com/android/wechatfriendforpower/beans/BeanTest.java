package com.android.wechatfriendforpower.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试数据模型（Bean）类
 */
public class BeanTest {

    @Test
    public void testUserBean() {
        UserBean userBean = new UserBean();
        
        // 测试设置和获取用户ID
        userBean.setUserId("123");
        assertEquals("123", userBean.getUserId());
        
        // 测试设置和获取用户名
        userBean.setUserName("测试用户");
        assertEquals("测试用户", userBean.getUserName());
        
        // 测试设置和获取头像URL
        userBean.setUserAvatarUrl("test_avatar_url");
        assertEquals("test_avatar_url", userBean.getUserAvatarUrl());
    }
    
    @Test
    public void testPraiseBean() {
        PraiseBean praiseBean = new PraiseBean();
        
        // 测试设置和获取点赞用户名
        praiseBean.setPraiseUserName("测试点赞用户");
        assertEquals("测试点赞用户", praiseBean.getPraiseUserName());
        
        // 测试设置和获取点赞用户ID
        praiseBean.setPraiseUserId("test_praise_id");
        assertEquals("test_praise_id", praiseBean.getPraiseUserId());
    }
    
    @Test
    public void testCommentBean() {
        CommentBean commentBean = new CommentBean();
        
        // 测试设置和获取评论内容
        commentBean.setCommentContent("测试评论内容");
        assertEquals("测试评论内容", commentBean.getCommentContent());
        
        // 测试设置和获取子用户名
        commentBean.setChildUserName("子用户名");
        assertEquals("子用户名", commentBean.getChildUserName());
        
        // 测试设置和获取父用户名
        commentBean.setParentUserName("父用户名");
        assertEquals("父用户名", commentBean.getParentUserName());
        
        // 测试设置和获取用户Bean
        UserBean childUserBean = new UserBean();
        childUserBean.setUserName("子用户");
        commentBean.setChildUserBean(childUserBean);
        assertNotNull(commentBean.getChildUserBean());
        assertEquals("子用户", commentBean.getChildUserBean().getUserName());
        
        // 测试设置和获取父用户Bean
        UserBean parentUserBean = new UserBean();
        parentUserBean.setUserName("父用户");
        commentBean.setParentUserBean(parentUserBean);
        assertNotNull(commentBean.getParentUserBean());
        assertEquals("父用户", commentBean.getParentUserBean().getUserName());
    }
    
    @Test
    public void testOtherInfoBean() {
        OtherInfoBean otherInfoBean = new OtherInfoBean();
        
        // 测试设置和获取时间
        otherInfoBean.setTime("10分钟前");
        assertEquals("10分钟前", otherInfoBean.getTime());
        
        // 测试设置和获取来源
        otherInfoBean.setSource("来自Android客户端");
        assertEquals("来自Android客户端", otherInfoBean.getSource());
        
        // 测试设置和获取位置
        otherInfoBean.setLocation("北京市");
        assertEquals("北京市", otherInfoBean.getLocation());
    }
    
    @Test
    public void testFriendCircleBean() {
        FriendCircleBean friendCircleBean = new FriendCircleBean();
        
        // 测试设置和获取内容
        friendCircleBean.setContent("测试朋友圈内容");
        assertEquals("测试朋友圈内容", friendCircleBean.getContent());
        
        // 测试设置和获取UserBean
        UserBean userBean = new UserBean();
        userBean.setUserName("测试用户");
        friendCircleBean.setUserBean(userBean);
        assertNotNull(friendCircleBean.getUserBean());
        assertEquals("测试用户", friendCircleBean.getUserBean().getUserName());
        
        // 测试设置和获取OtherInfoBean
        OtherInfoBean otherInfoBean = new OtherInfoBean();
        otherInfoBean.setTime("5分钟前");
        friendCircleBean.setOtherInfoBean(otherInfoBean);
        assertNotNull(friendCircleBean.getOtherInfoBean());
        assertEquals("5分钟前", friendCircleBean.getOtherInfoBean().getTime());
        
        // 测试设置和获取点赞列表
        List<PraiseBean> praiseBeans = new ArrayList<>();
        PraiseBean praiseBean = new PraiseBean();
        praiseBean.setPraiseUserName("点赞用户");
        praiseBeans.add(praiseBean);
        friendCircleBean.setPraiseBeans(praiseBeans);
        assertNotNull(friendCircleBean.getPraiseBeans());
        assertEquals(1, friendCircleBean.getPraiseBeans().size());
        assertEquals("点赞用户", friendCircleBean.getPraiseBeans().get(0).getPraiseUserName());
        
        // 测试设置和获取评论列表
        List<CommentBean> commentBeans = new ArrayList<>();
        CommentBean commentBean = new CommentBean();
        commentBean.setCommentContent("测试评论");
        commentBeans.add(commentBean);
        friendCircleBean.setCommentBeans(commentBeans);
        assertNotNull(friendCircleBean.getCommentBeans());
        assertEquals(1, friendCircleBean.getCommentBeans().size());
        assertEquals("测试评论", friendCircleBean.getCommentBeans().get(0).getCommentContent());
        
        // 测试设置和获取图片URL列表
        List<String> imageUrls = new ArrayList<>();
        imageUrls.add("test_image_url");
        friendCircleBean.setImageUrls(imageUrls);
        assertNotNull(friendCircleBean.getImageUrls());
        assertEquals(1, friendCircleBean.getImageUrls().size());
        assertEquals("test_image_url", friendCircleBean.getImageUrls().get(0));
        
        // 测试显示控制标志
        friendCircleBean.setShowComment(true);
        assertTrue(friendCircleBean.isShowComment());
        
        friendCircleBean.setShowPraise(true);
        assertTrue(friendCircleBean.isShowPraise());
        
        friendCircleBean.setShowCheckAll(true);
        assertTrue(friendCircleBean.isShowCheckAll());
        
        friendCircleBean.setShowCheckAll(false);
        assertFalse(friendCircleBean.isShowCheckAll());
    }
} 