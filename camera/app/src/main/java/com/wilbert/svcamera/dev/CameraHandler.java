package com.wilbert.svcamera.dev;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.wilbert.svcamera.CameraFragment;
import com.wilbert.svcamera.IPreviewListener;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wilbert
 * @Date 2020/12/15 10:00
 * @email jiangwang.wilbert@bigo.sg
 **/
public class CameraHandler extends Handler implements ICameraHandler{
    private final String TAG = "CameraHandler";
    public static final int MSG_CAMERA_START = 0x01;

    public static final int MSG_CAMERA_PREVIEW = 0x02;

    public static final int MSG_DOWN_FPS = 0X03;

    public static final int MSG_SWITCH_STABILIZATION = 0x04;

    public static final int MSG_SWITCH_CAMERA = 0x05;

    public static final int MSG_ZOOM = 0x06;

    public static final int MSG_RELEASE = 0X07;
    public static int currentCameraId = -1;
    Camera camera;
    Camera.CameraInfo info;
    Camera.Parameters parameters;
    CameraIndexer cameraIndexer;
    AtomicBoolean mCameraOpened = new AtomicBoolean(false);
    boolean mCameraPreviewing = false;
    int mDefaultZoom = 1;
    int mMaxZoom = 1;
    int mPreviewWidth = 1280;
    int mPreviewHeight = 720;
    int mFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    int[] targetFpsRange;
    List<int[]> mFpsRange;
    ICameraListener mListener;
    Object mLock = new Object();
    Context mContext;
    SurfaceTexture mSurfaceTexture;

    public CameraHandler(Context context,Looper looper){
        super(looper);
        mContext = context;
    }

    @Override
    public IPreviewListener init(ICameraListener listener,int previewWidth, int previewHeight){
        synchronized (mLock) {
            if(_isInit()){
               return null;
            }
            mListener = listener;
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            sendEmptyMessage(MSG_CAMERA_START);
            Log.e(TAG,"MSG_CAMERA_PREVIEW 2");
        }
        return new IPreviewListener() {
            @Override
            public void onSurfaceCreated(SurfaceTexture surfaceTexture) {
                synchronized (mLock){
                    mSurfaceTexture = surfaceTexture;
                    mLock.notify();
                    Log.e(TAG,"MSG_CAMERA_PREVIEW 1");
                    if(!mCameraPreviewing && mCameraOpened.get()){
                        sendEmptyMessage(MSG_CAMERA_PREVIEW);
                    }
                }
            }

            @Override
            public void onSurfaceDestroy() {
                synchronized (mLock){
                    mSurfaceTexture = null;
                    mCameraPreviewing = false;
                }
            }
        };
    }

