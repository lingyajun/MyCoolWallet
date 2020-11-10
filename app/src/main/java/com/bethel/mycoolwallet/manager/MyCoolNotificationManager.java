package com.bethel.mycoolwallet.manager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.MainActivity;
import com.bethel.mycoolwallet.helper.Configuration;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.utils.MonetaryFormat;

import java.util.LinkedList;
import java.util.List;

public class MyCoolNotificationManager {
    private CoolApplication application;
    private Context service;
    private Configuration mConfig;
    private NotificationManager manager;

    private int notificationCount = 0;
    private Coin notificationAccumulatedAmount = Coin.ZERO;
    private final List<Address> notificationAddresses = new LinkedList<>();

    public void init(Context service) {
        this.service = service;
        application = CoolApplication.getApplication();
        mConfig = Configuration.instance(application);
        manager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void notifyCoinsReceived(@Nullable final Address address, final Coin amount,
                                    final Sha256Hash transactionHash) {
        notificationCount ++;
        notificationAccumulatedAmount = notificationAccumulatedAmount.add(amount);
        if (null!=address && !notificationAddresses.contains(address)) {
            notificationAddresses.add(address);
        }

        final MonetaryFormat btcFormat = CurrencyTools.getFormat(mConfig.getBtcShift(), mConfig.getBtcPrecision());
        final String pkgFlavor = application.applicationPackageFlavor();
        final String msgSuffix = !TextUtils.isEmpty(pkgFlavor) ? String.format(" [%s]", pkgFlavor): "";

        // summary notification
        final NotificationCompat.Builder summaryNotification = new NotificationCompat.Builder(service,
                Constants.NOTIFICATION_CHANNEL_ID_RECEIVED);
        summaryNotification.setGroup(Constants.NOTIFICATION_GROUP_KEY_RECEIVED);
        summaryNotification.setGroupSummary(true);
        summaryNotification.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
        summaryNotification.setWhen(System.currentTimeMillis());
        summaryNotification.setSmallIcon(R.drawable.stat_notify_received_24dp);
        summaryNotification.setContentTitle(
               service.getString(R.string.notification_coins_received_msg, btcFormat.format(notificationAccumulatedAmount))
                        + msgSuffix);

        if (!notificationAddresses.isEmpty()) {
            final StringBuilder text = new StringBuilder();
            for (final Address notificationAddress : notificationAddresses) {
                if (text.length() > 0)
                    text.append(", ");
                final String addressStr = notificationAddress.toString();
//  todo :        final String label = addressBookDao.resolveLabel(addressStr);
//                text.append(label != null ? label : addressStr);
                text.append(addressStr);
            }
            summaryNotification.setContentText(text);
        }

        summaryNotification.setContentIntent(
                        PendingIntent.getActivity(service, 0, new Intent(service, MainActivity.class),
                                0));
        manager.notify(Constants.NOTIFICATION_ID_COINS_RECEIVED, summaryNotification.build());

        // child notification
        final NotificationCompat.Builder childNotification = new NotificationCompat.Builder(service,
                Constants.NOTIFICATION_CHANNEL_ID_RECEIVED);
        childNotification.setGroup(Constants.NOTIFICATION_GROUP_KEY_RECEIVED);
        childNotification.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
        childNotification.setWhen(System.currentTimeMillis());
        childNotification.setColor(ContextCompat.getColor(service, R.color.fg_network_significant));
        childNotification.setSmallIcon(R.drawable.stat_notify_received_24dp);

        final String msg = service.getString(R.string.notification_coins_received_msg, btcFormat.format(amount)) + msgSuffix;
        childNotification.setTicker(msg);
        childNotification.setContentTitle(msg);
        if (address != null) {
            final String addressStr = address.toString();
//   todo         final String addressLabel = addressBookDao.resolveLabel(addressStr);
//            if (addressLabel != null)
//                childNotification.setContentText(addressLabel);
//            else
                childNotification.setContentText(addressStr);
        }

        childNotification.setContentIntent(
                        PendingIntent.getActivity(service, 0, new Intent(service, MainActivity.class), 0));
        childNotification.setSound(Uri.parse("android.resource://" + application.getPackageName() + "/" + R.raw.coins_received));
        manager.notify(transactionHash.toString(), Constants.NOTIFICATION_ID_COINS_RECEIVED, childNotification.build());
    }

    public void cancelNotification(int notificationId) {
        manager.cancel(notificationId);
    }
    public void cancelCoinsReceivedNotification() {
        notificationAddresses.clear();
        notificationAccumulatedAmount = Coin.ZERO;
        notificationCount = 0;
        cancelNotification(Constants.NOTIFICATION_ID_COINS_RECEIVED);
    }

    // startForeground()
    public static Notification buildPeersCountNotification(Context service, final int numPeers) {
        final NotificationCompat.Builder notification = new NotificationCompat.Builder(service,
                Constants.NOTIFICATION_CHANNEL_ID_ONGOING);
        notification.setColor(ContextCompat.getColor(service, R.color.fg_network_significant));
        notification.setSmallIcon(R.drawable.stat_notify_peers, Math.min(numPeers, 4));
        notification.setContentTitle(service.getString(R.string.app_name));
        notification.setContentText(service.getString(R.string.notification_peers_connected_msg, numPeers));
        notification.setContentIntent(PendingIntent.getActivity(service, 0,
                new Intent(service, MainActivity.class), 0));
        notification.setWhen(System.currentTimeMillis());
        notification.setOngoing(true);
        return notification.build();
    }
}
