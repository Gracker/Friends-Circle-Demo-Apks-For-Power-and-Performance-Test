package com.kcrason.highperformancefriendscircle.span;

import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import com.kcrason.highperformancefriendscircle.R;

import java.lang.ref.WeakReference;

/**
 * @author KCrason
 * @date 2018/4/28
 */
public class TextClickSpan extends ClickableSpan {

    private static final String TAG = "TextClickSpan";
    private WeakReference<Context> mContextRef;

    private String mUserName;
    private boolean mPressed;

    public TextClickSpan(Context context, String userName) {
        this.mContextRef = new WeakReference<>(context);
        this.mUserName = userName;
    }

    public void setPressed(boolean isPressed) {
        this.mPressed = isPressed;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        Context context = mContextRef.get();
        if (context == null) {
            return;
        }
        ds.bgColor = mPressed ? ContextCompat.getColor(context, R.color.base_B5B5B5) : Color.TRANSPARENT;
        ds.setColor(ContextCompat.getColor(context, R.color.base_697A9F));
        ds.setUnderlineText(false);
    }

    @Override
    public void onClick(View widget) {
        Log.d(TAG, "You Click " + mUserName);
    }
}
