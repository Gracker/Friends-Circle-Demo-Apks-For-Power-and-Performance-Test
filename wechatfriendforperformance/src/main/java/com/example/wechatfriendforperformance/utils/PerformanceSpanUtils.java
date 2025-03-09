package com.example.wechatfriendforperformance.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.example.wechatfriendforperformance.PerformanceConstants;
import com.example.wechatfriendforperformance.beans.PraiseBean;

import java.util.List;

/**
 * 文本样式工具类
 */
public class PerformanceSpanUtils {

    private static final String TAG = "PerformanceSpanUtils";

    /**
     * 创建点赞文本样式
     * @param context 上下文
     * @param praiseBeans 点赞数据列表
     * @return 带样式的文本
     */
    public static SpannableStringBuilder makePraiseSpan(Context context, List<PraiseBean> praiseBeans) {
        if (praiseBeans == null || praiseBeans.isEmpty()) {
            Log.d(TAG, "makePraiseSpan: 点赞列表为空");
            return new SpannableStringBuilder();
        }
        
        Log.d(TAG, "makePraiseSpan: 开始构建点赞文本，点赞数量: " + praiseBeans.size());
        
        SpannableStringBuilder builder = new SpannableStringBuilder();
        
        for (int i = 0; i < praiseBeans.size(); i++) {
            PraiseBean praiseBean = praiseBeans.get(i);
            
            // 获取用户名，优先使用praiseUserName，如果为空则从userBean获取
            String userName = praiseBean.getPraiseUserName();
            if (userName == null || userName.isEmpty()) {
                if (praiseBean.getUserBean() != null) {
                    userName = praiseBean.getUserBean().getUserName();
                    Log.d(TAG, "makePraiseSpan: 从userBean获取用户名: " + userName);
                }
                
                // 如果仍然为空，使用默认名称
                if (userName == null || userName.isEmpty()) {
                    userName = "用户" + i;
                    Log.d(TAG, "makePraiseSpan: 使用默认用户名: " + userName);
                }
            } else {
                Log.d(TAG, "makePraiseSpan: 使用praiseUserName: " + userName);
            }
            
            // 添加用户名
            int start = builder.length();
            builder.append(userName);
            int end = builder.length();
            
            // 设置颜色
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor(PerformanceConstants.BLUE));
            builder.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 添加分隔符
            if (i < praiseBeans.size() - 1) {
                builder.append("，");
            }
        }
        
        Log.d(TAG, "makePraiseSpan: 点赞文本构建完成: " + builder.toString());
        return builder;
    }
} 