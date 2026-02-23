package com.elfilibustero.uabe.python.task;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class UnityTask<T> {

    public interface OnSuccessListener<T> {
        void onSuccess(T result);
    }

    public interface OnFailureListener {
        void onFailure(Throwable e);
    }

    public interface OnCompleteListener<T> {
        void onComplete(UnityTask<T> task);
    }

    public interface Continuation<T, R> {
        R then(UnityTask<T> task) throws Exception;
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final Object lock = new Object();

    private boolean complete;
    private boolean successful;
    private T result;
    private Throwable exception;

    private final List<ListenerHolder<OnSuccessListener<T>>> successListeners = new ArrayList<>();
    private final List<ListenerHolder<OnFailureListener>> failureListeners = new ArrayList<>();
    private final List<ListenerHolder<OnCompleteListener<T>>> completeListeners = new ArrayList<>();

    public boolean isComplete() {
        synchronized (lock) {
            return complete;
        }
    }

    public boolean isSuccessful() {
        synchronized (lock) {
            return complete && successful;
        }
    }

    @Nullable
    public T getResult() {
        synchronized (lock) {
            return result;
        }
    }

    @Nullable
    public Throwable getException() {
        synchronized (lock) {
            return exception;
        }
    }

    // default main-thread listeners
    public UnityTask<T> addOnSuccessListener(OnSuccessListener<T> l) {
        return addOnSuccessListener(MainThreadExecutor.INSTANCE, l);
    }

    public UnityTask<T> addOnFailureListener(OnFailureListener l) {
        return addOnFailureListener(MainThreadExecutor.INSTANCE, l);
    }

    public UnityTask<T> addOnCompleteListener(OnCompleteListener<T> l) {
        return addOnCompleteListener(MainThreadExecutor.INSTANCE, l);
    }

    // custom executor listeners
    public UnityTask<T> addOnSuccessListener(Executor ex, OnSuccessListener<T> l) {
        boolean callNow = false;
        T res = null;
        synchronized (lock) {
            if (!complete) {
                successListeners.add(new ListenerHolder<>(ex, l));
            } else if (successful) {
                callNow = true;
                res = result;
            }
        }
        if (callNow) {
            T finalRes = res;
            ex.execute(() -> l.onSuccess(finalRes));
        }
        return this;
    }

    public UnityTask<T> addOnFailureListener(Executor ex, OnFailureListener l) {
        boolean callNow = false;
        Throwable e = null;
        synchronized (lock) {
            if (!complete) {
                failureListeners.add(new ListenerHolder<>(ex, l));
            } else if (!successful) {
                callNow = true;
                e = exception;
            }
        }
        if (callNow) {
            Throwable finalE = e;
            ex.execute(() -> l.onFailure(
                    finalE != null ? finalE : new RuntimeException("Unknown error")));
        }
        return this;
    }

    public UnityTask<T> addOnCompleteListener(Executor ex, OnCompleteListener<T> l) {
        boolean callNow = false;
        synchronized (lock) {
            if (!complete) {
                completeListeners.add(new ListenerHolder<>(ex, l));
            } else {
                callNow = true;
            }
        }
        if (callNow) {
            ex.execute(() -> l.onComplete(this));
        }
        return this;
    }

    public <R> UnityTask<R> continueWith(Continuation<T, R> cont) {
        UnityTaskSource<R> src = new UnityTaskSource<>();
        addOnCompleteListener(task -> {
            try {
                src.setResult(cont.then(task));
            } catch (Throwable e) {
                src.setException(e);
            }
        });
        return src.getTask();
    }

    // used by UnityTaskSource
    void setResult(T r) {
        List<ListenerHolder<OnSuccessListener<T>>> sList;
        List<ListenerHolder<OnCompleteListener<T>>> cList;

        synchronized (lock) {
            if (complete) {
                return;
            }
            complete = true;
            successful = true;
            result = r;

            sList = new ArrayList<>(successListeners);
            cList = new ArrayList<>(completeListeners);

            successListeners.clear();
            failureListeners.clear();
            completeListeners.clear();
        }

        for (ListenerHolder<OnSuccessListener<T>> h : sList)
            h.executor.execute(() -> h.listener.onSuccess(r));
        for (ListenerHolder<OnCompleteListener<T>> h : cList)
            h.executor.execute(() -> h.listener.onComplete(this));
    }

    public <R> UnityTask<R> continueWithTask(ContinuationTask<T, R> cont) {
        UnityTaskSource<R> src = new UnityTaskSource<>();

        addOnCompleteListener(task -> {
            try {
                UnityTask<R> next = cont.then(task);
                if (next == null) {
                    src.setException(new NullPointerException("continueWithTask returned null"));
                    return;
                }

                // Bridge next task into src
                next.addOnSuccessListener(src::setResult)
                        .addOnFailureListener(src::setException);
            } catch (Throwable e) {
                src.setException(e);
            }
        });

        return src.getTask();
    }

    public <R> UnityTask<R> onSuccessTask(ContinuationTask<T, R> cont) {
        UnityTaskSource<R> src = new UnityTaskSource<>();

        addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                src.setException(task.getException() != null ? task.getException()
                        : new RuntimeException("Task failed"));
                return;
            }
            try {
                UnityTask<R> next = cont.then(task);
                if (next == null) {
                    src.setException(new NullPointerException("onSuccessTask returned null"));
                    return;
                }
                next.addOnSuccessListener(src::setResult)
                        .addOnFailureListener(src::setException);
            } catch (Throwable e) {
                src.setException(e);
            }
        });

        return src.getTask();
    }

    void setException(Throwable e) {
        List<ListenerHolder<OnFailureListener>> fList;
        List<ListenerHolder<OnCompleteListener<T>>> cList;

        synchronized (lock) {
            if (complete) {
                return;
            }
            complete = true;
            successful = false;
            exception = e;

            fList = new ArrayList<>(failureListeners);
            cList = new ArrayList<>(completeListeners);

            successListeners.clear();
            failureListeners.clear();
            completeListeners.clear();
        }

        for (ListenerHolder<OnFailureListener> h : fList)
            h.executor.execute(() -> h.listener.onFailure(e));
        for (ListenerHolder<OnCompleteListener<T>> h : cList)
            h.executor.execute(() -> h.listener.onComplete(this));
    }

    private static final class ListenerHolder<L> {
        final Executor executor;
        final L listener;

        ListenerHolder(Executor executor, L listener) {
            this.executor = executor;
            this.listener = listener;
        }
    }

    private enum MainThreadExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                command.run();
            } else {
                MAIN.post(command);
            }
        }
    }

    public interface ContinuationTask<T, R> {
        UnityTask<R> then(UnityTask<T> task) throws Exception;
    }
}
