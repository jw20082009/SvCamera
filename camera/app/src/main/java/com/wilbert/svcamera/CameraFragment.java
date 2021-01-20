package com.wilbert.svcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.preference.Preference;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wilbert.svcamera.dev.Camera2Handler;
import com.wilbert.svcamera.dev.CameraHandler;
import com.wilbert.svcamera.dev.FrameProcessor;
import com.wilbert.svcamera.dev.ICameraHandler;
import com.wilbert.svcamera.dev.ICameraListener;
import com.wilbert.svcamera.filters.GLImageFilter;
import com.wilbert.svcamera.filters.GLImageOESInputFilter;
import com.wilbert.svcamera.glutils.OpenGLUtils;
import com.wilbert.svcamera.glutils.TextureRotationUtils;
import com.wilbert.svcamera.metering.RectDrawer;
import com.wilbert.svcamera.views.FocusView;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener, View.OnClickListener {
    private final String TAG = "CameraFragment";
    GLSurfaceView mSurfaceView;
    FocusView mFocusView;
    TextView mTvUp,mTvDown,mTvTips,mTvSwitch,mTvApi,mTvBrand,mTvSwitchFps;
    LinearLayout mTestLinear;
    SeekBar mSeekBar;
    ICameraHandler mCameraHandler;
    IPreviewListener mPreviewListener;

    AtomicBoolean mSurfaceCreated = new AtomicBoolean(false);

    int mSurfaceWidth = 0;
    int mSurfaceHeight = 0;
    int mPreviewWidth = 1280;
    int mPreviewHeight = 720;

    int mTextureId = OpenGLUtils.GL_NOT_TEXTURE;

    SurfaceTexture mSurfaceTexture;
    GLImageOESInputFilter mInputFilter;
    GLImageFilter mOutputFilter;
    final float[] mSTMatrix = new float[16];
    FloatBuffer mVertexBuffer;
    FloatBuffer mTextureBuffer;
    NV21Processor mFrameProcessor;

    Object mLock = new Object();
    int screenWidth;
    int screenHeight;
    byte[] mYuvData;
    float[] mTestRect = new float[8];
    Matrix matrix;
    private void setMeteringFlag(){
//        Preference preference = getContext().getSharedPreferences("mediaReport", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_camera,container,false);
        mSurfaceView = layout.findViewById(R.id.gl_surfaceview);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mTestLinear = layout.findViewById(R.id.ll_test_view);
        mTvTips = layout.findViewById(R.id.tv_tips);
        mTvSwitchFps = layout.findViewById(R.id.tv_switch_fps);
        mTvSwitchFps.setOnClickListener(this);
        mTvUp = layout.findViewById(R.id.tv_up);
        mTvUp.setOnClickListener(this);
        mTvDown = layout.findViewById(R.id.tv_down);
        mTvDown.setOnClickListener(this);
        mTvSwitch = layout.findViewById(R.id.tv_switch);
        mTvSwitch.setOnClickListener(this);
        mTvApi = layout.findViewById(R.id.tv_switchApi);
        mTvApi.setOnClickListener(this);
        mTvBrand = layout.findViewById(R.id.tv_brand);
        mTvBrand.setText(Build.BRAND);
        mSeekBar = layout.findViewById(R.id.zoomBar);
        mSeekBar.setOnSeekBarChangeListener(zoomListener);
        mFocusView = layout.findViewById(R.id.focusView);
        mFocusView.setFocusListener(mFocusListener);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        float[] p = new float[]{1,1};
        Matrix matrix = new Matrix();
//        matrix.setRotate(90,2,2);
        matrix.setScale(1,-1,2,2);
        matrix.mapPoints(p);
        Log.e("mapPoints","point:"+p[0]+"*"+p[1]);
        int width = screenWidth;
        int height = screenHeight;
        if(1.0f * screenWidth/screenHeight > 720.0f/1280.0f){
            width = (int) (screenHeight * 720f/1280f);
        }else{
            height = (int)(screenWidth /(720f/1280f));
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        params.width = width;
        params.height = height;
        mSurfaceView.setLayoutParams(params);
        mSurfaceView.setKeepScreenOn(true);
        initCamera();

        return layout;
    }

    FocusView.FocusListener mFocusListener = new FocusView.FocusListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onFocusEvent(MotionEvent event) {
            if(getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }else if(getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }else{
                float x = event.getX();
                float y = event.getY();
                int width = mFocusView.getWidth();
                int height = mFocusView.getHeight();
                Log.e(TAG,"onSingleTapConfirmed:"+x+"*"+y+";"+width+"*"+height);
                mCameraHandler.requestFocus(x,y,mFocusView.getWidth(),mFocusView.getHeight());
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initCamera(){
        HandlerThread thread = new HandlerThread("CameraThread");
        thread.start();
        mCameraHandler = new CameraHandler(getContext(),thread.getLooper());
//        mCameraHandler = new Camera2Handler(getContext(),thread.getLooper());
//        mFrameProcessor = new NV21Processor();
//        mCameraHandler.setFrameProcessor(mFrameProcessor);

        mPreviewListener = mCameraHandler.init(mCameraListener,mPreviewWidth,mPreviewHeight);
    }

    ICameraListener mCameraListener = new ICameraListener() {
        @Override
        public void onSwitchStabilization(final int status) {
            mTvDown.post(new Runnable() {
                @Override
                public void run() {
                    switch (status){
                        case 0:
                            mTvDown.setText("Camera Stabilization closed");
                            break;
                        case 1:
                            mTvDown.setText("Camera Stabilization opened");
                            break;
                        default:
                            mTvDown.setText("Camera Stabilization not supported");
                            break;
                    }
                }
            });
        }

        @Override
        public void onSwitchFps(final int[] fpsRange) {
            mTvSwitchFps.post(new Runnable() {
                @Override
                public void run() {
                    mTvSwitchFps.setText(fpsRange[0]+"*"+fpsRange[1]);
                }
            });
        }

        @Override
        public void onCameraOpened(final String msg) {
            mTvTips.post(new Runnable() {
                @Override
                public void run() {
                    mTvTips.setText(msg);
                }
            });
        }

        @Override
        public void onMeteringSwitch(Rect rect) {
            if(rect == null){
                return;
            }
            if(matrix == null){
                matrix = new Matrix();
                matrix.setScale(-1,1);
            }
            RectF rectF = new RectF(rect);
            matrix.mapRect(rectF);
            float left = rectF.left/1000;
            float top = rectF.top / 1000;
            float right = rectF.right / 1000;
            float bottom = rectF.bottom / 1000;
            mTestRect[0] = left;
            mTestRect[1] = top;
            mTestRect[2] = left;
            mTestRect[3] = bottom;
            mTestRect[4] = right;
            mTestRect[5] = bottom;
            mTestRect[6] = right;
            mTestRect[7] = top;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseGLRes();
    }

    private void releaseGLRes(){
        synchronized (mLock) {
            mInputFilter = null;
            mOutputFilter = null;
        }
        mSurfaceView.onPause();
        if(mPreviewListener != null){
            mPreviewListener.onSurfaceDestroy();
        }
        mSurfaceCreated.set(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraHandler.release();
    }

    RectDrawer drawer = null;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureId = OpenGLUtils.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
        mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);
        if(mPreviewListener != null){
            mPreviewListener.onSurfaceCreated(mSurfaceTexture);
        }
        drawer = new RectDrawer();
        mSurfaceCreated.set(true);
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

    int i = 0;
    long time = 0;
    float lastFps = 0;

    @Override
    public void onDrawFrame(GL10 gl) {
        if(!mSurfaceCreated.get() || !mCameraHandler.isCameraOpened()){
            return;
        }
        if(i%4 == 0){
            long current = SystemClock.elapsedRealtime();
            final float fps = 1000.0f/((current - time)/4);
            time = current;
            if(Math.abs(fps - lastFps)>= 0.01){
                mTvUp.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvUp.setText("fps:"+fps);
                    }
                });
                lastFps = fps;
            }
        }
        i++;
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        synchronized (mLock) {
            if (mInputFilter == null) {
                mInputFilter = new GLImageOESInputFilter(getContext());
                mInputFilter.initFrameBuffer(mPreviewHeight, mPreviewWidth);
                mInputFilter.onInputSizeChanged(mPreviewHeight, mPreviewWidth);
                mInputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
            }
            if (mOutputFilter == null) {
                mOutputFilter = new GLImageFilter(getContext());
                mOutputFilter.onInputSizeChanged(mPreviewHeight, mPreviewWidth);
                mOutputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
            }
            mInputFilter.setTextureTransformMatrix(mSTMatrix);
            int textureId = mInputFilter.drawFrameBuffer(mTextureId, mVertexBuffer, mTextureBuffer);
            mOutputFilter.drawFrame(textureId, mVertexBuffer, mTextureBuffer);

            drawer.draw(mTestRect);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSurfaceView.requestRender();
    }

    SeekBar.OnSeekBarChangeListener zoomListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser){
                mCameraHandler.zoom(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_switch_fps:{
                mCameraHandler.switchFps();
            }
                break;
            case R.id.tv_up:
                break;
            case R.id.tv_down:
                mCameraHandler.switchStabilization();
                break;
            case R.id.tv_switch:
                releaseGLRes();
                mSurfaceView.onPause();
                mSurfaceView.onResume();
                mCameraHandler.switchCamera();
                break;
            case R.id.tv_switchApi:
                if(mCameraHandler != null){
                    mCameraHandler.release();
                }
                if(mCameraHandler instanceof CameraHandler){
                    HandlerThread thread = new HandlerThread("CameraThread");
                    thread.start();
                    mCameraHandler = new Camera2Handler(getContext(),thread.getLooper());
                    mTvApi.post(new Runnable() {
                        @Override
                        public void run() {
                            mTvApi.setText("Camera 2 API");
                        }
                    });
                }else{
                    HandlerThread thread = new HandlerThread("CameraThread");
                    thread.start();
                    mCameraHandler = new CameraHandler(getContext(),thread.getLooper());
                    mTvApi.post(new Runnable() {
                        @Override
                        public void run() {
                            mTvApi.setText("Camera API");
                        }
                    });
                }
                if(mSurfaceCreated.get() && mSurfaceTexture != null){
                    mPreviewListener = mCameraHandler.init(mCameraListener,mPreviewWidth,mPreviewHeight);
                    releaseGLRes();
                    mSurfaceView.onPause();
                    mSurfaceView.onResume();
                }
                break;
        }
    }

    class NV21Processor extends FrameProcessor{
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            mYuvData = data;
            super.onPreviewFrame(data, camera);
        }
    }
}
