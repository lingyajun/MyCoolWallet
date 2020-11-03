package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.bethel.mycoolwallet.CoolApplication;

public class BaseViewModel extends AndroidViewModel {
    protected final CoolApplication app;
    public BaseViewModel(@NonNull Application application) {
        super( application);
        this.app = (CoolApplication) application;
    }

    /**
     * 基础方法，异步任务
     * */
    protected void executeAsyncTask(Runnable task) {
        AsyncTask.execute(task);
    }
}
