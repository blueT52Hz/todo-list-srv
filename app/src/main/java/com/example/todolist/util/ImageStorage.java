package com.example.todolist.util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.todolist.data.AppDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/** Copies picked images into app-private internal storage and cleans them up. */
public final class ImageStorage {
    public interface OnSaved { void onSaved(String path); }

    public static void copyToInternal(Context ctx, Uri uri, OnSaved cb) {
        Handler main = new Handler(Looper.getMainLooper());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            String path = null;
            try {
                File dir = new File(ctx.getFilesDir(), "images");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, UUID.randomUUID() + ".jpg");
                try (InputStream in = ctx.getContentResolver().openInputStream(uri);
                     OutputStream os = new FileOutputStream(out)) {
                    if (in == null) throw new Exception("null stream");
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
                path = out.getAbsolutePath();
            } catch (Exception e) {
                path = null;
            }
            String finalPath = path;
            main.post(() -> cb.onSaved(finalPath));
        });
    }

    public static void delete(String path) {
        if (path != null) {
            File f = new File(path);
            if (f.exists()) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private ImageStorage() {}
}
