package com.timepath.vfs.fuse;

import com.timepath.vfs.MockFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class FUSEFSTest {

    @Test
    public void testRun() throws Exception {
        @NotNull FUSEFS fusefs = new FUSEFS("test");
        fusefs.add(new MockFile("folder").add(new MockFile("test.txt", "It works!\n")));
        fusefs.add(new MockFile("test.txt", "It works!\n"));
        fusefs.add(new MockFile("world.txt", "Hello world\n"));
        fusefs.run();
    }
}