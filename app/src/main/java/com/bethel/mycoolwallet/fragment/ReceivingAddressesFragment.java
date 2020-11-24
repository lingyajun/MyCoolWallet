package com.bethel.mycoolwallet.fragment;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.adapter.AddressListAdapter;
import com.bethel.mycoolwallet.adapter.CommonEmptyStatusViewAdapter;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.interfaces.OnItemClickListener;
import com.bethel.mycoolwallet.mvvm.view_model.ReceivingAddressesViewModel;
import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.bethel.mycoolwallet.view.DividerItemDecoration;
import com.xuexiang.xui.widget.statelayout.StatusLoader;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.uri.BitcoinURI;

import java.util.Map;

import butterknife.BindView;

/**
 * 您的比特币地址.
 *
 * 数据「live data, view model」/
 * ui展示 「列表」
 * todo  列表点击事件
 * 菜单
 *
 * 空页面 /
 */
public class ReceivingAddressesFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private ReceivingAddressesViewModel viewModel;
    private AddressListAdapter adapter;

    private AddressBookDao addressBookDao;
    private ClipboardManager clipboardManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = getViewModel(ReceivingAddressesViewModel.class);
        adapter = new AddressListAdapter(getContext());
        this.addressBookDao = AppDatabase.getInstance(getContext()).addressBookDao();
        this.clipboardManager =(ClipboardManager)  getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));

        showLoading();
        observeData();
        adapter.setOnItemClickListener((view1, position) -> {
            MenuCallback menuCallback = new MenuCallback(position);
            getActivity().startActionMode(menuCallback);
        });
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

        viewModel.showBitmapDialog.observe(this, new Event.Observer<Bitmap>() {
            @Override
            public void onEvent(Bitmap content) {
                BitmapFragment.show(getFragmentManager(), content);
            }
        });
    }

    private void checkAndShowList() {
        if (Utils.isEmpty(adapter.getCurrentList())) {
            showEmpty();
        } else {
            showContent();
        }
    }

    private final class MenuCallback implements  ActionMode.Callback {
        protected int position;

        public MenuCallback(int position) {
            this.position = position;
        }

        private String getCurrentAddress() {
            if (Utils.size(adapter.getCurrentList()) > position) {
                return adapter.getCurrentList().get(position).address;
            }
            return null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            final MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.wallet_addresses_context, menu);
            menu.findItem(R.id.wallet_addresses_context_browse).setVisible(Constants.ENABLE_BROWSE);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            final String address = getCurrentAddress();
            final String label = addressBookDao.resolveLabel(address);
            actionMode.setTitle(label != null ? label
                    : WalletUtils.formatHash(address, Constants.ADDRESS_FORMAT_GROUP_SIZE, 0));
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            final String address = getCurrentAddress();
            int itemId = menuItem.getItemId();
            switch (itemId) { // todo ,edit label
                case R.id.wallet_addresses_context_edit:break;
                case R.id.wallet_addresses_context_show_qr:
                    if (TextUtils.isEmpty(address)) break;
                    final String label = viewModel.ownName.getValue();
                    final String uri = BitcoinURI.convertToBitcoinURI(Constants.NETWORK_PARAMETERS,
                            address,null, label, null);
                    AsyncTask.execute(()->{
                        Bitmap bitmap = Qr.bitmap(uri);
                        viewModel.showBitmapDialog.postValue(new Event<>(bitmap));
                    });
                    break;
                case R.id.wallet_addresses_context_copy_to_clipboard:
                    handleCopyToClipboard(address);
                    break;
                case R.id.wallet_addresses_context_browse:
//                    https://www.blockchain.com/btc/address/18cBEMRxXHqzWWCxZNtU91F5sbUNKhL5PX
                    String url = Commons.BLOCK_CHAIN_VIEW+ "address/"+ address;
                    WebActivity.start(getContext(), url);
                    break;
                default: return false;
            }
            actionMode.finish();
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
        }

        private void handleCopyToClipboard(final String address) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("Bitcoin address", address));
            XToast.success(getContext(),R.string.wallet_address_fragment_clipboard_msg).show();
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
