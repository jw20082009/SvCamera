package com.wilbert.svcamera.metering;

/**
 * @author wilbert
 * @Date 2021/1/14 22:00
 * @email jw20082009@qq.com
 **/
public class DefaultMetering  extends Metering{

    protected DefaultMetering(MeteringController controller) {
        super(controller, ExposureStatus.DEFAULT);
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
            if(mHasFace){
                mController.switchState(new FaceMetering(mController).onFaceEvent(mHasFace,mMeterRect).resetFlag());
            }else{
                mController.switchState(new CenterMetering(mController));
            }
            mFaceChanged = false;
            return;
        }
    }
}
