package com.example.wechatfriendfordualwindow;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;
import com.example.wechatfriendfordualwindow.adapters.DualWindowFriendCircleAdapter;
import com.example.wechatfriendfordualwindow.beans.FriendCircleBean;

import java.util.List;

/**
 * Base Activity that creates dual windows for testing.
 * Window 1: Main activity window with scrollable friend circle list (same as aosp-performance)
 * Window 2: Overlay window that continuously animates
 * 
 * In systrace, you will see:
 * - 2 doFrame callbacks per vsync
 * - 2 RenderThread drawFrame per vsync
 */
public abstract class BaseDualWindowActivity extends AppCompatActivity implements Choreographer.FrameCallback {

    protected RecyclerView recyclerView;
    protected DualWindowFriendCircleAdapter adapter;
    protected SecondWindowView secondWindowView;
    protected WindowManager windowManager;
    protected boolean isSecondWindowAdded = false;
    protected int mLoadType;

    // Choreographer 回调相关
    private Choreographer mChoreographer;
    private LoadSimulator mLoadSimulator;
    private int mFrameCount = 0;
    private boolean mIsEnabled = true;
    private boolean mIsScrolling = false;

    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual_window);

        // 设置状态栏透明
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 获取负载类型
        mLoadType = getLoadType();

        // 初始化负载模拟器和 Choreographer
        mLoadSimulator = new LoadSimulator();
        mChoreographer = Choreographer.getInstance();

        // Setup main window view with RecyclerView (friend circle list)
        recyclerView = findViewById(R.id.recycler_view);
        initRecyclerView();

        // 初始化滚动监听器，用于控制主窗口负载
        initScrollListener();

        // Create second window view
        secondWindowView = new SecondWindowView(this);
        secondWindowView.setLoadType(mLoadType);

        // Check overlay permission and add second window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                addSecondWindow();
            } else {
                Toast.makeText(this, "Please grant overlay permission for dual-window demo", Toast.LENGTH_LONG).show();
            }
        } else {
            addSecondWindow();
        }
    }

    /**
     * 初始化滚动监听器
     * 控制主窗口的负载执行和第二窗口的动画
     */
    private void initScrollListener() {
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                    if (isSecondWindowAdded) {
                        secondWindowView.stopAnimation();
                    }
                } else {
                    if (!mIsScrolling) {
                        mIsScrolling = true;
                        // 启动主窗口的负载执行
                        mChoreographer.postFrameCallback(BaseDualWindowActivity.this);
                        // 启动第二窗口的动画
                        if (isSecondWindowAdded) {
                            secondWindowView.startAnimation();
                        }
                    }
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    /**
     * Choreographer 的 doFrame 回调
     * 在滚动期间执行主窗口的负载模拟
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (mIsEnabled && mIsScrolling) {
            mFrameCount++;
            // 根据负载级别决定执行频率
            int frameInterval = getFrameInterval();
            if (mFrameCount % frameInterval == 0) {
                mLoadSimulator.executeInFrameLoad(mLoadType, "DualWindow_MainWindow_doFrame");
            }
            mChoreographer.postFrameCallback(this);
        }
    }

    /**
     * 获取帧间隔，根据负载类型决定执行频率
     */
    private int getFrameInterval() {
        int level = LoadType.getLoadLevel(mLoadType);
        switch (level) {
            case 1: // Light
                return 10;
            case 2: // Medium
                return 8;
            case 3: // Heavy
            default:
                return 6;
        }
    }

    private void initRecyclerView() {
        // 设置布局管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 创建适配器
        adapter = new DualWindowFriendCircleAdapter(this, recyclerView, mLoadType);

        // 添加header view
        View headerView = LayoutInflater.from(this).inflate(R.layout.include_title_bar_view, recyclerView, false);
        adapter.setHeaderView(headerView);

        recyclerView.setAdapter(adapter);

        // 设置数据
        List<FriendCircleBean> friendCircleBeans = DualWindowDataCenter.getInstance()
                .generateDataForLoadType(this, mLoadType);
        adapter.setFriendCircleBeans(friendCircleBeans);
    }

    private void addSecondWindow() {
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                400,
                600,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 100;

        try {
            windowManager.addView(secondWindowView, params);
            isSecondWindowAdded = true;
            // Don't auto-start animation, it will be started when main window scrolls
        } catch (Exception e) {
            Toast.makeText(this, "Failed to add second window: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    protected abstract @LoadType.Type int getLoadType();

    @Override
    protected void onResume() {
        super.onResume();
        // 清空缓存，确保使用正确的负载类型
        DualWindowDataCenter.getInstance().clearCachedData();

        // 刷新数据
        if (adapter != null) {
            List<FriendCircleBean> friendCircleBeans = DualWindowDataCenter.getInstance()
                    .generateDataForLoadType(this, mLoadType);
            adapter.setFriendCircleBeans(friendCircleBeans);
        }

        mIsEnabled = true;
        mIsScrolling = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsEnabled = false;
        // 显式移除 Choreographer 回调，防止 Activity 销毁后继续执行
        if (mChoreographer != null) {
            mChoreographer.removeFrameCallback(this);
        }
        if (isSecondWindowAdded) {
            secondWindowView.stopAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        // 首先标记不再活跃并移除 Choreographer 回调
        mIsEnabled = false;
        if (mChoreographer != null) {
            mChoreographer.removeFrameCallback(this);
        }

        // 移除滚动监听器
        if (recyclerView != null && mScrollListener != null) {
            recyclerView.removeOnScrollListener(mScrollListener);
        }

        // 移除第二窗口
        if (isSecondWindowAdded && secondWindowView != null) {
            secondWindowView.stopAnimation();
            try {
                windowManager.removeView(secondWindowView);
            } catch (Exception e) {
                // View might already be removed
            }
        }

        // 清理适配器资源
        if (adapter != null) {
            adapter.stopContinuousLoadSimulation();
        }

        // 释放负载模拟器资源
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }

        recyclerView.setAdapter(null);
        adapter = null;

        super.onDestroy();
    }
}
