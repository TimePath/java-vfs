package com.timepath.vfs.sftp;

import com.timepath.vfs.MockFile;
import org.junit.Test;

import static org.junit.Assert.*;

public class SFTPFSTest {

    @Test
    public void testRun() throws Exception {
        SFTPFS root = new SFTPFS();
        MockFile file = new MockFile("Hello", "world");
        root.add(file);
        root.run();
    }
}