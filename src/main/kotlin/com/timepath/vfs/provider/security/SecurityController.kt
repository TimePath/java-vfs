package com.timepath.vfs.provider.security

import com.timepath.vfs.SimpleVFile

import java.io.InputStream

/**
 * Represents the various actions which can be performed on files, and gives you a chance to override or extend their
 * implementation
 *
 * @author TimePath
 */
public abstract class SecurityController {

    /**
     * Called in response to {@link com.timepath.vfs.SimpleVFile#openStream()}
     *
     * @param file The requested file
     * @return Potentially modified {@code file}
     */
    public fun openStream(file: SimpleVFile): InputStream? = file.openStream()

    /**
     * Called in response to {@link SimpleVFile#add(SimpleVFile)}
     *
     * @param parent The parent file
     * @param file   The file
     */
    public fun add(parent: SimpleVFile, file: SimpleVFile) {
        parent.add(file)
    }

    /**
     * Called in response to {@link SimpleVFile#list()}
     *
     * @param file The file
     * @return Potentially modified {@code file.list()}
     */
    public fun list(file: SimpleVFile): Collection<SimpleVFile> = file.list()

    /**
     * Called in response to {@link SimpleVFile#get(String)}
     *
     * @param file The file
     * @return Potentially modified {@code file}
     */
    public fun get(file: SimpleVFile?): SimpleVFile? = file
}
