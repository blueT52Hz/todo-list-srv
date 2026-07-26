package com.example.todolist.data;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY createdAt ASC") LiveData<List<Topic>> getAll();
    @Query("SELECT * FROM topics ORDER BY createdAt ASC") List<Topic> getAllSync();
    @Query("SELECT * FROM topics WHERE id = :id") Topic getByIdSync(long id);
    @Insert long insert(Topic topic);
    @Update void update(Topic topic);
    @Delete void delete(Topic topic);
}
