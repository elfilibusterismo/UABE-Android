package com.elfilibustero.uabe.python.task;

public final class UnityTaskSource<T> {
    private final UnityTask<T> task = new UnityTask<>();

    public UnityTask<T> getTask() {
        return task;
    }

    public void setResult(T result) {
        task.setResult(result);
    }

    public void setException(Throwable e) {
        task.setException(e);
    }
}
