package com.bethel.mycoolwallet.helper;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.activity.BaseActivity;
import com.bethel.mycoolwallet.data.tx_list.ColorType;
import com.xuexiang.xui.widget.guidview.FocusShape;
import com.xuexiang.xui.widget.guidview.GuideCaseQueue;
import com.xuexiang.xui.widget.guidview.GuideCaseView;

public final class GuideHelper {
    public static void showMain(BaseActivity activity) {
        if (null==activity || activity.isFinishing()) return;

        final FragmentManager fm = activity.getSupportFragmentManager();
        // init view
        View balance = findView(fm, R.id.wallet_balance_fragment ,R.id.wallet_balance_layout);
        View qr = fm.findFragmentById(R.id.wallet_address_fragment).getView();
//        View qr = findView(fm, R.id.wallet_address_fragment,R.id.bitcoin_address_qr_card);
        View bottomActions = fm.findFragmentById(R.id.wallet_actions_fragment).getView();
        Toolbar toolbar = activity.findViewById(R.id.toolbar);//toolbar, action_menu_view
        View menu = findActionMenuView(toolbar);
        Log.d("GuideHelper", String.format("showMain: %s  %s  %s  %s ",
                null!= balance,null!= qr,null!= bottomActions,null!= menu));

        final int colorBg =  ContextCompat.getColor(activity, R.color.guide_bg);
        // guide queue
        final GuideCaseView guideStep1 =  new GuideCaseView.Builder(activity)
                .focusOn(balance)
                .title(activity.getString(R.string.guide_wallet_1))
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .roundRectRadius(90)
//                .backgroundColor(colorBg )
                .build();

        final GuideCaseView guideStep2 = new GuideCaseView.Builder(activity)
                .title(activity.getString(R.string.guide_wallet_2))
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .roundRectRadius(90)
                .focusOn(qr)
//                .backgroundColor(colorBg )
                .build();
        final GuideCaseView guideStep3 = new GuideCaseView.Builder(activity)
                .title(activity.getString(R.string.guide_wallet_3))
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .roundRectRadius(90)
                .focusOn(bottomActions)
                .backgroundColor(colorBg )
                .build();

        final GuideCaseView guideStep4 = new GuideCaseView.Builder(activity)
                .title(activity.getString(R.string.guide_wallet_4))
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .roundRectRadius(90)
                .backgroundColor(colorBg )
                .focusOn(menu)
                .backgroundColor(colorBg )
                .build();

        // show
        new GuideCaseQueue()
                .add(guideStep1)
                .add(guideStep2)
                .add(guideStep3)
                .add(guideStep4)
                .show();

        Configuration.INSTANCE.guideUser();
    }

    private static View findView(final FragmentManager fm , final int fragmentId, final int viewId) {
        Fragment fragment = fm.findFragmentById(fragmentId);
        View target = null;
        if (null!=fragment) {
            View root = fragment.getView();
            if (null!=root) {
                target = root.findViewById(viewId);
            }
            Log.d("GuideHelper", "findView: root = "+(null!=root? root.getClass().getSimpleName(): "NULL"));
        }
        Log.d("GuideHelper", "findFragmentById: "+ (null!=fragment? fragment.getClass().getSimpleName(): "NULL"));
        return target;
    }

    private static View findActionMenuView(Toolbar toolbar) {
        if (null==toolbar) return null;
        final int num = toolbar.getChildCount();
        for (int i = 0; i < num; i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof ActionMenuView) {
                return child;
            }
        }
        return toolbar;
    }

}
