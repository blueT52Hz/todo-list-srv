package com.example.todolist.reminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.todolist.R;
import com.example.todolist.TodoApp;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.Task;
import com.example.todolist.ui.detail.TaskDetailActivity;
import com.example.todolist.util.NotificationPrefs;

/** Fires when a task reminder is due → posts a notification. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1);
        if (taskId < 0) return;
        Context app = context.getApplicationContext();

        // Respect the user's notifications toggle from Settings.
        if (!NotificationPrefs.isEnabled(app)) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task t = AppDatabase.getInstance(app).taskDao().getByIdSync(taskId);
            if (t == null || t.done) return;
            notify(app, t);
        });
    }

    private void notify(Context ctx, Task t) {
        Intent open = new Intent(ctx, TaskDetailActivity.class).putExtra("task_id", t.id);
        PendingIntent pi = PendingIntent.getActivity(ctx, (int) t.id, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, TodoApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(t.title)
            .setContentText("Task reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi);

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(ctx).notify((int) t.id, builder.build());
        }
    }
}
