package com.timepath.vfs.provider.local;

import com.timepath.vfs.provider.ExtendedVFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class LocalFile extends ExtendedVFile {

    private static final Logger LOG = Logger.getLogger(LocalFile.class.getName());

    @NotNull
    protected final File file;

    LocalFile(@NotNull File file) {
        this.file = file;
    }

    @Nullable
    @Override
    public Object getAttributes() {
        return null;
    }

    @NotNull
    @Override
    public ExtendedVFile getRoot() {
        return this;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @NotNull
    @Override
    public String getName() {
        return file.getName();
    }

    @Nullable
    @Override
    public InputStream openStream() {
        try {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public long length() {
        return file.length();
    }

}
