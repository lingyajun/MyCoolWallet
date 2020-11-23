/*
 * Copyright (C) 2019 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.bethel.mycoolwallet.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.OnFirstPreDraw;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xqrcode.ui.CaptureActivity;
import com.xuexiang.xui.widget.toast.XToast;

/**
 * 自定义二维码扫描界面
 *
 * @author xuexiang
 * @since 2019/5/30 10:43
 */
public class CustomCaptureActivity extends CaptureActivity implements View.OnClickListener {
    private static final String INTENT_EXTRA_SCENE_TRANSITION_X = "scene_transition_x";
    private static final String INTENT_EXTRA_SCENE_TRANSITION_Y = "scene_transition_y";

    /**
     * 开始二维码扫描
     *
     * @param fragment
     * @param requestCode 请求码
     * @param theme       主题
     */
    public static void start(Fragment fragment, int requestCode, int theme) {
        Intent intent = new Intent(fragment.getContext(), CustomCaptureActivity.class);
        intent.putExtra(KEY_CAPTURE_THEME, theme);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 开始二维码扫描
     *
     * @param activity
     * @param requestCode 请求码
     * @param theme       主题
     */
    public static void start(Activity activity, int requestCode, int theme) {
        start(activity, null, requestCode, theme);
//        Intent intent = new Intent(activity, CustomCaptureActivity.class);
//        intent.putExtra(KEY_CAPTURE_THEME, theme);
//        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 开始二维码扫描
     *
     * @param activity
     * @param requestCode 请求码
     * @param theme       主题
     */
    public static void start(Activity activity, View v, int requestCode, int theme) {
        Intent intent = new Intent(activity, CustomCaptureActivity.class);
        intent.putExtra(KEY_CAPTURE_THEME, theme);
        if (null != v) {
            final int[] clickViewLocation = new int[2];
            v.getLocationOnScreen(clickViewLocation);
            intent.putExtra(INTENT_EXTRA_SCENE_TRANSITION_X,
                    (int) (clickViewLocation[0] + v.getWidth() / 2));
            intent.putExtra(INTENT_EXTRA_SCENE_TRANSITION_Y,
                    (int) (clickViewLocation[1] + v.getHeight() / 2));

            if (Build.VERSION.SDK_INT >= 21) {
                final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, v,
                        "transition");
                activity.startActivityForResult(intent, requestCode, options.toBundle());
                return;
            }
        }
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 开始二维码扫描
     *
     * @param fragment
     * @param requestCode 请求码
     */
    public static void start(Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getContext(), CustomCaptureActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 开始二维码扫描
     *
     * @param activity
     * @param requestCode 请求码
     */
    public static void start(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, CustomCaptureActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    private AppCompatImageView mIvFlashLight;
    private AppCompatImageView mIvFlashLight1;
    private boolean mIsOpen;


    private View contentView;
    private Animator sceneTransition = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contentView = findViewById(android.R.id.content);
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 21 && checkCameraPermission()) {
            Intent data = getIntent();
            int x = data.getIntExtra(INTENT_EXTRA_SCENE_TRANSITION_X, -1);
            int y = data.getIntExtra(INTENT_EXTRA_SCENE_TRANSITION_Y, -1);
            if (x != -1 && y != -1) {
                contentView.setAlpha(0);
                getWindow().setBackgroundDrawable(
                        new ColorDrawable(ContextCompat.getColor(this, android.R.color.transparent)));
                OnFirstPreDraw.listen(contentView, () -> {
                        float finalRadius = (float) (Math.max(contentView.getWidth(), contentView.getHeight()));
                        final int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
                        sceneTransition = ViewAnimationUtils.createCircularReveal(contentView, x, y, 0, finalRadius);
                        sceneTransition.setDuration(duration);
                        sceneTransition.setInterpolator(new AccelerateInterpolator());
                        // TODO Here, the transition should start in a paused state, showing the first frame
                        // of the animation. Sadly, RevealAnimator doesn't seem to support this, unlike
                        // (subclasses of) ValueAnimator.
                        return false;
                });
            }
//            XToast.info(this, x+" : "+y).show();
        }

        contentView.postDelayed(() -> {
            if (sceneTransition != null) {
                contentView.setAlpha(1);
                sceneTransition.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        getWindow().setBackgroundDrawable(new ColorDrawable(
                                ContextCompat.getColor(CustomCaptureActivity.this, android.R.color.black)));
                    }
                });
                sceneTransition.start();
                sceneTransition = null;
            }
        }, 200);
    }

    protected boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    //===============================重写UI===================================//

    @Override
    protected int getCaptureLayoutId() {
        return R.layout.activity_custom_capture;
    }

    @Override
    protected void beforeCapture() {
        findViewById(R.id.iv_back).setOnClickListener(this);
        mIvFlashLight = findViewById(R.id.iv_flash_light);
        mIvFlashLight1 = findViewById(R.id.iv_flash_light1);
    }

    @Override
    protected void onCameraInitSuccess() {
        mIvFlashLight.setVisibility(View.VISIBLE);
        mIvFlashLight1.setVisibility(View.VISIBLE);

        mIsOpen = XQRCode.isFlashLightOpen();
        refreshFlashIcon();
        mIvFlashLight.setOnClickListener(this);
        mIvFlashLight1.setOnClickListener(this);
    }

    @Override
    protected void onCameraInitFailed() {
        mIvFlashLight.setVisibility(View.GONE);
        mIvFlashLight1.setVisibility(View.GONE);
    }

    private void refreshFlashIcon() {
        if (mIsOpen) {
            mIvFlashLight.setImageResource(R.drawable.ic_flash_light_on);
            mIvFlashLight1.setImageResource(R.drawable.ic_flash_light_open);
        } else {
            mIvFlashLight.setImageResource(R.drawable.ic_flash_light_off);
            mIvFlashLight1.setImageResource(R.drawable.ic_flash_light_close);
        }
    }

    private void switchFlashLight() {
        mIsOpen = !mIsOpen;
        try {
            XQRCode.switchFlashLight(mIsOpen);
            refreshFlashIcon();
        } catch (RuntimeException e) {
            e.printStackTrace();
            XToast.error(this, "设备不支持闪光灯!").show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                finish();
                break;
            case R.id.iv_flash_light:
            case R.id.iv_flash_light1:
                switchFlashLight();
                break;
            default:
                break;
        }
    }

    //===============================重写业务处理===================================//

    /**
     * 处理扫描成功（重写扫描成功，增加applink拦截）
     *
     * @param bitmap
     * @param result
     */
    @Override
    protected void handleAnalyzeSuccess(Bitmap bitmap, String result) {
//        if (isAppLink(result)) {
//            openAppLink(this, result);
//        } else
//            if (isWeb(result)) {
////            Utils.goWeb(this, result);
//        } else {
//            Intent resultIntent = new Intent();
//            Bundle bundle = new Bundle();
//            bundle.putInt(XQRCode.RESULT_TYPE, XQRCode.RESULT_SUCCESS);
//            bundle.putString(XQRCode.RESULT_DATA, result);
//            resultIntent.putExtras(bundle);
//            setResult(RESULT_OK, resultIntent);
//        }
        super.handleAnalyzeSuccess(bitmap, result);
//        finish();
    }

    /**
     * 格式：https://xuexiangjys.club/xpage/transfer?pageName=xxxxx&....
     * 例子：https://xuexiangjys.club/xpage/transfer?pageName=UserGuide&position=2
     *
     * @param url
     * @return
     */
//    private boolean isAppLink(String url) {
//        Uri uri = Uri.parse(url);
//        return uri != null
//                && APP_LINK_HOST.equals(uri.getHost())
//                && (url.startsWith("http") || url.startsWith("https"))
//                && url.contains("xpage");
//    }

    private boolean isWeb(String url) {
        return !TextUtils.isEmpty(url)
                && (url.startsWith("http") || url.startsWith("https"));
    }

//    private void openAppLink(Context context, String url) {
//        try {
//            Intent intent = new Intent(APP_LINK_ACTION);
//            intent.setData(Uri.parse(url));
//            context.startActivity(intent);
//        } catch (Exception e) {
//            XToast.error(this, "您所打开的第三方App未安装！").show();
//        }
//    }

}
