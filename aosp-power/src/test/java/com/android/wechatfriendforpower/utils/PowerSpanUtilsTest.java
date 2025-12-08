package com.android.wechatfriendforpower.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.android.wechatfriendforpower.PowerTestUtil;
import com.android.wechatfriendforpower.beans.PraiseBean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

/**
 * PowerSpanUtils工具类的单元测试
 */
public class PowerSpanUtilsTest {

    @Mock
    private Context mockContext;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    public void testMakeSingleCommentSpan() {
        // 测试创建单个评论的富文本
        String userName = "测试用户";
        String commentContent = "这是一条测试评论";
        
        SpannableStringBuilder result = PowerSpanUtils.makeSingleCommentSpan(mockContext, userName, commentContent);
        
        // 验证结果
        assertNotNull(result);
        String expectedString = userName + ": " + commentContent;
        assertEquals(expectedString, result.toString());
        
        // 验证样式
        ForegroundColorSpan[] spans = result.getSpans(0, userName.length(), ForegroundColorSpan.class);
        assertEquals(1, spans.length); // 应该有一个颜色样式
    }
    
    @Test
    public void testMakeSingleCommentSpanWithNullOrEmptyUserName() {
        // 测试用户名为null的情况
        String commentContent = "这是一条测试评论";
        
        SpannableStringBuilder result = PowerSpanUtils.makeSingleCommentSpan(mockContext, null, commentContent);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(commentContent, result.toString());
        
        // 测试用户名为空的情况
        result = PowerSpanUtils.makeSingleCommentSpan(mockContext, "", commentContent);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(commentContent, result.toString());
    }
    
    @Test
    public void testMakeSingleCommentSpanWithNullOrEmptyContent() {
        // 测试评论内容为null的情况
        String userName = "测试用户";
        
        SpannableStringBuilder result = PowerSpanUtils.makeSingleCommentSpan(mockContext, userName, null);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(userName + ": ", result.toString());
        
        // 测试评论内容为空的情况
        result = PowerSpanUtils.makeSingleCommentSpan(mockContext, userName, "");
        
        // 验证结果
        assertNotNull(result);
        assertEquals(userName + ": ", result.toString());
    }
    
    @Test
    public void testMakeReplyCommentSpan() {
        // 测试创建回复评论的富文本
        String parentUserName = "被回复用户";
        String childUserName = "回复用户";
        String commentContent = "这是回复内容";
        
        SpannableStringBuilder result = PowerSpanUtils.makeReplyCommentSpan(
                mockContext, parentUserName, childUserName, commentContent);
        
        // 验证结果
        assertNotNull(result);
        String expectedString = childUserName + "回复" + parentUserName + ": " + commentContent;
        assertEquals(expectedString, result.toString());
        
        // 验证样式
        ForegroundColorSpan[] spans = result.getSpans(0, result.length(), ForegroundColorSpan.class);
        assertEquals(2, spans.length); // 应该有两个颜色样式（用户名和被回复用户名）
    }
    
    @Test
    public void testMakePraiseSpan() {
        // 创建测试数据
        List<PraiseBean> praiseBeans = PowerTestUtil.createTestPraiseBeans(3);
        
        // 测试创建点赞列表的富文本
        SpannableStringBuilder result = PowerSpanUtils.makePraiseSpan(mockContext, praiseBeans);
        
        // 验证结果
        assertNotNull(result);
        
        // 验证内容格式：用户1, 用户2, 用户3
        String resultText = result.toString();
        assertEquals("点赞用户0, 点赞用户1, 点赞用户2", resultText);
        
        // 验证样式
        ForegroundColorSpan[] spans = result.getSpans(0, result.length(), ForegroundColorSpan.class);
        assertEquals(3, spans.length); // 应该有3个颜色样式，每个用户名一个
    }
    
    @Test
    public void testMakePraiseSpanWithEmptyList() {
        // 测试空列表
        SpannableStringBuilder result = PowerSpanUtils.makePraiseSpan(mockContext, new ArrayList<>());
        
        // 验证结果
        assertNotNull(result);
        assertEquals("", result.toString());
    }
    
    @Test
    public void testMakePraiseSpanWithNullList() {
        // 测试null列表
        SpannableStringBuilder result = PowerSpanUtils.makePraiseSpan(mockContext, null);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("", result.toString());
    }
} 