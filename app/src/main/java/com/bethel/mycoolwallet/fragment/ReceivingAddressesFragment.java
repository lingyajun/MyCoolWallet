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

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.adapter.AddressListAdapter;
import com.bethel.mycoolwallet.adapter.CommonEmptyStatusViewAdapter;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.mvvm.view_model.ReceivingAddressesViewModel;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.view.DividerItemDecoration;
import com.xuexiang.xui.widget.statelayout.StatusLoader;

import java.util.Map;

import butterknife.BindView;

/**
 * 您的比特币地址.
 *
 * 数据「live data, view model」/
 * ui展示 「列表」
 * todo 列表点击事件
 * 菜单
 *
 * 空页面 /
 */
public class ReceivingAddressesFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private ReceivingAddressesViewModel viewModel;
    private AddressListAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = getViewModel(ReceivingAddressesViewModel.class);
        adapter = new AddressListAdapter(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));

        showLoading();
        observeData();
    }

    private void observeData() {
        viewModel.issuedReceiveAddresses.observe(this, addresses -> {
            adapter.replaceDerivedAddresses(addresses);
            checkAndShowList();
        });
        viewModel.importedAddresses.observe(this, addresses -> {
            adapter.replaceRandomAddresses(addresses);
            checkAndShowList();
        });
        viewModel.wallet.observe(this, wallet -> adapter.updateWallet(wallet));
        viewModel.addressBook.observe(this, list -> {
            Map<String , AddressBook> map = AddressBook.asMap(list);
            adapter.updateAddressBook(map);
        });
        viewModel.ownName.observe(this, s -> adapter.notifyDataSetChanged());
    }

    private void checkAndShowList() {
        if (Utils.isEmpty(adapter.getCurrentList())) {
            showEmpty();
        } else {
            showContent();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_receiving_addresses;
    }

    @Override
    protected View getWrapView() {
        return recyclerView;
    }

    @Override
    protected StatusLoader.Adapter getStatusLoaderAdapter() {
        return new CommonEmptyStatusViewAdapter(R.string.address_book_empty_text);
    }
}
