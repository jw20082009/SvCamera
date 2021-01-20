package com.wilbert.svcamera.metering;

import android.os.SystemClock;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jiangwang.wilbert@bigo.sg
 **/
public class FaceMetering extends Metering {

    private static final String TAG = "FaceMetering";
    private long mLastFaceMeteringTime = 0L;

    protected FaceMetering(MeteringController controller) {
        super(controller, ExposureStatus.FACE_EXIST);
    }

    @Override
    public void onFrameAvailable(byte[] yuvData, int width, int height) {
        if(mHasManual){
            //有手动对焦优先手动对焦
            //resetFlag避免在切换时调用两遍回调
            mController.switchState(new ManualMetering(mController).onManualEvent(mMeterType, mMeterRect,mFocusRect,mManualCenter).resetFlag());
            return;
        }
        if(mFaceChanged){
            if(mHasFace){
                mController.switchState(this);
            }else{
                mController.switchState(new CenterMetering(mController));
            }
            mFaceChanged = false;
            return;
        }
        mController.switchState(new DefaultMetering(mController).onFaceEvent(mHasFace,mMeterRect).resetFlag());
    }

}
