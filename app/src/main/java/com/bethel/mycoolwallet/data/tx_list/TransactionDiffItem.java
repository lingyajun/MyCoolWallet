package com.bethel.mycoolwallet.data.tx_list;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.bethel.mycoolwallet.data.tx_list.item.IListItem;
import com.bethel.mycoolwallet.data.tx_list.item.TransactionListItem;
import com.bethel.mycoolwallet.data.tx_list.item.TransactionWarningItem;
import com.bethel.mycoolwallet.utils.Utils;

import org.bitcoinj.core.Coin;

import java.util.EnumSet;

/**
 * 实现 DiffUtil.ItemCallback<TransactionListItem>
 *
 * ------------------------
 * 代码量大，单独提出来写，使得adapter类简洁一些
 * ------------------------
 */
public class TransactionDiffItem extends DiffUtil.ItemCallback<IListItem> {
    @Override
    public boolean areItemsTheSame(@NonNull IListItem oldItem, @NonNull IListItem newItem) {
        if (oldItem instanceof TransactionListItem) {
            if (!( newItem instanceof TransactionListItem)) {
                return false;
            }
            return Utils.equals(((TransactionListItem) oldItem).getTransactionHash(),
                    ((TransactionListItem) newItem).getTransactionHash());
        }
        if (oldItem instanceof TransactionWarningItem) {
            if (!( newItem instanceof TransactionWarningItem)) {
                return false;
            }
            return Utils.equals(((TransactionWarningItem) oldItem).type,
                    ((TransactionWarningItem) newItem).type);
        }
        return false;
    }

    @Override
    public boolean areContentsTheSame(@NonNull IListItem oldItem, @NonNull IListItem newItem) {
        if (oldItem instanceof TransactionListItem ) {
            if (!( newItem instanceof TransactionListItem)) {
                return false;
            }

            final TransactionListItem txItemOld = (TransactionListItem) oldItem;
            final TransactionListItem txItemNew = (TransactionListItem) newItem;
            // confidence
            final boolean confidence = Utils.equals(txItemOld.getConfidenceCircularFillColor(), txItemNew.getConfidenceCircularFillColor())
                    && Utils.equals(txItemOld.getConfidenceCircularMaxProgress(), txItemNew.getConfidenceCircularMaxProgress())
                    && Utils.equals(txItemOld.getConfidenceCircularMaxSize(), txItemNew.getConfidenceCircularMaxSize())
                    && Utils.equals(txItemOld.getConfidenceCircularProgress(), txItemNew.getConfidenceCircularProgress())
                    && Utils.equals(txItemOld.getConfidenceCircularSize(), txItemNew.getConfidenceCircularSize())
                    && Utils.equals(txItemOld.getConfidenceCircularStrokeColor(), txItemNew.getConfidenceCircularStrokeColor())
                    && Utils.equals(txItemOld.getConfidenceMessage(), txItemNew.getConfidenceMessage())
                    && Utils.equals(txItemOld.getConfidenceTextual(), txItemNew.getConfidenceTextual())
                    && Utils.equals(txItemOld.getConfidenceTextualColor(), txItemNew.getConfidenceTextualColor());

            // time
            final boolean time = Utils.equals(txItemOld.getTime(), txItemNew.getTime())
                    && Utils.equals(txItemOld.getTimeColor(), txItemNew.getTimeColor());

            // address
            final boolean address = Utils.equals(txItemOld.getAddress(), txItemNew.getAddress())
                    && Utils.equals(txItemOld.getAddressColor(), txItemNew.getAddressColor())
                    && Utils.equals(txItemOld.getAddressTypeface(), txItemNew.getAddressTypeface());

            // fee
            final boolean fee = Utils.equals(txItemOld.getFee(), txItemNew.getFee())
                    &&   Utils.equals(null!=txItemOld.getFeeFormat()? txItemOld.getFeeFormat().format(Coin.COIN).toString() :null,
                    null!=txItemNew.getFeeFormat()?  txItemNew.getFeeFormat().format(Coin.COIN).toString():null);

            // value
            final boolean value = Utils.equals(txItemOld.getValue(), txItemNew.getValue())
                    && Utils.equals(txItemOld.getValueColor(), txItemNew.getValueColor())
                    &&   Utils.equals(null!=txItemOld.getValueFormat()? txItemOld.getValueFormat().format(Coin.COIN).toString() :null,
                    null!=txItemNew.getValueFormat()?  txItemNew.getValueFormat().format(Coin.COIN).toString():null);

            // fiat value (ExchangeRate)
            final boolean fiat = Utils.equals(txItemOld.getFiat(), txItemNew.getFiat())
                    && Utils.equals(txItemOld.getFiatPrefixColor(), txItemNew.getFiatPrefixColor())
                    &&   Utils.equals(null!=txItemOld.getFiatFormat()? txItemOld.getFiatFormat().format(Coin.COIN).toString() :null,
                    null!=txItemNew.getFiatFormat()?  txItemNew.getFiatFormat().format(Coin.COIN).toString():null);

            // message
            final boolean message = Utils.equals(txItemOld.getMessage(), txItemNew.getMessage())
                    && txItemOld.getMessageColor() == txItemNew.getMessageColor()
                    && txItemOld.isMessageSingleLine() == txItemNew.isMessageSingleLine();

            final boolean isSelected = txItemOld.isSelected() == txItemNew.isSelected();

            return confidence && time && address && fee && value && fiat && message && isSelected;
        }

        return true;
    }

