package com.timepath.vfs;

import java.io.InputStream;
import java.util.logging.Logger;

public class VFSStub extends SimpleVFile {

    private static final Logger LOG = Logger.getLogger(VFSStub.class.getName());

    private String name;

    public VFSStub() {
        this("");
    }

    public VFSStub(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
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

    @Override
    public InputStream stream() {
        return null;
    }

}
