package com.example.wechatfriendforwebview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

/**
 * 重负载WebView版朋友圈Activity
 * 添加高级别的额外负载任务，全部在WebView中执行
 */
public class HeavyLoadWebViewActivity extends BaseFriendCircleWebViewActivity {
    private static final String TAG = "HeavyLoadWebView";
    
    // 处理器实例
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("HeavyLoadWebViewActivity_onCreate");
        super.onCreate(savedInstanceState);
        // 设置标题
        setTitle("WebView朋友圈 - 重负载");
        Trace.endSection();
    }

    /**
     * 执行负载任务，移除启动时的负载，确保快速加载
     */
    @Override
    protected void performLoadTask() {
        // 不执行任何负载，仅记录日志
        Log.d(TAG, "重负载模式 - 仅在滚动时执行负载");
    }
    
    /**
     * 在fling过程中执行负载
     */
    @Override
    protected void executeFlingLoad() {
        // 阻塞主线程一小段时间
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            Log.e(TAG, "executeFlingLoad阻塞被中断", e);
        }
        
        // 执行同步JavaScript
        final String testJs = "\"测试成功\"";
        webView.evaluateJavascript(testJs, value -> {
            // 测试成功后直接执行负载
            if (value != null && !value.equals("null")) {
                // 重负载JavaScript
                String loadJs = 
                    "(function() {\n" +
                    "  var startTime = performance.now();\n" +
                    "  \n" +
                    "  // 1. 创建样式表\n" +
                    "  for (var i = 0; i < 8; i++) {\n" +
                    "    var style = document.createElement('style');\n" +
                    "    style.id = 'heavyLoadStyle' + i;\n" +
                    "    style.innerHTML = '.heavy-load-' + i + ' { color: red; font-size: ' + (i+10) + 'px; }';\n" +
                    "    document.head.appendChild(style);\n" +
                    "    document.body.offsetHeight;\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 2. 创建更多DOM元素并强制布局\n" +
                    "  for (var i = 0; i < 6; i++) {\n" +
                    "    var div = document.createElement('div');\n" +
                    "    div.className = 'heavy-load-' + (i % 10);\n" +
                    "    div.style.width = (15 + i) + 'px';\n" +
                    "    div.style.height = (15 + i) + 'px';\n" +
                    "    div.style.position = 'absolute';\n" +
                    "    div.style.top = (i * 8) + 'px';\n" +
                    "    div.style.left = (i * 8) + 'px';\n" +
                    "    div.style.backgroundColor = 'rgba(' + i*5 + ',' + i*8 + ',' + i*3 + ',0.7)';\n" +
                    "    div.innerText = 'Heavy Load ' + i;\n" +
                    "    document.body.appendChild(div);\n" +
                    "    div.getBoundingClientRect();\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 3. 创建并绘制Canvas\n" +
                    "  for (var c = 0; c < 3; c++) {\n" +
                    "    var canvas = document.createElement('canvas');\n" +
                    "    canvas.width = 300;\n" +
                    "    canvas.height = 300;\n" +
                    "    var ctx = canvas.getContext('2d');\n" +
                    "    for (var j = 0; j < 20; j++) {\n" +
                    "      ctx.fillStyle = 'rgb(' + j*5 + ',' + j*8 + ',' + j*3 + ')';\n" +
                    "      ctx.fillRect(j*6, j*6, 40, 40);\n" +
                    "    }\n" +
                    "    document.body.appendChild(canvas);\n" +
                    "    canvas.getBoundingClientRect();\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 4. 耗时计算\n" +
                    "  var result = 0;\n" +
                    "  for (var k = 0; k < 200; k++) {\n" +
                    "    result += Math.sqrt(k) * Math.cos(k) * Math.sin(k);\n" +
                    "  }\n" +
                    "  \n" +
                    "  var endTime = performance.now();\n" +
                    "  \n" +
                    "  // 移除创建的元素\n" +
                    "  setTimeout(function() {\n" +
                    "    var styles = document.querySelectorAll('style[id^=\"heavyLoadStyle\"]');\n" +
                    "    for (var i = 0; i < styles.length; i++) {\n" +
                    "      styles[i].parentNode.removeChild(styles[i]);\n" +
                    "    }\n" +
                    "    var canvases = document.querySelectorAll('canvas');\n" +
                    "    for (var i = 0; i < canvases.length; i++) {\n" +
                    "      canvases[i].parentNode.removeChild(canvases[i]);\n" +
                    "    }\n" +
                    "    var divs = document.querySelectorAll('div[class^=\"heavy-load\"]');\n" +
                    "    for (var i = 0; i < divs.length; i++) {\n" +
                    "      divs[i].parentNode.removeChild(divs[i]);\n" +
                    "    }\n" +
                    "  }, 500);\n" +
                    "  \n" +
                    "  return '耗时:' + (endTime - startTime).toFixed(2) + 'ms';\n" +
                    "})();";
                
                webView.evaluateJavascript(loadJs, loadResult -> {
                    // 不输出Toast，只记录日志
                    Log.d(TAG, "重负载JavaScript结果: " + loadResult);
                });
            }
        });
        
    }
    
    /**
     * 执行实际的fling重负载
     */
    private void executeRealFlingLoad() {
        // 非常简单的重负载JavaScript，没有try-catch、没有函数调用，直接执行计算
        String simpleFlingJs = "javascript:" +
                "var startTime = performance.now();" +
                "var sum = 0;" +
                "for (var i = 0; i < 1000000; i++) {" +
                "  sum += Math.sqrt(i);" +
                "}" +
                "var endTime = performance.now();" +
                "var elapsed = endTime - startTime;" +
                "try {" +
                "  Android.showToast('重负载:' + elapsed.toFixed(0) + 'ms');" +
                "} catch(e) {}" +
                "elapsed.toFixed(2)";
        
        webView.evaluateJavascript(simpleFlingJs, value -> {
            Log.d(TAG, "重负载JavaScript返回值: " + value);
        });
        
        Log.d(TAG, "已触发简化版重负载JavaScript");
    }
    
    /**
     * 执行重负载JavaScript任务
     */
    private void executeHeavyJavaScriptLoad() {
        Trace.beginSection("HeavyLoadWebView_executeHeavyJavaScriptLoad");
        
        if (webView != null) {
            webView.post(() -> {
                // 初始化重负载JavaScript
                String initHeavyLoadJs = "javascript:" +
                        "var heavyLoadEnabled = true;" +
                        "var frameCount = 0;" +
                        "var lastScrollY = 0;" +
                        "var lastTime = Date.now();" +
                        "var isScrolling = false;" +
                        "var scrollCount = 0;" +
                        "var heavyLoadStats = {" +
                        "  taskCount: 0," +
                        "  totalCalcTime: 0," +
                        "  maxCalcTime: 0," +
                        "  avgFps: 0," +
                        "  lastTimestamp: Date.now()," +
                        "  frames: 0" +
                        "};" +
                        // 增强滚动检测，确保立即响应用户滑动
                        "var lastTouchY = 0;" +
                        "var touchScrollDetected = false;" +
                        // 触摸开始时记录位置
                        "window.addEventListener('touchstart', function(e) {" +
                        "  lastTouchY = e.touches[0].clientY;" +
                        "});" +
                        // 触摸移动时检测滑动
                        "window.addEventListener('touchmove', function(e) {" +
                        "  var currentY = e.touches[0].clientY;" +
                        "  if (Math.abs(currentY - lastTouchY) > 5) {" +
                        "    isScrolling = true;" +
                        "    touchScrollDetected = true;" +
                        "    scrollCount = 5;" + // 只保持5帧，滑动结束后快速恢复
                        "  }" +
                        "  lastTouchY = currentY;" +
                        "});" +
                        // 标准滚动事件监听
                        "window.addEventListener('scroll', function() {" +
                        "  isScrolling = true;" +
                        "  scrollCount = 5;" + // 只保持5帧，滑动结束后快速恢复
                        "  lastScrollY = window.scrollY;" +
                        "});";
                webView.evaluateJavascript(initHeavyLoadJs, null);
                
                // 设置周期性执行的重负载任务
                String setupHeavyLoadJs = "javascript:" +
                        "function performHeavyCalculation() {" +
                        "  if (!heavyLoadEnabled) return;" +
                        "  frameCount++;" +
                        "  heavyLoadStats.frames++;" +
                        "  " +
                        "  // 计算FPS" +
                        "  var now = Date.now();" +
                        "  var elapsed = now - heavyLoadStats.lastTimestamp;" +
                        "  if (elapsed > 1000) {" +
                        "    heavyLoadStats.avgFps = Math.round((heavyLoadStats.frames * 1000) / elapsed);" +
                        "    heavyLoadStats.lastTimestamp = now;" +
                        "    heavyLoadStats.frames = 0;" +
                        "  }" +
                        "  " +
                        "  // 处理滚动计数器" +
                        "  if (scrollCount > 0) {" +
                        "    scrollCount--;" +
                        "    // 只在滚动状态下执行负载操作" +
                        "    executeHeavyScrollLoad();" +
                        "  } else {" +
                        "    isScrolling = false;" +
                        "    touchScrollDetected = false;" +
                        "    // 静止状态不执行任何负载" +
                        "  }" +
                        "  " +
                        "  // 使用requestAnimationFrame确保下一帧继续执行" +
                        "  requestAnimationFrame(performHeavyCalculation);" +
                        "}" +
                        "" +
                        "// 滚动时执行的超重负载" +
                        "function executeHeavyScrollLoad() {" +
                        "  var startTime = performance.now();" +
                        "  " +
                        "  // 第一阶段：创建大量样式规则，迫使样式系统超负荷工作" +
                        "  if (!window._heavyStyleSheets) {" +
                        "    window._heavyStyleSheets = [];" +
                        "    // 一次性创建样式表，数量再增加5倍" +
                        "    for (var i = 0; i < 1000; i++) {" + // 从500增加到1000
                        "      var styleSheet = document.createElement('style');" +
                        "      document.head.appendChild(styleSheet);" +
                        "      window._heavyStyleSheets.push(styleSheet);" +
                        "    }" +
                        "  }" +
                        "  " +
                        "  // 滚动时强制更新所有样式表" +
                        "  var styleUpdateCount = Math.min(window._heavyStyleSheets.length, 500);" + // 从300增加到500
                        "  var phaseOffset = frameCount * 0.1;" +
                        "  for (var i = 0; i < styleUpdateCount; i++) {" +
                        "    var sheet = window._heavyStyleSheets[i];" +
                        "    var cssText = '';" +
                        "    // 为每个样式表创建规则，数量增加" +
                        "    var rulesPerSheet = 40;" + // 从25增加到40
                        "    for (var r = 0; r < rulesPerSheet; r++) {" +
                        "      var selector = '.friend-circle-item:nth-child(' + ((i * rulesPerSheet + r) % 30 + 1) + ') ';" +
                        "      var hue = (phaseOffset + i * 10 + r * 5) % 360;" +
                        "      var translateX = Math.sin(phaseOffset + i * 0.1 + r * 0.05) * 15;" + // 增加变换幅度
                        "      var translateY = Math.cos(phaseOffset + i * 0.1 + r * 0.05) * 15;" +
                        "      var scale = 1 + Math.sin(phaseOffset + i * 0.1) * 0.3;" + // 增加缩放幅度
                        "      var rotate = Math.sin(phaseOffset + i * 0.1) * 15;" + // 增加旋转幅度
                        "      var opacity = 0.8 + Math.sin(phaseOffset + i * 0.1) * 0.2;" +
                        "      " +
                        "      cssText += selector + ' { " +
                        "        box-shadow: ' + Math.abs(Math.sin(phaseOffset + i)) * 30 + 'px ' + " + // 增加阴影复杂度
                        "                     Math.abs(Math.cos(phaseOffset + i)) * 30 + 'px ' + " +
                        "                     Math.abs(Math.sin(phaseOffset + i)) * 60 + 'px rgba(' + " +
                        "                     Math.floor(Math.random() * 255) + ',' + " +
                        "                     Math.floor(Math.random() * 255) + ',' + " +
                        "                     Math.floor(Math.random() * 255) + ',0.3); " +
                        "        transform: translate(' + translateX + 'px, ' + translateY + 'px) " +
                        "                   scale(' + scale + ') " +
                        "                   rotate(' + rotate + 'deg); " +
                        "        filter: blur(' + Math.abs(Math.sin(phaseOffset + i * 0.2)) * 1.5 + 'px) " + // 增加滤镜复杂度
                        "                brightness(' + (0.8 + Math.sin(phaseOffset + i * 0.1) * 0.2) + ') " +
                        "                contrast(' + (1 + Math.sin(phaseOffset + i * 0.15) * 0.3) + '); " +
                        "        opacity: ' + opacity + '; " +
                        "      } ';" +
                        "    }" +
                        "    sheet.textContent = cssText;" +
                        "    // 强制布局重新计算，这会极大加重CrRendererMain负担" +
                        "    var forceCalculate = document.body.offsetHeight;" +
                        "  }" +
                        "  " +
                        "  // 第二阶段：严重布局抖动（Layout Thrashing）" +
                        "  // 这种操作对CrRendererMain线程影响最大" +
                        "  var items = document.querySelectorAll('.friend-circle-item');" +
                        "  var measurements = [];" +
                        "  " +
                        "  // 每个元素重复多次强制布局计算，极大增加CrRendererMain负载" +
                        "  for (var k = 0; k < 5; k++) {" + // 增加一个外循环，将整个布局抖动操作重复5次
                        "    // 第一轮：读取所有元素的布局信息" +
                        "    for (var i = 0; i < items.length; i++) {" +
                        "      var rect = items[i].getBoundingClientRect(); // 读取布局信息" +
                        "      measurements.push({" +
                        "        width: rect.width," +
                        "        height: rect.height," +
                        "        top: rect.top," +
                        "        left: rect.left" +
                        "      });" +
                        "    }" +
                        "    " +
                        "    // 第二轮：多次修改和读取，制造严重的布局抖动" +
                        "    for (var j = 0; j < 15; j++) {" + // 从8增加到15次循环，大幅增加布局抖动次数
                        "      for (var i = 0; i < items.length; i++) {" +
                        "        var phase = (frameCount * 0.1 + i * 0.05 + j * 0.2) % 10;" +
                        "        " +
                        "        // 修改元素宽度" +
                        "        items[i].style.width = (measurements[i].width + Math.sin(phase) * 30) + 'px';" + // 增加变化幅度
                        "        // 强制读取布局，触发布局重计算" +
                        "        var dummy1 = items[i].offsetWidth;" +
                        "        " +
                        "        // 修改元素高度" +
                        "        items[i].style.height = (measurements[i].height + Math.cos(phase) * 30) + 'px';" + // 增加变化幅度
                        "        // 再次强制读取，触发又一次布局重计算" +
                        "        var dummy2 = items[i].offsetHeight;" +
                        "        " +
                        "        // 修改元素边距" +
                        "        items[i].style.margin = (Math.sin(phase) * 15 + 20) + 'px';" + // 增加变化幅度
                        "        // 强制读取，又一次触发布局重计算" +
                        "        var dummy3 = items[i].getBoundingClientRect();" +
                        "        " +
                        "        // 修改元素填充" +
                        "        items[i].style.padding = (Math.cos(phase) * 15 + 20) + 'px';" + // 增加变化幅度
                        "        // 再次强制读取" +
                        "        var dummy4 = window.getComputedStyle(items[i]).padding;" +
                        "        " +
                        "        // 修改元素位置" +
                        "        items[i].style.transform = \"translate(\" + Math.sin(phase) * 15 + \"px, \" + Math.cos(phase) * 15 + \"px) scale(\" + (1 + Math.sin(phase * 0.5) * 0.3) + \")\";" +
                        "        var dummy5 = window.getComputedStyle(items[i]).transform;" +
                        "        " +
                        "        // 添加背景颜色变化" +
                        "        var r = Math.floor(128 + Math.sin(phase) * 127);" +
                        "        var g = Math.floor(128 + Math.sin(phase + 2) * 127);" +
                        "        var b = Math.floor(128 + Math.sin(phase + 4) * 127);" +
                        "        items[i].style.backgroundColor = 'rgba(' + r + ',' + g + ',' + b + ',0.05)';" +
                        "        var dummy6 = window.getComputedStyle(items[i]).backgroundColor;" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "  " +
                        "  // 第三阶段：JavaScript计算负载" +
                        "  var iterations = 25000000;" + // 从10000000增加到25000000
                        "  var data = new Array(iterations);" +
                        "  for (var i = 0; i < iterations; i++) {" +
                        "    data[i] = Math.sqrt(i * Math.PI) * Math.sin(i) / Math.cos(i * 0.5) + Math.tan(i * 0.2) + Math.log(i + 1) * Math.sqrt(i * 0.1);" +
                        "  }" +
                        "  " +
                        "  // 增加更多复杂计算" +
                        "  data.sort((a, b) => a - b);" +
                        "  " +
                        "  // 再次执行布局操作" +
                        "  var items = document.querySelectorAll('.friend-circle-item');" +
                        "  for (var i = 0; i < items.length; i++) {" +
                        "    items[i].style.boxShadow = (1 + Math.sin(frameCount * 0.1 + i * 0.1)) * 30 + 'px ' + " + // 增加阴影复杂度
                        "                             (1 + Math.cos(frameCount * 0.1 + i * 0.1)) * 30 + 'px ' + " +
                        "                             '45px rgba(0,0,0,0.3)';" +
                        "    items[i].classList.toggle('heavy-frame-' + frameCount % 30);" + // 增加类名切换数量
                        "    " +
                        "    // 对每个元素的所有子元素应用变换" +
                        "    var children = items[i].querySelectorAll('*');" +
                        "    for (var c = 0; c < Math.min(children.length, 100); c++) {" + // 处理更多子元素
                        "      var phase = (frameCount * 0.1 + i * 0.05 + c * 0.02) % 10;" +
                        "      children[c].style.transform = 'translate(' + Math.sin(phase) * 10 + 'px, ' + Math.cos(phase) * 10 + 'px)';" +
                        "      var dummy = children[c].offsetWidth;" + // 强制布局重计算
                        "    }" +
                        "    " +
                        "    var dummy = items[i].offsetHeight * items[i].offsetWidth;" + // 强制布局计算
                        "  }" +
                        "  " +
                        "  // 第四阶段：DOM操作和Canvas绘制" +
                        "  // DOM元素操作" +
                        "  if (!window._heavyTempElements) {" +
                        "    window._heavyTempElements = [];" +
                        "  }" +
                        "  " +
                        "  // 删除旧元素" +
                        "  if (window._heavyTempElements.length > 1000) {" + // 从500增加到1000
                        "    var toRemove = window._heavyTempElements.splice(0, 800);" + // 从300增加到800
                        "    for (var i = 0; i < toRemove.length; i++) {" +
                        "      if (toRemove[i].parentNode) {" +
                        "        toRemove[i].parentNode.removeChild(toRemove[i]);" +
                        "        // 每10个元素强制一次布局计算" +
                        "        if (i % 10 === 0) {" + // 从20降低到10，增加强制计算次数
                        "          var dummyRemove = document.body.offsetHeight;" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "  " +
                        "  // 创建新元素" +
                        "  var fragment = document.createDocumentFragment();" +
                        "  for (var i = 0; i < 800; i++) {" + // 从300增加到800
                        "    var div = document.createElement('div');" +
                        "    div.className = 'heavy-dynamic-' + (frameCount % 20);" + // 增加类名变化周期
                        "    div.style.position = 'fixed';" +
                        "    div.style.top = (Math.random() * 100) + '%';" +
                        "    div.style.left = (Math.random() * 100) + '%';" +
                        "    div.style.width = (20 + Math.random() * 80) + 'px';" + // 增加尺寸变化范围
                        "    div.style.height = (20 + Math.random() * 80) + 'px';" +
                        "    div.style.backgroundColor = 'rgba(' + Math.floor(Math.random() * 255) + ',' + " +
                        "                             Math.floor(Math.random() * 255) + ',' + " +
                        "                             Math.floor(Math.random() * 255) + ',0.1)';" +
                        "    div.style.borderRadius = (5 + Math.random() * 40) + 'px';" + // 增加边框圆角变化范围
                        "    div.style.boxShadow = '0 0 ' + (10 + Math.random() * 40) + 'px rgba(0,0,0,0.2)';" + // 增加阴影变化范围
                        "    div.style.transform = 'rotate(' + (Math.random() * 360) + 'deg) scale(' + (0.5 + Math.random() * 1.5) + ')';" +
                        "    div.style.transition = 'all 0.5s ease';" +
                        "    div.style.zIndex = '-1';" +
                        "    div.style.opacity = '0.03';" + // 保持低透明度，减少视觉干扰
                        "    " +
                        "    // 添加内部内容增加复杂度" +
                        "    for (var j = 0; j < 3; j++) {" + // 每个元素添加3个子元素
                        "      var innerDiv = document.createElement('div');" +
                        "      innerDiv.style.width = (50 + Math.random() * 50) + '%';" +
                        "      innerDiv.style.height = (50 + Math.random() * 50) + '%';" +
                        "      innerDiv.style.backgroundColor = 'rgba(' + Math.floor(Math.random() * 255) + ',' + " +
                        "                                    Math.floor(Math.random() * 255) + ',' + " +
                        "                                    Math.floor(Math.random() * 255) + ',0.2)';" +
                        "      innerDiv.style.position = 'absolute';" +
                        "      innerDiv.style.top = (Math.random() * 50) + '%';" +
                        "      innerDiv.style.left = (Math.random() * 50) + '%';" +
                        "      div.appendChild(innerDiv);" +
                        "    }" +
                        "    " +
                        "    fragment.appendChild(div);" +
                        "    window._heavyTempElements.push(div);" +
                        "  }" +
                        "  document.body.appendChild(fragment);" +
                        "  // 强制布局重计算" +
                        "  var dummyBody = document.body.offsetHeight;" +
                        "  " +
                        "  // Canvas绘制" +
                        "  for (var canvasIdx = 0; canvasIdx < 50; canvasIdx++) {" + // 从30增加到50
                        "    var canvas = document.createElement('canvas');" +
                        "    canvas.width = window.innerWidth;" +
                        "    canvas.height = window.innerHeight;" +
                        "    canvas.style.position = 'fixed';" +
                        "    canvas.style.top = (canvasIdx * 3.33) + '%';" + // 分布更密集
                        "    canvas.style.left = '0';" +
                        "    canvas.style.pointerEvents = 'none';" +
                        "    canvas.style.zIndex = '-2';" +
                        "    canvas.style.opacity = '0.01';" + // 保持低透明度，减少视觉干扰
                        "    document.body.appendChild(canvas);" +
                        "    " +
                        "    var ctx = canvas.getContext('2d');" +
                        "    ctx.clearRect(0, 0, canvas.width, canvas.height);" +
                        "    " +
                        "    // 绘制大量复杂形状" +
                        "    for (var i = 0; i < 2000; i++) {" + // 从1000增加到2000
                        "      // 创建复杂渐变" +
                        "      var gradient;" +
                        "      if (i % 2 === 0) {" +
                        "        gradient = ctx.createRadialGradient(" +
                        "          Math.random() * canvas.width, Math.random() * canvas.height, 5," +
                        "          Math.random() * canvas.width, Math.random() * canvas.height, 100" + // 增加渐变半径
                        "        );" +
                        "      } else {" +
                        "        gradient = ctx.createLinearGradient(" +
                        "          Math.random() * canvas.width, Math.random() * canvas.height," +
                        "          Math.random() * canvas.width, Math.random() * canvas.height" +
                        "        );" +
                        "      }" +
                        "      " +
                        "      // 添加更多渐变色标" +
                        "      for (var s = 0; s < 8; s++) {" + // 从5增加到8
                        "        gradient.addColorStop(s / 7, " +
                        "          'rgba(' + Math.floor(Math.random() * 255) + ',' + " +
                        "                 Math.floor(Math.random() * 255) + ',' + " +
                        "                 Math.floor(Math.random() * 255) + ',' + " +
                        "                 (0.1 + Math.random() * 0.8) + ')');" +
                        "      }" +
                        "      " +
                        "      // 设置更复杂阴影" +
                        "      ctx.shadowBlur = Math.random() * 100;" + // 从50增加到100
                        "      ctx.shadowColor = 'rgba(' + Math.floor(Math.random() * 255) + ',' + " +
                        "                       Math.floor(Math.random() * 255) + ',' + " +
                        "                       Math.floor(Math.random() * 255) + ',0.8)';" +
                        "      ctx.shadowOffsetX = Math.random() * 40 - 20;" + // 从20增加到40
                        "      ctx.shadowOffsetY = Math.random() * 40 - 20;" +
                        "      " +
                        "      // 设置填充样式" +
                        "      ctx.fillStyle = gradient;" +
                        "      ctx.strokeStyle = 'rgba(' + Math.floor(Math.random() * 255) + ',' + " +
                        "                        Math.floor(Math.random() * 255) + ',' + " +
                        "                        Math.floor(Math.random() * 255) + ',0.8)';" +
                        "      ctx.lineWidth = 1 + Math.random() * 10;" + // 增加线宽
                        "      " +
                        "      // 绘制复杂形状" +
                        "      switch (i % 6) {" + // 增加形状种类
                        "        case 0: // 圆形" +
                        "          ctx.beginPath();" +
                        "          ctx.arc(" +
                        "            Math.random() * canvas.width, " +
                        "            Math.random() * canvas.height, " +
                        "            5 + Math.random() * 80, " + // 增加半径变化范围
                        "            0, Math.PI * 2" +
                        "          );" +
                        "          ctx.fill();" +
                        "          ctx.stroke();" +
                        "          break;" +
                        "        case 1: // 星形" +
                        "          ctx.beginPath();" +
                        "          var spikes = 5 + Math.floor(Math.random() * 10);" + // 增加尖刺数量
                        "          var outerRadius = 30 + Math.random() * 80;" + // 增加半径变化范围
                        "          var innerRadius = 15 + Math.random() * 40;" +
                        "          var centerX = Math.random() * canvas.width;" +
                        "          var centerY = Math.random() * canvas.height;" +
                        "          var rot = Math.PI / 2 * 3;" +
                        "          var step = Math.PI / spikes;" +
                        "          " +
                        "          ctx.moveTo(centerX, centerY - outerRadius);" +
                        "          for (var j = 0; j < spikes; j++) {" +
                        "            var x = centerX + Math.cos(rot) * outerRadius;" +
                        "            var y = centerY + Math.sin(rot) * outerRadius;" +
                        "            ctx.lineTo(x, y);" +
                        "            rot += step;" +
                        "            " +
                        "            x = centerX + Math.cos(rot) * innerRadius;" +
                        "            y = centerY + Math.sin(rot) * innerRadius;" +
                        "            ctx.lineTo(x, y);" +
                        "            rot += step;" +
                        "          }" +
                        "          ctx.lineTo(centerX, centerY - outerRadius);" +
                        "          ctx.closePath();" +
                        "          ctx.fill();" +
                        "          ctx.stroke();" +
                        "          break;" +
                        "        case 2: // 贝塞尔曲线" +
                        "          ctx.beginPath();" +
                        "          var x = Math.random() * canvas.width;" +
                        "          var y = Math.random() * canvas.height;" +
                        "          ctx.moveTo(x, y);" +
                        "          " +
                        "          for (var j = 0; j < 8; j++) {" + // 增加曲线复杂度
                        "            var cp1x = x + Math.random() * 200 - 100;" + // 增加控制点距离
                        "            var cp1y = y + Math.random() * 200 - 100;" +
                        "            var cp2x = x + Math.random() * 200 - 100;" +
                        "            var cp2y = y + Math.random() * 200 - 100;" +
                        "            var nx = x + Math.random() * 200 - 100;" +
                        "            var ny = y + Math.random() * 200 - 100;" +
                        "            " +
                        "            ctx.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, nx, ny);" +
                        "            " +
                        "            x = nx;" +
                        "            y = ny;" +
                        "          }" +
                        "          " +
                        "          ctx.closePath();" +
                        "          ctx.fill();" +
                        "          ctx.stroke();" +
                        "          break;" +
                        "        case 3: // 矩形网格" +
                        "          for (var j = 0; j < 20; j++) {" + // 增加网格密度
                        "            for (var k = 0; k < 20; k++) {" +
                        "              ctx.fillRect(" +
                        "                Math.random() * canvas.width, " +
                        "                Math.random() * canvas.height, " +
                        "                5 + Math.random() * 30, " + // 增加矩形尺寸变化范围
                        "                5 + Math.random() * 30" +
                        "              );" +
                        "              ctx.strokeRect(" +
                        "                Math.random() * canvas.width, " +
                        "                Math.random() * canvas.height, " +
                        "                5 + Math.random() * 30, " +
                        "                5 + Math.random() * 30" +
                        "              );" +
                        "            }" +
                        "          }" +
                        "          break;" +
                        "        case 4: // 文本渲染" +
                        "          ctx.font = (10 + Math.random() * 60) + 'px Arial';" + // 增加字体大小变化范围
                        "          for (var j = 0; j < 10; j++) {" + // 增加文本数量
                        "            ctx.fillText('Heavy Load ' + frameCount + '-' + i + '-' + j, " +
                        "                       Math.random() * canvas.width, " +
                        "                       Math.random() * canvas.height);" +
                        "            ctx.strokeText('Heavy Load ' + frameCount + '-' + i + '-' + j, " +
                        "                         Math.random() * canvas.width, " +
                        "                         Math.random() * canvas.height);" +
                        "          }" +
                        "          break;" +
                        "        case 5: // 复杂路径" +
                        "          ctx.beginPath();" +
                        "          var points = 10 + Math.floor(Math.random() * 10);" +
                        "          var centerX = Math.random() * canvas.width;" +
                        "          var centerY = Math.random() * canvas.height;" +
                        "          var radius = 20 + Math.random() * 80;" +
                        "          for (var j = 0; j < points; j++) {" +
                        "            var angle = j * 2 * Math.PI / points;" +
                        "            var r = radius * (0.5 + Math.random() * 0.5);" +
                        "            var x = centerX + r * Math.cos(angle);" +
                        "            var y = centerY + r * Math.sin(angle);" +
                        "            if (j === 0) ctx.moveTo(x, y);" +
                        "            else ctx.lineTo(x, y);" +
                        "          }" +
                        "          ctx.closePath();" +
                        "          ctx.fill();" +
                        "          ctx.stroke();" +
                        "          break;" +
                        "      }" +
                        "    }" +
                        "    " +
                        "    // 短时间后删除Canvas" +
                        "    (function(currentCanvas) {" +
                        "      setTimeout(function() {" +
                        "        if (currentCanvas.parentNode) {" +
                        "          currentCanvas.parentNode.removeChild(currentCanvas);" +
                        "          // 强制布局重计算" +
                        "          var dummyCanvas = document.body.offsetHeight;" +
                        "        }" +
                        "      }, 100 + canvasIdx * 10);" +
                        "    })(canvas);" +
                        "  }" +
                        "  " +
                        "  // 特别增加更多的强制布局计算，作为最后一击" +
                        "  for (var forceLayout = 0; forceLayout < 50; forceLayout++) {" +
                        "    document.body.style.padding = (forceLayout % 5) + 'px';" +
                        "    var forceDummy = document.body.offsetHeight;" +
                        "    document.body.style.margin = (forceLayout % 5) + 'px';" +
                        "    forceDummy = document.body.offsetWidth;" +
                        "  }" +
                        "  " +
                        "  // 记录统计信息" +
                        "  var calcTime = performance.now() - startTime;" +
                        "  heavyLoadStats.taskCount++;" +
                        "  heavyLoadStats.totalCalcTime += calcTime;" +
                        "  heavyLoadStats.maxCalcTime = Math.max(heavyLoadStats.maxCalcTime, calcTime);" +
                        "  " +
                        "  // 输出统计信息" +
                        "  if (frameCount % 3 === 0) {" + // 保持每3帧输出一次统计
                        "    console.log('重负载统计: 任务次数=' + heavyLoadStats.taskCount + " +
                        "                ', 平均耗时=' + (heavyLoadStats.totalCalcTime/heavyLoadStats.taskCount).toFixed(2) + 'ms' + " +
                        "                ', 最大耗时=' + heavyLoadStats.maxCalcTime.toFixed(2) + 'ms' + " +
                        "                ', 平均FPS=' + heavyLoadStats.avgFps + " +
                        "                ', 滚动状态=' + (isScrolling ? '滚动中' : '静止'));" +
                        "    " +
                        "    try {" +
                        "      Android.reportPerformance('heavy', heavyLoadStats.avgFps, isScrolling);" +
                        "    } catch(e) {}" +
                        "  }" +
                        "}" +
                        "" +
                        "// 启动性能监测任务（不执行负载，只在滚动时执行负载）" +
                        "requestAnimationFrame(performHeavyCalculation);";
                
                webView.evaluateJavascript(setupHeavyLoadJs, null);
            });
        }
        
        Trace.endSection();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停JavaScript负载任务
        if (webView != null) {
            webView.evaluateJavascript("javascript: heavyLoadEnabled = false;", null);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 恢复JavaScript负载任务
        if (webView != null) {
            webView.evaluateJavascript("javascript: heavyLoadEnabled = true; requestAnimationFrame(performHeavyCalculation);", null);
        }
    }
    
    @Override
    protected void onDestroy() {
        // 停止JavaScript负载任务
        if (webView != null) {
            webView.evaluateJavascript("javascript: heavyLoadEnabled = false;", null);
        }
        super.onDestroy();
    }
} 