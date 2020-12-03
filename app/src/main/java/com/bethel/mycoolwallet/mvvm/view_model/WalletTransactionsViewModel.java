package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.data.tx.TransactionDirection;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.mvvm.live_data.ConfigFormatLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.transation.TransactionsConfidenceLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.transation.TransactionsSetLiveData;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Sha256Hash;

import java.util.List;

public class WalletTransactionsViewModel extends BaseViewModel {
    public final WalletLiveData wallet;
    public final TransactionsSetLiveData transactions;

    private final TransactionsConfidenceLiveData transactionsConfidence;
    private final ConfigFormatLiveData configFormat;
    private final LiveData<List<AddressBook>> addressBook;

    public final MutableLiveData<TransactionDirection> direction = new MutableLiveData<>();
    private final MutableLiveData<Sha256Hash> selectedTransaction = new MutableLiveData<>();

    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Address>> showEditAddressBookEntryDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<String>> showReportIssueDialog = new MutableLiveData<>();

//  todo  public final MutableLiveData<TransactionsAdapter.WarningType> warning = new MutableLiveData<>();
//    public final MediatorLiveData<List<TransactionsAdapter.ListItem>> list = new MediatorLiveData<>();

    public WalletTransactionsViewModel(@NonNull Application app) {
        super(app);
        wallet = new WalletLiveData(getApplication());
        transactions = new TransactionsSetLiveData(getApplication());
        transactionsConfidence = new TransactionsConfidenceLiveData(getApplication());
        configFormat = new ConfigFormatLiveData();
        addressBook = AppDatabase.getInstance(app).addressBookDao().getAll();


    }
}
