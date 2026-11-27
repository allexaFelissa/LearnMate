package com.learnmate.app.core.ai;

/**
 * Generic async callback delivered on the Main UI thread.
 */
public interface AiCallback<T> {
    void onSuccess(T result);
    void onError(String errorMessage);
}