    @Nullable
    @Override
    public Object getChangePayload(@NonNull IListItem oldItem, @NonNull IListItem newItem) {
        final EnumSet<TransactionChangeType> changes = EnumSet.noneOf(TransactionChangeType.class);
        if (!(oldItem instanceof TransactionListItem) || ! (newItem instanceof TransactionListItem)) {
            return changes;
        }

        final TransactionListItem txItemOld = (TransactionListItem) oldItem;
        final TransactionListItem txItemNew = (TransactionListItem) newItem;
        // confidence
        final boolean confidence = Utils.equals(txItemOld.getConfidenceCircularFillColor(), txItemNew.getConfidenceCircularFillColor())
                && Utils.equals(txItemOld.getConfidenceCircularMaxProgress(), txItemNew.getConfidenceCircularMaxProgress())
                && Utils.equals(txItemOld.getConfidenceCircularMaxSize(), txItemNew.getConfidenceCircularMaxSize())
                && Utils.equals(txItemOld.getConfidenceCircularProgress(), txItemNew.getConfidenceCircularProgress())
                && Utils.equals(txItemOld.getConfidenceCircularSize(), txItemNew.getConfidenceCircularSize())
                && Utils.equals(txItemOld.getConfidenceCircularStrokeColor(), txItemNew.getConfidenceCircularStrokeColor())
                && Utils.equals(txItemOld.getConfidenceMessage(), txItemNew.getConfidenceMessage())
                && Utils.equals(txItemOld.getConfidenceTextual(), txItemNew.getConfidenceTextual())
                && Utils.equals(txItemOld.getConfidenceTextualColor(), txItemNew.getConfidenceTextualColor());
        if (!confidence) {
            changes.add(TransactionChangeType.CONFIDENCE);
        }

        // time
        final boolean time = Utils.equals(txItemOld.getTime(), txItemNew.getTime())
                && Utils.equals(txItemOld.getTimeColor(), txItemNew.getTimeColor());
        if (!time) {
            changes.add(TransactionChangeType.TIME);
        }

        // address
        final boolean address = Utils.equals(txItemOld.getAddress(), txItemNew.getAddress())
                && Utils.equals(txItemOld.getAddressColor(), txItemNew.getAddressColor())
                && Utils.equals(txItemOld.getAddressTypeface(), txItemNew.getAddressTypeface());
        if (!address) {
            changes.add(TransactionChangeType.ADDRESS);
        }

        // fee
        final boolean fee = Utils.equals(txItemOld.getFee(), txItemNew.getFee())
                &&   Utils.equals(null!=txItemOld.getFeeFormat()? txItemOld.getFeeFormat().format(Coin.COIN).toString() :null,
                null!=txItemNew.getFeeFormat()?  txItemNew.getFeeFormat().format(Coin.COIN).toString():null);
        if (!fee) {
            changes.add(TransactionChangeType.FEE);
        }

        // value
        final boolean value = Utils.equals(txItemOld.getValue(), txItemNew.getValue())
                && Utils.equals(txItemOld.getValueColor(), txItemNew.getValueColor())
                &&   Utils.equals(null!=txItemOld.getValueFormat()? txItemOld.getValueFormat().format(Coin.COIN).toString() :null,
                null!=txItemNew.getValueFormat()?  txItemNew.getValueFormat().format(Coin.COIN).toString():null);
        if (!value) {
            changes.add(TransactionChangeType.VALUE);
        }

        // fiat value (ExchangeRate)
        final boolean fiat = Utils.equals(txItemOld.getFiat(), txItemNew.getFiat())
                && Utils.equals(txItemOld.getFiatPrefixColor(), txItemNew.getFiatPrefixColor())
                &&   Utils.equals(null!=txItemOld.getFiatFormat()? txItemOld.getFiatFormat().format(Coin.COIN).toString() :null,
                      null!=txItemNew.getFiatFormat()?  txItemNew.getFiatFormat().format(Coin.COIN).toString():null);

        if (!fiat) {
            changes.add(TransactionChangeType.FIAT);
        }

        // message
        final boolean message = Utils.equals(txItemOld.getMessage(), txItemNew.getMessage())
                && txItemOld.getMessageColor() == txItemNew.getMessageColor()
                && txItemOld.isMessageSingleLine() == txItemNew.isMessageSingleLine();
        if (!message) {
            changes.add(TransactionChangeType.MESSAGE);
        }

        final boolean isSelected = txItemOld.isSelected() == txItemNew.isSelected();
        if (!isSelected) {
            changes.add(TransactionChangeType.IS_SELECTED);
        }
        return changes;
    }
}
