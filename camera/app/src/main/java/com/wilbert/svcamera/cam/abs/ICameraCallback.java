package com.wilbert.svcamera.cam.abs;

/**
 * @author wilbert
 * @Date 2021/2/18 16:55
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface ICameraCallback {
    void onCameraStatusChanged(CameraStatus status,ICamera camera);
}
