package com.timepath.vfs;

/**
 *
 * @author TimePath
 */
public interface FileChangeListener {

    public void fileAdded(SimpleVFile f);

    public void fileModified(SimpleVFile f);

    public void fileRemoved(SimpleVFile f);
    
}
