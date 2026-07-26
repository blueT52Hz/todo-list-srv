# Tasca To-do List — Android App Design Spec

**Date:** 2026-07-26
**Status:** Approved (brainstorming)
**Scope:** Ứng dụng To-do List Android hoàn chỉnh, bám sát giao diện Stitch "Tasca" (Minimal & Airy). Không login — vào thẳng app.

## 1. Mục tiêu & phạm vi

Xây app Android quản lý công việc cá nhân, 1 người dùng, offline, không đăng nhập.

**Chức năng bắt buộc:**
- Thêm / sửa / xóa task; hiển thị dạng list.
- Thêm / sửa / xóa chủ đề (topic).
- Task đính kèm 1 ảnh.
- Lọc task theo chủ đề.
- Nhắc nhở đúng ngày+giờ đến hạn (notification).

**Ngoài phạm vi (YAGNI):** login/tài khoản, subtask/nested task, đồng bộ cloud, nhiều ảnh/task, chụp ảnh bằng camera, dark mode tùy chỉnh.

## 2. Stack kỹ thuật

- **Ngôn ngữ:** Java (giữ scaffold hiện có), `minSdk 34`, `targetSdk 36`.
- **UI:** XML layout + Material Components 3 + **ViewBinding**.
- **Lưu trữ:** **Room** (SQLite).
- **List:** RecyclerView + ListAdapter/DiffUtil.
- **Ảnh:** Android **Photo Picker** (`ActivityResultContracts.PickVisualMedia`), copy vào internal storage; load bằng **Glide**.
- **Nhắc nhở:** **AlarmManager** (`setExactAndAllowWhileIdle`) + BroadcastReceiver + NotificationManager.
- **Font:** font hệ thống — `sans-serif` cho chữ, `monospace` cho số/ngày giờ. Không dùng file font ngoài.

**Deps thêm vào `app/build.gradle.kts`:** `androidx.room:room-runtime`, `room-compiler` (annotationProcessor), `androidx.recyclerview`, `com.github.bumptech.glide:glide`. Bật `buildFeatures { viewBinding = true }`.

## 3. Mô hình dữ liệu (Room)

**Entity `Topic`**
| field | kiểu | ghi chú |
|---|---|---|
| id | long (PK, autoGenerate) | |
| name | String | |
| colorHex | String | màu dot, vd `#2F7A6F` |
| createdAt | long | epoch millis |

**Entity `Task`**
| field | kiểu | ghi chú |
|---|---|---|
| id | long (PK, autoGenerate) | |
| title | String | |
| note | String? | mô tả ngắn (optional) |
| topicId | Long? | FK → Topic; `null` = không chủ đề |
| dueAt | Long? | epoch millis; `null` = không nhắc |
| done | boolean | |
| imagePath | String? | đường dẫn ảnh trong internal storage |
| createdAt | long | |

**Quan hệ & quy tắc:**
- Xóa Topic → các Task thuộc topic đó set `topicId = null` (không xóa task). (`onDelete = SET_NULL`, `topicId` nullable + index.)
- Task 1 cấp, không subtask.
- DAO trả `LiveData<List<Task>>` để UI observe; có query lọc theo `topicId`.

## 4. Kiến trúc & điều hướng

- **`MainActivity`**: chứa `BottomNavigationView` 2 tab → **Tasks** · **Topics** (host 2 Fragment).
- Pattern: Room DAO → **Repository** → **ViewModel** (AndroidViewModel + LiveData) → Fragment/Activity. Đơn giản, không thêm DI.

**Màn hình:**

| Màn (Stitch) | Thành phần | Nội dung |
|---|---|---|
| Today / Filtered | `TasksFragment` | Header + count, `ChipGroup` lọc topic (cuộn ngang), RecyclerView task, FAB thêm |
| Empty state | trong `TasksFragment` | View rỗng khi list trống + nút "Add a task" |
| New / Edit Task | `AddEditTaskBottomSheet` (BottomSheetDialogFragment) | title, chọn topic (chips), date+time picker, đính ảnh, Save |
| Topics Manager | `TopicsFragment` | RecyclerView topic (dot màu + đếm task), FAB thêm |
| Add / Edit Topic | `AddEditTopicBottomSheet` | name + chọn màu dot, Save/Delete |
| Task Detail | `TaskDetailActivity` | Ảnh lớn, topic, giờ nhắc, note, nút Edit (mở sheet) / Delete |

