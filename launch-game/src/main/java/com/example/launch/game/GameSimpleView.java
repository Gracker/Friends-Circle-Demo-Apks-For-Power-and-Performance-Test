package com.example.launch.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * 简单的游戏视图 - 使用Canvas 2D绘制替代OpenGL
 * 用于测试和备选方案
 */
public class GameSimpleView extends View {
    private static final String TAG = "GameSimpleView";
    private static final long PARTICLE_RANDOM_SEED = 0x5EED1234L;

    private boolean isLoading = false;
    private int loadingProgress = 0;
    private String loadingMessage = "";
    private Paint paint;
    private Paint textPaint;
    private final Random random = new Random(PARTICLE_RANDOM_SEED);
    private long animationTime = 0;

    // 粒子数据
    private static final int PARTICLE_COUNT = 20;
    private float[] particleX = new float[PARTICLE_COUNT];
    private float[] particleY = new float[PARTICLE_COUNT];
    private float[] particleVX = new float[PARTICLE_COUNT];
    private float[] particleVY = new float[PARTICLE_COUNT];
    private int[] particleColor = new int[PARTICLE_COUNT];

    public GameSimpleView(Context context) {
        super(context);
        init();
    }

    public GameSimpleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // 初始化粒子
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] = random.nextFloat();
            particleY[i] = random.nextFloat();
            particleVX[i] = (random.nextFloat() - 0.5f) * 0.02f;
            particleVY[i] = (random.nextFloat() - 0.5f) * 0.02f;
            particleColor[i] = Color.rgb(
                100 + random.nextInt(156),
                100 + random.nextInt(156),
                100 + random.nextInt(156)
            );
        }

        // 启动动画
        post(animationRunnable);
    }

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            animationTime += 16; // ~60fps

            // 更新粒子位置
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                particleX[i] += particleVX[i];
                particleY[i] += particleVY[i];

                // 边界反弹
                if (particleX[i] < 0 || particleX[i] > 1) {
                    particleVX[i] = -particleVX[i];
                    particleX[i] = Math.max(0, Math.min(1, particleX[i]));
                }
                if (particleY[i] < 0 || particleY[i] > 1) {
                    particleVY[i] = -particleVY[i];
                    particleY[i] = Math.max(0, Math.min(1, particleY[i]));
                }
            }

            invalidate();
            postDelayed(this, 16);
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 动态背景色
        float hue = (animationTime * 0.1f) % 360;
        int bgColor = Color.HSVToColor(new float[]{hue, 0.3f, 0.2f});
        canvas.drawColor(bgColor);

        if (isLoading) {
            // 绘制加载界面

            // 绘制粒子效果
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                paint.setColor(particleColor[i]);
                paint.setAlpha(180);
                float x = particleX[i] * width;
                float y = particleY[i] * height;
                canvas.drawCircle(x, y, 10, paint);
            }

            // 绘制GRACKER Logo
            paint.setColor(Color.parseColor("#4CAF50"));
            paint.setAlpha(255);
            canvas.drawRect(width * 0.3f, height * 0.2f, width * 0.7f, height * 0.4f, paint);

            // 绘制Logo文字
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("GRACKER", width / 2f, height * 0.33f, paint);

            // 绘制进度条背景
            paint.setColor(Color.parseColor("#333333"));
            paint.setAlpha(200);
            RectF progressBg = new RectF(width * 0.2f, height * 0.6f, width * 0.8f, height * 0.65f);
            canvas.drawRoundRect(progressBg, 10, 10, paint);

            // 绘制进度条填充
            paint.setColor(Color.parseColor("#4CAF50"));
            paint.setAlpha(255);
            float progressWidth = (width * 0.6f) * (loadingProgress / 100.0f);
            RectF progressFill = new RectF(width * 0.2f, height * 0.6f,
                                          width * 0.2f + progressWidth, height * 0.65f);
            canvas.drawRoundRect(progressFill, 10, 10, paint);

            // 绘制进度文字
            paint.setColor(Color.WHITE);
            paint.setTextSize(24);
            canvas.drawText(loadingProgress + "%", width / 2f, height * 0.72f, paint);

            // 绘制加载消息
            if (!loadingMessage.isEmpty()) {
                paint.setTextSize(18);
                paint.setAlpha(200);
                canvas.drawText(loadingMessage, width / 2f, height * 0.78f, paint);
            }
        }
    }

    public void setLoadingState(boolean loading) {
        this.isLoading = loading;
        invalidate();
    }

    public void updateLoadingProgress(String message, int progress) {
        this.loadingMessage = message;
        this.loadingProgress = progress;
        invalidate();
    }

    public void setLoadingComplete() {
        this.isLoading = false;
        this.loadingMessage = "";
        this.loadingProgress = 100;
        invalidate();
    }
}
