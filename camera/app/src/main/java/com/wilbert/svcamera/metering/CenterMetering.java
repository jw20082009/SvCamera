package com.wilbert.svcamera.metering;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jw20082009@qq.com
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
            if(mHasFace){
                mController.switchState(new FaceMetering(mController).onFaceEvent(mHasFace, mMeterRect).resetFlag());
            }else{
                mController.switchState(new CenterMetering(mController));
            }
            mFaceChanged = false;
            return;
        }
        mController.switchState(new DefaultMetering(mController).onFaceEvent(mHasFace,mMeterRect).resetFlag());
    }
}
