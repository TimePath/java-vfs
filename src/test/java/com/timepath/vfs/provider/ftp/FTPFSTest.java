package com.timepath.vfs.provider.ftp;

import com.timepath.vfs.MockFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class FTPFSTest {

    @Test
    public void testRun() throws Exception {
        @NotNull FtpProvider f = new FtpProvider(2121, null);
        f.add(new MockFile("test.txt", "It works!"))
                .add(new MockFile("world.txt", "Hello world"))
                .add(new MockFile("folder").add(new MockFile("file", "test")));
        f.run();
    }
}