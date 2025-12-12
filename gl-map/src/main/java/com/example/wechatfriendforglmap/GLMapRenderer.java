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
    private FloatBuffer buildingVertices;
    private FloatBuffer markerVertices;
    private FloatBuffer waterVertices;
    private FloatBuffer parkVertices;

    private int gridVertexCount;
    private int roadVertexCount;
    private int buildingVertexCount;
    private int markerVertexCount;
    private int waterVertexCount;
    private int parkVertexCount;

    private final Random random = new Random(LoadConfig.COMPUTATION_SEED);

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
        }
    }

    public void release() {
        if (mLoadSimulator != null) {
            mLoadSimulator.release();
            mLoadSimulator = null;
        }
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
        drawWater(); // Bottom layer
        drawParks();
        drawGrid();
        drawRoads();
        drawBuildings();
        drawMarkers(); // Top layer

        // Execute load
        executeLoad();

        Trace.endSection();
    }

    private void generateMapGeometry() {
        Random r = new Random(LoadConfig.DATA_GENERATION_SEED);

        float range = 100f; // Map size range (-50 to 50)

        // 1. Generate Water Bodies (Lakes)
        int waterCount = 50;
        float[] waterCoords = new float[waterCount * 6 * 3]; // 2 triangles per quad * 3 vertices * 3 coords
        for (int i = 0; i < waterCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            float w = 0.5f + r.nextFloat() * 4.0f;
            float h = 0.3f + r.nextFloat() * 3.0f;
            addQuadToBuffer(waterCoords, i * 18, x, y, w, h);
        }
        waterVertexCount = waterCount * 6;
        waterVertices = createFloatBuffer(waterCoords, waterCoords.length);

        // 2. Generate Parks
        int parkCount = 80;
        float[] parkCoords = new float[parkCount * 6 * 3];
        for (int i = 0; i < parkCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            float w = 0.3f + r.nextFloat() * 2.5f;
            float h = 0.3f + r.nextFloat() * 2.5f;
            addQuadToBuffer(parkCoords, i * 18, x, y, w, h);
        }
        parkVertexCount = parkCount * 6;
        parkVertices = createFloatBuffer(parkCoords, parkCoords.length);

        // 3. Generate Grid
        int gridSize = 200;
        float[] gridCoords = new float[gridSize * 4 * 3 * 2]; // lines * 2 points * 3 coords * 2 directions
        int idx = 0;

        for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
            float pos = i * 0.5f; // Grid spacing
            float limit = gridSize / 2 * 0.5f;

            // Vertical lines
            gridCoords[idx++] = pos;
            gridCoords[idx++] = -limit;
            gridCoords[idx++] = 0f;
            gridCoords[idx++] = pos;
            gridCoords[idx++] = limit;
            gridCoords[idx++] = 0f;
            // Horizontal lines
            gridCoords[idx++] = -limit;
            gridCoords[idx++] = pos;
            gridCoords[idx++] = 0f;
            gridCoords[idx++] = limit;
            gridCoords[idx++] = pos;
            gridCoords[idx++] = 0f;
        }
        gridVertexCount = idx / 3;
        gridVertices = createFloatBuffer(gridCoords, idx);

        // 4. Generate Roads
        int roadCount = 300;
        float[] roadCoords = new float[roadCount * 6];
        for (int i = 0; i < roadCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            float dx = (r.nextFloat() - 0.5f) * 10f;
            float dy = (r.nextFloat() - 0.5f) * 10f;
            roadCoords[i * 6] = x;
            roadCoords[i * 6 + 1] = y;
            roadCoords[i * 6 + 2] = 0f;
            roadCoords[i * 6 + 3] = x + dx;
            roadCoords[i * 6 + 4] = y + dy;
            roadCoords[i * 6 + 5] = 0f;
        }
        roadVertexCount = roadCount * 2;
        roadVertices = createFloatBuffer(roadCoords, roadCoords.length);

        // 5. Generate Buildings (as quads)
        int buildingCount = 500;
        float[] buildingCoords = new float[buildingCount * 18]; // 6 vertices per quad (2 triangles)
        for (int i = 0; i < buildingCount; i++) {
            float x = (r.nextFloat() - 0.5f) * range;
            float y = (r.nextFloat() - 0.5f) * range;
            float size = 0.2f + r.nextFloat() * 0.5f;
            addQuadToBuffer(buildingCoords, i * 18, x, y, size, size);
        }
        buildingVertexCount = buildingCount * 6;
        buildingVertices = createFloatBuffer(buildingCoords, buildingCoords.length);

        // 6. Generate Markers
        int markerCount = 100;
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
        // Check and execute long frame load
        if (LoadType.isLongFrameLoad(loadType) && isScrolling) {
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
