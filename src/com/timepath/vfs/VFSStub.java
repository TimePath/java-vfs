package com.timepath.vfs;

import java.io.InputStream;

/**
 *
 * @author TimePath
 */
public class VFSStub extends SimpleVFile {
    
    public VFSStub() {
        this("");
    }
    
    private String name;
    
    public VFSStub(String name) {
        this.name = name;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public String owner() {
        return "ftp";
    }

    @Override
    public String group() {
        return "ftp";
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public long lastModified() {
        return System.currentTimeMillis();
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream stream() {
        return null;
    }
    
}
