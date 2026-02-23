package com.elfilibustero.uabe.python.repo;

import android.net.Uri;

import com.elfilibustero.uabe.python.task.UnityTask;

import java.util.Map;

public interface UnityPyRepository {

    UnityTask<OpenBundleResult> openBundle(String localPath);

    UnityTask<Void> closeBundle(String sessionId);

    UnityTask<Boolean> saveBundle(String sessionId, String outPath);

    UnityTask<Void> setBundleDecryptionKey(String key);

    UnityTask<ExportFileResult> exportObject(String sessionId, int idx, Uri uri);

    UnityTask<Void> importObject(String sessionId, int idx, Uri uri);

    UnityTask<ObjectData> getObjectData(String sessionId, int idx);

    UnityTask<Void> setObjectData(String sessionId, int idx, byte[] data);

    UnityTask<Map<String, Object>> getObjectInfo(String sessionId, int idx);

    void shutdown();
}
