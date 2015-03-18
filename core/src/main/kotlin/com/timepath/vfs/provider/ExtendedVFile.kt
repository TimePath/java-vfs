package com.timepath.vfs.provider

import com.timepath.swing.TreeUtils
import com.timepath.vfs.SimpleVFile

import javax.swing.tree.DefaultMutableTreeNode
import java.util.logging.Logger

/**
 * @author TimePath
 */
public abstract class ExtendedVFile protected() : SimpleVFile() {

    public fun analyze(top: DefaultMutableTreeNode, leaves: Boolean) {
        if (top.getUserObject() is ExtendedVFile) {
            // the root node has been added
            val e = top.getUserObject() as ExtendedVFile
            for (n in e.list()) {
                val ret = DefaultMutableTreeNode(n)
                if (n.isDirectory) {
                    analyze(ret, leaves)
                    top.add(ret)
                } else if (leaves) {
                    top.add(ret)
                }
            }
        } else {
            // the root node has not been added
            val ret = DefaultMutableTreeNode(this)
            analyze(ret, leaves)
            TreeUtils.moveChildren(ret, top)
        }
    }

    public open fun calculateChecksum(): Long = -1L

    public val absoluteName: String
        get() = "$path$name"

    public abstract val attributes: Any?

    public open val checksum: Long
        get() = -1L

    public abstract val root: ExtendedVFile

    public abstract val isComplete: Boolean

    class object {
        private val LOG = Logger.getLogger(javaClass<ExtendedVFile>().getName())
    }
}
