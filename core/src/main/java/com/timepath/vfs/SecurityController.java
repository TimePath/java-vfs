package com.timepath.vfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collection;

/**
 * Represents the various actions which can be performed on files, and gives you a chance to override or extend their
 * implementation
 *
 * @author TimePath
 */
public abstract class SecurityController {

    /**
     * Called in response to {@link SimpleVFile#openStream()}
     *
     * @param file The requested file
     * @return Potentially modified {@code file}
     */
    @Nullable
    public InputStream openStream(@NotNull final SimpleVFile file) {
        return file.openStream();
    }

    /**
     * Called in response to {@link SimpleVFile#add(SimpleVFile)}
     *
     * @param parent The parent file
     * @param file   The file
     */
    public void add(@NotNull final SimpleVFile parent, final SimpleVFile file) {
        parent.add(file);
    }

    /**
     * Called in response to {@link SimpleVFile#list()}
     *
     * @param file The file
     * @return Potentially modified {@code file.list()}
     */
    @NotNull
    public Collection<? extends SimpleVFile> list(@NotNull final SimpleVFile file) {
        return file.list();
    }

    /**
     * Called in response to {@link SimpleVFile#get(String)}
     *
     * @param file The file
     * @return Potentially modified {@code file}
     */
    public SimpleVFile get(final SimpleVFile file) {
        return file;
    }
}
