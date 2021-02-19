package com.wilbert.svcamera.cam.abs;

/**
 * @author wilbert
 * @Date 2021/2/18 16:52
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface IParam {
    void apply(ICamera camera);
    void reset();
    boolean isApplied();
    ParamType getType();
}
