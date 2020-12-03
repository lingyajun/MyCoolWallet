package com.bethel.mycoolwallet.data.tx_list.holder;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.tx_list.TransactionListItem;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.ViewUtil;
import com.bethel.mycoolwallet.view.CircularProgressView;

/**
 * RecyclerView.ViewHolder
 * [ R.layout.transaction_row_card ]
 *
 * ------------------------
 * 代码量大，单独提出来写，使得adapter类简洁一些
 * ------------------------
 */
public class TransactionViewHolder extends RecyclerView.ViewHolder {
    private final int colorBackground;
    private final int colorBackgroundSelected;

    private final View extendTimeView;
    private final TextView fullTimeView;
    private final View extendAddressView;
    private final CircularProgressView confidenceCircularNormalView, confidenceCircularSelectedView;
    private final TextView confidenceTextualNormalView, confidenceTextualSelectedView;
    private final View extendConfidenceMessageNormalView, extendConfidenceMessageSelectedView;
    private final TextView confidenceMessageNormalView, confidenceMessageSelectedView;
    private final TextView timeView;
    private final TextView addressView;
    private final TextView valueView;
    private final TextView fiatView;
//    private final CurrencyTextView valueView;
//    private final CurrencyTextView fiatView;
    private final View extendFeeView;
    private final TextView feeView;
//    private final CurrencyTextView feeView;
    private final View extendMessageView;
    private final TextView messageView;
    public final ImageButton menuView;

    public TransactionViewHolder(@NonNull View itemView) {
        super(itemView);
        final Context context = itemView.getContext();
        this.colorBackground = ContextCompat.getColor(context, R.color.bg_level2);
        this.colorBackgroundSelected = ContextCompat.getColor(context, R.color.bg_level3);

        this.extendTimeView = itemView.findViewById(R.id.transaction_row_extend_time);
        this.fullTimeView = (TextView) itemView.findViewById(R.id.transaction_row_full_time);
        this.extendAddressView = itemView.findViewById(R.id.transaction_row_extend_address);
        this.confidenceCircularNormalView = (CircularProgressView) itemView
                .findViewById(R.id.transaction_row_confidence_circular);
        this.confidenceCircularSelectedView = (CircularProgressView) itemView
                .findViewById(R.id.transaction_row_confidence_circular_selected);
        this.confidenceTextualNormalView = (TextView) itemView
                .findViewById(R.id.transaction_row_confidence_textual);
        this.confidenceTextualSelectedView = (TextView) itemView
                .findViewById(R.id.transaction_row_confidence_textual_selected);
        this.extendConfidenceMessageNormalView = itemView
                .findViewById(R.id.transaction_row_extend_confidence_message);
        this.extendConfidenceMessageSelectedView = itemView
                .findViewById(R.id.transaction_row_extend_confidence_message_selected);
        this.confidenceMessageNormalView = (TextView) itemView
                .findViewById(R.id.transaction_row_confidence_message);
        this.confidenceMessageSelectedView = (TextView) itemView
                .findViewById(R.id.transaction_row_confidence_message_selected);
        this.timeView = (TextView) itemView.findViewById(R.id.transaction_row_time);
        this.addressView = (TextView) itemView.findViewById(R.id.transaction_row_address);
        this.valueView =   itemView.findViewById(R.id.transaction_row_value);
        this.fiatView =   itemView.findViewById(R.id.transaction_row_fiat);
        this.extendFeeView = itemView.findViewById(R.id.transaction_row_extend_fee);
        this.feeView =  itemView.findViewById(R.id.transaction_row_fee);
        this.extendMessageView = itemView.findViewById(R.id.transaction_row_extend_message);
        this.messageView = (TextView) itemView.findViewById(R.id.transaction_row_message);
        this.menuView = (ImageButton) itemView.findViewById(R.id.transaction_row_menu);
    }

    public void bind(final TransactionListItem item) {
        bindConfidence(item);
        bindTime(item);
        bindAddress(item);
        bindFee(item);
        bindValue(item);
        bindFiat(item);
        bindMessage(item);
        bindIsSelected(item);
    }

    public void bindIsSelected(final TransactionListItem item) {
        if (itemView instanceof CardView) {
            ((CardView) itemView).setCardBackgroundColor(
                    item.isSelected() ? colorBackgroundSelected : colorBackground);
        }
//        menuView.setVisibility(item.isSelected ? View.VISIBLE : View.GONE);
        ViewUtil.showView(menuView, item.isSelected());

        bindConfidence(item);
        bindTime(item);
        bindAddress(item);
    }
    public void bindMessage(final TransactionListItem item) {
        ViewUtil.showView(extendMessageView,null!=item.getMessage());
//        extendMessageView.setVisibility(item.message != null ? View.VISIBLE : View.GONE);
        messageView.setText(item.getMessage());
        messageView.setTextColor(item.getMessageColor());
        messageView.setSingleLine(item.isMessageSingleLine());
    }

