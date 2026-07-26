package com.example.todolist.data;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    LiveData<List<Task>> getAll();
    @Query("SELECT * FROM tasks WHERE topicId = :topicId ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    LiveData<List<Task>> getByTopic(long topicId);
    @Query("SELECT * FROM tasks WHERE id = :id") Task getByIdSync(long id);
    @Query("SELECT * FROM tasks WHERE done = 0 AND dueAt IS NOT NULL") List<Task> getPendingWithDueSync();
    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY dueAt IS NULL, dueAt ASC, createdAt DESC")
    List<Task> getPendingSync();
    @Query("SELECT dueAt FROM tasks WHERE dueAt >= :from AND dueAt < :to")
    LiveData<List<Long>> getDueAtInRange(long from, long to);
    @Query("SELECT * FROM tasks WHERE dueAt >= :from AND dueAt < :to ORDER BY dueAt ASC")
    LiveData<List<Task>> getByDueRange(long from, long to);
    @Insert long insert(Task task);
    @Update void update(Task task);
    @Delete void delete(Task task);
}
