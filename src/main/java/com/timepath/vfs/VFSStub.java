package com.timepath.vfs;

import java.io.InputStream;
import java.util.logging.Logger;

public class VFSStub extends SimpleVFile {

    private static final Logger LOG = Logger.getLogger(VFSStub.class.getName());
    public String name;

    protected VFSStub() {
        this(null);
    }

    protected VFSStub(String name) {
        if (name == null) {
            name = toString();
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream openStream() {
        return null;
    }

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
