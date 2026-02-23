package com.elfilibustero.uabe.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elfilibustero.f3d.MainView;
import com.elfilibustero.uabe.R;
import com.elfilibustero.uabe.databinding.BottomsheetEditObjectBinding;
import com.elfilibustero.uabe.enums.SupportedTypes;
import com.elfilibustero.uabe.python.repo.ObjectData;
import com.elfilibustero.uabe.python.repo.UnityPyRepositoryImpl;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditObjectBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SESSION = "session";
    private static final String ARG_IDX = "idx";
    private static final String ARG_TYPE = "type";
    private static final String ARG_ID = "id";

    @NonNull
    public static EditObjectBottomSheet newInstance(String sessionId, int idx, String type, long id) {
        Bundle b = new Bundle();
        b.putString(ARG_SESSION, sessionId);
        b.putInt(ARG_IDX, idx);
        b.putString(ARG_TYPE, type);
        b.putLong(ARG_ID, id);

        EditObjectBottomSheet f = new EditObjectBottomSheet();
        f.setArguments(b);
        return f;
    }

    public interface Callback {
        /**
         * @param idx object idx
         */
        void onEdited(int idx);
    }

    private BottomsheetEditObjectBinding b;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private UnityPyRepositoryImpl unityPyRepository;
    private Callback callback;

    private String sessionId;
    private int idx;
    private String type;


    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(
                dialogInterface -> {
                    BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                    FrameLayout bottomSheet =
                            bottomSheetDialog.findViewById(
                                    com.google.android.material.R.id.design_bottom_sheet);

                    if (bottomSheet != null) {
                        BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        behavior.setSkipCollapsed(true);
                        behavior.setDraggable(false);
                        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                        bottomSheet.setLayoutParams(layoutParams);
                    }
                });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = BottomsheetEditObjectBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        unityPyRepository = new UnityPyRepositoryImpl(requireContext());

        Bundle args = requireArguments();
        sessionId = args.getString(ARG_SESSION, "");
        idx = args.getInt(ARG_IDX, 0);
        type = args.getString(ARG_TYPE, "Unknown");
        long id = args.getLong(ARG_ID, 0);
        b.subtitle.setText(String.valueOf(id));

        b.btnCancel.setOnClickListener(v -> dismiss());
        b.btnSave.setOnClickListener(v -> save());

        loadDataOnly();
    }

    private void loadDataOnly() {
        setLoading(true, "Loading…");
        SupportedTypes supportedTypes = SupportedTypes.fromName(type);
        if (supportedTypes == null) {
            Toast.makeText(requireContext(), R.string.message_type_not_supported,
                    Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        unityPyRepository.getObjectData(sessionId, idx)
                .addOnSuccessListener(result -> {
                    switch (result.getSupportedType()) {
                        case MESH:
                            loadModel(result);
                            return;
                        case TEXTURE_2D:
                            loadTexture2d(result);
                            return;
                    }

                    setLoading(false, null);
                    String data = new String(result.getData(), StandardCharsets.UTF_8);
                    SupportedTypes supportedType = result.getSupportedType();
                    b.tilText.setVisibility(
                            supportedType.isEditable() ? View.VISIBLE : View.GONE);
                    b.txt2dContainer.setVisibility(View.GONE);
                    b.btnSave.setVisibility(
                            supportedType.isEditable() ? View.VISIBLE : View.GONE);
                    b.modelContainer.setVisibility(View.GONE);

                    if (!supportedType.isEditable()) {
                        Toast.makeText(requireContext(),
                                R.string.message_object_type_not_editable,
                                Toast.LENGTH_SHORT).show();
                        dismiss();
                        return;
                    }

                    //dataMode = result.getMode();

                    //if ("file".equals(dataMode)) {
                    //    b.tilText.setHint(type + " typetree (JSON)");
                    //} else {
                    //    b.tilText.setHint(type + " content");
                    //}

                    b.etText.setText(data);
                    setLoading(false, null);
                    b.btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    setLoading(false, null);
                    Toast.makeText(requireContext(), e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private void loadModel(ObjectData data) {
        b.tilText.setVisibility(View.GONE);
        b.txt2dContainer.setVisibility(View.GONE);
        b.btnSave.setVisibility(View.GONE);
        b.modelContainer.setVisibility(View.VISIBLE);
        io.execute(() -> {
            Context ctx = requireContext();
            File tmp = new File(ctx.getCacheDir(), "preview.obj");
            try {
                Files.write(tmp.toPath(), data.getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            main.post(() -> {
                setLoading(false, "Preview");
                if (tmp.exists()) {
                    b.model.updateFilePath(tmp.getAbsolutePath());
                    b.model.setOnLoadSceneListener(new MainView.OnLoadSceneListener() {
                        @Override
                        public void onLoadScene(String path) {
                            //noinspection ResultOfMethodCallIgnored
                            tmp.delete();
                        }

                        @Override
                        public void onLoadFailed(String path, Throwable t) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                                    getString(R.string.message_preview_failed_add, t.getMessage()),
                                    Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            });
        });
    }

    private void loadTexture2d(ObjectData data) {
        b.tilText.setVisibility(View.GONE);
        b.txt2dContainer.setVisibility(View.VISIBLE);
        b.btnSave.setVisibility(View.GONE);
        b.modelContainer.setVisibility(View.GONE);
        io.execute(() -> {
            if (data.getData() == null || data.getData().length == 0) {
                Toast.makeText(requireContext(), R.string.message_preview_failed_add,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    data.getData(),
                    0,
                    data.getData().length
            );
            main.post(() -> {
                setLoading(false, "Preview");
                if (bitmap != null) {
                    b.texture2d.setImageBitmap(bitmap);
                }
            });
        });
    }

    private void save() {
        final boolean doData = b.tilText.getVisibility() == View.VISIBLE;
        if (!doData) {
            return;
        }

        final String dataText = b.etText.getText() != null ? b.etText.getText().toString() : "";

        setLoading(true, getString(R.string.message_saving));

        unityPyRepository.setObjectData(sessionId, idx, dataText.getBytes())
                .addOnSuccessListener(result -> {
                    setLoading(false, null);
                    Toast.makeText(requireContext(), R.string.text_saved, Toast.LENGTH_SHORT)
                            .show();

                    if (callback != null) {
                        callback.onEdited(idx);
                    }

                    dismiss();
                })
                .addOnFailureListener(e -> {
                    setLoading(false, null);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading, String title) {
        if (b == null) {
            return;
        }

        b.btnCancel.setEnabled(!loading);
        b.btnSave.setEnabled(!loading);

        b.title.setText(
                Objects.requireNonNullElseGet(title, () -> getString(R.string.message_edit, type)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
        unityPyRepository.shutdown();
    }
}
