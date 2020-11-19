package com.bethel.mycoolwallet.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.ExchangeRateBean;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.utils.Utils;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

import java.util.LinkedList;
import java.util.List;

public class ExchangeRatesAdapter extends ListAdapter<ExchangeRatesAdapter.ListItem, ExchangeRatesAdapter.CoolViewHolder> {

    private OnItemClickListener itemClickListener;
    private final LayoutInflater inflater;
    public ExchangeRatesAdapter(Context context) {
        super(new DiffUtil.ItemCallback<ListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                return TextUtils.equals(oldItem.currencyCode, newItem.currencyCode);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                if (!Utils.equals(oldItem.baseRateAsFiat, newItem.baseRateAsFiat)) return false;
                if ( oldItem.baseRateMinDecimals != newItem.baseRateMinDecimals) return false;
                if ( oldItem.isSelected != newItem.isSelected) return false;
                return true;
            }
        });

        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public CoolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.exchange_rate_row, parent,false);
        return new CoolViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CoolViewHolder holder, int position) {
        final ListItem item = getItem(position);
        holder.itemView.setBackgroundResource(item.isSelected ? R.color.bg_level3 : R.color.bg_level2);
        holder.defaultView.setVisibility(item.isSelected ? View.VISIBLE : View.INVISIBLE);
        holder.currencyCodeView.setText(item.currencyCode);
        MonetaryFormat format = Constants.LOCAL_FORMAT.minDecimals(item.baseRateMinDecimals);
        CurrencyTools.setText(holder.rateView, format, item.baseRateAsFiat);

        if (null!=itemClickListener) {
            holder.menuView.setOnClickListener(view ->
                    itemClickListener.onExchangeRateMenuClick(view, item.currencyCode));
        }
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public static List<ListItem> buildListItems(final List<ExchangeRateBean> source,
                                                final String defaultCurrency, final Coin rateBase) {
        List<ListItem> list = new LinkedList<>();
        if (null== source) return list;
        for (ExchangeRateBean bean: source  ) {
            list.add(buildListItem(bean, defaultCurrency, rateBase));
        }
        return list;
    }
    public static ListItem buildListItem(final ExchangeRateBean bean, final String defaultCurrency, final Coin rateBase) {
        final ExchangeRate rate = bean.rate;
        final Fiat baseRateAsFiat = rate.coinToFiat(rateBase);
        final int baseRateMinDecimals = !rateBase.isLessThan(Coin.COIN) ? 2 : 4;
        final boolean isDefaultCurrency = TextUtils.equals(defaultCurrency, bean.getCurrencyCode());
        return new ListItem(bean.getCurrencyCode(), baseRateAsFiat, baseRateMinDecimals, isDefaultCurrency);
    }

    public static class ListItem{
        public final String currencyCode;
        public final Fiat baseRateAsFiat;
        public final int baseRateMinDecimals;
        public  boolean isSelected;

        public ListItem(String currencyCode, Fiat baseRateAsFiat, int baseRateMinDecimals, boolean isSelected) {
            this.currencyCode = currencyCode;
            this.baseRateAsFiat = baseRateAsFiat;
            this.baseRateMinDecimals = baseRateMinDecimals;
            this.isSelected = isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }

    public void updateDefaultCurrencyCode(final String defaultCurrency) {
        List<ListItem> list = getCurrentList();
        if (null==list || list.isEmpty() || TextUtils.isEmpty(defaultCurrency)) return;
        for (ListItem item: list ) {
            item.setSelected(defaultCurrency.equals(item.currencyCode));
        }
        notifyDataSetChanged();
    }

    public static class CoolViewHolder extends RecyclerView.ViewHolder {
        private final View defaultView;
        private final TextView currencyCodeView;
        private final TextView rateView;
        private final ImageButton menuView;
        public CoolViewHolder(@NonNull View itemView) {
            super(itemView);
            defaultView = itemView.findViewById(R.id.exchange_rate_row_default);
            currencyCodeView = itemView.findViewById(R.id.exchange_rate_row_currency_code);
            rateView =   itemView.findViewById(R.id.exchange_rate_row_rate);
            menuView =  itemView.findViewById(R.id.exchange_rate_row_menu);
        }
    }

    public  interface OnItemClickListener {
        void onExchangeRateMenuClick(View view, String currencyCode);
    }

}
