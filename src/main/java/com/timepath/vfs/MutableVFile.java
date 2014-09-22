package com.timepath.vfs;

/**
 * @param <V> Specific type of VFile
 * @author TimePath
 */
public interface MutableVFile<V extends VFile<?>> {

    V add(V file);

    V addAll(Iterable<? extends V> c);

    void remove(V file);

    void removeAll(Iterable<? extends V> files);
}
