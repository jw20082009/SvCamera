package com.wilbert.svcamera.dev;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.wilbert.svcamera.CameraActivity;
import com.wilbert.svcamera.IPreviewListener;
import com.wilbert.svcamera.metering.CameraHelper;
import com.wilbert.svcamera.metering.Metering;
import com.wilbert.svcamera.metering.MeteringCallback;
import com.wilbert.svcamera.metering.MeteringDelegate;
import com.wilbert.svcamera.metering.PointTransform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wilbert
 * @Date 2020/12/15 10:00
 * @email jiangwang.wilbert@bigo.sg
 **/
@SuppressWarnings("deprecation")
public class CameraHandler extends Handler implements ICameraHandler{
    private final String TAG = "CameraHandler";
    public static final int MSG_CAMERA_START = 0x01;

    public static final int MSG_CAMERA_PREVIEW = 0x02;

    public static final int MSG_SWITCH_FPS = 0X03;

    public static final int MSG_SWITCH_STABILIZATION = 0x04;

    public static final int MSG_SWITCH_CAMERA = 0x05;

    public static final int MSG_ZOOM = 0x06;

    public static final int MSG_RELEASE = 0X07;

    public static final int MSG_REQUEST_FOCUS = 0x08;

    public static int currentCameraId = -1;
    Camera camera;
    Camera.CameraInfo info;
    Camera.Parameters parameters;
    CameraIndexer cameraIndexer;
    AtomicBoolean mCameraOpened = new AtomicBoolean(false);
    boolean mCameraPreviewing = false;
    int mDefaultZoom = 1;
    int mMaxZoom = 1;
    int mPreviewWidth = 640;
    int mPreviewHeight = 480;
    int mViewWidth = 0;
    int mViewHeight = 0;
    int mFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    int mCurrentFpsIndex;
    int mOrientation = 0;
    int mCameraOrientation = 0;
    List<int[]> mFpsRange;
    ICameraListener mListener;
    Object mLock = new Object();
    Context mContext;
    SurfaceTexture mSurfaceTexture;

