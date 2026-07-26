package com.example.todolist;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todolist.databinding.ActivityMainBinding;
import com.example.todolist.ui.calendar.CalendarFragment;
import com.example.todolist.ui.settings.SettingsFragment;
import com.example.todolist.ui.tasks.TasksFragment;
import com.example.todolist.ui.topics.TopicsFragment;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> notificationPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        if (savedInstanceState == null) {
            show(new TasksFragment());
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tasks) {
                show(new TasksFragment());
                return true;
            } else if (id == R.id.nav_topics) {
                show(new TopicsFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                show(new SettingsFragment());
                return true;
            } else if (id == R.id.nav_calendar) {
                show(new CalendarFragment());
                return true;
            }
            return false;
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.nav_host, fragment)
            .commit();
    }
}
