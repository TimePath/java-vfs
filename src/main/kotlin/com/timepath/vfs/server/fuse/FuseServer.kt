package com.timepath.vfs.server.fuse

import com.timepath.vfs.VFile
import com.timepath.vfs.provider.ProviderStub
import net.fusejna.DirectoryFiller
import net.fusejna.ErrorCodes
import net.fusejna.FuseException
import net.fusejna.StructFuseFileInfo.FileInfoWrapper
import net.fusejna.StructStat.StatWrapper
import net.fusejna.types.TypeMode.NodeType
import net.fusejna.util.FuseFilesystemAdapterFull
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger


public class FuseServer(private val mountpoint: File) : ProviderStub(), Runnable {
    private val fuse: FuseFilesystemAdapterFull

    init {
        fuse = object : FuseFilesystemAdapterFull() {
            override fun getattr(path: String, stat: StatWrapper): Int {
                val file = query(path)
                if (file == null) {
                    return -ErrorCodes.ENOENT()
                }
                if (file.isDirectory) {
                    stat.setMode(NodeType.DIRECTORY)
                } else {
                    stat.setMode(NodeType.FILE)
                    stat.size(Math.max(file.length, 0))
                }
                return 0
            }

            override fun read(path: String, buffer: ByteBuffer, size: Long, offset: Long, info: FileInfoWrapper?): Int {
                query(path)?.let {
                    val stream = it.openStream()!!
                    try {
                        stream.skip(offset)
                        val buf = ByteArray(Math.max(Math.min(size, stream.available().toLong()), 0).toInt())
                        stream.read(buf)
                        buffer.put(buf)
                        return buf.size()
                    } catch (ex: IOException) {
                        LOG.log(Level.SEVERE, null, ex)
                    }
                }
                return -1
            }

            override fun readdir(path: String, filler: DirectoryFiller): Int {
                val file = query(path)
                if (file == null) {
                    return -ErrorCodes.ENOENT()
                }
                for (vf in file.list()) {
                    filler.add("$path${VFile.SEPARATOR}${vf.name}")
                }
                return 0
            }
        }
    }

    override fun run() {
        try {
            LOG.log(Level.INFO, "Mounted on ${mountpoint.getAbsolutePath()}")
            fuse.mount(mountpoint)
        } catch (ex: FuseException) {
            LOG.log(Level.SEVERE, null, ex)
        } finally {
            LOG.log(Level.INFO, "Unmounted")
        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<FuseServer>().getName())
    }
}
