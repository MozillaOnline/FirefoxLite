package org.mozilla.focus.content;

import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;


/**
 * Created by mozillabeijing on 2018/8/9.
 */

public class ContentTray {
    public static void show(FragmentManager manager) {
        if (!manager.isStateSaved()){
            ContentTrayFragment.newInstance().show(manager, ContentTrayFragment.FRAGMENT_TAG);
        }
    }

    public static void dismiss(FragmentManager manager) {
        Fragment contentTray = manager.findFragmentByTag(ContentTrayFragment.FRAGMENT_TAG);
        if (contentTray != null) {
            ((DialogFragment) contentTray).dismissAllowingStateLoss();
        }
    }

    public static boolean isShowing(@Nullable FragmentManager manager) {
        return manager != null && (manager.findFragmentByTag(ContentTrayFragment.FRAGMENT_TAG) != null);
    }
}
