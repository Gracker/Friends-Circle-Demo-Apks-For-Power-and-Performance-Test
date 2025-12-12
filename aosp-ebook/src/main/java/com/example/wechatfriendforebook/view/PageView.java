package com.example.wechatfriendforebook.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.List;

/**
 * 电子书页面视图
 * 支持点击左右翻页和滑动翻页动画
 */
public class PageView extends View {
    private static final String TAG = "PageView";

    // 触摸区域划分比例
    private static final float LEFT_AREA_RATIO = 0.33f;
    private static final float RIGHT_AREA_RATIO = 0.67f;

    // 页面内容
    private List<String> pages;
    private int currentPage = 0;

    // 绘制相关
    private TextPaint textPaint;
    private Paint bgPaint;
    private int paddingLeft = 48;
    private int paddingTop = 64;
    private int paddingRight = 48;
    private int paddingBottom = 80;

    // 颜色配置
    private int backgroundColor = 0xFFFFF8E1; // 暖黄色背景
    private int textColor = 0xFF333333;

    // 动画相关
    private float animationOffset = 0f;
    private boolean isAnimating = false;
    private int animationDirection = 0; // -1: 向左（下一页），1: 向右（上一页）
    private ValueAnimator pageAnimator;

    // 手势检测
    private GestureDetector gestureDetector;

    // 回调
    private OnPageChangeListener pageChangeListener;
    private OnMenuToggleListener menuToggleListener;

    public PageView(Context context) {
        super(context);
        init();
    }

    public PageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 设置可点击，确保能接收触摸事件
        setClickable(true);
        setFocusable(true);

        // 初始化文本画笔
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(48f);
        textPaint.setColor(textColor);

        // 初始化背景画笔
        bgPaint = new Paint();
        bgPaint.setColor(backgroundColor);

        // 初始化手势检测器
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                // 必须返回 true，否则后续事件不会传递
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                handleTap(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX < -500) {
                        // 向左滑动 -> 下一页
                        goToNextPage();
                        return true;
                    } else if (velocityX > 500) {
                        // 向右滑动 -> 上一页
                        goToPreviousPage();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * 处理点击事件
     */
    private void handleTap(float x, float y) {
        if (isAnimating) {
            Log.d(TAG, "点击被忽略：动画进行中");
            return;
        }

        int width = getWidth();
        float leftBoundary = width * LEFT_AREA_RATIO;
        float rightBoundary = width * RIGHT_AREA_RATIO;

        Log.d(TAG, "处理点击: x=" + x + ", width=" + width + 
                   ", leftBoundary=" + leftBoundary + ", rightBoundary=" + rightBoundary);

        if (x < leftBoundary) {
            // 点击左侧 -> 上一页
            Log.d(TAG, "点击左侧 -> 上一页");
            goToPreviousPage();
        } else if (x > rightBoundary) {
            // 点击右侧 -> 下一页
            Log.d(TAG, "点击右侧 -> 下一页");
            goToNextPage();
        } else {
            // 点击中间 -> 显示/隐藏菜单
            Log.d(TAG, "点击中间 -> 切换菜单");
            if (menuToggleListener != null) {
                menuToggleListener.onMenuToggle();
            }
        }
    }

    /**
     * 设置页面内容
     */
    public void setPages(List<String> pages) {
        this.pages = pages;
        this.currentPage = 0;
        invalidate();

        if (pageChangeListener != null) {
            pageChangeListener.onPageChanged(currentPage, getPageCount());
        }
    }

    /**
     * 获取当前页码
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * 获取总页数
     */
    public int getPageCount() {
        return pages != null ? pages.size() : 0;
    }

    /**
     * 跳转到指定页面
     */
    public void goToPage(int pageIndex) {
        if (pages == null || pageIndex < 0 || pageIndex >= pages.size()) {
            return;
        }
        currentPage = pageIndex;
        invalidate();

        if (pageChangeListener != null) {
            pageChangeListener.onPageChanged(currentPage, getPageCount());
        }
    }

    /**
     * 上一页
     */
    public void goToPreviousPage() {
        Log.d(TAG, "goToPreviousPage: currentPage=" + currentPage + ", isAnimating=" + isAnimating);
        if (isAnimating) {
            Log.d(TAG, "上一页被忽略：动画进行中");
            return;
        }
        if (currentPage <= 0) {
            Log.d(TAG, "已经是第一页");
            return;
        }

        animatePageTurn(1); // 向右动画
    }

    /**
     * 下一页
     */
    public void goToNextPage() {
        Log.d(TAG, "goToNextPage: currentPage=" + currentPage + ", totalPages=" + getPageCount() + ", isAnimating=" + isAnimating);
        if (isAnimating) {
            Log.d(TAG, "下一页被忽略：动画进行中");
            return;
        }
        if (pages == null || currentPage >= pages.size() - 1) {
            Log.d(TAG, "已经是最后一页");
            return;
        }

        animatePageTurn(-1); // 向左动画
    }

    /**
     * 翻页动画
     */
    private void animatePageTurn(int direction) {
        isAnimating = true;
        animationDirection = direction;

        if (pageAnimator != null && pageAnimator.isRunning()) {
            pageAnimator.cancel();
        }

        pageAnimator = ValueAnimator.ofFloat(0f, 1f);
        pageAnimator.setDuration(300);
        pageAnimator.setInterpolator(new DecelerateInterpolator());
        pageAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            animationOffset = progress * getWidth() * direction;
            invalidate();
        });
        pageAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // 更新页码
                if (direction < 0) {
                    currentPage++;
                } else {
                    currentPage--;
                }

