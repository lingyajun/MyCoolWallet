package com.bethel.mycoolwallet.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public class BaseDialogFragment extends DialogFragment {
    /**
     * 根布局
     */
    protected View mRootView = null;
    protected Unbinder mUnbinder = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    protected View createAndBindDialogView(int layoutId) {
        mRootView = LayoutInflater.from(getActivity()).inflate(layoutId, null);
        mUnbinder = ButterKnife.bind(this, mRootView);
        return mRootView;
    }


    @Override
    public void onDestroyView() {
        if (null!=mUnbinder) {
            mUnbinder.unbind();
        }
        super.onDestroyView();
    }

    protected  void runOnUIthread(Runnable task) {
        runOnUIthread(task, 0);
    }
    protected  void runOnUIthread(Runnable task, long delayMillis) {
        prepareMainHandler();
        if (delayMillis > 0) {
            mainHandler.postDelayed(task, delayMillis);
        } else {
            mainHandler.post(task);
        }
    }

    protected Handler mainHandler = null;
    private synchronized void prepareMainHandler() {
        if (null == mainHandler) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
    }

}
