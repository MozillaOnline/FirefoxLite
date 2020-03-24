package org.mozilla.rocket.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import org.mozilla.focus.R;

public class WebViewActivity extends Activity {

    private WebView webView;
    private ImageButton closeButton;
    private String url = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        Bundle addr = getIntent().getExtras();
        if (addr != null) {
            url = addr.getString("url");
        }

        webView = (WebView) findViewById(R.id.webView);
        closeButton = (ImageButton) findViewById(R.id.closeButton);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url1) {
                view.loadUrl(url1);
                return false;
            }
        });
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);
    }


}
