package com.example.todolist.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores multiple attachment image paths inside the single {@code Task.imagePath}
 * TEXT column, newline-separated. Keeps the schema unchanged (no migration): a
 * legacy single path splits into a one-element list, and an empty list joins back
 * to {@code null} so existing "imagePath == null" checks keep working.
 */
public final class ImagePaths {
    private static final String SEP = "\n";

    public static List<String> split(String stored) {
        List<String> out = new ArrayList<>();
        if (stored == null || stored.isEmpty()) return out;
        for (String p : stored.split(SEP)) {
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    public static String join(List<String> paths) {
        if (paths == null || paths.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) sb.append(SEP);
            sb.append(paths.get(i));
        }
        return sb.toString();
    }

    /** First image path, or null — used for compact single-thumbnail spots (list row). */
    public static String first(String stored) {
        List<String> l = split(stored);
        return l.isEmpty() ? null : l.get(0);
    }

    private ImagePaths() {}
}
