package com.bethel.mycoolwallet.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.fragment.dialog.WalletAddressDialogFragment;
import com.bethel.mycoolwallet.mvvm.view_model.MainActivityViewModel;
import com.bethel.mycoolwallet.mvvm.view_model.WalletAddressViewModel;
import com.bethel.mycoolwallet.utils.NfcTools;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 当前btc地址.
 */
public class WalletAddressFragment extends BaseFragment {
    private WalletAddressViewModel viewModel;
    private MainActivityViewModel activityViewModel;

    private NfcAdapter nfcAdapter;

    @BindView(R.id.bitcoin_address_qr_card)
    CardView cardView;

    @BindView(R.id.bitcoin_address_qr)
    ImageView qrImg;

    @OnClick(R.id.bitcoin_address_qr_card)
    public void onCardClick() {
        viewModel.showWalletAddressDialog.setValue(Event.simple());
    }

    private static final Logger log = LoggerFactory.getLogger(WalletAddressFragment.class);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XToast.Config.get().setAlpha(200).allowQueue(false).setGravity(Gravity.CENTER);
        viewModel = getViewModel(WalletAddressViewModel.class);
        activityViewModel = getActivityViewModel(MainActivityViewModel.class);
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cardView.setPreventCornerOverlap(false);
        cardView.setUseCompatPadding(false);
        cardView.setMaxCardElevation(0);

        viewModel.qrCode.observe(this, ( bitmap)-> showQrBmp(bitmap));
//    uri NfcAdapter
     viewModel.bitcoinUri.observe(this, (uri)-> {
         if (null!=nfcAdapter && null!=uri) {
             NdefMessage message = NfcTools.createNdefMessage(uri.toString());
             nfcAdapter.setNdefPushMessage(message, getActivity());
         }
         activityViewModel.addressLoadingFinished();
         log.info("bitcoin Uri {}", uri);
     });

        viewModel.showWalletAddressDialog.observe(this, (event)->{
            final Address addr = viewModel.currentAddress.getValue();
            final String address = null!=addr? addr.toString(): null;
            if (TextUtils.isEmpty(address)) return;

            final String label = viewModel.getLabel();
            WalletAddressDialogFragment.show(getFragmentManager(), address, label);
        });
    }

    private void showQrBmp(Bitmap bitmap) {
        if (isAdded()) {
            final BitmapDrawable qrDrawable = new BitmapDrawable(getResources(), bitmap);
            qrDrawable.setFilterBitmap(false);
            qrImg.setImageDrawable(qrDrawable);
//            qrImg.setImageBitmap(bitmap);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_address;
    }

}
