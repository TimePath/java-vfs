package com.timepath.vfs;

/**
 * @author TimePath
 */
public interface FileChangeListener {

    void fileAdded(SimpleVFile f);

    void fileModified(SimpleVFile f);

    void fileRemoved(SimpleVFile f);
}
