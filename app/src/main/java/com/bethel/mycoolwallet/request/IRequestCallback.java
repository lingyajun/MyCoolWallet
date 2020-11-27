package com.bethel.mycoolwallet.request;

//import com.bethel.mycoolwallet.data.ExchangeRateBean;

public interface IRequestCallback {
//    void onSuccess(ExchangeRateBean result);
    void onSuccess(String response);
    void onFailed(String message);
}
