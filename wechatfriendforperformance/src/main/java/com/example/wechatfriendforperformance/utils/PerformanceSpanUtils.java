package com.example.wechatfriendforperformance.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.example.wechatfriendforperformance.PerformanceConstants;
import com.example.wechatfriendforperformance.beans.PraiseBean;

import java.util.List;

/**
 * 文本样式工具类
 */
public class PerformanceSpanUtils {

    /**
     * 创建点赞文本样式
     * @param context 上下文
     * @param praiseBeans 点赞数据列表
     * @return 带样式的文本
     */
    public static SpannableStringBuilder makePraiseSpan(Context context, List<PraiseBean> praiseBeans) {
        if (praiseBeans == null || praiseBeans.isEmpty()) {
            return new SpannableStringBuilder();
        }
        
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        for (int i = 0; i < praiseBeans.size(); i++) {
            PraiseBean praiseBean = praiseBeans.get(i);
            
            // 添加用户名
            int start = builder.length();
            builder.append(praiseBean.getPraiseUserName());
            int end = builder.length();
            
            // 设置颜色
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor(PerformanceConstants.BLUE));
            builder.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 添加分隔符
            if (i < praiseBeans.size() - 1) {
                builder.append("，");
            }
        }
        
        return builder;
    }
} 