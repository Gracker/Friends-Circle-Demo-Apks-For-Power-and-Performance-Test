package com.example.wechatfriendforebook;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wechatfriendforebook.parser.EpubParser;
import com.example.wechatfriendforebook.parser.PageSplitter;
import com.example.wechatfriendforebook.view.PageView;
import com.example.scrolling.common.model.AppInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 电子书阅读器主界面
 * 
 * 交互方式：
 * - 点击屏幕左侧（1/3）：上一页
 * - 点击屏幕右侧（1/3）：下一页
 * - 点击屏幕中间（1/3）：显示/隐藏菜单
 * - 左右滑动：翻页
 */
public class EBookReaderActivity extends AppCompatActivity {
    private static final String TAG = "EBookReaderActivity";

    // 默认电子书文件名
    private static final String DEFAULT_BOOK = "default_book.epub";

    // UI 组件
    private PageView pageView;
    private ProgressBar loadingProgress;
    private TextView tvPageInfo;
    private TextView tvBookTitle;
    private View topMenu;
    private View bottomMenu;
    private SeekBar seekBarProgress;
    private ImageButton btnBack;
    private ImageButton btnBookmark;

    // 功能按钮
    private View btnContents;
    private View btnBrightness;
    private View btnFontSize;
    private View btnTheme;
    private View btnSettings;

    // 菜单状态
    private boolean isMenuVisible = false;

    // 解析器
    private EpubParser epubParser;
    private PageSplitter pageSplitter;

