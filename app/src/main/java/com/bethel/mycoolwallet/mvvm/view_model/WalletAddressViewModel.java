package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.live_data.address.CurrentAddressLiveData;
import com.bethel.mycoolwallet.utils.Qr;

import org.bitcoinj.core.Address;
import org.bitcoinj.uri.BitcoinURI;

public class WalletAddressViewModel extends BaseViewModel {
    public final CurrentAddressLiveData currentAddress;
    public final MediatorLiveData<Bitmap> qrCode = new MediatorLiveData<>();
    public final MediatorLiveData<Uri> bitcoinUri = new MediatorLiveData<>();
    public final MutableLiveData<Event<Void>> showWalletAddressDialog = new MutableLiveData<>();

    public WalletAddressViewModel(@NonNull Application app) {
        super(app);
        currentAddress = new CurrentAddressLiveData(application);
        qrCode.addSource(currentAddress, (address)-> maybeGenerateQrCode());
        bitcoinUri.addSource(currentAddress, (address)-> maybeGenerateBitcoinUri());
    }

    private void maybeGenerateQrCode() {
        final Address address = currentAddress.getValue();
        if (address != null) {
            executeAsyncTask(()->{
                qrCode.postValue(Qr.bitmap(uri(address)));
            });
        }
    }


    private void maybeGenerateBitcoinUri() {
        final Address address = currentAddress.getValue();
        if (address != null) {
            bitcoinUri.setValue(Uri.parse(uri(address)));
        }
    }

    private String uri(final Address address) {
        return BitcoinURI.convertToBitcoinURI(address, null, null, null);
    }

}
