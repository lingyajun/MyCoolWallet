package com.bethel.mycoolwallet.fragment;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.adapter.CommonEmptyStatusViewAdapter;
import com.bethel.mycoolwallet.adapter.TransactionListAdapter;
import com.bethel.mycoolwallet.data.tx_list.OnTxItemClickListener;
import com.bethel.mycoolwallet.data.tx_list.TransactionDirection;
import com.bethel.mycoolwallet.data.tx_list.TransactionListItemAnimator;
import com.bethel.mycoolwallet.data.tx_list.TransactionWarningType;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.mvvm.view_model.MainActivityViewModel;
import com.bethel.mycoolwallet.mvvm.view_model.WalletTransactionsViewModel;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.view.StickToTopLinearLayoutManager;
import com.bethel.mycoolwallet.view.TransactionsItemDecoration;
import com.xuexiang.xui.widget.statelayout.StatusLoader;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Sha256Hash;

import butterknife.BindView;

/**
 * 交易{Transaction}列表.
 *-----------------------------------
 *数据「live data, view model」 /
 *  * ui展示 「列表」
 *  * l列表点击事件  itemClickListener
 *  * 列表：交易记录  ?
 *
 *      menu
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
        activityViewModel = ViewModelProviders.of(getActivity()).get(MainActivityViewModel.class);
        mAdapter = new TransactionListAdapter(getContext(), application.maxConnectedPeers(), itemClickListener);
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

 // todo observe
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StickToTopLinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new TransactionListItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new TransactionsItemDecoration(getContext()));
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
        // todo
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // todo
        final int id = item.getItemId();
        switch (id) {
            case R.id.wallet_transactions_options_filter_all:
                break;
            case R.id.wallet_transactions_options_filter_received:
                XToast.info(getContext(), "received tx").show();
                break;
            case R.id.wallet_transactions_options_filter_sent:
                XToast.info(getContext(), "sent tx").show();
                break;
                default: return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private OnTxItemClickListener itemClickListener = new OnTxItemClickListener() {
        @Override
        public void onTransactionClick(View view, Sha256Hash transactionHash) {
            // todo
        }

        @Override
        public void onTransactionMenuClick(View view, Sha256Hash transactionHash) {

        }

        @Override
        public void onWarningClick(View view) {

        }
    };

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
