package com.bethel.mycoolwallet.data.tx_list.item;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.format.DateUtils;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.tx_list.ColorType;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.WalletUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.DefaultCoinSelector;
import org.bitcoinj.wallet.Wallet;

import java.util.Date;
import java.util.Map;

/**
 * 交易记录列表项
---------------------
 参数较多，代码量大，故单独提出来写，使得adapter类简洁一些
 ----------------------
 // confidence
 // time
 // address
 // fee
 // value
 // fiat value (ExchangeRate)
 // message

 */
public class TransactionListItem implements IListItem {
    private static final String CONFIDENCE_SYMBOL_IN_CONFLICT = "\u26A0"; // warning sign ⚠ ⚠️
    private static final String CONFIDENCE_SYMBOL_DEAD = "\u271D"; // latin cross ✝️
    private static final String CONFIDENCE_SYMBOL_UNKNOWN = "?";

    private final Transaction tx;
    private final   Context context;
    private final Wallet wallet;
    private final int maxConnectedPeers;
    private final boolean isSelected;
    private  final Map<String, AddressBook> addressBook;
    private final MonetaryFormat monetaryFormat;
    private final Sha256Hash transactionHash;

    private final int baseTextColor, baseLessSignificantColor, baseValueColor;

    public TransactionListItem(final Context context, final Transaction tx, final @Nullable Wallet wallet,
                               final @Nullable Map<String, AddressBook> addressBook, final MonetaryFormat format,
                               final int maxConnectedPeers, final boolean isSelected) {
        this.context = context;
        this.wallet = wallet;
        this.tx = tx;
        this.maxConnectedPeers =maxConnectedPeers;
        this.isSelected = isSelected;
        this.addressBook = addressBook;
        this.monetaryFormat = format;
        this.transactionHash = tx.getTxId();

        if (getConfidenceType() == TransactionConfidence.ConfidenceType.DEAD) {
            // all : colorError
            baseTextColor = ColorType.Error.getColor(context);
            baseLessSignificantColor = baseTextColor;
            baseValueColor = baseTextColor;
        } else if (DefaultCoinSelector.isSelectable(tx)) {
            baseTextColor = ColorType.Significant.getColor(context);
            baseLessSignificantColor = ColorType.LessSignificant.getColor(context);
            baseValueColor = isTxSent() ?
                    ColorType.ValueNegative.getColor(context) : ColorType.ValuePositve.getColor(context);
        } else {
            // all: colorInsignificant
            baseTextColor = ColorType.Insignificant.getColor(context);
            baseLessSignificantColor = baseTextColor;
            baseValueColor = baseTextColor;
        }
        // confidence
        initConfidence();

        // time
        initTimes();

        // address (tx, wallet)
        initAddress();

        // fee
        initFee();

        // value
//        final Coin value = tx.getValue(wallet);
        initTxValue();

        // fiat value (ExchangeRate)
        initExchangeRate();

        // message
        initMessage();
    }

    @Nullable
    private Fiat fiat;
    @Nullable
    private MonetaryFormat fiatFormat;
    private int fiatPrefixColor;
    private void initExchangeRate() {
        final ExchangeRate exchangeRate = tx.getExchangeRate();
        final Coin value = getTxValue();
        if (null!=exchangeRate && null!=value && !value.isZero()) {
            this.fiat = exchangeRate.coinToFiat(value);
            this.fiatFormat = Constants.LOCAL_FORMAT.code(0,
                    Constants.PREFIX_ALMOST_EQUAL_TO + exchangeRate.fiat.getCurrencyCode());
            this.fiatPrefixColor = ColorType.Insignificant.getColor(context);
            return;
        }
        // else ...
        this.fiat = null;
        this.fiatFormat = null;
        this.fiatPrefixColor = 0;
    }

    @Nullable
    private Coin value;
    private MonetaryFormat valueFormat;
    private int valueColor;
    private void initTxValue() {
        final Coin fee = tx.getFee();
        final Coin val = getTxValue();
        final Transaction.Purpose purpose = tx.getPurpose();

        this.valueFormat = monetaryFormat;
        if (Transaction.Purpose.RAISE_FEE==purpose) {
            this.valueColor = ColorType.Insignificant.getColor(context);
            this.value = null!=fee ? fee.negate(): null;
            return;
        }

        if (null==val || val.isZero()) {
            this.valueColor = 0;
            this.value = null;
            return;
        }

        // else ..
        this.valueColor = baseValueColor;

        final boolean showFee = fee != null && !fee.isZero() && isTxSent() ;
        this.value = showFee ?  val.add(fee): val;
    }

