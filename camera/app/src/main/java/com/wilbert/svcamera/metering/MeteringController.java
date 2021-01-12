package com.wilbert.svcamera.metering;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jiangwang.wilbert@bigo.sg
 **/
public class MeteringController implements IMetering {

    private static final String TAG = "MeteringController";

    private MeteringCallback mMeteringCallback;
    private Metering mMetering;

    public MeteringController(MeteringCallback meteringCallback) {
        mMetering = new CenterMetering(this);
        if (meteringCallback != null) {
            mMeteringCallback = meteringCallback;
        }
    }

    @Override
    public Metering onFaceEvent(boolean hasFace, Rect rect) {
        return mMetering.onFaceEvent(hasFace, rect);
    }

    @Override
    public Metering onManualEvent(ManualType meterType, Rect meterRect, Rect focusRect, Point manualCenter) {
        return mMetering.onManualEvent(meterType, meterRect, focusRect, manualCenter);
    }

    @Override
    public void onFrameAvailable(byte[] yuvData, int width, int height) {
        mMetering.onFrameAvailable(yuvData, width, height);
    }

    void switchState(Metering metering) {
        mMeteringCallback.onMeteringChanged(mMetering, metering);
        mMetering = metering;
    }

    void showMessage(String msg) {
        mMeteringCallback.onMessage(msg);
    }
}
