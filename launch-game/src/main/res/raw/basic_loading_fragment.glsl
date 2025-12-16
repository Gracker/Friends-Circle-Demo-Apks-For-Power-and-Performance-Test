precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 深蓝色背景
    vec3 color = vec3(0.02, 0.05, 0.15);

    // 添加微小的网格效果
    float grid = step(0.99, fract(uv.x * 100.0)) + step(0.99, fract(uv.y * 100.0));
    color += grid * 0.01;

    // 简单的进度条
    float barY = 0.5;
    float barHeight = 0.02;
    float barStart = 0.2;
    float barEnd = 0.8;
    float currentEnd = barStart + (barEnd - barStart) * uProgress;

    // 进度条背景
    if (uv.y > barY - barHeight && uv.y < barY + barHeight) {
        if (uv.x > barStart && uv.x < barEnd) {
            color = mix(color, vec3(0.1, 0.1, 0.2), 0.8);
        }
    }

    // 进度条填充
    if (uv.y > barY - barHeight && uv.y < barY + barHeight) {
        if (uv.x > barStart && uv.x < currentEnd) {
            vec3 progressColor = mix(vec3(1.0, 0.3, 0.1), vec3(0.1, 0.8, 0.3), uProgress);
            color = mix(color, progressColor, 1.0);
        }
    }

    // 扫描线效果
    float scanline = sin(uv.y * uResolution.y * 2.0 + uTime * 10.0) * 0.01;
    color += scanline;

    gl_FragColor = vec4(color, 1.0);
}