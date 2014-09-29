package com.timepath.vfs.examples;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.MutableVFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.provider.ftp.FtpProvider;
import com.timepath.vfs.provider.fuse.FuseProvider;
import com.timepath.vfs.provider.http.HttpProvider;
import com.timepath.vfs.provider.sftp.SftpProvider;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        FtpProvider ftp = new FtpProvider(2121, null);
        addMocks(ftp);
        ftp.run();

        FuseProvider fuse = new FuseProvider("test");
        addMocks(fuse);
        fuse.run();

        HttpProvider http = new HttpProvider(8000);
        addMocks(http);
        http.run();

        SftpProvider sftp = new SftpProvider(1234);
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