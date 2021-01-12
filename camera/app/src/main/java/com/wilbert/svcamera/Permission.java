package com.wilbert.svcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class Permission extends AppCompatActivity {


    private String[] permissions = new String[]{Manifest.permission.CAMERA};
    private int[] permissionResults;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean needRequest = false;
            permissionResults = new int[permissions.length];
            for(int i = 0;i<permissions.length;i++){
                String p = permissions[i];
                if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                    needRequest = true;
                }
                permissionResults[i] = PackageManager.PERMISSION_GRANTED;
            }
            if(needRequest){
                requestPermissions(permissions, 0);
            }else{
                handlePermission(0,permissions,permissionResults);
            }

        }
    }

    protected void handlePermission(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == 0){
            boolean granted = true;
            for(int g:grantResults){
                if(g != PackageManager.PERMISSION_GRANTED){
                    granted = false;
                    break;
                }
            }
            if(granted){
                onCameraPermissionGranted();
            }else{
                Toast.makeText(this,"权限被拒绝",Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void onCameraPermissionGranted(){

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        handlePermission(requestCode,permissions,grantResults);
    }
}