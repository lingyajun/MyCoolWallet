package com.bethel.mycoolwallet.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Qr;
import com.xuexiang.xui.widget.toast.XToast;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 当前btc地址.
 */
public class WalletAddressFragment extends BaseFragment {
    private Handler mHandler;

    @BindView(R.id.bitcoin_address_qr_card)
    CardView cardView;

    @BindView(R.id.bitcoin_address_qr)
    ImageView qrImg;

    @OnClick(R.id.bitcoin_address_qr_card)
    public void onCardClick() {
        WalletAddressDialogFragment.show(getFragmentManager());
        XToast.info(getActivity(), "view my address detail").show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XToast.Config.get().setAlpha(200).allowQueue(false).setGravity(Gravity.CENTER);
        mHandler = new Handler();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cardView.setPreventCornerOverlap(false);
        cardView.setUseCompatPadding(false);
        cardView.setMaxCardElevation(0);

        showQrBmp("bit,eth,defi,,,,9977");

    }

    private void showQrBmp(final String content) {
        new Thread(() -> {
            final  Bitmap bitmap = Qr.bitmap(content);
            mHandler.post( () -> {
                        if (isAdded()) {
                            qrImg.setImageBitmap(bitmap);
                        }
                    }
            );
        }).start();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_address;
    }

}
