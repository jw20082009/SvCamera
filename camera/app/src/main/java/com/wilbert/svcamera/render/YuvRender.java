package com.wilbert.svcamera.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.wilbert.svcamera.glutils.OpenGLUtils;
import com.wilbert.svcamera.glutils.TextureRotationUtils;
import com.wilbert.svcamera.glutils2.STGLRender;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class YuvRender  implements GLSurfaceView.Renderer, IRender  {
    final String TAG = "YuvRender";
    GLSurfaceView mSurfaceView;
    Object mLock = new Object();
    protected final float[] mSTMatrix = new float[16];
    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureBuffer;
    AtomicBoolean mSurfaceCreated = new AtomicBoolean(false);
    int mSurfaceWidth = 0;
    int mSurfaceHeight = 0;
    int mPreviewWidth = 0;
    int mPreviewHeight = 0;
    SurfaceTexture mSurfaceTexture;
    ByteBuffer frameRenderBuffer;
    boolean nv21YUVDataDirty = false;
    private int[] mTextureY;
    private int[] mTextureUV;
    private boolean mTextureInit = false;
    private STGLRender mGLRender;
    private byte[][] mDataBuffer = new byte[2][];
    private int bufferIndex = 0;
    private void setUpTexture(){
        // nv21 y texture
        mTextureY = new int[1];
        GLES20.glGenTextures(1, mTextureY, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureY[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // nv21 uv texture
        mTextureUV = new int[1];
        GLES20.glGenTextures(1, mTextureUV, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureUV[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void updateNV21YUVTexture() {
        if (!mTextureInit) {
            frameRenderBuffer.position(0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureY[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mPreviewHeight, mPreviewWidth, 0,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
            frameRenderBuffer.position(4 * (mPreviewWidth / 2) * (mPreviewHeight / 2));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureUV[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, mPreviewHeight / 2, mPreviewWidth / 2, 0,
                    GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
            mTextureInit = true;
        } else {
            frameRenderBuffer.position(0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureY[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mPreviewHeight, mPreviewWidth, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
            frameRenderBuffer.position(4 * (mPreviewWidth / 2) * (mPreviewHeight / 2));
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureUV[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,  mPreviewHeight / 2, mPreviewWidth / 2, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        setUpTexture();
    }

    boolean mInit = false;

    @Override
    public void onDrawFrame(GL10 gl) {
        if(mDataBuffer == null || mDataBuffer.length < 2 || mPreviewWidth <= 0 || mPreviewHeight <= 0){
            return;
        }
        if(!mInit) {
            mGLRender.init(mPreviewWidth, mPreviewHeight);
            mGLRender.calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, mPreviewWidth, mPreviewHeight);
            mGLRender.init(mPreviewWidth, mPreviewHeight);
            mInit = true;
        }
        if(nv21YUVDataDirty){
            updateFrameWhenDirty(mDataBuffer[bufferIndex]);
            updateNV21YUVTexture();
        }
        int textureId = mGLRender.YUV2RGB(mTextureY[0], mTextureUV[0], bufferIndex%2== 0);
        mGLRender.onDrawFrame(textureId);
    }

    @Override
    public void init(GLSurfaceView surfaceView) {
        Log.i(TAG,"onSurfaceCreated");
        mSurfaceTexture = new SurfaceTexture(0);
        mSurfaceTexture.detachFromGLContext();
        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);
        mGLRender = new STGLRender();
        synchronized (mLock){
            mSurfaceView = surfaceView;
            mSurfaceView.setEGLContextClientVersion(2);
            mSurfaceView.setRenderer(this);
            mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
        mSurfaceCreated.set(true);
        synchronized (mLock){
            mLock.notifyAll();
        }
    }

    public void adjustTextureBuffer(int orientation,boolean flipHorizontal, boolean flipVertical){
        mGLRender.adjustTextureBuffer(orientation,flipHorizontal,flipVertical);
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        synchronized (mLock){
            if(mSurfaceTexture == null){
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return mSurfaceTexture;
        }
    }

    @Override
    public void onCameraOpened(int width, int height) {
        mPreviewWidth = height;
        mPreviewHeight = width;
        frameRenderBuffer = ByteBuffer.allocateDirect(width * height * 3/2);
    }

    private void updateFrameWhenDirty(byte[] data) {
        frameRenderBuffer.clear();
        frameRenderBuffer.position(0);
        frameRenderBuffer.put(data);
        frameRenderBuffer.position(0);
        mSurfaceView.requestRender();
        nv21YUVDataDirty = false;
    }

    public void onFrameAvailable(byte[] data){
        if(mDataBuffer == null){
            mDataBuffer = new byte[2][];
        }
        bufferIndex = (bufferIndex+1)%2;
        mDataBuffer[bufferIndex] = data;
        nv21YUVDataDirty = true;
        mSurfaceView.requestRender();
    }
}
