package com.elfilibustero.fmod;

public class FmodException extends RuntimeException {
    public final int result;

    public FmodException(int result, String message) {
        super(message + " (FMOD_RESULT=" + result + ")");
        this.result = result;
    }
}
