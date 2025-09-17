package com.safeweb.matchdemo.net;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/** Cliente OkHttp com timeouts — igual ao sample, mas minimal. */
public final class SampleAppNetworkingLibExample {
    private static volatile OkHttpClient apiClient;

    public static OkHttpClient getApiClient() {
        if (apiClient == null) {
            synchronized (SampleAppNetworkingLibExample.class) {
                if (apiClient == null) {
                    apiClient = new OkHttpClient.Builder()
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return apiClient;
    }

    private SampleAppNetworkingLibExample() {}
}
