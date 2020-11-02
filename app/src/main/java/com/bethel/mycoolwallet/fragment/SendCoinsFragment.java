package com.bethel.mycoolwallet.fragment;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.CustomCaptureActivity;
import com.bethel.mycoolwallet.data.FeeCategory;
import com.bethel.mycoolwallet.interfaces.IQrScan;
import com.bethel.mycoolwallet.utils.Constants;
import com.bethel.mycoolwallet.utils.CurrencyTools;
import com.bethel.mycoolwallet.view.CurrencyAmountView;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.utils.MonetaryFormat;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class SendCoinsFragment extends BaseFragment implements IQrScan {
    /**
     * 扫描跳转Activity RequestCode
     */
    public static final int REQUEST_QR_SCAN_CODE = 111;

    @BindView(R.id.send_coins_payee_group)
     View payeeGroup;
    @BindView(R.id.send_coins_payee_name)
     TextView payeeNameView;
    @BindView(R.id.send_coins_payee_verified_by)
     TextView payeeVerifiedByView;
    @BindView(R.id.send_coins_receiving_address)
     AutoCompleteTextView receivingAddressView;
    @BindView(R.id.send_coins_receiving_static)
     View receivingStaticView;
    @BindView(R.id.send_coins_receiving_static_address)
     TextView receivingStaticAddressView;
    @BindView(R.id.send_coins_receiving_static_label)
     TextView receivingStaticLabelView;
    @BindView(R.id.send_coins_amount_group)
     View amountGroup;

    @BindView(R.id.send_coins_amount_btc)
    CurrencyAmountView coinAmountView;
    @BindView(R.id.send_coins_amount_local)
    CurrencyAmountView localAmountView;

    @BindView(R.id.send_coins_direct_payment_enable)
     CheckBox directPaymentEnableView;
    @BindView(R.id.send_coins_hint)
     TextView hintView;
    @BindView(R.id.send_coins_direct_payment_message)
     TextView directPaymentMessageView;
    @BindView(R.id.transaction_row)
     ViewGroup sentTransactionViewGroup;

    @BindView(R.id.send_coins_private_key_password_group)
     View privateKeyPasswordViewGroup;
    @BindView(R.id.send_coins_private_key_password)
     EditText privateKeyPasswordView;
    @BindView(R.id.send_coins_private_key_bad_password)
     View privateKeyBadPasswordView;

    @BindView(R.id.send_coins_go)
     Button viewGo;
    @BindView(R.id.send_coins_cancel)
     Button viewCancel;

    @OnClick(R.id.send_coins_go)
    protected void onSendClick() {
        XToast.warning(getContext(), "send coins!").show();
    }

    @OnClick(R.id.send_coins_cancel)
    protected void onCancelClick() {
        XToast.warning(getContext(), "cancel !").show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // test ui
        coinAmountView.setCurrencySymbol(MonetaryFormat.CODE_MBTC);
        coinAmountView.setInputFormat(CurrencyTools.getMaxPrecisionFormat(3));
        coinAmountView.setHintFormat(CurrencyTools.getFormat(3, 2));

        localAmountView.setInputFormat(Constants.LOCAL_FORMAT);
        localAmountView.setHintFormat(Constants.LOCAL_FORMAT);
        localAmountView.setCurrencySymbol("usd"); // exchangeRate.fiat.currencyCode

        viewCancel.setText(R.string.button_cancel);
        viewGo.setText(R.string.send_coins_fragment_button_send);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.send_coins_fragment_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        final MenuItem scanAction = menu.findItem(R.id.send_coins_options_scan);
        final PackageManager pm = getActivity().getPackageManager();
        scanAction.setVisible(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT));
        // todo
//        scanAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);
        scanAction.setEnabled(true);

        final MenuItem emptyAction = menu.findItem(R.id.send_coins_options_empty);
//        emptyAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT
//                && viewModel.paymentIntent.mayEditAmount() && viewModel.balance.getValue() != null);

//        final MenuItem feeCategoryAction = menu.findItem(R.id.send_coins_options_fee_category);
//        feeCategoryAction.setEnabled(viewModel.state == SendCoinsViewModel.State.INPUT);
//        if (viewModel.feeCategory == FeeCategory.ECONOMIC)
//            menu.findItem(R.id.send_coins_options_fee_category_economic).setChecked(true);
//        else if (viewModel.feeCategory == FeeCategory.NORMAL)
//            menu.findItem(R.id.send_coins_options_fee_category_normal).setChecked(true);
//        else if (viewModel.feeCategory == FeeCategory.PRIORITY)
//            menu.findItem(R.id.send_coins_options_fee_category_priority).setChecked(true);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.send_coins_options_scan:
                    startScan(null);
                break;
            case R.id.send_coins_options_fee_category_economic:
                break;
            case R.id.send_coins_options_fee_category_normal:
                break;
            case R.id.send_coins_options_fee_category_priority:
                break;
            case R.id.send_coins_options_empty:
                break;
                default:    return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void startScan(View v) {
        CustomCaptureActivity.start(this, REQUEST_QR_SCAN_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //处理二维码扫描结果
        if (requestCode == REQUEST_QR_SCAN_CODE && resultCode == Activity.RESULT_OK) {
            handleScanResult(data);
        }
    }

    /**
     * 处理二维码扫描结果
     *
     * @param data
     */
    private void handleScanResult(Intent data) {
        if (data != null) {
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_SUCCESS) {
                    String result = bundle.getString(XQRCode.RESULT_DATA);
                    // todo handle bitcoin pay
                    XToast.success(getContext(), "解析结果: " + result, Toast.LENGTH_LONG).show();
                } else if (bundle.getInt(XQRCode.RESULT_TYPE) == XQRCode.RESULT_FAILED) {
                    XToast.error(getContext(), R.string.parse_qr_code_failed, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_send_coins;
    }

}
