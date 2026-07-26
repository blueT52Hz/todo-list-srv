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
import com.example.todolist.ui.settings.SettingsFragment;
import com.example.todolist.ui.tasks.TasksFragment;
import com.example.todolist.ui.topics.TopicsFragment;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> notificationPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestNotificationPermission();

        binding.navTasks.setOnClickListener(v -> selectTab(R.id.nav_tasks));
        binding.navTopics.setOnClickListener(v -> selectTab(R.id.nav_topics));
        binding.navSettings.setOnClickListener(v -> selectTab(R.id.nav_settings));

        if (savedInstanceState == null) {
            selectTab(R.id.nav_tasks);
        }
    }

    private int currentNavId = -1;

    /** Highlights the whole icon+label block of the chosen tab and swaps the fragment. */
    private void selectTab(int id) {
        if (id == currentNavId) return;
        currentNavId = id;

        binding.navTasks.setSelected(id == R.id.nav_tasks);
        binding.navTopics.setSelected(id == R.id.nav_topics);
        binding.navSettings.setSelected(id == R.id.nav_settings);

        Fragment fragment;
        if (id == R.id.nav_topics) {
            fragment = new TopicsFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new TasksFragment();
        }
        show(fragment);
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
