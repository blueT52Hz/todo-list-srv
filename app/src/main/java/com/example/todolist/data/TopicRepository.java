package com.example.todolist.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

/** Thin wrapper over {@link TopicDao}; writes run on the shared background executor. */
public class TopicRepository {
    private final TopicDao dao;

    public TopicRepository(Context ctx) {
        dao = AppDatabase.getInstance(ctx).topicDao();
    }

    public LiveData<List<Topic>> getAll() { return dao.getAll(); }

    public void insert(Topic t) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(t));
    }

    public void update(Topic t) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(t));
    }

    public void delete(Topic t) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(t));
    }
}
