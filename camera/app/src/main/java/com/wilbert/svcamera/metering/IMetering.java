package com.wilbert.svcamera.metering;

import android.graphics.Point;
import android.graphics.Rect;

public interface IMetering{
    IMetering onFaceEvent(boolean hasFace, Rect rect);

    IMetering onManualEvent(ManualType meterType, Rect meterRect, Rect focusRect, Point centerPoint);

    void onFrameAvailable(byte[] yuvData, int width, int height);
}