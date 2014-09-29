package com.timepath.vfs.provider;

import com.timepath.vfs.FileChangeListener;
import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Decorates files. Implementers must override {@link #wrap(com.timepath.vfs.SimpleVFile)} to return their subclass
 *
 * @author TimePath
 */
public abstract class DelegateProvider extends SimpleVFile {

    @NotNull
    protected final SimpleVFile data;

    protected DelegateProvider(@NotNull SimpleVFile data) {
        this.data = data;
    }

    @Contract("null -> null")
    protected abstract SimpleVFile wrap(@Nullable SimpleVFile file);

    /**
     * @param unwrapped A collection of files to be decorated with the current security settings
     * @return The original list, decorated
     */
    @NotNull
    protected List<SimpleVFile> wrap(@NotNull Iterable<? extends SimpleVFile> unwrapped) {
        // TODO: be smart about the original collection type rather than assume list
        List<SimpleVFile> wrapped = new LinkedList<>();
        for (SimpleVFile v : unwrapped) {
            wrapped.add(wrap(v));
        }
        return wrapped;
    }

    /**
     * This method intentionally does not delegate
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("EmptyMethod")
    public boolean setParent(SimpleVFile newParent) {
        return super.setParent(newParent);
    }

    @NotNull
    @Override
    public SimpleVFile add(@NotNull SimpleVFile file) {
        data.add(file);
        return this;
    }

    @NotNull
    @Override
    public SimpleVFile addAll(@NotNull Iterable<? extends SimpleVFile> files) {
        data.addAll(files);
        return this;
    }

    @NotNull
    @Override
    public List<SimpleVFile> find(String search) {
        return wrap(data.find(search));
    }

    @Nullable
    @Override
    public SimpleVFile get(String name) {
        return wrap(data.get(name));
    }

    @Nullable
    @Override
    public SimpleVFile getParent() {
        return wrap(data.getParent());
    }

    @NotNull
    @Override
    public Collection<? extends SimpleVFile> list() {
        return wrap(data.list());
    }

    @Nullable
    @Override
    public SimpleVFile query(String path) {
        return wrap(data.query(path));
    }

    // Begin trivial delegation

    @Override
    public void addFileChangeListener(FileChangeListener listener) {
        data.addFileChangeListener(listener);
    }

    @Override
    public boolean canExecute() {
        return data.canExecute();
    }

    @Override
    public boolean canRead() {
        return data.canRead();
    }

    @Override
    public boolean canWrite() {
        return data.canWrite();
    }

    @Override
    public boolean createNewFile() {
        return data.createNewFile();
    }

    @Override
    public boolean delete() {
        return data.delete();
    }

    @Override
    public boolean exists() {
        return data.exists();
    }

    @NotNull
    @Override
    public String getPath() {
        return data.getPath();
    }

    @Override
    public long getTotalSpace() {
        return data.getTotalSpace();
    }

    @Override
    public long getUsableSpace() {
        return data.getUsableSpace();
    }

    @Override
    public boolean isDirectory() {
        return data.isDirectory();
    }

    @Override
    public boolean isFile() {
        return data.isFile();
    }

    @Override
    public long lastModified() {
        return data.lastModified();
    }

    @Override
    public long length() {
        return data.length();
    }

    @Override
    public boolean renameTo(SimpleVFile dest) {
        return data.renameTo(dest);
    }

    @Override
    public boolean setExecutable(boolean executable) {
        return data.setExecutable(executable);
    }

    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return data.setExecutable(executable, ownerOnly);
    }

    @Override
    public boolean setLastModified(long time) {
        return data.setLastModified(time);
    }

    @Override
    public boolean setReadable(boolean readable) {
        return data.setReadable(readable);
    }

    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return data.setReadable(readable, ownerOnly);
    }

    @Override
    public boolean setWritable(boolean writable) {
        return data.setWritable(writable);
    }

    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return data.setWritable(writable, ownerOnly);
    }

    @Override
    public void remove(@NotNull SimpleVFile file) {
        data.remove(file);
    }

    @Override
    public void removeAll(@NotNull Iterable<? extends SimpleVFile> files) {
        data.removeAll(files);
    }

    @Override
    public void extract(File dir) throws IOException {
        data.extract(dir);
    }

    @Override
    public void fileAdded(SimpleVFile f) {
        data.fileAdded(f);
    }

    @Override
    public void fileModified(SimpleVFile f) {
        data.fileModified(f);
    }

    @Override
    public void fileRemoved(SimpleVFile f) {
        data.fileRemoved(f);
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return data.getIcon();
    }

    @Override
    public String group() {
        return data.group();
    }

    @Override
    public String owner() {
        return data.owner();
    }

    @NotNull
    @Override
    public String toString() {
        return data.toString();
    }

    @NotNull
    @Override
    public String getName() {
        return data.getName();
    }

    @Override
    public InputStream openStream() {
        return data.openStream();
    }
}
