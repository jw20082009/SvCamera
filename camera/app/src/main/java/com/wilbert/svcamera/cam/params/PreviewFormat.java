package com.wilbert.svcamera.cam.params;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;

import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.ParamType;

/**
 * @author wilbert
 * @Date 2021/2/18 21:16
 * @email jiangwang.wilbert@bigo.sg
 **/
public class PreviewFormat extends BaseParam {
    int imageFormat;

    public PreviewFormat(){
        imageFormat = ImageFormat.NV21;
    }

    public int getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(int imageFormat) {
        this.imageFormat = imageFormat;
    }

    @Override
    void applyCamera(ICamera camera, Camera.Parameters parameters) {
        parameters.setPreviewFormat(imageFormat);
    }

    @Override
    void applyCamera2(ICamera camera,CaptureRequest.Builder captureBuilder) {
    }

    @Override
    public ParamType getType() {
        return ParamType.PreviewFormat;
    }
}
