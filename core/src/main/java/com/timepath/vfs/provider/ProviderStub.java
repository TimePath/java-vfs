package com.timepath.vfs.provider;

import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

public class ProviderStub extends SimpleVFile {

    @NonNls
    private static final String NOBODY = System.getProperty("user.name", "nobody");
    @NotNull
    protected String name;

    protected ProviderStub() {
        this(null);
    }

    protected ProviderStub(@NonNls @Nullable String name) {
        if (name == null) name = toString();
        this.name = name;
    }

    @NotNull
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
        return NOBODY;
    }
}
