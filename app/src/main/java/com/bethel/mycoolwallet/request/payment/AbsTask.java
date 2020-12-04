package com.bethel.mycoolwallet.request.payment;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.bethel.mycoolwallet.helper.CoolThreadPool;

public abstract class AbsTask implements Runnable {
    protected final Handler callbackHandler; // main thread

    protected AbsTask() {
        callbackHandler = new Handler(Looper.myLooper());
    }

    public  void executeAsyncTask() {
        CoolThreadPool.execute(this);
//        AsyncTask.execute(this);
//        new Thread(this).start();
    }


    protected  void runOnCallbackThread(Runnable runnable) {
        if (null!= callbackHandler) callbackHandler.post(runnable);
    }
}
