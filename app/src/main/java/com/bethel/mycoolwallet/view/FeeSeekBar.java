package com.bethel.mycoolwallet.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.widget.picker.XSeekBar;

import org.bitcoinj.core.Coin;

public class FeeSeekBar extends FrameLayout {
    private XSeekBar mSeekBar;
    private TextView mFeeTv;

    private final static String FEE_FORMAT = "%d satoshis/kb";
    private final static int DEFAULT_MIN_FEE = 1000;
    private final static int DEFAULT_MAX_FEE = 15000;
    private final static int DEFAULT_FEE = 2200;

    private XSeekBar.OnSeekBarListener mOnSeekBarListener;

    public FeeSeekBar(Context context) {
        super(context);
        initView(context);
    }

    public FeeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.fee_seek_bar, this);
        mSeekBar = findViewById(R.id.fee_seek_bar);
        mFeeTv = findViewById(R.id.fee_tv);

        mSeekBar.setMin(DEFAULT_MIN_FEE);
        mSeekBar.setMax(DEFAULT_MAX_FEE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSeekBar.setOnSeekBarListener(seekBarListener);
        mSeekBar.setDefaultValue(DEFAULT_FEE);
    }

    private final XSeekBar.OnSeekBarListener seekBarListener = new XSeekBar.OnSeekBarListener() {
        @Override
        public void onValueChanged(XSeekBar seekBar, int newValue) {
            if (null!=mOnSeekBarListener) mOnSeekBarListener.onValueChanged(seekBar, newValue);
            mFeeTv.setText(String.format(FEE_FORMAT, newValue));
        }
    };

    public void setOnSeekBarListener(XSeekBar.OnSeekBarListener listener) {
        mOnSeekBarListener = listener;
    }

    public void setMin(int min) {
        mSeekBar.setMin(min);
    }

    public int getSelectedNumber() {
        return mSeekBar.getSelectedNumber();
    }

    public void setDefaultValue(int value) {
        mSeekBar.setDefaultValue(value);
    }
    public void setMax(int max) {
        mSeekBar.setMax(max);
    }

    public void reset() {
        mSeekBar.reset();
    }

    public Coin getFee() {
        return Coin.valueOf(getSelectedNumber());
    }
}
