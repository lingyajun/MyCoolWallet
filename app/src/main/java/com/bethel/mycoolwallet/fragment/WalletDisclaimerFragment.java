package com.bethel.mycoolwallet.fragment;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class WalletDisclaimerFragment extends BaseFragment {
    TextView msgTv;


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        msgTv =(TextView) view;

        // todo

        int  progressResId = R.string.blockchain_state_progress_problem_network;
        final SpannableStringBuilder text = new SpannableStringBuilder();
//        if (progressResId != 0)
            text.append(Html.fromHtml("<b>" + getString(progressResId) + "</b>"));
//        if (progressResId != 0 && showDisclaimer)
            text.append('\n');
//        if (showDisclaimer)
            text.append(Html.fromHtml(getString(R.string.wallet_disclaimer_fragment_remind_safety)));
        msgTv.setText(text);

    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_disclaimer;
    }

}
