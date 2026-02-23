package com.elfilibustero.uabe.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.elfilibustero.uabe.R;
import com.elfilibustero.uabe.databinding.EditTextItemBinding;
import com.elfilibustero.uabe.databinding.FragmentBundleViewerBinding;
import com.elfilibustero.uabe.enums.PendingKind;
import com.elfilibustero.uabe.enums.SortMode;
import com.elfilibustero.uabe.enums.SupportedTypes;
import com.elfilibustero.uabe.model.ObjectItem;
import com.elfilibustero.uabe.model.RecentBundle;
import com.elfilibustero.uabe.python.repo.OpenBundleResult;
import com.elfilibustero.uabe.util.BundleRecentsStore;
import com.elfilibustero.uabe.util.DocumentUtil;
import com.elfilibustero.uabe.util.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class BundleViewerFragment extends Fragment {

    private FragmentBundleViewerBinding b;
    private ObjectAdapter adapter;

    private BundleViewerViewModel vm;

    private BundleRecentsStore recentsStore;

    private ObjectItem pendingActionItem;

    private PendingKind pendingExportKind = PendingKind.NONE;

    private PendingKind pendingImportKind = PendingKind.NONE;

    private File currentLocalCopy;

    private ActivityResultLauncher<String> createDocLauncher;
    private ActivityResultLauncher<String[]> openDocLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(BundleViewerViewModel.class);

        createDocLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("*/*"),
                uri -> {
                    if (uri == null || pendingActionItem == null) {
                        return;
                    }

                    if (pendingExportKind == PendingKind.OBJECT) {
                        vm.exportObjectToUri(
                                uri,
                                pendingActionItem.getIndex(),
                                getString(R.string.message_exporting_object),
                                () -> {
                                    toast(R.string.message_exported_object);
                                    setStatus(getString(R.string.message_exported_object));
                                },
                                err -> setStatus(getString(R.string.message_export_failed, err))
                        );
                    } else if (pendingExportKind == PendingKind.BUNDLE) {
                        vm.exportBundleFileToUri(
                                uri,
                                getString(R.string.message_exporting),
                                () -> setStatus(getString(R.string.message_exported)),
                                err -> setStatus(getString(R.string.message_export_failed, err))
                        );
                    }

                    pendingExportKind = PendingKind.NONE;
                }
        );

        openDocLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    if (pendingImportKind == PendingKind.OBJECT) {
                        if (pendingActionItem == null) {
                            return;
                        }
                        vm.importObjectFromUri(
                                uri,
                                pendingActionItem.getIndex(),
                                getString(R.string.message_importing),
                                () -> toast(R.string.message_imported),
                                err -> setStatus(getString(R.string.message_import_failed, err))
                        );
                    } else if (pendingImportKind == PendingKind.BUNDLE) {
                        importAssetBundleFromUri(uri);
                    }

                    pendingImportKind = PendingKind.NONE;
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentBundleViewerBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Utils.addSystemWindowInsetToMargin(b.headerContainer, true, false, true, false);
        Utils.addSystemWindowInsetToMargin(b.chips, true, false, true, false);
        Utils.addSystemWindowInsetToPadding(b.recycler, true, false, true, true);

        recentsStore = new BundleRecentsStore(requireContext());

        adapter = new ObjectAdapter(new ObjectAdapter.Listener() {
            @Override
            public void onClick(ObjectItem item) {
                openEditor(item);
            }

            @Override
            public void onLongClick(ObjectItem item) {
                showItemActionsDialog(item);
            }
        });
        b.recycler.setAdapter(adapter);

        vm.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (b == null || state == null) {
                return;
            }
            b.progress.setVisibility(state.loading() ? View.VISIBLE : View.GONE);
            b.progress.setIndeterminate(state.loading());
            if (state.status() != null) {
                b.status.setText(state.status());
            }
        });

        vm.getItems().observe(getViewLifecycleOwner(), list -> {
            if (list == null) {
                adapter.submitList(null);
            } else {
                adapter.submitList(new ArrayList<>(list));
            }
        });

        vm.getObjectActions().observe(getViewLifecycleOwner(), st -> {
            if (b == null || st == null) {
                return;
            }

            if (st.loading()) {
                b.progress.setVisibility(View.VISIBLE);
                b.progress.setIndeterminate(true);
                return;
            }

            if (st.item() != null) {
                b.progress.setVisibility(View.GONE);
            }

            if (st.error() != null) {
                Toast.makeText(requireContext(),
                        R.string.message_failed_to_load_data,
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (st.item() == null || st.filename() == null || st.ext() == null || st.mime() == null) {
                return;
            }

            showActionsDialogFromState(st);
            vm.clearObjectActions(); // prevent re-show after rotation
        });

        if (b != null) {
            b.status.setText(R.string.message_open_a_bundle_to_begin);
        }

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == R.id.action_open) {
                    showOpenDialogWithRecents();
                    return true;

                } else if (id == R.id.action_export) {
                    exportBundle();
                    return true;

                } else if (id == R.id.action_reload) {
                    vm.reload();
                    return true;

                } else if (id == R.id.action_close) {
                    closeBundle();
                    return true;

                } else if (id == R.id.action_decyption_key) {
                    showDecryptionKeyDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        b.chipFilter.setOnClickListener(v -> {
            OpenBundleResult result = vm.getOpenBundleResult().getValue();
            if (result == null) {
                return;
            }
            PopupMenu pm = new PopupMenu(requireContext(), v);
            Menu menu = pm.getMenu();

            menu.add(Menu.NONE, 1, 0, "All types");

            int baseId = 1000;
            for (int i = 0; i < result.types.size(); i++) {
                String t = result.types.get(i);
                if (t == null || t.trim().isEmpty()) {
                    continue;
                }
                menu.add(Menu.NONE, baseId + i, i + 1, t);
            }

            pm.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    vm.setFilterTypes(Collections.emptySet());
                    return true;
                }

                String selectedType = Objects.requireNonNull(item.getTitle()).toString();
                vm.setFilterTypes(new HashSet<>(Collections.singletonList(selectedType)));
                return true;
            });

            pm.show();
        });

        b.chipSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_sort, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_sort_idx) {
                    vm.setSortMode(SortMode.IDX);
                } else if (id == R.id.action_sort_name) {
                    vm.setSortMode(SortMode.NAME);
                } else if (id == R.id.action_sort_type) {
                    vm.setSortMode(SortMode.TYPE);
                } else if (id == R.id.action_sort_size) {
                    vm.setSortMode(SortMode.SIZE);
                } else if (id == R.id.action_sort_edited) {
                    vm.setSortMode(SortMode.EDITED);
                }
                b.chipSort.setText(item.getTitle());
                return false;
            });

            popup.show();
        });

        if (savedInstanceState == null) {
            autoReopenLastIfAny();
        }
        vm.setBundleDecryptionKey(
                "mgkiiq40ogdepy"
        );
    }

    private void showItemActionsDialog(@NonNull ObjectItem item) {
        String sid = vm.getSessionId().getValue();
        if (sid == null || sid.isEmpty()) {
            return;
        }

        pendingActionItem = item;

        SupportedTypes supportedTypes = SupportedTypes.fromName(item.getType());
        if (supportedTypes == null) {
            toast(getString(R.string.message_type_not_supported, item.getType()));
            return;
        }

        vm.requestObjectActions(item);
    }

    private void showActionsDialogFromState(@NonNull BundleViewerViewModel.ObjectActionsState st) {
        ObjectItem item = st.item();
        if (item == null) {
            return;
        }

        pendingActionItem = item;

        SupportedTypes supportedTypes = SupportedTypes.fromName(item.getType());
        if (supportedTypes == null) {
            toast(getString(R.string.message_type_not_supported, item.getType()));
            return;
        }

        String file = st.filename() + "." + st.ext();

        ArrayList<String> actionItems = new ArrayList<>();
        actionItems.add("Edit/Preview");
        if (supportedTypes.canExport()) {
            actionItems.add(getString(R.string.action_export_add, st.ext()));
        }
        if (supportedTypes.canImport()) {
            actionItems.add(getString(R.string.action_import_add, st.ext()));
        }
        String[] actions = actionItems.toArray(new String[0]);

        final String title = safe(item.getName()).isEmpty()
                ? getString(R.string.title_unnamed_asset)
                : safe(item.getName());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        openEditor(item);
                    } else if (which == 1) {
                        pendingExportKind = PendingKind.OBJECT;
                        createDocLauncher.launch(file);
                    } else if (which == 2) {
                        pendingImportKind = PendingKind.OBJECT;
                        openDocLauncher.launch(new String[]{st.mime()});
                    }
                })
                .show();
    }

    private void openEditor(@NonNull ObjectItem item) {
        String sid = vm.getSessionId().getValue();
        if (sid == null || sid.isEmpty()) {
            Log.d("Editor", "Session ID is empty");
            return;
        }

        SupportedTypes supportedTypes = SupportedTypes.fromName(item.getType());
        if (supportedTypes == null) {
            toast(getString(R.string.message_type_not_supported, item.getType()));
            return;
        }

        EditObjectBottomSheet sheet = EditObjectBottomSheet.newInstance(
                sid,
                item.getIndex(),
                item.getType(),
                item.getId()
        );

        sheet.setCallback(vm::markItemModified);

        sheet.show(getChildFragmentManager(), "edit_object");
    }

    private void exportBundle() {
        if (currentLocalCopy == null) {
            return;
        }

        String suggested = "modified_bundle.unity3d";
        String displayName = vm.getDisplayName().getValue();
        if (displayName != null) {
            String s = displayName.trim();
            if (!s.isEmpty()) {
                suggested = s;
            }
        }

        pendingExportKind = PendingKind.BUNDLE;
        createDocLauncher.launch(suggested);
    }

    private void closeBundle() {
        if (currentLocalCopy == null) {
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_close_viewer)
                .setMessage(R.string.message_close_current_bundle_view)
                .setNegativeButton(R.string.text_cancel, null)
                .setPositiveButton(R.string.text_close, (d, w) -> closeCurrentInternal())
                .show();
    }

    private void autoReopenLastIfAny() {
        if (currentLocalCopy != null) {
            return;
        }

        String lastPath = recentsStore.getLastPath();
        String lastName = recentsStore.getLastName();

        if (lastPath == null || lastPath.trim().isEmpty()) {
            return;
        }

        File f = new File(lastPath);
        if (!f.exists()) {
            recentsStore.clearLast();
            return;
        }

        if (lastName == null || lastName.trim().isEmpty()) {
            lastName = f.getName();
        }

        openLocalFile(f, lastName);
    }

    private void showOpenDialogWithRecents() {
        List<RecentBundle> recents = recentsStore.getRecents();

        ArrayList<String> items = new ArrayList<>();
        for (RecentBundle r : recents) {
            if (r == null || r.path == null || !new File(r.path).exists()) {
                continue;
            }
            String n = (r.displayName == null || r.displayName.trim().isEmpty())
                    ? new File(r.path).getName()
                    : r.displayName.trim();
            items.add(n);
        }
        items.add(getString(R.string.message_browse));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_open_bundle)
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    if (which == items.size() - 1) {
                        pickAssetBundleFile();
                        return;
                    }

                    RecentBundle r = recents.get(which);
                    if (r == null || r.path == null) {
                        return;
                    }

                    File f = new File(r.path);
                    if (!f.exists()) {
                        toast(R.string.message_recent_file_missing);
                        recentsStore.removeRecent(r.path);
                        return;
                    }

                    openLocalFile(f,
                            (r.displayName != null && !r.displayName.trim().isEmpty())
                                    ? r.displayName
                                    : f.getName());
                })
                .setNeutralButton(R.string.text_manage, (d, w) -> showManageRecentsDialog())
                .show();
    }

    private void showManageRecentsDialog() {
        List<RecentBundle> recents = recentsStore.getRecents();
        if (recents.isEmpty()) {
            toast(R.string.message_no_recents);
            return;
        }

        ArrayList<String> items = new ArrayList<>();
        boolean hasCurrent = false;

        for (RecentBundle r : recents) {
            if (r == null || r.path == null || !new File(r.path).exists()) {
                continue;
            }
            String n = (r.displayName == null || r.displayName.trim().isEmpty())
                    ? new File(r.path).getName()
                    : r.displayName.trim();
            items.add(getString(R.string.message_remove, n));
            if (currentLocalCopy != null && r.path.equals(currentLocalCopy.getAbsolutePath())) {
                hasCurrent = true;
            }
        }

        boolean finalHasCurrent = hasCurrent;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_manage_recents)
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    RecentBundle r = recents.get(which);
                    if (r == null || r.path == null) {
                        return;
                    }

                    try { //noinspection ResultOfMethodCallIgnored
                        new File(r.path).delete();
                    } catch (Exception ignored) {
                    }

                    recentsStore.removeRecent(r.path);
                    if (currentLocalCopy != null && r.path.equals(
                            currentLocalCopy.getAbsolutePath())) {
                        closeCurrentInternal();
                    }

                    toast(R.string.message_removed);
                })
                .setNeutralButton("Remove all", (d, w) -> {
                    recentsStore.clear();
                    if (finalHasCurrent) {
                        closeCurrentInternal();
                    }
                })
                .show();
    }

    private void pickAssetBundleFile() {
        pendingImportKind = PendingKind.BUNDLE;
        openDocLauncher.launch(new String[]{
                "application/octet-stream",
                "application/x-tar",
                "*/*"
        });
    }

    private void importAssetBundleFromUri(Uri uri) {
        if (currentLocalCopy != null) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.title_replace_current_file)
                    .setMessage(R.string.message_close_the_current_bundle_and_open_the_new_one)
                    .setNegativeButton(R.string.text_cancel, null)
                    .setPositiveButton(R.string.text_replace, (d, w) -> {
                        closeCurrentInternal();
                        openFromUriToRecents(uri);
                    })
                    .show();
        } else {
            openFromUriToRecents(uri);
        }
    }

    private void openFromUriToRecents(Uri uri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }

        String name = DocumentUtil.getDisplayName(requireContext(), uri);
        if (name == null || name.trim().isEmpty()) {
            name = "bundle";
        }

        File localCopy;
        try {
            localCopy = DocumentUtil.copyToRecentsStorage(requireContext(), uri, name);
        } catch (Exception e) {
            toast(getString(R.string.message_copy_failed, e.getMessage()));
            return;
        }

        currentLocalCopy = localCopy;

        vm.setDisplayName(name);

        recentsStore.setLastOpen(localCopy.getAbsolutePath(), name);
        recentsStore.upsertRecent(localCopy.getAbsolutePath(), name);

        vm.openBundle(localCopy.getAbsolutePath(), getString(R.string.message_scanning));
    }

    private void openLocalFile(@NonNull File file, String displayName) {
        currentLocalCopy = file;

        vm.setDisplayName(displayName);

        Log.d("openLocalFile", "displayName: " + displayName + " file: " + file.getAbsolutePath());

        recentsStore.setLastOpen(file.getAbsolutePath(), displayName);
        recentsStore.upsertRecent(file.getAbsolutePath(), displayName);

        vm.openBundle(file.getAbsolutePath(), getString(R.string.message_scanning));
    }

    private void closeCurrentInternal() {
        currentLocalCopy = null;

        vm.setDisplayName(null);

        recentsStore.clearLast();
        if (b != null) {
            b.status.setText(R.string.message_open_a_bundle_to_begin);
        }

        vm.closeBundleState();
    }

    private void showDecryptionKeyDialog() {
        EditTextItemBinding binding = EditTextItemBinding.inflate(getLayoutInflater());
        EditText editText = binding.textInputLayout.getEditText();
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Decryption Key")
                .setView(binding.getRoot())
                .setPositiveButton("Set", (d, w) -> {
                    String key = editText.getText().toString();
                    vm.setBundleDecryptionKey(key);
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void setStatus(@NonNull String s) {
        if (b != null) {
            b.status.setText(s);
        }
    }

    private void toast(@NonNull String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_LONG).show();
    }

    private void toast(int resId) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private static String safe(@Nullable String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
