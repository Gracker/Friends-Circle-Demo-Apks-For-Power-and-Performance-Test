package com.example.wechatfriendforglmap;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Trace;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import com.example.loadconfig.LoadConfig;
import com.example.loadconfig.LoadSimulator;
import com.example.loadconfig.LoadType;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 2.0 renderer for map-like visualization.
 * Renders a grid of tiles with roads and landmarks, similar to map apps.
 * Uses LoadConfig for unified load configuration.
 */
public class GLMapRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLMapRenderer";
    private static final long LOAD_TYPE_SEED_STRIDE = 1009L;
    private static final int LIGHT_SCROLL_EXTRA_ITERATIONS = 1200;
    private static final int MEDIUM_SCROLL_EXTRA_ITERATIONS = 8000;
    private static final int HEAVY_SCROLL_EXTRA_ITERATIONS = 28000;
    private static final int LONG_FRAME_SCROLL_EXTRA_ITERATIONS = 36000;
    private static final int LIGHT_IDLE_EXTRA_ITERATIONS = 300;
    private static final int MEDIUM_IDLE_EXTRA_ITERATIONS = 1800;
    private static final int HEAVY_IDLE_EXTRA_ITERATIONS = 6000;
    private static final int LONG_FRAME_IDLE_EXTRA_ITERATIONS = 8000;

    private int loadType = LoadType.MINIMAL;
    private LoadSimulator mLoadSimulator;

    // Long frame state
    private long scrollStartTime = 0;
    private int longFrameTriggerCount = 0;
    private int currentLongFrameIndex = 0;
    private long[] longFrameTriggerTimes;
    private long lastLongFrameTime = 0;
    private boolean isScrolling = false;

    // Shader handles
    private int program;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    // Matrices
    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];

    // Map state
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float zoom = 1f;

    // Geometry buffers
    private FloatBuffer gridVertices;
    private FloatBuffer roadVertices;
    private FloatBuffer mainRoadVertices;
    private FloatBuffer subwayVertices;
    private FloatBuffer buildingVertices;
    private FloatBuffer markerVertices;
    private FloatBuffer waterVertices;
    private FloatBuffer parkVertices;
    private FloatBuffer riverVertices;

    private int gridVertexCount;
    private int roadVertexCount;
    private int mainRoadVertexCount;
    private int subwayVertexCount;
    private int buildingVertexCount;
    private int markerVertexCount;
    private int waterVertexCount;
    private int parkVertexCount;
    private int riverVertexCount;

    private Random rendererLoadRandom = new Random(LoadConfig.COMPUTATION_SEED);
    private double rendererLoadAccumulator = 0.0;
    private long rendererFrameCounter = 0;

    // Shaders
    private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  gl_PointSize = 20.0;" +
            "}";

    private static final String FRAGMENT_SHADER = "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    public void setLoadType(@LoadType.Type int loadType) {
        this.loadType = loadType;
        if (mLoadSimulator == null) {
            mLoadSimulator = new LoadSimulator();
        } else {
            mLoadSimulator.resetRandomState();
        }

        // 负载类型变化时重置模块内伪随机状态，确保每次进入同档位表现一致
        rendererLoadRandom = new Random(LoadConfig.COMPUTATION_SEED + loadType * LOAD_TYPE_SEED_STRIDE);
        rendererLoadAccumulator = 0.0;
        rendererFrameCounter = 0;
    }

    public void release() {
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
        // 释放OpenGL资源
        releaseGLResources();
    }

    /**
     * 释放所有OpenGL资源，防止内存泄漏
     * 应在GLSurfaceView的onPause或销毁时调用
     */
    public void releaseGLResources() {
        // 删除shader程序
        if (program != 0) {
            GLES20.glDeleteProgram(program);
            program = 0;
        }

        // 清空FloatBuffer引用，让GC回收Native内存
        // 注意：DirectByteBuffer的Native内存由JVM的Cleaner自动回收
        gridVertices = null;
        roadVertices = null;
        mainRoadVertices = null;
        subwayVertices = null;
        buildingVertices = null;
        markerVertices = null;
        waterVertices = null;
        parkVertices = null;
        riverVertices = null;

        // 重置顶点计数
        gridVertexCount = 0;
        roadVertexCount = 0;
        mainRoadVertexCount = 0;
        subwayVertexCount = 0;
        buildingVertexCount = 0;
        markerVertexCount = 0;
        waterVertexCount = 0;
        parkVertexCount = 0;
        riverVertexCount = 0;

        Log.d(TAG, "OpenGL resources released");
    }

    public void setOffset(float dx, float dy) {
        this.offsetX += dx / 500f / zoom;
        this.offsetY -= dy / 500f / zoom;
    }

    public void setZoom(float scaleFactor) {
        this.zoom *= scaleFactor;
        this.zoom = Math.max(0.5f, Math.min(5f, zoom));
    }

    public void onScrollStart() {
        if (!isScrolling) {
            isScrolling = true;
            if (LoadType.isLongFrameLoad(loadType)) {
                startLongFrameCycle();
            }
        }
    }

    public void onScrollStop() {
        isScrolling = false;
        resetLongFrameState();
    }

    private void startLongFrameCycle() {
        scrollStartTime = System.currentTimeMillis();
        longFrameTriggerCount = LoadConfig.getLongFrameTriggerCount();
        longFrameTriggerTimes = LoadConfig.getLongFrameTriggerTimes(longFrameTriggerCount);
        currentLongFrameIndex = 0;
        lastLongFrameTime = 0;
    }

    private void resetLongFrameState() {
        scrollStartTime = 0;
        currentLongFrameIndex = 0;
        longFrameTriggerTimes = null;
    }

    private void checkAndExecuteLongFrame() {
        if (currentLongFrameIndex >= longFrameTriggerCount || longFrameTriggerTimes == null)
            return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - scrollStartTime;

        if (elapsedTime >= longFrameTriggerTimes[currentLongFrameIndex]) {
            if (currentTime - lastLongFrameTime >= LoadConfig.LONG_FRAME_MIN_INTERVAL_MS) {
                Trace.beginSection("GLMap_longFrameLoad_" + (currentLongFrameIndex + 1));
                executeLongFrameLoad();
                Trace.endSection();
                lastLongFrameTime = currentTime;
                currentLongFrameIndex++;
            }
        }

        if (elapsedTime >= LoadConfig.LONG_FRAME_SCROLL_PERIOD_MS) {
            startLongFrameCycle();
        }
    }

    private void executeLongFrameLoad() {
        if (mLoadSimulator != null) {
            mLoadSimulator.executeLongFrameLoad("GLMap_longFrameLoad");
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.93f, 0.93f, 0.88f, 1.0f); // Map background color

        // Create shader program
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        // Generate map geometry
        generateMapGeometry();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Trace.beginSection("GLMap_onDrawFrame");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set camera
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Apply transformations
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.scaleM(modelMatrix, 0, zoom, zoom, 1f);
        Matrix.translateM(modelMatrix, 0, offsetX, offsetY, 0f);

        // Calculate MVP matrix
        float[] tempMatrix = new float[16];
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        GLES20.glUseProgram(program);

        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw map layers (Order matters for "painter's algorithm")
        drawRivers(); // Rivers on bottom
        drawWater(); // Lakes
        drawParks(); // Parks/green areas
        drawGrid(); // Street grid
        drawSubway(); // Subway lines
        drawMainRoads(); // Highways
        drawRoads(); // Local roads
        drawBuildings(); // Buildings
        drawMarkers(); // POI markers on top

        // Execute load
        executeLoad();

        Trace.endSection();
    }

    private void generateMapGeometry() {
        Random r = new Random(LoadConfig.DATA_GENERATION_SEED);

        float range = 100f; // Map size range (-50 to 50)

        // 1. Generate Rivers (long flowing water bodies) - 5 rivers
        int riverCount = 5;
        int riverSegmentCount = 200;
        float[] riverCoords = new float[riverCount * riverSegmentCount * 6]; // Lines
        int riverIdx = 0;
        for (int river = 0; river < riverCount; river++) {
            float riverX = -50f;
            float riverY = r.nextFloat() * 80f - 40f;
            for (int i = 0; i < riverSegmentCount; i++) {
                float nextX = riverX + 0.5f;
                float nextY = riverY + (r.nextFloat() - 0.5f) * 2f;
                riverCoords[riverIdx++] = riverX;
                riverCoords[riverIdx++] = riverY;
                riverCoords[riverIdx++] = 0f;
                riverCoords[riverIdx++] = nextX;
                riverCoords[riverIdx++] = nextY;
                riverCoords[riverIdx++] = 0f;
                riverX = nextX;
                riverY = nextY;
            }
        }
        riverVertexCount = riverCount * riverSegmentCount * 2;
        riverVertices = createFloatBuffer(riverCoords, riverCoords.length);

        // 2. Generate Water Bodies (Lakes) - larger and more diverse
        int waterCount = 400;
        float[] waterCoords = new float[waterCount * 6 * 3];
        for (int i = 0; i < waterCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            float w = 1.0f + r.nextFloat() * 6.0f;
            float h = 0.8f + r.nextFloat() * 4.0f;
            addQuadToBuffer(waterCoords, i * 18, x, y, w, h);
        }
        waterVertexCount = waterCount * 6;
        waterVertices = createFloatBuffer(waterCoords, waterCoords.length);

        // 3. Generate Parks - more and varied
        int parkCount = 750;
        float[] parkCoords = new float[parkCount * 6 * 3];
        for (int i = 0; i < parkCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            float w = 0.5f + r.nextFloat() * 4.0f;
            float h = 0.5f + r.nextFloat() * 4.0f;
            addQuadToBuffer(parkCoords, i * 18, x, y, w, h);
        }
        parkVertexCount = parkCount * 6;
        parkVertices = createFloatBuffer(parkCoords, parkCoords.length);

        // 4. Generate Grid (street blocks)
        int gridSize = 200;
        float[] gridCoords = new float[gridSize * 4 * 3 * 2];
        int idx = 0;
        for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
            float pos = i * 0.5f;
            float limit = gridSize / 2 * 0.5f;
            gridCoords[idx++] = pos;
            gridCoords[idx++] = -limit;
            gridCoords[idx++] = 0f;
            gridCoords[idx++] = pos;
            gridCoords[idx++] = limit;
            gridCoords[idx++] = 0f;
            gridCoords[idx++] = -limit;
            gridCoords[idx++] = pos;
            gridCoords[idx++] = 0f;
            gridCoords[idx++] = limit;
            gridCoords[idx++] = pos;
            gridCoords[idx++] = 0f;
        }
        gridVertexCount = idx / 3;
        gridVertices = createFloatBuffer(gridCoords, idx);

        // 5. Generate Main Roads (Highways) - thick, long roads
        int mainRoadCount = 150;
        float[] mainRoadCoords = new float[mainRoadCount * 6];
        for (int i = 0; i < mainRoadCount; i++) {
            boolean horizontal = r.nextBoolean();
            float startX, startY, endX, endY;
            if (horizontal) {
                startX = -50f;
                endX = 50f;
                startY = endY = (r.nextFloat() - 0.5f) * range;
            } else {
                startY = -50f;
                endY = 50f;
                startX = endX = (r.nextFloat() - 0.5f) * range;
            }
            mainRoadCoords[i * 6] = startX;
            mainRoadCoords[i * 6 + 1] = startY;
            mainRoadCoords[i * 6 + 2] = 0f;
            mainRoadCoords[i * 6 + 3] = endX;
            mainRoadCoords[i * 6 + 4] = endY;
            mainRoadCoords[i * 6 + 5] = 0f;
        }
        mainRoadVertexCount = mainRoadCount * 2;
        mainRoadVertices = createFloatBuffer(mainRoadCoords, mainRoadCoords.length);

        // 6. Generate Local Roads - shorter, thinner
        int roadCount = 3000;
        float[] roadCoords = new float[roadCount * 6];
        for (int i = 0; i < roadCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            boolean horizontal = r.nextBoolean();
            float len = 2f + r.nextFloat() * 8f;
            if (horizontal) {
                roadCoords[i * 6] = x;
                roadCoords[i * 6 + 1] = y;
                roadCoords[i * 6 + 2] = 0f;
                roadCoords[i * 6 + 3] = x + len;
                roadCoords[i * 6 + 4] = y;
                roadCoords[i * 6 + 5] = 0f;
            } else {
                roadCoords[i * 6] = x;
                roadCoords[i * 6 + 1] = y;
                roadCoords[i * 6 + 2] = 0f;
                roadCoords[i * 6 + 3] = x;
                roadCoords[i * 6 + 4] = y + len;
                roadCoords[i * 6 + 5] = 0f;
            }
        }
        roadVertexCount = roadCount * 2;
        roadVertices = createFloatBuffer(roadCoords, roadCoords.length);

        // 7. Generate Subway Lines
        int subwayLineCount = 40;
        int segmentsPerLine = 50;
        float[] subwayCoords = new float[subwayLineCount * segmentsPerLine * 6];
        idx = 0;
        for (int line = 0; line < subwayLineCount; line++) {
            float sx = (r.nextFloat() - 0.5f) * range;
            float sy = (r.nextFloat() - 0.5f) * range;
            float angle = r.nextFloat() * (float) Math.PI * 2;
            for (int seg = 0; seg < segmentsPerLine; seg++) {
                float nextX = sx + (float) Math.cos(angle) * 2f;
                float nextY = sy + (float) Math.sin(angle) * 2f;
                angle += (r.nextFloat() - 0.5f) * 0.3f; // Slight curve
                subwayCoords[idx++] = sx;
                subwayCoords[idx++] = sy;
                subwayCoords[idx++] = 0f;
                subwayCoords[idx++] = nextX;
                subwayCoords[idx++] = nextY;
                subwayCoords[idx++] = 0f;
                sx = nextX;
                sy = nextY;
            }
        }
        subwayVertexCount = idx / 3;
        subwayVertices = createFloatBuffer(subwayCoords, idx);

        // 8. Generate Buildings - more variety in size and density
        int buildingCount = 6000;
        float[] buildingCoords = new float[buildingCount * 18];
        for (int i = 0; i < buildingCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            // Varied building sizes: small apartments to large complexes
            float size = 0.15f + r.nextFloat() * 0.8f;
            float sizeY = size * (0.7f + r.nextFloat() * 0.6f);
            addQuadToBuffer(buildingCoords, i * 18, x, y, size, sizeY);
        }
        buildingVertexCount = buildingCount * 6;
        buildingVertices = createFloatBuffer(buildingCoords, buildingCoords.length);

        // 9. Generate Markers (POIs) - more markers
        int markerCount = 1000;
        float[] markerCoords = new float[markerCount * 3];
        for (int i = 0; i < markerCount; i++) {
            markerCoords[i * 3] = (r.nextFloat() - 0.5f) * range;
            markerCoords[i * 3 + 1] = (r.nextFloat() - 0.5f) * range;
            markerCoords[i * 3 + 2] = 0f;
        }
        markerVertexCount = markerCount;
        markerVertices = createFloatBuffer(markerCoords, markerCoords.length);
    }

    private void addQuadToBuffer(float[] buffer, int offset, float x, float y, float w, float h) {
        // Triangle 1
        buffer[offset] = x;
        buffer[offset + 1] = y;
        buffer[offset + 2] = 0f;
        buffer[offset + 3] = x + w;
        buffer[offset + 4] = y;
        buffer[offset + 5] = 0f;
        buffer[offset + 6] = x + w;
        buffer[offset + 7] = y + h;
        buffer[offset + 8] = 0f;
        // Triangle 2
        buffer[offset + 9] = x;
        buffer[offset + 10] = y;
        buffer[offset + 11] = 0f;
        buffer[offset + 12] = x + w;
        buffer[offset + 13] = y + h;
        buffer[offset + 14] = 0f;
        buffer[offset + 15] = x;
        buffer[offset + 16] = y + h;
        buffer[offset + 17] = 0f;
    }

    private FloatBuffer createFloatBuffer(float[] coords, int count) {
        ByteBuffer bb = ByteBuffer.allocateDirect(count * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords, 0, count);
        fb.position(0);
        return fb;
    }

    private void drawRivers() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 0.3f, 0.6f, 0.9f, 1.0f); // River blue

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, riverVertices);

        GLES20.glLineWidth(12f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, riverVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawMainRoads() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 1.0f, 0.7f, 0.3f, 1.0f); // Orange highways

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, mainRoadVertices);

        GLES20.glLineWidth(16f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, mainRoadVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawSubway() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 0.8f, 0.2f, 0.5f, 0.8f); // Magenta subway

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, subwayVertices);

        GLES20.glLineWidth(6f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, subwayVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawWater() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 0.6f, 0.8f, 1.0f, 1.0f); // Light Blue

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, waterVertices);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, waterVertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawParks() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 0.7f, 0.9f, 0.7f, 1.0f); // Light Green

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, parkVertices);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, parkVertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawGrid() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 0.8f, 0.8f, 0.75f, 1.0f);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, gridVertices);

        GLES20.glLineWidth(1f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawRoads() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 1.0f, 0.9f, 0.6f, 1.0f); // Yellow roads

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, roadVertices);

        GLES20.glLineWidth(8f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, roadVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawBuildings() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 0.7f, 0.7f, 0.7f, 0.8f); // Gray buildings

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buildingVertices);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, buildingVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawMarkers() {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4f(colorHandle, 0.9f, 0.2f, 0.2f, 1.0f); // Red markers

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, markerVertices);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, markerVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void executeLoad() {
        if (!isScrolling) {
            return;
        }

        // Check and execute long frame load
        if (LoadType.isLongFrameLoad(loadType)) {
            checkAndExecuteLongFrame();
        }

        // Handle different load types
        if (LoadType.isMixedLoad(loadType)) {
            // For mixed load: execute both doFrame load (simulated here as in-frame) and
            // between-frame load
            if (mLoadSimulator != null) {
                // 1. Execute doFrame part
                mLoadSimulator.executeDoFrameLoad(loadType, "GLMap_doFrameLoad");

                // 2. Execute between-frame part of mixed load
                mLoadSimulator.executeBetweenFrameLoad(loadType, "GLMap_betweenFrameLoad");
            }
        } else if (LoadType.isBetweenFramesLoad(loadType)) {
            // For pure between-frames load
            if (mLoadSimulator != null) {
                mLoadSimulator.executePureBetweenFrameLoad(loadType, "GLMap_pureBetweenFrameLoad");
            }
        } else {
            // For standard in-frame load (Minimal, Light, Medium, Heavy, LongFrame
            // triggers)
            if (mLoadSimulator != null) {
                mLoadSimulator.executeInFrameLoad(loadType, "GLMap_inFrameLoad");
            }
        }

        executeRendererSpecificLoad();
    }

    private void executeRendererSpecificLoad() {
        int iterations = getRendererSpecificIterations();
        if (iterations <= 0) {
            return;
        }

        double acc = rendererLoadAccumulator + rendererFrameCounter * 0.001d;
        for (int i = 0; i < iterations; i++) {
            double x = rendererLoadRandom.nextDouble() * (i + 1);
            double y = rendererLoadRandom.nextDouble() * (rendererFrameCounter + 1);
            acc += Math.sin(x + acc * 0.0001d) * Math.cos(y);
            acc = acc * 0.999999d + (x - y) * 0.000001d;
        }

        rendererLoadAccumulator = acc;
        rendererFrameCounter++;
    }

    private int getRendererSpecificIterations() {
        switch (loadType) {
            case LoadType.MINIMAL:
                return 0;
            case LoadType.LIGHT:
            case LoadType.LIGHT_BETWEEN_FRAMES:
            case LoadType.LIGHT_MIXED:
                return LIGHT_SCROLL_EXTRA_ITERATIONS;
            case LoadType.MEDIUM:
            case LoadType.MEDIUM_BETWEEN_FRAMES:
            case LoadType.MEDIUM_MIXED:
                return MEDIUM_SCROLL_EXTRA_ITERATIONS;
            case LoadType.HEAVY:
            case LoadType.HEAVY_BETWEEN_FRAMES:
            case LoadType.HEAVY_MIXED:
                return HEAVY_SCROLL_EXTRA_ITERATIONS;
            case LoadType.LONG_FRAME:
                return LONG_FRAME_SCROLL_EXTRA_ITERATIONS;
            default:
                return 0;
        }
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }
}
