package com.bethel.mycoolwallet.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Utils;
import com.bethel.mycoolwallet.utils.ViewUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.OnClick;

public class WebFragment extends BaseFragment implements DownloadListener {
    @BindView(R.id.wv_browser)
    protected WebView mWebView;
    @BindView(R.id.layout_loading)
    protected View mVLoading;
    @BindView(R.id.layout_load_failed)
    protected View mVLoadFailed;
    @BindView(R.id.toolbar)
    Toolbar toolBar;

    @OnClick(R.id.layout_load_failed)
    void onFailedViewClick() {
        mWebView.reload();
    }

    protected String mUrl;
    protected String mTitle;
    protected boolean isForeClose = false;

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";
    private static final int MSG_SHOW_LOADING = 1;
    private static final Logger log = LoggerFactory.getLogger(WebFragment.class);

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == MSG_SHOW_LOADING) {
                ViewUtil.setVisibility(mVLoading, View.VISIBLE);
            }
        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getActivity().getIntent().getExtras();
//        Bundle data = getArguments();
        if (data != null) {
            mUrl = data.getString(EXTRA_URL);
            mTitle = data.getString(EXTRA_TITLE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initTitleBar();

        mWebView.requestFocus();
        mWebView.setWebViewClient(webViewClient);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        // 解决 has leaked window
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.setDownloadListener(this);

        mWebView.loadUrl(mUrl);
    }

    protected void initTitleBar() {
        if (null!=mTitle)
            toolBar.setTitle(mTitle);
        Drawable d= Utils.zoomImage(getResources(), R.drawable.ic_navigation_back_white,
                Utils.dip2px(getContext(), 30), Utils.dip2px(getContext(), 26));
        toolBar.setNavigationIcon(d); // toolbar的左侧返回按钮
        toolBar.setNavigationOnClickListener((v)-> onBackPressed());
    }

    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            onWebPageStarted(view, url, favicon);
        }

        public void onReceivedError(WebView view, int errorCode, String description,
                                    String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            log.info( "onReceivedError url: {}, errorCode: {}", failingUrl, errorCode);
            ViewUtil.showView(mVLoadFailed, true);
        }

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // return shouldWebPageOverrideUrl(view, url);
            if (!handleCallback(url)) {
                view.loadUrl(url);
            }
            return true;
        }

        public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler,
                                       android.net.http.SslError error) {
            handler.proceed();
        };

        @Override
        public void onPageFinished(WebView view, String url) {
            log.debug( "onPageFinished url: {}", url);
            super.onPageFinished(view, url);
            onWebPageFinished(view, url);
        }

    };

    protected void onWebPageStarted(WebView view, String url, Bitmap favicon) {
        if (!mHandler.hasMessages(MSG_SHOW_LOADING)) {
            mHandler.sendEmptyMessageDelayed(MSG_SHOW_LOADING, 500);
        }
        ViewUtil.showView(mVLoadFailed, false);
        log.info( "onPageStarted url: {}", url);
    }

    protected void onWebPageFinished(WebView view, String url) {
        log.info(  "onWebPageFinished url: {}", url);
        if (mHandler.hasMessages(MSG_SHOW_LOADING)) {
            mHandler.removeMessages(MSG_SHOW_LOADING);
        }
        ViewUtil.showView(mVLoading, false);
    }

    /**
     * 处理网页端回调客户端 url 类似: "objc://gotoProfile/uid#xx" 方法名: gotoProfile #号分隔各个参数
     *
     * @param url
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected boolean handleCallback(String url) {
        Matcher matcher = Pattern.compile("objc://(\\w*)/?(.*?)").matcher(url);
        if (matcher.matches()) {
            String method = matcher.group(1);
            String param = matcher.group(2);
            String[] params = null;
            Class[] classes = null;
            if (!TextUtils.isEmpty(param)) {
                params = param.split("#");
                classes = new Class[params.length];
                for (int i = 0; i < params.length; i++) {
                    classes[i] = String.class;
                }
            }
            if (null==method || null== params) return false;
            try {//WebFragment.class
                Method m = getClass().getDeclaredMethod(method, classes);
                m.setAccessible(true);
                final Object[] objects = params;
                m.invoke(WebFragment.this, objects);
//                m.invoke(WebFragment.this, params); // 警告: 最后一个参数使用了不准确的变量类型的 varargs 方法的非 varargs 调用
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

//    @Override
    public boolean onBackPressed() {
        if (!isForeClose && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        getActivity().onBackPressed();
        return false;
    }

    @Override
    public void onDestroyView() {
        if (null!=mWebView) {
            mWebView.stopLoading();
            mWebView.freeMemory();
        }
        super.onDestroyView();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.web_layout;
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                String mimetype, long contentLength) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
