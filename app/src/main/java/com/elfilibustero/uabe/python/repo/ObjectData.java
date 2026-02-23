package com.elfilibustero.uabe.python.repo;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elfilibustero.uabe.enums.SupportedTypes;
import com.google.gson.annotations.SerializedName;

public class ObjectData implements Parcelable {

    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("idx")
    private int idx;

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type;

    @SerializedName("data")
    private byte[] data;

    public ObjectData() {
    }

    public ObjectData(String sessionId, int idx, long id, String name, String type, byte[] data) {
        this.sessionId = sessionId;
        this.idx = idx;
        this.id = id;
        this.name = name;
        this.type = type;
        this.data = data;
    }

    protected ObjectData(@NonNull Parcel in) {
        sessionId = in.readString();
        idx = in.readInt();
        id = in.readLong();
        name = in.readString();
        type = in.readString();
        data = in.createByteArray();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(sessionId);
        dest.writeInt(idx);
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(type);
        dest.writeByteArray(data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ObjectData> CREATOR = new Creator<>() {
        @NonNull
        @Override
        public ObjectData createFromParcel(Parcel in) {
            return new ObjectData(in);
        }

        @NonNull
        @Override
        public ObjectData[] newArray(int size) {
            return new ObjectData[size];
        }
    };

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SupportedTypes getSupportedType() {
        return SupportedTypes.fromName(type);
    }

    @Nullable
    public byte[] getData() {
        return data.clone();
    }

    public void setData(@Nullable byte[] data) {
        this.data = data;
    }
}