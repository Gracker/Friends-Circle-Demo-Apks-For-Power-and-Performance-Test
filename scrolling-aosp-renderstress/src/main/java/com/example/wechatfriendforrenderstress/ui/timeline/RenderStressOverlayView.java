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
 * 半透明覆盖层：通过 RenderEffect / Shader 在 RenderThread 端制造繁重工作。
 */
public class RenderStressOverlayView extends View implements Choreographer.FrameCallback {

    private static final int LEVEL_NONE = 0;
    private static final int LEVEL_LIGHT = 1;
    private static final int LEVEL_MEDIUM = 2;
    private static final int LEVEL_HEAVY = 3;
    private static final int LEVEL_LONG_FRAME = 4;

    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tempRect = new RectF();
    private final Path wavePath = new Path();
    private final Path accentPath = new Path();
    private final Choreographer choreographer = Choreographer.getInstance();

    private boolean running;
    private float phase;
    private int currentRenderLevel = LEVEL_NONE;
    private @LoadType.Type int currentLoad = LoadType.MINIMAL;
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
        int renderLevel = resolveRenderLevel(loadType);
        if (renderLevel == LEVEL_NONE) {
            stop();
            return;
        }

        currentLoad = loadType;
        currentRenderLevel = renderLevel;

        if (running) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                ensureLegacyAnimator(renderLevel);
            }
            return;
        }

        running = true;
        phase = 0f;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ensureLegacyAnimator(renderLevel);
        }
        choreographer.postFrameCallback(this);
    }

    public void stop() {
        if (!running && currentRenderLevel == LEVEL_NONE) {
            return;
        }
        running = false;
        currentRenderLevel = LEVEL_NONE;
        currentLoad = LoadType.MINIMAL;
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
        if (!running || currentRenderLevel == LEVEL_NONE) {
            return;
        }
        phase += getPhaseStep(currentRenderLevel);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyRenderEffect(currentRenderLevel);
        }
        invalidate();
        choreographer.postFrameCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!running || currentRenderLevel == LEVEL_NONE) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        Trace.beginSection("RenderStressOverlay_draw");

        int repeats = getLayerRepeats(currentRenderLevel);
        for (int layer = 0; layer < repeats; layer++) {
            float layerAlpha = (layer + 1f) / repeats;
            overlayPaint.setShader(new LinearGradient(
                    0,
                    0,
                    width,
                    height,
                    new int[]{
                            Color.argb((int) (18 + 25 * layerAlpha + currentRenderLevel * 4), 255, 255 - layer * 8, 255),
                            Color.argb((int) (45 + 60 * layerAlpha + currentRenderLevel * 6), 120, 200 - layer * 16, 255),
                            Color.argb((int) (70 + 85 * layerAlpha + currentRenderLevel * 8), 40 + layer * 8, 60 + layer * 4, 180)
                    },
                    null,
                    Shader.TileMode.MIRROR
            ));
            canvas.saveLayer(0, 0, width, height, null);
            canvas.drawRect(0, 0, width, height, overlayPaint);
            drawWaveBand(canvas, width, height, layerAlpha, currentRenderLevel);
            drawAccentArcs(canvas, width, height, layerAlpha, currentRenderLevel);
            canvas.restore();
        }
        Trace.endSection();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void applyRenderEffect(int renderLevel) {
        float baseBlur;
        float oscillationScale;
        switch (renderLevel) {
            case LEVEL_LIGHT:
                baseBlur = 42f;
                oscillationScale = 15f;
                break;
            case LEVEL_MEDIUM:
                baseBlur = 90f;
                oscillationScale = 30f;
                break;
            case LEVEL_HEAVY:
                baseBlur = 210f;
                oscillationScale = 90f;
                break;
            case LEVEL_LONG_FRAME:
                baseBlur = 320f;
                oscillationScale = 140f;
                break;
            default:
                baseBlur = 0f;
                oscillationScale = 0f;
                break;
        }
        float oscillation = (float) Math.abs(Math.sin(phase * 3f)) * oscillationScale;
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

        ColorMatrix cm = new ColorMatrix();
        switch (renderLevel) {
            case LEVEL_LIGHT:
                cm.setScale(1.02f, 0.98f, 1.04f, 0.92f);
                break;
            case LEVEL_MEDIUM:
                cm.setScale(1.05f, 0.95f, 1.10f, 0.85f);
                break;
            case LEVEL_HEAVY:
                cm.setRotate(0, 12f);
                cm.setScale(1.20f, 0.90f, 1.30f, 0.75f);
                break;
            case LEVEL_LONG_FRAME:
                cm.setRotate(0, 18f);
                cm.setScale(1.35f, 0.86f, 1.45f, 0.70f);
                break;
            default:
                break;
        }

        android.graphics.RenderEffect colorEffect = android.graphics.RenderEffect.createColorFilterEffect(
                new ColorMatrixColorFilter(cm),
                android.graphics.RenderEffect.createColorFilterEffect(
                        new ColorMatrixColorFilter(new ColorMatrix(new float[]{
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

    private void ensureLegacyAnimator(int renderLevel) {
        if (legacyAnimator != null) {
            legacyAnimator.cancel();
        }
        legacyAnimator = ValueAnimator.ofFloat(0f, 1f);
        long duration;
        switch (renderLevel) {
            case LEVEL_LIGHT:
                duration = 720;
                break;
            case LEVEL_MEDIUM:
                duration = 520;
                break;
            case LEVEL_HEAVY:
                duration = 340;
                break;
            case LEVEL_LONG_FRAME:
                duration = 220;
                break;
            default:
                duration = 520;
                break;
        }
        legacyAnimator.setDuration(duration);
        legacyAnimator.setRepeatMode(ValueAnimator.REVERSE);
        legacyAnimator.setRepeatCount(ValueAnimator.INFINITE);
        legacyAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            float radius;
            switch (renderLevel) {
                case LEVEL_LIGHT:
                    radius = 18f;
                    break;
                case LEVEL_MEDIUM:
                    radius = 28f;
                    break;
                case LEVEL_HEAVY:
                    radius = 65f;
                    break;
                case LEVEL_LONG_FRAME:
                    radius = 95f;
                    break;
                default:
                    radius = 28f;
                    break;
            }
            overlayPaint.setMaskFilter(new BlurMaskFilter(radius + value * radius, BlurMaskFilter.Blur.NORMAL));
            invalidate();
        });
        legacyAnimator.start();
    }

    private void drawWaveBand(Canvas canvas, float width, float height, float layerAlpha, int renderLevel) {
        float amplitudeRatio;
        float wavelengthDivisor;
        int pointCount;
        switch (renderLevel) {
            case LEVEL_LIGHT:
                amplitudeRatio = 0.05f;
                wavelengthDivisor = 2.6f;
                pointCount = 28;
                break;
            case LEVEL_MEDIUM:
                amplitudeRatio = 0.08f;
                wavelengthDivisor = 2.2f;
                pointCount = 40;
                break;
            case LEVEL_HEAVY:
                amplitudeRatio = 0.18f;
                wavelengthDivisor = 3.5f;
                pointCount = 52;
                break;
            case LEVEL_LONG_FRAME:
                amplitudeRatio = 0.24f;
                wavelengthDivisor = 3.0f;
                pointCount = 64;
                break;
            default:
                amplitudeRatio = 0.08f;
                wavelengthDivisor = 2.2f;
                pointCount = 40;
                break;
        }

        float amplitude = amplitudeRatio * height * layerAlpha;
        float wavelength = width / wavelengthDivisor;
        wavePath.reset();
        wavePath.moveTo(0, height);
        for (int i = 0; i <= pointCount; i++) {
            float x = i * width / pointCount;
            float y = (float) (height * 0.55f + Math.sin(phase * 6 + x / wavelength) * amplitude);
            wavePath.lineTo(x, y);
        }
        wavePath.lineTo(width, height);
        wavePath.close();

        overlayPaint.setShader(null);
        overlayPaint.setColor(Color.argb((int) (30 + 70 * layerAlpha + renderLevel * 8), 0, 0, 0));
        canvas.drawPath(wavePath, overlayPaint);
    }

    private void drawAccentArcs(Canvas canvas, float width, float height, float layerAlpha, int renderLevel) {
        if (renderLevel == LEVEL_NONE) {
            return;
        }

        float radius = Math.min(width, height) * (renderLevel <= LEVEL_MEDIUM ? 0.25f : 0.42f);
        float centerX = width / 2f;
        float centerY = height / 2f;
        tempRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        accentPaint.setAlpha((int) (70 + 110 * layerAlpha));
        canvas.drawArc(tempRect, phase * 120f, 120f + layerAlpha * 90f, false, accentPaint);

        if (renderLevel >= LEVEL_HEAVY) {
            int arcPasses = renderLevel == LEVEL_LONG_FRAME ? 5 : 3;
            for (int i = 0; i < arcPasses; i++) {
                float inset = i * radius * 0.10f;
                tempRect.set(
                        centerX - radius + inset,
                        centerY - radius + inset,
                        centerX + radius - inset,
                        centerY + radius - inset
                );
                canvas.drawArc(tempRect, phase * 90f + i * 45f, 180f, false, accentPaint);
            }

            accentPath.reset();
            accentPath.moveTo(0, height * 0.3f);
            accentPath.cubicTo(width * 0.25f, height * (0.2f + 0.05f * layerAlpha),
                    width * 0.75f, height * (0.6f - 0.1f * layerAlpha),
                    width, height * 0.4f);
            canvas.drawPath(accentPath, accentPaint);
        }

        if (renderLevel == LEVEL_LONG_FRAME) {
            accentPath.reset();
            accentPath.moveTo(0, height * 0.7f);
            accentPath.cubicTo(width * 0.2f, height * 0.85f, width * 0.55f, height * 0.55f, width, height * 0.68f);
            canvas.drawPath(accentPath, accentPaint);
        }
    }

    private int resolveRenderLevel(@LoadType.Type int loadType) {
        if (loadType == LoadType.MINIMAL) {
            return LEVEL_NONE;
        }
        int level = LoadType.getLoadLevel(loadType);
        if (level <= 0) {
            return LEVEL_NONE;
        }
        if (level >= LEVEL_LONG_FRAME) {
            return LEVEL_LONG_FRAME;
        }
        return level;
    }

    private int getLayerRepeats(int renderLevel) {
        switch (renderLevel) {
            case LEVEL_LIGHT:
                return 2;
            case LEVEL_MEDIUM:
                return 3;
            case LEVEL_HEAVY:
                return 6;
            case LEVEL_LONG_FRAME:
                return 9;
            default:
                return 1;
        }
    }

    private float getPhaseStep(int renderLevel) {
        switch (renderLevel) {
            case LEVEL_LIGHT:
                return 0.022f;
            case LEVEL_MEDIUM:
                return 0.035f;
            case LEVEL_HEAVY:
                return 0.08f;
            case LEVEL_LONG_FRAME:
                return 0.11f;
            default:
                return 0.0f;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 确保 View 被移除时停止所有动画和 Choreographer 回调
        stop();
    }
}
