package com.example.todolist.data;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.*;

@Database(entities = {Task.class, Topic.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract TopicDao topicDao();

    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                            AppDatabase.class, "tasca.db")
                        .addCallback(SEED)
                        .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Callback SEED = new Callback() {
        @Override public void onCreate(@NonNull SupportSQLiteDatabase db) {
            databaseWriteExecutor.execute(() -> {
                TopicDao dao = INSTANCE.topicDao();
                long now = System.currentTimeMillis();
                seed(dao, "Work", "#2F7A6F", now);
                seed(dao, "Personal", "#B4593F", now + 1);
                seed(dao, "Study", "#6B7270", now + 2);
                seed(dao, "Errands", "#A0A5A3", now + 3);
            });
        }
    };
    private static void seed(TopicDao dao, String name, String hex, long t) {
        Topic tp = new Topic(); tp.name = name; tp.colorHex = hex; tp.createdAt = t; dao.insert(tp);
    }
}
