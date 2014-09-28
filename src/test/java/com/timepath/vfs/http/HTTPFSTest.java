package com.timepath.vfs.http;

import com.timepath.vfs.MockFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class HTTPFSTest {

    @Test
    public void testRun() throws Exception {
        @NotNull HTTPFS httpfs = new HTTPFS(8000);
        httpfs.add(new MockFile("test.txt", "It works!"));
        httpfs.add(new MockFile("world.txt", "Hello world"));
        httpfs.run();
    }
}