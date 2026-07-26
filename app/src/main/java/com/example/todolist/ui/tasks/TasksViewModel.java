package com.example.todolist.ui.tasks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.todolist.data.Task;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.Topic;
import com.example.todolist.data.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TasksViewModel extends AndroidViewModel {
    private final TaskRepository taskRepo;
    private final TopicRepository topicRepo;
    private final ZoneId zone = ZoneId.systemDefault();

    /** null = no date filter. */
    private final MutableLiveData<LocalDate> dateFilter = new MutableLiveData<>(null);
    /** empty = all categories. */
    private final MutableLiveData<Set<Long>> categories = new MutableLiveData<>(new HashSet<>());
    /** 0 = all, 1 = not done, 2 = done. */
    private final MutableLiveData<Integer> status = new MutableLiveData<>(0);
    private final MediatorLiveData<FilterState> state = new MediatorLiveData<>();
    private final LiveData<List<Task>> tasks;

    /** Snapshot of all filter dimensions; drives the combined query. */
    static final class FilterState {
        final LocalDate date;
        final Set<Long> cats;
        final int status;
        FilterState(LocalDate date, Set<Long> cats, int status) {
            this.date = date; this.cats = cats; this.status = status;
        }
    }

    public TasksViewModel(@NonNull Application app) {
        super(app);
        taskRepo = new TaskRepository(app);
        topicRepo = new TopicRepository(app);

        state.setValue(new FilterState(null, new HashSet<>(), 0));
        state.addSource(dateFilter, d -> recompute());
        state.addSource(categories, c -> recompute());
        state.addSource(status, s -> recompute());

        tasks = Transformations.switchMap(state, s -> {
            int hasDate = s.date != null ? 1 : 0;
            long from = hasDate == 1 ? startMillis(s.date) : 0L;
            long to = hasDate == 1 ? startMillis(s.date.plusDays(1)) : 0L;
            boolean hasCats = s.cats != null && !s.cats.isEmpty();
            List<Long> cats = hasCats ? new ArrayList<>(s.cats) : Collections.singletonList(-1L);
            return taskRepo.getFiltered(hasDate, from, to, hasCats ? 1 : 0, cats, s.status);
        });
    }

    private void recompute() {
        int s = status.getValue() != null ? status.getValue() : 0;
        state.setValue(new FilterState(dateFilter.getValue(), categories.getValue(), s));
    }

    private long startMillis(LocalDate d) {
        return d.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public LiveData<List<Task>> getTasks() { return tasks; }

    public LiveData<List<Topic>> getTopics() { return topicRepo.getAll(); }

    public void setCategories(Set<Long> ids) {
        categories.setValue(ids != null ? new HashSet<>(ids) : new HashSet<>());
    }

    public LiveData<Set<Long>> getCategories() { return categories; }

    public void setDate(LocalDate d) { dateFilter.setValue(d); }

    public void clearDate() { dateFilter.setValue(null); }

    public LiveData<LocalDate> getDate() { return dateFilter; }

    public void setStatus(int s) { status.setValue(s); }

    public LiveData<Integer> getStatus() { return status; }

    public void insert(Task t, TaskRepository.OnId cb) { taskRepo.insert(t, cb); }

    public void update(Task t) { taskRepo.update(t); }

    public void delete(Task t) { taskRepo.delete(t); }

    public void getTaskById(long id, TaskRepository.OnTask cb) { taskRepo.getByIdAsync(id, cb); }
}
