package com.timepath.vfs.examples;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.server.ftp.FtpServer;
import com.timepath.vfs.server.fuse.FuseServer;
import com.timepath.vfs.server.http.HttpServer;
import com.timepath.vfs.server.sftp.SftpServer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        FtpServer ftp = new FtpServer(2121, null);
        addMocks(ftp);
        ftp.run();

        FuseServer fuse = new FuseServer("test");
        addMocks(fuse);
        fuse.run();

        HttpServer http = new HttpServer(8000);
        addMocks(http);
        http.run();

        SftpServer sftp = new SftpServer(1234);
        addMocks(sftp);
        sftp.run();
    }

    private static void addMocks(SimpleVFile root) {
        root
                .add(new MockFile("test.txt", "It works!"))
                .add(new MockFile("world.txt", "Hello world"))
                .add(new MockFile("folder")
                                .add(new MockFile("file", "test"))
                                .add(new MockFile("test.txt", "It works!"))
                );
    }

}