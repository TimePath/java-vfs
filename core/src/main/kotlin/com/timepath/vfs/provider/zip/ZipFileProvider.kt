package com.timepath.vfs.provider.zip

import com.timepath.vfs.MockFile
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ProviderStub

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipInputStream
import com.timepath.vfs.VFile

public class ZipFileProvider [throws(javaClass<IOException>())](data: ByteArray) : ProviderStub() {

    {
        val bis = BufferedInputStream(ByteArrayInputStream(data))
        bis.mark(data.size())
        ZipInputStream(bis).use { zs ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val e = zs.getNextEntry()
                if (e == null) break
                LOG.log(Level.FINE, "{0}", e)
                var sizeLong = e.getSize()
                if (sizeLong == -1L) sizeLong = e.getCompressedSize()
                if (sizeLong == -1L) sizeLong = BUFFER_SIZE.toLong()
                if (sizeLong > Integer.MAX_VALUE) {
                    LOG.log(Level.SEVERE, "ZipEntry exceeds sizeof int")
                    continue
                }
                ByteArrayOutputStream(sizeLong.toInt()).use { baos ->
                    while (true) {
                        val len = zs.read(buffer)
                        if (len < 0) break
                        baos.write(buffer, 0, len)
                    }
                    atPath(e.getName()).add(ZipFile(e, baos.toByteArray()))
                }
            }
        }
    }

    private fun atPath(path: CharSequence): SimpleVFile {
        val split = VFile.SEPARATOR_PATTERN.split(path)
        var dir: SimpleVFile = this
        for (i in (split.size() - 1).indices) {
            val dirName = split[i]
            var sub = dir[dirName]
            // Create transient directories
            if (sub == null) {
                sub = MockFile(dirName)
                dir.add(sub!!)
            }
            dir = sub!!
        }
        return dir
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<ZipFileProvider>().getName())
        private val BUFFER_SIZE = 2048
    }

}
