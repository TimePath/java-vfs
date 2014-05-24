package com.timepath.vfs;

import com.timepath.io.utils.ViewableData;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public abstract class SimpleVFile implements VFile<SimpleVFile>, ViewableData, FileChangeListener {

    private static final Logger                         LOG           = Logger.getLogger(SimpleVFile.class.getName());
    private static final String                         DEFAULT_GROUP = System.getProperty("user.name", "nobody");
    private static final String                         DEFAULT_OWNER = System.getProperty("user.name", "nobody");
    protected final      Map<String, SimpleVFile>       files         = Collections.synchronizedMap(new HashMap<String, SimpleVFile>(0));
    private final        Collection<FileChangeListener> listeners     = new LinkedList<>();
    private              long                           length        = -1;
    private              long                           lastModified  = System.currentTimeMillis();
    private SimpleVFile parent;

    protected SimpleVFile() {}

    public SimpleVFile add(SimpleVFile f) {
        add(f, true);
        return this;
    }

    SimpleVFile add(SimpleVFile f, boolean move) {
        if(( f != null ) && ( f != this )) {
            synchronized(files) {
                if(!files.containsValue(f)) {
                    files.put(f.getName(), f);
                }
            }
        }
        return this;
    }

    public SimpleVFile addAll(Iterable<? extends SimpleVFile> c) {
        addAll(c, true);
        return this;
    }

    SimpleVFile addAll(Iterable<? extends SimpleVFile> c, boolean move) {
        for(SimpleVFile f : c) {
            add(f, move);
        }
        return this;
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
        setParent(newParent, true);
    }

    @Override
    public Collection<? extends SimpleVFile> list() {
        return files.values();
    }

    @Override
    public SimpleVFile get(String path) {
        String[] split = path.split(VFile.SEPARATOR);
        if(split.length == 1) {
            return files.get(path);
        }
        Deque<String> stack = new LinkedList<>();
        for(String token : split) {
            if(!token.isEmpty()) {
                if("..".equals(token)) {
                    if(!stack.isEmpty()) {
                        stack.removeLast();
                    }
                } else {
                    stack.addLast(token);
                }
            }
        }
        LOG.log(Level.FINE, "Getting {0}", stack);
        SimpleVFile get = this;
        for(String token : stack) {
            get = get.get(token);
            if(get == null) {
                return null;
            }
        }
        return get;
    }

    @Override
    public String getPath() {
        String path = ( isDirectory() ? getName() : "" ).replaceAll(VFile.SEPARATOR, "");
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
            return stream().available();
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch(NullPointerException ignored) {
        }
        return -1;
    }

    public Collection<SimpleVFile> children() {
        return files.values();
    }

    public void copy(SimpleVFile file) {
        add(file, false);
    }

    public void copyFrom(Iterable<? extends SimpleVFile> files) {
        addAll(files, false);
    }

    public void extract(File dir) throws IOException {
        File out = new File(dir, getName());
        if(isDirectory()) {
            out.mkdir();
            for(SimpleVFile f : children()) {
                f.extract(out);
            }
        } else {
            out.createNewFile();
            InputStream is = stream();
            FileOutputStream fos = new FileOutputStream(out);
            BufferedOutputStream os = new BufferedOutputStream(fos);
            byte[] buf = new byte[1024 * 8]; // 8K read buffer
            int read;
            while(( read = is.read(buf) ) > -1) {
                os.write(buf, 0, read);
            }
            os.flush();
            os.close();
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

    public List<SimpleVFile> find(String search) {
        return find(search, this);
    }

    public List<SimpleVFile> find(String search, SimpleVFile root) {
        search = search.toLowerCase();
        List<SimpleVFile> list = new LinkedList<>();
        for(SimpleVFile e : root.children()) {
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
            if(parent == null) {
                UIManager.getIcon("FileView.hardDriveIcon");
            }
            Icon i = null;
            if(i == null) {
                i = UIManager.getIcon("FileView.directoryIcon");
            }
            return i;
            //        } else if(!isComplete()) {
            //            return UIManager.getIcon("html.missingImage");
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

    public void remove(SimpleVFile f) {
        if(( f == null ) || ( f == this )) {
            return;
        }
        Set<Entry<String, SimpleVFile>> entries = files.entrySet();
        synchronized(files) {
            for(Map.Entry<String, SimpleVFile> e : entries) {
                if(e.getValue() == f) {
                    files.remove(e.getKey());
                    f.setParent(null);
                }
            }
        }
    }

    public void removeAll(Iterable<? extends SimpleVFile> c) {
        for(SimpleVFile f : c) {
            remove(f);
        }
    }

    void setParent(SimpleVFile newParent, boolean move) {
        if(parent == newParent) {
            return;
        }
        if(move) {
            if(parent != null) {
                parent.remove(this);
            }
            if(newParent != null) {
                newParent.add(this);
            }
        }
        parent = newParent;
    }

    @Override
    public String toString() {
        return getName();
    }
}
