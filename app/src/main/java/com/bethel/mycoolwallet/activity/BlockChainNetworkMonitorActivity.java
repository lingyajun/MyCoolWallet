package com.bethel.mycoolwallet.activity;

import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.fragment.BaseFragment;
import com.bethel.mycoolwallet.fragment.BlocksNetworkMonitorFragment;
import com.bethel.mycoolwallet.fragment.PeersNetworkMonitorFragment;
import com.xuexiang.xui.adapter.FragmentAdapter;
import com.xuexiang.xui.widget.tabbar.EasyIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * 区块链 网络监视器
 */
public class BlockChainNetworkMonitorActivity extends BaseActivity {
    EasyIndicator mEasyIndicator;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.network_monitor_content);
        mEasyIndicator = findViewById(R.id.easy_indicator);
        mViewPager = findViewById(R.id.view_pager);

        final String[] titles = new String[] {getString(R.string.network_monitor_peer_list_title),
                getString(R.string.network_monitor_block_list_title)};
        final  List<BaseFragment> list = new ArrayList<>();
        list.add(new PeersNetworkMonitorFragment());
        list.add(new BlocksNetworkMonitorFragment());

        mEasyIndicator.setTabTitles(titles);
        mEasyIndicator.setViewPager(mViewPager, new FragmentAdapter<>(getSupportFragmentManager(), list));
        mViewPager.setOffscreenPageLimit(list.size() -1);

        initTitleBar(R.string.network_monitor_activity_title, true);
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, BlockChainNetworkMonitorActivity.class));
    }
}
