package com.example.todolist.data;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "topics")
public class Topic {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public String colorHex;
    public long createdAt;
}
