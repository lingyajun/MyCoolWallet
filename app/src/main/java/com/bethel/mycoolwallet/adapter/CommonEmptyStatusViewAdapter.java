package com.bethel.mycoolwallet.adapter;

import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.statelayout.StatefulLayout;

public class CommonEmptyStatusViewAdapter extends StatusSingleViewAdapter {
    private int emptyMessageResId;

    private StatefulLayout stateLayout;

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
            TextView tv = stateLayout.findViewById(R.id.stMessage);
            tv.setSingleLine(false);
            stateLayout.showEmpty(msg);
        }
    }
}
