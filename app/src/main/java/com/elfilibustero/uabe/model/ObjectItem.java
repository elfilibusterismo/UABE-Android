package com.elfilibustero.uabe.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public final class ObjectItem implements Parcelable {

    @SerializedName("idx")
    private int index;
    @SerializedName("id")
    private long id;
    @NonNull
    @SerializedName("type")
    private String type;
    @NonNull
    @SerializedName("name")
    private String name;
    @Nullable
    @SerializedName("bytes")
    private Long bytes;
    @Nullable
    @SerializedName("container")
    private String container;
    @SerializedName("modified")
    private boolean modified;

    public ObjectItem(int index,
                      long id,
                      @NonNull String type,
                      @NonNull String name,
                      @Nullable Long bytes,
                      @Nullable String container,
                      boolean modified) {
        this.index = index;
        this.id = id;
        this.type = type;
        this.name = name;
        this.bytes = bytes;
        this.container = container;
        this.modified = modified;
    }

    public ObjectItem() {
        this(0, 0L, "", "", null, null, false);
    }

    private ObjectItem(@NonNull Parcel in) {
        index = in.readInt();
        id = in.readLong();
        type = nonNullString(in.readString());
        name = nonNullString(in.readString());

        // Nullable Integer
        bytes = (in.readByte() == 0) ? null : in.readLong();

        container = in.readString();
        modified = in.readByte() != 0;
    }

    @NonNull
    private static String nonNullString(String s) {
        return s != null ? s : "";
    }

    public static final Creator<ObjectItem> CREATOR = new Creator<>() {
        @NonNull
        @Override
        public ObjectItem createFromParcel(Parcel in) {
            return new ObjectItem(in);
        }

        @NonNull
        @Override
        public ObjectItem[] newArray(int size) {
            return new ObjectItem[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(index);
        dest.writeLong(id);
        dest.writeString(type);
        dest.writeString(name);

        if (bytes == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(bytes);
        }

        dest.writeString(container);
        dest.writeByte((byte) (modified ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int idx) {
        this.index = idx;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public Long getBytes() {
        return bytes;
    }

    public void setBytes(@Nullable Long bytes) {
        this.bytes = bytes;
    }

    @Nullable
    public String getContainer() {
        return container;
    }

    public void setContainer(@Nullable String container) {
        this.container = container;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    @NonNull
    @Override
    public String toString() {
        return "ObjectItem{idx=" + index + ", id=" + id + ", type='" + type + "', name='" + name +
                "', bytes=" + bytes + ", container='" + container + "', modified=" + modified + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ObjectItem that)) {
            return false;
        }
        return index == that.index && id == that.id;
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + Long.hashCode(id);
        return result;
    }
}
