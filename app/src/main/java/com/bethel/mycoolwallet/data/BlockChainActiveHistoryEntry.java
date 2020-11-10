package com.bethel.mycoolwallet.data;

import androidx.annotation.NonNull;

import java.util.Locale;

public class BlockChainActiveHistoryEntry {
    public final int numTransactionsReceived;
    public final int numBlocksDownloaded;

    public BlockChainActiveHistoryEntry(int numTransactionsReceived, int numBlocksDownloaded) {
        this.numTransactionsReceived = numTransactionsReceived;
        this.numBlocksDownloaded = numBlocksDownloaded;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US,"%d / %d", numTransactionsReceived, numBlocksDownloaded);
    }
}
