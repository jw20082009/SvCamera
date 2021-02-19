package com.wilbert.svcamera.cam.camImpl;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.wilbert.svcamera.cam.abs.ICameraIndex;

import java.util.ArrayList;

/**
 * @author wilbert
 * @Date 2021/2/18 20:26
 * @email jiangwang.wilbert@bigo.sg
 **/
public class DefaultCameraIndex implements ICameraIndex {
    private final String TAG = "DefaultCameraIndex";
    private int mFrontCameraIndex = -1;
    private int mBackCameraIndex = -1;
    private int mCurrentIndex = -1;
    private Context mContext;

    public DefaultCameraIndex(Context context){
        mContext = context;
        init();
    }

    public void init() {
        ArrayList<Integer> frontIndexList = new ArrayList();
        ArrayList<Integer> backIndexList = new ArrayList();
        try {
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    frontIndexList.add(i);
//                    mFrontCameraIndex = i;
                } else {
                    backIndexList.add(i);
//                    mBackCameraIndex = i;
                }
            }
            if ((frontIndexList.size() > 1 || backIndexList.size() > 1)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mFrontCameraIndex = getOptimalCameraIndexByAPI2(mContext, frontIndexList);
                mBackCameraIndex = getOptimalCameraIndexByAPI2(mContext, backIndexList);
            } else {
                if (!frontIndexList.isEmpty()) {
                    mFrontCameraIndex = frontIndexList.get(0);
                }
                if (!backIndexList.isEmpty()) {
                    mBackCameraIndex = backIndexList.get(0);
                }
            }
            mCurrentIndex = mFrontCameraIndex;
        } catch(Exception e) {
            e.printStackTrace();
            Log.i(TAG,"failed to get camera info.");
        }
    }

    @Override
    public int switchCamera() {
        mCurrentIndex = mCurrentIndex == mFrontCameraIndex? mBackCameraIndex:mFrontCameraIndex;
        return mCurrentIndex;
    }

    @Override
    public int getCameraIndex() {
        return mCurrentIndex;
    }

    @Override
    public boolean isFacingFront() {
        return mCurrentIndex == mFrontCameraIndex;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getOptimalCameraIndexByAPI2(Context context, ArrayList<Integer> indexList) {
        if (indexList.isEmpty()) {
            return -1;
        }
        if (indexList.size() == 1) {
            return indexList.get(0);
        }

        int optimalIndex = indexList.get(0);

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIdList = new String[0];
        try {
            cameraIdList = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.toString());
            return optimalIndex;
        }

        if (cameraIdList.length == 0) {
            return optimalIndex;
        }

        for (Integer index : indexList) {
            String cameraId = cameraIdList[index];
            CameraCharacteristics cameraCharacteristics = null;
            try {
                cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics == null) {
                    continue;
                }
                if (cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF).intValue() > 0) {
                    optimalIndex = index;
                    break;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        return optimalIndex;
    }
}
