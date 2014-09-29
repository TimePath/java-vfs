package com.timepath.vfs;

import org.jetbrains.annotations.NotNull;

/**
 * @param <V> Specific type of VFile
 * @author TimePath
 */
public interface MutableVFile<V extends VFile<?>> extends VFile<V> {

    @NotNull
    V add(@NotNull V file);

    @NotNull
    V addAll(@NotNull Iterable<? extends V> files);

    void remove(@NotNull V file);

    void removeAll(@NotNull Iterable<? extends V> files);
}
