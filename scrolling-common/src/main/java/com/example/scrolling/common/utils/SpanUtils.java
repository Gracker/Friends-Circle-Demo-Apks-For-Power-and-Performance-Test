package com.example.scrolling.common.utils;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

import com.example.scrolling.common.R;
import com.example.scrolling.common.beans.PraiseBean;
import com.example.scrolling.common.beans.UserBean;

import java.util.List;

/**
 * Spannable text utility class for friend circle UI
 */
public class SpanUtils {

    private static final String TAG = "SpanUtils";

    /**
     * Build praise (like) spannable text
     */
    public static SpannableStringBuilder makePraiseSpan(Context context, List<PraiseBean> praiseBeans) {
        if (praiseBeans == null || praiseBeans.isEmpty()) {
            return new SpannableStringBuilder();
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();
        int praiseTextColor = ContextCompat.getColor(context, R.color.praise_item_text);

        for (int i = 0; i < praiseBeans.size(); i++) {
            PraiseBean praiseBean = praiseBeans.get(i);

            String userName;
            if (praiseBean.getUserBean() != null && !TextUtils.isEmpty(praiseBean.getUserBean().getUserName())) {
                userName = praiseBean.getUserBean().getUserName();
            } else if (!TextUtils.isEmpty(praiseBean.getPraiseUserName())) {
                userName = praiseBean.getPraiseUserName();
            } else {
                userName = "用户" + i;
            }

            int start = builder.length();
            builder.append(userName);
            int end = builder.length();

            ForegroundColorSpan colorSpan = new ForegroundColorSpan(praiseTextColor);
            builder.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (i < praiseBeans.size() - 1) {
                builder.append("，");
            }
        }

        return builder;
    }

    /**
     * Build single comment spannable text
     */
    public static SpannableStringBuilder makeSingleCommentSpan(Context context, String userName, String content) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int praiseTextColor = ContextCompat.getColor(context, R.color.praise_item_text);

        int start = builder.length();
        builder.append(userName);
        int end = builder.length();

        ForegroundColorSpan colorSpan = new ForegroundColorSpan(praiseTextColor);
        builder.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append("：").append(content);

        return builder;
    }

    /**
     * Build reply comment spannable text
     */
    public static SpannableStringBuilder makeReplyCommentSpan(Context context, String fromUserName, String toUserName, String content) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int praiseTextColor = ContextCompat.getColor(context, R.color.praise_item_text);

        int start = builder.length();
        builder.append(fromUserName);
        int end = builder.length();

        ForegroundColorSpan fromColorSpan = new ForegroundColorSpan(praiseTextColor);
        builder.setSpan(fromColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append(" 回复 ");

        start = builder.length();
        builder.append(toUserName);
        end = builder.length();

        ForegroundColorSpan toColorSpan = new ForegroundColorSpan(praiseTextColor);
        builder.setSpan(toColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append("：").append(content);

        return builder;
    }
}
