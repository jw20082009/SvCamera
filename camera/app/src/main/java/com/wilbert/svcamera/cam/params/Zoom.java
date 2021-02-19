package com.wilbert.svcamera.cam.params;

import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;

import com.wilbert.svcamera.cam.abs.CameraType;
import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.ParamType;
import com.wilbert.svcamera.cam.camImpl.CameraImpl;

/**
 * @author wilbert
 * @Date 2021/2/19 11:23
 * @email jiangwang.wilbert@bigo.sg
 **/
public class Zoom extends BaseParam{

    int zoom;
    int maxZoom;

    public void setZoom(int zoom){
        this.zoom = zoom;
        reset();
    }

    public int getZoom(){
        return zoom;
    }

    public int getMaxZoom(ICamera camera){
        if(camera == null){
            return -1;
        }
        int result = -1;
        CameraType type = camera.getCameraType();
        switch (type){
            case Camera:{
                Camera.Parameters parameters = ((CameraImpl)camera).getParameters();
                result = parameters.getMaxZoom();
            }
            break;
            case Camera2:
                break;
        }
        return result;
    }

    @Override
    void applyCamera(ICamera camera,Camera.Parameters parameters) {
        maxZoom = parameters.getMaxZoom();
        zoom = zoom > maxZoom ?maxZoom:zoom;
        zoom = zoom < 0?0:zoom;
        parameters.setZoom(zoom);
    }

    @Override
    void applyCamera2(ICamera camera,CaptureRequest.Builder captureBuilder) {
    }

    @Override
    public ParamType getType() {
        return ParamType.Zoom;
    }

}
