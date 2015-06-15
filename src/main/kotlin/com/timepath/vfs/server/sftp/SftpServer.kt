package com.timepath.vfs.server.sftp

import com.timepath.util.Cache
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ProviderStub
import org.apache.sshd.SshServer
import org.apache.sshd.common.NamedFactory
import org.apache.sshd.common.Session
import org.apache.sshd.common.file.FileSystemFactory
import org.apache.sshd.common.file.FileSystemView
import org.apache.sshd.common.file.SshFile
import org.apache.sshd.server.Command
import org.apache.sshd.server.UserAuth
import org.apache.sshd.server.auth.UserAuthNone
import org.apache.sshd.server.command.ScpCommandFactory
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.sftp.subsystem.SftpSubsystem
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls
import java.io.IOException
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */

public fun SftpServer(): SftpServer {
    return SftpServer(0)
}

public fun SftpServer(port: Int): SftpServer {
    return SftpServer("sftp", port)
}

public class SftpServer(NonNls name: String, private val port: Int = 0) : ProviderStub(name), Runnable {

    private var sshd: SshServer? = null

    public fun getPort(): Int = sshd!!.getPort()

    override fun run() {
        sshd = SshServer.setUpDefaultServer()
        sshd!!.setPort(port)
        sshd!!.setKeyPairProvider(SimpleGeneratorHostKeyProvider("hostkey.ser"))
        val fileSystemFactory = object : FileSystemFactory {
            throws(IOException::class)
            override fun createFileSystemView(session: Session): FileSystemView {
                return FileSystemViewAdapter(this@SftpServer)
            }
        }
        sshd!!.setFileSystemFactory(fileSystemFactory)
        sshd!!.setUserAuthFactories(Arrays.asList<NamedFactory<UserAuth>>(UserAuthNone.Factory()))
        sshd!!.setCommandFactory(ScpCommandFactory())
        sshd!!.setSubsystemFactories(Arrays.asList<NamedFactory<Command>>(SftpSubsystem.Factory()))
        try {
            sshd!!.start()
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, null, e)
        }

    }

    class FileSystemViewAdapter(private val root: SimpleVFile) : FileSystemView {

        override fun getFile(file: String) = wrap(root[file])

        override fun getFile(baseDir: SshFile, file: String) = wrap(root[file])

        override fun getNormalizedView() = this
    }

    class SshFileAdapter(private val delegate: SimpleVFile) : SshFile {

        private val attributes = object : Cache<SshFile.Attribute, Any>(EnumMap<SshFile.Attribute, Any>(javaClass<SshFile.Attribute>())) {
            SuppressWarnings("MethodWithMultipleReturnPoints", "OverlyComplexMethod")
            override fun fill(key: SshFile.Attribute): Any? {
                return when (key) {
                    SshFile.Attribute.IsRegularFile -> !isDirectory()
                    SshFile.Attribute.Size -> getSize()
                    SshFile.Attribute.Uid -> 1000
                    SshFile.Attribute.Owner -> getOwner()
                    SshFile.Attribute.Gid -> 1000
                    SshFile.Attribute.Group -> delegate.group
                    SshFile.Attribute.IsDirectory -> isDirectory()
                    SshFile.Attribute.IsSymbolicLink -> false
                    SshFile.Attribute.Permissions -> EnumSet.of(
                            SshFile.Permission.UserRead,
                            SshFile.Permission.GroupRead,
                            SshFile.Permission.OthersRead,
                            SshFile.Permission.UserWrite,
                            SshFile.Permission.GroupWrite,
                            SshFile.Permission.OthersWrite,
                            SshFile.Permission.UserExecute,
                            SshFile.Permission.GroupExecute,
                            SshFile.Permission.OthersExecute
                    )
                    SshFile.Attribute.CreationTime -> delegate.lastModified
                    SshFile.Attribute.LastModifiedTime -> delegate.lastModified
                    SshFile.Attribute.LastAccessTime -> delegate.lastModified
                    else -> null
                }
            }
        }

        override fun getAbsolutePath() = delegate.path

        override fun getName() = delegate.name

        throws(IOException::class)
        override fun getAttributes(followLinks: Boolean): Map<SshFile.Attribute, Any> {
            return Collections.unmodifiableMap(attributes)
        }

        throws(IOException::class)
        override fun setAttributes(attributes: Map<SshFile.Attribute, Any>) {
            for ((k, v) in attributes.entrySet()) {
                setAttribute(k, v)
            }
        }

        throws(IOException::class)
        override fun getAttribute(attribute: SshFile.Attribute, followLinks: Boolean) = attributes[attribute]

        throws(IOException::class)
        override fun setAttribute(attribute: SshFile.Attribute, value: Any) {
            attributes.put(attribute, value)
        }

        throws(IOException::class)
        override fun readSymbolicLink() = null

        throws(IOException::class)
        override fun createSymbolicLink(destination: SshFile) = Unit

        override fun getOwner() = delegate.owner

        override fun isDirectory() = delegate.isDirectory

        override fun isFile() = delegate.isFile

        override fun doesExist() = delegate.exists()

        override fun isReadable() = true

        override fun isWritable() = false

        override fun isExecutable() = true

        override fun isRemovable() = false

        override fun getParentFile() = wrap(delegate.parent)

        override fun getLastModified() = delegate.lastModified

        override fun setLastModified(time: Long): Boolean {
            delegate.lastModified = time
            return true
        }

        override fun getSize() = delegate.length

        override fun mkdir() = false

        override fun delete() = false

        throws(IOException::class)
        override fun create() = false

        throws(IOException::class)
        override fun truncate() = Unit

        override fun move(destination: SshFile) = false

        override fun listSshFiles(): List<SshFile> {
            val list = delegate.list()
            val sshFiles = LinkedList<SshFile>()
            for (simpleVFile in list) {
                sshFiles.add(wrap(simpleVFile))
            }
            return sshFiles
        }

        throws(IOException::class)
        override fun createOutputStream(offset: Long) = null

        throws(IOException::class)
        override fun createInputStream(offset: Long) = delegate.openStream()

        throws(IOException::class)
        override fun handleClose() = Unit
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<SftpServer>().getName())

        Contract("null -> null")
        private fun wrap(file: SimpleVFile?) = when (file) {
            null -> null
            else -> SshFileAdapter(file)
        }
    }
}
