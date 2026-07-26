# Tasca — Category Filter Dropdown (multi-select) Design Spec

**Date:** 2026-07-26
**Status:** Approved (brainstorming)
**Scope:** Thay bộ lọc category (Topic) ở tab Tasks từ chip group chọn-đơn sang dropdown chọn-nhiều dạng BottomSheet: có ô search, mỗi category 1 checkbox, nút Áp dụng. Lọc theo nhiều category (OR) và kết hợp được với bộ lọc ngày. Không đụng việc tạo/sửa category (vẫn ở tab Topics).

## 1. Mục tiêu

- Đổi UI lọc category ở tab Tasks: nút "Danh mục ▾" → mở BottomSheet gồm ô search + danh sách category có checkbox + nút "Áp dụng"/"Bỏ chọn".
- Cho phép chọn **nhiều** category cùng lúc: task hiện nếu thuộc **bất kỳ** category nào được tích (OR). Không tích cái nào = hiện tất cả.
- Filter category **kết hợp** (AND) với filter ngày sẵn có (task phải khớp cả ngày lẫn tập category).
- Ô search chỉ **lọc/tìm** trong các category đã có; **không** tạo category mới (việc tạo vẫn ở tab Topics).

## 2. Quyết định đã chốt (brainstorming)

1. Ô input trong dropdown = **search** danh sách category hiện có (không tạo mới).
2. Không tích category nào = **hiện tất cả** task. Filter áp dụng khi bấm **"Áp dụng"** (không lọc live theo từng lần tích).
3. Filter ngày × filter category = **kết hợp** (AND giữa 2 chiều; trong chiều category là OR).
4. Cơ chế UI = **BottomSheetDialogFragment** (đúng pattern date picker / các bottom sheet sẵn có).

## 3. Data layer

**`TaskDao` — thêm query lọc gộp:**
```java
@Query("SELECT * FROM tasks " +
       "WHERE (:hasDate = 0 OR (dueAt >= :from AND dueAt < :to)) " +
       "AND (:hasCats = 0 OR topicId IN (:cats)) " +
       "ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
LiveData<List<Task>> getFiltered(int hasDate, long from, long to, int hasCats, List<Long> cats);
```

- **Empty-IN workaround:** SQLite báo lỗi cú pháp với `IN ()`. Khi không chọn category (`hasCats = 0`), truyền `cats = Collections.singletonList(-1L)` (id không tồn tại) để danh sách bind luôn có ≥1 phần tử; cờ `hasCats = 0` đã vô hiệu hoá mệnh đề `topicId IN (:cats)`.
- `hasDate`/`hasCats` dùng `int` (0/1) vì Room bind boolean sang int; giữ tường minh.

**`TaskRepository`:**
```java
public LiveData<List<Task>> getFiltered(int hasDate, long from, long to, int hasCats, List<Long> cats) {
    return dao.getFiltered(hasDate, from, to, hasCats, cats);
}
```

- Bỏ `getByTopic(...)` và `getByDueRange(...)` (biến thể `LiveData<List<Task>>`) khỏi repo + DAO nếu không còn nơi nào dùng sau khi chuyển sang `getFiltered`. **Giữ** `getDueAtInRange(...)` (chấm ngày ở date picker), `getPendingSync`, `getByIdSync`, `getByIdAsync`, insert/update/delete.
- Trước khi xoá `getByTopic`/`getByDueRange`, grep xác nhận không còn caller khác.

## 4. ViewModel — `TasksViewModel`

- **Bỏ:** `MutableLiveData<Long> filter`, `setFilter(Long)`, `getFilter()`.
- **Thêm:** `MutableLiveData<Set<Long>> categories` khởi tạo tập rỗng (rỗng = tất cả). Giữ `MutableLiveData<LocalDate> dateFilter`.
- **Gộp 2 chiều lọc** bằng `MediatorLiveData<FilterState>`:
  ```java
  static final class FilterState {
      final LocalDate date; final Set<Long> cats;
      FilterState(LocalDate d, Set<Long> c) { date = d; cats = c; }
  }
  ```
  - `state.addSource(dateFilter, d -> recompute());`
  - `state.addSource(categories, c -> recompute());`
  - `recompute()` → `state.setValue(new FilterState(dateFilter.getValue(), categories.getValue()))`.
  - Khởi tạo giá trị đầu cho `state` (date=null, cats=emptySet) để `switchMap` chạy ngay.
