precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;

// 简单的矩形绘制函数
float rect(vec2 p, vec2 pos, vec2 size) {
    vec2 d = abs(p - pos) - size;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

// 圆角矩形
float roundedRect(vec2 p, vec2 pos, vec2 size, float radius) {
    vec2 d = abs(p - pos) - size + radius;
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - radius;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 背景色 - 深灰色
    vec3 bgColor = vec3(0.1, 0.1, 0.12);

    // 添加细微的网格效果
    float grid = step(0.98, fract(uv.x * 50.0)) + step(0.98, fract(uv.y * 50.0));
    bgColor += vec3(0.02) * grid;

    vec3 color = bgColor;

    // 顶部装饰条
    vec2 topBarUV = uv - vec2(0.5, 0.9);
    if (abs(topBarUV.x) < 0.3 && abs(topBarUV.y) < 0.01) {
        color = mix(color, vec3(0.2, 0.6, 1.0), 0.8);
    }

    // 中心区域的矩形（模拟Logo位置）
    vec2 logoUV = uv - vec2(0.5, 0.75);
    float logoDist = rect(logoUV, vec2(0.0), vec2(0.15, 0.05));
    if (logoDist < 0.0) {
        // Logo背景
        color = mix(color, vec3(0.8, 0.8, 0.8), 0.9);
    }

    // 进度条背景
    vec2 barUV = uv - vec2(0.5, 0.5);
    float barBG = roundedRect(barUV, vec2(0.0), vec2(0.3, 0.015), 0.005);
    if (barBG < 0.0) {
        color = mix(color, vec3(0.2, 0.2, 0.25), 0.8);
    }

    // 进度条填充
    float progressWidth = 0.3 * uProgress;
    vec2 fillUV = uv - vec2(0.5 - 0.3 + progressWidth, 0.5);
    float barFill = roundedRect(fillUV, vec2(0.0), vec2(progressWidth, 0.015), 0.005);
    if (barFill < 0.0) {
        // 渐变色从红到绿
        vec3 progressColor = mix(vec3(1.0, 0.3, 0.1), vec3(0.1, 0.8, 0.2), uProgress);
        // 添加发光效果
        float glow = 1.0 - abs(barUV.y) / 0.015;
        progressColor += progressColor * glow * 0.3 * (0.9 + 0.1 * sin(uTime * 3.0));
        color = mix(color, progressColor, 1.0);
    }

    // 进度条边框
    float border = roundedRect(barUV, vec2(0.0), vec2(0.305, 0.02), 0.007);
    if (border < 0.0 && barBG > 0.0 && barFill > 0.0) {
        color = mix(color, vec3(0.3, 0.3, 0.35), 0.8);
    }

    // 百分比显示区域（用方块代表数字）
    if (uProgress > 0.01) {
        vec2 pctUV = uv - vec2(0.5, 0.4);
        float pctDigits = floor(uProgress * 100.0);

        // 绘制简单的百分比指示器
        if (abs(pctUV.x) < 0.05 && abs(pctUV.y) < 0.02) {
            color = mix(color, vec3(1.0, 1.0, 1.0), 0.9);
        }
        // 百分号
        vec2 pctSymbolUV = uv - vec2(0.58, 0.4);
        if (abs(pctSymbolUV.x) < 0.01 && abs(pctSymbolUV.y) < 0.015) {
            color = mix(color, vec3(1.0, 1.0, 1.0), 0.9);
        }
        pctSymbolUV = uv - vec2(0.6, 0.4);
        if (abs(pctSymbolUV.x) < 0.01 && abs(pctSymbolUV.y) < 0.015) {
            color = mix(color, vec3(1.0, 1.0, 1.0), 0.9);
        }
    }

    // 底部提示区域
    vec2 hintUV = uv - vec2(0.5, 0.15);
    if (abs(hintUV.x) < 0.2 && abs(hintUV.y) < 0.02) {
        color = mix(color, vec3(0.4, 0.4, 0.45), 0.3);
    }

    // 边角装饰
    vec2 cornerUV = uv;
    float cornerSize = 0.05;
    if (cornerUV.x < cornerSize && cornerUV.y < cornerSize) {
        color = mix(color, vec3(0.2, 0.5, 0.8), 0.3);
    }
    cornerUV = vec2(1.0 - uv.x, uv.y);
    if (cornerUV.x < cornerSize && cornerUV.y < cornerSize) {
        color = mix(color, vec3(0.2, 0.5, 0.8), 0.3);
    }

    // 扫描线效果
    float scanline = sin(uv.y * uResolution.y * 2.0 + uTime * 10.0) * 0.02;
    color += scanline;

    gl_FragColor = vec4(color, 1.0);
}