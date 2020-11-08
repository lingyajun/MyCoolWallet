package com.bethel.mycoolwallet.interfaces;

public interface IRequestCameraPermissions {
    boolean checkCameraPermission();
    void requestCameraPermissions();
    void onCameraPermissionsResult(boolean grant);
}
