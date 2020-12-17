package com.wilbert.svcamera.dev;

import com.wilbert.svcamera.IPreviewListener;

/**
 * @author wilbert
 * @Date 2020/12/15 10:04
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface ICameraHandler {

    boolean isCameraOpened();
    IPreviewListener init(ICameraListener listener, int previewWidth, int previewHeight);
    void release();
    void switchStabilization();
    void switchCamera();
    void zoom(int progress);
}
