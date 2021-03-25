package com.wilbert.svcamera.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

public interface IRender {

    void init(GLSurfaceView surfaceView);
    SurfaceTexture getSurfaceTexture();
    void onCameraOpened(int width,int height);
}
