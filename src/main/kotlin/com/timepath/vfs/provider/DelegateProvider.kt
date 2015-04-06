package com.timepath.vfs.provider

import com.timepath.vfs.FileChangeListener
import com.timepath.vfs.SimpleVFile
import org.jetbrains.annotations.Contract
import java.io.File
import java.io.IOException
import java.util.LinkedList
import javax.swing.Icon

/**
 * Decorates files. Implementers must override {@link #wrap(com.timepath.vfs.SimpleVFile)} to return their subclass
 *
 * @author TimePath
 */
public abstract class DelegateProvider protected(protected val data: SimpleVFile) : SimpleVFile() {

    Contract("null -> null")
    protected abstract fun wrap(file: SimpleVFile?): SimpleVFile?

    /**
     * @param unwrapped A collection of files to be decorated with the current security settings
     * @return The original list, decorated
     */
    protected fun wrap(unwrapped: Iterable<SimpleVFile>): List<SimpleVFile> {
        // TODO: be smart about the original collection type rather than assume list
        val wrapped = LinkedList<SimpleVFile>()
        for (v in unwrapped) {
            wrapped.add(wrap(v))
        }
        return wrapped
    }

    override fun add(file: SimpleVFile): SimpleVFile {
        data.add(file)
        return this
    }

    override fun addAll(files: Iterable<SimpleVFile>): SimpleVFile {
        data.addAll(files)
        return this
    }

    override fun find(search: String) = wrap(data.find(search))

    override fun get(name: String) = wrap(data[name])

    override var parent: SimpleVFile?
        get() = wrap(data.parent)
        /**
         * This method intentionally does not delegate
         * {@inheritDoc}
         */
        set(value) {
            super.parent = value
        }

    override fun list() = wrap(data.list())

    override fun query(path: String) = wrap(data.query(path))

    // Begin trivial delegation

    override fun addFileChangeListener(listener: FileChangeListener) = data.addFileChangeListener(listener)

    override fun canExecute() = data.canExecute()

    override fun canRead() = data.canRead()

    override fun canWrite() = data.canWrite()

    override fun createNewFile() = data.createNewFile()

    override fun delete() = data.delete()

    override fun exists() = data.exists()

    override val isDirectory: Boolean
        get() = data.isDirectory

    override val path: String
        get() = data.path

    override val totalSpace: Long
        get() = data.totalSpace

    override val usableSpace: Long
        get() = data.usableSpace

    override val isFile: Boolean
        get() = data.isFile

    override var lastModified: Long
        get() = data.lastModified
        set(value) {
            data.lastModified = value
        }

    override val length: Long get() = data.length

    override fun renameTo(dest: SimpleVFile) = data.renameTo(dest)

    override fun setExecutable(executable: Boolean) = data.setExecutable(executable)

    override fun setExecutable(executable: Boolean, ownerOnly: Boolean) = data.setExecutable(executable, ownerOnly)

    override fun setReadable(readable: Boolean) = data.setReadable(readable)

    override fun setReadable(readable: Boolean, ownerOnly: Boolean) = data.setReadable(readable, ownerOnly)

    override fun setWritable(writable: Boolean) = data.setWritable(writable)

    override fun setWritable(writable: Boolean, ownerOnly: Boolean) = data.setWritable(writable, ownerOnly)

    override fun remove(file: SimpleVFile) = data.remove(file)

    override fun removeAll(files: Iterable<SimpleVFile>) = data.removeAll(files)

    throws(javaClass<IOException>())
    override fun extract(dir: File) = data.extract(dir)

    override fun fileAdded(file: SimpleVFile) = data.fileAdded(file)

    override fun fileModified(file: SimpleVFile) = data.fileModified(file)

    override fun fileRemoved(file: SimpleVFile) = data.fileRemoved(file)

    override fun getIcon(): Icon? = data.getIcon()

    override val group: String
        get() = data.group

    override val owner: String
        get() = data.owner

    override fun toString(): String = data.toString()

    override val name: String
        get() = data.name

    override fun openStream() = data.openStream()
}
