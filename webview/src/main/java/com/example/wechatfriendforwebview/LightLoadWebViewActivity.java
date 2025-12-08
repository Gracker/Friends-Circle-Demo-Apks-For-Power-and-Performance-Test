package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 轻负载WebView版朋友圈Activity
 * 不添加额外的负载，仅显示内容
 */
public class LightLoadWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "LightLoadWebView";
    
    // 处理器实例
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("LightLoadWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        // 设置标题
        setTitle("WebView朋友圈 - 轻负载");
        Trace.endSection();
    }

    /**
     * 执行负载任务，移除启动时的负载，确保快速加载
     */
    @Override
    protected void performLoadTask() {
        // 不执行任何负载，仅记录日志
        Log.d(TAG, "低负载模式 - 仅在滚动时执行负载");
    }
    
    /**
     * 执行轻量级监控，不添加额外负载
     */
    private void executeLightMonitoring() {
        Trace.beginSection("LightLoadWebView_executeLightMonitoring");
        
        if (webView != null) {
            webView.post(() -> {
                // 初始化轻负载JavaScript
                String initLightLoadJs = "javascript:" +
                        "var lightLoadEnabled = true;" +
                        "var frameCount = 0;" +
                        "var lastScrollY = 0;" +
                        "var isScrolling = false;" +
                        "var scrollCount = 0;" +
                        "var lightLoadStats = {" +
                        "  taskCount: 0," +
                        "  totalCalcTime: 0," +
                        "  maxCalcTime: 0," +
                        "  avgFps: 0," +
                        "  lastTimestamp: Date.now()," +
                        "  frames: 0" +
                        "};" +
                        // 监听滚动事件
                        "window.addEventListener('scroll', function() {" +
                        "  isScrolling = true;" +
                        "  scrollCount = 10;" + // 滚动后保持10帧的轻负载
                        "  lastScrollY = window.scrollY;" +
                        "});";
                webView.evaluateJavascript(initLightLoadJs, null);
                
                // 设置周期性执行的轻负载任务
                String setupLightLoadJs = "javascript:" +
                        "function performLightCalculation() {" +
                        "  if (!lightLoadEnabled) return;" +
                        "  frameCount++;" +
                        "  lightLoadStats.frames++;" +
                        "  " +
                        "  // 计算FPS" +
                        "  var now = Date.now();" +
                        "  var elapsed = now - lightLoadStats.lastTimestamp;" +
                        "  if (elapsed > 1000) {" +
                        "    lightLoadStats.avgFps = Math.round((lightLoadStats.frames * 1000) / elapsed);" +
                        "    lightLoadStats.lastTimestamp = now;" +
                        "    lightLoadStats.frames = 0;" +
                        "  }" +
                        "  " +
                        "  // 处理滚动计数器" +
                        "  if (scrollCount > 0) {" +
                        "    scrollCount--;" +
                        "  } else {" +
                        "    isScrolling = false;" +
                        "  }" +
                        "  " +
                        "  var startTime = performance.now();" +
                        "  " +
                        "  // 第一阶段：JavaScript计算，轻量级 (10倍提升)" +
                        "  var iterations = isScrolling ? 100000 : 50000;" + // 原来是1万/5千
                        "  " +
                        "  // 创建数组并执行轻量级计算" +
                        "  var data = new Array(iterations);" +
                        "  for (var i = 0; i < iterations; i++) {" +
                        "    data[i] = Math.sqrt(i) * Math.sin(i/10) + Math.log(i + 1);" +
                        "  }" +
                        "  " +
                        "  // 轻量级排序 - 不排序全部以保持轻负载特性" +
                        "  if (iterations > 60000) {" +
                        "    data.slice(0, 30000).sort((a, b) => a - b);" +
                        "  } else {" +
                        "    data.sort((a, b) => a - b);" +
                        "  }" +
                        "  " +
                        "  // 第二阶段：轻量级DOM操作" +
                        "  var items = document.querySelectorAll('.friend-circle-item');" +
                        "  var viewportHeight = window.innerHeight;" +
                        "  var visibleItems = [];" +
                        "  " +
                        "  // 仅对可见项目进行处理" +
                        "  for (var i = 0; i < items.length; i++) {" +
                        "    var rect = items[i].getBoundingClientRect();" +
                        "    if (rect.top < viewportHeight && rect.bottom > 0) {" +
                        "      visibleItems.push(items[i]);" +
                        "      " +
                        "      // 为每个可见项目进行轻量级的样式修改 (5次，原来是1次)" +
                        "      for (var round = 0; round < 5; round++) {" +
                        "        var phase = (frameCount + i) * 0.05;" +
                        "        " +
                        "        // 轻微样式修改" +
                        "        if (round === 0) { // 只在第一轮时修改主元素样式" +
                        "          items[i].style.borderRadius = (5 + Math.sin(phase) * 1) + 'px';" +
                        "          var dummy1 = items[i].offsetHeight; // 轻量级强制布局" +
                        "        }" +
                        "        " +
                        "        // 仅处理少量子元素" +
                        "        var children = items[i].querySelectorAll('img, .avatar, .nickname');" +
                        "        var limit = Math.min(children.length, 10); // 处理最多10个元素（原来是3个）" +
                        "        " +
                        "        for (var j = 0; j < limit; j++) {" +
                        "          var child = children[j];" +
                        "          " +
                        "          // 轻微调整透明度" +
                        "          child.style.opacity = 0.95 + Math.sin(phase + j) * 0.05;" +
                        "          " +
                        "          // 添加数据属性进行轻量级标记" +
                        "          child.setAttribute('data-light-processed', Date.now());" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "  " +
                        "  // 第三阶段：创建和管理少量临时元素" +
                        "  if (!window._lightTempElements) {" +
                        "    window._lightTempElements = [];" +
                        "  }" +
                        "  " +
                        "  // 移除过多的元素" +
                        "  if (window._lightTempElements.length > 20) { // 保留20个元素（原来是5个）" +
                        "    var elemsToRemove = window._lightTempElements.splice(0, 5);" +
                        "    for (var i = 0; i < elemsToRemove.length; i++) {" +
                        "      if (elemsToRemove[i].parentNode) {" +
                        "        elemsToRemove[i].parentNode.removeChild(elemsToRemove[i]);" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "  " +
                        "  // 创建10个新元素（原来是1个）" +
                        "  if (frameCount % 3 === 0) { // 每3帧创建一次" +
                        "    var tempFragment = document.createDocumentFragment();" +
                        "    for (var i = 0; i < 10; i++) {" +
                        "      var div = document.createElement('div');" +
                        "      div.style.position = 'fixed';" +
                        "      div.style.top = (Math.random() * 100) + '%';" +
                        "      div.style.left = (Math.random() * 100) + '%';" +
                        "      div.style.width = (5 + Math.random() * 15) + 'px';" +
                        "      div.style.height = (5 + Math.random() * 15) + 'px';" +
                        "      div.style.backgroundColor = 'rgba(200,200,200,0.05)';" +
                        "      div.style.borderRadius = '3px';" +
                        "      div.style.zIndex = '-1';" +
                        "      div.style.opacity = '0.01';" +
                        "      div.textContent = 'Light ' + Date.now();" +
                        "      tempFragment.appendChild(div);" +
                        "      window._lightTempElements.push(div);" +
                        "    }" +
                        "    document.body.appendChild(tempFragment);" +
                        "  }" +
                        "  " +
                        "  // 第四阶段：轻量级Canvas绘制 - 2个Canvas（原来是偶尔1个）" +
                        "  if (frameCount % 4 === 0) { // 每4帧执行一次" +
                        "    for (var canvasIdx = 0; canvasIdx < 2; canvasIdx++) {" +
                        "      var canvas = document.createElement('canvas');" +
                        "      canvas.width = window.innerWidth / 4;" +
                        "      canvas.height = window.innerHeight / 4;" +
                        "      canvas.style.position = 'fixed';" +
                        "      canvas.style.top = (canvasIdx * 50) + '%';" +
                        "      canvas.style.left = '0';" +
                        "      canvas.style.pointerEvents = 'none';" +
                        "      canvas.style.zIndex = '-2';" +
                        "      canvas.style.opacity = '0.01';" +
                        "      document.body.appendChild(canvas);" +
                        "      " +
                        "      var ctx = canvas.getContext('2d');" +
                        "      ctx.clearRect(0, 0, canvas.width, canvas.height);" +
                        "      " +
                        "      // 绘制30个简单形状（原来是10个）" +
                        "      for (var i = 0; i < 30; i++) {" +
                        "        ctx.fillStyle = 'rgba(150,150,150,0.1)';" +
                        "        ctx.beginPath();" +
                        "        ctx.arc(Math.random() * canvas.width, Math.random() * canvas.height, " +
                        "                3 + Math.random() * 8, 0, Math.PI * 2);" +
                        "        ctx.fill();" +
                        "      }" +
                        "      " +
                        "      // 延迟移除Canvas" +
                        "      (function(currentCanvas) {" +
                        "        setTimeout(function() {" +
                        "          if (currentCanvas.parentNode) {" +
                        "            currentCanvas.parentNode.removeChild(currentCanvas);" +
                        "          }" +
                        "        }, 100 + canvasIdx * 20);" +
                        "      })(canvas);" +
                        "    }" +
                        "  }" +
                        "  " +
                        "  // 记录统计信息" +
                        "  var calcTime = performance.now() - startTime;" +
                        "  lightLoadStats.taskCount++;" +
                        "  lightLoadStats.totalCalcTime += calcTime;" +
                        "  lightLoadStats.maxCalcTime = Math.max(lightLoadStats.maxCalcTime, calcTime);" +
                        "  " +
                        "  // 每5帧记录一次统计信息" +
                        "  if (frameCount % 5 === 0) {" +
                        "    console.log('轻负载统计: 任务次数=' + lightLoadStats.taskCount + " +
                        "                ', 平均耗时=' + (lightLoadStats.totalCalcTime/lightLoadStats.taskCount).toFixed(2) + 'ms' + " +
                        "                ', 最大耗时=' + lightLoadStats.maxCalcTime.toFixed(2) + 'ms' + " +
                        "                ', 平均FPS=' + lightLoadStats.avgFps + " +
                        "                ', 滚动状态=' + (isScrolling ? '滚动中' : '静止'));" +
                        "    " +
                        "    try {" +
                        "      Android.reportPerformance('light', lightLoadStats.avgFps, isScrolling);" +
                        "    } catch(e) {}" +
                        "  }" +
                        "  " +
                        "  // 使用requestAnimationFrame确保每帧执行一次" +
                        "  requestAnimationFrame(performLightCalculation);" +
                        "}" +
                        "" +
                        "// 启动轻负载任务" +
                        "requestAnimationFrame(performLightCalculation);";
                
                webView.evaluateJavascript(setupLightLoadJs, null);
            });
        }
        
        Trace.endSection();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停监控
        if (webView != null) {
            webView.evaluateJavascript("javascript: lightLoadEnabled = false;", null);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复监控
        if (webView != null) {
            webView.evaluateJavascript("javascript: lightLoadEnabled = true; requestAnimationFrame(performLightCalculation);", null);
        }
    }
    
    @Override
    protected void onDestroy() {
        // 停止监控
        if (webView != null) {
            webView.evaluateJavascript("javascript: lightLoadEnabled = false;", null);
        }
        super.onDestroy();
    }

    /**
     * 在fling过程中执行负载
     */
    @Override
    protected void executeFlingLoad() {
        // 轻微阻塞主线程
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad阻塞被中断", e);
        }
        
        // 执行同步JavaScript
        final String testJs = "\"测试成功\"";
        webView.evaluateJavascript(testJs, value -> {
            // 测试成功后直接执行负载
            if (value != null && !value.equals("null")) {
                // 低负载JavaScript
                String loadJs = 
                    "(function() {\n" +
                    "  var startTime = performance.now();\n" +
                    "  \n" +
                    "  // 轻微阻塞主线程\n" +
                    "  var blockStart = performance.now();\n" +
                    "  while(performance.now() - blockStart < 5) {\n" +
                    "    Math.random();\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 1. 创建少量样式表\n" +
                    "  for (var i = 0; i < 3; i++) {\n" +
                    "    var style = document.createElement('style');\n" +
                    "    style.id = 'lightLoadStyle' + i;\n" +
                    "    style.innerHTML = '.light-load-' + i + ' { color: green; font-size: ' + (i+8) + 'px; }';\n" +
                    "    document.head.appendChild(style);\n" +
                    "    document.body.offsetHeight;\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 2. 创建少量DOM元素\n" +
                    "  for (var i = 0; i < 2; i++) {\n" +
                    "    var div = document.createElement('div');\n" +
                    "    div.className = 'light-load-' + (i % 3);\n" +
                    "    div.style.width = (10 + i) + 'px';\n" +
                    "    div.style.height = (10 + i) + 'px';\n" +
                    "    div.style.position = 'absolute';\n" +
                    "    div.style.top = (i * 5) + 'px';\n" +
                    "    div.style.left = (i * 5) + 'px';\n" +
                    "    div.style.backgroundColor = 'rgba(' + i*3 + ',' + i*5 + ',' + i*2 + ',0.5)';\n" +
                    "    div.innerText = 'Light Load ' + i;\n" +
                    "    document.body.appendChild(div);\n" +
                    "    div.getBoundingClientRect();\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 3. 创建并绘制一个简单Canvas\n" +
                    "  var canvas = document.createElement('canvas');\n" +
                    "  canvas.width = 100;\n" +
                    "  canvas.height = 100;\n" +
                    "  var ctx = canvas.getContext('2d');\n" +
                    "  for (var j = 0; j < 5; j++) {\n" +
                    "    ctx.fillStyle = 'rgb(' + j*3 + ',' + j*5 + ',' + j*2 + ')';\n" +
                    "    ctx.fillRect(j*4, j*4, 20, 20);\n" +
                    "  }\n" +
                    "  document.body.appendChild(canvas);\n" +
                    "  canvas.getBoundingClientRect();\n" +
                    "  \n" +
                    "  // 4. 轻量计算\n" +
                    "  var result = 0;\n" +
                    "  for (var k = 0; k < 5000; k++) {\n" +
                    "    result += Math.sqrt(k) * Math.cos(k);\n" +
                    "  }\n" +
                    "  \n" +
                    "  var endTime = performance.now();\n" +
                    "  \n" +
                    "  // 移除创建的元素\n" +
                    "  setTimeout(function() {\n" +
                    "    var styles = document.querySelectorAll('style[id^=\"lightLoadStyle\"]');\n" +
                    "    for (var i = 0; i < styles.length; i++) {\n" +
                    "      styles[i].parentNode.removeChild(styles[i]);\n" +
                    "    }\n" +
                    "    var canvases = document.querySelectorAll('canvas');\n" +
                    "    for (var i = 0; i < canvases.length; i++) {\n" +
                    "      canvases[i].parentNode.removeChild(canvases[i]);\n" +
                    "    }\n" +
                    "    var divs = document.querySelectorAll('div[class^=\"light-load\"]');\n" +
                    "    for (var i = 0; i < divs.length; i++) {\n" +
                    "      divs[i].parentNode.removeChild(divs[i]);\n" +
                    "    }\n" +
                    "  }, 500);\n" +
                    "  \n" +
                    "  return '耗时:' + (endTime - startTime).toFixed(2) + 'ms';\n" +
                    "})();";
                
                webView.evaluateJavascript(loadJs, loadResult -> {
                    // 不输出Toast，只记录日志
                    Log.d(TAG, "低负载JavaScript结果: " + loadResult);
                });
            }
        });
    }
} 