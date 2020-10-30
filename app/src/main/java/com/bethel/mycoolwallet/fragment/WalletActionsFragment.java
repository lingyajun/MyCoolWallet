package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.interfaces.IQrScan;
import com.bethel.mycoolwallet.interfaces.IRequestCoins;
import com.bethel.mycoolwallet.interfaces.ISendCoins;
import com.xuexiang.xui.widget.toast.XToast;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class WalletActionsFragment extends BaseFragment {
    @BindView(R.id.wallet_actions_request)
    Button requestBtn;

    @BindView(R.id.wallet_actions_send)
    Button sendBtn;

    @BindView(R.id.wallet_actions_send_qr)
    ImageButton qrBtn;

    @OnClick(R.id.wallet_actions_request)
    protected void requestCoin() {
        Activity activity = getActivity();
        if (activity instanceof IRequestCoins) {
            ((IRequestCoins) activity).requestCoins();
        }
//        XToast.info(getContext(), "requestCoin").show();
    }

    @OnClick(R.id.wallet_actions_send)
    protected void sendCoin() {
        Activity activity = getActivity();
        if (activity instanceof ISendCoins) {
            ((ISendCoins) activity).sendCoins();
        }
//        XToast.info(getContext(), "sendCoin").show();
    }

    @OnClick(R.id.wallet_actions_send_qr)
    protected void startQr() {
        Activity activity = getActivity();
        if (activity instanceof IQrScan) {
            ((IQrScan) activity).startScan(qrBtn);
        }
//        XToast.info(getContext(), "startQr").show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_actions;
    }

}
