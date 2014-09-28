package com.timepath.vfs.provider.http;

import com.timepath.vfs.MockFile;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class HTTPFSTest {

    @Test
    public void testRun() throws Exception {
        @NotNull HttpProvider httpfs = new HttpProvider(8000);
        httpfs.add(new MockFile("test.txt", "It works!"));
        httpfs.add(new MockFile("world.txt", "Hello world"));
        httpfs.run();
    }
}