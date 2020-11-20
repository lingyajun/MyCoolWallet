package com.bethel.mycoolwallet.fragment;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.adapter.StatusSingleViewAdapter;
import com.xuexiang.xui.widget.statelayout.StatusLoader;

import butterknife.BindView;

/**
 * 数据「live data, view model」
 * ui展示 「列表」
 *
 * 空页面
 */
public class BlocksNetworkMonitorFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.block_list)
    RecyclerView recyclerView;




    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showLoading();
        view.postDelayed(()-> showEmpty(), 4000);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_blocks_network_monitor;
    }

    @Override
    protected View getWrapView() {
        return recyclerView;
    }

    @Override
    protected StatusLoader.Adapter getStatusLoaderAdapter() {
        return new StatusSingleViewAdapter();
    }
}
