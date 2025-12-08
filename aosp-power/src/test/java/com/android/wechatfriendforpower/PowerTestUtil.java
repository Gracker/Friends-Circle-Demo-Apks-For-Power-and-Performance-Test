package com.android.wechatfriendforpower;

import android.content.Context;
import android.view.View;

import com.android.wechatfriendforpower.beans.CommentBean;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.beans.OtherInfoBean;
import com.android.wechatfriendforpower.beans.PraiseBean;
import com.android.wechatfriendforpower.beans.UserBean;

import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * 功耗测试模块单元测试工具类
 */
public class PowerTestUtil {

    /**
     * 创建一个模拟的Context对象
     */
    public static Context createMockContext() {
        return Mockito.mock(Context.class);
    }

    /**
     * 创建一个模拟的View对象
     */
    public static View createMockView() {
        return Mockito.mock(View.class);
    }

    /**
     * 创建测试用的UserBean对象
     */
    public static UserBean createTestUserBean() {
        UserBean userBean = new UserBean();
        userBean.setUserId("test_user_id");
        userBean.setUserName("测试用户");
        userBean.setUserAvatarUrl("avatar1");
        return userBean;
    }

    /**
     * 创建测试用的PraiseBean列表
     */
    public static List<PraiseBean> createTestPraiseBeans(int count) {
        List<PraiseBean> praiseBeans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PraiseBean praiseBean = new PraiseBean();
            praiseBean.setPraiseUserName("点赞用户" + i);
            praiseBean.setPraiseUserId("praise_id_" + i);
            praiseBeans.add(praiseBean);
        }
        return praiseBeans;
    }

    /**
     * 创建测试用的CommentBean列表
     */
    public static List<CommentBean> createTestCommentBeans(int count) {
        List<CommentBean> commentBeans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CommentBean commentBean = new CommentBean();
            // 设置评论人信息
            UserBean childUser = new UserBean();
            childUser.setUserId("comment_user_id_" + i);
            childUser.setUserName("评论用户" + i);
            childUser.setUserAvatarUrl("avatar" + (i % 10 + 1));
            commentBean.setChildUserBean(childUser);
            
            // 如果是回复，设置被回复人信息
            if (i % 2 == 1) {
                UserBean parentUser = new UserBean();
                parentUser.setUserId("parent_user_id_" + (i - 1));
                parentUser.setUserName("被回复用户" + (i - 1));
                parentUser.setUserAvatarUrl("avatar" + ((i - 1) % 10 + 1));
                commentBean.setParentUserBean(parentUser);
            }
            
            commentBean.setContent("这是第" + i + "条测试评论");
            commentBeans.add(commentBean);
        }
        return commentBeans;
    }

    /**
     * 创建测试用的图片URL列表
     */
    public static List<String> createTestImageUrls(int count) {
        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            imageUrls.add("local" + (i % 10 + 1));
        }
        return imageUrls;
    }

    /**
     * 创建测试用的OtherInfoBean
     */
    public static OtherInfoBean createTestOtherInfoBean() {
        OtherInfoBean otherInfoBean = new OtherInfoBean();
        otherInfoBean.setTime("10分钟前");
        otherInfoBean.setSource("来自Android客户端");
        otherInfoBean.setLocation("北京市");
        return otherInfoBean;
    }

    /**
     * 创建完整的测试用FriendCircleBean
     */
    public static FriendCircleBean createTestFriendCircleBean() {
        FriendCircleBean friendCircleBean = new FriendCircleBean();
        friendCircleBean.setUserBean(createTestUserBean());
        friendCircleBean.setContent("这是一条测试朋友圈内容，用于单元测试");
        friendCircleBean.setPraiseBeans(createTestPraiseBeans(3));
        friendCircleBean.setCommentBeans(createTestCommentBeans(5));
        friendCircleBean.setImageUrls(createTestImageUrls(4));
        friendCircleBean.setOtherInfoBean(createTestOtherInfoBean());
        return friendCircleBean;
    }
    
    /**
     * 创建多个测试用FriendCircleBean列表
     */
    public static List<FriendCircleBean> createTestFriendCircleBeans(int count) {
        List<FriendCircleBean> beans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            FriendCircleBean bean = createTestFriendCircleBean();
            // 稍微修改一下内容，确保每个Bean不完全相同
            bean.setContent("这是第" + i + "条测试朋友圈内容，用于单元测试");
            beans.add(bean);
        }
        return beans;
    }
} 