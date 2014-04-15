package com.timepath.vfs;

import com.timepath.io.utils.ViewableData;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 *
 * @author TimePath
 */
public abstract class SimpleVFile implements VFile<SimpleVFile>, ViewableData {

    private static final String DEFAULT_OWNER = System.getProperty("user.name", "nobody");

    private static final String DEFAULT_GROUP = "nobody";

    private static final Logger LOG = Logger.getLogger(SimpleVFile.class.getName());

    //<editor-fold defaultstate="collapsed" desc="Listener">
    public ArrayList<FileChangeListener> listeners = new ArrayList<FileChangeListener>();

    public void addFileChangeListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    public void copy(SimpleVFile file) {
        add(file, false);
    }

    public void copyFrom(Collection<? extends SimpleVFile> files) {
        addAll(files, false);
    }

    public static abstract class FileChangeListener {

        public abstract void fileAdded(SimpleVFile f);

        public abstract void fileModified(SimpleVFile f);

        public abstract void fileRemoved(SimpleVFile f);

    }
    //</editor-fold>

    public ArrayList<SimpleVFile> find(String search) {
        return find(search, this);
    }

    public ArrayList<SimpleVFile> find(String search, SimpleVFile root) {
        search = search.toLowerCase();
        ArrayList<SimpleVFile> list = new ArrayList<SimpleVFile>();
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

    public static final String sep = "/";

    public HashMap<String, SimpleVFile> files = new HashMap<String, SimpleVFile>();

    public void add(SimpleVFile f) {
        add(f, true);
    }

    public void add(SimpleVFile f, boolean move) {
        if(f == null || f == this) {
            return;
        }
        if(files.containsValue(f)) {
            return;
        }
        files.put(f.getName(), f);
        f.setParent(this, move);
    }

    public void addAll(Collection<? extends SimpleVFile> c) {
        addAll(c, true);
    }

    public void addAll(Collection<? extends SimpleVFile> c, boolean move) {
        for(SimpleVFile f : c) {
            add(f, move);
        }
    }

    public Icon getIcon() {
        if(isDirectory()) {
            Icon i = null;
            if(this.getParent() == null) {
                UIManager.getIcon("FileView.hardDriveIcon");
            }
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

    public Collection<SimpleVFile> children() {
        return files.values();
    }

    public void extract(File dir) throws IOException {
        File out = new File(dir, this.getName());
        if(this.isDirectory()) {
            out.mkdir();
            for(SimpleVFile f : this.children()) {
                f.extract(out);
            }
        } else {
            out.createNewFile();
            InputStream is = stream();
            FileOutputStream fos = new FileOutputStream(out);
            BufferedOutputStream os = new BufferedOutputStream(fos);
            byte[] buf = new byte[1024 * 8]; // 8K read buffer
            int read;
            while((read = is.read(buf)) > -1) {
                os.write(buf, 0, read);
            }
            os.flush();
            os.close();
        }
    }

    protected SimpleVFile parent;

    public void remove(SimpleVFile f) {
        if(f == null || f == this) {
            return;
        }
        for(Entry<String, SimpleVFile> e : files.entrySet()) {
            if(e.getValue() == f) {
                files.remove(e.getKey());
                f.setParent(null);
            }
        }
    }

    public void removeAll(Collection<? extends SimpleVFile> c) {
        for(SimpleVFile f : c) {
            remove(f);
        }
    }

    public void setParent(SimpleVFile newParent) {
        setParent(newParent, true);
    }

    public void setParent(SimpleVFile newParent, boolean move) {
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

    public SimpleVFile get(String name) {
        String[] split = name.split(sep);
        SimpleVFile f = this;
        for(String s : split) {
            if(s.length() == 0) {
                continue;
            }
            f = f.files.get(s);
            if(f == null) {
                return null;
            }
        }
        return f;
    }

    public void remove(String name) {
        files.remove(name);
    }

    public Collection<? extends SimpleVFile> list() {
        return files.values();
    }

    public abstract boolean isDirectory();

    public String owner() {
        return DEFAULT_OWNER;
    }

    public String group() {
        return DEFAULT_GROUP;
    }

    private long length = -1;

    public long length() {
        if(length == -1) {
            length = lengthEstimate();
        }
        return length;
    }

    private long lengthEstimate() {
        try {
            return this.stream().available();
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch(NullPointerException ex) {

        }
        return -1;
    }

    protected long lastModified = System.currentTimeMillis();

    public long lastModified() {
        return lastModified;
    }

    public String getPath() {
        String path = (isDirectory() ? getName() : "").replaceAll("/", "");
        if(parent != null) {
            path = parent.getPath() + "/" + path;
        }
        return path;
    }

    public abstract String getName();

    public abstract InputStream stream();

    public boolean canExecute() {
        return false;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public boolean createNewFile() throws IOException {
        return false; // TODO: lazy node traversal
    }

    public boolean delete() {
        return false;
    }

    public boolean exists() {
        return true;
    }

    public SimpleVFile getParent() {
        return this.parent;
    }

    public long getTotalSpace() {
        return 0;
    }

    public long getUsableSpace() {
        return 0;
    }

    public boolean isFile() {
        return !isDirectory();
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
        lastModified = time;
        return true;
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

}
