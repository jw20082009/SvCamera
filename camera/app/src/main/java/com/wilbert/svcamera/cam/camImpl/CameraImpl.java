package com.wilbert.svcamera.cam.camImpl;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.wilbert.svcamera.cam.abs.CameraStatus;
import com.wilbert.svcamera.cam.abs.CameraType;
import com.wilbert.svcamera.cam.abs.FrameWrapper;
import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.ICameraCallback;
import com.wilbert.svcamera.cam.abs.ICameraIndex;
import com.wilbert.svcamera.cam.abs.IFrameCallback;
import com.wilbert.svcamera.cam.abs.IParam;
import com.wilbert.svcamera.cam.abs.ParamType;
import com.wilbert.svcamera.cam.params.PreviewFormat;
import com.wilbert.svcamera.cam.params.PreviewSize;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wilbert
 * @Date 2021/2/18 17:03
 * @email jiangwang.wilbert@bigo.sg
 **/
public class CameraImpl implements ICamera {
    private static final String TAG = "CameraImpl";
    private CameraHandler mHandler;
    private Object mLock = new Object();
    private LinkedHashMap<ParamType, IParam> mParams = new LinkedHashMap<>();
    private SurfaceTexture mSurfaceTexture;
    private CameraFrameWrapper mFrameWrapper;
    private ICameraIndex mCameraIndex;
    private AtomicInteger mStatus = new AtomicInteger(CameraStatus.Released.ordinal());
    private int mDisplayRotation = 270;
    private IFrameCallback mFrameCallback;
    private ICameraCallback mCallback;
    private boolean mNeedYuvData = false;
    private FrameProcessor mFrameProcessor;

    private Camera mCamera;
    private Camera.Parameters mParameters;
    private Camera.CameraInfo mCameraInfo;

    public int getDisplayRotation() {
        return mDisplayRotation;
    }

