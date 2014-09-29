package com.timepath.vfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MockFile extends SimpleVFile {

    @NotNull
    private final String name;
    @Nullable
    private final byte[] bytes;

    public MockFile(@NotNull String name) {
        this(name, null);
    }

    public MockFile(@NotNull String name, @Nullable String cont) {
        this.name = name;
        this.bytes = cont != null ? cont.getBytes(StandardCharsets.UTF_8) : null;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("resource")
    @Nullable
    @Override
    public InputStream openStream() {
        return (bytes != null) ? new ByteArrayInputStream(bytes) : null;
    }

    @Override
    public boolean isDirectory() {
        return bytes == null;
    }
}
