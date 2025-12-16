package com.example.launch.game;

/**
 * 负载模拟器
 * 使用伪随机数确保每次启动的负载行为一致
 * 执行真实的计算负载，而不是简单的 sleep
 */
public class LoadSimulator {
    private static final long SEED_BASE = 123456789L; // 固定种子确保一致性
    private long currentSeed;
    private int loadMultiplier;

    public LoadSimulator(String loadType) {
        // 根据负载类型和包名生成种子
        String seedStr = SEED_BASE + "_" + loadType + "_stable";
        currentSeed = seedStr.hashCode();

        // 根据负载类型设置负载倍数
        if (loadType.contains("light")) {
            loadMultiplier = 1;
        } else if (loadType.contains("medium")) {
            loadMultiplier = 3;
        } else if (loadType.contains("heavy")) {
            loadMultiplier = 6;
        } else {
            loadMultiplier = 1;
        }
    }

    /**
     * 生成下一个伪随机数
     */
    private int nextInt(int bound) {
        // 使用简单的线性同余生成器
        currentSeed = (currentSeed * 1103515245L + 12345L) & 0x7fffffffL;
        return (int)(currentSeed % bound);
    }

    /**
     * 生成下一个伪随机浮点数 [0,1]
     */
    private double nextDouble() {
        return nextInt(Integer.MAX_VALUE) / (double)Integer.MAX_VALUE;
    }

