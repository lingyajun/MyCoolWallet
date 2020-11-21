package com.bethel.mycoolwallet.fragment;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.adapter.BlockListAdapter;
import com.bethel.mycoolwallet.adapter.StatusSingleViewAdapter;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.mvvm.view_model.BlocksNetworkMonitorViewModel;
import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.view.DividerItemDecoration;
import com.xuexiang.xui.widget.popupwindow.popup.XUISimplePopup;
import com.xuexiang.xui.widget.statelayout.StatusLoader;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import butterknife.BindView;

/**
 * 数据「live data, view model」 /
 * ui展示 「列表」
 *
 * 区块链浏览器 /
 * 列表：交易记录 /
 * todo 数据库address标签存储
 *
 * 空页面 /
 */
public class BlocksNetworkMonitorFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.block_list)
    RecyclerView recyclerView;

    private BlocksNetworkMonitorViewModel viewModel;
    private BlockListAdapter adapter;
    private Configuration mConfig;
    private XUISimplePopup mPopupMenu;

    private static final Logger log = LoggerFactory.getLogger(BlocksNetworkMonitorFragment.class);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = getViewModel(BlocksNetworkMonitorViewModel.class);
        adapter = new BlockListAdapter(getContext());
        mConfig = CoolApplication.getApplication().getConfiguration();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));

        mPopupMenu = new XUISimplePopup(getContext(), new String[]{getString(R.string.action_browse)});
        mPopupMenu.create(Utils.dip2px(getContext(), 70)).setHasDivider(false);
        mPopupMenu.setPopupLeftRightMinMargin(Utils.dip2px(getContext(), -5));

        adapter.setOnItemClickListener((v, blockHash) -> {
            mPopupMenu.setOnPopupItemClickListener((adapter, item, position) -> {
                final String url = Commons.BLOCK_CHAIN_VIEW + "block/" + blockHash;
                WebActivity.start(getActivity(),url);
                log.info(url);
            });
            mPopupMenu.showDown(v);
        });

        showLoading();
//        view.postDelayed(()-> showEmpty(), 4000);

        observeData();
    }

    private void observeData() {
        viewModel.blocks.observe(this, storedBlocks -> {
            maybeSubmitList();
            if (!Utils.isEmpty(storedBlocks)) {
                showContent();
            } else {
                showEmpty();
            }
            viewModel.transactions.loadTransactions();
        });
        viewModel.transactions.observe(this, transactions -> maybeSubmitList());
        viewModel.wallet.observe(this, wallet -> maybeSubmitList());
        viewModel.timeTick.observe(this, date -> maybeSubmitList());
    }

    private void maybeSubmitList() {
        final List<StoredBlock> list = viewModel.blocks.getValue();
        if (null!=list) {
            adapter.submitList(BlockListAdapter.buildListItems(getContext(), list,
                    viewModel.timeTick.getValue(), mConfig.getFormat(),
                    viewModel.transactions.getValue(), viewModel.wallet.getValue()));
        }
//        XToast.info(getContext(), "blocks:  "+ Utils.size(list)).show();
        log.info("blocks: {} ", Utils.size(list));
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
