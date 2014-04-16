package com.timepath.vfs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

public class MockFile extends SimpleVFile {

    private static final Logger LOG = Logger.getLogger(MockFile.class.getName());

    private String cont, name;

    public MockFile(String name) {
        this(name, null);
    }

    public MockFile(String name, String cont) {
        this.name = name;
        this.cont = cont;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDirectory() {
        return cont == null;
    }

    @Override
    public InputStream stream() {
        return new ByteArrayInputStream(cont.getBytes());
    }

}
