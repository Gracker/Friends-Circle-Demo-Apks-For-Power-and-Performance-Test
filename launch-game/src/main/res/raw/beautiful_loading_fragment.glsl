precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;

// 噪声函数
float noise(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

float fbm(vec2 st) {
    float value = 0.0;
    float amplitude = .5;
    float frequency = 0.;

    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(st);
        st *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// 圆角矩形 SDF
float roundedRectSDF(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// 绘制发光的进度条
vec3 drawProgressBar(vec2 uv, float progress) {
    vec3 color = vec3(0.0);

    // 进度条参数
    vec2 barPos = vec2(0.0, -0.2);
    vec2 barSize = vec2(0.4, 0.015);
    float radius = 0.008;

    // 背景槽
    float bgDist = roundedRectSDF(uv - barPos, barSize, radius);
    if (bgDist < 0.001) {
        color = vec3(0.1, 0.1, 0.15) * 0.8;
    }

    // 进度填充
    if (uv.x > barPos.x - barSize.x && uv.x < barPos.x - barSize.x + barSize.x * 2.0 * progress) {
        float fillDist = roundedRectSDF(uv - barPos, vec2(barSize.x * progress, barSize.y), radius);
        if (fillDist < 0.001) {
            // 动态颜色 - 从橙红到绿色
            vec3 progressColor = mix(vec3(1.0, 0.5, 0.1), vec3(0.1, 0.9, 0.3), progress);

            // 添加脉冲发光
            float pulse = sin(uTime * 4.0) * 0.1 + 0.9;
            progressColor *= pulse;

            // 边缘发光
            float edgeGlow = 1.0 - abs(fillDist) / 0.01;
            color += progressColor + progressColor * edgeGlow * 0.5;
        }
    }

    // 边框
    if (abs(bgDist) < 0.001) {
        color = vec3(0.3, 0.3, 0.4);
    }

    return color;
}

// 绘制六边形网格背景
vec3 drawHexagonGrid(vec2 uv) {
    vec3 color = vec3(0.0);

    // 六边形网格参数
    vec2 hexUv = uv * 30.0;
    hexUv.y += 0.5 * floor(hexUv.x);

    vec2 hexId = floor(hexUv);
    vec2 hexFrac = fract(hexUv);

    // 创建六边形
    vec2 center = vec2(0.5, 0.5);
    float hex = max(abs(hexFrac.x - 0.5), abs(hexFrac.y - 0.5));
    hex = max(hex, abs(hexFrac.x + hexFrac.y - 1.0) * 0.866);

    // 动画效果
    float anim = sin(uTime + hexId.x * 0.1 + hexId.y * 0.1) * 0.5 + 0.5;

    if (hex > 0.48 && hex < 0.5) {
        color = vec3(0.1, 0.2, 0.4) * anim * 0.3;
    }

    return color;
}

void main() {
    vec2 uv = (gl_FragCoord.xy - 0.5 * uResolution.xy) / min(uResolution.x, uResolution.y);

    // 动态渐变背景
    vec3 color1 = vec3(0.02, 0.02, 0.05);  // 深蓝紫
    vec3 color2 = vec3(0.05, 0.03, 0.1);   // 紫色
    vec3 color3 = vec3(0.02, 0.05, 0.12);  // 深蓝

    // 创建动态渐变
    float gradient = uv.y + sin(uTime * 0.3) * 0.1;
    vec3 bgColor = mix(color1, color2, gradient * 0.5 + 0.5);
    bgColor = mix(bgColor, color3, sin(uTime * 0.2 + uv.x * 2.0) * 0.2 + 0.3);

    // 添加六边形网格
    bgColor += drawHexagonGrid(uv);

    // 添加噪点
    float n = fbm(uv * 2.0 + uTime * 0.1);
    bgColor += n * 0.02;

    // 边缘暗角
    float vignette = 1.0 - smoothstep(0.8, 1.5, length(uv));
    bgColor *= vignette;

    // 绘制进度条
    vec3 progressBar = drawProgressBar(uv, uProgress);
    bgColor += progressBar;

    // 扫描线效果
    float scanline = sin(uv.y * uResolution.y * 2.0 + uTime * 10.0) * 0.02;
    bgColor += scanline;

    // 顶部装饰线
    float topLine = smoothstep(0.0, 0.002, 0.35 - uv.y);
    bgColor += vec3(0.2, 0.5, 0.8) * topLine * 0.5;

    // 底部装饰线
    float bottomLine = smoothstep(0.0, 0.002, uv.y + 0.25);
    bgColor += vec3(0.2, 0.5, 0.8) * bottomLine * 0.5;

    gl_FragColor = vec4(bgColor, 1.0);
}