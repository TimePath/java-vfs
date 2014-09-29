package com.timepath.vfs;

import org.jetbrains.annotations.NotNull;

/**
 * @author TimePath
 */
public interface FileChangeListener {

    void fileAdded(@NotNull SimpleVFile file);

    void fileModified(@NotNull SimpleVFile file);

    void fileRemoved(@NotNull SimpleVFile file);
}
