package com.timepath.vfs;

import com.timepath.io.utils.ViewableData;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public abstract class SimpleVFile implements VFile<SimpleVFile>, ViewableData, FileChangeListener {

    private static final Logger                         LOG           = Logger.getLogger(SimpleVFile.class.getName());
    private static final String                         DEFAULT_GROUP = System.getProperty("user.name", "nobody");
    private static final String                         DEFAULT_OWNER = System.getProperty("user.name", "nobody");
    protected final      Map<String, SimpleVFile>       files         = new HashMap<>(0);
    private final        Collection<FileChangeListener> listeners     = new LinkedList<>();
    private              long                           length        = -1;
    private              long                           lastModified  = System.currentTimeMillis();
    private SimpleVFile parent;

    public SimpleVFile add(SimpleVFile f) {
        add(f, true); return this;
    }

    SimpleVFile add(SimpleVFile f, boolean move) {
        if(f != null && f != this) {
            if(!files.containsValue(f)) {
                files.put(f.getName(), f); f.setParent(this, move);
            }
        } return this;
    }

    public SimpleVFile addAll(Iterable<? extends SimpleVFile> c) {
        addAll(c, true); return this;
    }

    SimpleVFile addAll(Iterable<? extends SimpleVFile> c, boolean move) {
        for(SimpleVFile f : c) {
            add(f, move);
        } return this;
    }

    public void addFileChangeListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    public boolean canExecute() {
        return false;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public boolean createNewFile() {
        return false; // TODO: lazy node traversal
    }

    public boolean delete() {
        return false;
    }

    public boolean exists() {
        return true;
    }

    public SimpleVFile getParent() {
        return parent;
    }

    public void setParent(SimpleVFile newParent) {
        setParent(newParent, true);
    }

    public Collection<? extends SimpleVFile> list() {
        return files.values();
    }

    public SimpleVFile get(String path) {
        String[] split = path.split(VFile.SEPARATOR); if(split.length == 1) {
            return files.get(path);
        } Deque<String> stack = new LinkedList<>(); for(String token : split) {
            if(!token.isEmpty()) {
                if("..".equals(token)) {
                    if(!stack.isEmpty()) {
                        stack.removeLast();
                    }
                } else {
                    stack.addLast(token);
                }
            }
        } LOG.log(Level.FINE, "Getting {0}", stack); SimpleVFile get = this; for(String token : stack) {
            get = get.get(token); if(get == null) {
                return null;
            }
        } return get;
    }

    public String getPath() {
        String path = ( isDirectory() ? getName() : "" ).replaceAll(VFile.SEPARATOR, ""); if(parent != null) {
            path = parent.getPath() + VFile.SEPARATOR + path;
        } return path;
    }

    public long getTotalSpace() {
        return 0;
    }

    public long getUsableSpace() {
        return 0;
    }

    public abstract boolean isDirectory();

    public boolean isFile() {
        return !isDirectory();
    }

    public long lastModified() {
        return lastModified;
    }

    public long length() {
        if(isDirectory()) {
            return files.size();
        } if(length == -1) {
            length = lengthEstimate();
        } return length;
    }

    public boolean renameTo(SimpleVFile dest) {
        return false;
    }

    public boolean setExecutable(boolean executable) {
        return false;
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    public boolean setLastModified(long time) {
        lastModified = time; return true;
    }

    public boolean setReadable(boolean readable) {
        return false;
    }

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    public boolean setWritable(boolean writable) {
        return false;
    }

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    private long lengthEstimate() {
        try {
            return stream().available();
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch(NullPointerException ex) {
        } return -1;
    }

    public Iterable<SimpleVFile> children() {
        return files.values();
    }

    public void copy(SimpleVFile file) {
        add(file, false);
    }

    public void copyFrom(Iterable<? extends SimpleVFile> files) {
        addAll(files, false);
    }

    public void extract(File dir) throws IOException {
        File out = new File(dir, getName()); if(isDirectory()) {
            out.mkdir(); for(SimpleVFile f : children()) {
                f.extract(out);
            }
        } else {
            out.createNewFile(); InputStream is = stream(); FileOutputStream fos = new FileOutputStream(out);
            BufferedOutputStream os = new BufferedOutputStream(fos); byte[] buf = new byte[1024 * 8]; // 8K read buffer
            int read; while(( read = is.read(buf) ) > -1) {
                os.write(buf, 0, read);
            } os.flush(); os.close();
        }
    }

    public void fileAdded(SimpleVFile f) {
        for(FileChangeListener listener : listeners) {
            listener.fileAdded(f);
        }
    }

    public void fileModified(SimpleVFile f) {
        for(FileChangeListener listener : listeners) {
            listener.fileModified(f);
        }
    }

    public void fileRemoved(SimpleVFile f) {
        for(FileChangeListener listener : listeners) {
            listener.fileRemoved(f);
        }
    }

    public List<SimpleVFile> find(String search) {
        return find(search, this);
    }

    public List<SimpleVFile> find(String search, SimpleVFile root) {
        search = search.toLowerCase(); List<SimpleVFile> list = new LinkedList<>();
        for(SimpleVFile e : root.children()) {
            String str = e.getName().toLowerCase(); if(str.contains(search)) {
                list.add(e);
            } if(e.isDirectory()) {
                list.addAll(find(search, e));
            }
        } return list;
    }

    public Icon getIcon() {
        if(isDirectory()) {
            Icon i = null; if(getParent() == null) {
                UIManager.getIcon("FileView.hardDriveIcon");
            } if(i == null) {
                i = UIManager.getIcon("FileView.directoryIcon");
            } return i;
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
        if(f == null || f == this) {
            return;
        } for(Map.Entry<String, SimpleVFile> e : files.entrySet()) {
            if(e.getValue() == f) {
                files.remove(e.getKey()); f.setParent(null);
            }
        }
    }

    public void remove(String name) {
        files.remove(name);
    }

    public void removeAll(Iterable<? extends SimpleVFile> c) {
        for(SimpleVFile f : c) {
            remove(f);
        }
    }

    void setParent(SimpleVFile newParent, boolean move) {
        if(parent == newParent) {
            return;
        } if(move) {
            if(parent != null) {
                parent.remove(this);
            } if(newParent != null) {
                newParent.add(this);
            }
        } parent = newParent;
    }
}
