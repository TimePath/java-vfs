package com.timepath.vfs;

import com.timepath.io.utils.ViewableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple {@link com.timepath.vfs.VFile} implementation using synchronized recursive {@link java.util.HashMap}s
 *
 * @author TimePath
 */
public abstract class SimpleVFile implements VFile<SimpleVFile>, MutableVFile<SimpleVFile>, ViewableData, FileChangeListener {

    private static final Logger LOG = Logger.getLogger(SimpleVFile.class.getName());
    private static final String DEFAULT_GROUP = System.getProperty("user.name", "nobody");
    private static final String DEFAULT_OWNER = System.getProperty("user.name", "nobody");
    @NotNull
    protected static List<MissingFileHandler> missingFileHandlers = new LinkedList<>();
    protected final Map<String, SimpleVFile> files;
    @NotNull
    private final Collection<FileChangeListener> listeners;
    private long length;
    private long lastModified;
    @Nullable
    private SimpleVFile parent;

    protected SimpleVFile() {
        files = Collections.synchronizedMap(new HashMap<String, SimpleVFile>(0));
        listeners = new LinkedList<>();
        length = -1;
        lastModified = System.currentTimeMillis();
    }

    public static void registerMissingFileHandler(MissingFileHandler h) {
        missingFileHandlers.add(h);
    }

