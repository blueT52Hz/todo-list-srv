package com.example.todolist.data;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "topics")
public class Topic {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String name;
    @NonNull public String colorHex;
    public long createdAt;
}
