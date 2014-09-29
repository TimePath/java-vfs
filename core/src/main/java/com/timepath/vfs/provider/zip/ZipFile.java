package com.timepath.vfs.provider.zip;

import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFile;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
* @author TimePath
*/
class ZipFile extends SimpleVFile {

    private final byte[] data;
    private final ZipEntry entry;

    ZipFile(ZipEntry e, byte[] data) {
        entry = e;
        this.data = data;
    }

    @NotNull
    @Override
    public String getName() {
        String name = entry.getName();
        return name.substring(name.lastIndexOf(VFile.SEPARATOR) + 1);
    }

    @NotNull
    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    @Override
    public long length() {
        return entry.getSize();
    }
}
