package com.timepath.vfs.examples

import com.timepath.vfs.MockFile
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.server.ftp.FtpServer
import com.timepath.vfs.server.fuse.FuseServer
import com.timepath.vfs.server.http.HttpServer
import com.timepath.vfs.server.sftp.SftpServer
import java.io.File
import kotlin.platform.platformStatic

object Main {

    public platformStatic fun main(args: Array<String>) {
        val ftp = FtpServer(2121, null)
        addMocks(ftp)
//        ftp.run()

        val fuse = FuseServer(File("test").let {
            it.mkdirs()
            it
        })
        addMocks(fuse)
        fuse.run()

        val http = HttpServer(8000)
        addMocks(http)
        http.run()

        val sftp = SftpServer(1234)
        addMocks(sftp)
        sftp.run()
    }

    private fun addMocks(root: SimpleVFile) {
        root.add(MockFile("test.txt", "It works!"))
                .add(MockFile("world.txt", "Hello world"))
                .add(MockFile("folder")
                        .add(MockFile("file", "test"))
                        .add(MockFile("test.txt", "It works!")))
    }
}

