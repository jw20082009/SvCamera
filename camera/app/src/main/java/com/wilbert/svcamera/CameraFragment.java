package com.wilbert.svcamera;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.wilbert.svcamera.cam.abs.CameraStatus;
import com.wilbert.svcamera.cam.abs.ICamera;
import com.wilbert.svcamera.cam.abs.ICameraCallback;
import com.wilbert.svcamera.cam.abs.ICameraIndex;
import com.wilbert.svcamera.cam.abs.IParam;
import com.wilbert.svcamera.cam.abs.ParamType;
import com.wilbert.svcamera.cam.camImpl.CameraImpl;
import com.wilbert.svcamera.cam.camImpl.DefaultCameraIndex;
import com.wilbert.svcamera.cam.params.BaseSwitchParam;
import com.wilbert.svcamera.cam.params.Focus;
import com.wilbert.svcamera.cam.params.PreviewSize;
import com.wilbert.svcamera.cam.params.Zoom;
import com.wilbert.svcamera.views.FocusView;

import java.util.List;

public class CameraFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "CameraFragment";
    GLSurfaceView mSurfaceView;
    FocusView mFocusView;
    LinearLayout mTestLinear;
    SeekBar mSeekBar;
    ICamera mCamera;
    CameraRenderer mCameraRenderer;
    ICameraIndex mCameraIndex;

    int screenWidth;
    int screenHeight;
    byte[] mYuvData;
    float[] mTestRect = new float[8];
    Matrix matrix;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_camera, container, false);
        mSurfaceView = layout.findViewById(R.id.gl_surfaceview);
        mTestLinear = layout.findViewById(R.id.ll_test_view);
        mSeekBar = layout.findViewById(R.id.zoomBar);
        mSeekBar.setOnSeekBarChangeListener(zoomListener);
        mFocusView = layout.findViewById(R.id.focusView);
        mFocusView.setFocusListener(mFocusListener);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        float[] p = new float[]{1, 1};
        Matrix matrix = new Matrix();
//        matrix.setRotate(90,2,2);
        matrix.setScale(1, -1, 2, 2);
        matrix.mapPoints(p);
        Log.e("mapPoints", "point:" + p[0] + "*" + p[1]);
        int width = screenWidth;
        int height = screenHeight;
        if (1.0f * screenWidth / screenHeight > 720.0f / 1280.0f) {
            width = (int) (screenHeight * 720f / 1280f);
        } else {
            height = (int) (screenWidth / (720f / 1280f));
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        params.width = width;
        params.height = height;
        mSurfaceView.setLayoutParams(params);
        mSurfaceView.setKeepScreenOn(true);
        initCamera();
        return layout;
    }

    private BaseSwitchParam getSwitchParam(ParamType type) {
        IParam param = mCamera.getParam(type);
        BaseSwitchParam result = null;
        if (param != null) {
            result = (BaseSwitchParam) param;
        } else {
            switch (type) {
                case PreviewSize:
                    result = new PreviewSize(1280, 720);
                    mCamera.applyParam(result);
                    break;
            }
        }
        return result;
    }

    private <T> String getSwitchText(BaseSwitchParam<T> switchParam){
        List<T> switchList = switchParam.getSwitchList(mCamera);
        int currentIndex = switchParam.getCurrentIndex();
        StringBuilder sb = new StringBuilder("previewSize:");
        for (int i = 0; i < switchList.size(); i++) {
            T t = switchList.get(i);
            if (i == currentIndex) {
                sb.append("<font color=\"red\">");
            }
            sb.append(t.toString());
            sb.append(";");
            if (i == currentIndex) {
                sb.append("</font>");
            }
        }
        return sb.toString();
    }

    private <T> void initSwitchListView(BaseSwitchParam<T> switchParam) {
        TextView textview = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 20;
        params.bottomMargin = 20;
        params.leftMargin = 10;
        params.rightMargin = 10;
        textview.setLayoutParams(params);
        textview.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        textview.setText(Html.fromHtml(getSwitchText(switchParam)));
        textview.setTag(switchParam.getType());
        textview.setOnClickListener(this);
        textview.setTextSize(10);
        mTestLinear.addView(textview);
    }

    private int getDisplayRotation() {
        int rotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
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
        return degrees;
    }

    FocusView.FocusListener mFocusListener = new FocusView.FocusListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onFocusEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            int width = mFocusView.getWidth();
            int height = mFocusView.getHeight();
            Log.e(TAG, "onSingleTapConfirmed:" + x + "*" + y + ";" + width + "*" + height);
            IParam param = mCamera.getParam(ParamType.Focus);
            if (param == null) {
                param = new Focus();
            }
            ((Focus) param).requestFocus(x, y, width, height);
            mCamera.applyParam(param);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initCamera() {
        mCamera = new CameraImpl();
        mCamera.init();
        ((CameraImpl) mCamera).setDisplayRotation(getDisplayRotation());
        mCameraIndex = new DefaultCameraIndex(getContext());
        mCameraRenderer = new CameraRenderer(mSurfaceView);
        mCamera.setCallback(new ICameraCallback() {
            @Override
            public void onCameraStatusChanged(CameraStatus status, ICamera camera) {
                switch (status) {
                    case Previewed:
                        PreviewSize previewSize = (PreviewSize) camera.getParam(ParamType.PreviewSize);
                        mCameraRenderer.onPreviewSizeChanged(previewSize.getWidth(), previewSize.getHeight());
                        if (camera instanceof CameraImpl) {
                            mCameraRenderer.setSurfaceTexture(((CameraImpl) camera).getSurfaceTexture());
                        }
                        mTestLinear.post(new Runnable() {
                            @Override
                            public void run() {
                                initSwitchListView(getSwitchParam(ParamType.PreviewSize));
                            }
                        });
                        break;
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mCamera.open(mCameraIndex);
        mCamera.startPreview();
        mSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCamera.close();
        releaseGLRes();
    }

    private void releaseGLRes() {
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraRenderer.release();
            }
        });
        mSurfaceView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    SeekBar.OnSeekBarChangeListener zoomListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                IParam param = mCamera.getParam(ParamType.Zoom);
                if (param == null) {
                    param = new Zoom();
                }
                ((Zoom) param).setZoom(progress);
                mCamera.applyParam(param);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if(!(tag instanceof ParamType)){
            return;
        }
        ParamType type = (ParamType) tag;
        switch (type){
            case PreviewSize:
                BaseSwitchParam param = (BaseSwitchParam) mCamera.getParam(ParamType.PreviewSize);
                if(param == null){
                    return;
                }
                param.switchParam(mCamera);
                mCamera.applyParam(param);
                ((TextView)v).setText(Html.fromHtml(getSwitchText(param)));
                break;
        }
    }
}
