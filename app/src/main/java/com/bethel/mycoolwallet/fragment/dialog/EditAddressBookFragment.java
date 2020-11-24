package com.bethel.mycoolwallet.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.bethel.mycoolwallet.CoolApplication;
import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.db.AddressBook;
import com.bethel.mycoolwallet.db.AddressBookDao;
import com.bethel.mycoolwallet.db.AppDatabase;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;

public class EditAddressBookFragment extends BaseDialogFragment {
    private static final String TAG = "EditAddressBookFragment";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_SUGGESTED_ADDRESS_LABEL = "suggested_address_label";

    private AddressBookDao addressBookDao;
    private Wallet wallet;

    private static final Logger log = LoggerFactory.getLogger(EditAddressBookFragment.class);

    public static void edit(final FragmentManager fm, final String address) {
        edit(fm, address, null);
    }

    private static void edit(final FragmentManager fm, final String  address,
                             @Nullable final String suggestedAddressLabel) {
        final DialogFragment newFragment = EditAddressBookFragment.instance(address, suggestedAddressLabel);
        newFragment.show(fm, TAG);
    }

    private static EditAddressBookFragment instance(final String address,
                                                         @Nullable final String suggestedAddressLabel) {
        final EditAddressBookFragment fragment = new EditAddressBookFragment();

        final Bundle args = new Bundle();
        args.putString(KEY_ADDRESS, address);
        args.putString(KEY_SUGGESTED_ADDRESS_LABEL, suggestedAddressLabel);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        CoolApplication application = CoolApplication.getApplication();
        addressBookDao = AppDatabase.getInstance(application).addressBookDao();
        wallet = application.getWallet();
    }

    @BindView(R.id.edit_address_book_entry_address)
    TextView viewAddress;
    @BindView(R.id.edit_address_book_entry_label)
    TextView viewLabel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final String addressStr = args.getString(KEY_ADDRESS);
        final String suggestedAddressLabel = args.getString(KEY_SUGGESTED_ADDRESS_LABEL);

        final String label = addressBookDao.resolveLabel(addressStr);
        log.info("{} {}", addressStr , label);

        boolean isAddressMine =false;
        try {
            final Address address = Address.fromString(Constants.NETWORK_PARAMETERS, addressStr);
            isAddressMine = null!=wallet && wallet.isAddressMine(address);
        } catch (AddressFormatException e) {
            log.info("AddressFormatException {} {}",addressStr,e);
        }
        final int title;
        if (isAddressMine) {
            title = TextUtils.isEmpty(label) ? R.string.edit_address_book_entry_dialog_title_add_receive:
                    R.string.edit_address_book_entry_dialog_title_edit_receive;
        } else {
            title = TextUtils.isEmpty(label) ? R.string.edit_address_book_entry_dialog_title_add
                    : R.string.edit_address_book_entry_dialog_title_edit;
        }

        View view = createAndBindDialogView(R.layout.edit_address_book_entry_dialog);
        viewAddress.setText(WalletUtils.formatHash(addressStr, Constants.ADDRESS_FORMAT_GROUP_SIZE,
                Constants.ADDRESS_FORMAT_LINE_SIZE));
//        viewAddress.setLines(Constants.ADDRESS_FORMAT_LINE_SIZE);
        viewLabel.setText(!TextUtils.isEmpty(label) ? label : suggestedAddressLabel);

        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .title(title)
                .customView(view, false)
                .negativeText(R.string.button_cancel)
                .positiveText(TextUtils.isEmpty(label) ? R.string.button_add
                                : R.string.edit_address_book_entry_dialog_button_edit)
                .show();
        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(view1 -> {
            final String newLabel = viewLabel.getText().toString().trim();
            if (!TextUtils.isEmpty(newLabel)) {
                addressBookDao.insertOrUpdate(new AddressBook(addressStr, newLabel));
            } else {
//                addressBookDao.insertOrUpdate(new AddressBook(addressStr, null));
                addressBookDao.delete(addressStr);
            }
            dismiss();
        });

        if (!TextUtils.isEmpty(label)) {
            dialog.getActionButton(DialogAction.NEGATIVE)
                    .setOnClickListener(view1 -> dismissAllowingStateLoss());
            dialog.getActionButton(DialogAction.NEUTRAL)
                    .setOnClickListener(view1 -> {
                        addressBookDao.delete(addressStr);
                        dismiss();
                    });
        }
        return dialog;
    }
}
