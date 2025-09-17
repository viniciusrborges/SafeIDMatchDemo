package com.safeweb.matchdemo;

import androidx.annotation.NonNull;
import com.facetec.sdk.FaceTecSessionRequestProcessor;

/** Recebe o requestBlob do SDK e delega para a camada de rede. */
public final class SessionRequestProcessor implements FaceTecSessionRequestProcessor {

    @Override
    public void onSessionRequest(@NonNull String sessionRequestBlob,
                                 @NonNull FaceTecSessionRequestProcessor.Callback sessionRequestCallback) {
        com.safeweb.matchdemo.net.SampleAppNetworkingRequest.send(
                this, sessionRequestBlob, sessionRequestCallback
        );
    }

    // Chamados pela camada de rede:
    public void onResponseBlobReceived(@NonNull String responseBlob,
                                       @NonNull FaceTecSessionRequestProcessor.Callback sessionRequestCallback) {
        sessionRequestCallback.processResponse(responseBlob);
    }

    public void onUploadProgress(float progress,
                                 @NonNull FaceTecSessionRequestProcessor.Callback sessionRequestCallback) {
        sessionRequestCallback.updateProgress(progress);
    }

    public void onCatastrophicNetworkError(@NonNull FaceTecSessionRequestProcessor.Callback sessionRequestCallback) {
        sessionRequestCallback.abortOnCatastrophicError();
    }
}
