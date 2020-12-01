package com.bethel.mycoolwallet.mvvm.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bethel.mycoolwallet.data.AddressBean;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.data.payment.PaymentData;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.mvvm.live_data.BlockChainStateLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletBalanceLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.exchange_rate.ExchangeRateLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.WalletLiveData;
import com.bethel.mycoolwallet.mvvm.live_data.exchange_rate.ExchangeRateSelectedLiveData;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;

import java.util.List;

public class SendCoinsViewModel extends BaseViewModel {
    public final WalletLiveData wallet;
    public final ExchangeRateSelectedLiveData exchangeRate;
    public final WalletBalanceLiveData balance;
    public final BlockChainStateLiveData blockChain ;
    public final LiveData<List<AddressBook>> addressBook;


    public final MutableLiveData<String> progress = new MutableLiveData<>();

//    public Address toAddress = null;
    public Transaction sentTransaction = null;

    public PaymentData paymentData = null;
    public State state = null;
    public AddressBean validatedAddress = null;
    public Boolean directPaymentAck = null;

    @Nullable
    public Transaction dryrunTransaction = null;
    @Nullable
    public Exception dryrunException = null;

    public SendCoinsViewModel(@NonNull Application app) {
        super(app);
        wallet = new WalletLiveData(getApplication());
        exchangeRate = new ExchangeRateSelectedLiveData();
        balance = new WalletBalanceLiveData(getApplication());
        blockChain = new BlockChainStateLiveData();
        addressBook = AppDatabase.getInstance(app).addressBookDao().getAll();
    }


    public enum State {
        REQUEST_PAYMENT_REQUEST, //
        INPUT, // asks for confirmation
        DECRYPTING, SIGNING, SENDING, SENT, FAILED // sending states
    }

}
