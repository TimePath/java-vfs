package com.timepath.vfs.provider.zip;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.ProviderStub;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileProvider extends ProviderStub {

    private static final Logger LOG = Logger.getLogger(ZipFileProvider.class.getName());
    private static final int BUFFER_SIZE = 2048;

    public ZipFileProvider(@NotNull byte[] data) throws IOException {
        @NotNull BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
        bis.mark(data.length);
        try (ZipInputStream zs = new ZipInputStream(bis)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (ZipEntry e; (e = zs.getNextEntry()) != null; ) {
                LOG.log(Level.FINE, "{0}", e);
                long sizeLong = e.getSize();
                if (sizeLong == -1) sizeLong = e.getCompressedSize();
                if (sizeLong == -1) sizeLong = BUFFER_SIZE;
                if(sizeLong > Integer.MAX_VALUE) {
                    LOG.log(Level.SEVERE, "ZipEntry exceeds sizeof int");
                    continue;
                }
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream((int) sizeLong)) {
                    for (int len; (len = zs.read(buffer)) > 0; ) {
                        baos.write(buffer, 0, len);
                    }
                    atPath(e.getName()).add(new ZipFile(e, baos.toByteArray()));
                }
            }
        }
    }

    @NotNull
    private SimpleVFile atPath(CharSequence path) {
        String[] split = SEPARATOR_PATTERN.split(path);
        SimpleVFile dir = this;
        for (int i = 0; i < (split.length - 1); i++) {
            String dirName = split[i];
            SimpleVFile sub = dir.get(dirName);
            // Create transient directories
            if (sub == null) {
                sub = new MockFile(dirName);
                dir.add(sub);
            }
            dir = sub;
        }
        return dir;
    }

}
