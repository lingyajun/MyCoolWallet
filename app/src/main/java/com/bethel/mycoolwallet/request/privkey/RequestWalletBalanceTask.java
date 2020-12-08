package com.bethel.mycoolwallet.request.privkey;

import android.content.res.AssetManager;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.manager.MyCoolWalletManager;
import com.bethel.mycoolwallet.request.payment.AbsTask;
import com.bethel.mycoolwallet.utils.Constants;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.UTXO;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.ContextPropagatingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLPeerUnverifiedException;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public abstract class RequestWalletBalanceTask extends AbsTask {
    private final AssetManager assets;
    private final ECKey key;

    public RequestWalletBalanceTask(AssetManager assets, ECKey key) {
        this.assets = assets;
        this.key = key;
    }

    private static final Logger log = LoggerFactory.getLogger(RequestWalletBalanceTask.class);

    @Override
    public void run() {
        MyCoolWalletManager.propagate();
        final Address legacyAddress = LegacyAddress.fromKey(Constants.NETWORK_PARAMETERS, key);
        final Script[] outputScripts;
        final String addressesStr;

        // Compressed pubkeys are only 33 bytes, not 64.
        if (key.isCompressed()) {
            final Address segwitAddress = SegwitAddress.fromKey(Constants.NETWORK_PARAMETERS, key);
            outputScripts = new Script[]{ScriptBuilder.createP2PKHOutputScript(legacyAddress.getHash()),
                    ScriptBuilder.createP2WPKHOutputScript(segwitAddress.getHash())};
            addressesStr = legacyAddress.toString() + "," + segwitAddress.toString();
        } else {
            outputScripts = new Script[]{ScriptBuilder.createP2PKHOutputScript(legacyAddress.getHash())};
            addressesStr = legacyAddress.toString();
        }

        try {
            final List<ElectrumServer> servers = ElectrumServer.loadElectrumServers(assets);
            final List<Callable<Set<UTXO>>> taskList = new ArrayList<>(servers.size());
            for (final ElectrumServer es : servers) {
//                Set<UTXO> utxoSet = connectAndParse(es, addressesStr, outputScripts);
                taskList.add(() -> connectAndParse(es, addressesStr, outputScripts));
            }

            // Future
            final List<Future<Set<UTXO>>> futureList;
            final ExecutorService threadPool = Executors.newFixedThreadPool(servers.size(),
                    new ContextPropagatingThreadFactory("request"));
            try {
                futureList = threadPool.invokeAll(taskList, 10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("Electrum Server interrupted ", e);
                throw new RuntimeException(e);
            } finally {
                threadPool.shutdown();
            }

            final Multiset<UTXO> utxoSets = HashMultiset.create();
            int numSuccess = 0, numFail = 0, numTimeOuts = 0;
            for (Future<Set<UTXO>> future : futureList) {
                if (future.isCancelled()) {
                    numTimeOuts++;
                    continue;
                }

                // not cancelled
                try {
                    Set<UTXO> utxos = future.get();
                    if (null != utxos) {
                        utxoSets.addAll(utxos);
                        numSuccess++;
                    } else {
                        numFail++;
                    }
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // trustThreshold
            final int trustThreshold = servers.size() / 2;
            for (Iterator<Multiset.Entry<UTXO>> iterator = utxoSets.entrySet().iterator(); iterator.hasNext(); ) {
                final Multiset.Entry<UTXO> utxoEntry = iterator.next();
                if (utxoEntry.getCount() < trustThreshold) { // 需要得到大多数节点的认证
                    iterator.remove();
                }
            }

            // elementSet
            final Set<UTXO> utxos = utxoSets.elementSet();
            log.info("{} successes, {} fails, {} time-outs, {} UTXOs {}", numSuccess, numFail, numTimeOuts,
                    utxos.size(), utxos);
            if (numSuccess < trustThreshold) {
                // 和Electrum网络的链接不良
                runOnCallbackThread(() -> onFail(R.string.sweep_wallet_fragment_request_wallet_balance_failed_connection));
            } else if (utxos.isEmpty()) {
                // 纸钱包是空的
                runOnCallbackThread(() -> onFail(R.string.sweep_wallet_fragment_request_wallet_balance_empty));
            } else {
                runOnCallbackThread(() -> onResult(utxos));
            }
        } catch (IOException e) {
            log.warn("load Electrum Servers  ", e);
        }
    }

    private static Set<UTXO> connectAndParse(final ElectrumServer server,
                                             final String addressesStr,
                                             final Script[] outputScripts) {
        log.info("{} - trying to request wallet balance for {}", server.socketAddress, addressesStr);
        try {
            final Socket socket = server.connect();

            final BufferedSink sink = Okio.buffer(Okio.sink(socket));
            sink.timeout().timeout(5000, TimeUnit.MILLISECONDS);

            final BufferedSource source = Okio.buffer(Okio.source(socket));
            source.timeout().timeout(5000, TimeUnit.MILLISECONDS);

            final Moshi moshi = new Moshi.Builder().build();
            final JsonAdapter<JsonRpcRequest> requestAdapter = moshi.adapter(JsonRpcRequest.class);
            for (Script out : outputScripts) {
                final Sha256Hash hash = Sha256Hash.of(out.getProgram());
                final JsonRpcRequest request = new JsonRpcRequest(out.getScriptType().ordinal(),
                        "blockchain.scripthash.listunspent",
                        new String[]{Constants.HEX.encode(hash.getReversedBytes())}); // a reversed copy of the internal byte array
                requestAdapter.toJson(sink, request);
                sink.writeUtf8("\n").flush();
            } // request

            final JsonAdapter<JsonRpcResponse> responseAdapter = moshi
                    .adapter(JsonRpcResponse.class);
            final Set<UTXO> utxos = new HashSet<>();
            for (Script out : outputScripts) {
                final JsonRpcResponse response = responseAdapter.fromJson(source);

                final int expectedResponseId = out.getScriptType().ordinal();
                if (null == response || response.id != expectedResponseId) {
                    log.warn("{} - id mismatch response:{} vs request:{}", server.socketAddress,
                            null == response ? -1 : response.id, expectedResponseId);
                    return null;
                }
                if (response.error != null) {
                    log.info("{} - server error {}: {}", server.socketAddress,
                            response.error.code, response.error.message);
                    return null;
                } // response

                // build UTXO
                for (JsonRpcResponse.Utxo respUtxo : response.result) {
                    final Sha256Hash hash = Sha256Hash.wrap(respUtxo.tx_hash);
                    final int index = respUtxo.tx_pos;
                    final Coin value = Coin.valueOf(respUtxo.value);
                    final int height = respUtxo.height;

                    final UTXO utxo = new UTXO(hash, index, value, height, false, out);
                    utxos.add(utxo);
                }
            }// end for

            log.info("{} - got {} UTXOs {}", server.socketAddress, utxos.size(), utxos);
            return utxos;

        } catch (final ConnectException | SSLPeerUnverifiedException | JsonDataException x) {
            log.warn("{} - {}", server.socketAddress, x.getMessage());
            return null;
        } catch (final IOException x) {
            log.info(server.socketAddress.toString(), x);
            return null;
        } catch (final RuntimeException x) {
            log.error(server.socketAddress.toString(), x);
            throw x;
        }
    }

    protected abstract void onResult(Set<UTXO> utxos);

    protected abstract void onFail(int messageResId, Object... messageArgs);
}
