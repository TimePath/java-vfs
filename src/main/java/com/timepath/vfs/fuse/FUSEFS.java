package com.timepath.vfs.fuse;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.VFSStub;
import com.timepath.vfs.VFile;
import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseException;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterFull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FUSEFS extends VFSStub implements Runnable {

    private static final Logger LOG = Logger.getLogger(FUSEFS.class.getName());
    private final FuseFilesystemAdapterFull fuse;
    private final String mountpoint;

    private FUSEFS(File mountpoint) {
        this(mountpoint.getPath());
    }

    public FUSEFS(String mountpoint) {
        fuse = new FuseFilesystemAdapterFull() {
            @Override
            public int getattr(String path, StatWrapper stat) {
                VFile<?> file = query(path);
                if (file == null) {
                    return -ErrorCodes.ENOENT();
                }
                if (file.isDirectory()) {
                    stat.setMode(NodeType.DIRECTORY);
                } else {
                    stat.setMode(NodeType.FILE);
                    stat.size(Math.max(file.length(), 0));
                }
                return 0;
            }

            @Override
            public int read(String path, ByteBuffer buffer, long size, long offset, FileInfoWrapper info) {
                VFile<?> file = query(path);
                if (file != null) {
                    InputStream stream = file.openStream();
                    try {
                        stream.skip(offset);
                        byte[] buf = new byte[(int) Math.max(Math.min(size, stream.available()), 0)];
                        stream.read(buf);
                        buffer.put(buf);
                        return buf.length;
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
                return -1;
            }

            @Override
            public int readdir(String path, DirectoryFiller filler) {
                VFile<?> file = query(path);
                if (file == null) {
                    return -ErrorCodes.ENOENT();
                }
                for (VFile<?> vf : file.list()) {
                    filler.add(path + SEPARATOR + vf.getName());
                }
                return 0;
            }
        };
        this.mountpoint = mountpoint;
    }

    public static void main(String... args) {
        FUSEFS fusefs = new FUSEFS("test");
        fusefs.add(new MockFile("folder").add(new MockFile("test.txt", "It works!\n")));
        fusefs.add(new MockFile("test.txt", "It works!\n"));
        fusefs.add(new MockFile("world.txt", "Hello world\n"));
        fusefs.run();
    }

    @Override
    public void run() {
        try {
            LOG.log(Level.INFO, "Mounted on {0}", mountpoint);
            fuse.mount(mountpoint);
        } catch (FuseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            LOG.log(Level.INFO, "Unmounted");
        }
    }
}
