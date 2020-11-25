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
import com.bethel.mycoolwallet.adapter.CommonEmptyStatusViewAdapter;
import com.bethel.mycoolwallet.mvvm.view_model.SendingAddressesViewModel;
import com.bethel.mycoolwallet.utils.Utils;
import com.xuexiang.xui.widget.statelayout.StatusLoader;

import butterknife.BindView;

/**
 *  ui展示 「列表」
 *  * todo  列表点击事件
 *  * 菜单
 */
public class SendingAddressesFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private SendingAddressesViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = getViewModel(SendingAddressesViewModel.class);
        viewModel.receivingAddresses.observe(this, addresses -> {
            viewModel.initAddressBook(addresses);
            viewModel.addressBook.observe(SendingAddressesFragment.this, list -> {
                // todo list ui
                if (Utils.isEmpty(list)) {
                    showEmpty();
                } else {
                    showContent();
                }
            });
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showLoading();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_sending_addresses;
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
