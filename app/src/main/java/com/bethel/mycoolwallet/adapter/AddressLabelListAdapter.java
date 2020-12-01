package com.bethel.mycoolwallet.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.WalletUtils;

/**
 * 输入框，地址自动填充
 */
public class AddressLabelListAdapter extends ArrayAdapter<AddressBook> {
    private final LayoutInflater inflater;
    private Filter mAddressLabelListFilter ;

    public AddressLabelListAdapter(@NonNull Context context) {
        super(context, 0);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (null!= mAddressLabelListFilter) return mAddressLabelListFilter;
        return super.getFilter();
    }

    public void setAddressLabelListFilter(Filter filter) {
        this.mAddressLabelListFilter = filter;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (null==convertView) {
            convertView = inflater.inflate(R.layout.address_book_list_row, parent, false);
            holder = new ViewHolder(convertView);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final AddressBook book = getItem(position);
        holder.labelTv.setText(book.getLabel());
        final CharSequence address = WalletUtils.formatHash(book.getAddress(),
                Constants.ADDRESS_FORMAT_GROUP_SIZE, Constants.ADDRESS_FORMAT_LINE_SIZE);
        holder.addressTv.setText(address);
        return convertView;
    }

    private static class ViewHolder {
        TextView labelTv;
        TextView addressTv;

        public ViewHolder(View convertView) {
             labelTv = convertView.findViewById(R.id.address_book_row_label);
             addressTv = convertView.findViewById(R.id.address_book_row_address);
             convertView.setTag(this);
        }
    }
}
