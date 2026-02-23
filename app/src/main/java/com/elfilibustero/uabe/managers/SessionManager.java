package com.elfilibustero.uabe.managers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chaquo.python.PyObject;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe singleton session manager.
 * <p>
 * - Stores sessions by ID in a ConcurrentHashMap
 * - Provides create/get/remove/clear
 * - Optional TTL cleanup (idle sessions)
 */
public final class SessionManager {

    // ---- Singleton (Initialization-on-demand holder, thread-safe) ----
    private SessionManager() {
    }

    public static SessionManager get() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    // ---- Storage ----
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // ---- Core API ----

    /**
     * Create a new session with a random ID and store it.
     */
    @NonNull
    public String create(@NonNull Session session) {
        String id = newId();
        session.sessionId = id;
        sessions.put(id, session);
        return id;
    }

    /**
     * Create and store a session using a caller-provided id (overwrites existing).
     */
    public void put(@NonNull String sessionId, @NonNull Session session) {
        session.sessionId = sessionId;
        sessions.put(sessionId, session);
    }

    /**
     * Get session or null. Optionally "touch" it to refresh lastAccess.
     */
    @Nullable
    public Session get(@NonNull String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Get session or throw if missing (useful for strict flows).
     */
    @NonNull
    public Session require(@NonNull String sessionId) {
        Session s = sessions.get(sessionId);
        if (s == null) {
            throw new IllegalStateException("Session not found: " + sessionId);
        }
        return s;
    }

    /**
     * True if session exists.
     */
    public boolean contains(@NonNull String sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * Remove and return session or null.
     */
    @Nullable
    public Session remove(@NonNull String sessionId) {
        return sessions.remove(sessionId);
    }

    /**
     * Remove everything.
     */
    public void clear() {
        sessions.clear();
    }

    /**
     * How many sessions currently stored.
     */
    public int size() {
        return sessions.size();
    }

    @NonNull
    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static class Session {
        public String sessionId;
        public PyObject env;
        public boolean dirty;
    }
}