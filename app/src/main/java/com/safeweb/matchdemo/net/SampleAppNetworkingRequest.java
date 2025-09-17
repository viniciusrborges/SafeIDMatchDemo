package com.safeweb.matchdemo.net;

import android.util.Log;
import androidx.annotation.NonNull;

import com.facetec.sdk.FaceTecSDK;
import com.facetec.sdk.FaceTecSessionRequestProcessor;
import com.safeweb.matchdemo.Config;
import com.safeweb.matchdemo.SessionRequestProcessor;

import org.json.JSONObject;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

public final class SampleAppNetworkingRequest {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void send(
            @NonNull SessionRequestProcessor referencingProcessor,
            @NonNull String sessionRequestBlob,
            @NonNull FaceTecSessionRequestProcessor.Callback sessionRequestCallback
    ) {
        try {
            JSONObject payload = new JSONObject().put("requestBlob", sessionRequestBlob);
            RequestBody body = RequestBody.create(JSON, payload.toString());

            Request request = new Request.Builder()
                    .url(Config.YOUR_API_OR_FACETEC_TESTING_API_ENDPOINT)
                    .addHeader("Content-Type", "application/json")
                    // SOMENTE para a Testing API:
                    .addHeader("X-Device-Key", Config.DeviceKeyIdentifier)
                    .addHeader("X-Testing-API-Header", FaceTecSDK.getTestingAPIHeader())
                    .post(new ProgressRequestBody(body, (written, total) -> {
                        float p = total > 0 ? (float) written / (float) total : 0f;
                        referencingProcessor.onUploadProgress(p, sessionRequestCallback);
                    }))
                    .build();

            SampleAppNetworkingLibExample.getApiClient().newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("FaceTec", "Network error", e);
                    referencingProcessor.onCatastrophicNetworkError(sessionRequestCallback);
                }
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String resp = response.body() != null ? response.body().string() : "";
                    referencingProcessor.onResponseBlobReceived(resp, sessionRequestCallback);
                }
            });

        } catch (Exception e) {
            Log.e("FaceTec", "Unexpected error building request", e);
            referencingProcessor.onCatastrophicNetworkError(sessionRequestCallback);
        }
    }

    private SampleAppNetworkingRequest() {}
}
