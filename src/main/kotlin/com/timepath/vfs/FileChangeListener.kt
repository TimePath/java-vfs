package com.timepath.vfs

/**
 * @author TimePath
 */
public interface FileChangeListener {

    public fun fileAdded(file: SimpleVFile)

    public fun fileModified(file: SimpleVFile)

    public fun fileRemoved(file: SimpleVFile)
}
