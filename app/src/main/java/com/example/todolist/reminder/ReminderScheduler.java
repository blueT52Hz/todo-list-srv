package com.example.todolist.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.todolist.data.Task;

/** Schedules / cancels exact alarms for task reminders. */
public final class ReminderScheduler {

    public static final String EXTRA_TASK_ID = "task_id";

    /** Sets an exact alarm at task.dueAt when the task is pending and in the future; otherwise cancels. */
    public static void schedule(Context ctx, Task t) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am == null) return;
        if (t.dueAt == null || t.done || t.dueAt <= System.currentTimeMillis()) {
            cancel(ctx, t.id);
            return;
        }
        PendingIntent pi = pendingIntent(ctx, t.id);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.dueAt, pi);
        } catch (SecurityException e) {
            // Exact-alarm permission not granted → fall back to an inexact alarm.
            am.set(AlarmManager.RTC_WAKEUP, t.dueAt, pi);
        }
    }

    public static void cancel(Context ctx, long taskId) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(pendingIntent(ctx, taskId));
    }

    private static PendingIntent pendingIntent(Context ctx, long taskId) {
        Intent i = new Intent(ctx, ReminderReceiver.class).putExtra(EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(ctx, (int) taskId, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private ReminderScheduler() {}
}
