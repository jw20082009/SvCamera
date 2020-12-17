package com.wilbert.svcamera.dev;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.SizeF;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wilbert
 * @Date 2020/12/15 11:58
 * @email jiangwang.wilbert@bigo.sg
 **/
public class CameraIndexer {
    private final String TAG = "CameraIndexer";
    List<Integer> frontCameras = null;
    List<Integer> backCameras = null;
    int cameraNum = 0;

    List<Integer> frontCamera2 = null;
    List<Integer> backCamera2 = null;

    public void initCamera2(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            List<String> frontCameras = null;
            List<String> backCameras = null;
            List<String> externalCameras = null;
            StringBuilder sbFront = new StringBuilder("camera2 front:");
            StringBuilder sbBack = new StringBuilder("camera2 back:");
            StringBuilder sbExternal = new StringBuilder("camera2 external:");
            String[] cameraIds = manager.getCameraIdList();
            int i = 0;
            for (String cameraId : cameraIds) {
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics == null) {
                    Log.e(TAG, "getCameraCharacteristics failed");
                    return;
                }
                int facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                switch (facing) {
                    case CameraCharacteristics.LENS_FACING_FRONT: {
                        if (frontCameras == null) {
                            frontCameras = new ArrayList<>();
                        }
                        frontCameras.add(cameraId);
                        if(frontCamera2 == null){
                            frontCamera2 = new ArrayList<>();
                        }
                        frontCamera2.add(i);
                        SizeF sizeF = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                        float[] focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        float w = sizeF != null ? sizeF.getWidth() : 0;
                        float h = sizeF != null ? sizeF.getHeight() : 0;
                        float horizontalAngle = (float) (2 * Math.atan(w / (focalLengths[0] * 2)));
                        float verticalAngle = (float) (2 * Math.atan(h / (focalLengths[0] * 2)));
                        sbFront.append("\n"+cameraId);
                        sbFront.append("{[hAngle:");
                        sbFront.append(horizontalAngle);
                        sbFront.append("]");
                        sbFront.append("[vAngle:");
                        sbFront.append(verticalAngle);
                        sbFront.append("]}");
                    }
                    break;
                    case CameraCharacteristics.LENS_FACING_BACK: {
                        if(backCamera2 == null){
                            backCamera2 = new ArrayList<>();
                        }
                        backCamera2.add(i);
                        if (backCameras == null) {
                            backCameras = new ArrayList<>();
                        }
                        SizeF sizeF = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                        float[] focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        float w = sizeF != null ? sizeF.getWidth() : 0;
                        float h = sizeF != null ? sizeF.getHeight() : 0;
                        float horizontalAngle = (float) (2 * Math.atan(w / (focalLengths[0] * 2)));
                        float verticalAngle = (float) (2 * Math.atan(h / (focalLengths[0] * 2)));
                        backCameras.add(cameraId);
                        sbBack.append("\n"+cameraId);
                        sbBack.append("{[hAngle:");
                        sbBack.append(horizontalAngle);
                        sbBack.append("]");
                        sbBack.append("[vAngle:");
                        sbBack.append(verticalAngle);
                        sbBack.append("]}");
                    }
                    break;
                    default: {
                        if (externalCameras == null) {
                            externalCameras = new ArrayList<>();
                        }
                        externalCameras.add(cameraId);
                        SizeF sizeF = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                        float[] focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        float w = sizeF != null ? sizeF.getWidth() : 0;
                        float h = sizeF != null ? sizeF.getHeight() : 0;
                        float horizontalAngle = (float) (2 * Math.atan(w / (focalLengths[0] * 2)));
                        float verticalAngle = (float) (2 * Math.atan(h / (focalLengths[0] * 2)));
                        sbExternal.append("\n"+cameraId);
                        sbExternal.append("{[hAngle:");
                        sbExternal.append(horizontalAngle);
                        sbExternal.append("]");
                        sbExternal.append("[vAngle:");
                        sbExternal.append(verticalAngle);
                        sbExternal.append("]}");
                    }
                    break;
                }
                i++;
            }
            Log.e(TAG, sbFront.toString());
            Log.e(TAG, sbBack.toString());
            Log.e(TAG, sbExternal.toString());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void init() {

        cameraNum = Camera.getNumberOfCameras();
        if (cameraNum <= 0) {
            return;
        }
        for (int i = 0; i < cameraNum; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            switch (info.facing) {
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    if (frontCameras == null) {
                        frontCameras = new ArrayList<>();
                    }
                    frontCameras.add(i);
                    break;
                default:
                    if (backCameras == null) {
                        backCameras = new ArrayList<>();
                    }
                    backCameras.add(i);
                    break;
            }
        }
        StringBuilder front = new StringBuilder("front:");
        StringBuilder back = new StringBuilder("back:");
        for (Integer integer : frontCameras) {
            front.append(integer);
            front.append(",");
        }
        for (Integer integer : backCameras) {
            back.append(integer);
            back.append(",");
        }
        Log.e(TAG, "camera num:" + cameraNum);
        Log.e(TAG, front.toString());
        Log.e(TAG, back.toString());
    }

    public int selectCameraId(int facing) {
        if (cameraNum <= 0) {
            return 0;
        }
        return selectByCurrentIndex2();
    }

    int currentIndex2 = 0;
    private int selectByCurrentIndex2(){
        if (frontCamera2 == null || frontCamera2.size() <= 0 || backCamera2 == null || backCamera2.size() <= 0) {
            return 0;
        }
        int frontSize = frontCamera2.size();
        int backSize = backCamera2.size();
        int totalSize = frontSize + backSize;
        int index = currentIndex2 % totalSize;
        currentIndex2++;
        if (index < frontSize) {
            return frontCamera2.get(index);
        } else {
            return backCamera2.get(index - frontSize);
        }
    }

    int currentIndex = 0;

    private int selectByCurrentIndex() {
        if (frontCameras == null || frontCameras.size() <= 0 || backCameras == null || backCameras.size() <= 0) {
            return 0;
        }
        int frontSize = frontCameras.size();
        int backSize = backCameras.size();
        int totalSize = frontSize + backSize;
        int index = currentIndex % totalSize;
        currentIndex++;
        if (index < frontSize) {
            return frontCameras.get(index);
        } else {
            return backCameras.get(index - frontSize);
        }
    }

    int currentFrontIndex = 0;
    int currentBackIndex = 0;

    private int selectByCurrentFacingIndex(int facing) {
        if (frontCameras == null || frontCameras.size() <= 0 || backCameras == null || backCameras.size() <= 0) {
            return 0;
        }
        int result = 0;
        switch (facing) {
            case Camera.CameraInfo.CAMERA_FACING_FRONT:
                result = frontCameras.get(currentFrontIndex % frontCameras.size());
                currentFrontIndex++;
                break;
            default:
                result = backCameras.get(currentBackIndex % backCameras.size());
                currentBackIndex++;
                break;
        }
        return result;
    }

    private int selectByScore(int facing, int cameraNum) {
        int maxScore = 0;
        int maxScoredId = 0;
        for (int i = 0; i < cameraNum; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == facing) {
                int score = scoreCamera(info);
                if (score > maxScore) {
                    maxScore = score;
                    maxScoredId = i;
                }
            }
        }
        return maxScoredId;
    }

    private int scoreCamera(Camera.CameraInfo info) {
        int score = 0;

        return score;
    }
}