    public void addFileChangeListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public boolean createNewFile() {
        return false; // TODO: lazy node traversal
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Nullable
    @Override
    public SimpleVFile getParent() {
        return parent;
    }

    @NotNull
    @Override
    public Collection<? extends SimpleVFile> list() {
        return files.values();
    }

    @Nullable
    @Override
    public SimpleVFile get(String name) {
        SimpleVFile f = files.get(name);
        if (f != null) return f;
        for (@NotNull MissingFileHandler h : missingFileHandlers) {
            @Nullable SimpleVFile root = h.handle(this, name);
            if (root != null) return root;
        }
        return null;
    }

    @Nullable
    @Override
    public String getPath() {
        @Nullable String path = (isDirectory() ? getName() : "");
        path = path.replaceAll(VFile.SEPARATOR, ""); // just in case
        if (parent != null) {
            path = parent.getPath() + VFile.SEPARATOR + path;
        }
        return path;
    }

    @Override
    public long getTotalSpace() {
        return 0;
    }

    @Override
    public long getUsableSpace() {
        return 0;
    }

    @Override
    public abstract boolean isDirectory();

    @Override
    public boolean isFile() {
        return !isDirectory();
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public long length() {
        if (isDirectory()) {
            return files.size();
        }
        if (length == -1) {
            length = lengthEstimate();
        }
        return length;
    }

    @Override
    public boolean renameTo(SimpleVFile dest) {
        return false;
    }

    @Override
    public boolean setExecutable(boolean executable) {
        return false;
    }

    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setLastModified(long time) {
        lastModified = time;
        return true;
    }

    @Override
    public boolean setReadable(boolean readable) {
        return false;
    }

    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean setWritable(boolean writable) {
        return false;
    }

    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    private long lengthEstimate() {
        try {
            return openStream().available();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (NullPointerException ignored) {
        }
        return -1;
    }

    public boolean setParent(@Nullable SimpleVFile newParent) {
        if (parent == newParent) {
            return false;
        }
        if (parent != null) {
            parent.remove(this);
        }
        if (newParent != null) {
            parent = newParent;
            newParent.add(this);
        }
        return true;
    }

    /**
     * Get a file separated by {@code SEPARATOR}
     * TODO: this should be a default interface method
     *
     * @param path
     * @return the file, or null
     */
    @Nullable
    public SimpleVFile query(@Nullable String path) {
        if (path == null) throw new IllegalArgumentException("path cannot be null");
        @NotNull String[] split = path.split(VFile.SEPARATOR);
        if (split.length == 1) {
            return get(path);
        }
        @NotNull Deque<String> stack = new LinkedList<>();
        for (@NotNull String token : split) {
            switch (token) {
                case "": // ignore repeated separator
                    break;
                case ".": // ignore current directory
                    break;
                case "..":
                    if (!stack.isEmpty()) { // ignore extra cdup's
                        stack.removeLast();
                    }
                    break;
                default:
                    stack.addLast(token);
                    break;
            }
        }
        LOG.log(Level.FINE, "Getting {0}", stack);
        @Nullable SimpleVFile result = this;
        for (String token : stack) {
            result = result.get(token);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    @NotNull
    @Override
    public SimpleVFile add(@Nullable SimpleVFile file) {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        if (file == this) throw new IllegalArgumentException("file cannot be this");
        synchronized (files) {
            addImpl(file);
        }
        return this;
    }

    @NotNull
    @Override
    public SimpleVFile addAll(@NotNull Iterable<? extends SimpleVFile> c) {
        synchronized (files) {
            for (@NotNull SimpleVFile f : c) {
                addImpl(f);
            }
        }
        return this;
    }

    @Override
    public void remove(@Nullable SimpleVFile file) {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        if (file == this) throw new IllegalArgumentException("file cannot be this");
        synchronized (files) {
            removeImpl(file);
        }
    }

    @Override
    public void removeAll(@NotNull Iterable<? extends SimpleVFile> files) {
        synchronized (this.files) {
            for (@NotNull SimpleVFile file : files) {
                removeImpl(file);
            }
        }
    }

    private void addImpl(@NotNull SimpleVFile file) {
        if (!files.containsValue(file)) {
            if (!file.setParent(this)) {
                files.put(file.getName(), file);
            }
        }
    }

    /**
     * Convenience method
     *
     * @param dir the directory to extract to
     * @throws IOException
     */
    public void extract(File dir) throws IOException {
        @NotNull File out = new File(dir, getName());
        if (isDirectory()) {
            out.mkdir();
            for (@NotNull SimpleVFile f : list()) {
                f.extract(out);
            }
        } else {
            out.createNewFile();
            @NotNull InputStream is = new BufferedInputStream(openStream());
            try (@NotNull BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                @NotNull byte[] buf = new byte[1024 * 8]; // 8K read buffer
                for (int read; (read = is.read(buf)) > -1; ) {
                    os.write(buf, 0, read);
                }
            }
        }
    }

    @Override
    public void fileAdded(SimpleVFile f) {
        for (@NotNull FileChangeListener listener : listeners) {
            listener.fileAdded(f);
        }
    }

    @Override
    public void fileModified(SimpleVFile f) {
        for (@NotNull FileChangeListener listener : listeners) {
            listener.fileModified(f);
        }
    }

    @Override
    public void fileRemoved(SimpleVFile f) {
        for (@NotNull FileChangeListener listener : listeners) {
            listener.fileRemoved(f);
        }
    }

    /**
     * Convenience method to find files recursively by name
     *
     * @param search
     * @return
     */
    @Nullable
    public List<SimpleVFile> find(String search) {
        return find(search, this);
    }

    @NotNull
    private List<SimpleVFile> find(String search, @NotNull SimpleVFile root) {
        search = search.toLowerCase();
        @NotNull List<SimpleVFile> list = new LinkedList<>();
        for (@NotNull SimpleVFile e : root.list()) {
            @NotNull String str = e.getName().toLowerCase();
            if (str.contains(search)) {
                list.add(e);
            }
            if (e.isDirectory()) {
                list.addAll(find(search, e));
            }
        }
        return list;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        if (isDirectory()) {
            @Nullable Icon i = null;
            if (parent == null) {
                i = UIManager.getIcon("FileView.hardDriveIcon");
            }
            if (i == null) {
                i = UIManager.getIcon("FileView.directoryIcon");
            }
            return i;
        } else {
            return UIManager.getIcon("FileView.fileIcon");
        }
    }

    public String group() {
        return DEFAULT_GROUP;
    }

    public String owner() {
        return DEFAULT_OWNER;
    }

    private void removeImpl(@NotNull SimpleVFile file) {
        SimpleVFile removed = files.remove(file.getName());
        if (removed != null) {
            removed.setParent(null);
        }
    }

    @NotNull
    @Override
    public String toString() {
        return getName();
    }

    public static interface MissingFileHandler {

        @Nullable
        SimpleVFile handle(SimpleVFile parent, String name);
    }
}
