package com.bethel.mycoolwallet.fragment;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.mvvm.view_model.MainActivityViewModel;

/**
 * todo A simple {@link Fragment} subclass.
 */
public class WalletDisclaimerFragment extends BaseFragment {
    TextView msgTv;

    private MainActivityViewModel activityViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityViewModel = ViewModelProviders.of(getActivity()).get(MainActivityViewModel.class);
    }

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

        msgTv.setOnClickListener(view1 ->
                activityViewModel.showHelpDialog.setValue(new Event<>(R.string.help_safety)));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_disclaimer;
    }

}
