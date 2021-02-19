package com.wilbert.svcamera.cam.params;

import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;

import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.ParamType;
import com.wilbert.svcamera.cam.camImpl.CameraImpl;
import com.wilbert.svcamera.metering.CameraHelper;

/**
 * @author wilbert
 * @Date 2021/2/19 11:41
 * @email jiangwang.wilbert@bigo.sg
 **/
public class Focus extends BaseParam{
    private final String TAG = "Focus";
    float touchX;
    float touchY;
    int viewWidth;
    int viewHeight;
    Camera.Parameters parameters;
    Camera camera;

    public void requestFocus(float touchX,float touchY,int viewWidth,int viewHeight){
        this.touchX = touchX;
        this.touchY = touchY;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    public float getTouchX() {
        return touchX;
    }

    public float getTouchY() {
        return touchY;
    }

    public int getViewWidth() {
        return viewWidth;
    }

    public int getViewHeight() {
        return viewHeight;
    }

    @Override
    void applyCamera(ICamera camera, Camera.Parameters parameters) {
        this.parameters = parameters;
        int orientation = ((CameraImpl)camera).getCameraInfo().orientation;
        Camera.Size previewSize = parameters.getPreviewSize();
        int captureWidth = previewSize.width;
        int captureHeight = previewSize.height;
        float areaMultiple = CameraHelper.DEFAULT_AREA_MULTIPLE;
        PointF centerPoint = CameraHelper.transFormPoint(new PointF(touchX, touchY), orientation,camera.getCameraIndex().isFacingFront(),viewWidth, viewHeight, captureWidth, captureHeight);
        Rect meterRect = CameraHelper.calcMeterRect(centerPoint, captureWidth,captureHeight,areaMultiple);
        CameraHelper.setMetering(parameters, meterRect, 1000);
    }

    @Override
    void applyCamera2(ICamera camera,CaptureRequest.Builder captureBuilder) {

    }

    @Override
    public ParamType getType() {
        return null;
    }
}
