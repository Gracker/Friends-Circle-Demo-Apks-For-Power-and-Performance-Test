precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 简单的深色背景
    vec3 bgColor = vec3(0.05, 0.05, 0.1);

    // 添加简单的扫描线
    float scanline = sin(uv.y * 800.0 + uTime * 5.0) * 0.02;
    bgColor += scanline;

    // 进度条区域
    vec2 barCenter = vec2(0.5, 0.5);
    vec2 barSize = vec2(0.3, 0.02);
    float barDist = distance(uv, barCenter);

    // 进度条背景
    if (abs(uv.x - barCenter.x) < barSize.x && abs(uv.y - barCenter.y) < barSize.y) {
        bgColor = mix(bgColor, vec3(0.2, 0.2, 0.25), 0.8);
    }

    // 进度条填充
    float progressStart = barCenter.x - barSize.x;
    float progressEnd = progressStart + barSize.x * 2.0 * uProgress;

    if (uv.x > progressStart && uv.x < progressEnd && abs(uv.y - barCenter.y) < barSize.y) {
        // 简单的颜色从红到绿
        vec3 progressColor = mix(vec3(1.0, 0.2, 0.1), vec3(0.1, 0.8, 0.2), uProgress);
        bgColor = mix(bgColor, progressColor, 1.0);
    }

    // 顶部装饰线
    if (uv.y > 0.85 && uv.y < 0.86) {
        bgColor = mix(bgColor, vec3(0.2, 0.5, 0.8), 0.5);
    }

    // 底部装饰线
    if (uv.y < 0.15 && uv.y > 0.14) {
        bgColor = mix(bgColor, vec3(0.2, 0.5, 0.8), 0.5);
    }

    gl_FragColor = vec4(bgColor, 1.0);
}