    private MeteringDelegate mMeteringDelegate = new MeteringDelegate(new MeteringCallback() {

        private int mCurrentSwitchTimes = 0;

        @Override
        public void onMeteringChanged(Metering oldMetering, Metering newMetering) {
            if(camera != null && parameters != null && newMetering != null){
                Rect meterRect = newMetering.getMeterRect();
                Rect focusRect = newMetering.getFocusRect();
                if(mListener != null){
                    mListener.onMeteringSwitch(CameraHelper.revertMeterRect(meterRect,mCameraOrientation,mFacing == Camera.CameraInfo.CAMERA_FACING_FRONT,mViewWidth,mViewHeight,mPreviewWidth,mPreviewHeight));
                }
                Metering.ExposureStatus status = newMetering.getState();
                Log.e(TAG,"onMeteringChanged:"+oldMetering.getState().name()+";"+newMetering.getState().name()+";"+meterRect);
                try{
                    boolean meterResult = false;
                    boolean focusResult = false;
                    switch (status) {
                        case DEFAULT:{
                            parameters.setMeteringAreas(null);
                            camera.setParameters(parameters);
                        }
                        break;
                        case MANUAL: {
                            meterResult = CameraHelper.setMetering(parameters, meterRect, 1000);
//                            focusResult = CameraHelper.setFocus(parameters, focusRect, 1000);
                            camera.setParameters(parameters);
//                            CameraHelper.autoFocus(camera);
                        }
                        break;
                        case FACE_EXIST: {
                            meterResult = CameraHelper.setMetering(parameters, meterRect, 1000);
                            camera.setParameters(parameters);
                        }
                        break;
                        case CENTER_METERING:
                        default: {
                            meterResult = CameraHelper.setMetering(parameters, meterRect, 0);
//                            focusResult = CameraHelper.setFocus(parameters, focusRect, 0);
                            camera.setParameters(parameters);
                        }
                        break;
                    }
                    reportSwitchEvent(oldMetering,newMetering,getCameraStatResult(meterResult,focusResult));
                }catch (Exception e){
                    Log.e(TAG,"changeState:"+status.name()+";rect:"+meterRect+";"+focusRect);
                    if(MeteringDelegate.sDebug){
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        @Override
        public void onMessage(String msg) {
            Log.e(TAG,"onMessage:"+msg);
        }

        private int getCameraStatResult(boolean meterResult,boolean focusResult){
            int camResult = 0;
            camResult = meterResult ? camResult + 1 : camResult;
            camResult = focusResult ? camResult + 1 : camResult;
            return camResult;
        }

        private void reportSwitchEvent(Metering oldMetering,Metering metering,int meterResult) {
            mCurrentSwitchTimes++;
            if (metering == null || oldMetering == null || oldMetering.getState() != metering.getState() || metering.getState() == Metering.ExposureStatus.MANUAL) {
                //手动测光或者测光模式发生改变时上报
                int oldM = oldMetering == null ? Metering.ExposureStatus.CENTER_METERING.ordinal() : oldMetering.getState().ordinal();
                int newM = metering == null ? Metering.ExposureStatus.CENTER_METERING.ordinal() : metering.getState().ordinal();
                int manual = oldMetering != null ? oldMetering.getManualType().ordinal() :
                        (metering != null ? metering.getManualType().ordinal() : Metering.ExposureStatus.CENTER_METERING.ordinal());
                Log.e(TAG,"[reportSwitchEvent] oldM:"+oldM+";newM:"+newM+";manual:"+manual+";switchTimes:"+mCurrentSwitchTimes+";meterResult:"+meterResult);
                mCurrentSwitchTimes = 0;
            }
        }
    });

    BufferedOutputStream bos = null;
    int i = 0;
    FrameProcessor mFrameProcessor = new FrameProcessor(){
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(bos == null){
                try {
                    String file = CameraActivity.mContext.getFilesDir().getAbsolutePath()+"/test.yuv";
                   bos = new BufferedOutputStream(new FileOutputStream(new File(file)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if(i == 20){
                try {
                    bos.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            i++;
            mMeteringDelegate.onFrameAvailable(data, mPreviewWidth ,mPreviewHeight);
            super.onPreviewFrame(data, camera);
        }
    };

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
            case MSG_REQUEST_FOCUS:{
                Bundle data = msg.getData();
                int touchX = data.getInt("touchX");
                int touchY = data.getInt("touchY");
                int viewWidth = data.getInt("viewWidth");
                int viewHeight = data.getInt("viewHeight");
                if(camera == null || parameters == null){
                    return;
                }
                int orientation = mCameraOrientation;
                int captureWidth = mPreviewWidth;
                int captureHeight = mPreviewHeight;
                mViewWidth = viewWidth;
                mViewHeight = viewHeight;
                float areaMultiple = CameraHelper.DEFAULT_AREA_MULTIPLE;
                PointF centerPoint = CameraHelper.transFormPoint(new PointF(touchX, touchY), orientation,mFacing == Camera.CameraInfo.CAMERA_FACING_FRONT,viewWidth, viewHeight, captureWidth, captureHeight);
                Rect meterRect = CameraHelper.calcMeterRect(centerPoint, captureWidth,captureHeight,areaMultiple);
                Rect focusRect = meterRect;
                boolean manualFocus = mMeteringDelegate.onManualFocus(meterRect, focusRect, new Point((int)centerPoint.x,(int)centerPoint.y));
                Log.e(TAG, "requestFocusMetering:" + manualFocus + ",touchX:" + touchX + ";touchY:" +
                        touchY + ";viewWidth:" + viewWidth + ";viewHeight:" + viewHeight + ";areaMultiple:" + areaMultiple);

//                PointTransform pointTransform = new PointTransform();
//                int orientation = mOrientation;
//                int captureWidth = mPreviewWidth;
//                int captureHeight = mPreviewHeight;
//                if (viewWidth > 0 && viewHeight > 0) {
//                    CameraHelper.getPointTransform(pointTransform, orientation, captureWidth, captureHeight, mFacing == Camera.CameraInfo.CAMERA_FACING_FRONT,
//                            viewWidth, viewHeight);
//                }
//                float areaMultiple = 1.5f;
//                Rect meterRect = CameraHelper.calculateTapArea(touchX, touchY, viewWidth, viewHeight, 1.5f, pointTransform);
//                Rect focusRect = CameraHelper.calculateTapArea(touchX,touchY,viewWidth,viewHeight,1.0f,pointTransform);
//                Point centerPoint = CameraHelper.transFormPoint(new PointF(touchX,touchY),orientation,viewWidth,viewHeight,captureWidth,captureHeight);
//                boolean manualFocus = mMeteringDelegate.onManualFocus(meterRect,focusRect,centerPoint);
//                Log.e(TAG, "requestFocusMetering:" + manualFocus + ",touchX:" + touchX + ";touchY:" +
//                        touchY + ";viewWidth:" + viewWidth + ";viewHeight:" + viewHeight + ";areaMultiple:" + areaMultiple);
            }
                break;
            case MSG_RELEASE:
            {
                if(camera != null){
                    camera.stopPreview();
//                    try {
//                        camera.setPreviewTexture(null);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    camera.release();
                    camera = null;
                    parameters = null;
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
            case MSG_SWITCH_FPS:{
                if(!mCameraOpened.get() || mFpsRange == null){
                    return;
                }
                int[] fpsRange = mFpsRange.get(mCurrentFpsIndex>=mFpsRange.size()?mCurrentFpsIndex%mFpsRange.size():mCurrentFpsIndex);
                parameters.setPreviewFpsRange(fpsRange[0],fpsRange[1]);
                camera.setParameters(parameters);
                mCurrentFpsIndex=++mCurrentFpsIndex%mFpsRange.size();
                if(mListener != null){
                    mListener.onSwitchFps(fpsRange);
                }
            }
            break;
            case MSG_CAMERA_START:
                if(mCameraOpened.get())
                    return;
                if(camera != null){
                    camera.stopPreview();
//                    camera.setPreviewCallback(null);
                    camera.release();
                    camera = null;
                    parameters = null;
                    mCameraOpened.set(false);
                    synchronized (mLock) {
                        mCameraPreviewing = false;
                    }
                }
                info = new Camera.CameraInfo();
                if(cameraIndexer == null){
                    cameraIndexer = new CameraIndexer();
                    cameraIndexer.init();
                    cameraIndexer.initCamera2(mContext);
                }
                int cameraId = 1;//cameraIndexer.selectCameraId(mFacing);
                mFacing =Camera.CameraInfo.CAMERA_FACING_FRONT;// (mFacing+1)%2;
                Camera.getCameraInfo(cameraId,info);
                Camera.getNumberOfCameras();
                camera = Camera.open(cameraId);
                currentCameraId = cameraId;
                Log.i(TAG,"Camera.open:"+cameraId);
                parameters = camera.getParameters();
                mDefaultZoom = parameters.getZoom();
                mMaxZoom = parameters.getMaxZoom();
                parameters.setPreviewSize(mPreviewWidth,mPreviewHeight);
                parameters.setPreviewFormat(ImageFormat.NV21);
                mFpsRange = parameters.getSupportedPreviewFpsRange();
                camera.setParameters(parameters);
                final String str = "CameraId:"+cameraId+";"+numToString(mFpsRange);
                Log.e(TAG,"getMaxNumFocusAreas0;" +parameters.getMaxNumFocusAreas()+";"+parameters.getMaxNumMeteringAreas());
                Log.e(TAG,str);
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
//                        camera.setPreviewCallback(null);
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
                    mOrientation = calcPreviewOrientation(mContext,info);
                    Log.e(TAG,"orientation:" + mOrientation);
                    Log.e(TAG,"getMaxNumFocusAreas1;" +parameters.getMaxNumFocusAreas()+";"+parameters.getMaxNumMeteringAreas());
                    camera.setDisplayOrientation(mOrientation);
                    camera.setPreviewTexture(mSurfaceTexture);
                    Log.e(TAG,"getMaxNumFocusAreas1;" +parameters.getMaxNumFocusAreas()+";"+parameters.getMaxNumMeteringAreas());
                    if(mFrameProcessor != null){
                        byte[] frameData = new byte[mPreviewWidth*mPreviewHeight * 3/2];
                        camera.addCallbackBuffer(frameData);
                        camera.setPreviewCallbackWithBuffer(mFrameProcessor);
                    }
                    camera.startPreview();
                    mMeteringDelegate.startFaceDetect(camera);
                    Log.e(TAG,"getMaxNumFocusAreas2;" +parameters.getMaxNumFocusAreas()+";"+parameters.getMaxNumMeteringAreas());
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
    public void switchFps() {
        if(!hasMessages(CameraHandler.MSG_SWITCH_FPS)){
            sendEmptyMessage(CameraHandler.MSG_SWITCH_FPS);
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

    @Override
    public void setFrameProcessor(FrameProcessor processor) {
        mFrameProcessor = processor;
    }

    @Override
    public void requestFocus(float touchX, float touchY, int viewWidth, int viewHeight) {
        if(!hasMessages(CameraHandler.MSG_ZOOM)){
            Message msg = obtainMessage(CameraHandler.MSG_REQUEST_FOCUS);
            Bundle data = new Bundle();
            data.putInt("touchX", (int) touchX);
            data.putInt("touchY", (int) touchY);
            data.putInt("viewWidth",viewWidth);
            data.putInt("viewHeight",viewHeight);
            msg.setData(data);
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
        mCameraOrientation = info.orientation;
        int result;
        if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (mCameraOrientation + degrees)%360;
            result = (360 - result)%360;
        }else{
            result = (mCameraOrientation - degrees + 360)%360;
        }
        return result;
    }

    @Override
    public boolean isCameraOpened() {
        return mCameraOpened.get();
    }
}

