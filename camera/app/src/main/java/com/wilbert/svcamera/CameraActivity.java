package com.wilbert.svcamera;

import android.os.Bundle;

public class CameraActivity extends Permission {

    CameraFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onCameraPermissionGranted() {
        super.onCameraPermissionGranted();
        if(fragment == null){
            fragment = new CameraFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.layout,fragment).commit();
        }
    }
}
