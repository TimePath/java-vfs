package com.timepath.vfs.ftp;

import com.timepath.vfs.MockFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class FTPFSTest {

    @Test
    public void testRun() throws Exception {
        @NotNull FTPFS f = new FTPFS(2121, null);
        f.add(new MockFile("test.txt", "It works!"))
                .add(new MockFile("world.txt", "Hello world"))
                .add(new MockFile("folder").add(new MockFile("file", "test")));
        f.run();
    }
}