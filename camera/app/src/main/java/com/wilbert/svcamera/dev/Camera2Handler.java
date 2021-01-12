package com.wilbert.svcamera.dev;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.wilbert.svcamera.IPreviewListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wilbert
 * @Date 2020/12/15 20:51
 * @email jiangwang.wilbert@bigo.sg
 **/
public class Camera2Handler extends Handler implements ICameraHandler {

    private final String TAG = "CameraHandler";
    public static final int MSG_CAMERA_START = 0x01;

    public static final int MSG_CAMERA_PREVIEW = 0x02;

    public static final int MSG_DOWN_FPS = 0X03;

    public static final int MSG_SWITCH_STABILIZATION = 0x04;

    public static final int MSG_SWITCH_CAMERA = 0x05;

    public static final int MSG_ZOOM = 0x06;

    public static final int MSG_RELEASE = 0X07;

    CameraIndexer cameraIndexer;
    boolean mCameraPreviewing = false;
    int mDefaultZoom = 1;
    int mMaxZoom = 1;
    int mPreviewWidth = 1280;
    int mPreviewHeight = 720;
    int mFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;

    ICameraListener mListener;
    Object mLock = new Object();
    Semaphore mPhore = new Semaphore(0);
    Context mContext;
    SurfaceTexture mSurfaceTexture;
    AtomicInteger mOpenStatus = new AtomicInteger(0);//0: 关闭，1：开启中，2：已开启, 3: 设置preview中, 4: preview中
    CaptureRequest.Builder mPreviewBuilder;
    CameraDevice mCameraDevice;
    CameraCaptureSession mPreviewSession;
    CameraCharacteristics mCameraCharacteristics;
    List<int[]> mFpsRanges;

    public Camera2Handler(Context context, Looper looper) {
        super(looper);
        mContext = context;
    }

    @Override
    public boolean isCameraOpened() {
        return mOpenStatus.get()>=2;
    }

    @Override
    public IPreviewListener init(ICameraListener listener, int previewWidth, int previewHeight) {
        synchronized (mLock) {
            mListener = listener;
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            sendEmptyMessage(MSG_CAMERA_START);
        }
        return new IPreviewListener() {
            @Override
            public void onSurfaceCreated(SurfaceTexture surfaceTexture) {
                synchronized (mLock) {
                    mSurfaceTexture = surfaceTexture;
                    mPhore.release(1);
                    if (!mCameraPreviewing && isCameraOpened()) {
                        sendEmptyMessage(MSG_CAMERA_PREVIEW);
                    }
                }
            }

            @Override
            public void onSurfaceDestroy() {
                synchronized (mLock) {
                    mSurfaceTexture = null;
                    mCameraPreviewing = false;
                    if(isCameraOpened()){
                        mPhore = new Semaphore(1);
                    }else{
                        mPhore = new Semaphore(0);
                    }
                }
            }
        };
    }

    private boolean _isSurfaceCreated() {
        return mContext != null && mSurfaceTexture != null;
    }

    @Override
    public void release() {
        if (!hasMessages(MSG_RELEASE)) {
            sendEmptyMessage(MSG_RELEASE);
        }
    }

    @Override
    public void switchFps() {

    }

    @Override
    public void switchStabilization() {
        if (!hasMessages(MSG_SWITCH_STABILIZATION)) {
            sendEmptyMessage(MSG_SWITCH_STABILIZATION);
        }
    }

    @Override
    public void switchCamera() {
        if (!hasMessages(MSG_SWITCH_CAMERA)) {
            sendEmptyMessage(MSG_SWITCH_CAMERA);
        }
    }

    @Override
    public void zoom(int progress) {
        if (!hasMessages(MSG_ZOOM)) {
            Message msg = obtainMessage(MSG_ZOOM);
            msg.arg1 = progress;
            msg.sendToTarget();
        }
    }

    @Override
    public void setFrameProcessor(FrameProcessor processor) {

    }

    @Override
    public void requestFocus(float touchX, float touchY, int viewWidth, int viewHeight) {

    }

    private int getFacing() {
        int facing = mFacing;
        mFacing = (mFacing + 1) % 2;
        return facing;
    }

