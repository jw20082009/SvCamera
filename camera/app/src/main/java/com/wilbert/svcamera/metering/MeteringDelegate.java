package com.wilbert.svcamera.metering;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;

/**
 * @author wilbert
 * @Date 2020/12/22 21:18
 * @email jiangwang.wilbert@bigo.sg
 **/
public class MeteringDelegate {
    public static boolean sDebug = false;
    private final String TAG = "MeteringDelegate";
    private MeteringController mMeteringController;
    private MeteringCallback mMeteringCallback;

    public MeteringDelegate(MeteringCallback callback){
        this.mMeteringCallback = callback;
    }

    private int getMeteringConfig(){
        return 1;
    }

    public boolean hitOnManualFocusMetering(){
        return getMeteringConfig() > 0;
    }

    public ManualType getMeterType(){
        return getMeteringConfig() ==2 ? ManualType.TYPE_MANUAL_LOCK:ManualType.TYPE_MANUAL_SMART;
    }

    public boolean startFaceDetect(Camera camera){
        boolean result = false;
        if(hitOnManualFocusMetering()){
            try {
                if (camera.getParameters().getMaxNumDetectedFaces() > 0) {
                    camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                        @Override
                        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                            boolean hasFace = faces != null && faces.length > 0;
                            Rect rect = new Rect(0, 0, 0, 0);
                            if (hasFace) {
                                RectF faceRect = new RectF(faces[0].rect);
                                rect.set(CameraHelper.clamp((int) faceRect.left, -1000, 1000),
                                        CameraHelper.clamp((int) faceRect.top, -1000, 1000),
                                        CameraHelper.clamp((int) faceRect.right, -1000, 1000),
                                        CameraHelper.clamp((int) faceRect.bottom, -1000, 1000));
                            }
                            getMeteringController().onFaceEvent(hasFace, rect);
                        }
                    });
                    camera.startFaceDetection();
                }
            } catch (Exception e) {
                Log.w(TAG, "[onCameraStartPreview] face detection failed", e);
            }
            result = true;
        }
        return result;
    }

    public boolean onFrameAvailable(byte[] yuv420,int width,int height){
        boolean result = false;
        if (hitOnManualFocusMetering()) {
            getMeteringController().onFrameAvailable(yuv420, width, height);
            result = true;
        }
        return result;
    }

    public boolean onManualFocus(Rect meteringRect,Rect focusRect, Point manualCenter){
        boolean result = false;
        if (hitOnManualFocusMetering()) {
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