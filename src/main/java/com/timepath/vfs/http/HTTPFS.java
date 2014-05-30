package com.timepath.vfs.http;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.SimpleVFile;
import com.timepath.vfs.VFSStub;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class HTTPFS extends VFSStub implements Runnable {

    private static final Logger          LOG  = Logger.getLogger(HTTPFS.class.getName());
    private final        ExecutorService pool = Executors.newFixedThreadPool(10, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(false);
            return t;
        }
    });
    private final ServerSocket servsock;

    public HTTPFS() throws IOException, UnknownHostException {
        this(8000);
    }

    private HTTPFS(int port) throws IOException, UnknownHostException {
        this(port, null);
    }

    private HTTPFS(int port, InetAddress addr) throws IOException, UnknownHostException {
        if(addr == null) { // On windows, this prevents firewall warnings. It's also good for security in general
            addr = InetAddress.getByName(null); // cannot use java7 InetAddress.getLoopbackAddress().
        }
        servsock = new ServerSocket(port, 0, addr);
        LOG.log(Level.INFO, "Listening on {0}:{1}", new Object[] {
                servsock.getInetAddress().getHostAddress(), servsock.getLocalPort()
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOG.info("HTTP server shutting down...");
            }
        }));
    }

    public static void main(String... args) throws IOException, UnknownHostException {
        HTTPFS httpfs = new HTTPFS(8000);
        httpfs.add(new MockFile("test.txt", "It works!"));
        httpfs.add(new MockFile("world.txt", "Hello world"));
        httpfs.run();
    }

    private static String in(BufferedReader in) throws IOException {
        String s = in.readLine();
        LOG.log(Level.FINE, "<<< {0}", s);
        return s;
    }

    private static void out(PrintWriter out, String cmd) {
        out.print(cmd + "\r\n");
        out.flush();
        LOG.log(Level.FINE, ">>> {0}", cmd);
    }

    private class HTTPConnection implements Runnable {

        private final Socket client;

        private HTTPConnection(Socket socket) {
            LOG.log(Level.FINE, "{0} connected.", socket);
            client = socket;
        }

        @Override
        public void run() {
            try {
                BufferedInputStream is = new BufferedInputStream(client.getInputStream());
                BufferedOutputStream os = new BufferedOutputStream(client.getOutputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                PrintWriter pw = new PrintWriter(os, true);
                while(!client.isClosed()) {
                    try {
                        String cmd = in(br);
                        if(cmd == null) {
                            client.close();
                            break;
                        } else if(cmd.startsWith("GET")) {
                            String[] args = cmd.substring(4).split(" ");
                            String req = args[0];
                            String http = args[1];
                            if(req.equals(SEPARATOR)) {
                                req = "/index.html";
                            }
                            SimpleVFile file = query(req);
                            LOG.log(Level.FINE, "*** GETing {0}", req);
                            if(file != null) {
                                InputStream stream = file.openStream();
                                if(stream != null) {
                                    out(pw, http + " 200 OK");
                                    out(pw, "");
                                    byte[] buf = new byte[1024 * 8];
                                    int read;
                                    while(( read = stream.read(buf) ) > -1) {
                                        os.write(buf, 0, read);
                                        os.flush();
                                    }
                                    stream.close();
                                }
                            } else {
                                out(pw, http + " 404 Not Found");
                                out(pw, "");
                            }
                        }
                    } catch(Exception ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                    client.close();
                }
                LOG.log(Level.FINE, "{0} closed.", client);
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                pool.submit(new HTTPConnection(servsock.accept()));
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }
}
