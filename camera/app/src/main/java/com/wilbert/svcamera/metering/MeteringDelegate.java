package com.wilbert.svcamera.metering;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认为中心测光
 * 当点击手动测光时进入手动测光模式
 * 当不处于手动测光状态画面中出现人脸时切换到人脸测光
 * 当为智能模式时，会检测手动测光区域像素是否发生变化，如果发生较大变化则取消手动测光，转换到中心/人脸测光
 * 当为非智能模式时，会锁定在手动测光区域
 *
 * @author wilbert
 * @Date 2020/12/22 21:18
 * @email jiangwang.wilbert@bigo.sg
 **/
public class MeteringDelegate {
    public static boolean sDebug = true;
    private final String TAG = "FaceDetect";
    private MeteringController mMeteringController;
    private MeteringCallback mMeteringCallback;
    private AtomicBoolean mHasFace = new AtomicBoolean(false);

    public MeteringDelegate(MeteringCallback callback){
        this.mMeteringCallback = callback;
    }

    /**
     * 0: 不使用任何测光对焦策略
     * 1: 手动测光后智能判断当前是否可回到人脸以及中心模式
     * 2: 手动测光后锁定测光位置
     * @return
     */
    private int getMeteringConfig(){
        return 2;
    }

    public boolean canFaceMetering(){
        return getMeteringConfig() > 0;
    }

    public ManualType getMeterType(){
        return getMeteringConfig() ==2 ? ManualType.TYPE_MANUAL_LOCK:ManualType.TYPE_MANUAL_SMART;
    }

    public boolean startFaceDetect(Camera camera){
        boolean result = false;
        if(canFaceMetering()){
            try {
                if (camera.getParameters().getMaxNumDetectedFaces() > 0)
                    camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                        @Override
                        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                            boolean hasFace = faces != null && faces.length > 0;
                            mHasFace.set(hasFace);
                            Rect rect = new Rect(0,0,0,0);
                            if(MeteringDelegate.sDebug) {
                                Log.e(TAG, "mHasFace:" + hasFace + ";" + (hasFace ? faces[0].rect : ""));
                            }
                            if (hasFace) {
                                Rect faceRect = faces[0].rect;
                                rect.set(CameraHelper.clamp(faceRect.left,-1000,1000),
                                        CameraHelper.clamp(faceRect.top,-1000,1000),
                                        CameraHelper.clamp(faceRect.right,-1000,1000),
                                        CameraHelper.clamp(faceRect.bottom,-1000,1000));
                            }
                            getMeteringController().onFaceEvent(hasFace,rect);
                        }
                    });
                camera.startFaceDetection();
            } catch (Exception e) {
                Log.w(TAG, "[onCameraStartPreview] face detection failed", e);
            }
            result = true;
        }
        return result;
    }

    public boolean onFrameAvailable(byte[] yuv420,int width,int height){
        boolean result = false;
        if (canFaceMetering()) {
            getMeteringController().onFrameAvailable(yuv420, width, height);
            result = true;
        }
        return result;
    }

    public boolean onManualFocus(Rect meteringRect,Rect focusRect, Point manualCenter){
        boolean result = false;
        if (canFaceMetering()) {
            getMeteringController().onManualEvent(getMeterType(),meteringRect,focusRect,manualCenter);
            result = true;
        }
        return result;
    }

    private MeteringController getMeteringController(){
        if(mMeteringController == null){
            mMeteringController = new MeteringController(mMeteringCallback);
        }
        return mMeteringController;
    }


}