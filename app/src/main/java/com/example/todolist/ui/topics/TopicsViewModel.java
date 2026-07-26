package com.example.todolist.ui.topics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.todolist.data.Topic;
import com.example.todolist.data.TopicRepository;

import java.util.List;

public class TopicsViewModel extends AndroidViewModel {
    private final TopicRepository repo;

    public TopicsViewModel(@NonNull Application app) {
        super(app);
        repo = new TopicRepository(app);
    }

    public LiveData<List<Topic>> getTopics() { return repo.getAll(); }

    public void insert(Topic t) { repo.insert(t); }

    public void update(Topic t) { repo.update(t); }

    public void delete(Topic t) { repo.delete(t); }
}
