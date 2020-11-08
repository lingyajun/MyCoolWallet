package com.bethel.mycoolwallet.interfaces;

import android.content.Context;

public interface IRequestStoragePermissions {
    boolean checkStoragePermission(Context context);
    void requestStoragePermissions(Object host);
    void onStoragePermissionsResult(boolean grant);
}
