package com.example.remoteh5;

import com.remote.webmode.view.BaseWebActivity;
import com.remote.webmode.view.Html5WebChromeClient;
import com.remote.webmode.view.Html5WebViewClient;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;


/**
 * 用于加载h5页面
 * Created by zhangjianliang on 2018/5/22
 */
public class WebActivity extends BaseWebActivity implements Html5WebViewClient.IUrlloadCallback {

    public static void launch(Context context, String url) {
        launch(context, url, "");
    }

    public static void launch(Context context, String url, String title) {
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(LOAD_URL_KEY, url);
        intent.putExtra(TITLE_KEY, title);
        context.startActivity(intent);
    }

    @Override
    public WebChromeClient getWebChromeClient() {
        return new Html5WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress < 100) {//未完全加载
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                } else {//完全加载
                    mProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                setTitle(title);//接受到标题
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                return true;
            }
        };
    }

    @Override
    public WebViewClient getWebViewClient() {
        return new Html5WebViewClient(this);
    }

    @Override
    public void syncWebViewCookie() {

    }

    @Override
    public void onLoadError(WebView view, int errorCode, String description, String failingUrl) {
        if (errorCode == WebViewClient.ERROR_UNKNOWN || errorCode == WebViewClient.ERROR_CONNECT
                || errorCode == WebViewClient.ERROR_TIMEOUT || errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
            // TODO: 2018/8/1  showNetworkErrorView();
        }
    }
}