    private boolean _isInit(){
        return mContext != null && mSurfaceTexture!= null;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        switch (msg.what){
            case MSG_RELEASE:
            {
                if(camera != null){
                    try {
                        camera.setPreviewTexture(null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    camera.release();
                    camera = null;
                }
                mCameraOpened.set(false);
                getLooper().quit();
            }
                break;
            case MSG_ZOOM:{
                int progress = msg.arg1;
                int totalZoom = mMaxZoom - mDefaultZoom;
                int currentZoom = (int) (mDefaultZoom + totalZoom *1.0f * progress/100);
                parameters.setZoom(currentZoom);
                camera.setParameters(parameters);
            }
            break;
            case MSG_SWITCH_CAMERA:{
                mCameraOpened.set(false);
                sendEmptyMessage(MSG_CAMERA_START);
                Log.e(TAG,"MSG_CAMERA_PREVIEW 0");
            }
            break;
            case MSG_SWITCH_STABILIZATION:
            {
                boolean isStabilizationOpen = parameters.getVideoStabilization();
                if (parameters.isVideoStabilizationSupported()) {
                    Log.i(TAG,"stabilization:"+parameters.getVideoStabilization());
                    if (isStabilizationOpen) {
                        parameters.setVideoStabilization(false);
                        mListener.onSwitchStabilization(0);
                    } else {
                        parameters.setVideoStabilization(true);
                        mListener.onSwitchStabilization(1);
                    }
                    camera.setParameters(parameters);
                }else{
                    mListener.onSwitchStabilization(-1);
                }
            }
            break;
            case MSG_DOWN_FPS:{
                if(!mCameraOpened.get()){
                    return;
                }
//                    int[] ranges = (int[]) msg.obj;

//                    parameters.setPreviewFpsRange(ranges[0],ranges[1]);
                camera.setParameters(parameters);
//                    final int[] fpsRange = new int[2];
//                    parameters.getPreviewFpsRange(fpsRange);
//                    mTvDown.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            mTvDown.setText(fpsRange[0]+"*"+fpsRange[1]);
//                        }
//                    });
//                    Log.e(TAG,"fpsRange[0]:"+fpsRange[0]+";fpsRange[1]:"+fpsRange[1]);
            }
            break;
            case MSG_CAMERA_START:
                if(mCameraOpened.get())
                    return;
                if(camera != null){
                    camera.stopPreview();
                    camera.setPreviewCallback(null);
                    camera.release();
                    mCameraOpened.set(false);
                    synchronized (mLock) {
                        mCameraPreviewing = false;
                    }
                }
                info = new Camera.CameraInfo();
                if(cameraIndexer == null){
                    cameraIndexer = new CameraIndexer();
                    cameraIndexer.init();
                }
                int cameraId = cameraIndexer.selectCameraId(mFacing);
                mFacing = (mFacing+1)%2;
                Camera.getCameraInfo(cameraId,info);
                Camera.getNumberOfCameras();
                camera = Camera.open(cameraId);
                currentCameraId = cameraId;
                Log.i(TAG,"Camera.open:"+cameraId);
                parameters = camera.getParameters();
                mDefaultZoom = parameters.getZoom();
                mMaxZoom = parameters.getMaxZoom();
                parameters.setPreviewSize(mPreviewWidth,mPreviewHeight);
                mFpsRange = parameters.getSupportedPreviewFpsRange();
                final String str = "CameraId:"+cameraId+";"+numToString(mFpsRange);
                Log.e(TAG,str);
                camera.setParameters(parameters);
                mListener.onCameraOpened(str);
                mCameraOpened.set(true);
                if(!hasMessages(MSG_CAMERA_PREVIEW)){
                    sendEmptyMessage(MSG_CAMERA_PREVIEW);
                }
                break;
            case MSG_CAMERA_PREVIEW:{
                synchronized (mLock) {
                    if (mCameraPreviewing) {
                        return;
                    }
                    mCameraPreviewing = true;
                    if(camera == null){
                        break;
                    }else{
                        camera.stopPreview();
                        camera.setPreviewCallback(null);
                    }
                    if(!_isInit()){
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    cameraIndexer.initCamera2(mContext);
                    int orientation = calcPreviewOrientation(mContext,info);
                    Log.e(TAG,"orientation:" + orientation);
                    camera.setDisplayOrientation(orientation);
                    camera.setPreviewTexture(mSurfaceTexture);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;
        }
    }

    public static String numToString(List<int[]> nums){
        StringBuilder resultBuilder = new StringBuilder();
        for(int[] num:nums){
            resultBuilder.append("[");
            for(int i:num){
                resultBuilder.append(i);
                resultBuilder.append(",");
            }
            resultBuilder.append("],");
        }
        return resultBuilder.toString();
    }

    @Override
    public void release(){
        if(!hasMessages(CameraHandler.MSG_RELEASE)){
            sendEmptyMessage(CameraHandler.MSG_RELEASE);
        }
    }

    @Override
    public void switchStabilization() {
        if(!hasMessages(CameraHandler.MSG_SWITCH_STABILIZATION)){
            sendEmptyMessage(CameraHandler.MSG_SWITCH_STABILIZATION);
        }
    }

    @Override
    public void switchCamera() {
        if(!hasMessages(CameraHandler.MSG_SWITCH_CAMERA)){
            sendEmptyMessage(CameraHandler.MSG_SWITCH_CAMERA);
        }
    }

    @Override
    public void zoom(int progress) {
        if(!hasMessages(CameraHandler.MSG_ZOOM)){
            Message msg = obtainMessage(CameraHandler.MSG_ZOOM);
            msg.arg1 = progress;
            msg.sendToTarget();
        }
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

    @Override
    public boolean isCameraOpened() {
        return mCameraOpened.get();
    }
}
