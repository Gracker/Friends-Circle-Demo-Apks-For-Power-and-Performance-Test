package com.android.wechatfriendforpower.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;

import com.android.wechatfriendforpower.beans.CommentBean;
import com.android.wechatfriendforpower.beans.FriendCircleBean;
import com.android.wechatfriendforpower.beans.OtherInfoBean;
import com.android.wechatfriendforpower.beans.PraiseBean;
import com.android.wechatfriendforpower.beans.UserBean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * PowerDataGenerator工具类的单元测试
 */
public class PowerDataGeneratorTest {

    @Mock
    private Context mockContext;
    
    private PowerDataGenerator dataGenerator;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        dataGenerator = new PowerDataGenerator(mockContext);
    }
    
    @Test
    public void testGenerateFakeFriendCircleData() {
        // 测试生成朋友圈数据
        List<FriendCircleBean> result = dataGenerator.generateFakeFriendCircleData(10);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(10, result.size());
        
        // 验证每个朋友圈项目的有效性
        for (FriendCircleBean item : result) {
            assertNotNull(item);
            validateFriendCircleBean(item);
        }
    }
    
    @Test
    public void testGenerateFakeFriendCircleDataWithZeroCount() {
        // 测试count=0的情况
        List<FriendCircleBean> result = dataGenerator.generateFakeFriendCircleData(0);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    public void testGenerateFakeFriendCircleDataWithNegativeCount() {
        // 测试count为负的情况
        List<FriendCircleBean> result = dataGenerator.generateFakeFriendCircleData(-5);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    public void testGenerateUser() {
        // 测试生成用户数据
        UserBean result = dataGenerator.generateUser();
        
        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getUserId());
        assertNotNull(result.getUserName());
        assertNotNull(result.getUserAvatarUrl());
    }
    
    @Test
    public void testGeneratePraises() {
        // 测试生成点赞数据
        List<PraiseBean> result = dataGenerator.generatePraises(5);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 验证每个点赞项目的有效性
        for (PraiseBean item : result) {
            assertNotNull(item);
            assertNotNull(item.getPraiseUserName());
            assertNotNull(item.getPraiseUserId());
        }
    }
    
    @Test
    public void testGenerateComments() {
        // 测试生成评论数据
        List<CommentBean> result = dataGenerator.generateComments(5);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 验证每个评论项目的有效性
        for (CommentBean item : result) {
            assertNotNull(item);
            assertNotNull(item.getContent());
            
            // 可能是普通评论或回复评论
            if (item.getChildUserBean() != null && item.getParentUserBean() != null) {
                // 回复评论
                assertNotNull(item.getChildUserBean().getUserName());
                assertNotNull(item.getParentUserBean().getUserName());
            } else {
                // 普通评论
                assertNotNull(item.getChildUserBean());
                assertNotNull(item.getChildUserBean().getUserName());
            }
        }
    }
    
    @Test
    public void testGenerateImages() {
        // 测试生成图片数据
        List<String> result = dataGenerator.generateImages(9);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(9, result.size());
        
        // 验证每个图片URL的有效性
        for (String url : result) {
            assertNotNull(url);
            assertFalse(url.isEmpty());
        }
    }
    
    @Test
    public void testGenerateOtherInfo() {
        // 测试生成其他信息数据
        OtherInfoBean result = dataGenerator.generateOtherInfo();
        
        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getTime());
        assertNotNull(result.getSource());
    }
    
    @Test
    public void testGenerateContent() {
        // 测试生成朋友圈内容文本
        String result = dataGenerator.generateContent();
        
        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    /**
     * 辅助方法：验证朋友圈数据的有效性
     */
    private void validateFriendCircleBean(FriendCircleBean bean) {
        // 验证用户信息
        assertNotNull(bean.getUserBean());
        assertNotNull(bean.getUserBean().getUserId());
        assertNotNull(bean.getUserBean().getUserName());
        
        // 验证内容
        assertNotNull(bean.getContent());
        
        // 验证其他信息
        assertNotNull(bean.getOtherInfoBean());
        assertNotNull(bean.getOtherInfoBean().getTime());
        assertNotNull(bean.getOtherInfoBean().getSource());
        
        // 验证点赞列表（可为空）
        assertNotNull(bean.getPraiseBeans());
        
        // 验证评论列表（可为空）
        assertNotNull(bean.getCommentBeans());
        
        // 验证图片列表（可为空）
        assertNotNull(bean.getImageUrls());
    }
} 