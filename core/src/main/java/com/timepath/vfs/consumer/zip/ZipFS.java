package com.timepath.vfs.consumer.zip;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFSStub;
import com.timepath.vfs.VFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFS extends VFSStub {

    private static final Logger LOG = Logger.getLogger(ZipFS.class.getName());
    private static final Pattern DIRECTORYSPLIT = Pattern.compile(VFile.SEPARATOR);

    public ZipFS(@NotNull byte[] data) throws IOException {
        @NotNull BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
        bis.mark(data.length);
        @NotNull ZipInputStream zs = new ZipInputStream(bis);
        @NotNull byte[] buffer = new byte[2048];
        for (ZipEntry e; (e = zs.getNextEntry()) != null; ) {
            LOG.log(Level.FINE, "{0}", e);
            @NotNull ByteArrayOutputStream baos = new ByteArrayOutputStream((int) Math.max(e.getCompressedSize(), 0));
            for (int len; (len = zs.read(buffer)) > 0; ) {
                baos.write(buffer, 0, len);
            }
            String[] split = DIRECTORYSPLIT.split(e.getName());
            @Nullable SimpleVFile dir = this;
            // create transient directories
            for (int i = 0; i < (split.length - 1); i++) {
                String dirName = split[i];
                @Nullable SimpleVFile sub = dir.get(dirName);
                if (sub == null) {
                    sub = new MockFile(dirName);
                    dir.add(sub);
                }
                dir = sub;
            }
            dir.add(new ZipFSEntry(e, baos.toByteArray()));
        }
    }

    private static class ZipFSEntry extends SimpleVFile {

        private final byte[] data;
        private final ZipEntry entry;

        ZipFSEntry(ZipEntry e, byte... data) {
            entry = e;
            this.data = data;
        }

        @NotNull
        @Override
        public String getName() {
            String name = entry.getName();
            return name.substring(name.lastIndexOf('/') + 1);
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
}
