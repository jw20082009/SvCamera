package com.wilbert.svcamera;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

import com.wilbert.svcamera.filters.GLImageFilter;
import com.wilbert.svcamera.filters.GLImageOESInputFilter;
import com.wilbert.svcamera.glutils.OpenGLUtils;
import com.wilbert.svcamera.glutils.TextureRotationUtils;
import com.wilbert.svcamera.metering.RectDrawer;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author wilbert
 * @Date 2021/2/18 22:16
 * @email jiangwang.wilbert@bigo.sg
 **/
public class CameraRenderer implements GLSurfaceView.Renderer {
    private final String TAG = "CameraRenderer";
    int mTextureId = OpenGLUtils.GL_NOT_TEXTURE;
    SurfaceTexture mSurfaceTexture;
    GLImageOESInputFilter mInputFilter;
    GLImageFilter mOutputFilter;
    int mSurfaceWidth = 0;
    int mSurfaceHeight = 0;
    final float[] mSTMatrix = new float[16];
    FloatBuffer mVertexBuffer;
    FloatBuffer mTextureBuffer;
    GLSurfaceView mSurfaceView;
    int mPreviewWidth,mPreviewHeight;
    RectDrawer drawer = null;
    boolean mSurfaceInit = false;
    boolean mSurfaceCreated = false;

    public CameraRenderer(GLSurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void onPreviewSizeChanged(int width,int height){
        mPreviewWidth = width;
        mPreviewHeight = height;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture){
        mSurfaceTexture = surfaceTexture;
        if(mSurfaceCreated){
            mSurfaceView.requestRender();
        }
    }

    private void initSurface(){
        if(mSurfaceTexture == null || mSurfaceInit){
            return;
        }
        mTextureId = OpenGLUtils.createOESTexture();
        mSurfaceTexture.attachToGLContext(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(frameAvailableListener);
        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);
        drawer = new RectDrawer();
        mSurfaceInit = true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initSurface();
        mSurfaceCreated = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
        if (mInputFilter != null) {
            mInputFilter.onDisplaySizeChanged(width, height);
        }
        if (mOutputFilter != null) {
            mOutputFilter.onDisplaySizeChanged(width, height);
        }
    }

    int i = 0;
    long time = 0;
    float lastFps = 0;

    @Override
    public void onDrawFrame(GL10 gl) {
        if(mPreviewWidth <= 0 || mPreviewHeight <= 0){
            return;
        }
        initSurface();
        if(i%4 == 0){
            long current = SystemClock.elapsedRealtime();
            final float fps = 1000.0f/((current - time)/4);
            time = current;
            if(Math.abs(fps - lastFps)>= 0.01){
                Log.i(TAG,"fps:"+fps);
                lastFps = fps;
            }
        }
        i++;
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        if (mInputFilter == null) {
            mInputFilter = new GLImageOESInputFilter(mSurfaceView.getContext());
            mInputFilter.initFrameBuffer(mPreviewHeight, mPreviewWidth);
            mInputFilter.onInputSizeChanged(mPreviewHeight, mPreviewWidth);
            mInputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
        }
        if (mOutputFilter == null) {
            mOutputFilter = new GLImageFilter(mSurfaceView.getContext());
            mOutputFilter.onInputSizeChanged(mPreviewHeight, mPreviewWidth);
            mOutputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
        }
        mInputFilter.setTextureTransformMatrix(mSTMatrix);
        int textureId = mInputFilter.drawFrameBuffer(mTextureId, mVertexBuffer, mTextureBuffer);
        mOutputFilter.drawFrame(textureId, mVertexBuffer, mTextureBuffer);
    }

    public void release() {
        if (mInputFilter != null) {
            mInputFilter.release();
            mInputFilter = null;
        }
        if (mOutputFilter != null) {
            mOutputFilter.release();
            mOutputFilter = null;
        }
    }

    SurfaceTexture.OnFrameAvailableListener frameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mSurfaceView.requestRender();
        }
    };
}