    // 后台线程
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // 生命周期标志
    private volatile boolean isActivityActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏模式
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_ebook_reader);

        // 设置应用信息
        setupAppInfo();

        initViews();
        setupListeners();
        loadBook();
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        pageView = findViewById(R.id.pageView);
        loadingProgress = findViewById(R.id.loadingProgress);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        tvBookTitle = findViewById(R.id.tvBookTitle);
        topMenu = findViewById(R.id.topMenu);
        bottomMenu = findViewById(R.id.bottomMenu);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        btnBack = findViewById(R.id.btnBack);
        btnBookmark = findViewById(R.id.btnBookmark);

        btnContents = findViewById(R.id.btnContents);
        btnBrightness = findViewById(R.id.btnBrightness);
        btnFontSize = findViewById(R.id.btnFontSize);
        btnTheme = findViewById(R.id.btnTheme);
        btnSettings = findViewById(R.id.btnSettings);

        // 初始隐藏菜单
        topMenu.setVisibility(View.GONE);
        bottomMenu.setVisibility(View.GONE);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 页面变化监听
        pageView.setOnPageChangeListener((currentPage, totalPages) -> {
            updatePageInfo(currentPage, totalPages);
            updateSeekBar(currentPage, totalPages);
        });

        // 菜单切换监听
        pageView.setOnMenuToggleListener(this::toggleMenu);

        // 返回按钮
        btnBack.setOnClickListener(v -> {
            hideMenu();
            finish();
        });

        // 书签按钮（模拟功能）
        btnBookmark.setOnClickListener(v -> {
            Toast.makeText(this, "已添加书签", Toast.LENGTH_SHORT).show();
        });

        // 进度条拖动
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && pageView.getPageCount() > 0) {
                    int targetPage = (int) ((float) progress / 100 * (pageView.getPageCount() - 1));
                    pageView.goToPage(targetPage);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 底部功能按钮（模拟功能，显示 Toast）
        btnContents.setOnClickListener(v -> {
            Toast.makeText(this, "目录功能（模拟）", Toast.LENGTH_SHORT).show();
        });

        btnBrightness.setOnClickListener(v -> {
            Toast.makeText(this, "亮度调节（模拟）", Toast.LENGTH_SHORT).show();
        });

        btnFontSize.setOnClickListener(v -> {
            Toast.makeText(this, "字体大小（模拟）", Toast.LENGTH_SHORT).show();
        });

        btnTheme.setOnClickListener(v -> {
            Toast.makeText(this, "主题切换（模拟）", Toast.LENGTH_SHORT).show();
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "设置（模拟）", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 加载电子书
     */
    private void loadBook() {
        showLoading(true);

        executor.execute(() -> {
            try {
                // 解析 EPUB 文件
                epubParser = new EpubParser(this);
                boolean success = epubParser.parseFromAssets(DEFAULT_BOOK);

                if (!success) {
                    mainHandler.post(() -> {
                        if (!isActivityActive) return;
                        showLoading(false);
                        Toast.makeText(this, "无法加载电子书", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // 等待视图测量完成后再分页
                mainHandler.post(() -> {
                    if (!isActivityActive) return;
                    pageView.post(() -> {
                        if (isActivityActive) {
                            splitPagesAndDisplay();
                        }
                    });
                });

            } catch (Exception e) {
                Log.e(TAG, "加载电子书失败", e);
                mainHandler.post(() -> {
                    if (!isActivityActive) return;
                    showLoading(false);
                    Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 分页并显示内容
     */
    private void splitPagesAndDisplay() {
        executor.execute(() -> {
            try {
                int width = pageView.getWidth();
                int height = pageView.getHeight();

                if (width == 0 || height == 0) {
                    // 使用默认尺寸
                    width = getResources().getDisplayMetrics().widthPixels;
                    height = getResources().getDisplayMetrics().heightPixels;
                }

                // 创建分页器
                pageSplitter = new PageSplitter(width, height);
                pageSplitter.setTextSize(48f);
                pageSplitter.setPadding(48, 64, 48, 80);
                pageSplitter.setLineSpacing(1.5f);
                pageSplitter.setParagraphSpacing(32f);

                // 执行分页
                pageSplitter.splitPages(epubParser.getAllContent());

                mainHandler.post(() -> {
                    if (!isActivityActive) return;
                    showLoading(false);

                    // 设置书名
                    String title = epubParser.getBookTitle();
                    tvBookTitle.setText(title);

                    // 设置页面内容
                    pageView.setPages(pageSplitter.getPages());

                    // 更新页码信息
                    updatePageInfo(0, pageSplitter.getPageCount());

                    Log.d(TAG, "电子书加载完成: " + title + ", 共 " + pageSplitter.getPageCount() + " 页");
                });

            } catch (Exception e) {
                Log.e(TAG, "分页失败", e);
                mainHandler.post(() -> {
                    if (!isActivityActive) return;
                    showLoading(false);
                    Toast.makeText(this, "分页失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 显示/隐藏加载指示器
     */
    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * 更新页码信息
     */
    private void updatePageInfo(int currentPage, int totalPages) {
        String pageInfo = getString(R.string.page_format, currentPage + 1, totalPages);
        tvPageInfo.setText(pageInfo);
    }

    /**
     * 更新进度条
     */
    private void updateSeekBar(int currentPage, int totalPages) {
        if (totalPages > 1) {
            int progress = (int) ((float) currentPage / (totalPages - 1) * 100);
            seekBarProgress.setProgress(progress);
        }
    }

    /**
     * 切换菜单显示状态
     */
    private void toggleMenu() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    /**
     * 显示菜单
     */
    private void showMenu() {
        isMenuVisible = true;

        // 显示顶部菜单（从上方滑入）
        topMenu.setVisibility(View.VISIBLE);
        topMenu.setTranslationY(-topMenu.getHeight());
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topMenu, "translationY", -200f, 0f);
        topAnimator.setDuration(250);
        topAnimator.start();

        // 显示底部菜单（从下方滑入）
        bottomMenu.setVisibility(View.VISIBLE);
        bottomMenu.setTranslationY(bottomMenu.getHeight());
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomMenu, "translationY", 200f, 0f);
        bottomAnimator.setDuration(250);
        bottomAnimator.start();

        // 隐藏页码（菜单显示时）
        tvPageInfo.setVisibility(View.INVISIBLE);
    }

    /**
     * 隐藏菜单
     */
    private void hideMenu() {
        isMenuVisible = false;

        // 隐藏顶部菜单（向上滑出）
        ObjectAnimator topAnimator = ObjectAnimator.ofFloat(topMenu, "translationY", 0f, -200f);
        topAnimator.setDuration(250);
        topAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                topMenu.setVisibility(View.GONE);
            }
        });
        topAnimator.start();

        // 隐藏底部菜单（向下滑出）
        ObjectAnimator bottomAnimator = ObjectAnimator.ofFloat(bottomMenu, "translationY", 0f, 200f);
        bottomAnimator.setDuration(250);
        bottomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bottomMenu.setVisibility(View.GONE);
            }
        });
        bottomAnimator.start();

        // 显示页码
        tvPageInfo.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        isActivityActive = false;
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
        // 正确关闭ExecutorService，等待任务完成
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                // 等待最多1秒让任务完成
                if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    // 如果超时，强制关闭
                    executor.shutdownNow();
                    android.util.Log.w("EBookReader", "Executor did not terminate in time, forced shutdown");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // 保持全屏模式
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void setupAppInfo() {
        // 创建应用信息
        AppInfo appInfo = new AppInfo(
                "电子书阅读器",
                "高性能电子书阅读器，支持EPUB格式，实现分页和手势操作",
                getPackageName());

        // 设置应用信息到UI
        TextView appNameView = findViewById(R.id.tv_app_name);
        TextView appFeatureView = findViewById(R.id.tv_app_feature);
        TextView appPackageView = findViewById(R.id.tv_package_name);

        if (appNameView != null) {
            appNameView.setText(appInfo.getAppName());
        }
        if (appFeatureView != null) {
            appFeatureView.setText(appInfo.getAppFeature());
        }
        if (appPackageView != null) {
            appPackageView.setText(appInfo.getPackageName());
        }
    }
}
