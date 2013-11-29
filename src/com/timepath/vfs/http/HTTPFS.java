package com.timepath.vfs.http;

import com.timepath.vfs.MockFile;
import com.timepath.vfs.VFSStub;
import com.timepath.vfs.SimpleVFile;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class HTTPFS extends VFSStub implements Runnable {

    private static final Logger LOG = Logger.getLogger(HTTPFS.class.getName());

    public static void main(String... args) throws IOException {
        HTTPFS f = new HTTPFS(8000);
        f.add(new MockFile("test.txt", "It works!"));
        f.add(new MockFile("world.txt", "Hello world"));
        f.run();
    }

    private static String in(BufferedReader in) throws IOException {
        String s = in.readLine();
        LOG.log(Level.INFO, "<<< {0}", s);
        return s;
    }

    private static void out(PrintWriter out, String cmd) {
        out.print(cmd + "\r\n");
        out.flush();
        LOG.log(Level.INFO, ">>> {0}", cmd);
    }

    private final ServerSocket servsock;

    public HTTPFS() throws IOException {
        this(8000);
    }

    public HTTPFS(int port) throws IOException {
        servsock = new ServerSocket(port);//, 0, InetAddress.getByName(null)); // cannot use java7 InetAddress.getLoopbackAddress(). On windows, this prevents firewall warnings. It's also good for security in general
        LOG.log(Level.INFO, "Listening on {0}:{1}", new Object[] {
            InetAddress.getLocalHost().getHostAddress(), servsock.getLocalPort()});

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOG.info("HTTP server shutting down...");
            }
        });
    }

    public void run() {
        for(;;) {
//            LOG.info("Waiting for client...");
            try {
                new Thread(new HTTPConnection(servsock.accept())).start();
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    private class HTTPConnection implements Runnable {

        private final Socket client;

        private HTTPConnection(Socket s) {
            LOG.log(Level.INFO, "{0} connected.", s);
            client = s;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
                while(!client.isClosed()) {
                    try {
                        String cmd = in(br);
                        if(cmd == null) {
                            client.close();
                            break;
                        } else if(cmd.startsWith("GET")) {
                            String[] args = cmd.substring(4).split(" ");
                            String ch = args[0];
                            String http = args[1];
                            if(ch.equals("/")) {
                                ch = "/index.html";
                            }
                            SimpleVFile f = get(ch);
                            LOG.log(Level.INFO, "*** GETing {0}", ch);
                            if(f != null) {
                                InputStream is = f.stream();
                                if(is != null) {
                                    out(pw, http + " 200 OK");
                                    out(pw, "");
                                    OutputStream os = client.getOutputStream();
                                    byte[] buf = new byte[1024 * 8];
                                    int read;
                                    while((read = is.read(buf)) > -1) {
                                        os.write(buf, 0, read);
                                        os.flush();
                                    }
                                    is.close();
                                }
                            } else {
                                out(pw, "404 File not found.");
                            }
                            client.close();
                            break;
                        }
                    } catch(Exception ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        break;
                    }
                }
                LOG.log(Level.INFO, "{0} closed.", client);
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }

    }

}
