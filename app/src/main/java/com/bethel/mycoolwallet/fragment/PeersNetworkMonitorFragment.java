package com.bethel.mycoolwallet.fragment;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.adapter.PeerListAdapter;
import com.bethel.mycoolwallet.adapter.CommonEmptyStatusViewAdapter;
import com.bethel.mycoolwallet.mvvm.view_model.PeersNetworkMonitorViewModel;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.view.DividerItemDecoration;
import com.xuexiang.xui.widget.statelayout.StatusLoader;

import org.bitcoinj.core.Peer;

import java.util.List;

import butterknife.BindView;

/**
 * 数据「live data, view model」 /
 * ui展示 「列表」/
 *
 * 空页面 /
 */
public class PeersNetworkMonitorFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.peer_list)
    RecyclerView recyclerView;

    private PeersNetworkMonitorViewModel viewModel;
    private PeerListAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = getViewModel(PeersNetworkMonitorViewModel.class);
        adapter = new PeerListAdapter(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));

        showLoading();
        viewModel.peers.observe(this, peers -> {
            maybeSubmitList();
            if (Utils.isEmpty(peers)) {
                showEmpty();
                return;
            }
            showContent();
            for (Peer p: peers ) {
                viewModel.hostName.reverseLookup(p.getAddress().getAddr());
            }
        });

        viewModel.hostName.observe(this, inetAddressStringMap -> maybeSubmitList());
    }

    private void maybeSubmitList() {
        List<Peer> list = viewModel.peers.getValue();
        //  update ui
        if (null!=list) {
            adapter.submitList(PeerListAdapter.buildListItems(getContext(),
                    list, viewModel.hostName.getValue()));
        }
//        XToast.info(getContext(), "peers: "+Utils.size(list)).show();
    }

    @Override
    protected void onLoadRetry(View view) {
        super.onLoadRetry(view);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_peers_network_monitor;
    }

    @Override
    protected View getWrapView() {
        return recyclerView;
    }

    @Override
    protected StatusLoader.Adapter getStatusLoaderAdapter() {
        return new CommonEmptyStatusViewAdapter(R.string.peer_list_fragment_empty);
    }
}
