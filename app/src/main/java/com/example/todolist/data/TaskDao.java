package com.example.todolist.data;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    LiveData<List<Task>> getAll();
    @Query("SELECT * FROM tasks WHERE id = :id") Task getByIdSync(long id);
    @Query("SELECT * FROM tasks WHERE done = 0 AND dueAt IS NOT NULL") List<Task> getPendingWithDueSync();
    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY dueAt IS NULL, dueAt ASC, createdAt DESC")
    List<Task> getPendingSync();
    @Query("SELECT dueAt FROM tasks WHERE dueAt >= :from AND dueAt < :to")
    LiveData<List<Long>> getDueAtInRange(long from, long to);
    @Query("SELECT * FROM tasks " +
           "WHERE (:hasDate = 0 OR (dueAt >= :from AND dueAt < :to)) " +
           "AND (:hasCats = 0 OR topicId IN (:cats)) " +
           "ORDER BY done ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    LiveData<List<Task>> getFiltered(int hasDate, long from, long to, int hasCats, List<Long> cats);
    @Insert long insert(Task task);
    @Update void update(Task task);
    @Delete void delete(Task task);
}
