package com.bethel.mycoolwallet.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.fragment.preference.AboutFragment;
import com.bethel.mycoolwallet.fragment.preference.DiagnosticsFragment;
import com.bethel.mycoolwallet.fragment.preference.SettingsFragment;
import com.bethel.mycoolwallet.utils.Utils;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    private Toolbar titleBar;

    public static void start(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTitleBar();
    }

    /**
     * PreferenceActivity+ToolBar(限Android 5.0以上)
     * https://www.chenlongfei.cn/2015/08/07/preference-activity-toolbar-usage/
     */
    private void initTitleBar() {
        // 找到Activity根布局
        ViewGroup rootView = findViewById(android.R.id.content);
        //获取根布局子View
        View content = rootView.getChildAt(0);
        //加载自定义布局文件
        LinearLayout toolbarLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.preference_layout, null);
        //移除根布局所有子view
        rootView.removeAllViews();
        //注意这里一要将前面移除的子View添加到我们自定义布局文件中，否则PreferenceActivity中的Header将不会显示
        toolbarLayout.addView(content);
        //将包含Toolbar的自定义布局添加到根布局中
        rootView.addView(toolbarLayout);
        //获取toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.preferences_activity_title);
        Drawable d= Utils.zoomImage(getResources(), R.drawable.ic_navigation_back_white,
                Utils.dip2px(this, 30), Utils.dip2px(this, 26));
//     getResources().getDrawable(R.drawable.ic_navigation_back_white);
        toolbar.setNavigationIcon(d); // toolbar的左侧返回按钮
        toolbar.setNavigationOnClickListener((v)-> onBackPressed());
        titleBar = toolbar;
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_settings_activity, target);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        CharSequence text =  header.getTitle(getResources());
        updateTitleText(text);
        super.onHeaderClick(header, position);
    }

    private void updateTitleText(CharSequence text) {
        titleBar.setTitle(text);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!isFinishing()) {
            updateTitleText(getString(R.string.preferences_activity_title));
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName)
                || DiagnosticsFragment.class.getName().equals(fragmentName)
                || AboutFragment.class.getName().equals(fragmentName);
    }

}
