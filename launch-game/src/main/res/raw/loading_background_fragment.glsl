precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;

// 噪声函数
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) + (c - a)* u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 创建渐变背景
    vec3 color1 = vec3(0.05, 0.05, 0.15);     // 深蓝紫
    vec3 color2 = vec3(0.1, 0.05, 0.2);      // 紫色
    vec3 color3 = vec3(0.05, 0.1, 0.25);     // 蓝色

    // 添加动态渐变
    float gradient = uv.y;
    gradient += sin(uTime * 0.3 + uv.x * 2.0) * 0.1;
    gradient += cos(uTime * 0.2) * 0.05;

    vec3 bgColor = mix(color1, color2, gradient);
    bgColor = mix(bgColor, color3, sin(uTime * 0.15) * 0.3 + 0.3);

    // 添加网格效果
    float gridSize = 50.0;
    vec2 grid = fract(gl_FragCoord.xy / gridSize);
    float gridLine = step(0.98, grid.x) + step(0.98, grid.y);
    bgColor = mix(bgColor, bgColor * 1.5, gridLine * 0.2);

    // 添加扫描线效果
    float scanline = sin(gl_FragCoord.y * 2.0 + uTime * 5.0) * 0.04;
    bgColor += scanline;

    // 添加噪点
    float noiseVal = noise(gl_FragCoord.xy * 0.01 + uTime * 0.1);
    bgColor += noiseVal * 0.02;

    // 边缘渐暗效果
    float vignette = 1.0 - distance(uv, vec2(0.5)) * 0.5;
    bgColor *= vignette;

    gl_FragColor = vec4(bgColor, 1.0);
}