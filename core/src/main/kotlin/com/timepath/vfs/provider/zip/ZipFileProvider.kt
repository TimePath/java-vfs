package com.timepath.vfs.provider.zip

import com.timepath.vfs.MockFile
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.VFile
import com.timepath.vfs.provider.ProviderStub
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

public class ZipFileProvider [throws(javaClass<IOException>())](data: ByteArray) : ProviderStub() {

    init {
        val bis = data.inputStream.buffered()
        bis.mark(data.size())
        bis.let { ZipInputStream(it) }.use {
            while (true) {
                val e: ZipEntry? = it.getNextEntry()
                if (e == null) break
                LOG.log(Level.FINE, "{0}", e)
                val sizeLong = run {
                    var sizeLong = e.getSize()
                    if (sizeLong == -1L) sizeLong = e.getCompressedSize()
                    sizeLong
                }
                if (sizeLong > Integer.MAX_VALUE) {
                    LOG.log(Level.SEVERE, "ZipEntry exceeds sizeof int")
                    continue
                }
                atPath(e.getName()).add(ZipFile(e, it.readBytes(sizeLong.toInt())))
            }
        }
    }

    private fun atPath(path: CharSequence) = VFile.SEPARATOR_PATTERN.split(path).let { split ->
        (split.size() - 1).indices.fold(this : SimpleVFile) { dir, i ->
            val dirName = split[i]
            dir[dirName] ?: MockFile(dirName).let {
                dir.add(it)
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(javaClass<ZipFileProvider>().getName())
    }

}