- `tasks = Transformations.switchMap(state, s -> { ... build query ... })`:
  ```java
  int hasDate = s.date != null ? 1 : 0;
  long from = hasDate == 1 ? startMillis(s.date) : 0L;
  long to   = hasDate == 1 ? startMillis(s.date.plusDays(1)) : 0L;
  boolean hasCats = s.cats != null && !s.cats.isEmpty();
  List<Long> cats = hasCats ? new ArrayList<>(s.cats) : Collections.singletonList(-1L);
  return taskRepo.getFiltered(hasDate, from, to, hasCats ? 1 : 0, cats);
  ```
- **API mới:** `setCategories(Set<Long>)`, `getCategories(): LiveData<Set<Long>>`.
- **Giữ:** `setDate(LocalDate)`, `clearDate()`, `getDate()`. **Bỏ** việc clear chéo (setDate không còn xoá category, và ngược lại) vì giờ kết hợp được.
- Giữ `getTopics()`, `insert/update/delete/getTaskById`.

## 5. UI — `CategoryFilterBottomSheet` (mới, package `ui.tasks`)

- Extends `BottomSheetDialogFragment`.
- **Arguments:** `long[]` id các category đang chọn (`newInstance(Set<Long> selected)`).
- **Hằng số:** `REQUEST_KEY = "category_filter"`, `RESULT_IDS = "selected_ids"` (long[]), `ARG_SELECTED = "arg_selected"`.
- **Nguồn dữ liệu:** observe `TopicRepository.getAll()` (LiveData) để dựng danh sách category (id, name, colorHex).
- **Layout `bottomsheet_category_filter.xml`:** theo theme Tasca —
  - Tiêu đề "Danh mục".
  - Ô search: `EditText` (hoặc `TextInputLayout`) với icon `ic_search`, hint `@string/search_category`.
  - `RecyclerView` danh sách category (cuộn dọc, chiều cao giới hạn hợp lý trong sheet).
  - `TextView` gợi ý rỗng `@string/no_category_hint` (hiện khi list gốc trống).
  - Hàng chân: nút text "Bỏ chọn" (`@string/clear_selection`) + nút "Áp dụng" (`@string/apply`).
- **`item_category_check.xml`:** hàng ngang — `CheckBox` + chấm màu (`bg_topic_dot`, tint theo `colorHex`) + tên (charcoal).
- **`CategoryCheckAdapter`:**
  - Giữ `List<Topic> full` (toàn bộ) + `List<Topic> shown` (sau search) + `Set<Long> checked` (trạng thái tích, độc lập với search).
  - `setItems(List<Topic>)`: cập nhật `full`, áp lại query search hiện tại → `shown`.
  - `filter(String q)`: `shown` = các topic có `name` chứa `q` (so sánh lowercase). **Không** đụng `checked`.
  - Toggle checkbox 1 dòng → thêm/bớt id trong `checked` (kể cả khi dòng đó đang bị search ẩn thì vẫn giữ nguyên vì chỉ ẩn hiển thị).
  - `clearChecks()`: `checked.clear()` + refresh.
  - `getChecked(): Set<Long>`.
- **Hành vi nút:**
  - "Áp dụng" → `getParentFragmentManager().setFragmentResult(REQUEST_KEY, bundle{RESULT_IDS = long[] từ checked})` rồi `dismiss()`.
  - "Bỏ chọn" → `adapter.clearChecks()` (không đóng sheet).
- **Kích thước:** để BottomSheet tự co theo nội dung; nếu cần, set `peekHeight`/expanded hợp lý (tuỳ chọn, không bắt buộc).

## 6. UI — `TasksFragment` + `fragment_tasks.xml`

**Layout:**
- **Bỏ** khối `HorizontalScrollView` + `ChipGroup` (id `chip_group`).
- Trong hàng filter hiện có (cùng hàng với `btn_pick_date`/`btn_clear_date`), **thêm**:
  - `btn_category` — `MaterialButton` OutlinedButton, text `@string/category`, icon `ic_arrow_drop_down` (iconGravity end), style đồng bộ nút ngày.
  - `btn_clear_category` — `ImageView` `ic_close`, `visibility=gone`, contentDescription `@string/clear_category` (giống `btn_clear_date`).
- Nếu hàng chật, cho hàng filter nằm trong `HorizontalScrollView` hoặc dùng layout weight; ưu tiên đơn giản, `wrap_content`.

