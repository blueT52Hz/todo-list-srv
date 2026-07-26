package com.example.todolist.ui.settings;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todolist.R;
import com.example.todolist.TodoApp;
import com.example.todolist.databinding.FragmentSettingsBinding;
import com.example.todolist.util.NotificationPrefs;
import com.example.todolist.widget.TasksWidgetProvider;

public class SettingsFragment extends Fragment {

    private static final int TEST_NOTIFICATION_ID = 9001;
    private FragmentSettingsBinding b;

    private final ActivityResultLauncher<String> notifPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) postTestNotification();
            else toast(getString(R.string.notif_permission_needed));
            updatePermissionCaption();
        });

    /** Permission request triggered by turning the notifications switch on. */
    private final ActivityResultLauncher<String> enableNotifPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                NotificationPrefs.setEnabled(requireContext(), true);
            } else {
                NotificationPrefs.setEnabled(requireContext(), false);
                setSwitchChecked(false);
                toast(getString(R.string.notif_permission_needed));
            }
            updatePermissionCaption();
        });

    private final CompoundButton.OnCheckedChangeListener notifToggleListener =
        (btn, checked) -> onNotifToggle(checked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentSettingsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.rowAddWidget.setOnClickListener(v -> requestPinWidget());
        b.rowTestNotification.setOnClickListener(v -> onTestNotification());
        syncNotifSwitch();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncNotifSwitch();
        updatePermissionCaption();
    }

    /** Reflect the stored toggle + current permission on the switch. */
    private void syncNotifSwitch() {
        setSwitchChecked(NotificationPrefs.isEnabled(requireContext()) && hasNotifPermission());
    }

    /** Set the switch state without triggering the toggle listener. */
    private void setSwitchChecked(boolean checked) {
        b.switchNotif.setOnCheckedChangeListener(null);
        b.switchNotif.setChecked(checked);
        b.switchNotif.setOnCheckedChangeListener(notifToggleListener);
    }

    private boolean hasNotifPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void onNotifToggle(boolean checked) {
        if (checked) {
            if (!hasNotifPermission()) {
                // Ask for permission; the launcher callback finalizes the state.
                enableNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
            NotificationPrefs.setEnabled(requireContext(), true);
        } else {
            NotificationPrefs.setEnabled(requireContext(), false);
        }
        updatePermissionCaption();
    }

    private void requestPinWidget() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(requireContext());
        ComponentName provider = new ComponentName(requireContext(), TasksWidgetProvider.class);
        if (mgr.isRequestPinAppWidgetSupported()) {
            mgr.requestPinAppWidget(provider, null, null);
        } else {
            toast(getString(R.string.widget_pin_unsupported));
        }
    }

    private void onTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        postTestNotification();
    }

    private void postTestNotification() {
        NotificationCompat.Builder n =
            new NotificationCompat.Builder(requireContext(), TodoApp.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.test_notification_body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(requireContext()).notify(TEST_NOTIFICATION_ID, n.build());
    }

    private void updatePermissionCaption() {
        boolean on = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
        b.notifStatus.setText(getString(on ? R.string.notif_on : R.string.notif_off));
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
