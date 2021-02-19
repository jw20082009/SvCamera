package com.wilbert.svcamera.cam.abs;

import java.util.List;

/**
 * @author wilbert
 * @Date 2021/2/18 16:49
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface ICamera {
    void init();
    void open(ICameraIndex index);
    void close();
    void release();
    void applyParam(IParam param);
    void applyParams(List<IParam> params);
    IParam getParam(ParamType type);
    List<IParam> getParams();
    void startPreview();
    void setCallback(ICameraCallback callback);
    void setFrameCallback(IFrameCallback callback,boolean needYuv);
    ICameraIndex getCameraIndex();
    CameraType getCameraType();
    FrameWrapper getCurrentFrame();
}
