package com.bethel.mycoolwallet.fragment;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.WebActivity;
import com.bethel.mycoolwallet.adapter.CommonEmptyStatusViewAdapter;
import com.bethel.mycoolwallet.adapter.TransactionListAdapter;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.data.tx_list.OnTxItemClickListener;
import com.bethel.mycoolwallet.data.tx_list.TransactionDirection;
import com.bethel.mycoolwallet.data.tx_list.TransactionListItemAnimator;
import com.bethel.mycoolwallet.data.tx_list.TransactionWarningType;
import com.bethel.mycoolwallet.fragment.dialog.EditAddressBookFragment;
import com.bethel.mycoolwallet.fragment.dialog.RaiseFeeDialogFragment;
import com.bethel.mycoolwallet.fragment.dialog.ReportIssueDialogFragment;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.mvvm.view_model.MainActivityViewModel;
import com.bethel.mycoolwallet.mvvm.view_model.WalletTransactionsViewModel;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.view.StickToTopLinearLayoutManager;
import com.bethel.mycoolwallet.view.TransactionPopupMenu;
import com.bethel.mycoolwallet.view.TransactionsItemDecoration;
import com.xuexiang.xui.widget.statelayout.StatusLoader;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;

import butterknife.BindView;

/**
 * 交易{Transaction}列表.
 *-----------------------------------
 *数据「live data, view model」 /
 *  * ui展示 「列表」
 *  * l列表点击事件  itemClickListener
 *  * 列表：交易记录  ?
 *
 *      menu /
 *  *
 *  * 空页面 /
 */
public class WalletTransactionsFragment extends BaseStatusLoaderFragment {
//    @BindView(R.id.wallet_transactions_group)
//    ViewAnimator viewAnimator;
//    @BindView(R.id.wallet_transactions_empty)
//    TextView emptyTv;

    @BindView(R.id.wallet_transactions_list)
    RecyclerView recyclerView;

    private TransactionListAdapter mAdapter;
    private MenuItem filterMenuItem;
    private WalletTransactionsViewModel viewModel;
    private MainActivityViewModel activityViewModel;
    private CommonEmptyStatusViewAdapter emptyStatus;

    private Configuration mConfig;
    private DevicePolicyManager devicePolicyManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        CoolApplication application = CoolApplication.getApplication();
        viewModel = getViewModel(WalletTransactionsViewModel.class);
        activityViewModel = getActivityViewModel(MainActivityViewModel.class);
        mAdapter = new TransactionListAdapter(getContext());
        emptyStatus = new CommonEmptyStatusViewAdapter(R.string.wallet_transactions_fragment_empty_text_howto);

