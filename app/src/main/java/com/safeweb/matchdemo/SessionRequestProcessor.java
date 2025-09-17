package com.safeweb.matchdemo;

import androidx.annotation.NonNull;
import com.facetec.sdk.FaceTecSessionRequestProcessor;

public final class SessionRequestProcessor implements FaceTecSessionRequestProcessor {

    @Override
    public void onSessionRequest(@NonNull String sessionRequestBlob,
                                 @NonNull Callback sessionRequestCallback) {
        com.safeweb.matchdemo.net.SampleAppNetworkingRequest.send(
                this, sessionRequestBlob, sessionRequestCallback
        );
    }

    public void onResponseBlobReceived(@NonNull String responseBlob,
                                       @NonNull Callback sessionRequestCallback) {
        sessionRequestCallback.processResponse(responseBlob);
    }

    public void onUploadProgress(float progress,
                                 @NonNull Callback sessionRequestCallback) {
        sessionRequestCallback.updateProgress(progress);
    }

    public void onCatastrophicNetworkError(@NonNull Callback sessionRequestCallback) {
        sessionRequestCallback.abortOnCatastrophicError();
    }
}
