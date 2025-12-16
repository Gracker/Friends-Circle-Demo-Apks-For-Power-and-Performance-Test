uniform mat4 uMVPMatrix;
uniform float uTime;

attribute vec3 aPosition;
attribute vec3 aVelocity;
attribute vec3 aColor;

varying vec3 vColor;

void main() {
    // 基于时间更新位置
    vec3 position = aPosition + aVelocity * uTime;

    // 添加一些扰动
    position.x += sin(uTime * 2.0 + aPosition.x) * 0.01;
    position.y += cos(uTime * 2.0 + aPosition.y) * 0.01;

    gl_Position = uMVPMatrix * vec4(position, 1.0);
    gl_PointSize = 10.0;

    vColor = aColor;
}