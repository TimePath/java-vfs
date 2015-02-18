package com.timepath.vfs.provider.local

import com.timepath.vfs.provider.ExtendedVFile

import java.io.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
open class LocalFile(protected val file: File) : ExtendedVFile() {

    override fun getAttributes(): Any? {
        return null
    }

    override fun getRoot(): ExtendedVFile {
        return this
    }

    override fun isComplete(): Boolean {
        return true
    }

    override val name: String
        get() = file.name

    override fun openStream(): InputStream? {
        try {
            return BufferedInputStream(FileInputStream(file))
        } catch (ex: FileNotFoundException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        return null
    }

    override val isDirectory: Boolean
        get() = file.isDirectory()

    override var lastModified: Long
        get() = file.lastModified()
        set(time) {
            file.setLastModified(time)
        }

    override val length: Long
        get() = file.length()

    class object {

        private val LOG = Logger.getLogger(javaClass<LocalFile>().getName())
    }

}
