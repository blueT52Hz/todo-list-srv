package com.example.todolist.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Thin wrapper over {@link TaskDao} that runs writes on a background executor
 * and delivers async single-row reads back on the main thread.
 */
public class TaskRepository {
    public interface OnId { void onId(long id); }
    public interface OnTask { void onTask(Task t); }

    private final TaskDao dao;
    private final Handler main = new Handler(Looper.getMainLooper());

    public TaskRepository(Context ctx) {
        dao = AppDatabase.getInstance(ctx).taskDao();
    }

    public LiveData<List<Task>> getAll() { return dao.getAll(); }

    public LiveData<List<Task>> getByTopic(long topicId) { return dao.getByTopic(topicId); }

    public void insert(Task t, OnId cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = dao.insert(t);
            if (cb != null) main.post(() -> cb.onId(id));
        });
    }

    public void update(Task t) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(t));
    }

    public void delete(Task t) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(t));
    }

    public void getByIdAsync(long id, OnTask cb) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task t = dao.getByIdSync(id);
            main.post(() -> cb.onTask(t));
        });
    }
}
