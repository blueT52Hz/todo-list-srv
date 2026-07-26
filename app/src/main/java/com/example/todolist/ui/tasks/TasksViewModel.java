package com.example.todolist.ui.tasks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.todolist.data.Task;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.Topic;
import com.example.todolist.data.TopicRepository;

import java.util.List;

public class TasksViewModel extends AndroidViewModel {
    private final TaskRepository taskRepo;
    private final TopicRepository topicRepo;
    /** null = show all topics. */
    private final MutableLiveData<Long> filter = new MutableLiveData<>(null);
    private final LiveData<List<Task>> tasks;

    public TasksViewModel(@NonNull Application app) {
        super(app);
        taskRepo = new TaskRepository(app);
        topicRepo = new TopicRepository(app);
        tasks = Transformations.switchMap(filter, id ->
            id == null ? taskRepo.getAll() : taskRepo.getByTopic(id));
    }

    public LiveData<List<Task>> getTasks() { return tasks; }

    public LiveData<List<Topic>> getTopics() { return topicRepo.getAll(); }

    public void setFilter(Long topicId) { filter.setValue(topicId); }

    public Long getFilter() { return filter.getValue(); }

    public void insert(Task t, TaskRepository.OnId cb) { taskRepo.insert(t, cb); }

    public void update(Task t) { taskRepo.update(t); }

    public void delete(Task t) { taskRepo.delete(t); }

    public void getTaskById(long id, TaskRepository.OnTask cb) { taskRepo.getByIdAsync(id, cb); }
}