**Fragment (`TasksFragment.java`):**
- Bỏ `buildChips`, `makeChip`, và listener `chipGroup`.
- Giữ observer `getTopics()` **nhưng chỉ** để dựng map màu: `adapter.setTopicColors(colors)` (dùng cho chấm topic trên item task). Không dựng chip nữa.
- `btn_category` click → mở `CategoryFilterBottomSheet.newInstance(current)` với `current = viewModel.getCategories().getValue()` qua `getChildFragmentManager()`.
- `btn_clear_category` click → `viewModel.setCategories(Collections.emptySet())`.
- `getChildFragmentManager().setFragmentResultListener(CategoryFilterBottomSheet.REQUEST_KEY, getViewLifecycleOwner(), (k, b) -> { long[] ids = b.getLongArray(RESULT_IDS); viewModel.setCategories(toSet(ids)); })`.
- Observe `viewModel.getCategories()`:
  - Nếu tập rỗng → `btn_category` text = `@string/category`, ẩn `btn_clear_category`.
  - Nếu có n>0 → text = "Danh mục (n)" (string format), hiện `btn_clear_category`.
- Date button + result listener + observer `getDate()` giữ nguyên.

## 7. Resource

**Strings mới (`values/strings.xml`):**
- `category` = "Danh mục"
- `category_count` = "Danh mục (%d)" (dùng cho nút khi có chọn)
- `search_category` = "Tìm danh mục..."
- `apply` = "Áp dụng"
- `clear_selection` = "Bỏ chọn"
- `clear_category` = "Bỏ lọc danh mục"
- `no_category_hint` = "Chưa có danh mục — tạo ở tab Topics"

Giữ `all` (còn dùng ở `AddEditTaskBottomSheet`). Bỏ string nào chỉ phục vụ chip filter cũ nếu có và không còn dùng (kiểm tra trước khi xoá).

**Drawable mới:** `ic_search.xml`, `ic_arrow_drop_down.xml` (vector, palette Tasca). Dùng lại `ic_close`, `bg_topic_dot`.

## 8. Edge cases & lưu ý

- Category đang được chọn bị xoá ở tab Topics → id vẫn còn trong `categories` set nhưng khớp 0 task (vô hại); lần mở sheet kế tiếp category đó không có trong list nên tự rụng khỏi lựa chọn khi Áp dụng lại. Không prune chủ động (YAGNI).
- Empty-IN: luôn truyền `cats` ≥1 phần tử (đã xử lý ở §3/§4).
- Search không phân biệt hoa/thường; so khớp `contains`. Đủ dùng, không cần bỏ dấu tiếng Việt (YAGNI).
- Filter là trạng thái theo instance `TasksFragment`; đổi tab tạo fragment mới → reset về "tất cả" (hành vi hiện có, không đổi).

## 9. Kiểm thử

Theo override user (không viết test khi chưa yêu cầu): **kiểm thử thủ công** —
1. Tab Tasks: nút "Danh mục ▾" hiện; bấm mở bottom sheet có search + list checkbox + Áp dụng/Bỏ chọn.
2. Tích 2 category + Áp dụng → list chỉ còn task thuộc 1 trong 2 category; nút hiện "Danh mục (2)", có ×.
3. Bấm × → về tất cả task, nút về "Danh mục".
4. Gõ search → list category lọc theo tên; tích/bỏ tích rồi xoá search vẫn giữ trạng thái tích.
5. Kết hợp: chọn 1 ngày + tích category → list chỉ còn task khớp cả ngày lẫn category.
6. "Bỏ chọn" trong sheet → bỏ hết tích, sheet vẫn mở.
7. Chưa có category (hoặc xoá hết ở tab Topics) → sheet hiện gợi ý rỗng.

## 10. File thay đổi

**Tạo mới:**
- `ui/tasks/CategoryFilterBottomSheet.java`
- `ui/tasks/CategoryCheckAdapter.java`
- `res/layout/bottomsheet_category_filter.xml`
- `res/layout/item_category_check.xml`
- `res/drawable/ic_search.xml`, `ic_arrow_drop_down.xml`

**Sửa:**
- `data/TaskDao.java` (+ `getFiltered`, −`getByTopic`/`getByDueRange` nếu hết caller)
- `data/TaskRepository.java` (+ `getFiltered`, dọn method tương ứng)
- `ui/tasks/TasksViewModel.java` (categories + FilterState + switchMap; bỏ single filter)
- `ui/tasks/TasksFragment.java` (bỏ chip, thêm nút danh mục + sheet + listeners)
- `res/layout/fragment_tasks.xml` (bỏ ChipGroup, thêm nút danh mục + ×)
- `res/values/strings.xml` (strings mới)

## 11. Câu hỏi chưa giải quyết

- Không có.
