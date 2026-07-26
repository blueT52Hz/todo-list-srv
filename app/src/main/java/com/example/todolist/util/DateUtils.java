package com.example.todolist.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Formatting helpers for task due dates. */
public final class DateUtils {
    private static final SimpleDateFormat DISPLAY =
        new SimpleDateFormat("MMM d · HH:mm", Locale.getDefault());

    public static String formatDue(Long dueAt) {
        return dueAt == null ? "" : DISPLAY.format(new Date(dueAt));
    }

    public static boolean isOverdue(Long dueAt) {
        return dueAt != null && dueAt < System.currentTimeMillis();
    }

    private DateUtils() {}
}
