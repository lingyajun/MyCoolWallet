package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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
import com.bethel.mycoolwallet.activity.SendCoinsActivity;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.adapter.AddressListAdapter;
import com.bethel.mycoolwallet.adapter.CommonEmptyStatusViewAdapter;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.data.PaymentIntent;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.fragment.dialog.EditAddressBookFragment;
import com.bethel.mycoolwallet.interfaces.IToolbar;
import com.bethel.mycoolwallet.mvvm.view_model.SendingAddressesViewModel;
import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.bethel.mycoolwallet.view.DividerItemDecoration;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.statelayout.StatusLoader;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import butterknife.BindView;

/**
 *  ui展示 「列表」
 *     列表点击事件
 *    toolbar 菜单 点击事件
 */
public class SendingAddressesFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private SendingAddressesViewModel viewModel;
    private AddressListAdapter adapter;

    private static final Logger log = LoggerFactory.getLogger(SendingAddressesFragment.class);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        adapter = new AddressListAdapter(getContext());

        viewModel = getViewModel(SendingAddressesViewModel.class);
        viewModel.receivingAddresses.observe(this, addresses -> {
            viewModel.initAddressBook(addresses);
            viewModel.addressBook.observe(SendingAddressesFragment.this, list -> {
                //  list ui
                if (Utils.isEmpty(list)) {
                    showEmpty();
                } else {
                    showContent();
                }
                adapter.submitList(AddressListAdapter.buildItems(list));
            });
        });

        viewModel.wallet.observe(this, wallet -> getActivity().invalidateOptionsMenu());
        viewModel.clipBoard.observe(this, clipData -> getActivity().invalidateOptionsMenu());

        viewModel.showBitmapDialog.observe(this, new Event.Observer<Bitmap>() {
            @Override
            public void onEvent(Bitmap content) {
                BitmapFragment.show(getFragmentManager(), content);
            }
        });
        viewModel.showEditAddressBookDialog.observe(this, new Event.Observer<String>() {
            @Override
            public void onEvent(String content) {
                EditAddressBookFragment.edit(getFragmentManager(), content);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));

        showLoading();
        adapter.setOnItemClickListener((view1, position) -> {
            ListItemMenuCallback callback = new ListItemMenuCallback(position);
            getActivity().startActionMode(callback);
        });

        Activity activity = getActivity();
        if (activity instanceof IToolbar) {
            // 解决Activity的 Toolbar 的 menu点击事件无法传递到 Fragment 中
            ((IToolbar) activity).getToolbar().setOnMenuItemClickListener(item -> {
                log.info("IToolbar, OnMenuItemClick {}", item);
                return onOptionsItemSelected(item);
            });
            log.info("IToolbar, MenuItem listener");
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        inflater.inflate(R.menu.sending_addresses_fragment_options, menu);
            inflateMenu(R.menu.sending_addresses_fragment_options);
//        final PackageManager pm = getContext().getPackageManager();
//        menu.findItem(R.id.sending_addresses_options_scan)
//                .setVisible(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
//                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));
//        menu.findItem(R.id.sending_addresses_options_scan).setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
        log.info("menu created");
    }

    /** 解决Activity的 Toolbar 的 menu点击事件无法传递到 Fragment 中
     *
     * @param resId
     */
    private void inflateMenu(@MenuRes int resId) {
        Activity activity = getActivity();
        if (activity instanceof IToolbar) {
         ((IToolbar) activity).getToolbar().inflateMenu(resId);
        }
        log.info("IToolbar inflateMenu");
    }
    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.sending_addresses_options_paste)
                .setEnabled(viewModel.wallet.getValue() != null &&
                        viewModel.clipBoard.getAddressFromPrimaryClip() != null);
        menu.findItem(R.id.sending_addresses_options_scan).setVisible(false);
        log.info(" Prepare Menu");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        log.info("itemId  {}", itemId);
        if (itemId == R.id.sending_addresses_options_paste) {
            handlePasteClipboard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handlePasteClipboard() {
        final Wallet wallet = viewModel.wallet.getValue();
        final Address address = viewModel.clipBoard.getAddressFromPrimaryClip();
        log.info("Clipboard {}", address);
        if (null == address) {
            alertUser(R.string.address_book_options_paste_from_clipboard_title,
                    R.string.address_book_options_paste_from_clipboard_invalid);
            return;
        }
        if (!wallet.isAddressMine(address) ) {
            viewModel.showEditAddressBookDialog.setValue(new Event<>(address.toString()));
            return;
        }

        alertUser(R.string.address_book_options_paste_from_clipboard_title,
                R.string.address_book_options_paste_from_clipboard_own_address);
    }

    private void alertUser(int title, int message) {
       new MaterialDialog.Builder(getContext())
                .title(title)
                .content(message)
                .neutralText(R.string.button_ok)
                .show();
        log.info("alert {}", getString(title));
    }

    private final class ListItemMenuCallback implements  ActionMode.Callback {
        protected int position;

        public ListItemMenuCallback(int position) {
            this.position = position;
        }

        private AddressListAdapter.ListItem getCurrentItem() {
            if (Utils.size(adapter.getCurrentList()) > position) {
                return adapter.getCurrentList().get(position);
            }
            return null;
        }

        private String getCurrentLabel() {
            AddressListAdapter.ListItem item = getCurrentItem();
            return null!=item ? item.label : null;
        }

        private String getCurrentAddress() {
            AddressListAdapter.ListItem item = getCurrentItem();
            return null!=item ? item.address : null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            final MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.sending_addresses_context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            final String address = getCurrentAddress();
            final String label = getCurrentLabel();
            actionMode.setTitle(label != null ? label
                    : WalletUtils.formatHash(address, Constants.ADDRESS_FORMAT_GROUP_SIZE, 0));
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            final String address = getCurrentAddress();
            final String label =getCurrentLabel();
            int itemId = menuItem.getItemId();
            switch (itemId) { //  ,edit label
                case R.id.sending_addresses_context_edit:
                    viewModel.showEditAddressBookDialog.setValue(new Event<>(address));
                    break;
                case R.id.sending_addresses_context_show_qr:
                    if (TextUtils.isEmpty(address)) break;

                    final String uri = BitcoinURI.convertToBitcoinURI(Constants.NETWORK_PARAMETERS,
                            address,null, label, null);
                    AsyncTask.execute(()->{
                        Bitmap bitmap = Qr.bitmap(uri);
                        viewModel.showBitmapDialog.postValue(new Event<>(bitmap));
                    });
                    break;
                case R.id.sending_addresses_context_copy_to_clipboard:
                    handleCopyToClipboard(address);
                    break;
                case R.id.sending_addresses_context_remove:
                    viewModel.addressBookDao.delete(address);
                    break;
                case R.id.sending_addresses_context_send:
                    handleSend(address, label);
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
            viewModel.clipBoard.setValue(ClipData.newPlainText("Bitcoin address", address));
            XToast.success(getContext(),R.string.wallet_address_fragment_clipboard_msg).show();
        }
    }

    private void handleSend(final String address, final String label) {
//        XToast.info(getContext(), String.format("send coins %s : %s" ,label, address)).show();
        SendCoinsActivity.start(getActivity(), PaymentIntent.fromAddress(address, label));
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
