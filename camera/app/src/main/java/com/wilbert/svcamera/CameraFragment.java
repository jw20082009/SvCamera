package com.wilbert.svcamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.wilbert.svcamera.filters.GLImageFilter;
import com.wilbert.svcamera.filters.GLImageOESInputFilter;
import com.wilbert.svcamera.glutils.OpenGLUtils;
import com.wilbert.svcamera.glutils.TextureRotationUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    GLSurfaceView mSurfaceView;
    CameraHandler mHandler;

    AtomicBoolean mSurfaceCreated = new AtomicBoolean(false);
    AtomicBoolean mCameraOpened = new AtomicBoolean(false);
    int mSurfaceWidth = 0;
    int mSurfaceHeight = 0;
    int mPreviewWidth = 960;
    int mPreviewHeight = 540;
    int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    int mTextureId = OpenGLUtils.GL_NOT_TEXTURE;
    SurfaceTexture mSurfaceTexture;
    GLImageOESInputFilter mInputFilter;
    GLImageFilter mOutputFilter;
    protected final float[] mSTMatrix = new float[16];
    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureBuffer;

    Object mLock = new Object();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_camera,container,false);
        mSurfaceView = layout.findViewById(R.id.gl_surfaceview);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        initCamera();
        return layout;
    }

    private void initCamera(){
        HandlerThread thread = new HandlerThread("CameraThread");
        thread.start();
        mHandler = new CameraHandler(thread.getLooper());
    }

    @Override
    public void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSurfaceView.onPause();
        mSurfaceCreated.set(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.release();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureId = OpenGLUtils.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);
        mSurfaceCreated.set(true);
        synchronized (mLock){
            mLock.notifyAll();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
        if(mInputFilter != null){
            mInputFilter.onDisplaySizeChanged(width,height);
        }
        if(mOutputFilter != null){
            mOutputFilter.onDisplaySizeChanged(width,height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(!mSurfaceCreated.get() || !mCameraOpened.get()){
            return;
        }
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        if(mInputFilter == null){
            mInputFilter = new GLImageOESInputFilter(getContext());
            mInputFilter.initFrameBuffer(mPreviewHeight,mPreviewWidth);
            mInputFilter.onInputSizeChanged(mPreviewHeight,mPreviewWidth);
            mInputFilter.onDisplaySizeChanged(mSurfaceWidth,mSurfaceHeight);
        }
        if(mOutputFilter == null){
            mOutputFilter = new GLImageFilter(getContext());
            mOutputFilter.onInputSizeChanged(mPreviewHeight,mPreviewWidth);
            mOutputFilter.onDisplaySizeChanged(mSurfaceWidth,mSurfaceHeight);
        }
        mInputFilter.setTextureTransformMatrix(mSTMatrix);
        int textureId = mInputFilter.drawFrameBuffer(mTextureId,mVertexBuffer,mTextureBuffer);
        mOutputFilter.drawFrame(textureId,mVertexBuffer,mTextureBuffer);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSurfaceView.requestRender();
    }


    class CameraHandler extends Handler{

        Camera camera;
        Camera.CameraInfo info;

        public static final int MSG_CAMERA_START = 0x01;

        public static final int MSG_CAMERA_PREVIEW = 0x02;

        public CameraHandler(Looper looper){
            super(looper);
            sendEmptyMessage(MSG_CAMERA_START);
            sendEmptyMessage(MSG_CAMERA_PREVIEW);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CAMERA_START:
                    if(mCameraOpened.get())
                        return;
                    info = new Camera.CameraInfo();
                    Camera.getCameraInfo(mCameraId,info);
                    camera = Camera.open(mCameraId);
                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setPreviewSize(mPreviewWidth,mPreviewHeight);
                    parameters.setPictureSize(1280,720);
                    camera.setParameters(parameters);
                    mCameraOpened.set(true);
                    break;
                case MSG_CAMERA_PREVIEW:{
                    if(camera == null){
                        break;
                    }
                    if(!mSurfaceCreated.get()){
                        synchronized (mLock){
                            try {
                                mLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        camera.setDisplayOrientation(calcPreviewOrientation(getContext(),info));
                        camera.setPreviewTexture(mSurfaceTexture);
                        mSurfaceTexture.setOnFrameAvailableListener(CameraFragment.this);
                        camera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                    break;
            }
        }

        public void release(){
            post(new Runnable() {
                @Override
                public void run() {
                    if(camera != null){
                        camera.release();
                        camera = null;
                    }
                    mCameraOpened.set(false);
                    getLooper().quit();
                }
            });
        }

        private int calcPreviewOrientation(Context context, Camera.CameraInfo info){
            int rotation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation){
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }
            int result;
            if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                result = (info.orientation + degrees)%360;
                result = (360 - result)%360;
            }else{
                result = (info.orientation - degrees + 360)%360;
            }
            return result;
        }
    }

}
