package com.timepath.vfs.provider.local;

import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class LocalFileProvider extends LocalFile {

    private static final Logger LOG = Logger.getLogger(LocalFileProvider.class.getName());

    public LocalFileProvider(@NotNull File file) {
        super(file);
        // TODO: lazy loading
        if (file.isDirectory()) {
            insert(file);
        }
    }

    /**
     * Adds one file to another, merging any directories.
     * TODO: additive/union directories to avoid this kludge
     *
     * @param src
     * @param parent
     */
    private static synchronized void merge(@NotNull SimpleVFile src, @NotNull SimpleVFile parent) {
        SimpleVFile existing = parent.get(src.getName());
        if (existing == null) {
            // Parent does not have this file, simple case
            parent.add(src);
        } else {
            // Add all child files, silently ignore duplicates
            Collection<? extends SimpleVFile> children = src.list();
            // Defensive copy
            for (@NotNull SimpleVFile file : children.toArray(new SimpleVFile[children.size()])) {
                merge(file, existing);
            }
        }
    }

    /**
     * Insert children of a directory
     *
     * @param toInsert the directory
     */
    private void insert(@NotNull File toInsert) {
        long start = System.currentTimeMillis();
        final Collection<Future<Void>> tasks = new LinkedList<>();
        visit(toInsert, new FileVisitor() {
            @Override
            public void visit(@NotNull final File file, @NotNull final SimpleVFile parent) {
                LocalFile entry = new LocalFile(file);
                parent.add(entry);
                if (file.isDirectory()) {
                    // Depth first search
                    entry.visit(file, this);
                } else {
                    // Start background identification
                    tasks.add(pool.submit(new Callable<Void>() {
                        @Nullable
                        @Override
                        public Void call() throws IOException {
                            for (@NotNull FileHandler handler : SimpleVFile.handlers) {
                                Collection<? extends SimpleVFile> root = handler.handle(file);
                                if (root == null) continue;
                                // Defensive copy
                                for (@NotNull SimpleVFile child : root.toArray(new SimpleVFile[root.size()])) {
                                    merge(child, parent);
                                }
                            }
                            return null;
                        }
                    }));
                }
            }
        });
        // Await all
        for (@NotNull Future<Void> fut : tasks) {
            try {
                fut.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.log(Level.SEVERE, null, e);
            }
        }
        LOG.log(Level.INFO, "Recursive file load took {0}ms", System.currentTimeMillis() - start);
    }

}
