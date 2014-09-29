package com.timepath.vfs.server.fuse;

import com.timepath.vfs.VFSStub;
import com.timepath.vfs.VFile;
import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseException;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterFull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FuseServer extends VFSStub implements Runnable {

    private static final Logger LOG = Logger.getLogger(FuseServer.class.getName());
    @NotNull
    private final FuseFilesystemAdapterFull fuse;
    private final String mountpoint;

    private FuseServer(@NotNull File mountpoint) {
        this(mountpoint.getPath());
    }

    public FuseServer(String mountpoint) {
        fuse = new FuseFilesystemAdapterFull() {
            @Override
            public int getattr(String path, @NotNull StatWrapper stat) {
                @Nullable VFile<?> file = query(path);
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
            public int read(String path, @NotNull ByteBuffer buffer, long size, long offset, FileInfoWrapper info) {
                @Nullable VFile<?> file = query(path);
                if (file != null) {
                    InputStream stream = file.openStream();
                    try {
                        stream.skip(offset);
                        @NotNull byte[] buf = new byte[(int) Math.max(Math.min(size, stream.available()), 0)];
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
            public int readdir(String path, @NotNull DirectoryFiller filler) {
                @Nullable VFile<?> file = query(path);
                if (file == null) {
                    return -ErrorCodes.ENOENT();
                }
                for (@NotNull VFile<?> vf : file.list()) {
                    filler.add(path + SEPARATOR + vf.getName());
                }
                return 0;
            }
        };
        this.mountpoint = mountpoint;
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
