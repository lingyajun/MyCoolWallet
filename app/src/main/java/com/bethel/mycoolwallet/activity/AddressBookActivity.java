package com.bethel.mycoolwallet.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.fragment.BaseFragment;
import com.bethel.mycoolwallet.fragment.ReceivingAddressesFragment;
import com.bethel.mycoolwallet.fragment.SendingAddressesFragment;
import com.bethel.mycoolwallet.interfaces.IToolbar;
import com.xuexiang.xui.adapter.FragmentAdapter;
import com.xuexiang.xui.widget.tabbar.EasyIndicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AddressBookActivity extends BaseActivity implements IToolbar {
    private   EasyIndicator mEasyIndicator;
    private  ViewPager mViewPager;
    private Toolbar toolBar;

    private static final Logger log = LoggerFactory.getLogger(AddressBookActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.address_book_content);
        mEasyIndicator = findViewById(R.id.easy_indicator);
        mViewPager = findViewById(R.id.view_pager);

        final String[] titles = new String[] {getString(R.string.address_book_list_receiving_title),
                getString(R.string.address_book_list_sending_title)};
        final List<BaseFragment> list = new ArrayList<>();
        list.add(new ReceivingAddressesFragment());
        list.add(new SendingAddressesFragment());

        mEasyIndicator.setTabTitles(titles);
        mEasyIndicator.setViewPager(mViewPager, new FragmentAdapter<>(getSupportFragmentManager(), list));
        mViewPager.setOffscreenPageLimit(list.size() -1);

        toolBar =  initTitleBar(R.string.address_book_activity_title, true);
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, AddressBookActivity.class));
    }

    @Override
    public Toolbar getToolbar() {
        return toolBar;
    }
}
