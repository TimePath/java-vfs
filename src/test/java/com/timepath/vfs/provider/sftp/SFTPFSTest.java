package com.timepath.vfs.provider.sftp;

import com.timepath.vfs.MockFile;
import org.junit.Test;

public class SFTPFSTest {

    @Test
    public void testRun() throws Exception {
        SftpProvider root = new SftpProvider();
        MockFile file = new MockFile("Hello", "world");
        root.add(file);
        root.run();
    }
}