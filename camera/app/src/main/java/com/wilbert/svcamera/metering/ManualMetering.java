package com.wilbert.svcamera.metering;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import com.wilbert.svcamera.CameraActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author wilbert
 * @Date 2020/12/22 18:33
 * @email jiangwang.wilbert@bigo.sg
 **/
public class ManualMetering extends Metering {
    private static final String TAG = "ManualMetering";
    private static final long AUTO_EXPOSURE_DUR_TIME = 2000L;
    private static final long SENCE_DETECT_INTERVAL = 1000L;
    private static final float DEFAULT_SIMILAR_RATIO = 0.8f;

    private boolean mOriginFrameSaved = false;
    private long mLastDetectTs = 0;
    private long mLastTouchTs = 0;
    private byte[] mOriginRegionData;
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
            return;
        }
        if ((SystemClock.elapsedRealtime() - mLastDetectTs) < SENCE_DETECT_INTERVAL) {
            //上次检测时间太近时跳过检测
            return;
        }
        // 触摸区域没有发生剧烈变化，继续对触摸区域测光
        if (!isRegionDrasticChanged(yuvData, width, height, mManualRect,mManualPoint)) {
            return;
        }
        if (mFaceChanged && mHasFace) {
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

    private boolean isRegionDrasticChanged(byte[] yuvData, int width, int height, Rect rect, Point manualCenter) {
        mLastDetectTs = SystemClock.elapsedRealtime();
        if (!mOriginFrameSaved && (SystemClock.elapsedRealtime() - mLastTouchTs) > AUTO_EXPOSURE_DUR_TIME) {
            Rect finalRect = new Rect(rect);
            mOriginRegionData = copyRegion(finalRect, yuvData,width,height,rect,manualCenter);
            if(mOriginRegionData != null){
                mManualRect.set(finalRect);
                mOriginFrameSaved = true;
                //保存成功，第一次是保存原始数据，后续才会执行对比
                Log.e(TAG,"isRegionDrasticChanged cost0:"+(SystemClock.elapsedRealtime() - mLastDetectTs));
            }
            return false;
        }
        if (!mOriginFrameSaved || mOriginRegionData == null) {
            return false;
        }
        float simiRate = getSimilarity(mOriginRegionData,yuvData,width,height,mManualRect);
        if(MeteringDelegate.sDebug) {
            Log.i(TAG, "isRegionDrasticChanged cost1:" + (SystemClock.elapsedRealtime() - mLastDetectTs)+";"+simiRate);
        }
        if (simiRate >= DEFAULT_SIMILAR_RATIO) {
            return false;
        } else {
            long currentTime = SystemClock.elapsedRealtime();
            long drasticDuration = currentTime - mLastDetectTs;
            long touchDuration = currentTime - mLastTouchTs;
            mController.showMessage("区域相似度低于阈值 " + DEFAULT_SIMILAR_RATIO + " 重置测光状态 当前= " + simiRate);
//            MediaReporter.reportDrasticChanged(drasticDuration,touchDuration,simiRate);
            return true;
        }
    }

    private float getSimilarity(byte[] region,byte[] full,int fullWidth,int fullHeight,Rect rect){
        if(region == null || full == null || rect == null){
            return 0f;
        }
        int similarity = 0;
        int length = region.length;
        try {
            int width = rect.width();
            int height = rect.height();
            if (length == 0 || width * height != length) {
                Log.e(TAG, "getSimilarity encounter range error");
                return 0;
            }

            for (int i = 0; i < height; i++) {
                int start = (rect.top + i) * fullWidth + rect.left;
                for (int j = 0; j < width; j++) {
                    int fullIndex = start + j;
                    int regionIndex = j + width * i;
                    if (Math.abs(region[regionIndex] - full[fullIndex]) <= 40) {
                        similarity++;
                    }
                }
            }
        }catch (Exception e){
            if(MeteringDelegate.sDebug){
                throw new RuntimeException("getSimilarity failed");
            }
        }
        return similarity / (float) length;
    }

    private byte[] copyRegion(Rect outRect,byte[] full,int yuvWidth,int yuvHeight, Rect rect, Point manualCenter) {
        if(outRect == null || full == null || yuvWidth <=0 || yuvHeight <= 0 || rect == null || manualCenter == null){
            return null;
        }
        byte[] region = null;
        try {
            RectF rectF = CameraHelper.calcTapRect(new PointF(manualCenter),yuvWidth,yuvHeight,CameraHelper.DEFAULT_AREA_MULTIPLE);
            outRect.set((int)rectF.left, (int)rectF.top, (int)rectF.right, (int)rectF.bottom);
            int width = outRect.width();
            int height = outRect.height();
            int left = outRect.left;
            int top = outRect.top;
            Log.e(TAG, "width:" + width + ";height:" + height + ";" + rect + ";" + manualCenter + ";yuvSize:" + yuvWidth + "*" + yuvHeight);
            region = new byte[width * height];
            for (int i = 0; i < height; i++) {
                int start = (top + i) * yuvWidth + left;
                System.arraycopy(full, start, region, i * width, width);
            }
        }catch (Exception e){
            region = null;
            if(MeteringDelegate.sDebug){
                throw new RuntimeException("copyRegion failed");
            }
        }
        return region;
    }
}
