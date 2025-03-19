package com.example.wechatfriendforwebview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * WebView版朋友圈基类Activity
 * 提供WebView的基本配置和通用功能
 */
public abstract class BaseFriendCircleWebViewActivity extends AppCompatActivity {
    private static final String TAG = "FriendCircleWebView";
    
    protected WebView webView;
    protected ProgressBar progressBar;
    protected int loadType;
    
    // 添加手势检测器
    protected GestureDetector gestureDetector;
    
    // 添加跟踪fling状态的变量
    protected boolean isFling = false;
    protected int flingFrameCount = 0;
    protected final int MAX_FLING_FRAMES = 200; // fling最大持续帧数
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("BaseFriendCircleWebView_onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_circle_webview);
        
        // 获取传入的负载类型
        loadType = getIntent().getIntExtra(WebViewMainActivity.EXTRA_LOAD_TYPE, WebViewMainActivity.LOAD_TYPE_LIGHT);
        
        // 初始化视图
        initViews();
        
        // 配置WebView
        setupWebView();
        
        // 初始化手势检测器
        initGestureDetector();
        
        // 加载朋友圈HTML
        loadFriendCircleHtml();
        
        Trace.endSection();
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        Trace.beginSection("BaseFriendCircleWebView_initViews");
        
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        
        Trace.endSection();
    }
    
    /**
     * 配置WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        Trace.beginSection("BaseFriendCircleWebView_setupWebView");

        WebSettings webSettings = webView.getSettings();
        
        // 启用JavaScript
        webSettings.setJavaScriptEnabled(true);
        
        // 启用DOM存储API
        webSettings.setDomStorageEnabled(true);
        
        // 启用数据库存储API
        webSettings.setDatabaseEnabled(true);
        
        // 设置缓存模式
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 允许混合内容（现代API已经不再支持以前的方法）
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // 允许加载网络图片
        webSettings.setBlockNetworkImage(false);
        
        // 支持自动加载图片
        webSettings.setLoadsImagesAutomatically(true);
        
        // 设置默认字体大小
        webSettings.setDefaultFontSize(16);
        
        // 设置WebView客户端
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 处理URL加载请求
                String url = request.getUrl().toString();
                Log.d(TAG, "URL被点击: " + url);
                
                // 在这里可以处理特定的URL跳转逻辑
                
                return true; // 返回true表示应用处理URL
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后，隐藏进度条
                progressBar.setVisibility(View.GONE);
                
                // 页面加载完成后，加载朋友圈数据
                loadFriendCircleData();
            }
        });
        
        // 设置WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // 更新进度条
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
        
        // 添加JavaScript接口
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        
        Log.d(TAG, "JavascriptInterface已注册到WebView");
        
        Trace.endSection();
    }
    
    /**
     * 初始化手势检测器
     */
    private void initGestureDetector() {
        Log.d(TAG, "初始化手势检测器");
        
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                Log.d(TAG, "手势检测: onDown");
                return true; // 返回true以便能够检测到后续手势
            }
            
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // 当检测到fling手势时，记录详细信息
                float distance = 0;
                
                if (e1 != null && e2 != null) {
                    distance = (float) Math.sqrt(Math.pow(e2.getX() - e1.getX(), 2) + Math.pow(e2.getY() - e1.getY(), 2));
                }
                
                Log.d(TAG, "手势检测: onFling 检测到!" +
                        " 速度X:" + velocityX + 
                        " 速度Y:" + velocityY + 
                        " 距离:" + distance);
                
                // 设置fling状态
                isFling = true;
                flingFrameCount = 0;
                
                // 触发fling操作
                handleFling(velocityX, velocityY);
                
