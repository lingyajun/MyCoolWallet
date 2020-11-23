package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.mvvm.live_data.BlocksLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.TimeLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.TransactionsLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;

import java.util.List;

public class BlocksNetworkMonitorViewModel extends BaseViewModel {
    public final TransactionsLiveData transactions;
    public final BlocksLiveData blocks;
    public final WalletLiveData wallet;
    public final TimeLiveData timeTick;

    public final LiveData<List<AddressBook>> addressBook;

    public BlocksNetworkMonitorViewModel(@NonNull Application app) {
        super(app);
        transactions = new TransactionsLiveData(getApplication());
        blocks = new BlocksLiveData(app);
        wallet = new WalletLiveData(getApplication());
        timeTick = new TimeLiveData(app);
        addressBook = AppDatabase.getInstance(app).addressBookDao().getAll();
    }
}
