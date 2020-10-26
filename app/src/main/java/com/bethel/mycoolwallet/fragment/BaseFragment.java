package com.bethel.mycoolwallet.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
    protected View mRootView;
    protected Unbinder mUnbinder;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflateView(inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mRootView = view;
        mUnbinder = ButterKnife.bind(this, view);
        super.onViewCreated(view, savedInstanceState);
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

}
