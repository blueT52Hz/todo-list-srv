package com.example.todolist;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

/** Application entry point — creates the reminder notification channel once. */
public class TodoApp extends Application {
    public static final String CHANNEL_ID = "reminders";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notifications when a task is due");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }
}
