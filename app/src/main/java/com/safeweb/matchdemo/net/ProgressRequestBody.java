package com.safeweb.matchdemo.net;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

/** Wrapper para reportar progresso de upload (igual ao sample). */
public final class ProgressRequestBody extends RequestBody {
    public interface Listener { void onUploadProgressChanged(long bytesWritten, long totalBytes); }

    private final RequestBody delegate;
    private final Listener listener;

    public ProgressRequestBody(RequestBody delegate, Listener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override public MediaType contentType() { return delegate.contentType(); }
    @Override public long contentLength() throws IOException { return delegate.contentLength(); }

    @Override public void writeTo(BufferedSink sink) throws IOException {
        long total = contentLength();
        CountingSink counting = new CountingSink(sink, total, listener);
        BufferedSink buffered = Okio.buffer(counting);
        delegate.writeTo(buffered);
        buffered.flush();
    }

    private static final class CountingSink extends ForwardingSink {
        private long written = 0L; private final long total; private final Listener listener;
        CountingSink(BufferedSink d, long total, Listener l) { super(d); this.total = total; this.listener = l; }
        @Override public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            written += byteCount;
            listener.onUploadProgressChanged(written, total);
        }
    }
}
