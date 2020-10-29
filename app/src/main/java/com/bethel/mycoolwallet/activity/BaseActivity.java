package com.bethel.mycoolwallet.activity;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 基类
 * */
public class BaseActivity extends AppCompatActivity {
    /**
     * 相机权限
     */
    public static final int REQUEST_CAMERA_PERMISSION_CODE = 777;

    protected boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, REQUEST_CAMERA_PERMISSION_CODE);
    }

    protected void requestCameraPermissionsIfNotGranted() {
        if (checkCameraPermission()) {
            onCameraPermissionsResult(true);
        } else {
            requestCameraPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                onCameraPermissionsResult(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
                default:  super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void onCameraPermissionsResult(boolean grant) {
    }
}
