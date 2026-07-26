package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.ui.detail.TaskDetailActivity;

public class TasksWidgetProvider extends AppWidgetProvider {

    static final String ACTION_ITEM_CLICK = "com.example.todolist.widget.ITEM_CLICK";
    static final String EXTRA_MARK_DONE = "mark_done";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

            Intent svc = new Intent(context, TasksWidgetService.class);
            svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            // Adapter connections are cached by Intent.filterEquals(), which ignores extras;
            // without a distinct data URI the host reuses a stale adapter across updates.
            svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setRemoteAdapter(R.id.widget_list, svc);
            rv.setEmptyView(R.id.widget_list, R.id.widget_empty);

            Intent openApp = new Intent(context, MainActivity.class);
            PendingIntent appPi = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widget_header, appPi);

            // One template for the whole list; each row's fill-in intent decides whether the
            // tap opens the task or ticks it off.
            Intent itemClick = new Intent(context, TasksWidgetProvider.class)
                .setAction(ACTION_ITEM_CLICK);
            PendingIntent itemPi = PendingIntent.getBroadcast(context, 0, itemClick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            rv.setPendingIntentTemplate(R.id.widget_list, itemPi);

            mgr.updateAppWidget(id, rv);
        }
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_ITEM_CLICK.equals(intent.getAction())) {
            long taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L);
            if (taskId < 0) return;
            if (intent.getBooleanExtra(EXTRA_MARK_DONE, false)) {
                markDone(context, taskId);
            } else {
                context.startActivity(new Intent(context, TaskDetailActivity.class)
                    .putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return;
        }
        super.onReceive(context, intent);
    }

    private void markDone(Context context, long taskId) {
        Context app = context.getApplicationContext();
        PendingResult pending = goAsync();
        ReminderScheduler.cancel(app, taskId);
        new TaskRepository(app).setDone(taskId, true, pending::finish);
    }
}
