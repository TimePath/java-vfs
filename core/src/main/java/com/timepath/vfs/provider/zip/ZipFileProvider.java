package com.timepath.vfs.provider.zip;

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

public class ZipFileProvider extends VFSStub {

    private static final Logger LOG = Logger.getLogger(ZipFileProvider.class.getName());
    private static final Pattern DIRECTORYSPLIT = Pattern.compile(VFile.SEPARATOR);

    public ZipFileProvider(@NotNull byte[] data) throws IOException {
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
            dir.add(new ZipFile(e, baos.toByteArray()));
        }
    }

}
