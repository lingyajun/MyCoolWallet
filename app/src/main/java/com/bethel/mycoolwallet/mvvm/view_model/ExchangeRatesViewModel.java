package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MediatorLiveData;

import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.mvvm.live_data.ExchangeRateListLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.ExchangeRateLiveData;

import java.util.LinkedList;
import java.util.List;

public class ExchangeRatesViewModel extends BaseViewModel {
    private final ExchangeRateListLiveData rateListLiveData;
    private final ExchangeRateLiveData rateLiveData;

    public final MediatorLiveData<List<ExchangeRateBean>> listExchangeRate = new MediatorLiveData<>();

    public ExchangeRatesViewModel(@NonNull Application app) {
        super(app);
        rateLiveData = new ExchangeRateLiveData(ExchangeRateLiveData.defaultCurrencyCode());
        rateListLiveData = new ExchangeRateListLiveData();

        listExchangeRate.addSource(rateListLiveData, exchangeRateBeans -> merge());
        listExchangeRate.addSource(rateLiveData, rateBean -> merge());
    }

    public void load() {
        rateListLiveData.load();
        rateLiveData.load();
    }

    private void merge() {
        List<ExchangeRateBean> list = mergeValue();
        listExchangeRate.postValue(list);
    }

    private List<ExchangeRateBean> mergeValue() {
        List<ExchangeRateBean> list = rateListLiveData.getValue();
        list = checkRepeat(list);
        ExchangeRateBean bean = rateLiveData.getValue();
        if (null==bean || TextUtils.isEmpty(bean.getCurrencyCode())) return list;
        if (null==list) {
            list = new LinkedList<>();
            list.add(bean);
            return list;
        }
        boolean contains = isExist(bean, list);

        if (!contains) {
            list.add(0, bean);
        }
        return list;
    }

    private static List<ExchangeRateBean> checkRepeat(List<ExchangeRateBean> list) {
        if (null==list || list.isEmpty()) return list;
        List<ExchangeRateBean> data = new LinkedList<>();
        for (ExchangeRateBean bean: list ) {
            boolean contains = isExist(bean, data);
            if (!contains) {
                data.add(bean);
            }
        }
        return data;
    }

    private static boolean isExist(ExchangeRateBean bean, List<ExchangeRateBean> list) {
        if (null == bean || null == list) return false;
        boolean contains = false;
        for (ExchangeRateBean rate: list  ) {
            if (TextUtils.equals(rate.getCurrencyCode(), bean.getCurrencyCode())) {
                contains = true;
                break;
            }
        }
        return contains;
    }
}
