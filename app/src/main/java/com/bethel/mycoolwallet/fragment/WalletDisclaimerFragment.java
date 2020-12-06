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
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.BlockChainState;
import com.bethel.mycoolwallet.data.Event;
import com.bethel.mycoolwallet.data.Impediment;
import com.bethel.mycoolwallet.mvvm.view_model.MainActivityViewModel;
import com.bethel.mycoolwallet.mvvm.view_model.WalletDisclaimerViewModel;
import com.bethel.mycoolwallet.utils.ViewUtil;

/**
 *  免责声明
 */
public class WalletDisclaimerFragment extends BaseFragment {
    private   TextView msgTv;

    private MainActivityViewModel activityViewModel;
    private WalletDisclaimerViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityViewModel = getActivityViewModel(MainActivityViewModel.class);
        viewModel = getViewModel(WalletDisclaimerViewModel.class);

        viewModel.blockChainState.observe(this, blockChainState -> updateUI());
        viewModel.disclaimer.observe(this, blockChainState -> updateUI());

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        msgTv =(TextView) view;
        msgTv.setOnClickListener(view1 ->
                activityViewModel.showHelpDialog.setValue(new Event<>(R.string.help_safety)));
    }

    private void updateUI() {
        final     BlockChainState state = viewModel.blockChainState.getValue();
        final Boolean disclaimer = viewModel.disclaimer.getValue();
        if (null == msgTv) return;

        int  progressResId =0;
        if (null!=state) {
            if (state.impediments.contains(Impediment.STORAGE)) {
                progressResId = R.string.blockchain_state_progress_problem_storage;
            } else if (state.impediments.contains(Impediment.NETWORK)) {
                progressResId = R.string.blockchain_state_progress_problem_network;
            }
        }
        final SpannableStringBuilder text = new SpannableStringBuilder();
        if (progressResId != 0) {
            text.append(Html.fromHtml("<b>" + getString(progressResId) + "</b>"));
        }
        if (progressResId != 0 && null!= disclaimer&& disclaimer) {
            text.append('\n');
        }
        if (null!= disclaimer&& disclaimer) {
            text.append(Html.fromHtml(getString(R.string.wallet_disclaimer_fragment_remind_safety)));
        }
            msgTv.setText(text);

        View view = getView();
        ViewParent viewParent = view.getParent();
        View fragment = viewParent instanceof FrameLayout ? (View) viewParent : view;
        ViewUtil.showView(fragment, text.length() >0);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_disclaimer;
    }

}
