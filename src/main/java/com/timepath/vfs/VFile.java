package com.timepath.vfs;

import java.io.InputStream;
import java.util.Collection;

/**
 * @param <V>
 *         Specific type of VFile
 *
 * @author TimePath
 */
public interface VFile<V extends VFile<?>> {

    String SEPARATOR = "/";

    boolean canExecute();

    boolean canRead();

    boolean canWrite();

    boolean createNewFile();

    boolean delete();

    boolean exists();

    /**
     * @return the file name without {@code SEPARATOR}s
     */
    String getName();

    V getParent();

    Collection<? extends V> list();

    /**
     * Get a file separated by {@code SEPARATOR}
     *
     * @param path
     *
     * @return the file, or null
     */
    V get(String path);

    String getPath();

    long getTotalSpace();

    long getUsableSpace();

    boolean isDirectory();

    boolean isFile();

    long lastModified();

    long length();

    boolean renameTo(V dest);

    boolean setExecutable(boolean executable);

    boolean setExecutable(boolean executable, boolean ownerOnly);

    boolean setLastModified(long time);

    boolean setReadable(boolean readable);

    boolean setReadable(boolean readable, boolean ownerOnly);

    boolean setWritable(boolean writable);

    boolean setWritable(boolean writable, boolean ownerOnly);

    InputStream openStream();

    /**
     * @return some identifier, may contain {@code SEPARATOR} unlike {@link #getName()}
     */
    String toString();
}
