package com.bethel.mycoolwallet;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.bethel.mycoolwallet.utils.Logging;
import com.xuexiang.xui.XUI;

public class CoolApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        XUI.init(this);
        XUI.debug(true);
        Logging.init(getFilesDir());
    }
}
