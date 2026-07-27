package com.example.todolist.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.todolist.widget.WidgetUpdater;

import java.util.List;

/**
 * Thin wrapper over {@link TaskDao} that runs writes on a background executor
 * and delivers async single-row reads back on the main thread.
 */
public class TaskRepository {
    public interface OnId { void onId(long id); }
    public interface OnTask { void onTask(Task t); }

    private final TaskDao dao;
    private final Context appContext;
    private final Handler main = new Handler(Looper.getMainLooper());

    public TaskRepository(Context ctx) {
        appContext = ctx.getApplicationContext();
        dao = AppDatabase.getInstance(ctx).taskDao();
    }

    public LiveData<List<Task>> getAll() { return dao.getAll(); }

    public LiveData<List<Task>> getFiltered(int hasDate, long from, long to,
                                            int hasCats, List<Long> cats, int status) {
        return dao.getFiltered(hasDate, from, to, hasCats, cats, status);
    }

    public void insert(Task t, OnId cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = dao.insert(t);
            WidgetUpdater.refresh(appContext);
            if (cb != null) main.post(() -> cb.onId(id));
        });
    }

    public void update(Task t) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.update(t);
            WidgetUpdater.refresh(appContext);
        });
    }

    public void delete(Task t) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.delete(t);
            WidgetUpdater.refresh(appContext);
        });
    }

    /** Flips the done flag of a stored task. {@code after} runs on the write executor thread. */
    public void setDone(long id, boolean done, Runnable after) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task t = dao.getByIdSync(id);
            if (t != null && t.done != done) {
                t.done = done;
                dao.update(t);
                WidgetUpdater.refresh(appContext);
            }
            if (after != null) after.run();
        });
    }

    public void getByIdAsync(long id, OnTask cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task t = dao.getByIdSync(id);
            main.post(() -> cb.onTask(t));
        });
    }
}
