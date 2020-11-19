package com.bethel.mycoolwallet.fragment;


import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.adapter.ExchangeRatesAdapter;
import com.bethel.mycoolwallet.adapter.StatusSingleViewAdapter;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.mvvm.live_data.ExchangeRateLiveData;
import com.bethel.mycoolwallet.mvvm.view_model.ExchangeRatesViewModel;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.view.DividerItemDecoration;
import com.xuexiang.xui.adapter.simple.AdapterItem;
import com.xuexiang.xui.adapter.simple.XUISimpleAdapter;
import com.xuexiang.xui.widget.popupwindow.popup.XUISimplePopup;
import com.xuexiang.xui.widget.statelayout.StatusLoader;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Coin;

import butterknife.BindView;

/**
 * 网络请求，数据解析 「live data，view model，http」
 * 加载/成功/失败。。。各种状态展示
 * RecyclerView，adapter
 *
 *  点击事件 :
 * 列表item样式，字体大一些，高度增大 /
 * mPopupMenu 样式 /
 * 点击事件 -> 更新列表 /
 * 数据列表排除重复 /
 */
public class ExchangeRatesFragment extends BaseStatusLoaderFragment {

    @BindView(R.id.toolbar)
    Toolbar toolBar;
    @BindView(R.id.exchange_rates_list)
    RecyclerView recyclerView;

    private ExchangeRatesViewModel viewModel;
    private ExchangeRatesAdapter ratesAdapter;
    private Configuration mConfig;
    private XUISimplePopup mPopupMenu;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = getViewModel(ExchangeRatesViewModel.class);
        ratesAdapter = new ExchangeRatesAdapter(getContext());
        ratesAdapter.setOnItemClickListener(itemClickListener);
        mConfig = CoolApplication.getApplication().getConfiguration();
        mConfig.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        mPopupMenu = new XUISimplePopup(getContext(), new String[]{getString(R.string.button_set_as_default)});
        mPopupMenu.create(Utils.dip2px(getContext(), 100)).setHasDivider(false);
        mPopupMenu.setPopupLeftRightMinMargin(Utils.dip2px(getContext(), -5));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initTitleBar();
        showLoading();
        recyclerView.setAdapter(ratesAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));

        viewModel.listExchangeRate.observe(this, exchangeRateBeans -> {
            final int size = null!=exchangeRateBeans? exchangeRateBeans.size(): 0;
//            XToast.info(getContext(), "ExchangeRates: "+size).show();
            if (size>0) {
                showContent();
                final String defaultCurrency = mConfig.getMyCurrencyCode();
                final Coin rateBase = mConfig.getBtcBase();
                ratesAdapter.submitList(ExchangeRatesAdapter.buildListItems(exchangeRateBeans, defaultCurrency, rateBase));
                return;
            }
            // 1. no network , retry
            if (!Utils.isNetworkConnected(getContext())) {
                showError();
                return;
            }

            // 2. empty
            showEmpty();
        });
    }

    private final ExchangeRatesAdapter.OnItemClickListener itemClickListener = new ExchangeRatesAdapter.OnItemClickListener() {
        @Override
        public void onExchangeRateMenuClick(View view, final String currencyCode) {
            // pop menu
            mPopupMenu.setOnPopupItemClickListener((adapter, item, position) -> {
                mConfig.setMyCurrencyCode(currencyCode);
            });
            mPopupMenu.showDown(view);
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if (Configuration.PREFS_KEY_CURRENCY_CODE.equals(s)) {
                // notify adapter data changed
                ratesAdapter.updateDefaultCurrencyCode(mConfig.getMyCurrencyCode());
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mConfig.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    protected void onLoadRetry(View view) {
        super.onLoadRetry(view);
        //  request
        viewModel.load();
    }

    private void initTitleBar() {
        toolBar.setTitle(R.string.exchange_rates_activity_title);
        Drawable d= Utils.zoomImage(getResources(), R.drawable.ic_navigation_back_white,
                Utils.dip2px(getContext(), 30), Utils.dip2px(getContext(), 26));
        toolBar.setNavigationIcon(d); // toolbar的左侧返回按钮
        toolBar.setNavigationOnClickListener((v)-> finishActivity());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_exchange_rates;
    }

    @Override
    protected View getWrapView() {
        return recyclerView;
    }

    @Override
    protected StatusLoader.Adapter getStatusLoaderAdapter() {
        return new StatusSingleViewAdapter();
    }
}
