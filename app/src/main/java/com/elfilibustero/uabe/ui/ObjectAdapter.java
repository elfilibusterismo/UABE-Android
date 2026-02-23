package com.elfilibustero.uabe.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.elfilibustero.uabe.R;
import com.elfilibustero.uabe.databinding.RowObjectItemBinding;
import com.elfilibustero.uabe.model.ObjectItem;

import java.util.List;
import java.util.Locale;

public class ObjectAdapter extends ListAdapter<ObjectItem, ObjectAdapter.VH> {

    public interface Listener {
        void onClick(ObjectItem item);

        void onLongClick(ObjectItem item);
    }

    private final Listener listener;

    public ObjectAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        ObjectItem it = getItem(position);
        return it != null ? it.getIndex() : RecyclerView.NO_ID;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowObjectItemBinding b = RowObjectItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ObjectItem it = getItem(position);
        if (it == null) {
            return;
        }
        bindAll(h, it);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(h, position);
            return;
        }

        ObjectItem it = getItem(position);
        if (it == null) {
            return;
        }

        Object p0 = payloads.get(0);
        if (!(p0 instanceof Bundle p)) {
            bindAll(h, it);
            return;
        }

        if (p.getBoolean("name")) {
            String name = safe(it.getName());
            if (name.isEmpty()) {
                name = "Unnamed asset";
            }
            h.b.title.setText(name);
        }

        if (p.getBoolean("type")) {
            String type = safe(it.getType());
            if (type.isEmpty()) {
                type = "Unknown";
            }

            h.b.type.setText(type);

            // type can affect icon too
            h.b.icon.setImageResource(typeToIcon(type));
        }

        if (p.getBoolean("modified")) {
            h.b.badge.setVisibility(it.isModified() ? View.VISIBLE : View.GONE);
        }

        if (p.getBoolean("container")) {
            String container = safe(it.getContainer());
            if (container.isEmpty()) {
                h.b.container.setVisibility(View.GONE);
            } else {
                h.b.container.setVisibility(View.VISIBLE);
                h.b.container.setText(container);
            }
        }

        // Subtitle depends on multiple fields; easiest is recompute if any relevant changed
        if (p.getBoolean("name") || p.getBoolean("modified") || p.getBoolean("type")
                || p.getBoolean("bytes") || p.getBoolean("id")) {
            h.b.subtitle.setText(buildSubtitle(it));
        }
    }

    private void bindAll(@NonNull VH h, @NonNull ObjectItem it) {
        String name = safe(it.getName());
        if (name.isEmpty()) {
            name = "Unnamed asset";
        }

        String type = safe(it.getType());
        if (type.isEmpty()) {
            type = "Unknown";
        }

        h.b.title.setText(name);

        h.b.type.setText(type);

        h.b.badge.setVisibility(it.isModified() ? View.VISIBLE : View.GONE);

        h.b.icon.setImageResource(typeToIcon(type));

        String container = safe(it.getContainer());
        if (container.isEmpty()) {
            h.b.container.setVisibility(View.GONE);
        } else {
            h.b.container.setVisibility(View.VISIBLE);
            h.b.container.setText(container);
        }

        h.b.subtitle.setText(buildSubtitle(it));
    }

    @NonNull
    private String buildSubtitle(@NonNull ObjectItem it) {
        StringBuilder sb = new StringBuilder();
        sb.append("id: ").append(it.getId());
        if (it.getBytes() != null) {
            sb.append(" • ").append(formatBytes(it.getBytes()));
        }
        return sb.toString();
    }

    public static class VH extends RecyclerView.ViewHolder {
        final RowObjectItemBinding b;

        VH(@NonNull RowObjectItemBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VH holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            ObjectItem it = getItem(pos);
            if (it != null) {
                listener.onClick(it);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return false;
            }
            ObjectItem it = getItem(pos);
            if (it != null) {
                listener.onLongClick(it);
            }
            return true;
        });
    }

    @NonNull
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @NonNull
    private static String formatBytes(long b) {
        if (b < 1024) {
            return b + " B";
        }
        double kb = b / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.US, "%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(Locale.US, "%.2f GB", gb);
    }

    private static int typeToIcon(@NonNull String type) {
        return switch (type) {
            case "Texture2D", "Sprite" -> R.drawable.ic_object_texture;
            case "TextAsset" -> R.drawable.ic_object_text;
            case "MonoBehaviour" -> R.drawable.ic_object_script;
            case "GameObject" -> R.drawable.ic_object_game_object;
            case "SkinnedMeshRenderer", "Mesh" -> R.drawable.ic_object_mesh;
            case "AudioClip" -> R.drawable.ic_object_audio;
            case "AnimationClip" -> R.drawable.ic_object_animation_clip;
            case "Animation" -> R.drawable.ic_object_animation;
            case "Light" -> R.drawable.ic_object_light;
            case "Transform" -> R.drawable.ic_object_transform;
            default -> R.drawable.ic_object_file;
        };
    }

    private static final DiffUtil.ItemCallback<ObjectItem> DIFF =
            new DiffUtil.ItemCallback<>() {

                @Override
                public boolean areItemsTheSame(@NonNull ObjectItem a, @NonNull ObjectItem c) {
                    return a.getIndex() == c.getIndex();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ObjectItem a, @NonNull ObjectItem c) {
                    return a.getIndex() == c.getIndex()
                            && a.getId() == c.getId()
                            && eq(a.getType(), c.getType())
                            && eq(a.getName(), c.getName())
                            && eq(a.getContainer(), c.getContainer())
                            && intEq(a.getBytes(), c.getBytes())
                            && a.isModified() == c.isModified();
                }

                @Nullable
                @Override
                public Object getChangePayload(@NonNull ObjectItem a, @NonNull ObjectItem c) {
                    Bundle p = new Bundle();
                    boolean any = false;

                    if (!eq(a.getName(), c.getName())) {
                        p.putBoolean("name", true);
                        any = true;
                    }
                    if (!eq(a.getType(), c.getType())) {
                        p.putBoolean("type", true);
                        any = true;
                    }
                    if (a.isModified() != c.isModified()) {
                        p.putBoolean("modified", true);
                        any = true;
                    }
                    if (!eq(a.getContainer(), c.getContainer())) {
                        p.putBoolean("container", true);
                        any = true;
                    }
                    if (!intEq(a.getBytes(), c.getBytes())) {
                        p.putBoolean("bytes", true);
                        any = true;
                    }
                    if (a.getId() != c.getId()) {
                        p.putBoolean("id", true);
                        any = true;
                    }

                    return any ? p : null;
                }

                private boolean eq(String x, String y) {
                    if (x == null) {
                        x = "";
                    }
                    if (y == null) {
                        y = "";
                    }
                    return x.equals(y);
                }

                private boolean intEq(Long x, Long y) {
                    if (x == null && y == null) {
                        return true;
                    }
                    if (x == null || y == null) {
                        return false;
                    }
                    return x.longValue() == y.longValue();
                }
            };
}
