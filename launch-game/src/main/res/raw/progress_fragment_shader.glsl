precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 进度条区域
    float barTop = 0.35;
    float barBottom = 0.45;
    float barLeft = 0.1;
    float barRight = 0.9;

    // 背景透明
    if (uv.y < barTop || uv.y > barBottom) {
        discard;
    }

    // 进度条背景
    if (uv.x < barLeft || uv.x > barRight) {
        discard;
    }

    // 计算进度
    float progressWidth = (barRight - barLeft) * uProgress;
    float currentX = uv.x - barLeft;

    // 背景部分
    if (currentX > progressWidth) {
        gl_FragColor = vec4(0.2, 0.2, 0.2, 0.8);
    } else {
        // 进度条填充部分
        float glow = sin(uTime * 3.0) * 0.1 + 0.9;
        gl_FragColor = vec4(0.0 * glow, 0.8 * glow, 0.4 * glow, 1.0);
    }

    // 边框
    if (uv.x < barLeft + 0.002 || uv.x > barRight - 0.002 ||
        uv.y < barTop + 0.002 || uv.y > barBottom - 0.002) {
        gl_FragColor = vec4(0.3, 0.3, 0.3, 1.0);
    }
}