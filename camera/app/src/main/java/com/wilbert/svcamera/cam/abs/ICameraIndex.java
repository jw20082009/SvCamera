package com.wilbert.svcamera.cam.abs;

import android.content.Context;

/**
 * @author wilbert
 * @Date 2021/2/18 16:50
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface ICameraIndex {
    int switchCamera();
    int getCameraIndex();
    boolean isFacingFront();
}
