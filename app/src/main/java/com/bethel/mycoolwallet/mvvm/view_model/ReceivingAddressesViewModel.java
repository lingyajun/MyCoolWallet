package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.mvvm.live_data.ConfigOwnNameLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.address.ImportedAddressesLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.address.IssuedReceiveAddressesLiveData;

import org.bitcoinj.core.Address;

import java.util.List;

public class ReceivingAddressesViewModel extends BaseViewModel {
    public final IssuedReceiveAddressesLiveData issuedReceiveAddresses;
    public final ImportedAddressesLiveData importedAddresses;

    public final LiveData<List<AddressBook>> addressBook;
    public final WalletLiveData wallet;
    public final ConfigOwnNameLiveData ownName;

    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Address>> showEditAddressBookDialog = new MutableLiveData<>();

    public ReceivingAddressesViewModel(@NonNull Application app) {
        super(app);
        issuedReceiveAddresses = new IssuedReceiveAddressesLiveData(getApplication());
        importedAddresses = new ImportedAddressesLiveData(getApplication());

        addressBook = AppDatabase.getInstance(app).addressBookDao().getAll();
        wallet = new WalletLiveData(getApplication());
        ownName = new ConfigOwnNameLiveData();
    }
}
