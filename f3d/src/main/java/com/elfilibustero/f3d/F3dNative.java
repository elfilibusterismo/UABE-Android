package com.elfilibustero.f3d;

import android.content.Context;

import java.io.File;

public final class F3dNative {
    private static volatile boolean checked;
    private static volatile boolean available;

    private F3dNative() {
    }

    public static boolean isAvailable(Context context) {
        ensureChecked(context);
        return available;
    }

    private static void ensureChecked(Context context) {
        if (checked) {
            return;
        }
        synchronized (F3dNative.class) {
            if (checked) {
                return;
            }
            checked = true;

            File so = new File(context.getApplicationInfo().nativeLibraryDir, "libf3d-java.so");
            if (!so.exists()) {
                available = false;
                return;
            }

            try {
                System.loadLibrary("f3d-java");
                available = true;
            } catch (Throwable t) {
                available = false;
            }
        }
    }
}