package com.wilbert.svcamera.metering;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jiangwang.wilbert@bigo.sg
 **/
public class CenterMetering extends Metering {

    private static final String TAG = "CenterMetering";

    protected CenterMetering(MeteringController controller) {
        super(controller, ExposureStatus.CENTER_METERING);
    }

    @Override
    public void onFrameAvailable(byte[] yuvData, int width, int height) {
        if(mHasManual){
            //resetFlag避免在切换时调用两遍回调
            mController.switchState(new ManualMetering(mController).onManualEvent(mMeterType, mMeterRect,mFocusRect,mManualCenter).resetFlag());
            mHasManual = false;
            return;
        }
        if(mFaceChanged){
            //resetFlag避免在切换时调用两遍回调
            mController.switchState(new FaceMetering(mController).onFaceEvent(mHasFace, mMeterRect).resetFlag());
            mFaceChanged = false;
            return;
        }
    }
}
