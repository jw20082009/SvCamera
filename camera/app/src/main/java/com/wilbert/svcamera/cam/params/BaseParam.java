package com.wilbert.svcamera.cam.params;

import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;

import com.wilbert.svcamera.cam.abs.CameraType;
import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.IParam;
import com.wilbert.svcamera.cam.camImpl.CameraImpl;

/**
 * @author wilbert
 * @Date 2021/2/18 21:17
 * @email jiangwang.wilbert@bigo.sg
 **/
public abstract class BaseParam implements IParam {

    boolean mApplied = false;

    @Override
    public void apply(ICamera camera) {
        if(camera == null){
            return;
        }
        CameraType type = camera.getCameraType();
        switch (type){
            case Camera:{
                Camera.Parameters parameters = ((CameraImpl)camera).getParameters();
                applyCamera(camera,parameters);
                mApplied = true;
            }
            break;
            case Camera2:
                break;
        }
    }

    @Override
    public void reset() {
        mApplied = false;
    }

    @Override
    public boolean isApplied() {
        return mApplied;
    }

    abstract void applyCamera(ICamera camera,Camera.Parameters parameters);

    abstract void applyCamera2(ICamera camera,CaptureRequest.Builder captureBuilder);
}
