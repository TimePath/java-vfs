package com.timepath.vfs;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFS extends VFSStub {

    private static final Logger LOG = Logger.getLogger(ZipFS.class.getName());

    public ZipFS(byte[] data) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
        bis.mark(data.length);
        ZipInputStream zs = new ZipInputStream(bis);
        byte[] buffer = new byte[2048];

        for(ZipEntry e; (e = zs.getNextEntry()) != null;) {
            LOG.log(Level.FINE, "{0}", e);
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) Math.max(e.getCompressedSize(), 0));
            for(int len; (len = zs.read(buffer)) > 0;) {
                baos.write(buffer, 0, len);
            }
            String[] split = e.getName().split(VFile.SEPARATOR);
            SimpleVFile dir = this;
            String dirName;
            for(int i = 0; i < split.length - 1; i++) {
                dirName = split[i];
                SimpleVFile sub = dir.get(dirName);
                if(sub == null) {
                    sub = new MockFile(dirName);
                    dir.add(sub);
                }
                dir = sub;
            }
            dir.add(new ZipFSEntry(e, baos.toByteArray()));
        }
    }

    private class ZipFSEntry extends SimpleVFile {

        private final byte[] data;

        private final ZipEntry entry;

        ZipFSEntry(ZipEntry e, byte[] data) {
            this.entry = e;
            this.data = data;
        }

        public String getName() {
            return entry.getName();
        }

        @Override
        public boolean isDirectory() {
            return entry.isDirectory();
        }

        public InputStream stream() {
            return new ByteArrayInputStream(data);
        }

    }

}
