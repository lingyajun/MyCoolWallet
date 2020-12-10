package com.bethel.mycoolwallet.helper;

import android.app.Activity;
import android.content.res.Resources;
import android.text.Html;
import android.view.Gravity;

import androidx.core.content.ContextCompat;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.data.tx_list.ColorType;
import com.xuexiang.xui.widget.guidview.GuideCaseQueue;
import com.xuexiang.xui.widget.guidview.GuideCaseView;

public class DisclaimerHelper {
    private static final int[] messages = {R.string.help_safety1, R.string.help_safety2,
            R.string.help_safety3, R.string.help_safety4, R.string.help_safety5, };

    public static void show(final Activity activity) {
        final int colorBg = ContextCompat.getColor(activity, R.color.disclaimer_bg);
        final GuideCaseQueue queue = new GuideCaseQueue();
        for (int id: messages  ) {
            final GuideCaseView guide = new GuideCaseView.Builder(activity)
                    .title(Html.fromHtml(activity.getString(id)))
                    .backgroundColor(colorBg)
                    .titleStyle(R.style.MyTitleStyle, Gravity.CENTER)
                    .build();
            queue.add(guide);
        }

        queue.show();
    }
}