**Luồng lọc:** chọn chip topic trong `TasksFragment` → ViewModel đổi filter → DAO query lại → list cập nhật. Chip "All" = không lọc.

## 5. Nhắc nhở (reminder)

- Lưu task có `dueAt` (tương lai) → `ReminderScheduler.schedule(task)` đặt exact alarm qua AlarmManager, PendingIntent chứa `taskId`.
- `dueAt` đến → `ReminderReceiver` (BroadcastReceiver) đọc task từ Room, post notification (channel "reminders").
- Sửa `dueAt` / xóa task / đánh dấu done → hủy hoặc đặt lại alarm (`cancel` theo `taskId`).
- Khởi động lại máy → `BootReceiver` (`RECEIVE_BOOT_COMPLETED`) đặt lại alarm cho task chưa done còn `dueAt` tương lai.
- **Quyền:** `POST_NOTIFICATIONS` (xin runtime lần mở đầu), `USE_EXACT_ALARM` + `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`.

## 6. Giao diện (fidelity Tasca)

- `res/values/colors.xml`: nạp palette Tasca — Canvas Mist `#F7F8F7`, Pure Surface `#FFFFFF`, Charcoal Ink `#1C1F1E`, Muted Steel `#6B7270`, Faint Steel `#A0A5A3`, Muted Sage `#2F7A6F`, Sage Wash `#1A2F7A6F`, Soft Clay `#B4593F`, Whisper Border `#141C1F1E`.
- Theme `Theme.TodoList`: nền Canvas Mist, `colorPrimary` = Muted Sage, no ActionBar (đã sẵn).
- Task row: divider phẳng (không CardView), checkbox tròn, done → gạch ngang + Faint Steel; caption ngày/giờ `monospace`; overdue → Soft Clay; thumbnail ảnh bên phải nếu có.
- Chip lọc pill: chọn = Sage Wash fill + Muted Sage text.
- FAB Muted Sage góc dưới phải.
- Empty state: minh họa nhẹ + 1 dòng chữ + nút "Add a task".

## 7. Cấu trúc package đề xuất

```
com.example.todolist
├── data
│   ├── Task.java, Topic.java              (entities)
│   ├── TaskDao.java, TopicDao.java
│   ├── AppDatabase.java
│   └── TaskRepository.java, TopicRepository.java
├── reminder
│   ├── ReminderScheduler.java
│   ├── ReminderReceiver.java
│   └── BootReceiver.java
├── ui
│   ├── MainActivity.java
│   ├── tasks (TasksFragment, TasksViewModel, TaskAdapter, AddEditTaskBottomSheet)
│   ├── topics (TopicsFragment, TopicsViewModel, TopicAdapter, AddEditTopicBottomSheet)
│   └── detail (TaskDetailActivity)
└── util (ImageStorage.java, DateUtils.java)
```
Mỗi file giữ dưới ~200 dòng, tách adapter/viewmodel riêng.

## 8. Kiểm thử

Theo override của user (không viết test khi chưa yêu cầu): **kiểm thử thủ công** trên emulator/thiết bị — CRUD task/topic, lọc, đính ảnh, nhận notification đúng giờ. Không viết unit/instrumentation test trừ khi được yêu cầu.

## 9. Rủi ro & lưu ý

- Exact alarm trên một số OEM có thể bị hạn chế pin — chấp nhận cho phạm vi bài tập.
- Fidelity font không khớp Satoshi 100% (dùng font hệ thống) — đã thống nhất.
- Ảnh copy vào internal storage → xóa task cần xóa file ảnh kèm theo (tránh rác).

## 10. Câu hỏi chưa giải quyết

- Có cần seed sẵn vài topic mẫu (Work/Personal/Study/Errands) khi mở app lần đầu không? (Đề xuất: có, cho đỡ trống — sẽ xác nhận ở bước plan.)
