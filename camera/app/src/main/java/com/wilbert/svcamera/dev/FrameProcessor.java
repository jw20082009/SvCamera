package com.wilbert.svcamera.dev;

import android.hardware.Camera;

/**
 * @author wilbert
 * @Date 2021/1/5 16:51
 * @email jw20082009@qq.com
 **/
public class FrameProcessor implements Camera.PreviewCallback{

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
    }
}
