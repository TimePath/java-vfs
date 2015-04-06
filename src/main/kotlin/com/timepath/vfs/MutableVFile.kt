package com.timepath.vfs

/**
 * @param <V> Specific type of VFile
 * @author TimePath
 */
public trait MutableVFile<V : VFile<V>> : VFile<V> {

    public fun add(file: V): V

    public fun addAll(files: Iterable<V>): V

    public fun remove(file: V)

    public fun removeAll(files: Iterable<V>)
}
