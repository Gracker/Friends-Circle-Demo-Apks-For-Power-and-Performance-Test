precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;
uniform vec2 uMouse;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    vec2 d = abs(center) - size + radius;
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - radius;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 进度条参数
    vec2 barCenter = vec2(0.5, 0.7);
    vec2 barSize = vec2(0.35, 0.015);
    float radius = 0.008;

    // 背景框
    float bgBox = roundedBoxSDF(uv - barCenter, barSize, radius);
    vec3 bgColor = vec3(0.1, 0.1, 0.15);
    float bgAlpha = 1.0 - smoothstep(0.0, 0.005, bgBox);

    // 进度条填充
    vec2 progressSize = vec2(barSize.x * uProgress, barSize.y);
    float progressBox = roundedBoxSDF(uv - barCenter, progressSize, radius);

    // 动态颜色
    float pulse = sin(uTime * 3.0) * 0.1 + 0.9;
    vec3 progressColor = vec3(0.2 * pulse, 0.8 * pulse, 0.4);
    float progressAlpha = 1.0 - smoothstep(0.0, 0.005, progressBox);

    // 发光效果
    float glow = 1.0 - smoothstep(-0.02, 0.02, progressBox);
    vec3 glowColor = progressColor * 0.5;

    // 边框
    float border = smoothstep(-0.005, 0.0, bgBox) - smoothstep(0.0, 0.005, bgBox);
    vec3 borderColor = vec3(0.3, 0.3, 0.4);

    // 组合最终颜色
    vec3 finalColor = bgColor * bgAlpha * 0.5;
    finalColor = mix(finalColor, progressColor, progressAlpha);
    finalColor += glowColor * glow * 0.3;
    finalColor = mix(finalColor, borderColor, border);

    gl_FragColor = vec4(finalColor, max(bgAlpha * 0.5, progressAlpha));
}