package com.timepath.vfs.provider.local

import com.timepath.vfs.SimpleVFile

import java.io.File
import java.util.LinkedList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
public open class LocalFileProvider(file: File) : LocalFile(file) {

    init {
        // TODO: lazy loading
        if (file.isDirectory()) {
            insert(file)
        }
    }

    /**
     * Insert children of a directory
     *
     * @param toInsert the directory
     */
    private fun insert(toInsert: File) {
        val start = System.currentTimeMillis()
        val tasks = LinkedList<Future<Unit>>()
        visit(toInsert, object : SimpleVFile.FileVisitor {
            override fun visit(file: File, parent: SimpleVFile) {
                val entry = LocalFile(file)
                parent.add(entry)
                if (file.isDirectory()) {
                    // Depth first search
                    entry.visit(file, this)
                } else {
                    // Start background identification
                    tasks.add(SimpleVFile.pool.submit<Unit> {
                        SimpleVFile.handlers.forEach {
                            it.handle(file)?.let {
                                // Defensive copy
                                it.toTypedArray().forEach {
                                    merge(it, parent)
                                }
                            }
                        }
                    })
                }
            }
        })
        // Await all
        tasks.forEach {
            try {
                it.get()
            } catch (e: InterruptedException) {
                LOG.log(Level.SEVERE, null, e)
            } catch (e: ExecutionException) {
                LOG.log(Level.SEVERE, null, e)
            }
        }
        LOG.log(Level.INFO, "Recursive file load took {0}ms", System.currentTimeMillis() - start)
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<LocalFileProvider>().getName())

        /**
         * Adds one file to another, merging any directories.
         * TODO: additive/union directories to avoid this kludge
         *
         * @param src
         * @param parent
         */
        synchronized private fun merge(src: SimpleVFile, parent: SimpleVFile) {
            val existing = parent[src.name]
            if (existing == null) {
                // Parent does not have this file, simple case
                parent.add(src)
            } else {
                // Add all child files, silently ignore duplicates
                val children = src.list()
                // Defensive copy
                for (file in children.toTypedArray()) {
                    merge(file, existing)
                }
            }
        }
    }

}
