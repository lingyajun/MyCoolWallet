package com.bethel.mycoolwallet.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.adapter.TransactionListAdapter;
import com.bethel.mycoolwallet.data.tx_list.OnTxItemClickListener;
import com.bethel.mycoolwallet.data.tx_list.TransactionListItemAnimator;
import com.bethel.mycoolwallet.mvvm.view_model.WalletTransactionsViewModel;
import com.bethel.mycoolwallet.view.StickToTopLinearLayoutManager;
import com.bethel.mycoolwallet.view.TransactionsItemDecoration;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Sha256Hash;

import butterknife.BindView;

/**
 * 交易{Transaction}列表.
 *-----------------------------------
 *数据「live data, view model」
 *  * ui展示 「列表」
 *  *
 *  * 区块链浏览器
 *  * 列表：交易记录
 *  *  数据库address标签存储
 *  *
 *  * 空页面
 */
public class WalletTransactionsFragment extends BaseFragment {
    @BindView(R.id.wallet_transactions_group)
    ViewAnimator viewAnimator;

    @BindView(R.id.wallet_transactions_empty)
    TextView emptyTv;

    @BindView(R.id.wallet_transactions_list)
    RecyclerView recyclerView;

    private TransactionListAdapter mAdapter;
    private MenuItem filterMenuItem;
    private WalletTransactionsViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = getViewModel(WalletTransactionsViewModel.class);
        mAdapter = new TransactionListAdapter(getContext(),
                CoolApplication.getApplication().maxConnectedPeers(), itemClickListener);

        observeData();
    }

    private void observeData() {
        viewModel.list.observe(this, iListItems -> {
            viewAnimator.setDisplayedChild(1);
            mAdapter.submitList(iListItems);
//     todo       activityViewModel.transactionsLoadingFinished();
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
                XToast.info(getContext(), "received tx");
                break;
            case R.id.wallet_transactions_options_filter_sent:
                XToast.info(getContext(), "sent tx");
                break;
                default: return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_transactions;
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
}
