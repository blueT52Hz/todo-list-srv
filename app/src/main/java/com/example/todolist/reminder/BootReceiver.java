package com.example.todolist.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.Task;

import java.util.List;

/** Re-schedules pending reminders after the device reboots. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Context app = context.getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Task> pending = AppDatabase.getInstance(app).taskDao().getPendingWithDueSync();
            for (Task t : pending) {
                ReminderScheduler.schedule(app, t);
            }
        });
    }
}
