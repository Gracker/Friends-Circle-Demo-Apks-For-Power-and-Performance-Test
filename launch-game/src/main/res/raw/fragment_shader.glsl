precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;
uniform vec4 uColor;

varying vec2 vTexCoord;

// 噪声函数
float noise(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 创建动态渐变背景
    vec3 color1 = vec3(0.1, 0.0, 0.2);      // 深紫色
    vec3 color2 = vec3(0.0, 0.1, 0.3);      // 深蓝色
    vec3 color3 = vec3(0.0, 0.2, 0.4);      // 蓝色

    // 添加时间动画
    float wave = sin(uTime * 0.5 + uv.x * 3.0) * 0.1;

    // 混合颜色
    vec3 bgColor = mix(color1, color2, uv.y + wave);
    bgColor = mix(bgColor, color3, sin(uTime * 0.3) * 0.5 + 0.5);

    // 添加网格效果
    float grid = step(0.98, fract(uv.x * 20.0)) + step(0.98, fract(uv.y * 20.0));
    bgColor = mix(bgColor, bgColor * 1.2, grid * 0.3);

    gl_FragColor = vec4(bgColor, 1.0);
}