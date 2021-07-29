package com.wilbert.svcamera.dev;

import com.wilbert.svcamera.IPreviewListener;

/**
 * @author wilbert
 * @Date 2020/12/15 10:04
 * @email jw20082009@qq.com
 **/
public interface ICameraHandler {

    boolean isCameraOpened();
    IPreviewListener init(ICameraListener listener, int previewWidth, int previewHeight);
    void release();
    void switchFps();
    void switchStabilization();
    void switchCamera();
    void zoom(int progress);
    void setFrameProcessor(FrameProcessor processor);
    void requestFocus(final float touchX,
                             final float touchY,
                             final int viewWidth,
                             final int viewHeight);
}
