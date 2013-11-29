package com.timepath.vfs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 *
 * @author TimePath
 */
public class MockFile extends SimpleVFile {

    private String name, cont;

    public MockFile(String name, String cont) {
        this.name = name;
        this.cont = cont;
    }

    @Override
    public boolean isDirectory() {
        return cont == null;
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
        return cont != null ? cont.getBytes().length : this.files.size();
    }

    @Override
    public long lastModified() {
        return System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public InputStream stream() {
        return new ByteArrayInputStream(cont.getBytes());
    }

    @Override
    public String getPath() {
        return "";
    }

}
