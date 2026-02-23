package com.elfilibustero.uabe.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class DocumentUtil {

    public static String getDisplayName(Context ctx, Uri uri) {
        String name = "bundle";
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return name;
    }

    public static boolean copyFileToUri(@NonNull Context ctx, @NonNull File src, @NonNull Uri dest) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = ctx.getContentResolver().openOutputStream(dest, getMode())) {

            if (out == null) throw new RuntimeException("Failed to open output stream");

            byte[] buf = new byte[1024 * 256];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    @NonNull
    public static File copyToRecentsStorage(@NonNull Context ctx, @NonNull Uri uri, @NonNull String displayName) {
        File dir = new File(ctx.getFilesDir(), "recent_bundles");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();

        String safe = displayName.replaceAll("[\\\\/:*?\"<>|\\n\\r\\t]", "_").trim();
        if (safe.isEmpty()) safe = "bundle";

        String outName = safe + "_" + UUID.randomUUID().toString().substring(0, 8);

        File out = new File(dir, outName);

        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {

            if (in == null) throw new RuntimeException("Failed to open input stream");

            byte[] buf = new byte[1024 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
            fos.flush();
            return out;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void writeBytesToUri(@NonNull Context ctx, @NonNull Uri dest, @NonNull byte[] bytes) throws Exception {
        try (OutputStream out = ctx.getContentResolver().openOutputStream(dest, getMode())) {
            if (out == null) throw new RuntimeException("Failed to open output stream");
            out.write(bytes);
            out.flush();
        }
    }
    @NonNull
    public static byte[] readBytesFromUri(@NonNull Context ctx, @NonNull Uri src, int maxBytes) throws Exception {
        try (InputStream in = ctx.getContentResolver().openInputStream(src)) {
            if (in == null) throw new RuntimeException("Failed to open input stream");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 64];
            int r;
            int total = 0;

            while ((r = in.read(buf)) != -1) {
                total += r;
                if (maxBytes > 0 && total > maxBytes) {
                    throw new RuntimeException("File too large.");
                }
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        }
    }
    @NonNull
    private static String getMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return "wt";
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and 11 (API level 29-30), use "rwt" as a workaround
            return "rwt";
        } else {
            // For Android 9 and below, use "w"
            return "w";
        }
    }
}
