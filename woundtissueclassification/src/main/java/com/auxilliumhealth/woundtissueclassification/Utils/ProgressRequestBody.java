package com.auxilliumhealth.woundtissueclassification.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    /** Interface to report upload progress */
    public interface UploadCallbacks {
        void onProgressUpdate(int percentage);  // called with 0-100
        void onError();                         // optional: called if error occurs
        void onFinish();                        // optional: called when upload finishes
    }

    private File file;
    private String contentType;
    private UploadCallbacks listener;

    public ProgressRequestBody(File file, String contentType, UploadCallbacks listener) {
        this.file = file;
        this.contentType = contentType;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[4096];
        FileInputStream in = new FileInputStream(file);
        long uploaded = 0;

        try {
            int read;
            while ((read = in.read(buffer)) != -1) {
                // Write bytes to sink
                sink.write(buffer, 0, read);
                uploaded += read;

                // Report progress
                if (listener != null) {
                    int progress = (int) (100 * uploaded / fileLength);
                    listener.onProgressUpdate(progress);
                }
            }
            // Finished
            if (listener != null) listener.onFinish();
        } catch (Exception e) {
            if (listener != null) listener.onError();
            throw e;
        } finally {
            in.close();
        }
    }
}
