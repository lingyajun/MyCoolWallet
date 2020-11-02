package com.bethel.mycoolwallet.manager;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bethel.mycoolwallet.interfaces.IRequestPermissions;

public  class RequestPermissionsManager implements IRequestPermissions {
    /**
     * 相机权限
     */
    public static final int REQUEST_CAMERA_PERMISSION_CODE = 1777;

    private Activity activity;
    private IRequestPermissions iRequest;

    public  RequestPermissionsManager(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestCameraPermissions() {
        ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.CAMERA }, REQUEST_CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onCameraPermissionsResult(boolean grant) {
        if (null != iRequest) {
            iRequest.onCameraPermissionsResult(grant);
        }
    }

    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                onCameraPermissionsResult(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            default:  return false;
        }
        return true;
    }

    public void setiRequest(IRequestPermissions iRequest) {
        this.iRequest = iRequest;
    }
}
