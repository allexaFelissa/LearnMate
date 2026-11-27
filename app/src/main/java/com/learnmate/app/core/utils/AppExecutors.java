package com.learnmate.app.core.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared thread pools for SQLite/PDF disk I/O, network calls, and Main UI thread callbacks.
 */
public final class AppExecutors {
    private static volatile AppExecutors instance;

    private final ExecutorService diskIO;
    private final ExecutorService networkIO;
    private final Executor mainThread;

    private AppExecutors() {
        this.diskIO = Executors.newSingleThreadExecutor();
        this.networkIO = Executors.newFixedThreadPool(3);
        this.mainThread = new MainThreadExecutor();
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public ExecutorService networkIO() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
