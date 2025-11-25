package com.example.wechatfriendforglmap;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Trace;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 2.0 renderer for map-like visualization.
 * Renders a grid of tiles with roads and landmarks, similar to map apps.
 */
public class GLMapRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLMapRenderer";
    
    private int loadType = LoadProfile.LOAD_TYPE_MINIMAL;
    
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
    
    private int gridVertexCount;
    private int roadVertexCount;
    private int buildingVertexCount;
    private int markerVertexCount;
    
    private final Random random = new Random(12345L);
    
    // Shaders
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "  gl_PointSize = 20.0;" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    public void setLoadType(@LoadProfile.LoadType int loadType) {
        this.loadType = loadType;
    }
    
    public void setOffset(float dx, float dy) {
        this.offsetX += dx / 500f / zoom;
        this.offsetY -= dy / 500f / zoom;
    }
    
    public void setZoom(float scaleFactor) {
        this.zoom *= scaleFactor;
        this.zoom = Math.max(0.5f, Math.min(5f, zoom));
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
        
        // Draw map layers
        drawGrid();
        drawRoads();
        drawBuildings();
        drawMarkers();
        
        // Execute load
        executeLoad();
        
        Trace.endSection();
    }
    
    private void generateMapGeometry() {
        // Generate grid
        int gridSize = 20;
        float[] gridCoords = new float[gridSize * 4 * 3 * 2]; // lines * 2 points * 3 coords * 2 directions
        int idx = 0;
        
        for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
            float pos = i * 0.2f;
            // Vertical lines
            gridCoords[idx++] = pos; gridCoords[idx++] = -2f; gridCoords[idx++] = 0f;
            gridCoords[idx++] = pos; gridCoords[idx++] = 2f; gridCoords[idx++] = 0f;
            // Horizontal lines
            gridCoords[idx++] = -2f; gridCoords[idx++] = pos; gridCoords[idx++] = 0f;
            gridCoords[idx++] = 2f; gridCoords[idx++] = pos; gridCoords[idx++] = 0f;
        }
        gridVertexCount = idx / 3;
        gridVertices = createFloatBuffer(gridCoords, idx);
        
        // Generate roads
        Random r = new Random(42);
        int roadCount = 15;
        float[] roadCoords = new float[roadCount * 6];
        for (int i = 0; i < roadCount; i++) {
            float x = (r.nextFloat() - 0.5f) * 3f;
            float y = (r.nextFloat() - 0.5f) * 3f;
            float dx = (r.nextFloat() - 0.5f) * 1.5f;
            float dy = (r.nextFloat() - 0.5f) * 1.5f;
            roadCoords[i * 6] = x; roadCoords[i * 6 + 1] = y; roadCoords[i * 6 + 2] = 0f;
            roadCoords[i * 6 + 3] = x + dx; roadCoords[i * 6 + 4] = y + dy; roadCoords[i * 6 + 5] = 0f;
        }
        roadVertexCount = roadCount * 2;
        roadVertices = createFloatBuffer(roadCoords, roadCoords.length);
        
        // Generate buildings (as quads)
        int buildingCount = 30;
        float[] buildingCoords = new float[buildingCount * 18]; // 6 vertices per quad (2 triangles)
        for (int i = 0; i < buildingCount; i++) {
            float x = (r.nextFloat() - 0.5f) * 3f;
            float y = (r.nextFloat() - 0.5f) * 3f;
            float size = 0.05f + r.nextFloat() * 0.1f;
            
            int offset = i * 18;
            // Triangle 1
            buildingCoords[offset] = x; buildingCoords[offset + 1] = y; buildingCoords[offset + 2] = 0f;
            buildingCoords[offset + 3] = x + size; buildingCoords[offset + 4] = y; buildingCoords[offset + 5] = 0f;
            buildingCoords[offset + 6] = x + size; buildingCoords[offset + 7] = y + size; buildingCoords[offset + 8] = 0f;
            // Triangle 2
            buildingCoords[offset + 9] = x; buildingCoords[offset + 10] = y; buildingCoords[offset + 11] = 0f;
            buildingCoords[offset + 12] = x + size; buildingCoords[offset + 13] = y + size; buildingCoords[offset + 14] = 0f;
            buildingCoords[offset + 15] = x; buildingCoords[offset + 16] = y + size; buildingCoords[offset + 17] = 0f;
        }
        buildingVertexCount = buildingCount * 6;
        buildingVertices = createFloatBuffer(buildingCoords, buildingCoords.length);
        
        // Generate markers
        int markerCount = 10;
        float[] markerCoords = new float[markerCount * 3];
        for (int i = 0; i < markerCount; i++) {
            markerCoords[i * 3] = (r.nextFloat() - 0.5f) * 2.5f;
            markerCoords[i * 3 + 1] = (r.nextFloat() - 0.5f) * 2.5f;
            markerCoords[i * 3 + 2] = 0f;
        }
        markerVertexCount = markerCount;
        markerVertices = createFloatBuffer(markerCoords, markerCoords.length);
    }
    
    private FloatBuffer createFloatBuffer(float[] coords, int count) {
        ByteBuffer bb = ByteBuffer.allocateDirect(count * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords, 0, count);
        fb.position(0);
        return fb;
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
        int iterations;
        switch (loadType) {
            case LoadProfile.LOAD_TYPE_MINIMAL:
                iterations = 0;
                break;
            case LoadProfile.LOAD_TYPE_LIGHT:
            case LoadProfile.LOAD_TYPE_LIGHT_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_LIGHT_MIXED:
                iterations = 200;
                break;
            case LoadProfile.LOAD_TYPE_MEDIUM:
            case LoadProfile.LOAD_TYPE_MEDIUM_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_MEDIUM_MIXED:
                iterations = 1000;
                break;
            case LoadProfile.LOAD_TYPE_HEAVY:
            case LoadProfile.LOAD_TYPE_HEAVY_BETWEEN_FRAMES:
            case LoadProfile.LOAD_TYPE_HEAVY_MIXED:
                iterations = 5000;
                break;
            default:
                iterations = 0;
        }
        
        if (iterations == 0) return;
        
        Trace.beginSection("GLMap_executeLoad");
        double sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += Math.sin(i * 0.1) * Math.cos(i * 0.1) + Math.sqrt(i + 1);
        }
        Trace.endSection();
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

