package com.example.todolist.ui.detail;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.todolist.data.AppDatabase;
import com.example.todolist.data.Task;
import com.example.todolist.data.TaskRepository;
import com.example.todolist.data.Topic;
import com.example.todolist.databinding.ActivityTaskDetailBinding;
import com.example.todolist.reminder.ReminderScheduler;
import com.example.todolist.ui.tasks.AddEditTaskBottomSheet;
import com.example.todolist.util.DateUtils;
import com.example.todolist.util.ImageStorage;

import java.io.File;

public class TaskDetailActivity extends AppCompatActivity {

    private ActivityTaskDetailBinding b;
    private TaskRepository repo;
    private long taskId;
    private Task current;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        repo = new TaskRepository(this);
        taskId = getIntent().getLongExtra("task_id", -1);

        b.btnBack.setOnClickListener(v -> finish());
        b.btnEdit.setOnClickListener(v ->
            AddEditTaskBottomSheet.newInstance(taskId).show(getSupportFragmentManager(), "edit_task"));
        b.btnDelete.setOnClickListener(v -> confirmDelete());

        // Editing happens in a BottomSheet dialog, which does NOT re-trigger onResume() on dismiss.
        // Reload the detail as soon as the edit sheet reports a successful save.
        getSupportFragmentManager().setFragmentResultListener(
            AddEditTaskBottomSheet.REQUEST_SAVED, this, (key, bundle) -> load());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        repo.getByIdAsync(taskId, task -> {
            if (task == null) { finish(); return; }
            current = task;
            bind(task);
        });
    }

    private void bind(Task t) {
        b.detailTitle.setText(t.title);

        if (t.dueAt != null) {
            b.detailDue.setText(DateUtils.formatDue(t.dueAt));
            b.detailDue.setVisibility(View.VISIBLE);
        } else {
            b.detailDue.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(t.note)) {
            b.detailNote.setText(t.note);
            b.detailNote.setVisibility(View.VISIBLE);
        } else {
            b.detailNote.setVisibility(View.GONE);
        }

        if (t.imagePath != null) {
            b.detailImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(new File(t.imagePath)).centerCrop().into(b.detailImage);
        } else {
            b.detailImage.setVisibility(View.GONE);
        }

        // topic name + dot color (background thread lookup)
        if (t.topicId != null) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Topic topic = AppDatabase.getInstance(this).topicDao().getByIdSync(t.topicId);
                main.post(() -> {
                    if (topic != null) {
                        b.detailTopic.setText(topic.name);
                        b.detailTopic.setVisibility(View.VISIBLE);
                        b.detailDot.setVisibility(View.VISIBLE);
                        try { b.detailDot.getBackground().setTint(Color.parseColor(topic.colorHex)); }
                        catch (IllegalArgumentException ignored) { }
                    }
                });
            });
        } else {
            b.detailTopic.setVisibility(View.GONE);
            b.detailDot.setVisibility(View.GONE);
        }
    }

    private void confirmDelete() {
        if (current == null) return;
        new AlertDialog.Builder(this)
            .setMessage(getString(com.example.todolist.R.string.delete) + "?")
            .setPositiveButton(com.example.todolist.R.string.delete, (d, w) -> {
                ImageStorage.delete(current.imagePath);
                ReminderScheduler.cancel(this, current.id);
                repo.delete(current);
                finish();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
}
