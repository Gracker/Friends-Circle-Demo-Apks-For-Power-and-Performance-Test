package com.android.wechatfriendforpower.utils;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.android.wechatfriendforpower.PowerConstants;
import com.android.wechatfriendforpower.beans.PraiseBean;

import java.util.List;

/**
 * Power测试专用的富文本工具类
 */
public class PowerSpanUtils {

    /**
     * 创建单条评论的富文本
     */
    public static SpannableStringBuilder makeSingleCommentSpan(Context context, String childUserName, String commentContent) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(childUserName)) {
            builder.append(childUserName);
            builder.setSpan(new ForegroundColorSpan(0xFF576B95), 0, childUserName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(": ");
        }
        if (!TextUtils.isEmpty(commentContent)) {
            builder.append(commentContent);
        }
        return builder;
    }

    /**
     * 创建回复评论的富文本
     */
    public static SpannableStringBuilder makeReplyCommentSpan(Context context, String parentUserName, String childUserName, String commentContent) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int parentUserNameStart = 0;
        int parentUserNameEnd = 0;
        int childUserNameStart = 0;
        int childUserNameEnd = 0;

        if (!TextUtils.isEmpty(childUserName)) {
            childUserNameStart = 0;
            childUserNameEnd = childUserName.length();
            builder.append(childUserName);
            builder.setSpan(new ForegroundColorSpan(0xFF576B95), childUserNameStart, childUserNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (!TextUtils.isEmpty(parentUserName)) {
            builder.append("回复");
            parentUserNameStart = builder.length();
            builder.append(parentUserName);
            parentUserNameEnd = builder.length();
            builder.setSpan(new ForegroundColorSpan(0xFF576B95), parentUserNameStart, parentUserNameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (!TextUtils.isEmpty(commentContent)) {
            builder.append(": ");
            builder.append(commentContent);
        }
        return builder;
    }

    /**
     * 创建点赞列表的富文本
     */
    public static SpannableStringBuilder makePraiseSpan(Context context, List<PraiseBean> praiseBeans) {
        if (praiseBeans == null || praiseBeans.size() <= 0) {
            return new SpannableStringBuilder();
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int start = 0;
        int end = 0;
        for (int i = 0; i < praiseBeans.size(); i++) {
            PraiseBean praiseBean = praiseBeans.get(i);
            String praiseUserName = praiseBean.getPraiseUserName();
            if (!TextUtils.isEmpty(praiseUserName)) {
                if (i > 0) {
                    builder.append(", ");
                }
                start = builder.length();
                builder.append(praiseUserName);
                end = builder.length();
                builder.setSpan(new ForegroundColorSpan(0xFF576B95), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }
} 