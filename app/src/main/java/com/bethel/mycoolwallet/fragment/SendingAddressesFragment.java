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
import com.xuexiang.xui.widget.statelayout.StatusLoader;

import butterknife.BindView;

/**
 * A simple {@link Fragment} subclass.
 */
public class SendingAddressesFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    public SendingAddressesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showEmpty();
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