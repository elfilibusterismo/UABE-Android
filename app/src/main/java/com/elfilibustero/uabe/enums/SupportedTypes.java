package com.elfilibustero.uabe.enums;

import androidx.annotation.Nullable;

public enum SupportedTypes {
    ASSET_BUNDLE("AssetBundle", true, true, true),
    GAME_OBJECT("GameObject", true, true, true),
    MONO_BEHAVIOUR("MonoBehaviour", true, true, true),
    MESH("Mesh", true, false),
    TEXT_ASSET("TextAsset", true, true, true),
    TEXTURE_2D("Texture2D", true, true);

    private final String name;
    private final boolean canExport;
    private final boolean canImport;
    private final boolean editable;

    SupportedTypes(String name, boolean canExport, boolean canImport) {
        this(name, canExport, canImport, false);
    }

    SupportedTypes(String name, boolean canExport, boolean canImport, boolean editable) {
        this.name = name;
        this.canExport = canExport;
        this.canImport = canImport;
        this.editable = editable;
    }

    public String getName() {
        return name;
    }

    public boolean canExport() {
        return canExport;
    }

    public boolean canImport() {
        return canImport;
    }

    public boolean isEditable() {
        return editable;
    }

    @Nullable
    public static SupportedTypes fromName(String name) {
        for (SupportedTypes type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
