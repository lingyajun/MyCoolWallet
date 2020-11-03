package com.bethel.mycoolwallet.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.view_model.WalletAddressViewModel;
import com.bethel.mycoolwallet.utils.Qr;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Address;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 当前btc地址.
 */
public class WalletAddressFragment extends BaseFragment {
    private WalletAddressViewModel viewModel;
//    private Handler mHandler;

    @BindView(R.id.bitcoin_address_qr_card)
    CardView cardView;

    @BindView(R.id.bitcoin_address_qr)
    ImageView qrImg;

    @OnClick(R.id.bitcoin_address_qr_card)
    public void onCardClick() {
        viewModel.showWalletAddressDialog.setValue(Event.simple());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XToast.Config.get().setAlpha(200).allowQueue(false).setGravity(Gravity.CENTER);
        viewModel = getViewModel(WalletAddressViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cardView.setPreventCornerOverlap(false);
        cardView.setUseCompatPadding(false);
        cardView.setMaxCardElevation(0);

        viewModel.qrCode.observe(this, ( bitmap)-> showQrBmp(bitmap));
//        viewModel.bitcoinUri.observe(this, (uri)-> XToast.info(getActivity(), uri.toString()));
        viewModel.showWalletAddressDialog.observe(this, (event)->{
            final Address address = viewModel.currentAddress.getValue();
            WalletAddressDialogFragment.show(getFragmentManager(), address, null);
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
