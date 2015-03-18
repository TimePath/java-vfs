package com.timepath.vfs

import com.timepath.io.utils.ViewableData
import com.timepath.util.concurrent.DaemonThreadFactory
import com.timepath.vfs.provider.ProviderPlugin
import org.jetbrains.annotations.NonNls

import javax.swing.*
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger

/**
 * A simple {@link com.timepath.vfs.VFile} implementation using synchronized recursive {@link java.util.HashMap}s
 *
 * @author TimePath
 */
public abstract class SimpleVFile protected() : MutableVFile<SimpleVFile>, ViewableData, FileChangeListener {
    protected val files: MutableMap<String, SimpleVFile>
    private val listeners: MutableCollection<FileChangeListener>
    override var lastModified: Long = System.currentTimeMillis()
        protected set
    override var parent: SimpleVFile? = null
        protected set

    {
        files = Collections.synchronizedMap<String, SimpleVFile>(HashMap<String, SimpleVFile>(0))
        listeners = LinkedList<FileChangeListener>()
    }

    public fun visit(dir: File, v: FileVisitor) {
        val ls = dir.listFiles()
        if (ls == null) {
            return
        }
        for (f in ls) {
            v.visit(f, this)
        }
    }

    public open fun addFileChangeListener(listener: FileChangeListener) {
        listeners.add(listener)
    }

    override fun canExecute() = false

    override fun canRead() = true

    override fun canWrite() = false

    // TODO: lazy node traversal
    override fun createNewFile() = false

    override fun delete() = false

    override fun exists() = true

    override fun list(): Collection<SimpleVFile> = files.values()

    override fun get(NonNls name: String): SimpleVFile? {
        return when (name) {
            "." -> this
            ".." -> parent
            else -> {
                files[name]?.let { return it }
                missingFileHandlers.forEach {
                    it.handle(this, name)?.let { return it }
                }
                null
            }
        }
    }

    override val path: String
        get() {
            val path = (when {
                isDirectory -> name
                else -> ""
            }).let {
                VFile.SEPARATOR_PATTERN.matcher(it).replaceAll("") // Just in case
            }
            val parent = parent
            return when {
                parent == null -> path
                else -> "${parent.path}${VFile.SEPARATOR}$path"
            }
        }

    override val totalSpace = 0L
    override val usableSpace = 0L

    override val isFile: Boolean
        get() = !isDirectory

    override val length: Long = -1L
        get() {
            if (isDirectory) {
                return files.size().toLong()
            }
            if ($length == -1L) {
                $length = lengthEstimate()
            }
            return $length
        }

    override fun renameTo(dest: SimpleVFile) = false

    override fun setExecutable(executable: Boolean) = false

    override fun setExecutable(executable: Boolean, ownerOnly: Boolean) = false

    override fun setReadable(readable: Boolean) = false

    override fun setReadable(readable: Boolean, ownerOnly: Boolean) = false

    override fun setWritable(writable: Boolean) = false

    override fun setWritable(writable: Boolean, ownerOnly: Boolean) = false