    public void setDisplayRotation(int displayRotation) {
        this.mDisplayRotation = displayRotation;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public void init() {
        synchronized (mLock) {
            HandlerThread handlerThread = new HandlerThread("CameraHandler_" + hashCode());
            handlerThread.start();
            mHandler = new CameraHandler(handlerThread.getLooper(), this);
        }
    }

    @Override
    public void open(ICameraIndex index) {
        synchronized (mLock) {
            if (mHandler != null) {
                mHandler.removeMessages(MSG_OPEN);
                Message msg = mHandler.obtainMessage(MSG_OPEN);
                msg.obj = index;
                msg.sendToTarget();
            }
        }
    }

    @Override
    public void close() {
        synchronized (mLock) {
            if (mHandler != null) {
                mHandler.removeMessages(MSG_CLOSE);
                Message msg = mHandler.obtainMessage(MSG_CLOSE);
                msg.sendToTarget();
            }
        }
    }

    @Override
    public void release() {
        synchronized (mLock) {
            if (mHandler != null) {
                mHandler.release();
                mHandler = null;
            }
        }
    }

    @Override
    public void applyParam(IParam param) {
        if(param == null){
            return;
        }
        synchronized (mLock){
            mParams.put(param.getType(),param);
            if (mHandler != null) {
                mHandler.removeMessages(MSG_APPLY_PARAMS);
                Message msg = mHandler.obtainMessage(MSG_APPLY_PARAMS);
                msg.sendToTarget();
            }
        }
    }

    @Override
    public void applyParams(List<IParam> params) {
        if(params == null || params.size() <= 0){
            return;
        }
        synchronized (mLock){
            for(IParam param:params){
                mParams.put(param.getType(),param);
            }
            if (mHandler != null) {
                mHandler.removeMessages(MSG_APPLY_PARAMS);
                Message msg = mHandler.obtainMessage(MSG_APPLY_PARAMS);
                msg.sendToTarget();
            }
        }
    }

    private void _applyParams(){
        Iterator it = mParams.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<ParamType, IParam> entry = (Map.Entry<ParamType, IParam>) it.next();
            IParam param = entry.getValue();
            if(!param.isApplied()){
                param.apply(this);
            }
        }
        try {
            mCamera.setParameters(mParameters);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public IParam getParam(ParamType type) {
        if(type == null){
            return null;
        }
        return mParams.get(type);
    }

    @Override
    public List<IParam> getParams() {
        return null;
    }

    @Override
    public void startPreview() {
        synchronized (mLock) {
            if (mHandler != null) {
                mHandler.removeMessages(MSG_START_PREVIEW);
                Message msg = mHandler.obtainMessage(MSG_START_PREVIEW);
                msg.sendToTarget();
            }
        }
    }

    @Override
    public void setCallback(ICameraCallback callback) {
        synchronized (mLock){
            mCallback = callback;
        }
    }

    @Override
    public void setFrameCallback(IFrameCallback callback,boolean needYuv) {
        synchronized (mLock) {
            mFrameCallback = callback;
            mNeedYuvData = needYuv;
        }
    }

    @Override
    public ICameraIndex getCameraIndex() {
        return mCameraIndex;
    }

    @Override
    public CameraType getCameraType() {
        return CameraType.Camera;
    }

    @Override
    public FrameWrapper getCurrentFrame() {
        return mFrameWrapper;
    }

    public Camera.Parameters getParameters(){
        return mParameters;
    }

    public Camera.CameraInfo getCameraInfo(){
        return mCameraInfo;
    }

    private void _releaseCamera(){
        if(mCamera != null){
            try {
                mCamera.setPreviewTexture(null);
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.setPreviewDisplay(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initCameraParams(){
        PreviewSize previewSize = new PreviewSize(1280,720);
        PreviewFormat format = new PreviewFormat();
        mParams.put(previewSize.getType(),previewSize);
        mParams.put(format.getType(),format);
    }

    private int calcPreviewOrientation(int displayRotation, Camera.CameraInfo info){
        int rotation = displayRotation;
        int cameraOrientation = info.orientation;
        int result;
        if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (cameraOrientation + rotation)%360;
            result = (360 - result)%360;
        }else{
            result = (cameraOrientation - rotation + 360)%360;
        }
        return result;
    }

    private final int MSG_OPEN = 0x01;
    private final int MSG_CLOSE = 0x02;
    private final int MSG_START_PREVIEW = 0X03;
    private final int MSG_APPLY_PARAMS = 0x04;

    private void handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case MSG_OPEN:
                _releaseCamera();
                mCameraIndex = (ICameraIndex) msg.obj;
                if(mCameraIndex == null){
                    return;
                }
                int cameraId = mCameraIndex.getCameraIndex();
                try {
                    mCamera = Camera.open(cameraId);
                    mCameraInfo = new Camera.CameraInfo();
                    Camera.getCameraInfo(cameraId,mCameraInfo);
                    mParameters = mCamera.getParameters();
                    initCameraParams();
                    _applyParams();
                    if(mCallback != null){
                        mCallback.onCameraStatusChanged(CameraStatus.Opened,CameraImpl.this);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case MSG_CLOSE:
                _releaseCamera();
                if(mCallback != null){
                    mCallback.onCameraStatusChanged(CameraStatus.Released,CameraImpl.this);
                }
                break;
            case MSG_START_PREVIEW:
                synchronized (mLock) {
                    if (mSurfaceTexture == null) {
                        mSurfaceTexture = new SurfaceTexture(0);
                        mSurfaceTexture.detachFromGLContext();
                    }
                    mFrameWrapper = new CameraFrameWrapper(mSurfaceTexture);
                    if(mNeedYuvData){
                        int previewWidth = getPreviewSize().getWidth();
                        int previewHeight = getPreviewSize().getHeight();
                        byte[] frameData = new byte[previewWidth * previewHeight * 3/2];
                        byte[] frameData2 = new byte[previewWidth * previewHeight * 3/2];
                        mCamera.addCallbackBuffer(frameData);
                        mCamera.addCallbackBuffer(frameData2);
                        mCamera.setPreviewCallbackWithBuffer(mFrameProcessor);
                    }
                }
                int rotation = calcPreviewOrientation(mDisplayRotation,mCameraInfo);
                mCamera.setDisplayOrientation(rotation);
                try {
                    mCamera.setPreviewTexture(mSurfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(mCamera == null){
                    return;
                }
                mCamera.startPreview();
                if(mCallback != null){
                    mCallback.onCameraStatusChanged(CameraStatus.Previewed,CameraImpl.this);
                }
                break;
            case MSG_APPLY_PARAMS:
                _applyParams();
                break;
        }
    }

    public PreviewSize getPreviewSize(){
        IParam param = mParams.get(ParamType.PreviewSize);
        return param == null?null:(PreviewSize)param;
    }

    private void onPreviewFrame(byte[] data,Camera camera){
        mFrameWrapper.setYuvFrame(data);
        if(mFrameCallback != null){
            mFrameCallback.onFrameAvailable(mFrameWrapper);
        }
    }

    class FrameProcessor implements Camera.PreviewCallback{
        private final SoftReference<CameraImpl> reference;
        public FrameProcessor(CameraImpl impl){
            reference = new SoftReference<>(impl);
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            CameraImpl impl = null;
            synchronized (reference) {
                impl = reference.get();
                if (impl == null) {
                    Log.e(TAG, "CameraHandler handleMessage CameraImpl null");
                    return;
                }
            }
            impl.onPreviewFrame(data,camera);
        }
    }

    private static class CameraHandler extends Handler {
        private final SoftReference<CameraImpl> reference;

        public CameraHandler(Looper looper, CameraImpl impl) {
            super(looper);
            reference = new SoftReference<>(impl);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            CameraImpl impl = null;
            synchronized (reference) {
                impl = reference.get();
                if (impl == null) {
                    Log.e(TAG, "CameraHandler handleMessage CameraImpl null");
                    return;
                }
            }
            impl.handleMessage(msg);
        }

        public void release() {
            synchronized (reference) {
                reference.clear();
                Looper looper = getLooper();
                if (looper != null) {
                    removeCallbacksAndMessages(null);
                    looper.quit();
                }
            }
        }
    }
}
