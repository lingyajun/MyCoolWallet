package com.bethel.mycoolwallet.request;

import android.text.TextUtils;

import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.utils.Commons;
import com.google.common.io.CharStreams;

import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public final class HttpUtil {
    private final static String URL_FORMATE_SINGLE_CURRENCY_EXCHANGE_RATE =
            "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=%s&api_key=9305f9eefaa8751f83294254a0130fee0bfbeba1a5013c6f2058887cee3c5db2";

    private final static String URL_FORMATE_CURRENCY_EXCHANGE_RATE_LIST =
            "https://sochain.com/api/v2/get_price/BTC";
    private static final String CRYPTOCOMPARE_SOURCE = "CryptoCompare.com";
    private static final String SOCHAIN_SOURCE = "SoChain.com";

    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    //  request list
    public static void requestExchangeRateList(IRequestCallback callback) {
        sendRequest(URL_FORMATE_CURRENCY_EXCHANGE_RATE_LIST, callback);
    }

    public static void requestExchangeRate(final String currencyCode, IRequestCallback callback) {
        final String api = String.format(URL_FORMATE_SINGLE_CURRENCY_EXCHANGE_RATE, currencyCode);
        sendRequest(api, callback);
    }
    private static void sendRequest(final String api, IRequestCallback callback) {
        try {
            URL urlReq = new URL(api);
            HttpURLConnection connection = (HttpURLConnection) urlReq.openConnection();
            if (200 != connection.getResponseCode()) {
                String message = String.format(Locale.CHINESE,"%d , %s",
                        connection.getResponseCode(), connection.getResponseMessage()) ;
                if (null!=callback) callback.onFailed(message);
                //  handle request error
                log.error(" req error, Response:   {}", message);
                return;
            }

            InputStream is = connection.getInputStream();
            InputStreamReader inReader = new InputStreamReader(is, Commons.UTF_8);
            BufferedReader buffer = new BufferedReader(inReader);

            StringBuilder data = new StringBuilder();
            CharStreams.copy(buffer, data);

            final String json = data.toString();
            // parse data: {"CNY":106400}
            if (null!=callback) {
                callback.onSuccess(json);
            }
        } catch (Exception e) {
            if (null!=callback) callback.onFailed(e.getMessage());
            //  handle request error
            log.error(" req exception ", e);
        }
    }

    public static ExchangeRateBean parseSingleCurrencyExchangeRate(final String currencyCode,
                                                                   final String json) throws JSONException {
        return parseSingleCurrencyExchangeRate(currencyCode, json, CRYPTOCOMPARE_SOURCE);
    }

    /**
     * parse data
     *
     * @param json : {"CNY":106400}
     */
    public static ExchangeRateBean parseSingleCurrencyExchangeRate(final String currencyCode, final String json,
                                                                   final String source) throws JSONException {
        log.info("parse json {}", json);
        final JSONObject jsonObject = new JSONObject(json);
        double value = 0;
        for (Iterator<String> k = jsonObject.keys(); k.hasNext(); ) {
            String code = k.next();
            double rate = jsonObject.getDouble(code);
            log.info("parse single {} , {}", code, rate);
            if (TextUtils.equals(currencyCode, code)) {
                value = rate;
                break;
            }
        }

        Fiat fiat = parseFiatInexact(currencyCode, value);
        ExchangeRate ex = new ExchangeRate(fiat);
        ExchangeRateBean rateBean = new ExchangeRateBean(ex, source);
            log.info("result: {}", rateBean);
        return rateBean;
    }

    /**
     * parse data
     *
     * @param json : {
     *   "status" : "success",
     *   "data" : {
     *     "network" : "BTC",
     *     "prices" : [
     *       {
     *         "price" : "14994.0",
     *         "price_base" : "EUR",
     *         "exchange" : "bitfinex",
     *         "time" : 1605680394
     *       },... ]
     *             }
     *      }
     */
    public static List<ExchangeRateBean> parseExchangeRateList(final String json,
                                             final String source) throws JSONException {
        List<ExchangeRateBean> list = null;
        log.info("parse json {}", json);
        final JSONObject jsonObject = new JSONObject(json);
        if (!"success".equals(jsonObject.getString("status")))  return list;
        final JSONObject data = jsonObject.getJSONObject("data" );
        JSONArray prices = null!=data ? data.getJSONArray("prices" ) : null;
        final int length = null!=prices? prices.length():0;
        if (length <1)   return list;

        list = new LinkedList<>();
        for (int i = 0; i < length; i++) {
            JSONObject p = prices.getJSONObject(i);
            String currencyCode = p.getString("price_base");
            String value = p.getString("price" );

            Fiat fiat = parseFiatInexact(currencyCode, value);
            ExchangeRate ex = new ExchangeRate(fiat);
            ExchangeRateBean rateBean = new ExchangeRateBean(ex, source);
            list.add(rateBean);
            log.info("result: {} {}",i, rateBean);
        }
        return list;
    }

    public static List<ExchangeRateBean> parseExchangeRateList(final String json ) throws JSONException {
        return parseExchangeRateList(json, SOCHAIN_SOURCE);
    }

    private static Fiat parseFiatInexact(final String currencyCode, final double rate) {
//        final long value = new BigDecimal(rate).longValue();
        final long value =  new BigDecimal(rate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT).longValue();
        return Fiat.valueOf(currencyCode, value);
    }
    private static Fiat parseFiatInexact(final String currencyCode, final String rate) {
        final long value =  new BigDecimal(rate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT).longValue();
        return Fiat.valueOf(currencyCode, value);
    }
}