    @Nullable
    private Coin fee;
    private MonetaryFormat feeFormat;
    private void initFee() {
        final boolean sent = isTxSent();
        final Coin fee = tx.getFee();
        final boolean showFee = sent && fee != null && !fee.isZero();
        this.feeFormat = monetaryFormat;
        this.fee = isSelected && showFee ? fee.negate() : null;
    }

    @Nullable
    private Spanned address;
    private int addressColor;
    private Typeface addressTypeface;
    private boolean addressSingleLine;
    private void initAddress() {
        final boolean sent = isTxSent();
        final Address address = sent ? WalletUtils.getToAddressOfSent(tx, wallet)
                : WalletUtils.getWalletAddressOfReceived(tx, wallet);
        final String addressLabel;
        if (addressBook == null || address == null) {
            addressLabel = null;
        } else {
            final AddressBook entry = addressBook.get(address.toString());
            if (entry != null)
                addressLabel = entry.getLabel();
            else
                addressLabel = null;
        }

        this.addressSingleLine = !isSelected;
        if (tx.isCoinBase()) {
            // 挖矿 奖励
            this.address = SpannedString
                    .valueOf(context.getString(R.string.wallet_transactions_fragment_coinbase));
            this.addressColor = baseTextColor;
            this.addressTypeface = Typeface.DEFAULT_BOLD;
            return;
        }

        final Transaction.Purpose purpose = tx.getPurpose();
        if (purpose == Transaction.Purpose.RAISE_FEE) {
            this.address = null;
            this.addressColor = 0;
            this.addressTypeface = Typeface.DEFAULT;
            return;
        }

        final boolean self = WalletUtils.isEntirelySelf(tx, wallet);
        if (purpose == Transaction.Purpose.KEY_ROTATION || self) {
            // 钱包内部 资金转换
            this.address = SpannedString.valueOf(context.getString(R.string.symbol_internal) + " "
                    + context.getString(R.string.wallet_transactions_fragment_internal));
            this.addressColor = baseLessSignificantColor;
            this.addressTypeface = Typeface.DEFAULT_BOLD;
            return;
        }

        if (addressLabel != null) {
            this.address = SpannedString.valueOf(addressLabel);
            this.addressColor = baseTextColor;
            this.addressTypeface = Typeface.DEFAULT_BOLD;
            return;
        }

        final String[] memo = getTxMemo();
        if (memo != null && memo.length >= 2) {
            this.address = SpannedString.valueOf(memo[1]);
            this.addressColor = baseTextColor;
            this.addressTypeface = Typeface.DEFAULT_BOLD;
            return;
        }

        if (address != null) {
            this.address = WalletUtils.formatAddress(address, Constants.ADDRESS_FORMAT_GROUP_SIZE,
                    Constants.ADDRESS_FORMAT_LINE_SIZE);
            this.addressColor = baseLessSignificantColor;
            this.addressTypeface = Typeface.DEFAULT;
            return;
        }

        // else ...
        this.address = SpannedString.valueOf("?");
        this.addressColor = baseLessSignificantColor;
        this.addressTypeface = Typeface.DEFAULT;
    }

