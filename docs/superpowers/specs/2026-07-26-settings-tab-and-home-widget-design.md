# Tasca — Settings Tab & Home-screen Widget Design Spec

**Date:** 2026-07-26
**Status:** Approved (brainstorming)
**Scope:** Bổ sung vào app To-do List "Tasca" hiện có: thêm tab Settings (thứ 3 trên BottomNav) và một App Widget hiện danh sách task chưa hoàn thành trên màn hình chính. Không đụng logic Room/reminder cũ.

## 1. Mục tiêu

Thêm 2 tiện ích cho app đang chạy:
- **Tab Settings** với 2 hành động: (a) thêm widget vào màn hình chính, (b) test thông báo (bắn ngay).
- **Home-screen widget** dạng collection hiện các task chưa xong; bấm mở chi tiết task.

## 2. Điều hướng

- Thêm item thứ 3 `nav_settings` (icon `ic_settings`, title `@string/settings`) vào `res/menu/bottom_nav_menu.xml`. BottomNav thành 3 tab: **Tasks · Topics · Settings**.
- Tạo `ui/settings/SettingsFragment.java`. Thêm nhánh xử lý `nav_settings` vào logic swap fragment sẵn có trong `MainActivity.java`.
- Settings là màn hành động thuần — **không** ViewModel, không Room observe.

## 3. SettingsFragment

Layout `fragment_settings.xml` theo theme Tasca (nền Canvas Mist, hàng phẳng có divider Whisper Border, không CardView). Ba khối:

**a. Hàng "Thêm widget vào màn hình chính"**
- Bấm → `AppWidgetManager.getInstance(ctx).isRequestPinAppWidgetSupported()`; nếu `true` → `requestPinAppWidget(new ComponentName(ctx, TasksWidgetProvider.class), null, null)`.
- Nếu `false` (launcher không hỗ trợ pin) → Toast: hướng dẫn kéo widget thủ công từ danh sách widget của launcher.

**b. Hàng "Test thông báo"**
- Bấm → build 1 notification mẫu (title "Tasca", text "Đây là thông báo thử", `setSmallIcon`, priority cao) trên **đúng channel `reminders`** (channel do `TodoApp` tạo sẵn) và `NotificationManagerCompat.from(ctx).notify(id, n)` với `id` cố định riêng cho test (vd `9001`).
- Android 13+ (mọi thiết bị vì minSdk 34): nếu `POST_NOTIFICATIONS` chưa được cấp → xin quyền qua `ActivityResultContracts.RequestPermission`; cấp xong bắn lại, từ chối → Toast báo cần bật quyền trong Settings.

**c. Caption trạng thái quyền thông báo**
- 1 dòng text nhỏ (Muted Steel) hiện "Thông báo: đang bật/đã tắt" dựa trên `NotificationManagerCompat.from(ctx).areNotificationsEnabled()`. Cập nhật lại trong `onResume`. Chỉ hiển thị, không hành động.

## 4. Home-screen Widget (collection — pending tasks)

**Thành phần:**
- `widget/TasksWidgetProvider.java` — `AppWidgetProvider`. `onUpdate`: set `RemoteViews(widget_tasks.xml)`, `setRemoteAdapter` trỏ `TasksWidgetService`, set `PendingIntentTemplate` mở `TaskDetailActivity`, set click header mở `MainActivity`, gọi `notifyAppWidgetViewDataChanged`.
- `widget/TasksWidgetService.java` — `RemoteViewsService`, trả `TasksRemoteViewsFactory`.
- `widget/TasksRemoteViewsFactory.java` — `RemoteViewsService.RemoteViewsFactory`. `onDataSetChanged` đọc đồng bộ danh sách task **chưa `done`**, sort theo `dueAt` (null xuống cuối). Mỗi item render `widget_item_task.xml`: title, due (giờ monospace, ẩn nếu null), topic dot (màu theo topic, ẩn nếu không topic). `getViewAt` set `fillInIntent` mang extra `task_id` = id task (khớp `ReminderScheduler.EXTRA_TASK_ID`/`TaskDetailActivity` — dùng cùng key `"task_id"`).
- `widget/WidgetUpdater.java` — helper `refresh(Context)`: lấy mọi widget id của `TasksWidgetProvider` qua `AppWidgetManager`, gọi `notifyAppWidgetViewDataChanged(ids, R.id.widget_list)`. No-op an toàn nếu chưa có widget nào.

**Dữ liệu:** cần sync getter "tất cả task chưa done" trong `TaskDao`. Nếu chưa có, thêm `List<Task> getPendingSync()` (`SELECT * FROM tasks WHERE done = 0 ORDER BY dueAt IS NULL, dueAt ASC`). Factory chạy trên thread của RemoteViewsService nên gọi sync trực tiếp là hợp lệ. Topic color: nạp map topicId→colorHex qua `topicDao.getAllSync()`.

