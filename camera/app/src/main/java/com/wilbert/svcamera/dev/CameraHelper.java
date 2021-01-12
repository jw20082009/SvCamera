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

    // 前置摄像头人脸测光策略，获取用户手动触摸区域在最终分辨率的相对位置的矩形
    public static Rect calculateTapArea(float x, float y, int viewWidth, int viewHeight,
                                        float areaMultiple, int captureWidth, int captureHeight, int orientation, boolean isFront) {
        if (orientation == 90 || orientation == 270) {
            int tmp = captureHeight;
            captureHeight = captureWidth;
            captureWidth = tmp;
        }

        float imageScale, leftMargin = 0.0f, topMargin = 0.0f, tmp;
        // map screen size to capture size
        if (captureHeight * viewWidth > captureWidth * viewHeight) {
            imageScale = viewWidth * 1.0f / captureWidth;
            topMargin = (captureHeight - viewHeight / imageScale) / 2;
        } else {
            imageScale = viewHeight * 1.0f / captureHeight;
            leftMargin = (captureWidth - viewWidth / imageScale) / 2;
        }

        x = x / imageScale + leftMargin;
        y = y / imageScale + topMargin;

        if (isFront) {
            x = captureWidth - x;
        }

        if (orientation == 90) {
            tmp = x;
            x = y;
            y = captureHeight - tmp;
        } else if (orientation == 270) {
            tmp = x;
            x = captureWidth - y;
            y = tmp;
        }

        Rect rect = new Rect();
        rect.left = CameraHelper.clamp((int) (x - areaMultiple / 2 * captureWidth), 0, captureHeight);
        rect.right = CameraHelper.clamp((int) (x + areaMultiple / 2 * captureWidth), 0, captureHeight);
        rect.top = CameraHelper.clamp((int) (y - areaMultiple / 2 * captureWidth), 0, captureWidth);
        rect.bottom = CameraHelper.clamp((int) (y + areaMultiple / 2 * captureWidth), 0, captureWidth);

        return rect;
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

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    public static Rect calculateTapArea(float x, float y,
                                        int viewWidth, int viewHeight,
                                        Rect cropRegion, float areaMultiple, int captureWidth, int captureHeight, int orientation, boolean isFront) {
        int cropRegionWidth = cropRegion.width();
        int cropRegionHeight = cropRegion.height();
        if (orientation == 90 || orientation == 270) {
            int tmp = captureHeight;
            captureHeight = captureWidth;
            captureWidth = tmp;
        }

        float imageScale, leftMargin = 0.0f, topMargin = 0.0f, tmp;
        // map screen size to capture size
        if (captureHeight * viewWidth > captureWidth * viewHeight) {
            imageScale = viewWidth * 1.0f / captureWidth;
            topMargin = (captureHeight - viewHeight / imageScale) / 2;
        } else {
            imageScale = viewHeight * 1.0f / captureHeight;
            leftMargin = (captureWidth - viewWidth / imageScale) / 2;
        }

        x = x / imageScale + leftMargin;
        y = y / imageScale + topMargin;

        if (isFront) {
            x = captureWidth - x;
        }

        if (orientation == 90) {
            tmp = x;
            x = y;
            y = captureHeight - tmp;
        } else if (orientation == 270) {
            tmp = x;
            x = captureWidth - y;
            y = tmp;
        }

        // map capture size to SCALER_CROP_REGION
        if (captureHeight * cropRegionWidth > captureWidth * captureHeight) {
            imageScale = cropRegionHeight * 1.0f / captureHeight;
            topMargin = 0;
            leftMargin = (cropRegionWidth - imageScale * captureWidth) / 2;
        } else {
            imageScale = captureWidth * 1.0f / captureWidth;
            topMargin = (cropRegionHeight - imageScale * captureHeight) / 2;
            leftMargin = 0;
        }


        x = x * imageScale + leftMargin + cropRegion.left;
        y = y * imageScale + topMargin + cropRegion.top;

        Rect rect = new Rect();
        rect.left = clamp((int) (x - areaMultiple / 2 * cropRegionWidth), 0, cropRegionWidth);
        rect.right = clamp((int) (x + areaMultiple / 2 * cropRegionWidth), 0, cropRegionWidth);
        rect.top = clamp((int) (y - areaMultiple / 2 * cropRegionHeight), 0, cropRegionHeight);
        rect.bottom = clamp((int) (y + areaMultiple / 2 * cropRegionHeight), 0, cropRegionHeight);

        return rect;
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