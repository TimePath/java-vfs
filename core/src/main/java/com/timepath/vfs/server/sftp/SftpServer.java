package com.timepath.vfs.server.sftp;

import com.timepath.util.Cache;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFSStub;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.subsystem.SftpSubsystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class SftpServer extends VFSStub implements Runnable {

    private static final Logger LOG = Logger.getLogger(SftpServer.class.getName());
    private final int port;
    private SshServer sshd;

    public SftpServer() {
        this(0);
    }

    public SftpServer(String name) {
        this(name, 0);
    }

    public SftpServer(int port) {
        this("sftp", port);
    }

    public SftpServer(String name, int port) {
        super(name);
        this.port = port;
    }

    @Nullable
    private static SshFile wrap(@Nullable SimpleVFile file) {
        return (file == null) ? null : new SshFileAdapter(file);
    }

    public int getPort() {
        return sshd.getPort();
    }

    @Override
    public void run() {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        FileSystemFactory fileSystemFactory = new FileSystemFactory() {
            @Override
            public FileSystemView createFileSystemView(Session session) throws IOException {
                return new FileSystemViewAdapter(SftpServer.this);
            }
        };
        sshd.setFileSystemFactory(fileSystemFactory);
        sshd.setUserAuthFactories(Arrays.<NamedFactory<UserAuth>>asList(new UserAuthNone.Factory()));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));
        try {
            sshd.start();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, null, e);
        }
    }

    static class FileSystemViewAdapter implements FileSystemView {

        @NotNull
        private final SimpleVFile root;

        FileSystemViewAdapter(@NotNull SimpleVFile root) {
            this.root = root;
        }

        @Override
        public SshFile getFile(String file) {
            return wrap(root.get(file));
        }

        @Override
        public SshFile getFile(SshFile baseDir, String file) {
            return wrap(root.get(file));
        }

        @Override
        public FileSystemView getNormalizedView() {
            return this;
        }
    }

    static class SshFileAdapter implements SshFile {

        @NotNull
        private final SimpleVFile delegate;

        private final Map<Attribute, Object> attributes = new Cache<Attribute, Object>(new EnumMap<>(Attribute.class)) {
            @SuppressWarnings({"MethodWithMultipleReturnPoints", "OverlyComplexMethod"})
            @Nullable
            @Override
            protected Object fill(Attribute key) {
                switch (key) {
                    case IsRegularFile:
                        return !isDirectory();
                    case Size:
                        return getSize();
                    case Uid:
                        return 1000;
                    case Owner:
                        return getOwner();
                    case Gid:
                        return 1000;
                    case Group:
                        return delegate.group();
                    case IsDirectory:
                        return isDirectory();
                    case IsSymbolicLink:
                        return false;
                    case Permissions:
                        return EnumSet.of(
                                Permission.UserRead, Permission.GroupRead, Permission.OthersRead,
                                Permission.UserWrite, Permission.GroupWrite, Permission.OthersWrite,
                                Permission.UserExecute, Permission.GroupExecute, Permission.OthersExecute
                        );
                    case CreationTime:
                        return delegate.lastModified();
                    case LastModifiedTime:
                        return delegate.lastModified();
                    case LastAccessTime:
                        return delegate.lastModified();
                }
                return null;
            }
        };

        SshFileAdapter(@NotNull SimpleVFile file) {
            delegate = file;
        }

        @Override
        public String getAbsolutePath() {
            return delegate.getPath();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
            return Collections.unmodifiableMap(attributes);
        }

        @Override
        public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
            for (Map.Entry<Attribute, Object> entry : attributes.entrySet()) {
                setAttribute(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
            return attributes.get(attribute);
        }

        @Override
        public void setAttribute(Attribute attribute, Object value) throws IOException {
            attributes.put(attribute, value);
        }

        @Override
        public String readSymbolicLink() throws IOException {
            return null;
        }

        @Override
        public void createSymbolicLink(SshFile destination) throws IOException {

        }

        @Override
        public String getOwner() {
            return delegate.owner();
        }

        @Override
        public boolean isDirectory() {
            return delegate.isDirectory();
        }

        @Override
        public boolean isFile() {
            return delegate.isFile();
        }

        @Override
        public boolean doesExist() {
            return delegate.exists();
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean isExecutable() {
            return true;
        }

        @Override
        public boolean isRemovable() {
            return false;
        }

        @Override
        public SshFile getParentFile() {
            return wrap(delegate.getParent());
        }

        @Override
        public long getLastModified() {
            return delegate.lastModified();
        }

        @Override
        public boolean setLastModified(long time) {
            return delegate.setLastModified(time);
        }

        @Override
        public long getSize() {
            return delegate.length();
        }

        @Override
        public boolean mkdir() {
            return false;
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public boolean create() throws IOException {
            return false;
        }

        @Override
        public void truncate() throws IOException {

        }

        @Override
        public boolean move(SshFile destination) {
            return false;
        }

        @Override
        public List<SshFile> listSshFiles() {
            Collection<? extends SimpleVFile> list = delegate.list();
            List<SshFile> sshFiles = new LinkedList<SshFile>();
            for (SimpleVFile simpleVFile : list) {
                sshFiles.add(wrap(simpleVFile));
            }
            return sshFiles;
        }

        @Override
        public OutputStream createOutputStream(long offset) throws IOException {
            return new NullOutputStream();
        }

        @Override
        public InputStream createInputStream(long offset) throws IOException {
            return delegate.openStream();
        }

        @Override
        public void handleClose() throws IOException {

        }
    }
}
