package com.example.wechatfriendforrenderstress.ui.timeline;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Trace;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.loadconfig.LoadType;

/**
 * 半透明覆盖层：UI 线程只做轻量绘制，而通过 RenderEffect / Shader 在 RenderThread 端制造繁重工作。
 */
public class RenderStressOverlayView extends View implements Choreographer.FrameCallback {

    private static final int MEDIUM_LAYER_REPEATS = 3;
    private static final int HEAVY_LAYER_REPEATS = 6;

    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tempRect = new RectF();
    private final Path wavePath = new Path();
    private final Path accentPath = new Path();
    private final Choreographer choreographer = Choreographer.getInstance();

    private boolean running;
    private float phase;
    private @LoadType.Type int currentLoad = LoadType.LIGHT;
    private ValueAnimator legacyAnimator;

    public RenderStressOverlayView(Context context) {
        super(context);
        init();
    }

    public RenderStressOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RenderStressOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        overlayPaint.setStyle(Paint.Style.FILL);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(8f);
        accentPaint.setColor(Color.argb(120, 255, 255, 255));
        accentPaint.setPathEffect(new ComposePathEffect(new CornerPathEffect(24f), new CornerPathEffect(12f)));
    }

    public void start(@LoadType.Type int loadType) {
        if (loadType == LoadType.LIGHT) {
            stop();
            return;
        }
        currentLoad = loadType;
        if (running) {
            return;
        }
        running = true;
        phase = 0f;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ensureLegacyAnimator(loadType);
        }
        choreographer.postFrameCallback(this);
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        choreographer.removeFrameCallback(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(null);
        }
        if (legacyAnimator != null) {
            legacyAnimator.cancel();
        }
        invalidate();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        phase += currentLoad == LoadType.MEDIUM ? 0.035f : 0.08f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyRenderEffect();
        }
        invalidate();
        choreographer.postFrameCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!running) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Trace.beginSection("RenderStressOverlay_draw");
        int repeats = currentLoad == LoadType.MEDIUM ? MEDIUM_LAYER_REPEATS : HEAVY_LAYER_REPEATS;
        for (int layer = 0; layer < repeats; layer++) {
            float layerAlpha = (layer + 1f) / repeats;
            overlayPaint.setShader(new LinearGradient(
                    0,
                    0,
                    width,
                    height,
                    new int[]{
                            Color.argb((int) (20 + 40 * layerAlpha), 255, 255 - layer * 10, 255),
                            Color.argb((int) (60 + 70 * layerAlpha), 120, 200 - layer * 20, 255),
                            Color.argb((int) (90 + 100 * layerAlpha), 40 + layer * 10, 60 + layer * 5, 180)
                    },
                    null,
                    Shader.TileMode.MIRROR
            ));
            canvas.saveLayer(0, 0, width, height, null);
            canvas.drawRect(0, 0, width, height, overlayPaint);
            drawWaveBand(canvas, width, height, layerAlpha);
            drawAccentArcs(canvas, width, height, layerAlpha);
            canvas.restore();
        }
        Trace.endSection();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void applyRenderEffect() {
        float baseBlur = currentLoad == LoadType.MEDIUM ? 90f : 210f;
        float oscillation = (float) Math.abs(Math.sin(phase * 3f)) * (currentLoad == LoadType.MEDIUM ? 30f : 90f);
        float blurRadius = baseBlur + oscillation;
        android.graphics.RenderEffect blurA = android.graphics.RenderEffect.createBlurEffect(
                blurRadius,
                blurRadius * 0.9f,
                Shader.TileMode.MIRROR
        );
        android.graphics.RenderEffect blurB = android.graphics.RenderEffect.createBlurEffect(
                blurRadius * 0.4f,
                blurRadius * 0.6f,
                Shader.TileMode.CLAMP
        );
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        if (currentLoad == LoadType.MEDIUM) {
            cm.setScale(1.05f, 0.95f, 1.1f, 0.85f);
        } else {
            cm.setRotate(0, 12f);
            cm.setScale(1.2f, 0.9f, 1.3f, 0.75f);
        }
        android.graphics.RenderEffect colorEffect = android.graphics.RenderEffect.createColorFilterEffect(
                new android.graphics.ColorMatrixColorFilter(cm),
                android.graphics.RenderEffect.createColorFilterEffect(
                        new android.graphics.ColorMatrixColorFilter(new android.graphics.ColorMatrix(new float[]{
                                1, 0, 0, 0, 10,
                                0, 1, 0, 0, -5,
                                0, 0, 1, 0, 15,
                                0, 0, 0, 1, 0
                        })),
                        blurA
                )
        );
        setRenderEffect(android.graphics.RenderEffect.createChainEffect(blurB, colorEffect));
    }

    private void ensureLegacyAnimator(@LoadType.Type int loadType) {
        if (legacyAnimator != null) {
            legacyAnimator.cancel();
        }
        legacyAnimator = ValueAnimator.ofFloat(0f, 1f);
        legacyAnimator.setDuration(loadType == LoadType.MEDIUM ? 480 : 280);
        legacyAnimator.setRepeatMode(ValueAnimator.REVERSE);
        legacyAnimator.setRepeatCount(ValueAnimator.INFINITE);
        legacyAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            float radius = loadType == LoadType.MEDIUM ? 28f : 65f;
            overlayPaint.setMaskFilter(new BlurMaskFilter(radius + value * radius, BlurMaskFilter.Blur.NORMAL));
            invalidate();
        });
        legacyAnimator.start();
    }

    private void drawWaveBand(Canvas canvas, float width, float height, float layerAlpha) {
        float amplitude = (currentLoad == LoadType.MEDIUM ? 0.08f : 0.18f) * height * layerAlpha;
        float wavelength = currentLoad == LoadType.MEDIUM ? width / 2.2f : width / 3.5f;
        wavePath.reset();
        wavePath.moveTo(0, height);
        for (int i = 0; i <= 40; i++) {
            float x = i * width / 40f;
            float y = (float) (height * 0.55f + Math.sin(phase * 6 + x / wavelength) * amplitude);
            wavePath.lineTo(x, y);
        }
        wavePath.lineTo(width, height);
        wavePath.close();
        overlayPaint.setShader(null);
        overlayPaint.setColor(Color.argb((int) (40 + 80 * layerAlpha), 0, 0, 0));
        canvas.drawPath(wavePath, overlayPaint);
    }

    private void drawAccentArcs(Canvas canvas, float width, float height, float layerAlpha) {
        if (currentLoad == LoadType.LIGHT) {
            return;
        }
        float radius = Math.min(width, height) * (currentLoad == LoadType.MEDIUM ? 0.25f : 0.42f);
        float centerX = width / 2f;
        float centerY = height / 2f;
        tempRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        accentPaint.setAlpha((int) (70 + 110 * layerAlpha));
        canvas.drawArc(tempRect, phase * 120f, 120f + layerAlpha * 90f, false, accentPaint);

        if (currentLoad == LoadType.HEAVY) {
            for (int i = 0; i < 3; i++) {
                float inset = i * radius * 0.12f;
                tempRect.inset(inset, inset);
                canvas.drawArc(tempRect, phase * 90f + i * 45f, 180f, false, accentPaint);
            }
            accentPath.reset();
            accentPath.moveTo(0, height * 0.3f);
            accentPath.cubicTo(width * 0.25f, height * (0.2f + 0.05f * layerAlpha),
                    width * 0.75f, height * (0.6f - 0.1f * layerAlpha),
                    width, height * 0.4f);
            canvas.drawPath(accentPath, accentPaint);
        }
    }
}


