package com.bethel.mycoolwallet.mvvm.live_data.exchange_rate;

import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.helper.CoolThreadPool;
import com.bethel.mycoolwallet.request.HttpUtil;
import com.bethel.mycoolwallet.request.IRequestCallback;

import org.json.JSONException;

import java.util.List;

public class ExchangeRateListLiveData extends LiveData<List<ExchangeRateBean>> {
    private Configuration mConfig;

    public ExchangeRateListLiveData() {
        mConfig = CoolApplication.getApplication().getConfiguration();
    }

    @Override
    protected void onActive() {
        load();
    }

    public void load() {
        CoolThreadPool.execute(()-> HttpUtil.requestExchangeRateList(callback));
//        AsyncTask.execute(()-> HttpUtil.requestExchangeRateList(callback));
    }

    private final IRequestCallback callback = new IRequestCallback() {
        @Override
        public void onSuccess(String result) {
            if (TextUtils.isEmpty(result)) {
                List<ExchangeRateBean>  list = loadFromCache();
                postValue(list);
                return;
            }

            List<ExchangeRateBean>  list  = parseExchangeRateList( result);
            postValue(list);

            if (null!=list && !list.isEmpty()) {
                cacheRequest( result);
            }
        }

        @Override
        public void onFailed(String message) {
            List<ExchangeRateBean>  list  = loadFromCache();
            postValue(list);
        }
    };


    private  List<ExchangeRateBean>  loadFromCache() {
        String json = mConfig.getCacheExchangeRateListRequest();
        if (TextUtils.isEmpty(json)) return null;

        return parseExchangeRateList(json);
    }

    private static List<ExchangeRateBean> parseExchangeRateList(final String json )  {
        try {
            return HttpUtil.parseExchangeRateList(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void cacheRequest(String json) {
        mConfig.cacheExchangeRateListRequest( json);
    }

}
