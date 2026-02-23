package com.elfilibustero.uabe.python.core;

import static com.elfilibustero.uabe.util.Utils.GSON;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.elfilibustero.uabe.managers.SessionManager;
import com.elfilibustero.uabe.model.ObjectItem;
import com.elfilibustero.uabe.python.repo.ApiResult;
import com.elfilibustero.uabe.python.repo.ExportFileResult;
import com.elfilibustero.uabe.python.repo.ObjectData;
import com.elfilibustero.uabe.python.repo.OpenBundleResult;
import com.elfilibustero.uabe.python.repo.SaveBundleResult;
import com.elfilibustero.uabe.util.DocumentUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * UnityPyBridgeCore (SYNC):
 * - Direct UnityPy bridge using Chaquopy
 * - Returns ApiResult<T>
 * - Intended to be called from a Repository which handles threading
 */
public final class UnityPyBridge {

    private static final String TAG = "UnityPyBridge";

    private final Context context;
    private final PyObject unitypy;
    private final PyObject json;
    private final PyObject pilImage;
    private final PyObject io;

    private final SessionManager sessionManager;

    public UnityPyBridge(Context context) {
        this.context = context;
        Python py = Python.getInstance();
        unitypy = py.getModule("UnityPy");
        json = py.getModule("json");
        pilImage = py.getModule("PIL.Image");
        io = py.getModule("io");
        sessionManager = SessionManager.get();
    }