                return true;
            }
            
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.d(TAG, "手势检测: onScroll");
                
                // 在滚动时阻塞一段时间，减少为原来的1/10
                if (Math.abs(distanceY) > 10) {
                    try {
                        Thread.sleep(3); // 从30ms减少到3ms
                    } catch (InterruptedException e) {
                        Log.e(TAG, "滚动等待被中断", e);
                    }
                }
                
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
        
        // 设置WebView的触摸事件监听器
        setupTouchListener();
    }
    
    /**
     * 处理fling操作，在子类中实现具体逻辑
     */
    protected void handleFling(float velocityX, float velocityY) {
        // 基类中只设置状态，具体负载逻辑由子类实现
        Log.d(TAG, "handleFling - 开始执行, 速度X:" + velocityX + " 速度Y:" + velocityY);
        
        final long startTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "开始fling处理, 初始速度: X=" + velocityX + ", Y=" + velocityY);
        
        isFling = true;
        flingFrameCount = 0;
        
        // 执行单帧fling负载，同步执行
        executeFlingLoad();
        
        // 后续帧使用Handler延迟执行
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable flingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFling && flingFrameCount < MAX_FLING_FRAMES) {
                    flingFrameCount++;
                    Log.d(TAG, "执行fling负载帧 " + flingFrameCount + "/" + MAX_FLING_FRAMES);
                    
                    // 阻塞主线程同步执行负载
                    executeFlingLoad();
                    
                    // 继续安排下一帧，但不使用循环
                    handler.postDelayed(this, 16);
                } else {
                    isFling = false;
                    long duration = SystemClock.elapsedRealtime() - startTime;
                    Log.d(TAG, "fling处理结束, 执行了 " + flingFrameCount + " 帧, 总用时: " + duration + "ms");
                }
            }
        };
        
        // 安排后续帧
        handler.postDelayed(flingRunnable, 16);
    }
    
    /**
     * 在fling过程中每一帧执行的负载，由子类实现
     */
    protected abstract void executeFlingLoad();
    
    /**
     * 加载朋友圈HTML页面
     */
    private void loadFriendCircleHtml() {
        Trace.beginSection("BaseFriendCircleWebView_loadFriendCircleHtml");
        
        // 从assets目录加载HTML
        webView.loadUrl("file:///android_asset/friend_circle.html");
        
        Trace.endSection();
    }
    
    /**
     * 加载朋友圈数据到WebView
     */
    protected void loadFriendCircleData() {
        Trace.beginSection("BaseFriendCircleWebView_loadFriendCircleData");
        
        // 减少延迟时间，加快数据加载速度
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                // 获取朋友圈JSON数据
                String jsonData = WebViewDataCenter.getInstance().getFriendCircleJsonData(loadType);
                
                // 将数据传递给WebView
                if (webView != null && jsonData != null) {
                    String jsCode = "javascript:loadFriendCircleData(" + jsonData + ")";
                    webView.evaluateJavascript(jsCode, null);
                    Log.d(TAG, "朋友圈数据已加载");
                }
                
                // 执行额外的负载逻辑，由子类实现
                performLoadTask();
            } catch (Exception e) {
                Log.e(TAG, "加载朋友圈数据失败", e);
            }
        }, 100); // 减少延迟到100ms
        
        Trace.endSection();
    }
    
    /**
     * 执行负载任务，由子类实现
     */
    protected abstract void performLoadTask();
    
    /**
     * JavaScript接口，用于实现JavaScript和Android的交互
     */
    @SuppressLint("JavascriptInterface")
    public class WebAppInterface {
        private Context context;
        private Handler handler;

        public WebAppInterface(Context context) {
            this.context = context;
            this.handler = new Handler(Looper.getMainLooper());
        }

        /**
         * 显示Toast提示
         */
        @JavascriptInterface
        public void showToast(String message) {
            handler.post(() -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            });
        }
        
        /**
         * 获取负载类型
         */
        @JavascriptInterface
        public int getLoadType() {
            return loadType;
        }
        
        /**
         * 显示图片
         */
        @JavascriptInterface
        public void showImage(String imageUrl) {
            Log.d(TAG, "显示图片: " + imageUrl);
            handler.post(() -> {
                Toast.makeText(context, "查看图片: " + imageUrl, Toast.LENGTH_SHORT).show();
            });
        }
        
        /**
         * 上报性能数据
         */
        @JavascriptInterface
        public void reportPerformance(String loadType, int fps, boolean isScrolling) {
            Log.d(TAG, "性能数据: 负载=" + loadType + ", FPS=" + fps + ", 滚动状态=" + isScrolling);
            handler.post(() -> {
                Toast.makeText(context, "性能: " + loadType + ", FPS=" + fps, Toast.LENGTH_SHORT).show();
            });
        }
        
        /**
         * 加载更多朋友圈数据
         */
        @JavascriptInterface
        public void loadMoreItems(int count) {
            Log.d(TAG, "请求加载更多数据: " + count + "条");
            handler.post(() -> {
                // 生成更多朋友圈数据并返回给JavaScript
                generateMoreFriendCircleData(count);
            });
        }
    }
    
    /**
     * 生成更多朋友圈数据并发送回JavaScript
     */
    private void generateMoreFriendCircleData(int count) {
        try {
            String jsonData = WebViewDataCenter.getInstance().getMoreFriendCircleJsonData(count);
            
            if (webView != null && jsonData != null) {
                String jsCode = "javascript:appendFriendCircleData(" + jsonData + ")";
                webView.evaluateJavascript(jsCode, null);
                Log.d(TAG, "已加载" + count + "条新数据");
            }
        } catch (Exception e) {
            Log.e(TAG, "加载更多朋友圈数据失败", e);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        // 清理WebView
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    // 修改JavaScript评估方法的访问修饰符为public
    public void evaluateJavascript(String script, ValueCallback<String> callback) {
        Log.d(TAG, "准备执行JavaScript: " + (script.length() > 100 ? script.substring(0, 100) + "..." : script));
        try {
            webView.evaluateJavascript(script, value -> {
                Log.d(TAG, "JavaScript执行结果: " + value);
                if (callback != null) {
                    callback.onReceiveValue(value);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "执行JavaScript时出错", e);
            if (callback != null) {
                callback.onReceiveValue("null");
            }
        }
    }

    // 添加一个辅助方法来设置WebView的触摸监听器，在触摸事件中增加阻塞
    private void setupTouchListener() {
        if (webView != null) {
            webView.setOnTouchListener(new View.OnTouchListener() {
                long lastTouchTime = 0;
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // 记录触摸类型
                    String actionName = "";
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            actionName = "ACTION_DOWN";
                            break;
                        case MotionEvent.ACTION_UP:
                            actionName = "ACTION_UP";
                            // 在触摸结束时阻塞一小段时间，减少为原来的1/10
                            try {
                                Thread.sleep(2); // 从20ms减少到2ms
                            } catch (InterruptedException e) {
                                Log.e(TAG, "触摸等待被中断", e);
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            actionName = "ACTION_MOVE";
                            // 仅在移动时，限制频率并阻塞，减少为原来的1/10
                            long currentTime = SystemClock.elapsedRealtime();
                            if (currentTime - lastTouchTime > 30) { // 保持频率限制不变
                                lastTouchTime = currentTime;
                                // 在滑动过程中，阻塞主线程一小段时间
                                try {
                                    Thread.sleep(5); // 从50ms减少到5ms
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "触摸等待被中断", e);
                                }
                            }
                            break;
                    }
                    
                    Log.d(TAG, "WebView触摸事件: " + actionName);
                    
                    // 传递事件给手势检测器
                    boolean handled = gestureDetector.onTouchEvent(event);
                    
                    // 返回false以允许WebView继续处理触摸事件
                    return false;
                }
            });
            
            Log.d(TAG, "WebView触摸监听器已设置");
        } else {
            Log.e(TAG, "WebView为null，无法设置触摸监听器");
        }
    }
} 