                animationOffset = 0f;
                isAnimating = false;
                animationDirection = 0;
                invalidate();

                if (pageChangeListener != null) {
                    pageChangeListener.onPageChanged(currentPage, getPageCount());
                }
            }
        });
        pageAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 绘制背景
        canvas.drawColor(backgroundColor);

        if (pages == null || pages.isEmpty()) {
            // 没有内容时显示提示
            drawCenteredText(canvas, "正在加载...", width, height);
            return;
        }

        // 保存画布状态
        canvas.save();

        if (isAnimating) {
            // 动画中：绘制当前页和目标页
            if (animationDirection < 0) {
                // 向左翻页（下一页）
                // 绘制当前页（向左移动）
                canvas.save();
                canvas.translate(animationOffset, 0);
                drawPage(canvas, currentPage, width, height);
                canvas.restore();

                // 绘制下一页（从右侧进入）
                if (currentPage + 1 < pages.size()) {
                    canvas.save();
                    canvas.translate(width + animationOffset, 0);
                    drawPage(canvas, currentPage + 1, width, height);
                    canvas.restore();
                }
            } else {
                // 向右翻页（上一页）
                // 绘制当前页（向右移动）
                canvas.save();
                canvas.translate(animationOffset, 0);
                drawPage(canvas, currentPage, width, height);
                canvas.restore();

                // 绘制上一页（从左侧进入）
                if (currentPage - 1 >= 0) {
                    canvas.save();
                    canvas.translate(-width + animationOffset, 0);
                    drawPage(canvas, currentPage - 1, width, height);
                    canvas.restore();
                }
            }
        } else {
            // 正常绘制当前页
            drawPage(canvas, currentPage, width, height);
        }

        canvas.restore();
    }

    /**
     * 绘制指定页面
     */
    private void drawPage(Canvas canvas, int pageIndex, int width, int height) {
        if (pageIndex < 0 || pageIndex >= pages.size()) return;

        // 绘制背景
        canvas.drawRect(0, 0, width, height, bgPaint);

        String content = pages.get(pageIndex);

        int contentWidth = width - paddingLeft - paddingRight;

        // 使用 StaticLayout 绘制多行文本
        StaticLayout staticLayout = StaticLayout.Builder
                .obtain(content, 0, content.length(), textPaint, contentWidth)
                .setLineSpacing(16f, 1.3f)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build();

        canvas.save();
        canvas.translate(paddingLeft, paddingTop);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * 绘制居中文本
     */
    private void drawCenteredText(Canvas canvas, String text, int width, int height) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textWidth = textPaint.measureText(text);
        float x = (width - textWidth) / 2;
        float y = height / 2 - (fm.ascent + fm.descent) / 2;
        canvas.drawText(text, x, y, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 先交给手势检测器处理
        boolean handled = gestureDetector.onTouchEvent(event);
        // 确保返回 true 以继续接收后续事件
        return handled || super.onTouchEvent(event);
    }

    /**
     * 设置背景颜色
     */
    public void setPageBackgroundColor(int color) {
        this.backgroundColor = color;
        bgPaint.setColor(color);
        invalidate();
    }

    /**
     * 设置文字颜色
     */
    public void setPageTextColor(int color) {
        this.textColor = color;
        textPaint.setColor(color);
        invalidate();
    }

    /**
     * 设置文字大小
     */
    public void setPageTextSize(float size) {
        textPaint.setTextSize(size);
        invalidate();
    }

    /**
     * 设置页面变化监听器
     */
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    /**
     * 设置菜单切换监听器
     */
    public void setOnMenuToggleListener(OnMenuToggleListener listener) {
        this.menuToggleListener = listener;
    }

    /**
     * 页面变化监听器接口
     */
    public interface OnPageChangeListener {
        void onPageChanged(int currentPage, int totalPages);
    }

    /**
     * 菜单切换监听器接口
     */
    public interface OnMenuToggleListener {
        void onMenuToggle();
    }
}

