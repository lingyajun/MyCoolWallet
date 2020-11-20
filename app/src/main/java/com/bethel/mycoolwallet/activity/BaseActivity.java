package com.bethel.mycoolwallet.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.interfaces.IRequestCameraPermissions;
import com.bethel.mycoolwallet.manager.RequestPermissionsManager;

/**
 * 基类
 * */
public class BaseActivity extends AppCompatActivity implements IRequestCameraPermissions {
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
        permissionsManager.setPermissionsResult(grant -> onCameraPermissionsResult(grant));
        permissionsManager.requestCameraPermissions();
//        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
//        REQUEST_CAMERA_PERMISSION_CODE);
    }

    protected void requestCameraPermissionsIfNotGranted() {
        checkRequestPermissionsManager();
        if (permissionsManager.checkCameraPermission()) {
            onCameraPermissionsResult(true);
        } else {
            requestCameraPermissions();
//            permissionsManager.requestCameraPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkRequestPermissionsManager();
        boolean handle = permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!handle) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void onCameraPermissionsResult(boolean grant) {
    }

    private void checkRequestPermissionsManager() {
        if (null == permissionsManager) {
            permissionsManager = new RequestPermissionsManager(this);
        }
    }

    public RequestPermissionsManager getPermissionsManager() {
        checkRequestPermissionsManager();
        return permissionsManager;
    }

    /**
     * 顶部栏
     * Toolbar
     * */
    protected Toolbar initTitleBar(int titleRes) {
        return initTitleBar(titleRes, false);
    }
    protected Toolbar initTitleBar(int titleRes, boolean leftBack) {
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
        if (leftBack) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);//toolbar的左侧返回按钮
            toolBar.setNavigationOnClickListener((v)-> onBackPressed());
        }
        return toolBar;
    }

    protected  <T extends ViewModel> T getViewModel(@NonNull Class<T> modelClass) {
        return ViewModelProviders.of(this).get(modelClass);
    }
}
