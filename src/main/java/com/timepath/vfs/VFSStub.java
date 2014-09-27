package com.timepath.vfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.logging.Logger;

public class VFSStub extends SimpleVFile {

    private static final Logger LOG = Logger.getLogger(VFSStub.class.getName());
    @Nullable
    public String name;

    protected VFSStub() {
        this(null);
    }

    protected VFSStub(@Nullable String name) {
        if (name == null) {
            name = toString();
        }
        this.name = name;
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public InputStream openStream() {
        return null;
    }

    @NotNull
    @Override
    public String getPath() {
        return "";
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public long lastModified() {
        return System.currentTimeMillis();
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public String owner() {
        return System.getProperty("user.name", "nobody");
    }
}
