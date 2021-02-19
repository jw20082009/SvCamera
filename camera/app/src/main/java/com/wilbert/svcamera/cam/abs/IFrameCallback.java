package com.wilbert.svcamera.cam.abs;

/**
 * @author wilbert
 * @Date 2021/2/18 16:58
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface IFrameCallback {
    void onFrameAvailable(FrameWrapper frame);
}