    private CharSequence time;
    private int timeColor;
    private void initTimes() {
        final Date time = tx.getUpdateTime();
        this.time = isSelected
                ? DateUtils.formatDateTime(context, time.getTime(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME)
                : DateUtils.getRelativeTimeSpanString(context, time.getTime());
        this.timeColor = baseTextColor;
    }

    @Nullable
    private Spanned message;
    private int messageColor;
    private boolean messageSingleLine;
    private void initMessage() {
        final Transaction.Purpose purpose = tx.getPurpose();
        switch (purpose) {
            /* 提高费用. Raise fee, e.g. child-pays-for-parent. */
            case RAISE_FEE:
                this.message = SpannedString
                        .valueOf(context.getString(R.string.transaction_row_message_purpose_raise_fee));
                this.messageColor = ColorType.Insignificant.getColor(context);
                this.messageSingleLine = false;
                return;

            /* [rotation 轮换，旋转] in order to reallocate(重新分配) money from old to new keys. */
            case KEY_ROTATION:
                this.message = Html
                        .fromHtml(context.getString(R.string.transaction_row_message_purpose_key_rotation));
                this.messageColor = ColorType.Significant.getColor(context);
                this.messageSingleLine = false;
                return;

            /* Transaction created to satisfy(满足) a user payment request. */
            case USER_PAYMENT:break;

            /* Send-to-self transaction that exists just to create an output of the right size we can pledge. */
            case ASSURANCE_CONTRACT_STUB:break;

            /* [claim 声称；宣称]使用了担保合同质押的交易
             Transaction that uses up pledges(承诺,质押) to an assurance(保证) contract(合约) */
            case ASSURANCE_CONTRACT_CLAIM:break;

            /* Transaction that makes a pledge to an assurance contract. */
            case ASSURANCE_CONTRACT_PLEDGE:break;
            case UNKNOWN:break;
        }

        final TransactionConfidence confidence = tx.getConfidence();
        final TransactionConfidence.ConfidenceType confidenceType = confidence.getConfidenceType();
        final boolean sent = isTxSent();
        final boolean isOwn = isTxCreatedByOwnWallet();
        if (!isOwn && confidenceType == TransactionConfidence.ConfidenceType.PENDING
                && confidence.numBroadcastPeers() <1) {
            // 还没有广播出去，交易信息是被直接接收的
            this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_row_message_received_direct));
            this.messageColor = ColorType.Insignificant.getColor(context);
            this.messageSingleLine = false;
            return;
        }

        final Coin value = getTxValue();
        if (!sent && value.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0) {
            // "尘埃攻击"
            this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_row_message_received_dust));
            this.messageColor = ColorType.Insignificant.getColor(context);
            this.messageSingleLine = false;
            return;
        }

