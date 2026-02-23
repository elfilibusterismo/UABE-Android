package com.elfilibustero.uabe.exceptions;

import androidx.annotation.Nullable;

public final class UnityPyException extends RuntimeException {
    @Nullable
    public final String trace;
    public final int code;

    public UnityPyException(String message, @Nullable String trace, int code) {
        super(message != null ? message : "UnityPy error");
        this.trace = trace;
        this.code = code;
    }
}
