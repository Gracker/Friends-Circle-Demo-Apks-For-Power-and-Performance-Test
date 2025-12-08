package com.android.wechatfriendforpower.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * PowerUtils工具类的单元测试
 */
public class PowerUtilsTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private Resources mockResources;
    
    @Mock
    private DisplayMetrics mockDisplayMetrics;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        mockDisplayMetrics.density = 2.0f; // 模拟密度为2.0的屏幕
    }
    
    @Test
    public void testDp2px() {
        // 测试dp转px
        float dpValue = 10.0f;
        int expectedPx = 20; // 10 * 2.0 = 20
        assertEquals(expectedPx, PowerUtils.dp2px(dpValue));
    }
    
    @Test
    public void testCalculateShowCheckAllText() {
        // 测试空字符串或短文本
        assertFalse(PowerUtils.calculateShowCheckAllText(null));
        assertFalse(PowerUtils.calculateShowCheckAllText(""));
        assertFalse(PowerUtils.calculateShowCheckAllText("这是一段短文本"));
        
        // 生成一个超过140字符的长文本
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("测试文本");
        }
        
        // 测试长文本
        assertTrue(PowerUtils.calculateShowCheckAllText(longText.toString()));
    }
    
    @Test
    public void testGetScreenWidth() {
        // 模拟显示metrics
        mockDisplayMetrics.widthPixels = 1080;
        
        // 测试获取屏幕宽度
        assertEquals(1080, PowerUtils.getScreenWidth());
    }
    
    @Test
    public void testGetScreenHeight() {
        // 模拟显示metrics
        mockDisplayMetrics.heightPixels = 1920;
        
        // 测试获取屏幕高度
        assertEquals(1920, PowerUtils.getScreenHeight());
    }
    
    @Test
    public void testCalculateInSampleSize() {
        // 测试计算采样大小
        int reqWidth = 100;
        int reqHeight = 100;
        int width = 400;
        int height = 400;
        
        // 期望的采样率是4，因为原始尺寸是请求尺寸的4倍
        int expectedSampleSize = 4;
        int actualSampleSize = PowerUtils.calculateInSampleSize(width, height, reqWidth, reqHeight);
        
        assertEquals(expectedSampleSize, actualSampleSize);
        
        // 测试另一个例子：宽度需要的采样率比高度的大
        reqWidth = 50;
        reqHeight = 100;
        width = 400;
        height = 200;
        
        // 宽度需要采样率8，高度需要采样率2，应该使用较大的那个
        expectedSampleSize = 8;
        actualSampleSize = PowerUtils.calculateInSampleSize(width, height, reqWidth, reqHeight);
        
        assertEquals(expectedSampleSize, actualSampleSize);
    }
    
    @Test
    public void testFormatTimeString() {
        // 测试时间格式化
        // 测试当前时间
        assertNotNull(PowerUtils.formatTimeString(System.currentTimeMillis()));
        
        // 测试过去时间
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        assertNotNull(PowerUtils.formatTimeString(oneHourAgo));
        
        // 测试未来时间
        long oneHourLater = System.currentTimeMillis() + (60 * 60 * 1000);
        assertNotNull(PowerUtils.formatTimeString(oneHourLater));
    }
} 