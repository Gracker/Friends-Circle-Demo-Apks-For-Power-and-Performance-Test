package com.example.wechatfriendforpicasso.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.squareup.picasso.Transformation;

/**
 * Picasso Transformation for rounded corners
 * 用于实现圆角图片效果
 */
public class RoundedCornersTransformation implements Transformation {

    private final int radius;
    private final int margin;

    /**
     * Constructor with radius only
     * @param radius Corner radius in pixels
     */
    public RoundedCornersTransformation(int radius) {
        this(radius, 0);
    }

    /**
     * Constructor with radius and margin
     * @param radius Corner radius in pixels
     * @param margin Margin in pixels
     */
    public RoundedCornersTransformation(int radius, int margin) {
        this.radius = radius;
        this.margin = margin;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        RectF rect = new RectF(margin, margin, width - margin, height - margin);
        canvas.drawRoundRect(rect, radius, radius, paint);

        if (source != output) {
            source.recycle();
        }

        return output;
    }

    @Override
    public String key() {
        return "rounded_corners(radius=" + radius + ", margin=" + margin + ")";
    }
}

