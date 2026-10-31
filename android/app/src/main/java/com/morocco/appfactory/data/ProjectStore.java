package com.morocco.appfactory.data;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ProjectStore {
    private final Context context;
    public ProjectStore(Context context) { this.context = context.getApplicationContext(); }

    public File root() throws IOException {
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        File dir = new File(base, "MoroccoFactory");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create workspace");
        return dir;
    }

    private File projects() throws IOException {
        File dir = new File(root(), "Projects");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create Projects folder");
        return dir;
    }

    public void save(Project project) throws Exception {
        project.updatedAt = System.currentTimeMillis();
        byte[] bytes = project.toJson().toString(2).getBytes(StandardCharsets.UTF_8);
        File target = new File(projects(), project.id + ".json");
        File temp = new File(projects(), project.id + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) { out.write(bytes); out.getFD().sync(); }
        if (!temp.renameTo(target)) throw new IOException("Could not commit project");
    }

    public Project load(String id) throws Exception {
        if (!id.matches("[a-zA-Z0-9-]{1,64}")) throw new IOException("Invalid project ID");
        return Project.parse(read(new File(projects(), id + ".json")));
    }

    public List<Project> list() throws IOException {
        List<Project> items = new ArrayList<>();
        File[] files = projects().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) throw new IOException("Could not read workspace");
        for (File file : files) {
            try { items.add(Project.parse(read(file))); }
            catch (Exception e) { android.util.Log.w("ProjectStore", "Unreadable project: " + file.getName(), e); }
        }
        Collections.sort(items, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return items;
    }

    public void delete(Project project) throws IOException {
        File file = new File(projects(), project.id + ".json");
        if (file.exists() && !file.delete()) throw new IOException("Could not delete project");
    }

    public static String read(File file) throws IOException {
        if (file.length() > 4 * 1024 * 1024) throw new IOException("Project exceeds 4 MB limit");
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString("UTF-8");
        }
    }

    public void mirror(String folder, String name, byte[] content, String mime) throws IOException {
        String saved = context.getSharedPreferences("factory", 0).getString("workspace_uri", "");
        if (saved.isEmpty()) return;
        try {
            DocumentFile tree = DocumentFile.fromTreeUri(context, Uri.parse(saved));
            if (tree == null || !tree.canWrite()) throw new IOException("Reselect your workspace folder in Profile");
            DocumentFile dir = tree.findFile(folder);
            if (dir == null) dir = tree.createDirectory(folder);
            if (dir == null) throw new IOException("Cannot create shared folder");
            DocumentFile file = dir.findFile(name);
            if (file == null) file = dir.createFile(mime, name);
            if (file == null) throw new IOException("Cannot create shared file");
            try (OutputStream out = context.getContentResolver().openOutputStream(file.getUri(), "wt")) {
                if (out == null) throw new IOException("Cannot open shared file");
                out.write(content);
            }
        } catch (SecurityException e) { throw new IOException("Folder permission expired. Choose it again in Profile.", e); }
    }
}
