package com.bethel.mycoolwallet.adapter;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.statelayout.StatefulLayout;

public class PeersStatusViewAdapter extends StatusSingleViewAdapter {
    @Override
    protected void showEmpty(StatefulLayout statefulLayout) {
        statefulLayout.showEmpty(R.string.peer_list_fragment_empty);
    }
}
