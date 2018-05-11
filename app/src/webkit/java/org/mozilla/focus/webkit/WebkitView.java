/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.tabs.SiteIdentity;
import org.mozilla.focus.tabs.TabChromeClient;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabViewClient;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.FavIconUtils;
import org.mozilla.focus.utils.FileUtils;
import org.mozilla.focus.utils.IOUtils;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.ThreadUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.Download;
import org.mozilla.focus.web.DownloadCallback;
import org.mozilla.focus.web.WebViewProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.stream.Collectors;

public class WebkitView extends NestedWebView implements TabView {
    private static final String KEY_CURRENTURL = "currenturl";

    private DownloadCallback downloadCallback;
    private FocusWebViewClient webViewClient;
    private FocusWebChromeClient webChromeClient;
    private final LinkHandler linkHandler;

    private boolean shouldReloadOnAttached = false;

    private String lastNonErrorPageUrl;

    public WebkitView(Context context, AttributeSet attrs) {
        super(context, attrs);

        webViewClient = new FocusWebViewClient(getContext().getApplicationContext()) {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!UrlUtils.isInternalErrorURL(url)) {
                    lastNonErrorPageUrl = url;
                }
                super.onPageStarted(view, url, favicon);
            }
        };

        webChromeClient = new FocusWebChromeClient(this);
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(createDownloadListener());

        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true);
        }

        setLongClickable(true);

        linkHandler = new LinkHandler(this, this);
        setOnLongClickListener(linkHandler);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (shouldReloadOnAttached) {
            shouldReloadOnAttached = false;
            reload();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void restoreViewState(Bundle savedInstanceState) {
        // We need to have a different method name because restoreState() returns
        // a WebBackForwardList, and we can't overload with different return types:
        final WebBackForwardList backForwardList = restoreState(savedInstanceState);

        // Pages are only added to the back/forward list when loading finishes. If a new page is
        // loading when the Activity is paused/killed, then that page won't be in the list,
        // and needs to be restored separately to the history list. We detect this by checking
        // whether the last fully loaded page (getCurrentItem()) matches the last page that the
        // WebView was actively loading (which was retrieved during saveViewState():
        // WebView.getUrl() always returns the currently loading or loaded page).
        // If the app is paused/killed before the initial page finished loading, then the entire
        // list will be null - so we need to additionally check whether the list even exists.

        final String desiredURL = savedInstanceState.getString(KEY_CURRENTURL);

        // If WebView was connecting to a non-exist host (ie. 1.1.1.1:42), getUrl() returns null
        // in saveViewState. In any cases we can not get desiredURL, no need to load it.
        if (TextUtils.isEmpty(desiredURL)) {
            return;
        }

        webViewClient.notifyCurrentURL(desiredURL);

        if (backForwardList != null &&
                backForwardList.getCurrentItem().getUrl().equals(desiredURL)) {
            // restoreState doesn't actually load the current page, it just restores navigation history,
            // so we also need to explicitly reload in this case:
            reload();
        } else {
            loadUrl(desiredURL);
        }
    }

    @Override
    public void saveViewState(Bundle outState) {
        super.saveState(outState);
        // See restoreWebViewState() for an explanation of why we need to save this in _addition_
        // to WebView's state
        outState.putString(KEY_CURRENTURL, getUrl());
    }

    @Override
    public void setContentBlockingEnabled(boolean enable) {
        if (webViewClient.isBlockingEnabled() == enable) {
            return;
        }

        webViewClient.setBlockingEnabled(enable);

        if (!enable) {
            reloadOnAttached();
        }
    }

    @Override
    public void bindOnNewWindowCreation(@NonNull Message msg) {
        if (!(msg.obj instanceof WebView.WebViewTransport)) {
            throw new IllegalArgumentException("Message payload is not a WebViewTransport instance");
        }

        final WebView.WebViewTransport transport = (WebView.WebViewTransport) msg.obj;
        transport.setWebView(this);
        msg.sendToTarget();
    }

    public boolean isBlockingEnabled() {
        return webViewClient.isBlockingEnabled();
    }

    @Override
    public void setImageBlockingEnabled(boolean enable) {
        WebSettings settings = getSettings();
        if (enable == settings.getBlockNetworkImage()
                && enable == !settings.getLoadsImagesAutomatically()) {
            return;
        }

        WebViewProvider.applyAppSettings(getContext(), getSettings());

        if (enable) {
            reloadOnAttached();
        }
    }

    @Override
    public void setNightModeEnabled(boolean enable) {
        if (webViewClient.isNightModeEnabled() == enable) {
            return;
        }

        webViewClient.setNightModeEnabled(enable);

        if (!enable) {
            reloadOnAttached();
        } else {
            setBackgroundColor(Color.BLACK);

            // 京东 cannot be displayed correctly using "invert"
            if(getUrl().contains("m.jd.com")){
                evaluateJavascript("(function() { css = document.createElement('link'); css.id = 'moz_rocket'; css.rel = 'stylesheet'; css.href = 'data:text/css,html,link,textarea,select,button,menu,aside,img,strong, body,header,div,a,span,table,tr,td,th,tbody,p,form,input,ul,ol,li,dl,dt,dd,section,footer,nav,h1,h2,h3,h4,h5,h6,em,pre{background: #333 !important;color:#616161!important;border-color:#454530!important;text-shadow:0!important;-webkit-text-fill-color : none!important;}html a,html a *{color:#5a8498!important;text-decoration:underline!important;}html a:visited,html a:visited *,html a:active,html a:active *{color:#505f64!important;}html a:hover,html a:hover *{color:#cef!important;}html input,html select,html button,html textarea{background:#4d4c40!important;border:1px solid #5c5a46!important;border-top-color:#494533!important;border-bottom-color:#494533!important;}html input[type=button],html input[type=submit],html input[type=reset],html input[type=image],html button{border-top-color:#494533!important;border-bottom-color:#494533!important;}html input:focus,html select:focus,html option:focus,html button:focus,html textarea:focus{background:#5c5b3e!important;color:#fff!important;border-color:#494100 #635d00 #474531!important;outline:1px solid #041d29!important;}html input[type=button]:focus,html input[type=submit]:focus,html input[type=reset]:focus,html input[type=image]:focus,html button:focus{border-color:#494533 #635d00 #474100!important;}html input[type=radio]{background:none!important;border-color:#333!important;border-width:0!important;}html img[src],html input[type=image]{opacity:.5;}html img[src]:hover,html input[type=image]:hover{opacity:1;}html,html body {scrollbar-base-color: #4d4c40 !important;scrollbar-face-color: #5a5b3c !important;scrollbar-shadow-color: #5a5b3c !important;scrollbar-highlight-color: #5c5b3c !important;scrollbar-dlight-color: #5c5b3c !important;scrollbar-darkshadow-color: #474531 !important;scrollbar-track-color: #4d4c40 !important;scrollbar-arrow-color: #000 !important;scrollbar-3dlight-color: #6a6957 !important;}dt a{background-color: #333 !important;}'; document.getElementsByTagName('head')[0].appendChild(css);})(); ",null);

            }else {
                String js = "javascript: ("
                        + "function () { "

                        + "var css = 'html {-webkit-filter: hue-rotate(180deg) invert(100%) !important;}'+"
                        + "          'html {background:#222222 !important;}'+"
                        + "          'img,video {-webkit-filter: brightness(80%) invert(100%) hue-rotate(180deg) !important;}',"

                        + "head = document.getElementsByTagName('head')[0],"
                        + "style = document.createElement('style');"

                        + "style.type = 'text/css';"
                        + "if (style.styleSheet){"
                        + "style.styleSheet.cssText = css;"
                        + "} else {"
                        + "style.appendChild(document.createTextNode(css));"
                        + "}"

                        //injecting the css to the head
                        + "head.appendChild(style);"
                        + "}());";
                evaluateJavascript(js, null);
            }
        }
    }

    @Override
    public void performExitFullScreen() {
        evaluateJavascript("(function() { return document.webkitExitFullscreen(); })();", null);
    }

    @Override
    public void setViewClient(TabViewClient viewClient) {
        this.webViewClient.setViewClient(viewClient);
    }

    @Override
    public void setChromeClient(TabChromeClient chromeClient) {
        linkHandler.setChromeClient(chromeClient);
        this.webChromeClient.setChromeClient(chromeClient);
    }

    @Override
    public void setDownloadCallback(DownloadCallback callback) {
        this.downloadCallback = callback;
    }

    public void loadUrl(String url) {
        // We need to check external URL handling here - shouldOverrideUrlLoading() is only
        // called by webview when clicking on a link, and not when opening a new page for the
        // first time using loadUrl().
        if (!webViewClient.shouldOverrideUrlLoading(this, url)) {
            super.loadUrl(url);
        }

        webViewClient.notifyCurrentURL(url);
    }

    public void reload() {
        if (UrlUtils.isInternalErrorURL(getOriginalUrl())) {
            super.loadUrl(getUrl());
        } else {
            super.reload();
        }
    }

    @Override
    public String getUrl() {
        final String currentUrl = super.getUrl();
        if (UrlUtils.isInternalErrorURL(currentUrl)) {
            return lastNonErrorPageUrl;
        } else {
            return currentUrl;
        }
    }

    private void reloadOnAttached() {
        if (isAttachedToWindow()) {
            reload();
        } else {
            shouldReloadOnAttached = true;
        }
    }

    @Override
    public @SiteIdentity.SecurityState
    int getSecurityState() {
        // FIXME: Having certificate doesn't mean the connection is secure, see #1562
        return getCertificate() == null ? SiteIdentity.INSECURE : SiteIdentity.SECURE;
    }

    @Override
    public void cleanup() {
        clearFormData();
        clearHistory();
        clearMatches();
        clearSslPreferences();
        clearCache(true);

        // We don't care about the viewClient - we just want to make sure cookies are gone
        CookieManager.getInstance().removeAllCookies(null);

        WebStorage.getInstance().deleteAllData();

        final WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(getContext());
        // It isn't entirely clear how this differs from WebView.clearFormData()
        webViewDatabase.clearFormData();
        webViewDatabase.clearHttpAuthUsernamePassword();
    }

    public static void deleteContentFromKnownLocations(final Context context) {
        ThreadUtils.postToBackgroundThread(new Runnable() {
            @Override
            public void run() {
                // We call all methods on WebView to delete data. But some traces still remain
                // on disk. This will wipe the whole webview directory.
                FileUtils.deleteWebViewDirectory(context);

                // WebView stores some files in the cache directory. We do not use it ourselves
                // so let's truncate it.
                FileUtils.truncateCacheDirectory(context);
            }
        });
    }

    public void insertBrowsingHistory() {
        final String url = getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        } else if (SupportUtils.BLANK_URL.equals(url)) {
            return;
        }

        if (!UrlUtils.isHttpOrHttps(url)) {
            return;
        }

        evaluateJavascript("(function() { return document.getElementById('mozillaErrorPage'); })();",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String errorPage) {
                        if (!"null".equals(errorPage)) {
                            return;
                        }

                        Site site = new Site();
                        site.setUrl(url);
                        site.setTitle(getTitle());
                        site.setLastViewTimestamp(System.currentTimeMillis());
                        site.setFavIcon(FavIconUtils.getInitialBitmap(getResources(), null, FavIconUtils.getRepresentativeCharacter(url)));
                        BrowsingHistoryManager.getInstance().insert(site, null);
                    }
                });
    }

    @Override
    public View getView() {
        return this;
    }


    private DownloadListener createDownloadListener() {
        return new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if (!AppConstants.supportsDownloadingFiles()) {
                    return;
                }

                if (downloadCallback != null) {
                    final Download download = new Download(url, userAgent, contentDisposition, mimetype, contentLength, false);

                    AlertDialog.Builder localBuilder = new AlertDialog.Builder(getContext());
                    localBuilder.setTitle("Rocket将要下载此文件" );
                    localBuilder.setMessage("是否允许此次下载" );
                    localBuilder.setPositiveButton("是" , new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt)
                        {

                            downloadCallback.onDownloadStart(download);
                        }
                    });
                    localBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt)
                        {
                            return;
                        }
                    });

                    localBuilder.setCancelable(false).create();

                    localBuilder.show();

                }
            }
        };
    }
}
