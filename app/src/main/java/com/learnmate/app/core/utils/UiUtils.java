package com.learnmate.app.core.utils;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Shared UI helpers for edge-to-edge safe insets and toolbar back navigation.
 */
public final class UiUtils {
    private UiUtils() {}

    /**
     * Applies system bar (status bar + navigation bar + IME) padding to the root view
     * so content never overlaps system bars on Android 15+ edge-to-edge devices.
     */
    public static void applySystemBarInsets(View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    /**
     * Configures a MaterialToolbar as the Activity's ActionBar with an optional back button.
     */
    public static void setupToolbar(AppCompatActivity activity, MaterialToolbar toolbar, String title, boolean showBack) {
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(title);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(showBack);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(showBack);
        }
        if (showBack) {
            toolbar.setNavigationOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());
        }
    }
}
