package com.bethel.mycoolwallet.mvvm.live_data.exchange_rate;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.helper.CoolThreadPool;
import com.bethel.mycoolwallet.request.HttpUtil;
import com.bethel.mycoolwallet.request.IRequestCallback;

import org.json.JSONException;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ExchangeRateLiveData extends LiveData<ExchangeRateBean> {
//    private final AtomicLong requestTime = new AtomicLong(0);
    private final Map<String , Long> requestTime = new HashMap<>();
    protected Configuration mConfig;
    protected final String mCurrencyCode;

    public ExchangeRateLiveData(String currencyCode) {
        mConfig = CoolApplication.getApplication().getConfiguration();
        mCurrencyCode = currencyCode;
    }

    @Override
    protected void onActive() {
        load();
    }

    public void load() {
        String currencyCode = getMyCurrencyCode();
        load(currencyCode);
    }
    public void load(String currencyCode) {
        if (TextUtils.isEmpty(currencyCode)) {
            setValue(null);
            return;
        }

        final  Long last = requestTime.get(currencyCode);
        final long duration = System.currentTimeMillis() - (null!=last? last : 0) ;
        if (duration < (DateUtils.MINUTE_IN_MILLIS * 20)) {
            ExchangeRateBean rateBean = loadFromCache();
            if (null!=rateBean && rateBean.rate !=null) {
                postValue(rateBean);
                return;
            }
        }
//        AsyncTask.execute(()->
        CoolThreadPool.execute(()->
                HttpUtil.requestExchangeRate(currencyCode, callback));
    }

    private ExchangeRateBean loadFromCache() {
        String currencyCode = getMyCurrencyCode();
        String json = mConfig.getCacheExchangeRateRequest(currencyCode);
        if (TextUtils.isEmpty(json)) return null;
        return parseExchangeRate(currencyCode, json);
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
                requestTime.put(currencyCode, System.currentTimeMillis());
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

    public static String defaultCurrencyCode() {
        return Currency.getInstance(Locale.getDefault()).getCurrencyCode();
    }
}
