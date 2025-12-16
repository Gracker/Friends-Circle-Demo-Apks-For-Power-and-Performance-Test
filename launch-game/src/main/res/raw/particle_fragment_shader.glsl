precision mediump float;

varying vec3 vColor;

void main() {
    // 创建圆形粒子
    vec2 coord = gl_PointCoord - vec2(0.5);
    float dist = length(coord);

    if (dist > 0.5) {
        discard;
    }

    // 添加渐变效果
    float alpha = 1.0 - (dist * 2.0);
    alpha = alpha * alpha; // 平滑边缘

    gl_FragColor = vec4(vColor, alpha * 0.8);
}