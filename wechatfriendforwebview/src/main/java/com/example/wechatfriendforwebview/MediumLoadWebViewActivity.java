package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 中负载WebView版朋友圈Activity
 * 添加中等级别的额外负载任务，全部在WebView中执行
 */
public class MediumLoadWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "MediumLoadWebView";
    
    // 处理器实例
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("MediumLoadWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        // 设置标题
        setTitle("WebView朋友圈 - 中负载");
        Trace.endSection();
    }

    /**
     * 执行负载任务，移除启动时的负载，确保快速加载
     */
    @Override
    protected void performLoadTask() {
        // 不执行任何负载，仅记录日志
        Log.d(TAG, "中负载模式 - 仅在滚动时执行负载");
    }
    
    /**
     * 在fling过程中执行负载
     */
    @Override
    protected void executeFlingLoad() {
        // 阻塞主线程一小段时间
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad阻塞被中断", e);
        }
        
        // 执行同步JavaScript
        final String testJs = "\"测试成功\"";
        webView.evaluateJavascript(testJs, value -> {
            // 测试成功后直接执行负载
            if (value != null && !value.equals("null")) {
                // 中负载JavaScript
                String loadJs = 
                    "(function() {\n" +
                    "  var startTime = performance.now();\n" +
                    "  \n" +
                    "  // 直接阻塞主线程\n" +
                    "  var blockStart = performance.now();\n" +
                    "  while(performance.now() - blockStart < 10) {\n" +
                    "    Math.random();\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 1. 创建样式表\n" +
                    "  for (var i = 0; i < 6; i++) {\n" +
                    "    var style = document.createElement('style');\n" +
                    "    style.id = 'mediumLoadStyle' + i;\n" +
                    "    style.innerHTML = '.medium-load-' + i + ' { color: blue; font-size: ' + (i+8) + 'px; }';\n" +
                    "    document.head.appendChild(style);\n" +
                    "    document.body.offsetHeight;\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 2. 创建DOM元素并强制布局\n" +
                    "  for (var i = 0; i < 4; i++) {\n" +
                    "    var div = document.createElement('div');\n" +
                    "    div.className = 'medium-load-' + (i % 6);\n" +
                    "    div.style.width = (10 + i) + 'px';\n" +
                    "    div.style.height = (10 + i) + 'px';\n" +
                    "    div.style.position = 'absolute';\n" +
                    "    div.style.top = (i * 5) + 'px';\n" +
                    "    div.style.left = (i * 5) + 'px';\n" +
                    "    div.style.backgroundColor = 'rgba(' + i*3 + ',' + i*5 + ',' + i*2 + ',0.5)';\n" +
                    "    div.innerText = 'Medium Load ' + i;\n" +
                    "    document.body.appendChild(div);\n" +
                    "    div.getBoundingClientRect();\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 3. 创建并绘制Canvas\n" +
                    "  var canvas = document.createElement('canvas');\n" +
                    "  canvas.width = 200;\n" +
                    "  canvas.height = 200;\n" +
                    "  var ctx = canvas.getContext('2d');\n" +
                    "  for (var j = 0; j < 10; j++) {\n" +
                    "    ctx.fillStyle = 'rgb(' + j*3 + ',' + j*5 + ',' + j*2 + ')';\n" +
                    "    ctx.fillRect(j*4, j*4, 30, 30);\n" +
                    "  }\n" +
                    "  document.body.appendChild(canvas);\n" +
                    "  canvas.getBoundingClientRect();\n" +
                    "  \n" +
                    "  // 4. 耗时计算\n" +
                    "  var result = 0;\n" +
                    "  for (var k = 0; k < 10000; k++) {\n" +
                    "    result += Math.sqrt(k) * Math.cos(k);\n" +
                    "  }\n" +
                    "  \n" +
                    "  var endTime = performance.now();\n" +
                    "  \n" +
                    "  // 移除创建的元素\n" +
                    "  setTimeout(function() {\n" +
                    "    var styles = document.querySelectorAll('style[id^=\"mediumLoadStyle\"]');\n" +
                    "    for (var i = 0; i < styles.length; i++) {\n" +
                    "      styles[i].parentNode.removeChild(styles[i]);\n" +
                    "    }\n" +
                    "    var canvases = document.querySelectorAll('canvas');\n" +
                    "    for (var i = 0; i < canvases.length; i++) {\n" +
                    "      canvases[i].parentNode.removeChild(canvases[i]);\n" +
                    "    }\n" +
                    "    var divs = document.querySelectorAll('div[class^=\"medium-load\"]');\n" +
                    "    for (var i = 0; i < divs.length; i++) {\n" +
                    "      divs[i].parentNode.removeChild(divs[i]);\n" +
                    "    }\n" +
                    "  }, 500);\n" +
                    "  \n" +
                    "  return '耗时:' + (endTime - startTime).toFixed(2) + 'ms';\n" +
                    "})();";
                
                webView.evaluateJavascript(loadJs, loadResult -> {
                    // 不输出Toast，只记录日志
                    Log.d(TAG, "中负载JavaScript结果: " + loadResult);
                });
            }
        });

    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停JavaScript负载任务
        if (webView != null) {
            webView.evaluateJavascript("javascript: mediumLoadEnabled = false;", null);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复JavaScript负载任务
        if (webView != null) {
            webView.evaluateJavascript("javascript: mediumLoadEnabled = true; requestAnimationFrame(performMediumCalculation);", null);
        }
    }
    
    @Override
    protected void onDestroy() {
        // 停止JavaScript负载任务
        if (webView != null) {
            webView.evaluateJavascript("javascript: mediumLoadEnabled = false;", null);
        }
        super.onDestroy();
    }
} 