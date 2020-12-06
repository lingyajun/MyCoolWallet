package com.bethel.mycoolwallet.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.fragment.dialog.RaiseFeeDialogFragment;
import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.Qr;
import com.bethel.mycoolwallet.utils.WalletUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.wallet.Wallet;

public class TransactionPopupMenu{
    private final Wallet wallet;
    private final Transaction tx;
    private final AddressBookDao addressBookDao;

    private final PopupMenu popupMenu;
    private static final int SHOW_QR_THRESHOLD_BYTES = 2500;
    private static final String KEY_ROTATION_URL =  "https://bitcoin.org/en/alert/2013-08-11-android" ;

    private OnTxMenuItemClickListener itemClickListener;

    public TransactionPopupMenu(@NonNull Context context, @NonNull View anchor, final Wallet wallet,
                                final Sha256Hash transactionHash, final AddressBookDao dao) {
        this.wallet = wallet;
        this.tx = wallet.getTransaction(transactionHash);
        this.addressBookDao = dao;

        popupMenu = new PopupMenu(context, anchor);

        if (null!= tx) {
            popupMenu.inflate(R.menu.wallet_transactions_menu_context);
            initMenu();
        }
    }

    private void initMenu() {
        final boolean txSent = tx.getValue(wallet).signum() < 0;
        final Address txAddress = txSent ? WalletUtils.getToAddressOfSent(tx, wallet)
                : WalletUtils.getWalletAddressOfReceived(tx, wallet);

        final byte[] txSerialized = tx.unsafeBitcoinSerialize();
        final boolean txRotation = tx.getPurpose() == Transaction.Purpose.KEY_ROTATION;

        popupMenu.getMenu().findItem(R.id.wallet_transactions_context_show_qr)
                .setVisible(!txRotation && txSerialized.length < SHOW_QR_THRESHOLD_BYTES);
        popupMenu.getMenu().findItem(R.id.wallet_transactions_context_raise_fee)
                .setVisible(RaiseFeeDialogFragment.feeCanLikelyBeRaised(wallet, tx));
        popupMenu.getMenu().findItem(R.id.wallet_transactions_context_browse).setVisible(Constants.ENABLE_BROWSE);

        final MenuItem editAddressMenuItem = popupMenu.getMenu()
                .findItem(R.id.wallet_transactions_context_edit_address);
        if (txRotation || null== txAddress) {
            editAddressMenuItem.setVisible(false);
          //  return;
        } else {
            editAddressMenuItem.setVisible(true);
            final boolean isAdd = null == addressBookDao || TextUtils.isEmpty(addressBookDao.resolveLabel(txAddress.toString()));
            final boolean isOwn = wallet.isAddressMine(txAddress);
            if (isOwn) {
                editAddressMenuItem.setTitle(isAdd ? R.string.edit_address_book_entry_dialog_title_add_receive
                        : R.string.edit_address_book_entry_dialog_title_edit_receive);
            } else {
                editAddressMenuItem.setTitle(isAdd ? R.string.edit_address_book_entry_dialog_title_add
                        : R.string.edit_address_book_entry_dialog_title_edit);
            }
        }

        // ItemClickListener
        popupMenu.setOnMenuItemClickListener(menuItem -> {

            if (null == itemClickListener ) return false;
            final int id = menuItem.getItemId();
            switch (id) {
                case R.id.wallet_transactions_context_edit_address:
                    // txAddress
                    itemClickListener.onEditAddress(txAddress);
                    break;
                case R.id.wallet_transactions_context_show_qr:
                    // Bitmap
                    final String qrStr = Qr.encodeCompressBinary(txSerialized);
                    final Bitmap qrCodeBitmap = Qr.bitmap(qrStr);
                    itemClickListener.onShowQr(qrCodeBitmap);
                    break;
                case R.id.wallet_transactions_context_raise_fee:
                    // tx
                    itemClickListener.onRaiseFee(tx);
                    break;
                case R.id.wallet_transactions_context_report_issue:
                    // string
                    itemClickListener.onReportIssue(getReportIssue());
                    break;
                case R.id.wallet_transactions_context_browse:
                    // url
                    final String url;
                    if (txRotation) {
                        url = KEY_ROTATION_URL;
                    } else {
                        url = getTxExplorerUrl();
                    }
                    itemClickListener.onTxExplorer(url);
                    break;
                    default: return false;
            }
            return true;
        });

    }
    private String getReportIssue() {
        final StringBuilder builder = new StringBuilder();

        try {
            builder.append(tx.getValue(wallet).toFriendlyString()).append(" total value");
        } catch (ScriptException e) {
            builder.append(e.getMessage());
        }

        builder.append('\n');
        if (tx.hasConfidence()) {
            builder.append("  confidence: ").append(tx.getConfidence()).append('\n');
        }

        builder.append(getTxExplorerUrl()).append('\n');
        builder.append(tx.toString());
        return builder.toString();
    }

    // https://www.blockchain.com/btc/tx/ce4511165f378f0ba6421eacc291c965691ed0dc9fc8fa828a20cdd32479e8e5
    private String getTxExplorerUrl() {
        return String.format("%stx/%s" , Commons.BLOCK_CHAIN_VIEW, tx.getTxId().toString());
//        return   Uri.withAppendedPath(Uri.parse(Commons.BLOCK_CHAIN_VIEW), "tx/" + tx.getTxId().toString()).toString();
    }

    public void setOnMenuItemClickListener(OnTxMenuItemClickListener listener) {
        itemClickListener = listener;
    }

    public void show() {
        if (null== tx) return;
        popupMenu.show();
       // super.show();
    }

    public interface OnTxMenuItemClickListener {
        void onEditAddress(Address address);
        void onShowQr(Bitmap bitmap);
        void onRaiseFee(Transaction tx);
        void onReportIssue(String issue);
        void onTxExplorer(String url);
    }
}
