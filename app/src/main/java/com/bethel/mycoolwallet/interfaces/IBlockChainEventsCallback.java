package com.bethel.mycoolwallet.interfaces;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;

public interface IBlockChainEventsCallback {
    void onPeerConnectionChanged(final int peerCount);
    void onNetworkOrStorageChanged(); // Impediment
    void  onBlockChainIdling();
    void onBlocksDownloaded();
    void onCoinsReceived(Address address, Coin amount, Sha256Hash txId);
}
