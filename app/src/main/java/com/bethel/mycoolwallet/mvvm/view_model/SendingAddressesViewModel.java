package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.mvvm.live_data.ClipLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.address.ReceivingAddressesLiveData;

import java.util.List;
import java.util.Set;

public class SendingAddressesViewModel extends BaseViewModel {
    public final ReceivingAddressesLiveData receivingAddresses;
    public final ClipLiveData clipBoard;
    public final WalletLiveData wallet;

    public final AddressBookDao addressBookDao;
    public LiveData<List<AddressBook>> addressBook;

    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<String>> showEditAddressBookDialog = new MutableLiveData<>();

    public SendingAddressesViewModel(@NonNull Application app) {
        super(app);
        clipBoard = new ClipLiveData(app);
        receivingAddresses = new ReceivingAddressesLiveData(getApplication());
        wallet = new WalletLiveData(getApplication());
        addressBookDao = AppDatabase.getInstance(app).addressBookDao();
    }

    public void initAddressBook(Set<String> addresses) {
        addressBook = addressBookDao.getAllExcept(addresses);
    }

}
