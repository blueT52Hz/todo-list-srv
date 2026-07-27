package com.example.todolist.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.todolist.R;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.Task;
import com.example.todolist.data.Topic;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.util.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads pending tasks synchronously from Room and renders each widget row. */
class TasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context ctx;
    private final List<Task> tasks = new ArrayList<>();
    private final Map<Long, String> topicColors = new HashMap<>();

    TasksRemoteViewsFactory(Context ctx) { this.ctx = ctx; }

    @Override public void onCreate() { }

    @Override
    public void onDataSetChanged() {
        AppDatabase db = AppDatabase.getInstance(ctx);
        tasks.clear();
        tasks.addAll(db.taskDao().getPendingSync());
        topicColors.clear();
        for (Topic t : db.topicDao().getAllSync()) {
            topicColors.put(t.id, t.colorHex);
        }
    }

    @Override public void onDestroy() { tasks.clear(); topicColors.clear(); }

    @Override public int getCount() { return tasks.size(); }

    @Override
    public RemoteViews getViewAt(int position) {
        Task t = tasks.get(position);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_item_task);

        rv.setTextViewText(R.id.widget_item_title, t.title);

        if (t.dueAt != null) {
            rv.setTextViewText(R.id.widget_item_due, DateUtils.formatDue(t.dueAt));
            rv.setViewVisibility(R.id.widget_item_due, View.VISIBLE);
        } else {
            rv.setViewVisibility(R.id.widget_item_due, View.GONE);
        }

        String hex = t.topicId != null ? topicColors.get(t.topicId) : null;
        if (hex != null) {
            try {
                rv.setInt(R.id.widget_item_dot, "setColorFilter", Color.parseColor(hex));
                rv.setViewVisibility(R.id.widget_item_dot, View.VISIBLE);
            } catch (IllegalArgumentException e) {
                rv.setViewVisibility(R.id.widget_item_dot, View.GONE);
            }
        } else {
            rv.setViewVisibility(R.id.widget_item_dot, View.GONE);
        }

        Intent open = new Intent().putExtra(ReminderScheduler.EXTRA_TASK_ID, t.id);
        rv.setOnClickFillInIntent(R.id.widget_item_root, open);

        Intent done = new Intent()
            .putExtra(ReminderScheduler.EXTRA_TASK_ID, t.id)
            .putExtra(TasksWidgetProvider.EXTRA_MARK_DONE, true);
        rv.setOnClickFillInIntent(R.id.widget_item_check, done);
        return rv;
    }

    @Override public RemoteViews getLoadingView() { return null; }
    @Override public int getViewTypeCount() { return 1; }
    @Override public long getItemId(int position) { return tasks.get(position).id; }
    @Override public boolean hasStableIds() { return true; }
}
