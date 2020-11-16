package com.bethel.mycoolwallet.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * 基类 {@link Fragment} .
 */
public abstract class BaseFragment extends Fragment {

    /**
     * 根布局
     */
    protected View mRootView = null;
    protected Unbinder mUnbinder = null;

//    protected Handler mainHandler = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflateView(inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView = view;
        mUnbinder = ButterKnife.bind(this, view);
    }

    @Override
    public void onDestroyView() {
        if (null!=mUnbinder) {
            mUnbinder.unbind();
        }
        super.onDestroyView();
    }

    /**
     * 加载控件
     *
     * @param inflater
     * @param container
     * @return
     */
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    protected abstract int getLayoutId();

    protected final void runOnUiThread(Runnable task) {
        getActivity().runOnUiThread(task);
    }

    protected  <T extends ViewModel> T getViewModel(@NonNull Class<T> modelClass) {
        return ViewModelProviders.of(this).get(modelClass);
    }
}
