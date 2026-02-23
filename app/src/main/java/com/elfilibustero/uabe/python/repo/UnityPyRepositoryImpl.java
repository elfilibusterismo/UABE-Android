package com.elfilibustero.uabe.python.repo;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.elfilibustero.uabe.exceptions.UnityPyException;
import com.elfilibustero.uabe.python.core.UnityPyBridge;
import com.elfilibustero.uabe.python.task.UnityTask;
import com.elfilibustero.uabe.python.task.UnityTaskSource;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UnityPyRepositoryImpl implements UnityPyRepository {

    public static final int ERR_UNKNOWN = 0;
    public static final int ERR_NOT_OK = 1;
    public static final int ERR_THROWABLE = 2;

    private final UnityPyBridge core;
    private final ExecutorService pyExecutor;

    public UnityPyRepositoryImpl(Context context) {
        this.core = new UnityPyBridge(context);
        this.pyExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "UnityPyRepo");
            t.setDaemon(true);
            return t;
        });
    }

    private interface ApiCall<T> {
        ApiResult<T> call() throws Exception;
    }

    private <T> UnityTask<T> runApi(@NonNull ApiCall<T> call) {
        UnityTaskSource<T> src = new UnityTaskSource<>();

        pyExecutor.execute(() -> {
            try {
                ApiResult<T> r = call.call();
                if (r == null) {
                    src.setException(new UnityPyException("Null ApiResult", null, ERR_UNKNOWN));
                    return;
                }
                if (r.ok) {
                    src.setResult(r.data);
                } else {
                    src.setException(new UnityPyException(
                            r.error != null ? r.error : "Operation failed",
                            r.trace,
                            ERR_NOT_OK
                    ));
                }
            } catch (Throwable e) {
                src.setException(new UnityPyException(
                        e.getMessage() != null ? e.getMessage() : e.toString(),
                        null,
                        ERR_THROWABLE
                ));
            }
        });

        return src.getTask();
    }

    @Override
    public UnityTask<OpenBundleResult> openBundle(String localPath) {
        return runApi(() -> core.openBundle(localPath));
    }

    @Override
    public UnityTask<Void> closeBundle(String sessionId) {
        return runApi(() -> core.closeBundle(sessionId));
    }

    @Override
    public UnityTask<Boolean> saveBundle(String sessionId, String outPath) {
        return runApi(() -> core.saveBundle(sessionId, outPath));
    }

    @Override
    public UnityTask<Void> setBundleDecryptionKey(String key) {
        return runApi(() -> core.setBundleDecryptionKey(key));
    }

    @Override
    public UnityTask<ExportFileResult> exportObject(String sessionId, int idx, Uri uri) {
        return runApi(() -> core.exportObject(sessionId, idx, uri));
    }

    @Override
    public UnityTask<Void> importObject(String sessionId, int idx, Uri uri) {
        return runApi(() -> core.importObject(sessionId, idx, uri));
    }

    @Override
    public UnityTask<ObjectData> getObjectData(String sessionId, int idx) {
        return runApi(() -> core.getObjectData(sessionId, idx));
    }

    public UnityTask<Void> setObjectData(String sessionId, int idx, byte[] data) {
        return runApi(() -> core.setObjectData(sessionId, idx, data));
    }

    @Override
    public UnityTask<Map<String, Object>> getObjectInfo(String sessionId, int idx) {
        return runApi(() -> core.getObjectInfo(sessionId, idx));
    }

    @Override
    public void shutdown() {
        pyExecutor.shutdownNow();
    }
}
