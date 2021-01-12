package com.wilbert.svcamera.metering;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jiangwang.wilbert@bigo.sg
 **/
public class ManualMetering extends Metering {
    private static final String TAG = "ManualMetering";
    private static final long AUTO_EXPOSURE_DUR_TIME = 2000L;
    private static final long SENCE_DETECT_INTERVAL = 1000L;
    private static final float DEFAULT_SIMILAR_RATIO = 0.5f;

    private boolean mOriginFrameSaved = false;
    private long mLastDetectTs = 0;
    private long mLastTouchTs = 0;
    private byte[] mCurRegionData, mOriginRegionData;
    private Rect mManualRect = new Rect(0,0,0,0);
    private Point mManualPoint = new Point();

    protected ManualMetering(MeteringController controller) {
        super(controller, ExposureStatus.MANUAL);
    }

    @Override
    public Metering onManualEvent(ManualType meterType, Rect meterRect, Rect focusRect, Point centerPoint) {
        mManualRect.set(meterRect);
        mManualPoint.set(centerPoint.x,centerPoint.y);
        mLastTouchTs = SystemClock.elapsedRealtime();
        mOriginFrameSaved = false;
        return super.onManualEvent(meterType, meterRect, focusRect, centerPoint);
    }

    @Override
    public void onFrameAvailable(byte[] yuvData, int width, int height) {
        if (mHasManual) {
            //执行手动测光
            mController.switchState(this);
            mHasManual = false;
        }
        if (mMeterType == ManualType.TYPE_MANUAL_LOCK) {
            //Lock模式不需要执行图像变化检测
            return;
        }
        if ((SystemClock.elapsedRealtime() - mLastTouchTs) < AUTO_EXPOSURE_DUR_TIME) {
            //等待一段时间后保存点击区域像素
            if(MeteringDelegate.sDebug) {
                Log.e(TAG, "[onFrameAvailable] mLastTouchTs:" + (SystemClock.elapsedRealtime() - mLastTouchTs));
            }
            return;
        }
        if ((SystemClock.elapsedRealtime() - mLastDetectTs) < SENCE_DETECT_INTERVAL) {
            //上次检测时间太近时跳过检测
            if(MeteringDelegate.sDebug) {
                Log.e(TAG, "[onFrameAvailable] mLastDetectTs:" + (SystemClock.elapsedRealtime() - mLastDetectTs));
            }
            return;
        }
        // 触摸区域没有发生剧烈变化，继续对触摸区域测光
        if (!isRegionDrasticChanged(yuvData, width, height, mManualRect,mManualPoint)) {
            return;
        }
        if (mFaceChanged) {
            //resetFlag避免在切换时调用两遍回调
            mController.switchState(new FaceMetering(mController).onFaceEvent(mHasFace, mMeterRect).resetFlag());
            mFaceChanged = false;
        } else {
            mController.switchState(new CenterMetering(mController));
        }
    }

    @Override
    protected void clear() {
        mOriginFrameSaved = false;
    }

    private boolean saveOriginFrameData(byte[] yuvData, int width, int height,Rect rect, Point manualCenter) {
        boolean result = false;
        if (!mOriginFrameSaved && (SystemClock.elapsedRealtime() - mLastTouchTs) > AUTO_EXPOSURE_DUR_TIME) {
            mOriginFrameSaved = true;
            int pixelLength = rect.width() * rect.height();
            if (mOriginRegionData == null || mOriginRegionData.length != pixelLength) {
                mOriginRegionData = new byte[pixelLength];
            }
            copyRegion(mOriginRegionData, yuvData, rect,manualCenter);
            result = true;
        }
        return result;
    }

    private boolean isRegionDrasticChanged(byte[] yuvData, int width, int height, Rect rect, Point manualCenter) {
        mLastDetectTs = SystemClock.elapsedRealtime();
        if (saveOriginFrameData(yuvData, width, height,rect,manualCenter)) {
            //第一次是保存原始数据，后续才会执行对比
            Log.e(TAG,"isRegionDrasticChanged cost0:"+(SystemClock.elapsedRealtime() - mLastDetectTs));
            return false;
        }
        int pixelLength = rect.width() * rect.height();
        if (!mOriginFrameSaved || mOriginRegionData == null || mOriginRegionData.length != pixelLength) {
            return false;
        }
        if (mCurRegionData == null || mCurRegionData.length != pixelLength) {
            mCurRegionData = new byte[pixelLength];
        }
        copyRegion(mCurRegionData, yuvData, rect,manualCenter);
        int similarity = 0;
        for (int i = 0; i < pixelLength; i++) {
            if (Math.abs(mOriginRegionData[i] - mCurRegionData[i]) <= 20) {
                similarity++;
            }
        }
        float simiRate = similarity / (float) pixelLength;
        if (simiRate >= DEFAULT_SIMILAR_RATIO) {
            if(MeteringDelegate.sDebug) {
                Log.i(TAG, "isRegionDrasticChanged cost1:" + (SystemClock.elapsedRealtime() - mLastDetectTs));
            }
            return false;
        } else {
            mController.showMessage("区域相似度低于阈值 " + DEFAULT_SIMILAR_RATIO + " 重置测光状态 当前= " + simiRate);
            Log.e(TAG,"isRegionDrasticChanged cost2:"+(SystemClock.elapsedRealtime() - mLastDetectTs)+";simiRate:"+simiRate);
            return true;
        }
    }

    private void copyRegion(byte[] region, byte[] full, Rect rect, Point manualCenter) {
        int width = rect.width();
        int height = rect.height();
        int top = CameraHelper.clamp(manualCenter.y - height/2,0,Integer.MAX_VALUE);
        int left = CameraHelper.clamp(manualCenter.x - width/2,0,Integer.MAX_VALUE);
        int bottom = top + height;
        int row = top;
        try {
            for (row = top; row < bottom; row++) {
                int srcStart = row * width + left;
                int destStart = (row - top) * width;
                if((srcStart+width) > full.length * 2/3 || (destStart+width) > region.length){
                    Log.e(TAG,"copyRegion out of Range");
                    break;
                }
                System.arraycopy(full, srcStart, region, destStart, width);
            }
        } catch (Exception e) {
            if (MeteringDelegate.sDebug) {
                Log.e(TAG, "copyRegion:" + region.length + ";" + full.length + ";rect:" + rect + ";row:" + row);
                throw new RuntimeException(e);
            }
        }
    }
}
