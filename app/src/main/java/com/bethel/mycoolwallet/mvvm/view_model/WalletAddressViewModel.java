package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.mvvm.live_data.ConfigOwnNameLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.address.CurrentAddressLiveData;
import com.bethel.mycoolwallet.utils.Qr;

import org.bitcoinj.core.Address;
import org.bitcoinj.uri.BitcoinURI;

public class WalletAddressViewModel extends BaseViewModel {
    public final CurrentAddressLiveData currentAddress;
    public final MediatorLiveData<Bitmap> qrCode = new MediatorLiveData<>();
    public final MediatorLiveData<Uri> bitcoinUri = new MediatorLiveData<>(); // NFC
    public final MutableLiveData<Event<Void>> showWalletAddressDialog = new MutableLiveData<>();

//    public final AddressBookDao addressBookDao;
    public final ConfigOwnNameLiveData ownName;

    public WalletAddressViewModel(@NonNull Application app) {
        super(app);
        currentAddress = new CurrentAddressLiveData(application);
        qrCode.addSource(currentAddress, (address)-> maybeGenerateQrCode());
        bitcoinUri.addSource(currentAddress, (address)-> maybeGenerateBitcoinUri());

        ownName = new ConfigOwnNameLiveData();
        qrCode.addSource(ownName, (address)-> maybeGenerateQrCode());
        bitcoinUri.addSource(ownName, (address)-> maybeGenerateBitcoinUri());
    }

    private void maybeGenerateQrCode() {
        final Address address = currentAddress.getValue();
        if (address != null) {
            executeAsyncTask(()->{
                Bitmap bitmap = Qr.bitmap(uri(address, getLabel()));
                qrCode.postValue(bitmap);
            });
        }
    }


    private void maybeGenerateBitcoinUri() {
        final Address address = currentAddress.getValue();
        if (address != null) {
            Uri uri = Uri.parse(uri(address, getLabel()));
            bitcoinUri.setValue(uri);
        }
    }

    private String uri(final Address address, String label) {
        return BitcoinURI.convertToBitcoinURI(address, null, label, null);
    }

    public String getLabel() {
        return ownName.getValue();
    }
}
