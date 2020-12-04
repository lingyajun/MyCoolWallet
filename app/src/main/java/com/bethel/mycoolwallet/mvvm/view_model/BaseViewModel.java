package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.helper.CoolThreadPool;

public class BaseViewModel extends AndroidViewModel {
    protected final CoolApplication application;
    public BaseViewModel(@NonNull Application app) {
        super(app);
        this.application = (CoolApplication) app;
    }

    /**
     * 基础方法，异步任务
     * */
    protected void executeAsyncTask(Runnable task) {
        CoolThreadPool.execute(task);
//        AsyncTask.execute(task);
    }
}
