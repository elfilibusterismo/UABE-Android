package com.elfilibustero.uabe.python.repo;

import androidx.annotation.Nullable;

public final class ApiResult<T> {
    public final boolean ok;
    @Nullable public final T data;
    @Nullable public final String error;
    @Nullable public final String trace;

    private ApiResult(boolean ok, @Nullable T data, @Nullable String error, @Nullable String trace) {
        this.ok = ok;
        this.data = data;
        this.error = error;
        this.trace = trace;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, data, null, null);
    }

    public static <T> ApiResult<T> fail(String error, @Nullable String trace) {
        return new ApiResult<>(false, null, error, trace);
    }

    public T requireData() {
        if (!ok || data == null) throw new IllegalStateException(error != null ? error : "Unknown error");
        return data;
    }
}