    /**
     * 执行计算负载
     * @param complexity 复杂度系数 (1-100)
     * @return 实际执行时间（毫秒）
     */
    public long executeLoad(int complexity) {
        long startTime = System.currentTimeMillis();
        int adjustedComplexity = complexity * loadMultiplier;

        // 根据复杂度执行不同类型的计算
        for (int i = 0; i < adjustedComplexity; i++) {
            // 数学运算负载
            performMathOperations(i);

            // 内存操作负载
            if (i % 10 == 0) {
                performMemoryOperations(i);
            }

            // 字符串操作负载
            if (i % 15 == 0) {
                performStringOperations(i);
            }

            // 偶尔执行更复杂的操作
            if (nextDouble() < 0.1) {
                performComplexOperation(i);
            }

            // 随机的小延迟（使用伪随机）
            if (nextDouble() < 0.2) {
                try {
                    Thread.sleep(1 + nextInt(5)); // 1-5ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * 数学运算负载
     */
    private void performMathOperations(int seed) {
        double result = seed;
        for (int i = 0; i < 100; i++) {
            result = Math.sin(result) * Math.cos(i);
            result = Math.sqrt(Math.abs(result) + 1);
            result = Math.pow(result, 1.0 / 3);

            // 伪随机操作
            int op = nextInt(4);
            switch (op) {
                case 0: result += i * 0.1; break;
                case 1: result *= 1.1; break;
                case 2: result -= 0.1; break;
                case 3: result /= 1.1; break;
            }
        }
    }

    /**
     * 内存操作负载
     */
    private void performMemoryOperations(int seed) {
        // 分配和操作数组
        int[] array = new int[1000];
        for (int i = 0; i < array.length; i++) {
            array[i] = (seed + i) * nextInt(100);
        }

        // 排序操作
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = 0; j < array.length - 1 - i; j++) {
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }

        // 计算统计信息
        long sum = 0;
        for (int num : array) {
            sum += num;
        }
    }

    /**
     * 字符串操作负载
     */
    private void performStringOperations(int seed) {
        StringBuilder sb = new StringBuilder();
        String base = "GRACKER_LOAD_TEST_";

        // 字符串拼接
        for (int i = 0; i < 100; i++) {
            sb.append(base);
            sb.append(seed);
            sb.append("_");
            sb.append(nextInt(1000));
        }

        // 字符串操作
        String result = sb.toString();
        result = result.toLowerCase();
        result = result.replace("_", "-");
        String[] parts = result.split("-");

        // 模拟JSON序列化/反序列化
        for (String part : parts) {
            String json = "{\"value\":\"" + part + "\",\"index\":" + nextInt(100) + "}";
            json.substring(1, json.length() - 1); // 解析模拟
        }
    }

    /**
     * 复杂操作负载
     */
    private void performComplexOperation(int seed) {
        // 递归计算斐波那契
        int n = 20 + nextInt(10);
        long fibResult = fibonacci(n);

        // 模拟深度搜索
        int[][] matrix = new int[10][10];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = (i * j + seed) % 100;
            }
        }

        // 寻找最大路径
        int maxPath = findMaxPath(matrix, 0, 0);
    }

    /**
     * 斐波那契计算（递归版，计算密集）
     */
    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    /**
     * 寻找矩阵最大路径（简化版）
     */
    private int findMaxPath(int[][] matrix, int row, int col) {
        if (row >= matrix.length || col >= matrix[0].length) {
            return 0;
        }

        int current = matrix[row][col];
        int right = findMaxPath(matrix, row, col + 1);
        int down = findMaxPath(matrix, row + 1, col);

        return current + Math.max(right, down);
    }

    /**
     * 模拟着色器编译负载
     */
    public void simulateShaderCompilation() {
        // 模拟GLSL编译过程
        String vertexShader = "attribute vec4 position; uniform mat4 mvp; void main() { gl_Position = mvp * position; }";
        String fragmentShader = "precision mediump float; uniform vec3 color; void main() { gl_FragColor = vec4(color, 1.0); }";

        // 词法分析
        for (int i = 0; i < 1000; i++) {
            String[] tokens = vertexShader.split("\\s+");
            for (String token : tokens) {
                token.hashCode(); // 模拟token处理
            }

            // 语法树生成模拟
            int nodeCount = 50 + nextInt(50);
            for (int j = 0; j < nodeCount; j++) {
                int nodeType = nextInt(10);
                int childCount = nextInt(5);
                // 模拟节点处理
                for (int k = 0; k < childCount; k++) {
                    nextInt(1000);
                }
            }
        }
    }

    /**
     * 模拟纹理加载负载
     */
    public void simulateTextureLoading() {
        // 模拟2Kx2K纹理处理
        int width = 2048;
        int height = 2048;
        int[] pixels = new int[width * height];

        // 生成纹理数据
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 伪随机生成纹理
                int index = y * width + x;
                int r = (nextInt(256) + x) % 256;
                int g = (nextInt(256) + y) % 256;
                int b = (nextInt(256) + (x + y)) % 256;
                pixels[index] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // 模拟纹理压缩
        for (int i = 0; i < pixels.length; i += 4) {
            int block = 0;
            for (int j = 0; j < 4 && i + j < pixels.length; j++) {
                block |= pixels[i + j] & 0xFF;
            }
            // 模拟DXT压缩
            int compressed = compressBlock(block);
        }
    }

    /**
     * 模拟块压缩
     */
    private int compressBlock(int block) {
        // 简化的压缩算法
        return (block ^ (block >> 8)) & 0xFF;
    }

    /**
     * 模拟物理引擎初始化
     */
    public void simulatePhysicsEngineInit() {
        // 模拟物理对象创建
        int objectCount = 100 * loadMultiplier;
        float[] positions = new float[objectCount * 3];
        float[] velocities = new float[objectCount * 3];
        float[] masses = new float[objectCount];

        // 初始化物理对象
        for (int i = 0; i < objectCount; i++) {
            int idx = i * 3;
            positions[idx] = nextFloat() * 100;
            positions[idx + 1] = nextFloat() * 100;
            positions[idx + 2] = nextFloat() * 100;

            velocities[idx] = (nextFloat() - 0.5f) * 10;
            velocities[idx + 1] = (nextFloat() - 0.5f) * 10;
            velocities[idx + 2] = (nextFloat() - 0.5f) * 10;

            masses[i] = 1.0f + nextFloat() * 10;
        }

        // 模拟碰撞检测计算
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < objectCount - 1; j++) {
                for (int k = j + 1; k < objectCount; k++) {
                    // 计算距离
                    float dx = positions[j*3] - positions[k*3];
                    float dy = positions[j*3+1] - positions[k*3+1];
                    float dz = positions[j*3+2] - positions[k*3+2];
                    float distance = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

                    // 碰撞检测
                    if (distance < 5.0f) {
                        // 简单的弹性碰撞响应
                        float impulse = 2.0f / (masses[j] + masses[k]);
                        velocities[j] += impulse * masses[k] * dx / distance;
                        velocities[k] -= impulse * masses[j] * dx / distance;
                    }
                }
            }
        }
    }

    /**
     * 生成伪随机浮点数 [0,1)
     */
    private float nextFloat() {
        return nextInt(10000) / 10000.0f;
    }
}