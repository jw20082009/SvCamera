package com.wilbert.svcamera.cam.params;

import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;

import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.ParamType;

import java.util.List;

/**
 * @author wilbert
 * @Date 2021/2/18 21:00
 * @email jiangwang.wilbert@bigo.sg
 **/
public class PreviewSize extends BaseSwitchParam<PreviewSize.Size> {
    private final String TAG = "PreviewSize";
    private int width;
    private int height;
    private int presetWidth;
    private int presetHeight;
    private boolean cameraRecordingHintEnabled = false;

    public PreviewSize(int width,int height){
        this.presetWidth = width;
        this.presetHeight = height;
    }

    public int getPresetWidth() {
        return presetWidth;
    }

    public void setPresetWidth(int presetWidth) {
        this.presetWidth = presetWidth;
    }

    public int getPresetHeight() {
        return presetHeight;
    }

    public void setPresetHeight(int presetHeight) {
        this.presetHeight = presetHeight;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public ParamType getType() {
        return ParamType.PreviewSize;
    }

    public boolean isCameraRecordingHintEnabled() {
        return cameraRecordingHintEnabled;
    }

    public void setCameraRecordingHintEnabled(boolean cameraRecordingHintEnabled) {
        this.cameraRecordingHintEnabled = cameraRecordingHintEnabled;
    }

    @Override
    void applyCamera(ICamera camera, Camera.Parameters parameters) {
        if(currentParam == null){
            init(camera);
        }
        width = currentParam.width;
        height = currentParam.height;
        parameters.setPreviewSize(width,height);
    }

    @Override
    void applyCamera2(ICamera camera,CaptureRequest.Builder captureBuilder) {
    }

    @Override
    protected void initSwitchList(Camera.Parameters parameters,List<PreviewSize.Size> switchList) {
        List<Camera.Size> preview_list = parameters.getSupportedPreviewSizes();
        if (cameraRecordingHintEnabled) { // 针对部分手机设置record-hint 同时设置video-size
            List<Camera.Size> video_list = parameters.getSupportedVideoSizes();
            if (video_list != null) {
                boolean hasIntersection = false;
                try {
                    preview_list.retainAll(video_list);
                    if (!preview_list.isEmpty()) {
                        hasIntersection = true;
                    }
                } catch (NullPointerException npe) {
                    Log.e(TAG, "[onCameraOpen] record-hint null point " + npe.getMessage());
                    npe.printStackTrace();
                }
                if (!hasIntersection) {
                    cameraRecordingHintEnabled = false;
                    preview_list = parameters.getSupportedPreviewSizes();
                    if( preview_list == null || preview_list.size() == 0){
                        Log.e(TAG,"[onCameraOpen][FATAL ERROR] "
                                + "hasIntersection false and getSupportedPreviewSizes is empty");
                        return;
                    }
                }
            }else{
                cameraRecordingHintEnabled = false;
                Log.e(TAG, "[onCameraOpen] video_list is null, so mCameraRecordingHintEnabled false" );
            }
        }
        if (preview_list == null || preview_list.size() == 0) {
            Log.e(TAG,"getSupportedPreviewSizes is empty");
            return ;
        }
        boolean selected = false;
        switchList.clear();
        for(int i = 0;i< preview_list.size();i++){
            Camera.Size size = preview_list.get(i);
            Size s = new Size(size);
            switchList.add(s);
            if((s.width == presetWidth && s.height == presetHeight) || (s.width == presetHeight && s.height == presetWidth)){
                currentParam = s;
                currentIndex = i;
                selected = true;
            }
        }
        if(!selected){
            currentParam = switchList.get(0);
            currentIndex = 0;
        }
    }

    public static class Size{
        int width;
        int height;
        public Size(int width,int height){
            this.width = width;
            this.height = height;
        }
        public Size(){}

        public Size(Camera.Size size){
            this.width = size.width;
            this.height = size.height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "{" +width +
                    "*" + height +
                    '}';
        }
    }
}
