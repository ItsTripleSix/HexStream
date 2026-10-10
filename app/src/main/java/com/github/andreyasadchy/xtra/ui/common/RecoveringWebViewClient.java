package com.github.andreyasadchy.xtra.ui.common;

import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.webkit.WebViewClientCompat;

/** Shared renderer cleanup; the failed browser screen must be reopened with a new WebView. */
public abstract class RecoveringWebViewClient extends WebViewClientCompat {
    @Override
    public boolean onRenderProcessGone(@NonNull WebView view, @NonNull RenderProcessGoneDetail detail) {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        view.destroy();
        onBrowserFailure();
        return true;
    }

    protected abstract void onBrowserFailure();
}
