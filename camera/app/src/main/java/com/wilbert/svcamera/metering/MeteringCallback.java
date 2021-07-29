package com.wilbert.svcamera.metering;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jw20082009@qq.com
 **/
public interface MeteringCallback {
    void onMeteringChanged(Metering oldMetering, Metering newMetering);
    void onMessage(String msg);
}