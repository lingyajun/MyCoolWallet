package com.bethel.mycoolwallet.interfaces;

public interface IRequestPermissions {
    boolean checkCameraPermission();
    void requestCameraPermissions();
    void onCameraPermissionsResult(boolean grant);
}
