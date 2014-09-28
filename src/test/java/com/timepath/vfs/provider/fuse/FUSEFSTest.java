package com.timepath.vfs.provider.fuse;

import com.timepath.vfs.MockFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class FUSEFSTest {

    @Test
    public void testRun() throws Exception {
        @NotNull FuseProvider fusefs = new FuseProvider("test");
        fusefs.add(new MockFile("folder").add(new MockFile("test.txt", "It works!\n")));
        fusefs.add(new MockFile("test.txt", "It works!\n"));
        fusefs.add(new MockFile("world.txt", "Hello world\n"));
        fusefs.run();
    }
}