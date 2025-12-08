package com.example.wechatfriendforperformance.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.wechatfriendforperformance.R;
import com.example.wechatfriendforperformance.beans.PraiseBean;
import com.example.wechatfriendforperformance.beans.UserBean;

import java.util.List;

/**
 * 性能测试专用的富文本工具类
 */
public class PerformanceSpanUtils {

    private static final String TAG = "PerformanceSpanUtils";

    /**
     * 生成点赞文本
     */
    public static SpannableStringBuilder makePraiseSpan(Context context, List<PraiseBean> praiseBeans) {
        if (praiseBeans == null || praiseBeans.isEmpty()) {
            return new SpannableStringBuilder();
        }
        
        SpannableStringBuilder builder = new SpannableStringBuilder();
        // 获取praise_item_text颜色资源
        int praiseTextColor = ContextCompat.getColor(context, R.color.praise_item_text);
        
        for (int i = 0; i < praiseBeans.size(); i++) {
            PraiseBean praiseBean = praiseBeans.get(i);
            
            // 获取用户名
            String userName;
            if (praiseBean.getUserBean() != null && !TextUtils.isEmpty(praiseBean.getUserBean().getUserName())) {
                userName = praiseBean.getUserBean().getUserName();
            } else if (!TextUtils.isEmpty(praiseBean.getPraiseUserName())) {
                userName = praiseBean.getPraiseUserName();
            } else {
                userName = "用户" + i;
            }
            
            // 添加用户名
            int start = builder.length();
            builder.append(userName);
            int end = builder.length();
            
            // 设置用户名颜色为praise_item_text
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(praiseTextColor);
            builder.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // 添加分隔符
            if (i < praiseBeans.size() - 1) {
                builder.append("，");
            }
        }
        
        return builder;
    }
    
    /**
     * 生成单条评论文本
     */
    public static SpannableStringBuilder makeSingleCommentSpan(Context context, String userName, String content) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        // 获取praise_item_text颜色资源
        int praiseTextColor = ContextCompat.getColor(context, R.color.praise_item_text);
        
        // 添加用户名
        int start = builder.length();
        builder.append(userName);
        int end = builder.length();
        
        // 设置用户名颜色为praise_item_text
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(praiseTextColor);
        builder.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // 添加评论内容
        builder.append("：").append(content);
        
        return builder;
    }
    
    /**
     * 生成回复评论文本
     */
    public static SpannableStringBuilder makeReplyCommentSpan(Context context, String fromUserName, String toUserName, String content) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        // 获取praise_item_text颜色资源
        int praiseTextColor = ContextCompat.getColor(context, R.color.praise_item_text);
        
        // 添加评论者用户名
        int start = builder.length();
        builder.append(fromUserName);
        int end = builder.length();
        
        // 设置评论者用户名颜色为praise_item_text
        ForegroundColorSpan fromColorSpan = new ForegroundColorSpan(praiseTextColor);
        builder.setSpan(fromColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // 添加回复文本
        builder.append(" 回复 ");
        
        // 添加被回复者用户名
        start = builder.length();
        builder.append(toUserName);
        end = builder.length();
        
        // 设置被回复者用户名颜色为praise_item_text
        ForegroundColorSpan toColorSpan = new ForegroundColorSpan(praiseTextColor);
        builder.setSpan(toColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // 添加评论内容
        builder.append("：").append(content);
        
        return builder;
    }
} 