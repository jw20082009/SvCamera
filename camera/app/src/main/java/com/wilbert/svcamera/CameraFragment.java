package com.wilbert.svcamera;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.wilbert.svcamera.render.IRender;
import com.wilbert.svcamera.render.YuvRender;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment {
    final String TAG = "CameraFragment";

    GLSurfaceView mSurfaceView;
    CameraHandler mHandler;
    Camera.CameraInfo mCameraInfo;

    AtomicBoolean mCameraOpened = new AtomicBoolean(false);

    int mPreviewWidth = 1280;
    int mPreviewHeight = 720;
    int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    IRender mRender;

    Object mLock = new Object();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_camera,container,false);
        mSurfaceView = layout.findViewById(R.id.gl_surfaceview);
        mRender = new YuvRender();
        mRender.init(mSurfaceView);
        initCamera();
        return layout;
    }

    private void initCamera(){
        HandlerThread thread = new HandlerThread(TAG+"_CameraThread");
        thread.start();
        mHandler = new CameraHandler(thread.getLooper());
    }

    @Override
    public void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.release();
    }

    public int getOrientation(){
        if(mCameraInfo == null){
            return 0;
        }
        return mCameraInfo.orientation;
    }

    public boolean isFlipHorizontal(){
        if(mCameraInfo == null){
            return false;
        }
        return mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? true : false;
    }

    public boolean isFlipVertical(){
        if(mCameraInfo == null){
            return false;
        }
        return (mCameraInfo.orientation == 90 || mCameraInfo.orientation == 270) ? true : false;
    }

    class CameraHandler extends Handler{

        Camera camera;

        public static final int MSG_CAMERA_START = 0x01;

        public static final int MSG_CAMERA_PREVIEW = 0x02;

        public CameraHandler(Looper looper){
            super(looper);
            sendEmptyMessage(MSG_CAMERA_START);
            sendEmptyMessage(MSG_CAMERA_PREVIEW);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CAMERA_START:
                    Log.i(TAG,"start Camera");
                    if(mCameraOpened.get())
                        return;
                    mCameraInfo = new Camera.CameraInfo();
                    Camera.getCameraInfo(mCameraId,mCameraInfo);
                    camera = Camera.open(mCameraId);
                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setPreviewSize(mPreviewWidth,mPreviewHeight);
//                    parameters.setPictureSize(960,540);
                    camera.setParameters(parameters);
                    mCameraOpened.set(true);
                    break;
                case MSG_CAMERA_PREVIEW:{
                    if(camera == null){
                        break;
                    }

                    try {
                        camera.setDisplayOrientation(calcPreviewOrientation(getContext(),mCameraInfo));
                        camera.addCallbackBuffer(new byte[mPreviewWidth*mPreviewHeight*3/2]);
                        camera.addCallbackBuffer(new byte[mPreviewWidth*mPreviewHeight*3/2]);
                        camera.setPreviewCallbackWithBuffer(mPreviewCallback);
                        camera.setPreviewTexture(mRender.getSurfaceTexture());
                        camera.startPreview();
                        mRender.onCameraOpened(mPreviewWidth,mPreviewHeight);
                        if(mRender instanceof YuvRender){
                            ((YuvRender) mRender).adjustTextureBuffer(getOrientation(),isFlipHorizontal(),isFlipVertical());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                    break;
            }
        }

        public void release(){
            post(new Runnable() {
                @Override
                public void run() {
                    if(camera != null){
                        camera.release();
                        camera = null;
                    }
                    mCameraOpened.set(false);
                    getLooper().quit();
                }
            });
        }

        private int calcPreviewOrientation(Context context, Camera.CameraInfo info){
            int rotation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation){
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }
            int result;
            if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                result = (info.orientation + degrees)%360;
                result = (360 - result)%360;
            }else{
                result = (info.orientation - degrees + 360)%360;
            }
            return result;
        }

    }

    int previewFrameTimes = 0;

    Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            Log.i(TAG,"onPreviewFrame,avaliableTimes:"+avaliableTimes+";previewFrameTimes:"+previewFrameTimes);
            previewFrameTimes ++;
            if(mRender instanceof YuvRender){
                ((YuvRender) mRender).onFrameAvailable(data);
            }
            camera.addCallbackBuffer(data);
        }
    };
}
