package com.example.todolist.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the user's "notifications enabled" toggle from Settings. Default: on. */
public final class NotificationPrefs {
    private static final String FILE = "settings";
    private static final String KEY_ENABLED = "notifications_enabled";

    public static boolean isEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private NotificationPrefs() {}
}