**Đồng bộ khi data đổi:** sau mỗi insert/update/delete/toggle task đã ghi Room, gọi `WidgetUpdater.refresh(appContext)`. Đặt lời gọi trong `TaskRepository` (sau khi executor ghi xong, post về khi cần) để mọi đường ghi task đều cập nhật widget. Repository phải giữ `Context` (application context) — hiện đã có qua khởi tạo; nếu chưa, truyền application context vào.

**Layout & resource:**
- `res/layout/widget_tasks.xml`: root `bg_widget` (nền Canvas Mist bo góc), header TextView "Tasca" (bấm mở app), `ListView android:id="@+id/widget_list"`, `TextView android:id="@+id/widget_empty"` ("Không có việc nào") set qua `setEmptyView`.
- `res/layout/widget_item_task.xml`: hàng ngang — topic dot (View 8dp `bg_topic_dot`) + title (charcoal) + due (monospace, muted). Chỉ dùng view RemoteViews hỗ trợ (TextView/ImageView/View/LinearLayout).
- `res/xml/tasks_widget_info.xml`: `appwidget-provider` — `minWidth 250dp`, `minHeight 110dp`, `resizeMode horizontal|vertical`, `widgetCategory home_screen`, `initialLayout @layout/widget_tasks`, `updatePeriodMillis 0` (cập nhật chủ động qua WidgetUpdater), `previewImage` (dùng `ic_widget` tạm nếu chưa có ảnh).
- Drawable mới: `ic_settings.xml`, `ic_widget.xml`, `ic_bell.xml`, `bg_widget.xml` (vector/shape, palette Tasca).

**Manifest:** đăng ký
```xml
<receiver android:name=".widget.TasksWidgetProvider" android:exported="false">
  <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>
  <meta-data android:name="android.appwidget.provider" android:resource="@xml/tasks_widget_info"/>
</receiver>
<service android:name=".widget.TasksWidgetService" android:permission="android.permission.BIND_REMOTEVIEWS" android:exported="false"/>
```

## 5. File thay đổi

**Tạo mới:**
- `ui/settings/SettingsFragment.java`
- `widget/TasksWidgetProvider.java`, `TasksWidgetService.java`, `TasksRemoteViewsFactory.java`, `WidgetUpdater.java`
- `res/layout/fragment_settings.xml`, `widget_tasks.xml`, `widget_item_task.xml`
- `res/xml/tasks_widget_info.xml`
- `res/drawable/ic_settings.xml`, `ic_widget.xml`, `ic_bell.xml`, `bg_widget.xml`

**Sửa:**
- `res/menu/bottom_nav_menu.xml` (+ item settings)
- `ui/MainActivity.java` (nhánh `nav_settings`)
- `data/TaskDao.java` (thêm `getPendingSync()` nếu thiếu)
- `data/TopicDao.java` (đảm bảo có `getAllSync()`)
- `data/TaskRepository.java` (gọi `WidgetUpdater.refresh` sau ghi)
- `AndroidManifest.xml` (receiver + service)
- `res/values/strings.xml` (settings, test_notification, add_widget, …)

## 6. Kiểm thử

Theo override user (không viết test khi chưa yêu cầu): **kiểm thử thủ công** trên emulator/thiết bị —
1. Tab Settings hiện đủ 2 hành động + caption quyền.
2. Bấm "Test thông báo" → notification xuất hiện; nếu chưa cấp quyền thì hiện dialog xin quyền rồi bắn.
3. Bấm "Thêm widget" → launcher hỗ trợ thì hiện dialog pin; không thì Toast.
4. Widget trên màn hình chính hiện task chưa xong; thêm/sửa/xong task trong app → widget cập nhật.
5. Bấm 1 task trên widget → mở đúng `TaskDetailActivity`.

## 7. Rủi ro & lưu ý

- `requestPinAppWidget` phụ thuộc launcher; một số launcher trả `isRequestPinAppWidgetSupported()==false` → chỉ còn kéo thủ công (đã có Toast fallback).
- RemoteViews chỉ hỗ trợ tập view giới hạn — item layout phải dùng view cơ bản, không custom view/binding.
- `updatePeriodMillis 0` nghĩa là widget không tự refresh định kỳ; mọi cập nhật đến từ `WidgetUpdater.refresh`. Chấp nhận cho phạm vi này.
- Test notification dùng `id` cố định riêng để không đè lên notification nhắc nhở thật (dùng `taskId` làm id).

## 8. Câu hỏi chưa giải quyết

- Không có.
