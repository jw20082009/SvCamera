package com.wilbert.svcamera.dev;

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

    public static Point transFormPoint(PointF point, int orientation, int viewWidth, int viewHeight, int captureWidth, int captureHeight) {
        if (point == null || viewWidth <= 0 || viewHeight <= 0 || captureWidth <= 0 || captureHeight <= 0) {
            return null;
        }
        if (Math.abs(1.0f * viewWidth / viewHeight - 1.0f * captureWidth / viewHeight) > 0.1) {
            Log.e(TAG, "[transFormPoint] ratio not the same");
            return new Point((int)point.x,(int)point.y);
        }
        int width = (orientation == 0 || orientation == 180) ? captureWidth : captureHeight;
        float ratio = 1.0f * captureWidth / viewWidth;
        Point result = new Point((int) (point.x * ratio),(int) (point.y * ratio));
        return result;
    }

    /**
     *
     */
    public static void getPointTransform(PointTransform pointTransform,
                                         int orientation, int captureWidth, int captureHeight,
                                         boolean isFront, int viewWidth, int viewHeight) {
        Matrix matrix = new Matrix();
        matrix.setRotate(orientation); // Need mirror for front camera.
        matrix.postScale(isFront ? -1 : 1, 1);

        // 根据摄像头宽高和GLSurfaceView的宽高计算放大后的大小和位置
        int height = (orientation == 0 || orientation == 180) ? captureHeight : captureWidth;
        int width = (orientation == 0 || orientation == 180) ? captureWidth : captureHeight;

        if (viewWidth > width) {  // 扩充宽度
            height = viewWidth * height / width;
            width = viewWidth;
        }

        if (viewHeight > height) { // 扩充高度
            width = viewHeight * width / height;
            height = viewHeight;
        }
        int leftMargin = (width - viewWidth) / 2;
        int topMargin = (height - viewHeight) / 2;

        int scaledPreviewWidth = width;
        int scaledPreviewHeight = height;

        matrix.postScale(scaledPreviewWidth / 2000f, scaledPreviewHeight / 2000f);
        matrix.postTranslate(scaledPreviewWidth / 2f, scaledPreviewHeight / 2f);
        matrix.invert(matrix);
        pointTransform.matrix = matrix;
        pointTransform.leftMargin = leftMargin;
        pointTransform.topMargin = topMargin;
        pointTransform.scaledPreviewWidth = scaledPreviewWidth;
        pointTransform.scaledPreviewHeight = scaledPreviewHeight;
    }

    public static Rect calculateTapArea(float x, float y,
                                        int viewWidth, int viewHeight, float areaMultiple,
                                        PointTransform pointTransform) {
        x = x + pointTransform.leftMargin;
        y = y + pointTransform.topMargin;
        int width = (int) (viewWidth * areaMultiple / 10);
        int height = (int) (viewHeight * areaMultiple / 10);

        int left = clamp((int) x - width / 2, 0, pointTransform.scaledPreviewWidth - width);
        int top = clamp((int) y - height / 2, 0, pointTransform.scaledPreviewHeight - height);

        RectF rectF = new RectF(left, top, left + width, top + height);
        pointTransform.matrix.mapRect(rectF);

        return new Rect(clamp(Math.round(rectF.left),-1000,1000),
                clamp(Math.round(rectF.top),-1000,1000),
                clamp(Math.round(rectF.right),-1000,1000),
                clamp(Math.round(rectF.bottom),-1000,1000));
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

    public static boolean setMetering(Camera.Parameters parameters, Rect rect, int weight) {
        if(parameters == null || rect == null){
            return false;
        }
        boolean result = false;
        if(parameters.getMaxNumMeteringAreas() > 0){
            List<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(rect, weight));
            parameters.setMeteringAreas(areas);
            result = true;
        }
        if (parameters.isAutoExposureLockSupported()) {
            parameters.setAutoExposureLock(false);
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
        if(parameters == null || rect == null){
            return false;
        }
        boolean result = false;
        if(parameters.getMaxNumFocusAreas() > 0){
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