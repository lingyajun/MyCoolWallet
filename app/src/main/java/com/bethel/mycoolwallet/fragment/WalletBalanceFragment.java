package com.bethel.mycoolwallet.fragment;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;

import butterknife.BindView;

/**
 * A simple {@link Fragment} subclass.
 */
public class WalletBalanceFragment extends BaseFragment {

    @BindView(R.id.wallet_balance_btc)
    TextView balanceBtcTv;

    @BindView(R.id.wallet_balance_local)
    TextView balanceLocalTv;

    @BindView(R.id.wallet_balance_warning)
    TextView warningTv;

    @BindView(R.id.wallet_balance_progress)
    TextView progressTv;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_balance;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Build.VERSION.SECURITY_PATCH.compareToIgnoreCase(Constants.SECURITY_PATCH_INSECURE_BELOW) < 0) {
            warningTv.setVisibility(View.VISIBLE);
            warningTv.setText(R.string.wallet_balance_fragment_insecure_device);
        } else {
            warningTv.setVisibility(View.GONE);
        }

        progressTv.setVisibility(View.GONE);

        // test data
        Coin coin = Coin.parseCoin("0.0019");
        Coin c2 = Coin.COIN;
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
        MonetaryFormat format =  new MonetaryFormat().shift(0).minDecimals(2)
                .repeatOptionalDecimals(2, 1);
//        MonetaryFormat format2 = new MonetaryFormat(true);
                CurrencyTools.setText(balanceBtcTv, format, c2);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.wallet_balance_fragment_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        // todo 
        boolean hasSomeBalance = true;
        menu.findItem(R.id.wallet_balance_options_donate)
                .setVisible(Constants.DONATION_ADDRESS != null && hasSomeBalance);
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
        XToast.info(getContext(), "donate some bitcoin");
    }
}
