package com.timepath.vfs.provider.zip

import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.VFile

import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry

/**
 * @author TimePath
 */
class ZipFile(private val entry: ZipEntry, private val data: ByteArray) : SimpleVFile() {

    override val name: String
        get() = entry.getName().let {
            it.substring(it.lastIndexOf(VFile.SEPARATOR) + 1)
        }

    override fun openStream() = ByteArrayInputStream(data)

    override val isDirectory: Boolean
        get() = entry.isDirectory()

    override val length: Long
        get() = entry.getSize()
}
