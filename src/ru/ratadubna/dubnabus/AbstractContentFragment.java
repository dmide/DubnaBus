package ru.ratadubna.dubnabus;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;

abstract public class AbstractContentFragment extends WebViewFragment {
    abstract String getPage();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container,
                savedInstanceState);
        getWebView().setWebViewClient(new WebViewClient());
        getWebView().getSettings().setJavaScriptEnabled(true);
        getWebView().getSettings().setSupportZoom(true);
        getWebView().getSettings().setBuiltInZoomControls(true);
        String content = getPage();
        if (!content.substring(0, 4).equals("http")) {
            try {
                getWebView().loadData(URLEncoder.encode(content, "UTF-8").replaceAll("\\+", " "),
                        "text/html; charset=UTF-8", null);
            } catch (UnsupportedEncodingException e) {
                Log.e(getClass().getSimpleName(),
                        "Exception loading taxi data", e);
            }
        } else {
            getWebView().loadUrl(content);
        }
        return (result);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setUserVisibleHint(true);
    }
}