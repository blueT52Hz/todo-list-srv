package com.example.todolist.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.ui.detail.TaskDetailActivity;

public class TasksWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

            Intent svc = new Intent(context, TasksWidgetService.class);
            svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            rv.setRemoteAdapter(R.id.widget_list, svc);
            rv.setEmptyView(R.id.widget_list, R.id.widget_empty);

            Intent openApp = new Intent(context, MainActivity.class);
            PendingIntent appPi = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widget_header, appPi);

            Intent openTask = new Intent(context, TaskDetailActivity.class);
            openTask.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent taskPi = PendingIntent.getActivity(context, 1, openTask,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            rv.setPendingIntentTemplate(R.id.widget_list, taskPi);

            mgr.updateAppWidget(id, rv);
        }
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
    }
}
