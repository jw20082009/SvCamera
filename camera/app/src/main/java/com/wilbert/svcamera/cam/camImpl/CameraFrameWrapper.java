package com.wilbert.svcamera.cam.camImpl;

import android.graphics.SurfaceTexture;

import com.wilbert.svcamera.cam.abs.FrameWrapper;

/**
 * @author wilbert
 * @Date 2021/2/18 17:41
 * @email jiangwang.wilbert@bigo.sg
 **/
public class CameraFrameWrapper extends FrameWrapper {

    SurfaceTexture surfaceTexture;
    byte[] yuvFrame;

    public CameraFrameWrapper(SurfaceTexture surfaceTexture){
        this.surfaceTexture = surfaceTexture;
    }

    public byte[] getYuvFrame() {
        return yuvFrame;
    }

    public void setYuvFrame(byte[] yuvFrame) {
        this.yuvFrame = yuvFrame;
    }
}
