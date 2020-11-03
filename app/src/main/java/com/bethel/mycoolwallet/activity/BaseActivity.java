package com.bethel.mycoolwallet.activity;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.interfaces.IRequestPermissions;
import com.bethel.mycoolwallet.manager.RequestPermissionsManager;

/**
 * 基类
 * */
public class BaseActivity extends AppCompatActivity implements IRequestPermissions {
    /**
     * 相机权限
     */
//    public static final int REQUEST_CAMERA_PERMISSION_CODE = 1777;

    RequestPermissionsManager permissionsManager = null;

    public boolean checkCameraPermission() {
        checkRequestPermissionsManager();
        return permissionsManager.checkCameraPermission();
//        return ContextCompat.checkSelfPermission(this,
//                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestCameraPermissions() {
        checkRequestPermissionsManager();
        permissionsManager.requestCameraPermissions();
//        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
//        REQUEST_CAMERA_PERMISSION_CODE);
    }

    protected void requestCameraPermissionsIfNotGranted() {
        checkRequestPermissionsManager();
        if (permissionsManager.checkCameraPermission()) {
            onCameraPermissionsResult(true);
        } else {
            permissionsManager.requestCameraPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkRequestPermissionsManager();
        boolean handle = permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!handle) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
//        switch (requestCode) {
//            case REQUEST_CAMERA_PERMISSION_CODE:
//                onCameraPermissionsResult(grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED);
//                break;
//                default:  super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
    }

    public void onCameraPermissionsResult(boolean grant) {
    }

    private void checkRequestPermissionsManager() {
        if (null == permissionsManager) {
            permissionsManager = new RequestPermissionsManager(this);
            if (this instanceof IRequestPermissions)
            permissionsManager.setiRequest((IRequestPermissions)this);
        }
    }


    /**
     * 顶部栏
     * Toolbar
     * */
    protected Toolbar initTitleBar(int titleRes) {
        //隐藏默认actionbar
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.hide();
        }

        //获取toolbar
        Toolbar toolBar = findViewById(R.id.toolbar);
        //主标题，必须在setSupportActionBar之前设置，否则无效，如果放在其他位置，则直接setTitle即可
        if (0 != titleRes)
            toolBar.setTitle(titleRes);
        //用toolbar替换actionbar
        setSupportActionBar(toolBar);
        return toolBar;
    }

}
