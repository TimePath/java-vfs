package com.timepath.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 *
 * @author TimePath
 * @param <V> Specific type of VFile
 */
public interface VFile<V extends VFile<?>> {
    
    public static final String SEPARATOR = "/";

    public boolean canExecute();

    public boolean canRead();

    public boolean canWrite();

    public boolean createNewFile() throws IOException;

    public boolean delete();

    public boolean exists();

    public String getName();

    public V getParent();
    
    public Collection<? extends V> list();
    
    public V get(String path);

    public String getPath();

    public long getTotalSpace();

    public long getUsableSpace();

    public boolean isDirectory();

    public boolean isFile();

    public long lastModified();

    public long length();

    public boolean renameTo(V dest);

    public boolean setExecutable(boolean executable);

    public boolean setExecutable(boolean executable, boolean ownerOnly);

    public boolean setLastModified(long time);

    public boolean setReadable(boolean readable);

    public boolean setReadable(boolean readable, boolean ownerOnly);

    public boolean setWritable(boolean writable);

    public boolean setWritable(boolean writable, boolean ownerOnly);
    
    public InputStream stream();

}