//        if (!sent && confidenceType == TransactionConfidence.ConfidenceType.PENDING
//                && (tx.getUpdateTime() == null || wallet.getLastBlockSeenTimeSecs() * 1000
//                - tx.getUpdateTime().getTime() > Constants.DELAYED_TRANSACTION_THRESHOLD_MS)) {
//            this.message = SpannedString
//                    .valueOf(context.getString(R.string.transaction_row_message_received_unconfirmed_delayed));
//            this.messageColor = ColorType.Insignificant.getColor(context);
//            this.messageSingleLine = false;
//            return;
//        }

        switch (confidenceType) {
            case PENDING:
                if (!sent) {
                    if ((tx.getUpdateTime() == null || wallet.getLastBlockSeenTimeSecs() * 1000
                            - tx.getUpdateTime().getTime() > Constants.DELAYED_TRANSACTION_THRESHOLD_MS)) {
                        // 此付款的确认被延迟
                        this.message = SpannedString.valueOf(
                                context.getString(R.string.transaction_row_message_received_unconfirmed_delayed));
                        this.messageColor = ColorType.Insignificant.getColor(context);
                        this.messageSingleLine = false;
                        return;
                    }
                    // 此项付款所获得的比特币将在几分钟之后可以使用
                    this.message = SpannedString.valueOf(
                            context.getString(R.string.transaction_row_message_received_unconfirmed_unlocked));
                    this.messageColor = ColorType.Insignificant.getColor(context);
                    this.messageSingleLine = false;
                    return;
                }
                break;
            case IN_CONFLICT:
                if (!sent) {
                    // 冲突, 等待确认
                    this.message = SpannedString.valueOf(
                            context.getString(R.string.transaction_row_message_received_in_conflict));
                    this.messageColor = ColorType.Insignificant.getColor(context);
                    this.messageSingleLine = false;
                    return;
                }
                break;
            case DEAD:
                if (!sent) {
                    // 被付款人撤销
                    this.message = SpannedString
                            .valueOf(context.getString(R.string.transaction_row_message_received_dead));
                    this.messageColor = ColorType.Error.getColor(context);
                    this.messageSingleLine = false;
                    return;
                }
                break;
        }

        if (!sent && WalletUtils.isPayToManyTransaction(tx)) {
            this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_row_message_received_pay_to_many));
            this.messageColor = ColorType.Insignificant.getColor(context);
            this.messageSingleLine = false;
            return;
        }

        if (!sent && tx.isOptInFullRBF()) {
            // 使用不安全的交易类型
            this.message = SpannedString
                    .valueOf(context.getString(R.string.transaction_row_message_received_rbf));
            this.messageColor = ColorType.Insignificant.getColor(context);
            this.messageSingleLine = false;
            return;
        }

        // memo
        final String[] memo = getTxMemo();
        if (memo != null && memo.length>0) {
            this.message = SpannedString.valueOf(memo[0]);
            this.messageColor = ColorType.Insignificant.getColor(context);
            this.messageSingleLine = isSelected;
            return;
        }
        // else ...
        this.message = null;
        this.messageColor = 0;
        this.messageSingleLine = false;
    }


    private int confidenceCircularProgress, confidenceCircularMaxProgress;
    private int confidenceCircularSize, confidenceCircularMaxSize;
    private int confidenceCircularFillColor, confidenceCircularStrokeColor;
    @Nullable
    private String confidenceTextual;
    private int confidenceTextualColor;
    @Nullable
    private Spanned confidenceMessage;
    private void initConfidence() {
        final TransactionConfidence confidence = tx.getConfidence();
        final TransactionConfidence.ConfidenceType confidenceType = confidence.getConfidenceType();
        final boolean sent = isTxSent();

        switch (confidenceType) {
            // the transaction is unconfirmed
            case PENDING:
                this.confidenceCircularMaxProgress = 1;
                this.confidenceCircularProgress = 1;
                this.confidenceCircularMaxSize = maxConnectedPeers / 2; // magic value
                this.confidenceCircularSize = confidence.numBroadcastPeers();
                this.confidenceCircularFillColor = ColorType.Insignificant.getColor(context);
                this.confidenceCircularStrokeColor = Color.TRANSPARENT;
                this.confidenceTextual = null;
                this.confidenceTextualColor = 0;
                this.confidenceMessage = sent && confidence.numBroadcastPeers() == 0
                        ? SpannedString.valueOf(
                        context.getString(R.string.transaction_row_confidence_message_sent_unbroadcasted))
                        : null;
                break;

            // the transaction is included in the best chain
            case BUILDING:
                this.confidenceCircularMaxProgress = tx.isCoinBase()
                        ? Constants.NETWORK_PARAMETERS.getSpendableCoinbaseDepth()
                        : Constants.MAX_NUM_CONFIRMATIONS;
                this.confidenceCircularProgress = Math.min(confidence.getDepthInBlocks(),
                        this.confidenceCircularMaxProgress);
                this.confidenceCircularMaxSize = 1;
                this.confidenceCircularSize = 1;
                this.confidenceCircularFillColor = baseValueColor;
                this.confidenceCircularStrokeColor = Color.TRANSPARENT;
                this.confidenceTextual = null;
                this.confidenceTextualColor = 0;
                this.confidenceMessage = isSelected ? SpannedString.valueOf(
                        context.getString(sent ? R.string.transaction_row_confidence_message_sent_successful
                                : R.string.transaction_row_confidence_message_received_successful))
                        : null;
                break;

            // [conflict 冲突]
            // there is another transaction (or several other transactions) spending one
            // (or several) of its inputs
            // but nor this transaction nor the other/s transaction/s are included in the best chain.
            case IN_CONFLICT:
                this.confidenceTextual = CONFIDENCE_SYMBOL_IN_CONFLICT;
                this.confidenceTextualColor = ColorType.Error.getColor(context);
                this.confidenceCircularMaxProgress = 0;
                this.confidenceCircularProgress = 0;
                this.confidenceCircularMaxSize = 0;
                this.confidenceCircularSize = 0;
                this.confidenceCircularFillColor = 0;
                this.confidenceCircularStrokeColor = 0;
                this.confidenceMessage = null;
                break;

            // a transaction hasn't been broadcast yet, or there's no record of it.
            case UNKNOWN:
                this.confidenceTextual = CONFIDENCE_SYMBOL_UNKNOWN;
                this.confidenceTextualColor = ColorType.Insignificant.getColor(context);
                this.confidenceCircularMaxProgress = 0;
                this.confidenceCircularProgress = 0;
                this.confidenceCircularMaxSize = 0;
                this.confidenceCircularSize = 0;
                this.confidenceCircularFillColor = 0;
                this.confidenceCircularStrokeColor = 0;
                this.confidenceMessage = null;
                break;

            // the transaction won't confirm
            case DEAD:
                this.confidenceTextual = CONFIDENCE_SYMBOL_DEAD;
                this.confidenceTextualColor = ColorType.Error.getColor(context);
                this.confidenceCircularMaxProgress = 0;
                this.confidenceCircularProgress = 0;
                this.confidenceCircularMaxSize = 0;
                this.confidenceCircularSize = 0;
                this.confidenceCircularFillColor = 0;
                this.confidenceCircularStrokeColor = 0;
                this.confidenceMessage = SpannedString
                        .valueOf(context.getString(sent ? R.string.transaction_row_confidence_message_sent_failed
                                : R.string.transaction_row_confidence_message_received_failed));
                break;
        }
    }

    private TransactionConfidence.ConfidenceType getConfidenceType(){
        final TransactionConfidence confidence = tx.getConfidence();
        return confidence.getConfidenceType();
    }
    /**
     * 该交易是否是(本钱包)发送coin
     * @return
     */
    public boolean isTxSent() {
        final Coin value = tx.getValue(wallet);
//        final boolean sent = value.signum() < 0;
        return value.signum() < 0;
    }

    /**
     * This transaction was created by our own wallet,
     * so we know it's not a double spend.
     * @return
     */
    private boolean isTxCreatedByOwnWallet() {
        final TransactionConfidence confidence = tx.getConfidence();
        return confidence.getSource().equals(TransactionConfidence.Source.SELF);
    }

    private Coin getTxValue() {
        return tx.getValue(wallet);
    }

    private String[] getTxMemo() {
        return Commons.Formats.sanitizeMemo(tx.getMemo());
    }

    //////////////////////////////////////////////////
    /////////////////  getter //////////////////////
    //////////////////////////////////////////////////

    public Sha256Hash getTransactionHash() {
        return transactionHash;
    }

    public boolean isSelected() {
        return isSelected;
    }

    @Nullable
    public Fiat getFiat() {
        return fiat;
    }

    @Nullable
    public MonetaryFormat getFiatFormat() {
        return fiatFormat;
    }

    public int getFiatPrefixColor() {
        return fiatPrefixColor;
    }

    @Nullable
    public Coin getValue() {
        return value;
    }

    public MonetaryFormat getValueFormat() {
        return valueFormat;
    }

    public int getValueColor() {
        return valueColor;
    }

    @Nullable
    public Coin getFee() {
        return fee;
    }

    public MonetaryFormat getFeeFormat() {
        return feeFormat;
    }

    @Nullable
    public Spanned getAddress() {
        return address;
    }

    public int getAddressColor() {
        return addressColor;
    }

    public Typeface getAddressTypeface() {
        return addressTypeface;
    }

    public boolean isAddressSingleLine() {
        return addressSingleLine;
    }

    public CharSequence getTime() {
        return time;
    }

    public int getTimeColor() {
        return timeColor;
    }

    @Nullable
    public Spanned getMessage() {
        return message;
    }

    public int getMessageColor() {
        return messageColor;
    }

    public boolean isMessageSingleLine() {
        return messageSingleLine;
    }

    public int getConfidenceCircularProgress() {
        return confidenceCircularProgress;
    }

    public int getConfidenceCircularMaxProgress() {
        return confidenceCircularMaxProgress;
    }

    public int getConfidenceCircularSize() {
        return confidenceCircularSize;
    }

    public int getConfidenceCircularMaxSize() {
        return confidenceCircularMaxSize;
    }

    public int getConfidenceCircularFillColor() {
        return confidenceCircularFillColor;
    }

    public int getConfidenceCircularStrokeColor() {
        return confidenceCircularStrokeColor;
    }

    @Nullable
    public String getConfidenceTextual() {
        return confidenceTextual;
    }

    public int getConfidenceTextualColor() {
        return confidenceTextualColor;
    }

    @Nullable
    public Spanned getConfidenceMessage() {
        return confidenceMessage;
    }
}