    private int getValidAFMode(int targetMode) {
        int[] allAFMode = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        for (int mode : allAFMode) {
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Log.i(TAG, "not support af mode:" + targetMode + " use mode:" + allAFMode[0]);
        return allAFMode[0];
    }

    private int getValidAntiBandingMode(int targetMode) {
        int[] allABMode = mCameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);

        for (int mode : allABMode) {
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Log.i(TAG, "not support anti banding mode:" + targetMode
                + " use mode:" + allABMode[0]);
        return allABMode[0];
    }

    private String[] mCameraIdList = null;

    @Override
    @SuppressWarnings("MissingPermission")
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case MSG_RELEASE:{
                if(mOpenStatus.get() <= 0){
                    return;
                }
                mOpenStatus.set(0);
                if(mPreviewSession != null){
                    mPreviewSession.close();
                    mPreviewSession = null;
                }
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                getLooper().quit();
            }
            break;
            case MSG_ZOOM: {

            }
            break;
            case MSG_SWITCH_CAMERA: {
                if(mOpenStatus.get() >= 2){
                    mOpenStatus.set(0);
                    if(mPreviewSession != null){
                        mPreviewSession.close();
                        mPreviewSession = null;
                    }
                    if (mCameraDevice != null) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                    }
                }
                if(!hasMessages(MSG_CAMERA_START) && !hasMessages(MSG_CAMERA_PREVIEW)){
                    sendEmptyMessage(MSG_CAMERA_START);
                }
            }
            break;
            case MSG_SWITCH_STABILIZATION: {

            }
            break;
            case MSG_DOWN_FPS: {
                if (!isCameraOpened()) {
                    return;
                }

            }
            break;
            case MSG_CAMERA_START:
                if (mOpenStatus.get()>0)
                    return;
                mOpenStatus.set(1);
                CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                try {
                    Log.e(TAG, "tryAcquire lock opening camera");
                    if (mCameraIdList == null) {
                        String[] cameraIdList = null;
                        if (manager != null) {
                            cameraIdList = manager.getCameraIdList();
                        }
                        mCameraIdList = cameraIdList;
                    }
                    if (cameraIndexer == null) {
                        cameraIndexer = new CameraIndexer();
                        cameraIndexer.init();
                        cameraIndexer.initCamera2(mContext);
                    }
                    int cameraIndex = CameraHandler.currentCameraId;
                    if(CameraHandler.currentCameraId == -1){
                        cameraIndex = cameraIndexer.selectCameraId(getFacing());
                    }else{
                        CameraHandler.currentCameraId = -1;
                    }
                    String cameraId = mCameraIdList[cameraIndex];
                    mCameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                    if (mCameraCharacteristics == null) {
                        Log.e(TAG, "getCameraCharacteristics failed");
                        return;
                    }
                    manager.openCamera(cameraId, mStateCallback, this);
                    if(mListener != null){
                        mListener.onCameraOpened("CameraId:"+cameraId);
                    }
                } catch (CameraAccessException e) {
                    Log.e(TAG, e.toString());
                }
                break;
            case MSG_CAMERA_PREVIEW:{
                int status = mOpenStatus.get();
                if(status==3){
                    return;
                }else if(status == 4){
                    if (mPreviewSession != null) {
                        try {
                            mPreviewSession.abortCaptures();
                            mPreviewSession = null;
                            mPhore.release(1);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
                boolean available = true;
                try {
                    mPhore.acquire(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(available){
                    mOpenStatus.set(3);
                    List<Surface> surfaceList = new ArrayList<>();
                    try {
                        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        mSurfaceTexture.setDefaultBufferSize(mPreviewWidth,mPreviewHeight);
                        Surface surface = new Surface(mSurfaceTexture);
                        mPreviewBuilder.addTarget(surface);
                        surfaceList.add(surface);
                        mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mPreviewSession = session;
                                mOpenStatus.set(4);
                                lock3A(true);
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        },null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
        }
    }

    public void lock3A(boolean lock) {
        if (mPreviewBuilder == null) {
            Log.e(TAG, "try lock3A when mPreviewBuilder is null");
            return;
        }
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        int antiBMode = getValidAntiBandingMode(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antiBMode);
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);

        updatePreview();
    }

    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession
            .CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    private void updatePreview() {
        if (mCameraDevice == null) {
            Log.e(TAG, "try updatePreview when mCameraDevice is null");
            return;
        }

        if (mOpenStatus.get() != 4) {
            Log.e(TAG, "try updatePreview but now it's not in preview state");
            return;
        }

        CameraCaptureSession ccs = mPreviewSession;
        try {
            if (ccs != null) {
                ccs.setRepeatingRequest(mPreviewBuilder.build(), mPreviewCallback, null);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
        } catch (IllegalStateException e) {
            Log.e(TAG, e.toString());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.toString());
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mOpenStatus.set(2);
            synchronized (mLock){
                mCameraDevice = camera;
                if(mCameraDevice == null){
                    Log.e(TAG,"onOpened null cameraDev");
                }
                mPhore.release(1);
            }
            if(!hasMessages(MSG_CAMERA_PREVIEW)){
                sendEmptyMessage(MSG_CAMERA_PREVIEW);
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
            mOpenStatus.set(0);
            synchronized (mLock){
                if(_isSurfaceCreated()){
                    mPhore = new Semaphore(1);
                }else{
                    mPhore = new Semaphore(0);
                }
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mOpenStatus.set(0);
            synchronized (mLock){
                if(_isSurfaceCreated()){
                    mPhore = new Semaphore(1);
                }else{
                    mPhore = new Semaphore(0);
                }
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mOpenStatus.set(0);
            synchronized (mLock){
                if(_isSurfaceCreated()){
                    mPhore = new Semaphore(1);
                }else{
                    mPhore = new Semaphore(0);
                }
            }
        }
    };
}
