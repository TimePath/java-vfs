package com.timepath.vfs.fuse;

import com.timepath.vfs.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.fusejna.*;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterFull;

public class FUSEFS extends VFSStub implements Runnable {

    private static final Logger LOG = Logger.getLogger(FUSEFS.class.getName());

    public static void main(String[] args) {
        FUSEFS f = new FUSEFS("test");
        f.add(new MockFile("folder")
            .add(new MockFile("test.txt", "It works!\n"))
        );
        f.add(new MockFile("test.txt", "It works!\n"));
        f.add(new MockFile("world.txt", "Hello world\n"));
        f.run();
    }

    private final FuseFilesystemAdapterFull fuse;

    private String mountpoint;

    public FUSEFS(File mountpoint) {
        this(mountpoint.getPath());
    }

    public FUSEFS(String mountpoint) {
        fuse = new FuseFilesystemAdapterFull() {

            @Override
            public int getattr(String path, StatWrapper stat) {
                VFile<?> f = FUSEFS.this.get(path);
                if(f == null) {
                    return -ErrorCodes.ENOENT();
                }
                if(f.isDirectory()) {
                    stat.setMode(NodeType.DIRECTORY);
                } else {
                    stat.setMode(NodeType.FILE);
                    stat.size(Math.max(f.length(), 0));
                }
                return 0;
            }

            @Override
            public int read(String path, ByteBuffer buffer, long size, long offset, FileInfoWrapper info) {
                VFile<?> f = FUSEFS.this.get(path);
                if(f != null) {
                    InputStream stream = f.stream();
                    byte[] buf;
                    try {
                        stream.skip(offset);
                        final int s = (int) Math.max(Math.min(size, stream.available()), 0);
                        buf = new byte[s];
                        stream.read(buf);
                        buffer.put(buf);
                        return buf.length;
                    } catch(IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
                return -1;
            }

            @Override
            public int readdir(String path, DirectoryFiller filler) {
                VFile<?> f = FUSEFS.this.get(path);
                if(f == null) {
                    return -ErrorCodes.ENOENT();
                }
                for(VFile<?> vf : f.list()) {
                    filler.add(path + SEPARATOR + vf.getName());
                }
                return 0;
            }

        };
        this.mountpoint = mountpoint;
    }

    public void run() {
        try {
            LOG.log(Level.INFO, "Mounted on {0}", mountpoint);
            fuse.mount(mountpoint);
        } catch(FuseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            LOG.log(Level.INFO, "Unmounted");
        }
    }

}
