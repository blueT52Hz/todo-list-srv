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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class TasksViewModel extends AndroidViewModel {
    private final TaskRepository taskRepo;
    private final TopicRepository topicRepo;
    private final ZoneId zone = ZoneId.systemDefault();
    /** null = show all topics. */
    private final MutableLiveData<Long> filter = new MutableLiveData<>(null);
    /** null = no date filter (fall back to topic filter). */
    private final MutableLiveData<LocalDate> dateFilter = new MutableLiveData<>(null);
    private final LiveData<List<Task>> tasks;

    public TasksViewModel(@NonNull Application app) {
        super(app);
        taskRepo = new TaskRepository(app);
        topicRepo = new TopicRepository(app);

        LiveData<List<Task>> topicTasks = Transformations.switchMap(filter, id ->
            id == null ? taskRepo.getAll() : taskRepo.getByTopic(id));

        tasks = Transformations.switchMap(dateFilter, d ->
            d == null ? topicTasks
                      : taskRepo.getByDueRange(startMillis(d), startMillis(d.plusDays(1))));
    }

    private long startMillis(LocalDate d) {
        return d.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public LiveData<List<Task>> getTasks() { return tasks; }

    public LiveData<List<Topic>> getTopics() { return topicRepo.getAll(); }

    public void setFilter(Long topicId) {
        filter.setValue(topicId);
        dateFilter.setValue(null); // choosing a topic clears the date filter
    }

    public Long getFilter() { return filter.getValue(); }

    public void setDate(LocalDate d) { dateFilter.setValue(d); }

    public void clearDate() { dateFilter.setValue(null); }

    public LiveData<LocalDate> getDate() { return dateFilter; }

    public void insert(Task t, TaskRepository.OnId cb) { taskRepo.insert(t, cb); }

    public void update(Task t) { taskRepo.update(t); }

    public void delete(Task t) { taskRepo.delete(t); }

    public void getTaskById(long id, TaskRepository.OnTask cb) { taskRepo.getByIdAsync(id, cb); }
}
