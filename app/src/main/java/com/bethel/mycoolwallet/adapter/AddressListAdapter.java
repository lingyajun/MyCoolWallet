package com.bethel.mycoolwallet.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.interfaces.OnItemClickListener;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.ViewUtil;
import com.bethel.mycoolwallet.utils.WalletUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.wallet.Wallet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AddressListAdapter extends ListAdapter<AddressListAdapter.ListItem, AddressListAdapter.MViewHolder> {
    private final LayoutInflater inflater;
    private final int colorSignificant;
    private final int colorInsignificant;
    private final int colorLessSignificant;

    private final List<ListItem> derivedAddresses = new ArrayList<>(); // issuedReceiveAddresses
    private final List<ListItem> randomAddresses = new ArrayList<>(); // importedAddresses
    @Nullable
    private Wallet wallet = null;
    @Nullable
    private Map<String, AddressBook> addressBook = null;

    private OnItemClickListener itemClickListener;

    public AddressListAdapter(Context context) {
        super(new DiffUtil.ItemCallback<ListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                return TextUtils.equals(oldItem.address, newItem.address);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                 if (!TextUtils.equals(oldItem.label, newItem.label)) return false;
                 if ( oldItem.messageId!= newItem.messageId ) return false;
                return true;
            }
        });

        inflater = LayoutInflater.from(context);
        colorSignificant = ContextCompat.getColor(context, R.color.fg_significant);
        colorInsignificant = ContextCompat.getColor(context, R.color.fg_insignificant);
        colorLessSignificant = ContextCompat.getColor(context, R.color.fg_less_significant);
    }

    @NonNull
    @Override
    public MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.address_book_list_row, parent, false);
        return new MViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MViewHolder holder,final int position) {
        final ListItem item = getItem(position);
        CharSequence addressText = WalletUtils.formatHash(item.address,
                Constants.ADDRESS_FORMAT_GROUP_SIZE,
                Constants.ADDRESS_FORMAT_LINE_SIZE);
        holder.addressView.setText(addressText);

        CharSequence labelText = !TextUtils.isEmpty(item.label) ?
                item.label: holder.labelView.getContext().getText(R.string.address_unlabeled);
        holder.labelView.setText(labelText);

        if (0!=item.messageId) {
            holder.messageView.setText(item.messageId);
        } else {
            holder.messageView.setText(null);
        }

        ViewUtil.showView(holder.messageView, 0!=item.messageId);

        // todo text color
        holder.addressView.setTextColor(0==item.messageId ? colorInsignificant : colorSignificant);
        holder.labelView.setTextColor((TextUtils.isEmpty(item.label) || 0!=item.messageId)?
                colorLessSignificant: colorInsignificant);

        if (null!=itemClickListener) {
            holder.itemView.setOnClickListener(view -> itemClickListener.OnItemClicked(view, position));
        }
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void replaceDerivedAddresses(final Collection<Address> addresses) {
        derivedAddresses.clear();
        derivedAddresses.addAll(buildItems(addresses, wallet, addressBook));
        submitListItems();
    }

    public void replaceRandomAddresses(final Collection<Address> addresses) {
        this.randomAddresses.clear();
        this.randomAddresses.addAll(buildItems(addresses, wallet, addressBook, R.string.address_book_list_receiving_random));
        submitListItems();
    }

    private void submitListItems() {
        List<ListItem> list = new LinkedList<>();
        list.addAll(derivedAddresses);
        list.addAll(randomAddresses);
        submitList(list);
    }

    public static List<ListItem> buildItems(Collection<Address> addresses, Wallet wallet, Map<String , AddressBook> map) {
        List<ListItem> list = new LinkedList<>();
        for (Address address: addresses ) {
            list.add(buildItem(address, wallet, map));
        }
        return list;
    }
    public static List<ListItem> buildItems(Collection<Address> addresses, Wallet wallet, Map<String , AddressBook> map, int messageId) {
        List<ListItem> list = new LinkedList<>();
        for (Address address: addresses ) {
            list.add(buildItem(address, wallet, map, messageId));
        }
        return list;
    }
    private static ListItem buildItem(Address address, Wallet wallet, Map<String , AddressBook> map) {
        return buildItem(address, wallet, map, 0);
    }
    private static ListItem buildItem(Address address, Wallet wallet, Map<String , AddressBook> map, int messageId) {
        final String addressStr = address.toString();
        final String label = null!=map && map.containsKey(addressStr) ?
                map.get(addressStr).getLabel() : null;
        final boolean isRotateKey;
        if (wallet != null) {
            final ECKey key = wallet.findKeyFromAddress(address);
            isRotateKey =  wallet.isKeyRotating(key);
        } else {
            isRotateKey = false;
        }
        final int message = isRotateKey ? R.string.address_book_row_message_compromised_key: messageId;
        return  new ListItem(addressStr, label, message);
    }

    public void updateWallet(@Nullable Wallet wallet) {
        this.wallet = wallet;
        updateListItems(getCurrentList(), wallet, null);
        notifyDataSetChanged();
    }

    public void updateAddressBook(@Nullable Map<String, AddressBook> addressBook) {
        this.addressBook = addressBook;
        updateListItems(getCurrentList(), null, addressBook);
        notifyDataSetChanged();
    }

    private static void updateListItems(List<ListItem> list, Wallet wallet, Map<String , AddressBook> map) {
        for (ListItem item: list     ) {
            updateItem(item, wallet, map);
        }
    }

    private static void updateItem(ListItem item, Wallet wallet, Map<String , AddressBook> map) {
        final String addressStr = item.address;
        if (null!=map ) {
            if ( map.containsKey(addressStr)) {
                item.label = map.get(addressStr).getLabel();
            } else {
                item.label = null;
            }
        }
        if (wallet != null) {
             boolean isRotateKey = false;
            try {
                final ECKey key = wallet.findKeyFromAddress(Address.fromString(Constants.NETWORK_PARAMETERS, addressStr));
                isRotateKey =  wallet.isKeyRotating(key);
            } catch (AddressFormatException e) {
                e.printStackTrace();
            }

            if (isRotateKey) {
                item.messageId = R.string.address_book_row_message_compromised_key;
            }
        }
    }
    public static final class ListItem  {
        public  int messageId;
        public final String address;
        public  String label;
        public ListItem(String address, String label, int message) {
            this.address = address;
            this.label = label;
            this.messageId = message;
        }

        public ListItem(AddressBook book, int message) {
            this(book.getAddress(), book.getLabel(), message);
        }
    }

    protected static final class MViewHolder extends RecyclerView.ViewHolder {
        TextView addressView;
        TextView labelView;
        TextView messageView;
        View itemView;
        public MViewHolder(@NonNull View itemView) {
            super(itemView);
            addressView = itemView.findViewById(R.id.address_book_row_address);
            labelView = itemView.findViewById(R.id.address_book_row_label);
            messageView = itemView.findViewById(R.id.address_book_row_message);
            this.itemView = itemView;
        }
    }
}
