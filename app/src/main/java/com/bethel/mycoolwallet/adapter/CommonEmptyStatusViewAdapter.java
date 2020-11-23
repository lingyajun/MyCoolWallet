package com.bethel.mycoolwallet.adapter;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.statelayout.StatefulLayout;

public class CommonEmptyStatusViewAdapter extends StatusSingleViewAdapter {
    private int emptyMessageResId;

    public CommonEmptyStatusViewAdapter(int emptyMessageResId) {
        this.emptyMessageResId = emptyMessageResId;
    }

    @Override
    protected void showEmpty(StatefulLayout statefulLayout) {
        statefulLayout.showEmpty(emptyMessageResId);
    }
}
