package com.example.todolist.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** Notifies any placed Tasks widgets to reload their list. No-op when none are placed. */
public final class WidgetUpdater {
    private WidgetUpdater() {}

    public static void refresh(Context ctx) {
        Context app = ctx.getApplicationContext();
        AppWidgetManager mgr = AppWidgetManager.getInstance(app);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(app, TasksWidgetProvider.class));
        if (ids.length == 0) return;

        // notifyAppWidgetViewDataChanged alone is dropped when the host has torn down its
        // adapter connection (widget off-screen, launcher restarted), so go through onUpdate
        // to rebuild the RemoteViews as well.
        Intent update = new Intent(app, TasksWidgetProvider.class)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        app.sendBroadcast(update);
    }
}
