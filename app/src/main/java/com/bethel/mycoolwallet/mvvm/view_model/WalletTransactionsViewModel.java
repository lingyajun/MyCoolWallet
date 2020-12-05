package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.data.tx_list.TransactionDirection;
import com.bethel.mycoolwallet.data.tx_list.TransactionWarningType;
import com.bethel.mycoolwallet.data.tx_list.item.IListItem;
import com.bethel.mycoolwallet.data.tx_list.item.TransactionListHelper;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.mvvm.live_data.ConfigFormatLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.transation.TransactionsConfidenceLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.transation.TransactionsSetLiveData;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.Wallet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WalletTransactionsViewModel extends BaseViewModel {
    public final WalletLiveData wallet;
    public final TransactionsSetLiveData transactions;

    private final TransactionsConfidenceLiveData transactionsConfidence;
    private final ConfigFormatLiveData configFormat;
    private final LiveData<List<AddressBook>> addressBook;

    public final MutableLiveData<TransactionDirection> direction = new MutableLiveData<>();
    public final MutableLiveData<Sha256Hash> selectedTransaction = new MutableLiveData<>();

    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<Address>> showEditAddressBookDialog = new MutableLiveData<>();
    public final MutableLiveData<Event<String>> showReportIssueDialog = new MutableLiveData<>();

    public final MutableLiveData<TransactionWarningType> warning = new MutableLiveData<>();
    public final MediatorLiveData<List<IListItem>> list = new MediatorLiveData<>();

    public final AddressBookDao addressBookDao;

    public WalletTransactionsViewModel(@NonNull Application app) {
        super(app);
        wallet = new WalletLiveData(getApplication());
        transactions = new TransactionsSetLiveData(getApplication());
        transactionsConfidence = new TransactionsConfidenceLiveData(getApplication());
        configFormat = new ConfigFormatLiveData();
        addressBookDao = AppDatabase.getInstance(app).addressBookDao();
        addressBook = addressBookDao.getAll();

        list.addSource(wallet, wallet1 ->  maybePostList());
        list.addSource(transactions, transactions1 -> maybePostList());
        list.addSource(transactionsConfidence, aVoid -> maybePostList());
        list.addSource(configFormat, monetaryFormat -> maybePostList());
        list.addSource(addressBook, list1 -> maybePostList());
        list.addSource(direction, transactionDirection -> maybePostList());
        list.addSource(selectedTransaction, sha256Hash -> maybePostList());

    }

    private void maybePostList() {
        executeAsyncTask(()->{
            MyCoolWalletManager.propagate();
            final Set<Transaction> transactionSet = transactions.getValue();
            final MonetaryFormat format = configFormat.getValue();
            final Map<String, AddressBook > bookMap = AddressBook.asMap(addressBook.getValue() );
            if (null==transactionSet || null==format || null==bookMap) return;

            final Wallet wallet = application.getWallet();
            final List<Transaction> filteredList = new ArrayList<>(transactionSet.size());
            final TransactionDirection txDirection = direction.getValue(); // txDirection 用来筛选交易记录

            for (Transaction tx: transactionSet      ) {
                final boolean sent = tx.getValue(wallet).signum() < 0;
                final boolean isInternal = tx.getPurpose() == Transaction.Purpose.KEY_ROTATION;

                if (null==txDirection || TransactionDirection.ALL == txDirection
                        || (!sent && !isInternal && TransactionDirection.RECEIVED==txDirection)
                        || (sent && !isInternal && TransactionDirection.SENT==txDirection) ) {
                    // 交易不是发送coin && 不是钱包内资金流转 && 交易方向为接收
                    // 交易是发送coin && 不是钱包内资金流转 && 交易方向为发送
                    filteredList.add(tx);
                }
            } // for -- end

            Collections.sort(filteredList, TransactionListHelper.TRANSACTION_COMPARATOR);

            List<IListItem> array = TransactionListHelper.buildList(application, filteredList,
                    warning.getValue(), wallet, bookMap, format, application.maxConnectedPeers(),
                    selectedTransaction.getValue());

            list.postValue(array);
        });
    }

}
