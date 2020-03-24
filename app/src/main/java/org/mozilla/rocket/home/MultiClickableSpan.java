package org.mozilla.rocket.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.View;
import android.content.Context;

public class MultiClickableSpan extends ClickableSpan {

    int pos;
    Context context;
    public MultiClickableSpan(int position, Context context1) {
        this.pos = position;
        this.context = context1;
    }

    @Override
    public void onClick(View widget) {
        Intent webviewIntent = new Intent(context, WebViewActivity.class);
        webviewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle addr = new Bundle();
        switch (pos) {
            case 1:
                addr.putString("url", "https://www.mozilla.org/en-US/MPL/");
                break;
            case 2:
                addr.putString("url", "https://www.mozilla.org/en-US/foundation/trademarks/policy/");
                break;
            case 3:
                addr.putString("url", "file:///android_asset/licenses.html");
                break;
            case 4:
                addr.putString("url", "https://www.mozilla.org/zh-CN/privacy/firefox-lite/");
                break;

        }

        webviewIntent.putExtras(addr);

        context.startActivity(webviewIntent);
    }
}