    private fun lengthEstimate(): Long {
        try {
            openStream()?.use { return it.available().toLong() }
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, null, e)
        }
        return -1L
    }

    public open fun setParent(newParent: SimpleVFile?): Boolean {
        if (newParent === parent) return false
        parent?.remove(this)
        newParent?.let {
            parent = it
            it.add(this)
        }
        return true
    }

    /**
     * Get a file separated by {@code SEPARATOR}
     * TODO: this should be a default interface method
     *
     * @param path
     * @return the file, or null
     */
    public open fun query(path: String): SimpleVFile? {
        val split = path.split(VFile.SEPARATOR)
        if (split.size() == 1) return get(path) // Fast path
        // Compute absolute canonical path
        val stack = LinkedList<String>()
        for (token in split) {
            when (token) {
                "", // Ignore repeated separator
                "." // Ignore current directory
                -> Unit
                ".." -> {
                    if (!stack.isEmpty()) stack.removeLast()
                }
                else -> stack.addLast(token)
            }
        }
        LOG.log(Level.FINE, "Getting {0}", stack)
        return stack.fold(this : SimpleVFile?) {(result: SimpleVFile?, token: String) ->
            val next = result?.get(token)
            when (next) {
                null -> return null
                else -> next
            }
        }
    }

    override fun add(file: SimpleVFile): SimpleVFile {
        if (file === this) throw IllegalArgumentException("file cannot be this")
        synchronized (files) {
            addImpl(file)
        }
        return this
    }

    override fun addAll(files: Iterable<SimpleVFile>): SimpleVFile {
        synchronized (this.files) {
            for (file in files) {
                addImpl(file)
            }
        }
        return this
    }

    override fun remove(file: SimpleVFile) {
        if (file == this) throw IllegalArgumentException("file cannot be this")
        synchronized (files) {
            removeImpl(file)
        }
    }

    override fun removeAll(files: Iterable<SimpleVFile>) {
        synchronized (this.files) {
            for (file in files) {
                removeImpl(file)
            }
        }
    }

    private fun addImpl(file: SimpleVFile) {
        if (!files.containsValue(file)) {
            if (!file.setParent(this)) {
                files.put(file.name, file)
            }
        }
    }

    /**
     * Convenience method
     *
     * @param dir the directory to extract to
     * @throws java.io.IOException
     */
    throws(javaClass<IOException>())
    public open fun extract(dir: File) {
        val out = File(dir, name)
        if (isDirectory) {
            out.mkdir()
            for (f in list()) {
                f.extract(out)
            }
        } else {
            out.createNewFile()
            openStream()?.let {
                it.buffered().copyTo(out.let { FileOutputStream(it).buffered() })
            }
        }
    }

    override fun fileAdded(file: SimpleVFile) {
        for (listener in listeners) {
            listener.fileAdded(file)
        }
    }

    override fun fileModified(file: SimpleVFile) {
        for (listener in listeners) {
            listener.fileModified(file)
        }
    }

    override fun fileRemoved(file: SimpleVFile) {
        for (listener in listeners) {
            listener.fileRemoved(file)
        }
    }

    /**
     * Convenience method to find files recursively by name
     *
     * @param search
     * @return
     */
    public open fun find(search: String): List<SimpleVFile> {
        return find(search, this)
    }

    private fun find(NonNls search: String, root: SimpleVFile): List<SimpleVFile> {
        val list = LinkedList<SimpleVFile>()
        for (e in root.list()) {
            if (e.name.compareToIgnoreCase(search) == 0) {
                list.add(e)
            }
            if (e.isDirectory) {
                list.addAll(find(search, e))
            }
        }
        return list
    }

    override fun getIcon(): Icon? {
        if (isDirectory) {
            var icon: Icon? = null
            if (parent == null) {
                icon = UIManager.getIcon("FileView.hardDriveIcon")
            }
            if (icon == null) {
                icon = UIManager.getIcon("FileView.directoryIcon")
            }
            return icon
        } else {
            return UIManager.getIcon("FileView.fileIcon")
        }
    }

    public open val group: String = DEFAULT_GROUP

    public open val owner: String = DEFAULT_OWNER

    private fun removeImpl(file: SimpleVFile) {
        files.remove(file.name)?.setParent(null)
    }

    override fun toString() = name

    public trait FileHandler {

        throws(javaClass<IOException>())
        public fun handle(file: File): Collection<SimpleVFile>?
    }

    public trait FileVisitor {

        public fun visit(file: File, parent: SimpleVFile)
    }

    public trait MissingFileHandler {

        public fun handle(parent: SimpleVFile, name: String): SimpleVFile?
    }

    class object {

        public val pool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 10, DaemonThreadFactory())
        private val LOG = Logger.getLogger(javaClass<SimpleVFile>().getName())
        NonNls
        private val DEFAULT_GROUP = System.getProperty("user.name", "nobody")
        NonNls
        private val DEFAULT_OWNER = System.getProperty("user.name", "nobody")
        protected var missingFileHandlers: MutableList<MissingFileHandler> = LinkedList()
        var handlers: MutableList<FileHandler> = LinkedList()
            protected set

        SuppressWarnings("WhileLoopReplaceableByForEach")
        private fun locate() {
            val it = ServiceLoader.load(javaClass<ProviderPlugin>()).iterator()
            while (it.hasNext()) {
                try {
                    handlers.add(it.next().register())
                } catch (e: ServiceConfigurationError) {
                    LOG.log(Level.WARNING, "Unable to load Plugin", e)
                }
            }
        }

        public fun registerMissingFileHandler(h: MissingFileHandler) {
            missingFileHandlers.add(h)
        }

        {
            locate()
        }
    }
}
