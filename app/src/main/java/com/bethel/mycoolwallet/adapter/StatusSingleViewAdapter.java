/*
 * Copyright (C) 2020 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.bethel.mycoolwallet.adapter;

import android.view.View;

import com.bethel.mycoolwallet.R;
import com.xuexiang.xui.utils.ThemeUtils;
import com.xuexiang.xui.widget.statelayout.StatefulLayout;
import com.xuexiang.xui.widget.statelayout.StatusLoader;
//import com.xuexiang.xuidemo.R;

import static com.xuexiang.xui.widget.statelayout.StatusLoader.STATUS_CUSTOM;
import static com.xuexiang.xui.widget.statelayout.StatusLoader.STATUS_EMPTY_DATA;
import static com.xuexiang.xui.widget.statelayout.StatusLoader.STATUS_LOADING;
import static com.xuexiang.xui.widget.statelayout.StatusLoader.STATUS_LOAD_FAILED;
import static com.xuexiang.xui.widget.statelayout.StatusLoader.STATUS_LOAD_SUCCESS;

/**
 * 复用组件
 *
 * @author xuexiang
 * @since 2020/4/30 12:09 AM
 */
public class StatusSingleViewAdapter implements StatusLoader.Adapter {

    @Override
    public View getView(StatusLoader.Holder holder, View convertView, int status) {
        StatefulLayout statefulLayout = null;
        // 进行复用
        if (convertView instanceof StatefulLayout) {
            statefulLayout = (StatefulLayout) convertView;
        }
        if (statefulLayout == null) {
            statefulLayout = new StatefulLayout(holder.getContext());
            statefulLayout.setAnimationEnabled(false);
            statefulLayout.attachTemplate();
            statefulLayout.setBackgroundColor(ThemeUtils.resolveColor(holder.getContext(), R.attr.xui_config_color_background));
        }

        statefulLayout.setVisibility(status == STATUS_LOAD_SUCCESS ? View.GONE : View.VISIBLE);
        switch (status) {
            case STATUS_LOADING:
                showLoading(statefulLayout);
                break;
            case STATUS_LOAD_FAILED:
                showError(statefulLayout, holder.getRetryListener());
                break;
            case STATUS_EMPTY_DATA:
                showEmpty(statefulLayout);
                break;
            case STATUS_CUSTOM:
                showOffline(statefulLayout, holder.getRetryListener());
                break;
            default:
                break;
        }
        return statefulLayout;
    }

    protected void showOffline(StatefulLayout statefulLayout, View.OnClickListener retryListener) {
        statefulLayout.showOffline(retryListener);
    }

    protected void showEmpty(StatefulLayout statefulLayout) {
        statefulLayout.showEmpty();
    }

    protected void showError(StatefulLayout statefulLayout, View.OnClickListener retryListener) {
        statefulLayout.showError(retryListener);
    }

    protected void showLoading(StatefulLayout statefulLayout) {
        statefulLayout.showLoading();
    }


}
