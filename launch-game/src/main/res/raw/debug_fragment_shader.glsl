precision mediump float;

uniform float uTime;
uniform vec2 uResolution;

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 创建明显的颜色变化以确认渲染工作
    float r = 0.5 + 0.5 * sin(uTime);
    float g = 0.5 + 0.5 * cos(uTime * 1.3);
    float b = 0.5 + 0.5 * sin(uTime * 0.7);

    gl_FragColor = vec4(r, g, b, 1.0);
}