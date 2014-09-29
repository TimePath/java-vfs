package com.timepath.vfs;

import org.jetbrains.annotations.NotNull;

/**
 * @param <V> Specific type of VFile
 * @author TimePath
 */
public interface MutableVFile<V extends VFile<?>> {

    @NotNull
    V add(V file);

    @NotNull
    V addAll(Iterable<? extends V> c);

    void remove(V file);

    void removeAll(Iterable<? extends V> files);
}
