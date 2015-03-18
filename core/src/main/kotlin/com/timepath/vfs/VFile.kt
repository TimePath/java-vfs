package com.timepath.vfs

import org.jetbrains.annotations.NonNls

import java.io.InputStream
import java.util.regex.Pattern

/**
 * @param <V> Specific type of VFile
 * @author TimePath
 */
public trait VFile<V : VFile<V>> {

    fun canExecute(): Boolean

    fun canRead(): Boolean

    fun canWrite(): Boolean

    fun createNewFile(): Boolean

    fun delete(): Boolean

    fun exists(): Boolean

    /**
     * @return the file name without {@code SEPARATOR}s
     */
    val name: String

    val parent: V?

    [suppress("REDUNDANT_PROJECTION")]
    fun list(): Collection<out V>

    /**
     * Get a file by literal name
     *
     * @param name
     * @return the file, or null
     */
    fun get(NonNls name: String): V?

    val path: String

    val totalSpace: Long

    val usableSpace: Long

    val isDirectory: Boolean

    val isFile: Boolean

    val lastModified: Long

    val length: Long

    fun renameTo(dest: V): Boolean

    fun setExecutable(executable: Boolean): Boolean

    fun setExecutable(executable: Boolean, ownerOnly: Boolean): Boolean

    fun setReadable(readable: Boolean): Boolean

    fun setReadable(readable: Boolean, ownerOnly: Boolean): Boolean

    fun setWritable(writable: Boolean): Boolean

    fun setWritable(writable: Boolean, ownerOnly: Boolean): Boolean

    /**
     * Open the file for reading. It is the caller's responsibility to close the stream
     *
     * @return the stream, or null
     */
    fun openStream(): InputStream?

    /**
     * @return some identifier, may contain {@code SEPARATOR} unlike {@link #getName()}
     */
    override fun toString(): String

    class object {

        NonNls
        val SEPARATOR: String = "/"

        val SEPARATOR_PATTERN: Pattern = SEPARATOR.toRegex()
    }
}