    public void bindFiat(final TransactionListItem item) {
        ViewUtil.showView(fiatView,null!=item.getFiat());
//        fiatView.setVisibility(item.fiat != null ? View.VISIBLE : View.GONE);
//        fiatView.setAlwaysSigned(true);
//        fiatView.setAmount(item.fiat);
//        fiatView.setFormat(item.fiatFormat);
//        fiatView.setPrefixColor(item.fiatPrefixColor);
        CurrencyTools.setText(fiatView, item.getFiatFormat(), item.getFiat());
    }

    public void bindValue(final TransactionListItem item) {
        ViewUtil.showView(valueView,null!=item.getValue());
//        valueView.setVisibility(item.value != null ? View.VISIBLE : View.GONE);
//        valueView.setAlwaysSigned(true);
//        valueView.setAmount(item.value);
//        valueView.setFormat(item.valueFormat);
        valueView.setTextColor(item.getValueColor());
        CurrencyTools.setText(valueView, item.getValueFormat(), item.getValue());
    }

    public void bindFee(final TransactionListItem item) {
        ViewUtil.showView(extendFeeView,null!=item.getFee());
//        extendFeeView.setVisibility(item.fee != null ? View.VISIBLE : View.GONE);
//        feeView.setAlwaysSigned(true);
//        feeView.setFormat(item.feeFormat);
//        feeView.setAmount(item.fee);
        CurrencyTools.setText(feeView, item.getFeeFormat(), item.getFee());
    }

    public void bindAddress(final TransactionListItem item) {
//        extendAddressView.setVisibility(item.address != null || !item.isSelected ? View.VISIBLE : View.GONE);
        ViewUtil.showView( extendAddressView, null!= item.getAddress() || !item.isSelected());
        addressView.setText(item.getAddress());
        addressView.setTextColor(item.getAddressColor());
        addressView.setTypeface(item.getAddressTypeface());
        addressView.setSingleLine(item.isAddressSingleLine());
    }

    public void bindTime(final TransactionListItem item) {
//        (item.isSelected() ? extendTimeView : timeView).setVisibility(View.VISIBLE);
//        (item.isSelected() ? timeView : extendTimeView).setVisibility(View.GONE);
        ViewUtil.showView(item.isSelected() ? extendTimeView : timeView, true);
        ViewUtil.showView(!item.isSelected() ? extendTimeView : timeView, false);
        final TextView timeView = item.isSelected() ? this.fullTimeView : this.timeView;
        timeView.setText(item.getTime());
        timeView.setTextColor(item.getTimeColor());
    }

    public void bindConfidence(final TransactionListItem item) {
//        (item.isSelected() ? confidenceCircularNormalView : confidenceCircularSelectedView)
//                .setVisibility(View.INVISIBLE);
        ViewUtil.setVisibility(item.isSelected() ? confidenceCircularNormalView : confidenceCircularSelectedView,
                View.INVISIBLE);
//        (item.isSelected() ? confidenceTextualNormalView : confidenceTextualSelectedView).setVisibility(View.GONE);
        ViewUtil.showView(item.isSelected() ? confidenceTextualNormalView : confidenceTextualSelectedView , false);
        final CircularProgressView confidenceCircularView = item.isSelected() ? confidenceCircularSelectedView
                : confidenceCircularNormalView;
        final TextView confidenceTextualView = item.isSelected() ? confidenceTextualSelectedView
                : confidenceTextualNormalView;
//        confidenceCircularView
//                .setVisibility(item.getConfidenceCircularMaxProgress() > 0 || item.getConfidenceCircularMaxSize() > 0
//                        ? View.VISIBLE : View.GONE);
//        confidenceTextualView.setVisibility(item.getConfidenceTextual() != null ? View.VISIBLE : View.GONE);
//        extendConfidenceMessageSelectedView
//                .setVisibility(item.isSelected() && item.getConfidenceMessage() != null ? View.VISIBLE : View.GONE);
//        extendConfidenceMessageNormalView
//                .setVisibility(!item.isSelected() && item.getConfidenceMessage() != null ? View.VISIBLE : View.GONE);

        ViewUtil.showView(confidenceCircularView,
                item.getConfidenceCircularMaxProgress() > 0 || item.getConfidenceCircularMaxSize() > 0);
        ViewUtil.showView(confidenceTextualView, null!= item.getConfidenceTextual());
        ViewUtil.showView(extendConfidenceMessageSelectedView,
                item.isSelected() && item.getConfidenceMessage() != null);
        ViewUtil.showView(extendConfidenceMessageNormalView,
                !item.isSelected() && item.getConfidenceMessage() != null);

        confidenceCircularView.setMaxProgress(item.getConfidenceCircularMaxProgress());
        confidenceCircularView.setProgress(item.getConfidenceCircularProgress());
        confidenceCircularView.setMaxSize(item.getConfidenceCircularMaxSize());
        confidenceCircularView.setSize(item.getConfidenceCircularSize());
        confidenceCircularView.setColors(item.getConfidenceCircularFillColor(), item.getConfidenceCircularStrokeColor());
        confidenceTextualView.setText(item.getConfidenceTextual());
        confidenceTextualView.setTextColor(item.getConfidenceTextualColor());
        (item.isSelected() ? confidenceMessageSelectedView : confidenceMessageNormalView)
                .setText(item.getConfidenceMessage());
    }

}
