package com.bethel.mycoolwallet.manager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bethel.mycoolwallet.interfaces.IPermissionsResult;
import com.bethel.mycoolwallet.interfaces.IRequestCameraPermissions;
import com.bethel.mycoolwallet.interfaces.IRequestStoragePermissions;

public  class RequestPermissionsManager implements IRequestCameraPermissions, IRequestStoragePermissions {
    /**
     * 相机权限
     */
    public static final int REQUEST_CAMERA_PERMISSION_CODE = 1777;

    /**
     * 相机权限
     */
    public static final int REQUEST_STORAGE_PERMISSION_CODE = 1778;

    private Activity activity;
//    private IRequestCameraPermissions iCameraRequest;
    private IPermissionsResult permissionsResult;
//    private IRequestStoragePermissions iStorageRequest;

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
//        if (null != iCameraRequest) {
//            iCameraRequest.onCameraPermissionsResult(grant);
//        }
        if (null != permissionsResult) permissionsResult.onPermissionsResult(grant);
    }

    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean grant = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                onCameraPermissionsResult(grant);
                break;
            case REQUEST_STORAGE_PERMISSION_CODE:
                onStoragePermissionsResult(grant);
                break;
            default:  return false;
        }
        return true;
    }

//    public void setiCameraRequest(IRequestCameraPermissions iRequest) {
//        this.iCameraRequest = iRequest;
//    }
//
//    public IPermissionsResult getPermissionsResult() {
//        return permissionsResult;
//    }

    public void setPermissionsResult(IPermissionsResult permissionsResult) {
        this.permissionsResult = permissionsResult;
    }

    @Override
    public boolean checkStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @param host :
     *  Activity or Fragment
     */
    @Override
    public void requestStoragePermissions(Object host) {
        requestPermissions(host, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_STORAGE_PERMISSION_CODE);
    }

    private void requestPermissions(Object host, final String permission,final int requestCode) {
        if (host instanceof Activity) {
            ActivityCompat.requestPermissions((Activity)host, new String[] { permission}, requestCode);
        } else if (host instanceof Fragment) {
            ((Fragment) host).requestPermissions( new String[] { permission}, requestCode);
        } else if (host instanceof android.app.Fragment) {
            if (Build.VERSION.SDK_INT>=23)
            ((android.app.Fragment) host).requestPermissions( new String[] { permission}, requestCode);
        }
    }

    @Override
    public void onStoragePermissionsResult(boolean grant) {
        if (null != permissionsResult) permissionsResult.onPermissionsResult(grant);
    }
}
