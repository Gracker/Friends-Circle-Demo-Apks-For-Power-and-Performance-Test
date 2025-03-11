package com.android.wechatfriendforpower.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 通用工具类
 */
public class PowerUtils {
    
    private static int screenWidth = 1080; // 默认屏幕宽度
    private static int screenHeight = 1920; // 默认屏幕高度
    private static final int MAX_TEXT_LENGTH = 140; // 展示全文的最大长度
    
    /**
     * 将dp转换为px
     * @param dpValue dp值
     * @return px值
     */
    public static int dp2px(float dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                Resources.getSystem().getDisplayMetrics());
    }
    
    /**
     * 计算是否需要显示"查看全文"
     * @param text 文本内容
     * @return 是否需要显示"查看全文"
     */
    public static boolean calculateShowCheckAllText(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        return text.length() > MAX_TEXT_LENGTH;
    }
    
    /**
     * 获取屏幕宽度
     * @return 屏幕宽度（像素）
     */
    public static int getScreenWidth() {
        return screenWidth;
    }
    
    /**
     * 获取屏幕高度
     * @return 屏幕高度（像素）
     */
    public static int getScreenHeight() {
        return screenHeight;
    }
    
    /**
     * 计算图片的采样大小
     * @param width 原始宽度
     * @param height 原始高度
     * @param reqWidth 目标宽度
     * @param reqHeight 目标高度
     * @return 采样大小
     */
    public static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            // 计算采样大小
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * 格式化时间字符串
     * @param timestamp 时间戳
     * @return 格式化的时间字符串
     */
    public static String formatTimeString(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
} 