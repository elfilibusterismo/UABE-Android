package com.elfilibustero.uabe.util;

import static com.elfilibustero.uabe.util.Utils.GSON;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elfilibustero.uabe.model.RecentBundle;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BundleRecentsStore {

    private static final String PREF = "bundle_recents_v1";

    private static final String KEY_RECENTS = "recents_json";
    private static final String KEY_LAST_PATH = "last_path";
    private static final String KEY_LAST_NAME = "last_name";

    private static final int MAX_RECENTS = 15;

    private final SharedPreferences sp;

    public BundleRecentsStore(@NonNull Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void setLastOpen(@Nullable String path, @Nullable String displayName) {
        sp.edit()
                .putString(KEY_LAST_PATH, path)
                .putString(KEY_LAST_NAME, displayName)
                .apply();
    }

    @Nullable
    public String getLastPath() {
        return sp.getString(KEY_LAST_PATH, null);
    }

    @Nullable
    public String getLastName() {
        return sp.getString(KEY_LAST_NAME, null);
    }

    public void clearLast() {
        setLastOpen(null, null);
    }

    public void upsertRecent(@NonNull String path, @NonNull String displayName) {
        ArrayList<RecentBundle> list = loadInternal();

        // purge missing first
        purgeMissing(list);

        // remove existing same path
        for (int i = 0; i < list.size(); i++) {
            RecentBundle r = list.get(i);
            if (r != null && path.equals(r.path)) {
                list.remove(i);
                break;
            }
        }

        list.add(0, new RecentBundle(path, displayName, System.currentTimeMillis()));

        if (list.size() > MAX_RECENTS) {
            list.subList(MAX_RECENTS, list.size()).clear();
        }

        saveInternal(list);
    }

    @NonNull
    public List<RecentBundle> getRecents() {
        ArrayList<RecentBundle> list = loadInternal();
        boolean changed = purgeMissing(list);
        if (changed) {
            saveInternal(list);
        }
        return list;
    }

    public void removeRecent(@NonNull String path) {
        ArrayList<RecentBundle> list = loadInternal();
        boolean changed = false;

        Iterator<RecentBundle> it = list.iterator();
        while (it.hasNext()) {
            RecentBundle r = it.next();
            if (r != null && path.equals(r.path)) {
                it.remove();
                changed = true;
            }
        }

        if (changed) {
            saveInternal(list);
        }

        // if it was last open, clear last
        if (path.equals(getLastPath())) {
            clearLast();
        }
    }

    public void clear() {
        for (RecentBundle bundle : getRecents()) {
            if (bundle == null || bundle.path == null) {
                return;
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                new File(bundle.path).delete();
            } catch (Exception ignored) {
            }
            removeRecent(bundle.path);
        }
    }

    private boolean purgeMissing(@NonNull ArrayList<RecentBundle> list) {
        boolean changed = false;
        Iterator<RecentBundle> it = list.iterator();
        while (it.hasNext()) {
            RecentBundle r = it.next();
            if (r == null || r.path == null || !new File(r.path).exists()) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @NonNull
    private ArrayList<RecentBundle> loadInternal() {
        String json = sp.getString(KEY_RECENTS, "[]");
        Type t = new TypeToken<ArrayList<RecentBundle>>() {
        }.getType();

        try {
            ArrayList<RecentBundle> list = GSON.fromJson(json, t);
            return list != null ? list : new ArrayList<>();
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private void saveInternal(@NonNull List<RecentBundle> list) {
        sp.edit().putString(KEY_RECENTS, GSON.toJson(list)).apply();
    }
}
