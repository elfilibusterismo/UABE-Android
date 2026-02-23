package com.elfilibustero.uabe.ui;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.elfilibustero.uabe.enums.SortMode;
import com.elfilibustero.uabe.model.ObjectItem;
import com.elfilibustero.uabe.python.repo.OpenBundleResult;
import com.elfilibustero.uabe.python.repo.UnityPyRepositoryImpl;
import com.elfilibustero.uabe.util.DocumentUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class BundleViewerViewModel extends AndroidViewModel {

    // ---- UI state ----
    public record UiState(boolean loading, @Nullable String status) { }

    // ---- Object actions state (for dialog) ----
    public record ObjectActionsState(boolean loading, @Nullable String error,
                                     @Nullable ObjectItem item, @Nullable String filename,
                                     @Nullable String ext, @Nullable String mime) {

        @NonNull
        public static ObjectActionsState idle() {
            return new ObjectActionsState(false, null, null, null, null, null);
        }

        @NonNull
        public static ObjectActionsState loading(@NonNull ObjectItem item) {
            return new ObjectActionsState(true, null, item, null, null, null);
        }

        @NonNull
        public static ObjectActionsState error(@NonNull ObjectItem item, @NonNull String msg) {
            return new ObjectActionsState(false, msg, item, null, null, null);
        }

        @NonNull
        public static ObjectActionsState ready(@NonNull ObjectItem item,
                                               @NonNull String filename,
                                               @NonNull String ext,
                                               @NonNull String mime) {
            return new ObjectActionsState(false, null, item, filename, ext, mime);
        }
    }

    // ---- Filter state ----
    public record FilterState(@Nullable String query,
                              @NonNull Set<String> types,
                              boolean editedOnly,
                              @Nullable Long minBytes,
                              @Nullable Long maxBytes) {

        @NonNull
        public static FilterState none() {
            return new FilterState(null, Set.of(), false, null, null);
        }

        @NonNull
        public FilterState withQuery(@Nullable String q) {
            String nq = (q == null) ? null : q.trim();
            if (nq != null && nq.isEmpty()) nq = null;
            return new FilterState(nq, types, editedOnly, minBytes, maxBytes);
        }

        @NonNull
        public FilterState withTypes(@NonNull Set<String> t) {
            // normalize & copy (case-insensitive behavior comes from normalization below)
            HashSet<String> copy = new HashSet<>();
            for (String s : t) {
                if (s == null) continue;
                String ns = s.trim();
                if (!ns.isEmpty()) copy.add(ns);
            }
            return new FilterState(query, copy, editedOnly, minBytes, maxBytes);
        }

        @NonNull
        public FilterState toggleType(@NonNull String type) {
            HashSet<String> copy = new HashSet<>(types);
            String t = type.trim();
            if (t.isEmpty()) return this;
            if (copy.contains(t)) copy.remove(t); else copy.add(t);
            return new FilterState(query, copy, editedOnly, minBytes, maxBytes);
        }

        @NonNull
        public FilterState withEditedOnly(boolean v) {
            return new FilterState(query, types, v, minBytes, maxBytes);
        }

        @NonNull
        public FilterState withSizeRange(@Nullable Long min, @Nullable Long max) {
            Long nmin = min;
            Long nmax = max;

            if (nmin != null && nmin < 0) nmin = 0L;
            if (nmax != null && nmax < 0) nmax = 0L;

            // If swapped, auto-fix
            if (nmin != null && nmax != null && nmin > nmax) {
                long tmp = nmin;
                nmin = nmax;
                nmax = tmp;
            }
            return new FilterState(query, types, editedOnly, nmin, nmax);
        }
    }

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(false, null));
    private final MutableLiveData<List<ObjectItem>> items = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> sessionId = new MutableLiveData<>(null);
    private final MutableLiveData<String> currentPath = new MutableLiveData<>(null);
    private final MutableLiveData<String> displayName = new MutableLiveData<>(null);

    private final MutableLiveData<ObjectActionsState> objectActions =
            new MutableLiveData<>(ObjectActionsState.idle());
    private final MutableLiveData<OpenBundleResult> openBundleResult =
            new MutableLiveData<>(null);

    private final MutableLiveData<FilterState> filterState =
            new MutableLiveData<>(FilterState.none());

    private final UnityPyRepositoryImpl repo;

    private SortMode sortMode = SortMode.IDX;
    private final ArrayList<ObjectItem> rawItems = new ArrayList<>();
    private final HashSet<Integer> modifiedIdx = new HashSet<>();

    private boolean autoSaving = false;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = this::autoSaveToCacheSilent;
    private final Runnable autoReloadRunnable = this::reload;

    public BundleViewerViewModel(@NonNull Application app) {
        super(app);
        repo = new UnityPyRepositoryImpl(app.getApplicationContext());
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<List<ObjectItem>> getItems() {
        return items;
    }

    public LiveData<String> getSessionId() {
        return sessionId;
    }

    public LiveData<ObjectActionsState> getObjectActions() {
        return objectActions;
    }

    public void clearObjectActions() {
        uiState.setValue(new UiState(false, null));
        objectActions.setValue(ObjectActionsState.idle());
    }

    public void setSortMode(@NonNull SortMode mode) {
        sortMode = mode;
        publish();
    }

    public void setBundleDecryptionKey(String key) {
        repo.setBundleDecryptionKey(key);
    }

    public void setDisplayName(String name) {
        displayName.setValue(name);
    }

    public LiveData<String> getDisplayName() {
        return displayName;
    }

    public LiveData<OpenBundleResult> getOpenBundleResult() {
        return openBundleResult;
    }

    // ---------------- FILTER API ----------------

    public LiveData<FilterState> getFilterState() {
        return filterState;
    }

    public void clearFilters() {
        filterState.setValue(FilterState.none());
        publish();
    }

    public void setFilterQuery(@Nullable String query) {
        FilterState fs = filterState.getValue();
        if (fs == null) fs = FilterState.none();
        filterState.setValue(fs.withQuery(query));
        publish();
    }

    public void setFilterTypes(@NonNull Set<String> types) {
        FilterState fs = filterState.getValue();
        if (fs == null) fs = FilterState.none();
        filterState.setValue(fs.withTypes(types));
        publish();
    }

    public void toggleTypeFilter(@NonNull String type) {
        FilterState fs = filterState.getValue();
        if (fs == null) fs = FilterState.none();
        filterState.setValue(fs.toggleType(type));
        publish();
    }

    public void setEditedOnly(boolean editedOnly) {
        FilterState fs = filterState.getValue();
        if (fs == null) fs = FilterState.none();
        filterState.setValue(fs.withEditedOnly(editedOnly));
        publish();
    }

    public void setSizeRange(@Nullable Long minBytes, @Nullable Long maxBytes) {
        FilterState fs = filterState.getValue();
        if (fs == null) fs = FilterState.none();
        filterState.setValue(fs.withSizeRange(minBytes, maxBytes));
        publish();
    }

    // ---------------- Bundle lifecycle ----------------

    public boolean hasOpenBundle() {
        String p = currentPath.getValue();
        return p != null && !p.trim().isEmpty();
    }

    public void openBundle(@NonNull String localPath, @Nullable String scanningStatusText) {
        currentPath.setValue(localPath);

        uiState.setValue(new UiState(true, scanningStatusText));
        rawItems.clear();
        items.setValue(new ArrayList<>());

        modifiedIdx.clear();
        autoSaving = false;

        main.removeCallbacks(autoSaveRunnable);
        main.removeCallbacks(autoReloadRunnable);

        String oldSession = sessionId.getValue();
        if (oldSession != null && !oldSession.isEmpty()) {
            try { repo.closeBundle(oldSession); } catch (Exception ignored) {}
            sessionId.setValue(null);
        }

        repo.openBundle(localPath)
                .addOnSuccessListener(result -> {
                    openBundleResult.setValue(result);
                    sessionId.setValue(result.sessionId);

                    ArrayList<ObjectItem> list = new ArrayList<>();
                    if (result.objects != null) {
                        for (ObjectItem it : result.objects) {
                            if (it == null) continue;
                            ObjectItem c = copyItem(it);
                            c.setModified(modifiedIdx.contains(c.getIndex()));
                            list.add(c);
                        }
                    }

                    rawItems.clear();
                    rawItems.addAll(list);

                    uiState.setValue(new UiState(false,
                            result.archives != null ? result.archives.toString() : null));
                    publish();
                })
                .addOnFailureListener(e -> uiState.setValue(new UiState(false, e.getMessage())));
    }

    public void reload() {
        String path = currentPath.getValue();
        String sid = sessionId.getValue();
        if (path == null || path.trim().isEmpty() || sid == null || sid.isEmpty()) {
            return;
        }

        uiState.setValue(new UiState(true, null));

        repo.closeBundle(sid)
                .continueWithTask(closeTask -> repo.openBundle(path))
                .addOnSuccessListener(result -> {
                    sessionId.setValue(result.sessionId);

                    ArrayList<ObjectItem> list = new ArrayList<>();
                    if (result.objects != null) {
                        for (ObjectItem it : result.objects) {
                            if (it == null) continue;
                            ObjectItem c = copyItem(it);
                            c.setModified(modifiedIdx.contains(c.getIndex()));
                            list.add(c);
                        }
                    }

                    rawItems.clear();
                    rawItems.addAll(list);

                    uiState.setValue(new UiState(false,
                            result.archives != null ? result.archives.toString() : null));

                    publish();
                })
                .addOnFailureListener(t ->
                        uiState.setValue(new UiState(false, t != null ? t.getMessage() : "Unknown error")));
    }

    /**
     * Load filename/ext/mime for the long-press actions dialog
     */
    public void requestObjectActions(@NonNull ObjectItem item) {
        String sid = sessionId.getValue();
        if (sid == null || sid.isEmpty()) {
            objectActions.setValue(ObjectActionsState.error(item, "Session is empty"));
            return;
        }

        objectActions.setValue(ObjectActionsState.loading(item));

        repo.getObjectInfo(sid, item.getIndex())
                .addOnSuccessListener(result -> {
                    try {
                        String fileName = String.valueOf(result.get("filename"));
                        String extension = String.valueOf(result.get("ext"));
                        String mime = String.valueOf(result.get("mime"));

                        if (fileName.trim().isEmpty()) fileName = "asset";
                        if (extension.trim().isEmpty()) extension = "bin";
                        if (mime.trim().isEmpty()) mime = "*/*";

                        objectActions.setValue(ObjectActionsState.ready(item, fileName, extension, mime));
                    } catch (Exception e) {
                        objectActions.setValue(ObjectActionsState.error(item,
                                e.getMessage() != null ? e.getMessage() : "Failed to parse object info"));
                    }
                })
                .addOnFailureListener(e -> objectActions.setValue(
                        ObjectActionsState.error(item,
                                e.getMessage() != null ? e.getMessage() : "Failed to load object info")));
    }

    public void markItemModified(int idx) {
        modifiedIdx.add(idx);
        updateRowModifiedOnly(idx);
        debounceAutosave();
        publish();
    }

    public void exportObjectToUri(@NonNull Uri dest, int index,
                                  @Nullable String loadingText,
                                  @NonNull Runnable onSuccess,
                                  @NonNull Consumer<String> onError) {
        String sid = sessionId.getValue();
        if (sid == null || sid.isEmpty()) {
            onError.accept("Session is empty");
            return;
        }
        uiState.setValue(new UiState(true, loadingText));

        repo.exportObject(sid, index, dest)
                .addOnSuccessListener(r -> {
                    uiState.setValue(new UiState(false, null));
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    uiState.setValue(new UiState(false, null));
                    onError.accept(e.getMessage());
                });
    }

    public void importObjectFromUri(@NonNull Uri src, int index,
                                    @Nullable String loadingText,
                                    @NonNull Runnable onSuccess,
                                    @NonNull Consumer<String> onError) {
        String sid = sessionId.getValue();
        if (sid == null || sid.isEmpty()) {
            onError.accept("Session is empty");
            return;
        }
        uiState.setValue(new UiState(true, loadingText));

        repo.importObject(sid, index, src)
                .addOnSuccessListener(r -> {
                    uiState.setValue(new UiState(false, null));
                    markItemModified(index);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    uiState.setValue(new UiState(false, null));
                    onError.accept(e.getMessage());
                });
    }

    public void exportBundleFileToUri(@NonNull Uri dest,
                                      @NonNull String exportingText,
                                      @NonNull Runnable onSuccess,
                                      @NonNull Consumer<String> onError) {
        String path = currentPath.getValue();
        if (path == null || path.trim().isEmpty()) {
            onError.accept("No bundle open");
            return;
        }

        uiState.setValue(new UiState(true, exportingText));

        new Thread(() -> {
            String err = null;
            try {
                File src = new File(path);
                if (!src.exists()) {
                    throw new RuntimeException("Input file does not exist");
                }

                if (DocumentUtil.copyFileToUri(getApplication().getApplicationContext(), src, dest)) {
                    //noinspection ResultOfMethodCallIgnored
                    src.delete();
                }
            } catch (Exception e) {
                err = e.getMessage();
            }

            String finalErr = err;
            main.post(() -> {
                uiState.setValue(new UiState(false, null));
                if (finalErr == null) onSuccess.run();
                else onError.accept(finalErr);
            });
        }).start();
    }

    public void closeBundleState() {
        String sid = sessionId.getValue();
        if (sid != null && !sid.isEmpty()) {
            try { repo.closeBundle(sid); } catch (Exception ignored) {}
        }

        sessionId.setValue(null);
        currentPath.setValue(null);

        rawItems.clear();
        items.setValue(new ArrayList<>());

        modifiedIdx.clear();
        autoSaving = false;

        main.removeCallbacks(autoSaveRunnable);
        main.removeCallbacks(autoReloadRunnable);

        uiState.setValue(new UiState(false, null));
        objectActions.setValue(ObjectActionsState.idle());
    }

    private void publish() {
        FilterState fs = filterState.getValue();
        if (fs == null) fs = FilterState.none();

        ArrayList<ObjectItem> out = new ArrayList<>(rawItems.size());
        for (ObjectItem it : rawItems) {
            if (it == null) continue;
            if (passesFilter(it, fs)) out.add(it);
        }

        out.sort(getComparator(sortMode));
        items.setValue(out);
    }

    private boolean passesFilter(@NonNull ObjectItem it, @NonNull FilterState fs) {
        // edited only
        if (fs.editedOnly && !it.isModified()) return false;

        // size range
        long size = (it.getBytes() != null) ? it.getBytes() : -1;
        if (fs.minBytes != null) {
            if (size < 0 || size < fs.minBytes) return false;
        }
        if (fs.maxBytes != null) {
            if (size < 0 || size > fs.maxBytes) return false;
        }

        // type filter
        if (!fs.types.isEmpty()) {
            String t = safe(it.getType());
            boolean ok = false;
            for (String allowed : fs.types) {
                if (allowed == null) continue;
                if (t.equalsIgnoreCase(allowed)) { ok = true; break; }
            }
            if (!ok) return false;
        }

        // query filter (name/type/id/index)
        String q = fs.query;
        if (q != null && !q.trim().isEmpty()) {
            String nq = q.trim().toLowerCase(Locale.ROOT);

            String name = safe(it.getName()).toLowerCase(Locale.ROOT);
            String type = safe(it.getType()).toLowerCase(Locale.ROOT);

            String idx = String.valueOf(it.getIndex());
            String id = String.valueOf(it.getId());

            return name.contains(nq) ||
                    type.contains(nq) ||
                    idx.contains(nq) ||
                    id.contains(nq);
        }

        return true;
    }

    private void debounceAutosave() {
        main.removeCallbacks(autoSaveRunnable);
        main.postDelayed(autoSaveRunnable, 700);
    }

    private void autoSaveToCacheSilent() {
        String sid = sessionId.getValue();
        String path = currentPath.getValue();
        if (sid == null || sid.isEmpty() || path == null || path.isEmpty()) {
            return;
        }

        if (autoSaving) {
            main.removeCallbacks(autoSaveRunnable);
            main.postDelayed(autoSaveRunnable, 700);
            return;
        }
        autoSaving = true;

        repo.saveBundle(sid, path)
                .addOnSuccessListener(ok -> {
                    autoSaving = false;
                    if (Boolean.TRUE.equals(ok)) {
                        main.removeCallbacks(autoReloadRunnable);
                        main.postDelayed(autoReloadRunnable, 450);
                    }
                })
                .addOnFailureListener(e -> autoSaving = false);
    }

    private void updateRowModifiedOnly(int editedIdx) {
        ArrayList<ObjectItem> newList = new ArrayList<>(rawItems.size());
        boolean changed = false;

        for (ObjectItem it : rawItems) {
            if (it != null && it.getIndex() == editedIdx) {
                ObjectItem copy = copyItem(it);
                copy.setModified(true);
                newList.add(copy);
                changed = true;
            } else {
                newList.add(it);
            }
        }

        if (changed) {
            rawItems.clear();
            rawItems.addAll(newList);
        }
    }

    @NonNull
    private static Comparator<ObjectItem> getComparator(@NonNull SortMode mode) {
        return switch (mode) {
            case NAME -> (a, c) -> {
                String an = safe(a.getName());
                String cn = safe(c.getName());
                int r = an.compareToIgnoreCase(cn);
                return r != 0 ? r : Integer.compare(a.getIndex(), c.getIndex());
            };
            case TYPE -> (a, c) -> {
                String at = safe(a.getType());
                String ct = safe(c.getType());
                int r = at.compareToIgnoreCase(ct);
                return r != 0 ? r : Integer.compare(a.getIndex(), c.getIndex());
            };
            case SIZE -> (a, c) -> {
                long as = (a.getBytes() != null) ? a.getBytes() : -1;
                long cs = (c.getBytes() != null) ? c.getBytes() : -1;
                int r = Long.compare(cs, as);
                return r != 0 ? r : Integer.compare(a.getIndex(), c.getIndex());
            };
            case EDITED -> (a, c) -> {
                int ae = a.isModified() ? 1 : 0;
                int ce = c.isModified() ? 1 : 0;
                int r = Integer.compare(ce, ae);
                return r != 0 ? r : Integer.compare(a.getIndex(), c.getIndex());
            };
            default -> Comparator.comparingInt(ObjectItem::getIndex);
        };
    }

    @NonNull
    private static String safe(@Nullable String s) {
        return s == null ? "" : s;
    }

    @NonNull
    private static ObjectItem copyItem(@NonNull ObjectItem it) {
        ObjectItem c = new ObjectItem();
        c.setIndex(it.getIndex());
        c.setName(it.getName());
        c.setType(it.getType());
        c.setId(it.getId());
        c.setBytes(it.getBytes());
        c.setModified(it.isModified());
        return c;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        main.removeCallbacks(autoSaveRunnable);
        main.removeCallbacks(autoReloadRunnable);
        repo.shutdown();
    }
}