    public ApiResult<OpenBundleResult> openBundle(String localPath) {
        try {
            File in = new File(localPath);
            if (!in.exists()) {
                return fail("Input not found: " + localPath, null);
            }

            PyObject env = unitypy.callAttr("load", in.getAbsolutePath());
            String sessionId = uuid12();

            SessionManager.Session s = new SessionManager.Session();
            s.env = env;
            s.sessionId = sessionManager.create(s);
            sessionManager.put(sessionId, s);

            PyObject objects = env.get("objects");
            List<PyObject> objList = (objects != null) ? objects.asList() : Collections.emptyList();

            List<String> archives = archiveNames(env);
            List<ObjectItem> outObjects = new ArrayList<>(objList.size());

            LinkedHashSet<String> typeSet = new LinkedHashSet<>();

            for (int i = 0; i < objList.size(); i++) {
                PyObject obj = objList.get(i);

                String t = tname(obj);
                long pid = safeLong(getAttr(obj, "path_id"), 0);

                ObjectItem item = new ObjectItem();
                item.setIndex(i);
                item.setType(t);
                item.setId(pid);
                item.setBytes(tryGetSize(obj));

                String nm = extractName(obj);
                item.setName(!nm.isEmpty() ? nm : "Unnamed asset");
                item.setContainer(tryGetContainer(obj));

                outObjects.add(item);

                if (!t.trim().isEmpty()) {
                    String tt = t.trim();
                    typeSet.add(tt);
                }
            }

            OpenBundleResult resp = new OpenBundleResult();
            resp.sessionId = sessionId;
            resp.archives = archives;
            resp.objects = outObjects;
            resp.types = new ArrayList<>(typeSet);

            return ok(resp);

        } catch (Throwable t) {
            Log.e(TAG, "openBundle failed: " + t);
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public ApiResult<Void> closeBundle(String sessionId) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                return fail("Invalid sessionId", null);
            }
            sessionManager.remove(sessionId);
            return ok(null);
        } catch (Throwable t) {
            Log.d(TAG, "closeBundle failed: " + t);
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public ApiResult<Boolean> saveBundle(String sessionId, String outPath) {
        File out = new File(outPath);
        File tmp = new File(out.getParentFile(), out.getName() + ".tmp");
        try {
            SessionManager.Session s = sessionManager.require(sessionId);
            PyObject env = s.env;

            // data = env.file.save()
            PyObject envFile = env.get("file");
            if (envFile != null && !envFile.isEmpty()) {
                byte[] bytes = pyBytes(envFile.callAttr("save"));
                writeFile(tmp, bytes);
                s.dirty = false;

                SaveBundleResult r = new SaveBundleResult();
                r.path = tmp.getAbsolutePath();
                r.bytes = bytes.length;

                if (out.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    out.delete();
                    return ok(tmp.renameTo(out));
                }
            }
            if (out.exists()) {
                //noinspection ResultOfMethodCallIgnored
                out.delete();
            }
            return fail("save_bundle not supported (env.file missing)", null);
        } catch (Throwable t) {
            if (out.exists()) {
                //noinspection ResultOfMethodCallIgnored
                out.delete();
            }
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public ApiResult<Void> setBundleDecryptionKey(String key) {
        try {
            unitypy.callAttr("set_assetbundle_decrypt_key", key);
            return ok(null);
        } catch (Throwable t) {
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public byte[] getObjectData(PyObject obj) {
        return switch (tname(obj)) {
            case "TextAsset" -> exportTextAsset(obj);
            case "Texture2D" -> exportTexture2D(obj);
            case "Mesh" -> exportMesh(obj);
            case "MonoBehaviour" -> exportMonoBehaviour(obj);
            case "GameObject",
                 "AssetBundle" -> exportTypeTreeJson(obj);
            default -> new byte[0];
        };
    }

    public ApiResult<ExportFileResult> exportObject(String sessionId, int idx, Uri dest) {
        try {
            SessionManager.Session s = sessionManager.get(sessionId);
            assert s != null;
            PyObject obj = getObject(s.env, idx);

            String type = tname(obj);
            Log.d(TAG, "exportObject: " + type);
            byte[] data = getObjectData(obj);
            if (data.length == 0) {
                return fail("export_object not supported for type: " + type, null);
            }
            DocumentUtil.writeBytesToUri(context, dest, data);
            ExportFileResult r = new ExportFileResult();
            r.idx = idx;
            r.type = type;
            return ok(r);

        } catch (Throwable t) {
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public ApiResult<Void> importObject(String sessionId, int idx, Uri uri) {
        try {
            SessionManager.Session s = sessionManager.get(sessionId);
            assert s != null;
            PyObject obj = getObject(s.env, idx);

            byte[] dataBytes = DocumentUtil.readBytesFromUri(context, uri, 1024 * 1024);

            String t = tname(obj);
            if ("Texture2D".equals(t)) {
                PyObject tex = obj.callAttr("parse_as_object");
                PyObject bio = io.callAttr("BytesIO", (Object) dataBytes);
                PyObject img = pilImage.callAttr("open", bio);
                tex.put("image", img);
                tex.callAttr("save");
                s.dirty = true;
                return ok(null);
            }

            if ("TextAsset".equals(t)) {
                PyObject txt = obj.callAttr("parse_as_object");

                String text = new String(dataBytes, StandardCharsets.UTF_8);
                txt.put("m_Script", text);
                txt.callAttr("save");

                s.dirty = true;
                return ok(null);
            }

            if ("MonoBehaviour".equals(t) || "GameObject".equals(t) || "AssetBundle".equals(t)) {
                try {
                    String jsonText = decodeBytesToText(dataBytes).trim();
                    if (jsonText.isEmpty()) {
                        return fail("Input JSON is empty.", null);
                    }

                    Object parsed = GSON.fromJson(jsonText, Object.class);
                    if (!(parsed instanceof Map)) {
                        return fail("Typetree JSON must be a JSON object (dict).", null);
                    }

                    PyObject pyTree = json.callAttr("loads", jsonText);
                    saveTypeTree(obj, pyTree);
                    s.dirty = true;
                    return ok(null);
                } catch (Throwable t2) {
                    return fail(msgOf(t2), androidTrace(t2));
                }
            }

            return fail("import_object not supported for type: " + t, null);

        } catch (Throwable t) {
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public ApiResult<Void> setObjectData(String sessionId, int idx, byte[] data) {
        try {
            SessionManager.Session s = sessionManager.get(sessionId);
            assert s != null;
            PyObject obj = getObject(s.env, idx);
            String t = tname(obj);
            if ("TextAsset".equals(t)) {
                PyObject txt = obj.callAttr("parse_as_object");
                String text = new String(data, StandardCharsets.UTF_8);
                txt.put("m_Script", text);
                txt.callAttr("save");
                s.dirty = true;
                return ok(null);
            }
            if ("Mesh".equals(t)) {
                String msg = "export_mesh not supported for type: " + t;
                return fail(msg, null);
            }
            if ("Texture2D".equals(t)) {
                PyObject tex = obj.callAttr("parse_as_object");
                PyObject bio = io.callAttr("BytesIO", (Object) data);
                PyObject img = pilImage.callAttr("open", bio);
                tex.put("image", img);
                tex.callAttr("save");
                s.dirty = true;
                return ok(null);
            }
            if ("MonoBehaviour".equals(t) || "GameObject".equals(t) || "AssetBundle".equals(t)) {
                String jsonText = new String(data, StandardCharsets.UTF_8);
                Object parsed = GSON.fromJson(jsonText, Object.class);
                if (!(parsed instanceof Map)) {
                    return fail("Typetree JSON must be a JSON object (dict).", null);
                }

                PyObject pyTree = json.callAttr("loads", jsonText);
                saveTypeTree(obj, pyTree);
                s.dirty = true;
                return ok(null);
            }
            return fail("set_object_data not supported for type: " + t, null);
        } catch (Throwable t) {
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public ApiResult<ObjectData> getObjectData(String sessionId, int idx) {
        try {
            SessionManager.Session s = sessionManager.get(sessionId);
            assert s != null;
            PyObject obj = getObject(s.env, idx);
            ObjectData data = new ObjectData();
            data.setSessionId(sessionId);
            data.setIdx(idx);
            data.setId(safeLong(getAttr(obj, "path_id"), 0));
            data.setName(extractName(obj));
            data.setType(tname(obj));
            data.setData(getObjectData(obj));
            return ok(data);
        } catch (Throwable t) {
            return fail(msgOf(t), androidTrace(t));
        }
    }

    public ApiResult<Map<String, Object>> getObjectInfo(String sessionId, int idx) {
        try {
            SessionManager.Session s = sessionManager.get(sessionId);
            assert s != null;
            PyObject obj = getObject(s.env, idx);

            String t = tname(obj);
            String name = exportBaseName(obj, idx);
            String ext = getExtension(obj, t);

            Map<String, Object> info = new HashMap<>();
            info.put("type", t);
            info.put("filename", name);
            info.put("ext", ext);
            info.put("mime", getMimeType(t));
            return ok(info);
        } catch (Throwable t) {
            return fail(msgOf(t), androidTrace(t));
        }
    }

    // ------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------

    private PyObject getObject(@NonNull PyObject env, int idx) {
        PyObject objects = env.get("objects");
        List<PyObject> list = (objects != null) ? objects.asList() : Collections.emptyList();
        if (idx < 0 || idx >= list.size()) {
            throw new IndexOutOfBoundsException("Index out of range: " + idx + " / " + list.size());
        }
        return list.get(idx);
    }

    private String tname(PyObject obj) {
        try {
            return Objects.requireNonNull(Objects.requireNonNull(obj.get("type")).get("name"))
                    .toString();
        } catch (Throwable ignored) {
            try {
                PyObject typeObj = obj.get("type");
                String s = (typeObj == null || typeObj.isEmpty()) ? "" : typeObj.toString();
                if (s.startsWith("ClassIDType.")) {
                    s = s.substring("ClassIDType.".length());
                }
                return s.isEmpty() ? "Unknown" : s;
            } catch (Throwable ignored2) {
                return "Unknown";
            }
        }
    }

    @NonNull
    private String getMimeType(@NonNull String type) {
        return switch (type) {
            case "TextAsset" -> "text/plain";
            case "Texture2D" -> "image/png";
            case "MonoBehaviour", "GameObject", "AssetBundle" -> "application/json";
            default -> "application/octet-stream";
        };
    }

    @NonNull
    private String getExtension(PyObject obj, @NonNull String type) {
        if ("TextAsset".equals(type)) {
            String name = tryGetContainer(obj);
            if (name == null) {
                name = "textasset_" + safeLong(getAttr(obj, "path_id"), 0);
            }
            if (!name.endsWith(".txt") && !name.endsWith(".bytes") && !name.endsWith(
                    ".json") && !name.endsWith(".lua")) {
                return "txt";
            }
            return extractExtension(name);
        }
        return switch (type) {
            case "Texture2D" -> "png";
            case "Mesh" -> "obj";
            case "MonoBehaviour", "GameObject", "AssetBundle" -> "json";
            default -> "bin";
        };
    }

    @Nullable
    private Long tryGetSize(PyObject obj) {
        String[] attrs = {"byte_size", "size", "data_size", "m_Size"};
        for (String a : attrs) {
            try {
                PyObject v = obj.get(a);
                if (v != null && !v.isEmpty()) {
                    long n = v.toLong();
                    if (n >= 0) {
                        return n;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Nullable
    private String tryGetContainer(PyObject obj) {
        try {
            PyObject c = obj.get("container");
            if (c != null && !c.isEmpty()) {
                return c.toString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @NonNull
    private String normalizeExportName(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replace(" ", "_").replace("-", "_");
    }

    private String sanitizeFilename(String name, String fallback) {
        String n = (name == null || name.trim().isEmpty()) ? fallback : name.trim();
        String bad = "<>:\"/\\|?*\n\r\t";
        for (int i = 0; i < bad.length(); i++) n = n.replace(bad.charAt(i), '_');
        n = n.replaceAll("[. ]+$", "").trim();
        return n.isEmpty() ? fallback : n;
    }

    @NonNull
    private String extractName(PyObject obj) {
        try {
            PyObject parsed = obj.callAttr("parse_as_object");
            if (parsed != null) {
                PyObject mNameObject = parsed.get("m_Name");
                if (mNameObject != null) {
                    return mNameObject.toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    @NonNull
    private String extractExtension(@NonNull String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            return name.substring(dot + 1);
        }
        return "";
    }

    @NonNull
    private String archiveTag(PyObject obj) {
        try {
            PyObject af = obj.get("assets_file");
            if (af == null || af.isEmpty()) {
                return "";
            }

            PyObject n = af.get("name");
            String name = (n != null && !n.isEmpty()) ? n.toString() : "";

            if (name.isEmpty()) {
                PyObject file = af.get("file");
                if (file != null && !file.isEmpty()) {
                    PyObject fn = file.get("name");
                    if (fn != null && !fn.isEmpty()) {
                        name = fn.toString();
                    }
                }
            }

            if (!name.isEmpty()) {
                String base = new File(name).getName();
                int dot = base.lastIndexOf('.');
                if (dot >= 0) {
                    base = base.substring(0, dot);
                }
                return normalizeExportName(base);
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private String exportBaseName(PyObject obj, int idx) {
        String objName = normalizeExportName(extractName(obj));
        String arch = archiveTag(obj);
        long pid = safeLong(getAttr(obj, "path_id"), idx);

        String base;
        if (!objName.isEmpty() && !arch.isEmpty()) {
            base = objName + "_" + arch;
        } else if (!objName.isEmpty()) {
            base = objName + "_" + pid;
        } else {
            base = normalizeExportName(tname(obj)) + "_" + pid;
        }

        return sanitizeFilename(base, String.valueOf(pid));
    }

    @NonNull
    private List<String> archiveNames(PyObject env) {
        Set<String> names = new LinkedHashSet<>();

        // env.files (dict)
        try {
            PyObject files = env.get("files");
            if (files != null && !files.isEmpty()) {
                Map<PyObject, PyObject> map = files.asMap();
                for (PyObject k : map.keySet()) {
                    String ks = k.toString();
                    if (keepArchiveKey(ks)) {
                        names.add(ks);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // env.file.files (dict)
        try {
            PyObject bf = env.get("file");
            if (bf != null && !bf.isEmpty()) {
                PyObject f2 = bf.get("files");
                if (f2 != null && !f2.isEmpty()) {
                    Map<PyObject, PyObject> map = f2.asMap();
                    for (PyObject k : map.keySet()) {
                        String ks = k.toString();
                        if (keepArchiveKey(ks)) {
                            names.add(ks);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return new ArrayList<>(names);
    }

    private boolean keepArchiveKey(String k) {
        if (k == null || k.isEmpty()) {
            return false;
        }

        // drop filesystem absolute paths
        if (k.startsWith("/") || k.startsWith("\\") || k.contains(":/") || k.contains(":\\")) {
            return false;
        }

        if (k.startsWith("CAB-")) {
            return true;
        }
        if (k.endsWith(".resS") || k.endsWith(".resource") || k.endsWith(".resources")) {
            return true;
        }
        return k.startsWith("archive:/");
    }

    private byte[] exportTextAsset(@NonNull PyObject obj) {
        PyObject parsed = obj.callAttr("parse_as_object");
        PyObject script = getAttr(parsed, "m_Script");

        byte[] data;
        if (script != null && !script.isEmpty()) {
            data = script.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            PyObject b = getAttr(parsed, "m_Bytes");
            data = (b != null && !b.isEmpty()) ? pyBytes(b) : new byte[0];
        }
        return data;
    }

    private byte[] exportMesh(@NonNull PyObject obj) {
        PyObject mesh = obj.callAttr("parse_as_object");
        PyObject txt = mesh.callAttr("export");
        String txtStr = (txt == null || txt.isEmpty()) ? "" : txt.toString();

        return txtStr.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportTexture2D(@NonNull PyObject obj) {
        PyObject tex = obj.callAttr("parse_as_object");
        PyObject bio = io.callAttr("BytesIO");
        PyObject image = tex.get("image");
        if (image != null) {
            image.callAttr("save", bio, new Kwarg("format", "PNG"));
        }
        PyObject data = bio.callAttr("getvalue");
        return (data != null && !data.isEmpty()) ? pyBytes(data) : new byte[0];
    }

    private byte[] exportMonoBehaviour(PyObject obj) {
        try {
            PyObject d = obj.callAttr("parse_as_dict");

            String jsonText = json.callAttr(
                    "dumps",
                    d,
                    new Kwarg("ensure_ascii", false),
                    new Kwarg("indent", 2)
            ).toString();
            return jsonText.getBytes(StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }

        PyObject parsed = obj.callAttr("parse_as_object");
        PyObject raw = parsed.getOrDefault("get_raw_data", null);
        return (raw != null && !raw.isEmpty()) ? pyBytes(raw) : new byte[0];
    }

    private byte[] exportTypeTreeJson(@NonNull PyObject obj) {
        PyObject d = obj.callAttr("parse_as_dict");

        String jsonText = json.callAttr(
                "dumps",
                d,
                new Kwarg("ensure_ascii", false),
                new Kwarg("indent", 2)
        ).toString();
        return jsonText.getBytes(StandardCharsets.UTF_8);
    }

    @NonNull
    private PyObject saveTypeTree(@NonNull PyObject obj, @NonNull PyObject pyTree) {
        PyObject saveTypeTree = obj.get("save_typetree");
        if (saveTypeTree != null && !saveTypeTree.isEmpty()) {
            try {
                obj.callAttr("save_typetree", pyTree);
                return obj;
            } catch (Throwable e) {
                Log.d(TAG, "save_typetree failed: " + e);
            }
        }
        return obj;
    }

    private byte[] pyBytes(PyObject b) {
        if (b == null || b.isEmpty()) {
            return new byte[0];
        }

        try {
            return b.toJava(byte[].class);
        } catch (Throwable ignored) {
        }

        try {
            List<PyObject> list = b.asList();
            byte[] out = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) out[i] = (byte) (list.get(i).toInt() & 0xFF);
            return out;
        } catch (Throwable ignored) {
        }

        return b.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    @NonNull
    private String decodeBytesToText(byte[] b) {
        if (b == null || b.length == 0) {
            return "";
        }
        try {
            return new String(b, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return new String(b, StandardCharsets.ISO_8859_1);
        }
    }

    private void writeFile(File out, byte[] data) throws Exception {
        ensureParent(out);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(data);
        }
    }

    private void ensureParent(@NonNull File out) {
        File p = out.getParentFile();
        if (p != null && !p.exists()) {
            //noinspection ResultOfMethodCallIgnored
            p.mkdirs();
        }
    }

    @Nullable
    private PyObject getAttr(PyObject obj, String name) {
        try {
            return obj.get(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private long safeLong(PyObject v, long def) {
        try {
            if (v == null || v.isEmpty()) {
                return def;
            }
            return v.toLong();
        } catch (Throwable ignored) {
            return def;
        }
    }

    @NonNull
    private String uuid12() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @NonNull
    private static String msgOf(@NonNull Throwable t) {
        String m = t.getMessage();
        return (m == null || m.trim().isEmpty()) ? t.toString() : m;
    }

    @NonNull
    private <T> ApiResult<T> ok(T data) {
        return ApiResult.ok(data);
    }

    @NonNull
    private <T> ApiResult<T> fail(String error, @Nullable String trace) {
        return ApiResult.fail(error, trace);
    }

    @NonNull
    private String androidTrace(@NonNull Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
