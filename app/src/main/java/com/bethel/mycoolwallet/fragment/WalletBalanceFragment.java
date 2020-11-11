package com.bethel.mycoolwallet.fragment;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.mvvm.view_model.WalletBalanceViewModel;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;

/**
 * A simple {@link Fragment} subclass.
 */
public class WalletBalanceFragment extends BaseFragment {

    private static final long BLOCKCHAIN_UPTODATE_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS;

    @BindView(R.id.wallet_balance_btc)
    TextView balanceBtcTv;

    @BindView(R.id.wallet_balance_local)
    TextView balanceLocalTv;

    @BindView(R.id.wallet_balance_warning)
    TextView warningTv;

    @BindView(R.id.wallet_balance_progress)
    TextView progressTv;

    @BindView(R.id.wallet_balance_layout)
    ViewGroup balanceLayout;

    private WalletBalanceViewModel viewModel;
    private Configuration mConfig;

    private static final Logger log = LoggerFactory.getLogger(WalletBalanceFragment.class);

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mConfig = CoolApplication.getApplication().getConfiguration();

        viewModel = getViewModel(WalletBalanceViewModel.class);
        viewModel.balanceLiveData.observe(this, coin -> {
            getActivity().invalidateOptionsMenu();
            updateView();
            // todo 通知外部事件
        });

        viewModel.chainStateLiveData.observe(this, blockChainState -> updateView());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_balance;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
//                Build.VERSION.SECURITY_PATCH.compareToIgnoreCase(Constants.SECURITY_PATCH_INSECURE_BELOW) < 0) {
//            warningTv.setVisibility(View.VISIBLE);
//            warningTv.setText(R.string.wallet_balance_fragment_insecure_device);
//        } else {
//            warningTv.setVisibility(View.GONE);
//        }
//
//        progressTv.setVisibility(View.GONE);

        // test data
//        Coin coin = Coin.parseCoin("0.0019");
//        Coin c2 = Coin.COIN;
        /**
         * shift()
         * 设置位数，以使小数点分隔符向右移动，该位数来自2014年前的标准BTC表示法。
         *
         * minDecimals()
         * 设置用于格式化的最小小数位数。
         *
         * repeatOptionalDecimals(int decimals, int repetitions)
         * 设置重复的附加小数点组以在最小小数点后使用（如果它们对表示精度有用）。
         * 例如，如果您传递{@code 1,8}，则在需要时，格式字符串最多可以包含八个小数。
         */
//        MonetaryFormat format =  new MonetaryFormat().shift(0).minDecimals(2)
//                .repeatOptionalDecimals(2, 1);
////        MonetaryFormat format2 = new MonetaryFormat(true);
//                CurrencyTools.setText(balanceBtcTv, format, c2);
    }

    private void updateView() {
        log.info(".updateView()");
        // todo ExchangeRate
        BlockChainState chainState = viewModel.chainStateLiveData.getValue();
        Coin balance = viewModel.balanceLiveData.getValue();

        boolean showProgress =false;
        if (null!= chainState && null!= chainState.bestChainDate) {
            final boolean noImpediments = null== chainState.impediments || chainState.impediments.isEmpty();
            final long chainDuration = System.currentTimeMillis() - chainState.bestChainDate.getTime();
            final boolean noNeedUptodate = chainDuration < BLOCKCHAIN_UPTODATE_THRESHOLD_MS;

//            log.info("updateView : currentTimeMillis {},  bestChainDate {}",
//                    System.currentTimeMillis(), chainState.bestChainDate.getTime());
            showProgress = (!noNeedUptodate|| chainState.replaying);
            log.info("updateView : chainDuration {}, UPTODATE__MS {}, noNeedUptodate {},  replaying {}",
                    chainDuration, BLOCKCHAIN_UPTODATE_THRESHOLD_MS, noNeedUptodate, chainState.replaying);
//            log.info("updateView (chainDuration - UPTODATE__MS) = {}",
//                    chainDuration - BLOCKCHAIN_UPTODATE_THRESHOLD_MS);

            if (showProgress) {
                final String downloading = getString(noImpediments ? R.string.blockchain_state_progress_downloading
                        : R.string.blockchain_state_progress_stalled);
                if (chainDuration < 2 * DateUtils.DAY_IN_MILLIS) {
                    final long hours = chainDuration / DateUtils.HOUR_IN_MILLIS;
                    progressTv.setText(getString(R.string.blockchain_state_progress_hours, downloading, hours));
                } else if (chainDuration < 2 * DateUtils.WEEK_IN_MILLIS) {
                    final long days = chainDuration / DateUtils.DAY_IN_MILLIS;
                    progressTv.setText(getString(R.string.blockchain_state_progress_days, downloading, days));
                } else if (chainDuration < 90 * DateUtils.DAY_IN_MILLIS) {
                    final long weeks = chainDuration / DateUtils.WEEK_IN_MILLIS;
                    progressTv.setText(getString(R.string.blockchain_state_progress_weeks, downloading, weeks));
                } else {
                    final long months = chainDuration / (30 * DateUtils.DAY_IN_MILLIS);
                    progressTv.setText(getString(R.string.blockchain_state_progress_months, downloading, months));
                }
            }
        }

        if (!showProgress) {
            balanceLayout.setVisibility(View.VISIBLE);
            if (null != balance) {
                CurrencyTools.setText(balanceBtcTv, mConfig.getFormat(), balance);
                balanceBtcTv.setVisibility(View.VISIBLE);
            } else {
                balanceBtcTv.setVisibility(View.INVISIBLE);
            }
            // todo viewBalance

            if (balance != null && balance.isGreaterThan(Constants.TOO_MUCH_BALANCE_THRESHOLD)) {
                warningTv.setVisibility(View.VISIBLE);
                warningTv.setText(R.string.wallet_balance_fragment_too_much);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    Build.VERSION.SECURITY_PATCH.compareToIgnoreCase(Constants.SECURITY_PATCH_INSECURE_BELOW) < 0) {
                warningTv.setVisibility(View.VISIBLE);
                warningTv.setText(R.string.wallet_balance_fragment_insecure_device);
            } else {
                warningTv.setVisibility(View.GONE);
            }

            progressTv.setVisibility(View.GONE);
        } else {
            balanceLayout.setVisibility(View.INVISIBLE);
            progressTv.setVisibility(View.VISIBLE);
        }

        log.info("updateView : showProgress {},  balance {}", showProgress, balance);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.wallet_balance_fragment_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        final Coin balance = viewModel.balanceLiveData.getValue();
        boolean hasSomeBalance =  balance != null && !balance.isLessThan(Constants.SOME_BALANCE_THRESHOLD);
        menu.findItem(R.id.wallet_balance_options_donate).setVisible(true);
//                .setVisible(Constants.DONATION_ADDRESS != null && hasSomeBalance);
                super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.wallet_balance_options_donate) {
            donateBtc();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void donateBtc() {
        XToast.info(getContext(), "donate some bitcoin").show();
    }
}
