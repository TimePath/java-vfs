package com.timepath.vfs;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFS extends VFSStub {

    private static final Logger  LOG            = Logger.getLogger(ZipFS.class.getName());
    private static final Pattern DIRECTORYSPLIT = Pattern.compile(VFile.SEPARATOR);

    public ZipFS(byte[] data) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
        bis.mark(data.length);
        ZipInputStream zs = new ZipInputStream(bis);
        byte[] buffer = new byte[2048];
        for(ZipEntry e; ( e = zs.getNextEntry() ) != null; ) {
            LOG.log(Level.FINE, "{0}", e);
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) Math.max(e.getCompressedSize(), 0));
            for(int len; ( len = zs.read(buffer) ) > 0; ) {
                baos.write(buffer, 0, len);
            }
            String[] split = DIRECTORYSPLIT.split(e.getName());
            SimpleVFile dir = this;
            for(int i = 0; i < ( split.length - 1 ); i++) {
                String dirName = split[i];
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

    private static class ZipFSEntry extends SimpleVFile {

        private final byte[]   data;
        private final ZipEntry entry;

        ZipFSEntry(ZipEntry e, byte... data) {
            entry = e;
            this.data = data;
        }

        @Override
        public String getName() {
            return entry.getName();
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public boolean isDirectory() {
            return entry.isDirectory();
        }
    }
}
