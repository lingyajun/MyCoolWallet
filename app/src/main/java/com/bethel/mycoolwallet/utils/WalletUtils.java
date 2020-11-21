package com.bethel.mycoolwallet.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.TypefaceSpan;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.interfaces.IWalletBackupCallback;
import com.bethel.mycoolwallet.interfaces.IWalletRestoreCallback;
import com.google.common.base.Stopwatch;
import com.google.common.io.CharStreams;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletProtobufSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WalletUtils {
    private static final Logger log = LoggerFactory.getLogger(WalletUtils.class);

    public static void autoBackupWallet(final Context context, final Wallet wallet) {
        final Stopwatch watch = Stopwatch.createStarted();
        final Protos.Wallet.Builder builder = new WalletProtobufSerializer().walletToProto(wallet).toBuilder();

        // strip redundant
        builder.clearTransaction();
        builder.clearLastSeenBlockHash();
        builder.setLastSeenBlockHeight(-1);
        builder.clearLastSeenBlockTimeSecs();
        final Protos.Wallet walletProto = builder.build();

        try (final OutputStream os = context.openFileOutput(Constants.Files.WALLET_KEY_BACKUP_PROTOBUF,
                Context.MODE_PRIVATE)) {
            walletProto.writeTo(os);
            watch.stop();
            log.info("wallet backed up to: '{}', took {}", Constants.Files.WALLET_KEY_BACKUP_PROTOBUF, watch);
        } catch (final IOException x) {
            log.error("problem writing wallet backup", x);
        }
    }

    /**
     * 将钱包{ wallet } 写到目标文件{ targetUri }中。
     *
     * 请求创建文件 返回时调用。
     */
    public static void backupWallet2FileSystem(ContentResolver resolver,final Wallet wallet,
                                               final String password,final Uri targetUri,
                                               IWalletBackupCallback callback) {
        byte[] plainBytes = null;
        try {
            // Converts the given wallet to the object representation of the protocol buffers.
            Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(wallet);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            walletProto.writeTo(baos);
            baos.close();

            plainBytes = baos.toByteArray();
            String cipherText = Crypto.encrypt(plainBytes, password.toCharArray());
            Writer cipherOut = new OutputStreamWriter(resolver.openOutputStream(targetUri), Commons.UTF_8);
            cipherOut.write(cipherText);
            cipherOut.flush();

            final String target = getFileStorageName(targetUri);
            log.info("backed up wallet to: '{}'{}, {} characters written", targetUri,
                    target != null ? " (" + target + ")" : "", cipherText.length());
        } catch (IOException e) {
            log.error("problem backing up wallet to " + targetUri, e);
            if (null!=callback) {
                callback.onFailed(e);
            }
            return;
        }

        byte[] plainBytes2 = null;
        try {
            Reader cipherIn = new InputStreamReader(resolver.openInputStream(targetUri), Commons.UTF_8);
            StringBuilder cipherBuilder = new StringBuilder();
            CharStreams.copy(cipherIn, cipherBuilder);
            cipherIn.close();
            // verify ...先写文件，再读取刚刚写的文件，校验两次数据的一致性。
            plainBytes2 = Crypto.decryptBytes(cipherBuilder.toString(), password.toCharArray());
            if (!Arrays.equals(plainBytes, plainBytes2)) throw new IOException("verification failed");

            log.info("verified successfully: '" + targetUri + "'");
            if (null!=callback) {
                callback.onSuccess();
            }
        } catch (IOException e) {
            log.error("problem verifying backup from " + targetUri, e);
            if (null!=callback) {
                callback.onFailed(e);
            }
        }
    }

    public static void testRestoreWallet(Context context) {
        final String backupPath = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.getAbsolutePath();
        final String storagePath = Constants.Files.EXTERNAL_STORAGE_DIR.getAbsolutePath();
        // backupPath: /storage/emulated/0/Download  ; storagePath: /storage/emulated/0
        log.info("testRestoreWallet, backupPath: {}  ; storagePath: {}", backupPath, storagePath);

        // external storage
        log.info("testRestoreWallet, looking for backup files in '{}'", Constants.Files.EXTERNAL_WALLET_BACKUP_DIR);
        final File[] externalFiles = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.listFiles();
        if (null!= externalFiles) {
            for (File file: externalFiles ) {
                log.info("testRestoreWallet, external storage file: {}", file.getName());
            }
        }

        //  app-private storage
        for (final String filename : context.fileList()) {
            log.info("testRestoreWallet, app-private storage file: {}", filename );
        }

    }

    public static  void restoreWalletFromProtobuf(File file, IWalletRestoreCallback callback) {
        try {
            FileInputStream is = new FileInputStream(file);
            Wallet restoredWallet = restoreWalletFromProtobuf(is, Constants.NETWORK_PARAMETERS);
            if (null!=callback) callback.onSuccess(restoredWallet);
        } catch (UnreadableWalletException | IOException e) {
            if (null!=callback) callback.onFailed(e);
            log.info("problem restoring unencrypted wallet: " + file, e);
        }
    }
    public static Wallet restoreWalletFromProtobuf(final InputStream is,
                                                   final NetworkParameters expectedNetworkParameters)
            throws IOException, UnreadableWalletException {
        Wallet wallet = new WalletProtobufSerializer().readWallet(is, true, null);
        if (!wallet.getParams().equals(expectedNetworkParameters)) {
            throw new IOException("bad wallet backup network parameters: " +wallet.getParams().getId());
        }
        if (!wallet.isConsistent()) {
            throw new IOException("inconsistent wallet backup");
        }
        return wallet;
    }

    public static void restoreWalletFromEncrypted(final File file, final String password, IWalletRestoreCallback callback) {
        try {
            FileInputStream is = new FileInputStream(file);
            InputStreamReader inReader = new InputStreamReader(is, Commons.UTF_8);
            BufferedReader cipherIn = new BufferedReader(inReader);

            StringBuilder cipherText = new StringBuilder();
            CharStreams.copy(cipherIn, cipherText);
            cipherIn.close();

            // 将钱包文件解密
            byte[] plainText = Crypto.decryptBytes(cipherText.toString(), password.toCharArray());
            InputStream inputStream = new ByteArrayInputStream(plainText);
            // 将解密后的钱包文件 恢复
            Wallet restoredWallet = restoreWalletFromProtobuf(inputStream, Constants.NETWORK_PARAMETERS);
            if (null!=callback) callback.onSuccess(restoredWallet);

        }  catch (IOException | UnreadableWalletException e) {
            if (null!=callback) callback.onFailed(e);
            log.info("problem restoring encrypted wallet: " + file, e);
        }
    }

    public static Address getWalletAddressOfReceived(final Transaction tx, final Wallet wallet) {
        for (TransactionOutput output: tx.getOutputs() ) {
            try {
                if (output.isMine(wallet)) {
                    Script script = output.getScriptPubKey();
                    return script.getToAddress(Constants.NETWORK_PARAMETERS, true);
                }
            } catch (ScriptException e) {
                log.error("getWalletAddressOfReceived: {}", e.getMessage());
            }

        }
        return null;
    }

    public static String getFileStorageName(Uri fileUri) {
        if (!fileUri.getScheme().equals("content"))
            return null;
        final String host = fileUri.getHost();
        if ("com.google.android.apps.docs.storage".equals(host))
            return "Google Drive";
        if ("com.box.android.documents".equals(host))
            return "Box";
        if ("com.android.providers.downloads.documents".equals(host))
            return "internal storage";
        return null;
    }

    public static final FileFilter BACKUP_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File file) {
            try (final InputStream is = new FileInputStream(file)) {
                return WalletProtobufSerializer.isWallet(is);
            } catch (final IOException x) {
                return false;
            }
        }
    };

    public static Spanned formatHash(final String hash, final int groupSize, final int lineSize) {
        return formatHash(null, hash, groupSize, lineSize, Constants.CHAR_THIN_SPACE);
    }

    public static Spanned formatHash(@Nullable final String prefix, final String hash, final int groupSize,
                                     final int lineSize, final char groupSeparator) {
        final SpannableStringBuilder builder = prefix != null ? new SpannableStringBuilder(prefix)
                : new SpannableStringBuilder();

        final int len = hash.length();
        for (int i = 0; i < len; i += groupSize) {
            final int end = i + groupSize;
            final String part = hash.substring(i, end < len ? end : len);

            builder.append(part);
            builder.setSpan(new MonospaceSpan(), builder.length() - part.length(), builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (end < len) {
                final boolean endOfLine = lineSize > 0 && end % lineSize == 0;
                builder.append(endOfLine ? '\n' : groupSeparator);
            }
        }

        return SpannedString.valueOf(builder);
    }

    private static class MonospaceSpan extends TypefaceSpan {
        public MonospaceSpan() {
            super("monospace");
        }

        // TypefaceSpan doesn't implement this, and we need it so that Spanned.equals() works.
        @Override
        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (o == null || o.getClass() != getClass())
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @Nullable
    public static Address getToAddressOfSent(final Transaction tx, final Wallet wallet) {
        for (final TransactionOutput output : tx.getOutputs()) {
            try {
                if (!output.isMine(wallet)) {
                    final Script script = output.getScriptPubKey();
                    return script.getToAddress(Constants.NETWORK_PARAMETERS, true);
                }
            } catch (final ScriptException x) {
                // swallow
            }
        }

        return null;
    }

    public static boolean isEntirelySelf(final Transaction tx, final Wallet wallet) {
        for (final TransactionInput input : tx.getInputs()) {
            final TransactionOutput connectedOutput = input.getConnectedOutput();
            if (connectedOutput == null || !connectedOutput.isMine(wallet))
                return false;
        }

        for (final TransactionOutput output : tx.getOutputs()) {
            if (!output.isMine(wallet))
                return false;
        }

        return true;
    }

}
