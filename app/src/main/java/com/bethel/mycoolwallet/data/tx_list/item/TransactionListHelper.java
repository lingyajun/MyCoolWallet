package com.bethel.mycoolwallet.data.tx_list.item;

import android.content.Context;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.data.tx_list.TransactionWarningType;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
//import org.bitcoinj.core.Utils;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.Wallet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class TransactionListHelper {
    public static List<IListItem> buildList(final Context context, final List<Transaction> transactions,
                                            final TransactionWarningType warning,
                                            final @Nullable Wallet wallet,
                                            final @Nullable Map<String, AddressBook> addressBook,
                                            final MonetaryFormat format, final int maxConnectedPeers,
                                            final @Nullable Sha256Hash selectedTransaction) {
        List<IListItem> list = new ArrayList<>(transactions.size()+1);
        final MonetaryFormat noCodeFormat = format.noCode();

        if (null!=warning) {
            list.add(new TransactionWarningItem(warning));
        }

        for (Transaction tx: transactions     ) {
            TransactionListItem item = new TransactionListItem(context, tx, wallet, addressBook,
                    noCodeFormat, maxConnectedPeers, Utils.equals(selectedTransaction, tx.getTxId()));
            list.add(item);
        }
        return list;
    }

    public static final Comparator<Transaction> TRANSACTION_COMPARATOR = new Comparator<Transaction>() {
        @Override
        public int compare(final Transaction tx1, final Transaction tx2) {
            final boolean pending1 = tx1.getConfidence().getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING;
            final boolean pending2 = tx2.getConfidence().getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING;
            if (pending1 != pending2)
                return pending1 ? -1 : 1;

            final Date updateTime1 = tx1.getUpdateTime();
            final long time1 = updateTime1 != null ? updateTime1.getTime() : 0;
            final Date updateTime2 = tx2.getUpdateTime();
            final long time2 = updateTime2 != null ? updateTime2.getTime() : 0;
            if (time1 != time2)
                return time1 > time2 ? -1 : 1;

            return tx1.getTxId().compareTo(tx2.getTxId());
        }
    };

}