        mConfig = application.getConfiguration();
        devicePolicyManager = (DevicePolicyManager) application.getSystemService(Context.DEVICE_POLICY_SERVICE);
        observeData();
    }

    private void observeData() {
        viewModel.transactions.observe(this , transactions -> {
            if (!Utils.isEmpty(transactions)) {
                showContent();
                return;
            }
            // empty  showEmpty();
            final TransactionDirection direction = viewModel.direction.getValue();
            final SpannableStringBuilder message = new SpannableStringBuilder();
            message.append(getString(direction== TransactionDirection.SENT ?
                    R.string.wallet_transactions_fragment_empty_text_sent
                    : R.string.wallet_transactions_fragment_empty_text_received));

            message.setSpan(new StyleSpan(Typeface.BOLD), 0, message.length(),
                    SpannableStringBuilder.SPAN_POINT_MARK);

            if (direction != TransactionDirection.SENT) {
                message.append("\n\n").append(getString(R.string.wallet_transactions_fragment_empty_text_howto));
            }

            final TransactionWarningType warningType = viewModel.warning.getValue();
            if (TransactionWarningType.BACKUP == warningType) {
                final int start = message.length();
                message.append("\n\n").append(getString(R.string.wallet_transactions_fragment_empty_remind_backup));
                message.setSpan(new StyleSpan(Typeface.BOLD), start, message.length(),
                        SpannableStringBuilder.SPAN_POINT_MARK);
            }
            showEmpty(message.toString());
        });

        viewModel.list.observe(this, iListItems -> {
            mAdapter.submitList(iListItems);
            activityViewModel.transactionsLoadingFinished();
        });

        viewModel.direction.observe(this, transactionDirection -> getActivity().invalidateOptionsMenu());

        viewModel.showBitmapDialog.observe(this, new Event.Observer<Bitmap>(){
            @Override
            public void onEvent( Bitmap content) {
                if (null == content) return;
                BitmapFragment.show(getFragmentManager(), content);
            }
        });

        viewModel.showEditAddressBookDialog.observe(this, new Event.Observer<Address>(){
            @Override
            public void onEvent(Address content) {
                if (null == content) return;
                EditAddressBookFragment.edit(getFragmentManager(), content.toString());
            }
        });
        viewModel.showReportIssueDialog.observe(this, new Event.Observer<String>(){
            @Override
            public void onEvent(String content) {
                ReportIssueDialogFragment.show(getFragmentManager(), R.string.report_issue_dialog_title_transaction,
                        R.string.report_issue_dialog_message_issue, Constants.REPORT_SUBJECT_ISSUE, content);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StickToTopLinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new TransactionListItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new TransactionsItemDecoration(getContext()));
        mAdapter.setOnItemClickListener(itemClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.warning.setValue(getWarning());
    }

    private TransactionWarningType getWarning() {
        if (mConfig.remindBackup()) {
            return TransactionWarningType.BACKUP;
        }

        final int storage = devicePolicyManager.getStorageEncryptionStatus();
        switch (storage) { // 提醒用户 —— 加密设备，保护数据安全
            case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY:
                return TransactionWarningType.STORAGE_ENCRYPTION;
        }
        return null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.wallet_transactions_fragment_options, menu);
        filterMenuItem = menu.findItem(R.id.wallet_transactions_options_filter);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        TransactionDirection txDirection = viewModel.direction.getValue();
        if (null== txDirection) txDirection = TransactionDirection.ALL;
        switch (txDirection) {
            case ALL:
                menu.findItem(R.id.wallet_transactions_options_filter_all).setChecked(true);
                break;
            case SENT:
                menu.findItem(R.id.wallet_transactions_options_filter_sent).setChecked(true);
                break;
            case RECEIVED:
                menu.findItem(R.id.wallet_transactions_options_filter_received).setChecked(true);
                break;
        }
        maybeSetFilterMenuItemIcon(txDirection.getIconId());
        super.onPrepareOptionsMenu(menu);
    }

    private void maybeSetFilterMenuItemIcon(int iconId) {
        // Older Android versions can't deal with width and height in XML layer-list items.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            filterMenuItem.setIcon(iconId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        TransactionDirection direction;
        switch (id) {
            case R.id.wallet_transactions_options_filter_all:
                direction = TransactionDirection.ALL;
                break;
            case R.id.wallet_transactions_options_filter_received:
                direction = TransactionDirection.RECEIVED;
                break;
            case R.id.wallet_transactions_options_filter_sent:
                direction = TransactionDirection.SENT;
                break;
                default: return super.onOptionsItemSelected(item);
        }
        maybeSetFilterMenuItemIcon(direction.getIconId());
        viewModel.direction.setValue(direction);
        return true;
    }

    private OnTxItemClickListener itemClickListener = new OnTxItemClickListener() {
        @Override
        public void onTransactionClick(View view, Sha256Hash transactionHash) {
            viewModel.selectedTransaction.setValue(transactionHash);
        }

        @Override
        public void onTransactionMenuClick(View view, Sha256Hash transactionHash) {
            final Wallet wallet = viewModel.wallet.getValue();
            if (null==wallet || null == transactionHash) return;

            TransactionPopupMenu menu = new TransactionPopupMenu(getActivity(), view,
                    wallet, transactionHash, viewModel.addressBookDao);
            menu.setOnMenuItemClickListener(new TransactionPopupMenu.OnTxMenuItemClickListener(){
                @Override
                public void onEditAddress(Address address) {
                    viewModel.showEditAddressBookDialog.setValue(new Event<>(address));
                }

                @Override
                public void onShowQr(Bitmap bitmap) {
                    viewModel.showBitmapDialog.setValue(new Event<>(bitmap));
                }

                @Override
                public void onRaiseFee(Transaction tx) {
                      RaiseFeeDialogFragment.show(getFragmentManager(), tx);
                }

                @Override
                public void onReportIssue(String issue) {
                    viewModel.showReportIssueDialog.setValue(new Event<>(issue));
                }

                @Override
                public void onTxExplorer(String url) {
                    WebActivity.start(getContext(), url, getString(R.string.transaction_record));
                }
            });
            menu.show();
        }

        @Override
        public void onWarningClick(View view) {
            TransactionWarningType type = getWarning();
            if (TransactionWarningType.BACKUP == type) {
                // 提醒备份钱包文件
                activityViewModel.showBackupWalletDialog.setValue(Event.simple());
                return;
            }
            if (TransactionWarningType.STORAGE_ENCRYPTION == type) {
                go2SecuritySettings();
            }
        }
    };

    private void go2SecuritySettings() {
        Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        startActivity(intent);
    }

    private void showEmpty(String msg) {
        showEmpty();
        emptyStatus.showEmptyMessage(msg);
    }

    @Override
    protected View getWrapView() {
        return recyclerView;
    }

    @Override
    protected StatusLoader.Adapter getStatusLoaderAdapter() {
        return emptyStatus;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_transactions_x;
    }
}
