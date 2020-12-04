package com.bethel.mycoolwallet.adapter;

import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Utils;
import com.xuexiang.xui.widget.statelayout.StatefulLayout;

public class CommonEmptyStatusViewAdapter extends StatusSingleViewAdapter {
    private int emptyMessageResId;

    private StatefulLayout stateLayout;
    private TextView msgTv;

    public CommonEmptyStatusViewAdapter(int emptyMessageResId) {
        this.emptyMessageResId = emptyMessageResId;
    }

    @Override
    protected void showEmpty(StatefulLayout statefulLayout) {
        statefulLayout.showEmpty(emptyMessageResId);
        this.stateLayout = statefulLayout;
    }

    public void showEmptyMessage(String msg) {
        if (null!=stateLayout) {
            if (null== msgTv || !Utils.equals(stateLayout, msgTv.getTag())) {
                msgTv = stateLayout.findViewById(R.id.stMessage);
                msgTv.setSingleLine(false);
                msgTv.setTag(stateLayout);
            }
            stateLayout.showEmpty(msg);
        }
    }
}
