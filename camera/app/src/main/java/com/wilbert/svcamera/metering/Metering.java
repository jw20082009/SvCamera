package com.wilbert.svcamera.metering;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jiangwang.wilbert@bigo.sg
 **/
public abstract class Metering implements IMetering {
    private final String TAG = "Metering";
    public enum ExposureStatus {
        FACE_EXIST, CENTER_METERING, MANUAL;
    }
    protected final int FACE_DIFFERENT_VAL = 20;
    protected final MeteringController mController;
    protected final ExposureStatus mState;
    protected boolean mHasFace = false;
    protected boolean mFaceChanged = false;
    protected boolean mHasManual = false;
    protected Point mManualCenter;
    protected Rect mMeterRect = new Rect(0, 0, 0, 0);
    protected Rect mFocusRect = new Rect(0, 0, 0, 0);
    protected ManualType mMeterType = ManualType.TYPE_MANUAL_SMART;

    protected Metering(MeteringController controller, ExposureStatus state) {
        mController = controller;
        mState = state;
    }

    @Override
    public Metering onFaceEvent(boolean hasFace, Rect rect) {
        if(mHasManual){
            //当前正处于手动模式
            return this;
        }
        if(hasFace != mHasFace || isRectDifferent(rect)){
            mFaceChanged = true;
            mHasFace = hasFace;
            mMeterRect = rect;
        }
        return this;
    }

    @Override
    public Metering onManualEvent(ManualType meterType,Rect meterRect, Rect focusRect, Point centerPoint) {
        if(MeteringDelegate.sDebug) {
            Log.e(TAG, "onManualEvent:" + meterType.name() + ";meterRect:" + meterRect + ";focusRect:" + focusRect + ";centerPoint:" + centerPoint);
        }
        mManualCenter = centerPoint;
        mMeterType = meterType;
        mFaceChanged = false;
        mHasManual = true;
        mMeterRect = meterRect;
        mFocusRect = focusRect;
        return this;
    }

    public Metering resetFlag(){
        mFaceChanged = false;
        mHasManual = false;
        return this;
    }

    public ExposureStatus getState() {
        return mState;
    }

    public Rect getMeterRect(){
        return mMeterRect;
    }

    public Rect getFocusRect(){
        return mFocusRect;
    }

    public ManualType getManualType(){
        return mMeterType;
    }

    protected void clear() {
    }

    private boolean isRectDifferent(Rect rect) {
        if (rect == null) {
            Log.e(TAG,"[isFaceDifferent] rect == null");
            return false;
        }
        if (mMeterRect == null) {
            return true;
        }
        if (Math.abs(mMeterRect.left - rect.left) > FACE_DIFFERENT_VAL ||
                Math.abs(mMeterRect.top - rect.top) > FACE_DIFFERENT_VAL ||
                Math.abs(mMeterRect.right - rect.right) > FACE_DIFFERENT_VAL ||
                Math.abs(mMeterRect.bottom - rect.bottom) > FACE_DIFFERENT_VAL){
            return true;
        }else{
            if(MeteringDelegate.sDebug) {
                Log.e(TAG, "[isFaceDifferent] rect:" + rect + ";mFaceRect:" + mMeterRect);
            }
        }
        return false;
    }

}