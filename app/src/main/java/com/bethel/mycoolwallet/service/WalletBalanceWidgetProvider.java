package com.bethel.mycoolwallet.service;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.MainActivity;
import com.bethel.mycoolwallet.activity.RequestCoinsActivity;
import com.bethel.mycoolwallet.activity.SendCoinsActivity;
import com.bethel.mycoolwallet.activity.SendCoinsQrActivity;
import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.helper.CoolThreadPool;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 账户余额，桌面控件.
 */
public class WalletBalanceWidgetProvider extends AppWidgetProvider {
    private static final Logger log = LoggerFactory.getLogger(WalletBalanceWidgetProvider.class);

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, final Bundle appWidgetOptions, final Coin balance,
                                        final @Nullable ExchangeRateBean exchangeRate) {
//        final CoolApplication application = (CoolApplication) context.getApplicationContext();
        final MonetaryFormat btcFormat = Configuration.INSTANCE.getFormat();

        final CharSequence balanceStr = CurrencyTools.format(btcFormat.noCode(), false, balance);
        final CharSequence localBalanceStr;
        if (exchangeRate != null) {
            final Fiat localBalance = exchangeRate.rate.coinToFiat(balance);
            final MonetaryFormat localFormat = Constants.LOCAL_FORMAT.code(0,
                    Constants.PREFIX_ALMOST_EQUAL_TO + CurrencyTools.currencySymbol(exchangeRate.getCurrencyCode()));
//            final Object[] prefixSpans = new Object[] { MonetarySpannable.SMALLER_SPAN,
//                    new ForegroundColorSpan(ContextCompat.getColor(context, R.color.fg_less_significant)) };
            localBalanceStr = CurrencyTools.format(localFormat, false, localBalance);
//                    new MonetarySpannable(localFormat, localBalance).applyMarkup(prefixSpans,
//                    MonetarySpannable.STANDARD_INSIGNIFICANT_SPANS);
        } else {
            localBalanceStr = null;
        }

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wallet_balance_widget_provider);

        final String currencyCode = btcFormat.code();
        if (MonetaryFormat.CODE_BTC.equals(currencyCode))
            views.setImageViewResource(R.id.widget_wallet_prefix, R.drawable.currency_symbol_btc);
        else if (MonetaryFormat.CODE_MBTC.equals(currencyCode))
            views.setImageViewResource(R.id.widget_wallet_prefix, R.drawable.currency_symbol_mbtc);
        else if (MonetaryFormat.CODE_UBTC.equals(currencyCode))
            views.setImageViewResource(R.id.widget_wallet_prefix, R.drawable.currency_symbol_ubtc);

        views.setTextViewText(R.id.widget_wallet_balance_btc, balanceStr);
        views.setViewVisibility(R.id.widget_wallet_balance_local, localBalanceStr != null ? View.VISIBLE : View.GONE);
        views.setTextViewText(R.id.widget_wallet_balance_local, localBalanceStr);

        if (appWidgetOptions != null) {
            final int minWidth = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            views.setViewVisibility(R.id.widget_app_icon, minWidth > 400 ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_button_request, minWidth > 300 ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_button_send, minWidth > 300 ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_button_send_qr, minWidth > 200 ? View.VISIBLE : View.GONE);
        }

        views.setOnClickPendingIntent(R.id.widget_button_balance,
                PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0));
        views.setOnClickPendingIntent(R.id.widget_button_request,
                PendingIntent.getActivity(context, 0, new Intent(context, RequestCoinsActivity.class), 0));
        views.setOnClickPendingIntent(R.id.widget_button_send,
                PendingIntent.getActivity(context, 0, new Intent(context, SendCoinsActivity.class), 0));
        views.setOnClickPendingIntent(R.id.widget_button_send_qr,
                PendingIntent.getActivity(context, 0, new Intent(context, SendCoinsQrActivity.class), 0));

//        if (0!=appWidgetId) {
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
//            return;
//        }

        // 0 == appWidgetId
//        final ComponentName COMPONENT_NAME = new ComponentName(context, WalletBalanceWidgetProvider.class);
//        AppWidgetManager.getInstance(context).updateAppWidget(COMPONENT_NAME, views);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final PendingResult result = goAsync();
        CoolThreadPool.execute(() -> {
            final CoolApplication application = (CoolApplication) context.getApplicationContext();
            final Wallet wallet = application.getWallet();
            final Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
            final ExchangeRateBean rateBean = Configuration.INSTANCE.getCachedExchangeRate();
            updateAppWidgets(context, appWidgetManager, appWidgetIds, balance, rateBean);
            result.finish();
        });
    }

    private static void updateAppWidgets(final Context context, final AppWidgetManager appWidgetManager,
                                         final int[] appWidgetIds, final Coin balance, final @Nullable ExchangeRateBean exchangeRate) {
        for (final int appWidgetId : appWidgetIds) {
            final Bundle options = getAppWidgetOptions(appWidgetManager, appWidgetId);
            updateAppWidget(context, appWidgetManager, appWidgetId, options, balance, exchangeRate);
        }
    }

    public static void updateWidgets(final Context context, final Coin balance,
                                     final @Nullable ExchangeRateBean exchangeRate) {
        log.info("updateWidgets {}, {}", balance, exchangeRate);
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final ComponentName providerName = new ComponentName(context, WalletBalanceWidgetProvider.class);

        try {
            final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(providerName);
            if (null!=appWidgetIds && appWidgetIds.length > 0) {
                WalletBalanceWidgetProvider.updateAppWidgets(context, appWidgetManager, appWidgetIds, balance,
                        exchangeRate);
//            } else {
//                // test
//                WalletBalanceWidgetProvider.updateAppWidget(context, appWidgetManager, 0, null, balance,
//                        exchangeRate);
            }

            log.info("updateWidgets  ids, {}", appWidgetIds);
        } catch (final RuntimeException x) // system server dead?
        {
            log.warn("cannot update app widgets", x);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        if (newOptions != null)
            log.info("app widget {} options changed: minWidth={}", appWidgetId,
                    newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
        final PendingResult result = goAsync();
        CoolThreadPool.execute(() -> {
            final CoolApplication application = (CoolApplication) context.getApplicationContext();
            final Wallet wallet = application.getWallet();
            final Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
            final ExchangeRateBean rateBean = Configuration.INSTANCE.getCachedExchangeRate();
            updateAppWidget(context, appWidgetManager, appWidgetId, newOptions, balance, rateBean);
            result.finish();
        });
    }

    private static Bundle getAppWidgetOptions(final AppWidgetManager appWidgetManager, final int appWidgetId) {
        try {
            final Method getAppWidgetOptions = AppWidgetManager.class.getMethod("getAppWidgetOptions", Integer.TYPE);
            return (Bundle) getAppWidgetOptions.invoke(appWidgetManager, appWidgetId);
        } catch (final Exception x) {
            return null;
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

