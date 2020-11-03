package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.live_data.FreshReceiveAddressLiveData;
import com.bethel.mycoolwallet.utils.BluetoothTools;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.uri.BitcoinURI;

public class RequestCoinsViewModel extends BaseViewModel {
    public final FreshReceiveAddressLiveData freshReceiveAddress;

    public final MediatorLiveData<Bitmap> qrCode = new MediatorLiveData<>();
    public final MediatorLiveData<byte[]> paymentRequest = new MediatorLiveData<>();
    public final MediatorLiveData<Uri> bitcoinUri = new MediatorLiveData<>();
    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();
    public final MutableLiveData<Coin> amount = new MutableLiveData<>();
    public final MutableLiveData<String> bluetoothMac = new MutableLiveData<>();

    // todo ExchangeRate
//    public final SelectedExchangeRateLiveData exchangeRate;

    public RequestCoinsViewModel(@NonNull Application app) {
        super(app);
        freshReceiveAddress = new FreshReceiveAddressLiveData(application);

        qrCode.addSource(freshReceiveAddress, address -> maybeGenerateQrCode());
        qrCode.addSource(amount, address -> maybeGenerateQrCode());
        qrCode.addSource(bluetoothMac, address -> maybeGenerateQrCode());

        paymentRequest.addSource(freshReceiveAddress, address -> maybeGeneratePaymentRequest());
        paymentRequest.addSource(amount, address -> maybeGeneratePaymentRequest());
        paymentRequest.addSource(bluetoothMac, address -> maybeGeneratePaymentRequest());

        bitcoinUri.addSource(freshReceiveAddress, address -> maybeGenerateBitcoinUri());
        bitcoinUri.addSource(amount, address -> maybeGenerateBitcoinUri());
//        bitcoinUri.addSource(bluetoothMac, address -> maybeGeneratePaymentRequest());
    }

    private void maybeGenerateBitcoinUri() {
        final Address address = freshReceiveAddress.getValue();
        if (address != null) {
            String uri = uri(address, amount.getValue(), null, null);
            bitcoinUri.setValue(Uri.parse(uri));
        }
    }

    private void maybeGeneratePaymentRequest() {
        final Address address = freshReceiveAddress.getValue();
        if (null!= address) {
            String bleMac = bluetoothMac.getValue();
            String payUrl = null!=bleMac? "bt:"+bleMac : null;
            byte[] data = PaymentProtocol.createPaymentRequest(Constants.NETWORK_PARAMETERS,
                    amount.getValue(), address, null, payUrl, null).build().toByteArray();
            paymentRequest.setValue(data);
        }
    }

    private void maybeGenerateQrCode() {
        final Address address = freshReceiveAddress.getValue();
        if (null != address) {
            executeAsyncTask(()->{
                String uri = uri(address, amount.getValue(), null, bluetoothMac.getValue());
                qrCode.postValue(Qr.bitmap(uri));
            });
        }
    }

    private String uri(final Address address, final Coin amount, final String label, final String bluetoothMac) {
        final StringBuilder uri = new StringBuilder(BitcoinURI.convertToBitcoinURI(address, amount, label, null));
        if (bluetoothMac != null) {
            uri.append(amount == null && label == null ? '?' : '&');
            uri.append(BluetoothTools.MAC_URI_PARAM).append('=').append(bluetoothMac);
        }
        return uri.toString();
    }

}
