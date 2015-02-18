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

    public open fun calculateChecksum(): Long {
        return (-1).toLong()
    }

    public fun getAbsoluteName(): String {
        return path + name
    }

    public abstract fun getAttributes(): Any?

    public open fun getChecksum(): Long {
        return (-1).toLong()
    }

    public abstract fun getRoot(): ExtendedVFile

    public abstract fun isComplete(): Boolean

    class object {

        private val LOG = Logger.getLogger(javaClass<ExtendedVFile>().getName())
    }
}
