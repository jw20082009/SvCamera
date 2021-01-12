package com.wilbert.svcamera.metering;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jiangwang.wilbert@bigo.sg
 **/
public interface MeteringCallback {
    void onMeteringChanged(Metering oldMetering, Metering newMetering);
    void onMessage(String msg);
}