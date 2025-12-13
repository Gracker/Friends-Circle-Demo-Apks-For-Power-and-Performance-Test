package com.example.launch.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.launch.common.LoadSimulator
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer(
    private val context: Context,
    private val loadType: LoadSimulator.LoadType,
    private val onGameReady: () -> Unit
) : GLSurfaceView.Renderer {

    private var isEngineReady = false
    private var isAssetsLoaded = false

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        initEngine()
        isEngineReady = true
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    fun startAssetLoading() {
        simulateAssetUpload()
        isAssetsLoaded = true
        onGameReady()
    }

    private fun initEngine() {
        val shaderCode = "void main() { gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0); }"
        repeat(if (loadType == LoadSimulator.LoadType.HEAVY) 50 else 5) {
            loadShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
        }
    }

    private fun simulateAssetUpload() {
        val random = Random(12345)
        
        // 1. Texture Upload
        if (loadType == LoadSimulator.LoadType.MEDIUM || loadType == LoadSimulator.LoadType.HEAVY) {
            val size = if (loadType == LoadSimulator.LoadType.HEAVY) 2048 else 512
            val pixels = ByteBuffer.allocateDirect(size * size * 4)
            val bytes = ByteArray(size * size * 4)
            random.nextBytes(bytes)
            pixels.put(bytes)
            pixels.position(0)

            val textureHandle = IntArray(1)
            GLES20.glGenTextures(1, textureHandle, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, size, size, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        }
        
        // 2. State Thrashing (Switching GL States repeatedly) - GL Specific Bottleneck
        if (loadType == LoadSimulator.LoadType.HEAVY) {
             val iterations = 500
             GLES20.glEnable(GLES20.GL_BLEND)
             repeat(iterations) {
                 if (it % 2 == 0) GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
                 else GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
                 
                 // Dummy bind
                 GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
             }
             GLES20.glDisable(GLES20.GL_BLEND)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
