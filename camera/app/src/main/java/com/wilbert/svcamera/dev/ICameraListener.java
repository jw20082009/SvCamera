package com.wilbert.svcamera.dev;

/**
 * @author wilbert
 * @Date 2020/12/15 10:11
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface ICameraListener {
    void onSwitchStabilization(int status);//-1: not support, 0: off, 1: on
    void onCameraOpened(String msg);
}
