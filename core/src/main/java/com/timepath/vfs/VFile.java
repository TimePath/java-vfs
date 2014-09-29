package com.timepath.vfs;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @param <V> Specific type of VFile
 * @author TimePath
 */
public interface VFile<V extends VFile<?>> {

    @NonNls
    String SEPARATOR = "/";

    Pattern SEPARATOR_PATTERN = Pattern.compile(SEPARATOR);

    boolean canExecute();

    boolean canRead();

    boolean canWrite();

    boolean createNewFile();

    boolean delete();

    boolean exists();

    /**
     * @return the file name without {@code SEPARATOR}s
     */
    @NotNull
    String getName();

    @Nullable
    V getParent();

    @NotNull
    Collection<? extends V> list();

    /**
     * Get a file by literal name
     *
     * @param name
     * @return the file, or null
     */
    @Nullable
    V get(@NonNls @NotNull String name);

    @Nullable
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

    /**
     * Open the file for reading. It is the caller's responsibility to close the stream
     *
     * @return the stream, or null
     */
    @Nullable
    InputStream openStream();

    /**
     * @return some identifier, may contain {@code SEPARATOR} unlike {@link #getName()}
     */
    @NotNull
    String toString();
}
