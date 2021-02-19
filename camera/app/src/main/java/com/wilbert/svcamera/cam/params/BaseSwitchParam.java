package com.wilbert.svcamera.cam.params;

import android.hardware.Camera;

import com.wilbert.svcamera.cam.abs.CameraType;
import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.camImpl.CameraImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wilbert
 * @Date 2021/2/19 12:08
 * @email jiangwang.wilbert@bigo.sg
 **/
public abstract class BaseSwitchParam<T> extends BaseParam {

    protected List<T> switchList = new ArrayList<>();
    protected T currentParam;
    protected int currentIndex = 0;

    protected abstract void initSwitchList(Camera.Parameters parameters, List<T> switchList);

    protected void init(ICamera camera) {
        if (camera == null) {
            return;
        }
        CameraType type = camera.getCameraType();
        switch (type) {
            case Camera: {
                Camera.Parameters parameters = ((CameraImpl) camera).getParameters();
                if (switchList.isEmpty()) {
                    initSwitchList(parameters, switchList);
                }
            }
            break;
            case Camera2:
                break;
        }
    }

    public List<T> getSwitchList(ICamera camera) {
        init(camera);
        return switchList;
    }

    public void switchParam(ICamera camera) {
        init(camera);
        if (switchList == null || switchList.size() <= 0) {
            return;
        }
        currentParam = switchList.get(currentIndex % switchList.size());
        currentIndex++;
        reset();
    }

    public int getCurrentIndex() {
        return currentIndex % switchList.size();
    }
}
