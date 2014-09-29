package com.timepath.vfs.server.http;

import com.timepath.util.concurrent.DaemonThreadFactory;
import com.timepath.vfs.provider.ProviderStub;
import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class HttpServer extends ProviderStub implements Runnable {

    private static final Logger LOG = Logger.getLogger(HttpServer.class.getName());
    private final ExecutorService pool = Executors.newFixedThreadPool(10, new DaemonThreadFactory());
    @NotNull
    private final ServerSocket servsock;

    public HttpServer() throws IOException, UnknownHostException {
        this(8000);
    }

    public HttpServer(int port) throws IOException, UnknownHostException {
        this(port, null);
    }

    private HttpServer(int port, @Nullable InetAddress addr) throws IOException, UnknownHostException {
        if (addr == null) { // On windows, this prevents firewall warnings. It's also good for security in general
            addr = InetAddress.getByName(null); // cannot use java7 InetAddress.getLoopbackAddress().
        }
        servsock = new ServerSocket(port, 0, addr);
        LOG.log(Level.INFO, "Listening on {0}:{1}", new Object[]{
                servsock.getInetAddress().getHostAddress(), servsock.getLocalPort()
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOG.info("HTTP server shutting down...");
            }
        }));
    }

    private static String in(@NotNull BufferedReader in) throws IOException {
        String s = in.readLine();
        LOG.log(Level.FINE, "<<< {0}", s);
        return s;
    }

    private static void out(@NotNull PrintWriter out, String cmd) {
        out.print(cmd + "\r\n");
        out.flush();
        LOG.log(Level.FINE, ">>> {0}", cmd);
    }

    @Override
    public void run() {
        while (true) {
            try {
                pool.submit(new HTTPConnection(servsock.accept()));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
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
                @NotNull BufferedInputStream is = new BufferedInputStream(client.getInputStream());
                @NotNull BufferedOutputStream os = new BufferedOutputStream(client.getOutputStream());
                @NotNull BufferedReader br = new BufferedReader(new InputStreamReader(is));
                @NotNull PrintWriter pw = new PrintWriter(os, true);
                while (!client.isClosed()) {
                    try {
                        String cmd = in(br);
                        if (cmd == null) {
                            client.close();
                            break;
                        } else if (cmd.startsWith("GET")) {
                            @NotNull String[] args = cmd.substring(4).split(" ");
                            String req = args[0];
                            String http = args[1];
                            if (req.equals(SEPARATOR)) {
                                req = "/index.html";
                            }
                            @Nullable SimpleVFile file = query(req);
                            LOG.log(Level.FINE, "*** GETing {0}", req);
                            if (file != null) {
                                InputStream stream = file.openStream();
                                if (stream != null) {
                                    out(pw, http + " 200 OK");
                                    out(pw, "");
                                    @NotNull byte[] buf = new byte[1024 * 8];
                                    int read;
                                    while ((read = stream.read(buf)) > -1) {
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
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                    client.close();
                }
                LOG.log(Level.FINE, "{0} closed.", client);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }
}
