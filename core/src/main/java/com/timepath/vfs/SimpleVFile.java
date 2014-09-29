package com.timepath.vfs;

import com.timepath.io.utils.ViewableData;
import com.timepath.util.concurrent.DaemonThreadFactory;
import com.timepath.vfs.provider.ProviderPlugin;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple {@link com.timepath.vfs.VFile} implementation using synchronized recursive {@link java.util.HashMap}s
 *
 * @author TimePath
 */
public abstract class SimpleVFile implements MutableVFile<SimpleVFile>, ViewableData, FileChangeListener {

    protected static final ExecutorService pool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 10,
            new DaemonThreadFactory()
    );
    private static final Logger LOG = Logger.getLogger(SimpleVFile.class.getName());
    @NonNls
    private static final String DEFAULT_GROUP = System.getProperty("user.name", "nobody");
    @NonNls
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
    @NotNull
    protected static List<FileHandler> handlers = new LinkedList<>();

    @SuppressWarnings("WhileLoopReplaceableByForEach")
    private static void locate() {
        Iterator<ProviderPlugin> it = ServiceLoader.load(ProviderPlugin.class).iterator();
        while (it.hasNext()) {
            try {
                handlers.add(it.next().register());
            } catch(ServiceConfigurationError e) {
                LOG.log(Level.WARNING, "Unable to load Plugin", e);
            }
        }
    }

    public void visit(@NotNull File dir, @NotNull FileVisitor v) {
        @Nullable File[] ls = dir.listFiles();
        if (ls == null) {
            return;
        }
        for (File f : ls) {
            v.visit(f, this);
        }
    }

    public static interface FileHandler {

        @Nullable
        Collection<? extends SimpleVFile> handle(File file) throws IOException;
    }

    public interface FileVisitor {

        void visit(File f, SimpleVFile parent);
    }

    static {
        locate();
    }

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
    public SimpleVFile get(@NotNull @NonNls String name) {
        if (".".equals(name)) return this;
        if ("..".equals(name)) return getParent();
        SimpleVFile file = files.get(name);
        if (file != null) return file;
        for (@NotNull MissingFileHandler h : missingFileHandlers) {
            SimpleVFile root = h.handle(this, name);
            if (root != null) return root;
        }
        return null;
    }

    @NotNull
    @Override
    public String getPath() {
        String path = (isDirectory() ? getName() : "");
        path = SEPARATOR_PATTERN.matcher(path).replaceAll(""); // Just in case
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
        try (InputStream is = openStream()) {
            if (is != null) return is.available();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, null, e);
        }
        return -1;
    }

    public boolean setParent(@Nullable SimpleVFile newParent) {
        @SuppressWarnings("ObjectEquality") boolean nop = newParent == parent;
        if (nop) return false;
        if (parent != null) parent.remove(this);
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
    public SimpleVFile query(@NotNull String path) {
        String[] split = SEPARATOR.split(path);
        if (split.length == 1) return get(path); // Fast path
        // Compute absolute canonical path
        Deque<String> stack = new LinkedList<>();
        for (@NotNull String token : split) {
            switch (token) {
                case "":
                    // Ignore repeated separator
                case ".":
                    // Ignore current directory
                    break;
                case "..":
                    // Ignore extra cdup's
                    if (!stack.isEmpty()) stack.removeLast();
                    break;
                default:
                    stack.addLast(token);
                    break;
            }
        }
        LOG.log(Level.FINE, "Getting {0}", stack);
        SimpleVFile result = this;
        for (@NotNull String token : stack) {
            result = result.get(token);
            if (result == null) break;
        }
        return result;
    }

    @NotNull
    @Override
    public SimpleVFile add(@NotNull SimpleVFile file) {
        if (file == this) throw new IllegalArgumentException("file cannot be this");
        synchronized (files) {
            addImpl(file);
        }
        return this;
    }

    @NotNull
    @Override
    public SimpleVFile addAll(@NotNull Iterable<? extends SimpleVFile> files) {
        synchronized (this.files) {
            for (@NotNull SimpleVFile file : files) {
                addImpl(file);
            }
        }
        return this;
    }

    @Override
    public void remove(@NotNull SimpleVFile file) {
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
     * @throws java.io.IOException
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
    public void fileAdded(@NotNull SimpleVFile file) {
        for (@NotNull FileChangeListener listener : listeners) {
            listener.fileAdded(file);
        }
    }

    @Override
    public void fileModified(@NotNull SimpleVFile file) {
        for (@NotNull FileChangeListener listener : listeners) {
            listener.fileModified(file);
        }
    }

    @Override
    public void fileRemoved(@NotNull SimpleVFile file) {
        for (@NotNull FileChangeListener listener : listeners) {
            listener.fileRemoved(file);
        }
    }

    /**
     * Convenience method to find files recursively by name
     *
     * @param search
     * @return
     */
    @NotNull
    public List<SimpleVFile> find(String search) {
        return find(search, this);
    }

    @NotNull
    private List<SimpleVFile> find(@NonNls String search, @NotNull SimpleVFile root) {
        search = search.toLowerCase();
        List<SimpleVFile> list = new LinkedList<>();
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
            Icon icon = null;
            if (parent == null) {
                icon = UIManager.getIcon("FileView.hardDriveIcon");
            }
            if (icon == null) {
                icon = UIManager.getIcon("FileView.directoryIcon");
            }
            return icon;
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
