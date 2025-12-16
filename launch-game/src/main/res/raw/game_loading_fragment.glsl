precision mediump float;

uniform float uTime;
uniform vec2 uResolution;
uniform float uProgress;
uniform vec3 uProgressColor; // 动态进度条颜色

// 字符渲染辅助函数
float digit(vec2 p, int d) {
    // 简化的7段数码管显示
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return 0.0;

    // 定义每个数字的段
    bool segments[7];

    // 初始化所有段为关闭
    for (int i = 0; i < 7; i++) {
        segments[i] = false;
    }

    // 数字对应的段配置
    if (d == 0) {
        segments[0] = true; segments[1] = true; segments[2] = true;
        segments[4] = true; segments[5] = true; segments[6] = true;
    } else if (d == 1) {
        segments[2] = true; segments[5] = true;
    } else if (d == 2) {
        segments[0] = true; segments[1] = true; segments[3] = true;
        segments[4] = true; segments[6] = true;
    } else if (d == 3) {
        segments[0] = true; segments[2] = true; segments[3] = true;
        segments[4] = true; segments[6] = true;
    } else if (d == 4) {
        segments[2] = true; segments[3] = true; segments[5] = true;
        segments[6] = true;
    } else if (d == 5) {
        segments[0] = true; segments[2] = true; segments[3] = true;
        segments[5] = true; segments[6] = true;
    } else if (d == 6) {
        segments[0] = true; segments[2] = true; segments[3] = true;
        segments[4] = true; segments[5] = true; segments[6] = true;
    } else if (d == 7) {
        segments[0] = true; segments[2] = true; segments[5] = true;
    } else if (d == 8) {
        for (int i = 0; i < 7; i++) segments[i] = true;
    } else if (d == 9) {
        segments[0] = true; segments[2] = true; segments[3] = true;
        segments[5] = true; segments[6] = true;
    }

    // 顶部 - 段0
    if (p.y > 0.8 && p.y < 1.0 && segments[0]) return 1.0;
    // 左上 - 段1
    if (p.x < 0.2 && p.y > 0.4 && p.y < 0.8 && segments[1]) return 1.0;
    // 右上 - 段2
    if (p.x > 0.8 && p.y > 0.4 && p.y < 0.8 && segments[2]) return 1.0;
    // 中间 - 段3
    if (p.y > 0.35 && p.y < 0.45 && segments[3]) return 1.0;
    // 左下 - 段4
    if (p.x < 0.2 && p.y > 0.0 && p.y < 0.4 && segments[4]) return 1.0;
    // 右下 - 段5
    if (p.x > 0.8 && p.y > 0.0 && p.y < 0.4 && segments[5]) return 1.0;
    // 底部 - 段6
    if (p.y > 0.0 && p.y < 0.2 && segments[6]) return 1.0;

    return 0.0;
}

