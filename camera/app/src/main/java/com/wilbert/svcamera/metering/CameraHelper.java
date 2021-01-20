package com.wilbert.svcamera.metering;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CameraHelper {
    private static final String TAG = "CameraHelper";
    public static float DEFAULT_AREA_MULTIPLE = 0.15f;

    public static PointF transFormPoint(PointF point, int orientation,boolean facingFront, int viewWidth, int viewHeight, int captureWidth, int captureHeight) {
        if (point == null || viewWidth <= 0 || viewHeight <= 0 || captureWidth <= 0 || captureHeight <= 0) {
            return null;
        }
        float ratioView = 1.0f * viewWidth / viewHeight;
        if (orientation == 90 || orientation == 270) {
            int temp = captureWidth;
            captureWidth = captureHeight;
            captureHeight = temp;
        }
        float ratioCapture = 1.0f * captureWidth / captureHeight;
        float ratio = 1.0f;
        if (ratioCapture > ratioView) {
            ratio = 1.0f * captureHeight / viewHeight;
            float w = viewWidth * ratio;
            float leftOffset = (captureWidth - w) / 2.0f;
            point.x = leftOffset + point.x * ratio;
            point.y = point.y * ratio;
        } else {
            ratio = 1.0f * captureWidth / viewWidth;
            float h = viewHeight * ratio;
            float topOffset = (captureHeight - h) / 2.0f;
            point.y = point.y * ratio + topOffset;
            point.x = point.x * ratio;
        }
        float points[] = new float[]{point.x, point.y};//计算过画面裁剪的point

        Matrix matrix = new Matrix();
        matrix.setScale(facingFront?-1:1,1,captureWidth/2,captureHeight/2);

        int rotation = 360 - orientation;//逆转camera旋转角度
        matrix.postRotate(rotation);

        int transX = captureWidth;//画面左上角移动到原点
        int transY = captureHeight;
        switch (rotation){
            case 90:{
                transX = captureHeight;
                transY = 0;
            }
            break;
            case 180:{
                transX = captureWidth;
                transY = captureHeight;
            }
            break;
            case 270:{
                transX = 0;
                transY = captureWidth;
            }
            break;
        }
        matrix.postTranslate(transX,transY);

        matrix.mapPoints(points);
        PointF result = new PointF(points[0], points[1]);
        return result;
    }

    public static Rect revertMeterRect(Rect rect, int orientation,boolean facingFront, int viewWidth, int viewHeight, int captureWidth, int captureHeight){
        if (rect == null || viewWidth <= 0 || viewHeight <= 0 || captureWidth <= 0 || captureHeight <= 0) {
            return null;
        }
        float left =1.0f * captureWidth * (rect.left+1000)/2000;
        float top = 1.0f * captureHeight * (rect.top + 1000)/2000;
        float right =1.0f * captureWidth* (rect.right + 1000)/2000;
        float bottom =1.0f* captureHeight *(rect.bottom + 1000)/2000;
        RectF rectF = new RectF(left,top,right,bottom);
        Matrix matrix = new Matrix();
        matrix.setScale(facingFront?-1:1,1,captureWidth/2,captureHeight/2);
        int rotation = 360 - orientation;//逆转camera旋转角度
        int transX = captureWidth;//画面左上角移动到原点
        int transY = captureHeight;
        switch (rotation){
            case 90:{
                transX = -1* captureHeight;
                transY = 0;
            }
            break;
            case 180:{
                transX = -1* captureWidth;
                transY = -1* captureHeight;
            }
            break;
            case 270:{
                transX = 0;
                transY = -1* captureWidth;
            }
            break;
        }
        matrix.postTranslate(transX,transY);
        matrix.postRotate(-1* rotation);
        matrix.mapRect(rectF);

        float ratioView = 1.0f * viewWidth / viewHeight;
        if (orientation == 90 || orientation == 270) {
            int temp = captureWidth;
            captureWidth = captureHeight;
            captureHeight = temp;
        }
        float ratioCapture = 1.0f * captureWidth / captureHeight;
        float ratio = 1.0f;
        if (ratioCapture > ratioView) {
            ratio = 1.0f * captureHeight / viewHeight;
            float w = viewWidth * ratio;
            float leftOffset = (captureWidth - w) / 2.0f;
            rectF.left = (int) (1.0f * (rectF.left - leftOffset)/ ratio);
            rectF.right = (int) (1.0f * (rectF.right - leftOffset)/ ratio);
            rectF.top = (int) (1.0f * rectF.top / ratio);
            rectF.bottom = (int) (1.0f * rectF.bottom / ratio);
        } else {
            ratio = 1.0f * captureWidth / viewWidth;
            float h = viewHeight * ratio;
            float topOffset = (captureHeight - h) / 2.0f;
            rectF.top = (int) (1.0f * (rectF.top - topOffset) / ratio);
            rectF.bottom = (int) (1.0f * (rectF.bottom - topOffset) / ratio);
            rectF.left = (int) (1.0f * rectF.left / ratio);
            rectF.right = (int) (1.0f * rectF.right / ratio);
        }
        return new Rect((int)rectF.left,(int)rectF.top,(int)rectF.right,(int)rectF.bottom);
    }

    public Point revertMeterPoint(Point point, int orientation,boolean facingFront, int viewWidth, int viewHeight, int captureWidth, int captureHeight){
        if (point == null || viewWidth <= 0 || viewHeight <= 0 || captureWidth <= 0 || captureHeight <= 0) {
            return null;
        }
        float points[] = new float[]{point.x, point.y};//计算过画面裁剪的point

        return point;
    }

    /**
     * 根据手动测光的点在camera回调画面上的对应位置计算矩形(比例未缩放到-1000~1000)
     * @param manualCapturePoint 手动测光的点在camera回调画面上的对应位置
     * @param captureWidth       camera回调画面宽度
     * @param captureHeight      camera回调画面高度
     * @param areaMultiple       测光矩形最大边占画面的比例
     * @return
     */
    public static RectF calcTapRect(PointF manualCapturePoint,int captureWidth,int captureHeight,float areaMultiple){
        float compensate = 1.5f;
        int rectWidth = captureWidth<captureHeight?(int) (captureWidth * compensate * areaMultiple):(int)(captureWidth * areaMultiple);
        int rectHeight = captureWidth<captureHeight?(int) (captureHeight * areaMultiple):(int) (captureHeight * compensate * areaMultiple);
        float halfWidth = rectWidth/2.0f;
        float halfHeight = rectHeight/2.0f;
        RectF rectF = new RectF(clamp(manualCapturePoint.x - halfWidth,0,captureWidth),
                (int)clamp(manualCapturePoint.y- halfHeight,0,captureHeight),
                (int)clamp(manualCapturePoint.x + halfWidth,0,captureWidth),
                (int)clamp(manualCapturePoint.y +halfHeight,0,captureHeight));
        return rectF;
    }

    /**
     * 根据手动测光的点在camera回调画面上的对应位置计算矩形
     * @param manualCapturePoint    手动测光的点在camera回调画面上的对应位置
     * @param captureWidth          camera回调画面宽度
     * @param captureHeight         camera回调画面高度
     * @param areaMultiple          测光矩形最大边占画面的比例
     * @return
     */
    public static Rect calcMeterRect(PointF manualCapturePoint,int captureWidth,int captureHeight,float areaMultiple){
        if(manualCapturePoint == null || captureWidth<= 0 || captureHeight <= 0 || areaMultiple<= 0){
            return null;
        }
        RectF captureRect = calcTapRect(manualCapturePoint,captureWidth,captureHeight,areaMultiple);
        int left = (int) (2000 * captureRect.left/captureWidth - 1000);
        int top = (int) (2000 * captureRect.top / captureHeight - 1000);
        int right = (int) (2000 * captureRect.right / captureWidth - 1000);
        int bottom = (int) (2000 * captureRect.bottom / captureHeight - 1000);
        Rect meterRect = new Rect(left,top,right,bottom);
        Log.e(TAG,"calcMeterRect:"+meterRect);
        return meterRect;
    }

    /**
     *
     */
    public static int clamp(int value, int min, int max) {
        if (value > max) {
            return max;
        }
        if (value < min) {
            return min;
        }
        return value;
    }

    /**
     *
     */
    public static float clamp(float value, float min, float max) {
        if (value > max) {
            return max;
        }
        if (value < min) {
            return min;
        }
        return value;
    }

    public static boolean setMetering(Camera.Parameters parameters, Rect rect, int weight) {
        if (parameters == null || rect == null) {
            return false;
        }
        boolean result = false;
        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(rect, weight));
            parameters.setMeteringAreas(areas);
            result = true;
        }
        if (parameters.isAutoExposureLockSupported()) {
            parameters.setAutoExposureLock(false);
        }
        if (parameters.isAutoWhiteBalanceLockSupported()) {
            parameters.setAutoWhiteBalanceLock(false);
        }
        return result;
    }

    public static boolean setFocus(Camera.Parameters parameters, Rect rect, int weight) {
        if (parameters == null || rect == null) {
            return false;
        }
        boolean result = false;
        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            focusAreas.add(new Camera.Area(rect, weight));
            parameters.setFocusAreas(focusAreas);
            result = true;
        }
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        return result;
    }

    public static void autoFocus(Camera camera) {
        if (camera == null) {
            return;
        }
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Log.i(TAG, "[onAutoFocus] " + success + " done !");
                try {
                    camera.cancelAutoFocus();
                    Camera.Parameters parameters = camera.getParameters();
                    List<String> focusModes = parameters.getSupportedFocusModes();
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        camera.setParameters(parameters);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}