package com.wilbert.svcamera.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.wilbert.svcamera.filters.GLImageFilter;
import com.wilbert.svcamera.filters.GLImageGaussianBlurFilter;
import com.wilbert.svcamera.filters.GLImageOESInputFilter;
import com.wilbert.svcamera.glutils.OpenGLUtils;
import com.wilbert.svcamera.glutils.TextureRotationUtils;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TextureRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, IRender {
    final String TAG = "TextureRender";
    SurfaceTexture mSurfaceTexture;
    GLImageOESInputFilter mInputFilter;
    GLImageGaussianBlurFilter mGaussianFilter;
    GLImageFilter mOutputFilter;
    boolean mNeedGaussian = false;
    int mTextureId = OpenGLUtils.GL_NOT_TEXTURE;
    protected final float[] mSTMatrix = new float[16];
    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureBuffer;
    AtomicBoolean mSurfaceCreated = new AtomicBoolean(false);
    int mSurfaceWidth = 0;
    int mSurfaceHeight = 0;
    int mPreviewWidth = 0;
    int mPreviewHeight = 0;
    GLSurfaceView mSurfaceView;
    Object mLock = new Object();

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG,"onSurfaceCreated");
        mTextureId = OpenGLUtils.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);
        mSurfaceCreated.set(true);
        synchronized (mLock){
            mLock.notifyAll();
        }
    }

    @Override
    public void init(GLSurfaceView surfaceView) {
        synchronized (mLock){
            mSurfaceView = surfaceView;
            mSurfaceView.setEGLContextClientVersion(2);
            mSurfaceView.setRenderer(this);
            mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture(){
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
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
        if(mInputFilter != null){
            mInputFilter.onDisplaySizeChanged(width,height);
        }
        if(mNeedGaussian && mGaussianFilter != null){
            mGaussianFilter.onDisplaySizeChanged(width,height);
        }
        if(mOutputFilter != null){
            mOutputFilter.onDisplaySizeChanged(width,height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(!mSurfaceCreated.get() || mPreviewWidth <= 0 || mPreviewHeight <= 0){
            return;
        }
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        if(mInputFilter == null){
            mInputFilter = new GLImageOESInputFilter(mSurfaceView.getContext());
            mInputFilter.initFrameBuffer(mPreviewHeight,mPreviewWidth);
            mInputFilter.onInputSizeChanged(mPreviewHeight,mPreviewWidth);
            mInputFilter.onDisplaySizeChanged(mSurfaceWidth,mSurfaceHeight);
        }
        if(mNeedGaussian && mGaussianFilter == null){
            mGaussianFilter = new GLImageGaussianBlurFilter(mSurfaceView.getContext());
            mGaussianFilter.initFrameBuffer(mPreviewHeight,mPreviewWidth);
            mGaussianFilter.onInputSizeChanged(mPreviewHeight,mPreviewWidth);
            mGaussianFilter.onDisplaySizeChanged(mSurfaceWidth,mSurfaceHeight);
        }
        if(mOutputFilter == null){
            mOutputFilter = new GLImageFilter(mSurfaceView.getContext());
            mOutputFilter.onInputSizeChanged(mPreviewHeight,mPreviewWidth);
            mOutputFilter.onDisplaySizeChanged(mSurfaceWidth,mSurfaceHeight);
        }
        mInputFilter.setTextureTransformMatrix(mSTMatrix);
        int textureId = mInputFilter.drawFrameBuffer(mTextureId,mVertexBuffer,mTextureBuffer);
        if(mNeedGaussian) {
            textureId = mGaussianFilter.drawFrameBuffer(textureId, mVertexBuffer, mTextureBuffer);
        }
        mOutputFilter.drawFrame(textureId,mVertexBuffer,mTextureBuffer);
    }

    int avaliableTimes = 0;
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.i(TAG,"onFrameAvailable");
        avaliableTimes ++;
        mSurfaceView.requestRender();
    }
}
