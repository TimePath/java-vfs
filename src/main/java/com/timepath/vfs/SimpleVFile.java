package com.timepath.vfs;

import com.timepath.io.utils.ViewableData;

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

    private static final Logger LOG           = Logger.getLogger(SimpleVFile.class.getName());
    private static final String DEFAULT_GROUP = System.getProperty("user.name", "nobody");
    private static final String DEFAULT_OWNER = System.getProperty("user.name", "nobody");
    protected final Map<String, SimpleVFile>       files;
    private final   Collection<FileChangeListener> listeners;
    private         long                           length;
    private         long                           lastModified;
    private         SimpleVFile                    parent;

    protected SimpleVFile() {
        files = Collections.synchronizedMap(new HashMap<String, SimpleVFile>(0));
        listeners = new LinkedList<>();
        length = -1;
        lastModified = System.currentTimeMillis();
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

    @Override
    public SimpleVFile getParent() {
        return parent;
    }

    public void setParent(SimpleVFile newParent) {
        if(parent == newParent) {
            return;
        }
        if(parent != null) {
            parent.remove(this);
        }
        if(newParent != null) {
            newParent.add(this);
        }
        parent = newParent;
    }

    @Override
    public Collection<? extends SimpleVFile> list() {
        return files.values();
    }

    @Override
    public SimpleVFile get(String path) {
        if(path == null) throw new IllegalArgumentException("path cannot be null");
        // sanitize input
        Deque<String> stack = new LinkedList<>();
        for(String token : path.split(VFile.SEPARATOR)) {
            switch(token) {
                case "": // ignore repeated separator
                    break;
                case ".": // ignore current directory
                    break;
                case "..":
                    if(!stack.isEmpty()) { // ignore extra cdup's
                        stack.removeLast();
                    }
                    break;
                default:
                    stack.addLast(token);
                    break;
            }
        }
        LOG.log(Level.FINE, "Getting {0}", stack);
        SimpleVFile result = this;
        for(String token : stack) {
            result = result.get(token);
            if(result == null) {
                return null;
            }
        }
        return result;
    }

    @Override
    public String getPath() {
        String path = ( isDirectory() ? getName() : "" );
        path = path.replaceAll(VFile.SEPARATOR, ""); // just in case
        if(parent != null) {
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
        if(isDirectory()) {
            return files.size();
        }
        if(length == -1) {
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
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch(NullPointerException ignored) {
        }
        return -1;
    }

    @Override
    public SimpleVFile add(SimpleVFile file) {
        if(file == null) throw new IllegalArgumentException("file cannot be null");
        if(file == this) throw new IllegalArgumentException("file cannot be this");
        synchronized(files) {
            addImpl(file);
        }
        return this;
    }

    @Override
    public SimpleVFile addAll(Iterable<? extends SimpleVFile> c) {
        synchronized(files) {
            for(SimpleVFile f : c) {
                addImpl(f);
            }
        }
        return this;
    }

    private void addImpl(SimpleVFile file) {
        if(!files.containsValue(file)) {
            files.put(file.getName(), file);
        }
    }

    @Override
    public void remove(SimpleVFile file) {
        if(file == null) throw new IllegalArgumentException("file cannot be null");
        if(file == this) throw new IllegalArgumentException("file cannot be this");
        synchronized(files) {
            removeImpl(file);
        }
    }

    @Override
    public void removeAll(Iterable<? extends SimpleVFile> files) {
        synchronized(this.files) {
            for(SimpleVFile file : files) {
                removeImpl(file);
            }
        }
    }

    /**
     * Convenience method
     *
     * @param dir
     *         the directory to extract to
     *
     * @throws IOException
     */
    public void extract(File dir) throws IOException {
        File out = new File(dir, getName());
        if(isDirectory()) {
            out.mkdir();
            for(SimpleVFile f : list()) {
                f.extract(out);
            }
        } else {
            out.createNewFile();
            InputStream is = new BufferedInputStream(openStream());
            try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(out))) {
                byte[] buf = new byte[1024 * 8]; // 8K read buffer
                for(int read; ( read = is.read(buf) ) > -1; ) {
                    os.write(buf, 0, read);
                }
            }
        }
    }

    @Override
    public void fileAdded(SimpleVFile f) {
        for(FileChangeListener listener : listeners) {
            listener.fileAdded(f);
        }
    }

    @Override
    public void fileModified(SimpleVFile f) {
        for(FileChangeListener listener : listeners) {
            listener.fileModified(f);
        }
    }

    @Override
    public void fileRemoved(SimpleVFile f) {
        for(FileChangeListener listener : listeners) {
            listener.fileRemoved(f);
        }
    }

    /**
     * Convenience method to find files recursively by name
     *
     * @param search
     *
     * @return
     */
    public List<SimpleVFile> find(String search) {
        return find(search, this);
    }

    private List<SimpleVFile> find(String search, SimpleVFile root) {
        search = search.toLowerCase();
        List<SimpleVFile> list = new LinkedList<>();
        for(SimpleVFile e : root.list()) {
            String str = e.getName().toLowerCase();
            if(str.contains(search)) {
                list.add(e);
            }
            if(e.isDirectory()) {
                list.addAll(find(search, e));
            }
        }
        return list;
    }

    @Override
    public Icon getIcon() {
        if(isDirectory()) {
            Icon i = null;
            if(parent == null) {
                i = UIManager.getIcon("FileView.hardDriveIcon");
            }
            if(i == null) {
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

    private void removeImpl(SimpleVFile file) {
        SimpleVFile removed = files.remove(file);
        if(removed != null) {
            removed.setParent(null);
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
