
package com.wilbert.svcamera;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Permission implements View.OnClickListener {

    CameraFragment fragment;

    TextView tvCamera;
    boolean permissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvCamera = findViewById(R.id.tvCamera);
        tvCamera.setOnClickListener(this);
    }

    @Override
    protected void onCameraPermissionGranted() {
        super.onCameraPermissionGranted();
        permissionGranted = true;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tvCamera:
                if(!permissionGranted){
                    return;
                }
                if (fragment == null) {
                    fragment = new CameraFragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.layout, fragment).commit();
                }
                break;
        }
    }
}
