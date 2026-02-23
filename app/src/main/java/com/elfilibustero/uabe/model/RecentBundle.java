package com.elfilibustero.uabe.model;

import java.util.HashSet;

public class RecentBundle {
    public String path;
    public String displayName;
    public HashSet<Integer> modifiedIdx = new HashSet<>();
    public long lastOpened;

    public RecentBundle() {}

    public RecentBundle(String path, String displayName, long lastOpened) {
        this.path = path;
        this.displayName = displayName;
        this.lastOpened = lastOpened;
    }

    public RecentBundle(String path, String displayName, HashSet<Integer> modifiedIdx, long lastOpened) {
        this.path = path;
        this.displayName = displayName;
        this.modifiedIdx = modifiedIdx;
        this.lastOpened = lastOpened;
    }
}
