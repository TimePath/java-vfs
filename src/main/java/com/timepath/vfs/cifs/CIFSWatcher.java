package com.timepath.vfs.cifs;

import com.timepath.vfs.FileChangeListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * http://www.codefx.com/CIFS_Explained.htm
 * smbclient --ip-address=localhost --port=8000 -M hi
 *
 * @author TimePath
 */
public class CIFSWatcher {

    private static final Logger LOG = Logger.getLogger(CIFSWatcher.class.getName());
    private static CIFSWatcher instance;
    private final Collection<FileChangeListener> listeners = new LinkedList<>();

    private CIFSWatcher(int port) {
        try {
            // On windows, the loopback address does not prompt the firewall
            // Also good for security in general
            @NotNull final ServerSocket sock = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
            port = sock.getLocalPort();
            LOG.log(Level.INFO, "Listening on port {0}", port);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    LOG.info("CIFS server shutting down...");
                }
            }));
            new Thread(new Runnable() {
                private Socket data;
                private ServerSocket pasv;

                @Override
                public void run() {
                    while (true) {
                        final Socket client;
                        try {
                            LOG.info("Waiting for client...");
                            client = sock.accept();
                            LOG.info("Connected");
                        } catch (IOException ex) {
                            Logger.getLogger(CIFSWatcher.class.getName()).log(Level.SEVERE, null, ex);
                            continue;
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputStream is = client.getInputStream();
                                    OutputStream os = client.getOutputStream();
                                    while (!client.isClosed()) {
                                        try {
                                            @NotNull byte[] buf = new byte[200];
                                            while (is.read(buf) != -1) {
                                                @NotNull String text = new String(buf).trim();
                                                LOG.info(Arrays.toString(text.getBytes()));
                                                LOG.info(text);
                                            }
                                            // TODO: Packet handling
                                        } catch (Exception ex) {
                                            LOG.log(Level.SEVERE, null, ex);
                                            client.close();
                                            break;
                                        }
                                    }
                                    LOG.info("Socket closed");
                                } catch (IOException ex) {
                                    Logger.getLogger(CIFSWatcher.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }

                            class Packet {

                                private int header; // \0xFF S M B

                                Packet() {
                                }

                                @NotNull
                                private Packet read(@NotNull InputStream is) throws IOException {
                                    ByteBuffer buf = ByteBuffer.allocate(42); // Average CIFS header size
                                    @NotNull byte[] head = new byte[24];
                                    is.read(head);
                                    LOG.info(Arrays.toString(head));
                                    LOG.info(new String(head));
                                    buf.put(head);
                                    buf.flip();
                                    header = buf.getInt();
                                    command = buf.get();
                                    errorClass = buf.get();
                                    buf.get(); // == 0
                                    errorCode = buf.getShort();
                                    flags = buf.get();
                                    flags2 = buf.getShort();
                                    secure = buf.getLong(); // or padding
                                    tid = buf.getShort(); // Tree ID
                                    pid = buf.getShort(); // Process ID
                                    uid = buf.getShort(); // User ID
                                    mid = buf.getShort(); // Multiplex ID
                                    int wordCount = is.read();
                                    @NotNull byte[] words = new byte[wordCount * 2];
                                    is.read(words);
                                    ByteBuffer wordBuffer = ByteBuffer.wrap(words);
                                    parameterWords = new short[wordCount];
                                    for (int i = 0; i < words.length; i++) {
                                        parameterWords[i] = wordBuffer.getShort();
                                    }
                                    int payloadLength = is.read();
                                    buffer = new byte[payloadLength];
                                    is.read(buffer);
                                    return this;
                                }

                                private byte command;
                                /**
                                 * ERRDOS (0x01) –
                                 * Error is from the
                                 * core DOS operating
                                 * system
                                 * set
                                 * ERRSRV (0x02) –
                                 * Error is generated
                                 * by the server
                                 * network
                                 * file manager
                                 * ERRHRD (0x03) –
                                 * Hardware error
                                 * ERRCMD (0xFF) –
                                 * Command was not
                                 * in the “SMB”
                                 * format
                                 */
                                private byte errorClass;
                                /**
                                 * As specified in
                                 * CIFS1.0 draft
                                 */
                                private short errorCode;
                                /**
                                 * When bit 3 is set
                                 * to ‘1’, all pathnames
                                 * in this particular
                                 * packet must be
                                 * treated as
                                 * caseless
                                 * When bit 3 is set
                                 * to ‘0’, all pathnames
                                 * are case sensitive
                                 */
                                private byte flags;
                                /**
                                 * Bit 0, if set,
                                 * indicates that
                                 * the server may
                                 * return long
                                 * file names in the
                                 * response
                                 * Bit 6, if set,
                                 * indicates that
                                 * any pathname in
                                 * the request is
                                 * a long file name
                                 * Bit 16, if set,
                                 * indicates strings
                                 * in the packet are
                                 * encoded
                                 * as UNICODE
                                 */
                                private short flags2;
                                /**
                                 * Typically zero
                                 */
                                private long secure;
                                private short tid;
                                private byte[] buffer;
                                private short[] parameterWords;
                                private short pid;
                                private short uid;
                                private short mid;

                                @Nullable
                                byte[] getBytes() {
                                    return null;
                                }
                            }
                        }).start();
                    }
                }
            }, "CIFS Server").start();
        } catch (IOException ex) {
            Logger.getLogger(CIFSWatcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(@NotNull String... args) {
        int port = 8000;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        getInstance(port);
    }

    private static CIFSWatcher getInstance(int port) {
        if (instance == null) {
            instance = new CIFSWatcher(port);
        }
        return instance;
    }

    public void addFileChangeListener(FileChangeListener listener) {
        listeners.add(listener);
    }
}
