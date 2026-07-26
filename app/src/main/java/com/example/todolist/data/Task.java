package com.example.todolist.data;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks",
    foreignKeys = @ForeignKey(entity = Topic.class, parentColumns = "id",
        childColumns = "topicId", onDelete = ForeignKey.SET_NULL),
    indices = {@Index("topicId")})
public class Task {
    @PrimaryKey(autoGenerate = true) public long id;
    public String title;
    @Nullable public String note;
    @Nullable public Long topicId;
    @Nullable public Long dueAt;
    public boolean done;
    @Nullable public String imagePath;
    public long createdAt;
}
