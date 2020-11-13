package com.bethel.mycoolwallet.mvvm.live_data;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TimeUtils;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.http.HttpUtil;
import com.bethel.mycoolwallet.http.IRequestCallback;

import org.json.JSONException;

import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class ExchangeRateLiveData extends LiveData<ExchangeRateBean> {
    private final AtomicLong requestTime = new AtomicLong(0);
    private Configuration mConfig;
    private final String mCurrencyCode;

    public ExchangeRateLiveData(String currencyCode) {
        mConfig = CoolApplication.getApplication().getConfiguration();
        mCurrencyCode = currencyCode;
    }

    @Override
    protected void onActive() {
        load();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
    }

    public void load() {
        String currencyCode = getMyCurrencyCode();
        if (TextUtils.isEmpty(currencyCode)) {
            setValue(null);
            return;
        }
        long duration = System.currentTimeMillis() - requestTime.get();
        if (duration < (DateUtils.MINUTE_IN_MILLIS * 20)) {
            ExchangeRateBean rateBean = loadFromCache();
            if (null!=rateBean && rateBean.rate !=null) {
                postValue(rateBean);
                return;
            }
        }
        AsyncTask.execute(()->
                HttpUtil.requestExchangeRate(currencyCode, callback));
    }

    private ExchangeRateBean loadFromCache() {
        String currencyCode = getMyCurrencyCode();
        String json = mConfig.getCacheExchangeRateRequest(currencyCode);
        if (TextUtils.isEmpty(json)) return null;
        ExchangeRateBean rateBean = parseExchangeRate(currencyCode, json);
        return rateBean;
    }

    private ExchangeRateBean parseExchangeRate(String currencyCode, String json) {
        ExchangeRateBean rateBean = null;
        try {
            rateBean = HttpUtil.parseSingleCurrencyExchangeRate(currencyCode, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return rateBean;
    }

    private void cacheRequest(String currencyCode, String json) {
        mConfig.cacheExchangeRateRequest(currencyCode , json);
    }

    private final IRequestCallback callback = new IRequestCallback() {
        @Override
        public void onSuccess(String result) {
            if (TextUtils.isEmpty(result)) {
                ExchangeRateBean rateBean = loadFromCache();
                postValue(rateBean);
                return;
            }
            String currencyCode = getMyCurrencyCode();
            ExchangeRateBean rateBean = parseExchangeRate(currencyCode, result);
            postValue(rateBean);

            if (null!=rateBean) {
                requestTime.set(System.currentTimeMillis());
                cacheRequest(currencyCode, result);
            }
        }

        @Override
        public void onFailed(String message) {
            ExchangeRateBean rateBean = loadFromCache();
            postValue(rateBean);
        }
    };

    public String getMyCurrencyCode() {
        String code = mCurrencyCode;
        if (TextUtils.isEmpty(code)) {
            code = mConfig.getMyCurrencyCode();
        }
        if (TextUtils.isEmpty(code)) {
            code = defaultCurrencyCode();
        }
        return code;
    }

    private String defaultCurrencyCode() {
        return Currency.getInstance(Locale.getDefault()).getCurrencyCode();
    }
}
