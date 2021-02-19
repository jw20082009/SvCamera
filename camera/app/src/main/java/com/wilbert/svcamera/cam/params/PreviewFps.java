package com.wilbert.svcamera.cam.params;

import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;

import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.ParamType;

import java.util.List;

/**
 * @author wilbert
 * @Date 2021/2/18 21:39
 * @email jiangwang.wilbert@bigo.sg
 **/
public class PreviewFps extends BaseParam{

    int[] currentFps;
    List<int[]> fpsRanges;

    public int[] getCurrentFps() {
        return currentFps;
    }

    public void setCurrentFps(int[] currentFps) {
        this.currentFps = currentFps;
    }

    public List<int[]> getFpsRanges() {
        return fpsRanges;
    }

    public void setFpsRanges(List<int[]> fpsRanges) {
        this.fpsRanges = fpsRanges;
    }

    @Override
    void applyCamera(ICamera camera, Camera.Parameters parameters) {
        if(currentFps != null) {
            parameters.setPreviewFpsRange(currentFps[0], currentFps[1]);
        }
    }

    @Override
    void applyCamera2(ICamera camera,CaptureRequest.Builder captureBuilder) {

    }

    @Override
    public ParamType getType() {
        return ParamType.PreviewFps;
    }
}