// 渲染百分比文字
float renderPercentage(vec2 uv, float progress) {
    float percentage = progress * 100.0;
    int hundreds = int(percentage / 100.0);
    int tens = int((percentage - float(hundreds) * 100.0) / 10.0);
    int ones = int(percentage - float(hundreds) * 100.0 - float(tens) * 10.0);

    float result = 0.0;

    // 渲染三位数字
    if (hundreds > 0) {
        vec2 p1 = (uv - vec2(0.40, 0.44)) * 3.0;
        result = digit(p1, hundreds);
    }

    vec2 p2 = (uv - vec2(0.45, 0.44)) * 3.0;
    result = max(result, digit(p2, tens));

    vec2 p3 = (uv - vec2(0.50, 0.44)) * 3.0;
    result = max(result, digit(p3, ones));

    // 百分号
    vec2 p4 = (uv - vec2(0.54, 0.47)) * 3.0;
    if (p4.x > 0.3 && p4.x < 0.4 && p4.y > 0.2 && p4.y < 0.8) result = max(result, 1.0);
    if (p4.x > 0.6 && p4.x < 0.7 && p4.y > 0.2 && p4.y < 0.8) result = max(result, 1.0);
    if (p4.y > 0.8 && p4.y < 0.9 && p4.x > 0.3 && p4.x < 0.7) result = max(result, 1.0);

    return result;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution;

    // 深色背景
    vec3 bgColor = vec3(0.05, 0.05, 0.08);

    // 添加细微的网格纹理
    float grid = step(0.99, fract(uv.x * 100.0)) + step(0.99, fract(uv.y * 100.0));
    bgColor += grid * 0.02;

    vec3 color = bgColor;

    // LOADER 文字 (简化渲染)
    float loaderText = 0.0;
    vec2 loaderUV = (uv - vec2(0.5, 0.85)) * 8.0;
    if (abs(loaderUV.y) < 0.3) {
        // L
        if (loaderUV.x > -3.0 && loaderUV.x < -2.5 && loaderUV.y > -2.0 && loaderUV.y < 1.0) loaderText = 1.0;
        if (loaderUV.x > -2.5 && loaderUV.x < -0.5 && loaderUV.y > -2.0 && loaderUV.y < -1.5) loaderText = 1.0;

        // O (简化为方形)
        if (abs(loaderUV.x - 0.0) < 1.0 && abs(loaderUV.y - 0.0) < 0.8) {
            if (abs(loaderUV.x - 0.0) > 0.7 || abs(loaderUV.y - 0.0) > 0.5) {
                loaderText = 1.0;
            }
        }

        // A (简化)
        if (loaderUV.x > 2.0 && loaderUV.x < 3.0) loaderText = 1.0;
        if (loaderUV.x > 2.8 && loaderUV.x < 4.2 && abs(loaderUV.y - 0.0) < 2.0) {
            if (loaderUV.y > 0.5 || loaderUV.y < -0.5) loaderText = 1.0;
        }

        // D
        if (abs(loaderUV.x - 5.5) < 1.0 && abs(loaderUV.y - 0.0) < 2.0) {
            if (abs(loaderUV.x - 5.5) > 0.7 || loaderUV.y < -1.5 || loaderUV.y > 1.5) {
                loaderText = 1.0;
            }
        }

        // E
        if (loaderUV.x > 6.5 && loaderUV.x < 8.5 && abs(loaderUV.y - 0.0) < 2.0) {
            if (loaderUV.x < 7.0 || abs(loaderUV.y) > 1.5) loaderText = 1.0;
        }

        // R
        if (loaderUV.x > 9.0 && loaderUV.x < 10.0 && abs(loaderUV.y - 0.0) < 2.0) loaderText = 1.0;
        if (loaderUV.x > 10.0 && loaderUV.x < 11.0 && abs(loaderUV.y - 0.0) < 0.5) loaderText = 1.0;
        if (loaderUV.x > 9.0 && loaderUV.x < 11.0 && loaderUV.y > 1.5 && loaderUV.x < 10.5) loaderText = 1.0;
    }

    if (loaderText > 0.5) {
        color = mix(color, vec3(0.8, 0.8, 0.8), 0.8);
    }

    // 进度条背景
    vec2 barUV = uv - vec2(0.5, 0.5);
    if (abs(barUV.x) < 0.3 && abs(barUV.y) < 0.015) {
        color = mix(color, vec3(0.2, 0.2, 0.2), 0.8);
    }

    // 进度条填充
    if (barUV.x < -0.3 + 0.6 * uProgress && abs(barUV.x) < 0.3 && abs(barUV.y) < 0.015) {
        // 动态颜色变化
        vec3 progressColor = uProgressColor;
        progressColor.r *= (0.9 + 0.1 * sin(uTime * 5.0));
        progressColor.g *= (0.9 + 0.1 * cos(uTime * 3.0));

        color = mix(color, progressColor, 1.0);

        // 添加发光效果
        float glow = 1.0 - abs(barUV.y) / 0.015;
        color += progressColor * glow * 0.3;
    }

    // 百分比数字
    float pct = renderPercentage(uv, uProgress);
    if (pct > 0.5) {
        color = mix(color, vec3(1.0, 1.0, 1.0), 0.9);
    }

    // LOADING COMPLETE 文字 (当进度=100%时显示)
    if (uProgress >= 0.99) {
        vec2 textUV = (uv - vec2(0.5, 0.35)) * 20.0;
        float completeText = 0.0;

        // 简化的文字渲染
        if (textUV.x > -5.0 && textUV.x < 5.0 && abs(textUV.y) < 0.8) {
            // 这里简化处理，用矩形代表文字
            if (textUV.y > -0.5 && textUV.y < 0.5) {
                if (mod(floor(textUV.x + 5.0), 2.0) < 1.0) {
                    completeText = 1.0;
                }
            }
        }

        if (completeText > 0.5) {
            color = mix(color, vec3(0.2, 1.0, 0.2), 0.8);
        }
    }

    // 底部提示文字区域
    if (uv.y > 0.85 && uv.y < 0.95) {
        float hint = step(0.3, abs(uv.x - 0.5));
        color = mix(color, vec3(0.3, 0.3, 0.3), hint * 0.3);
    }

    // 边框发光效果
    float border = smoothstep(0.01, 0.02, uv.x) + smoothstep(0.99, 0.98, uv.x) +
                  smoothstep(0.01, 0.02, uv.y) + smoothstep(0.99, 0.98, uv.y);
    color += vec3(0.2, 0.3, 0.5) * border * 0.5;

    gl_FragColor = vec4(color, 1.0);
}