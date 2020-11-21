package com.bethel.mycoolwallet.utils;

import org.bitcoinj.params.MainNetParams;

import java.nio.charset.Charset;

/**
 * @author Bethel
 */
public class Commons {
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    // https://www.blockchain.com/btc/block/000000000000000000029aa834ab9127c23fc66e4b2268a9584fc88a48d6b288
    public static final String MAIN_NET_VIEW = "https://www.blockchain.com/btc/";
    // https://www.blockchain.com/btc-testnet/block/00000000000000493226636e9139fb52bc22cbdb294bb19b60a735ea1ce7cf17
    public static final String TEST_NET_VIEW = "https://www.blockchain.com/btc-testnet/";
    public static final String BLOCK_CHAIN_VIEW = MainNetParams.get().equals(Constants.NETWORK_PARAMETERS)?
            MAIN_NET_VIEW: TEST_NET_VIEW;
}
