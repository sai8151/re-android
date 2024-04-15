package com.android.ref;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.webkit.WebView;

public class WebViewRefreshService extends Service {

    private final String url = "https://saikiranreddy.info/api/index.html";
    private final long refreshInterval = 10 * 1000; // 10 seconds

    private WebView webView;
    private Handler handler;
    private Runnable runnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        webView = new WebView(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startRefreshing();
        return START_STICKY;
    }

    private void startRefreshing() {
        if (runnable == null) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl(url);
                    handler.postDelayed(this, refreshInterval);
                }
            };
            handler.post(runnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
