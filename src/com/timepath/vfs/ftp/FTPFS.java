package com.timepath.vfs.ftp;

import com.timepath.vfs.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * With reference to:
 * http://cr.yp.to/ftp.html
 * http://www.nsftools.com/tips/RawFTP.htm
 * http://www.ipswitch.com/support/ws_ftp-server/guide/v5/a_ftpref3.html
 * http://graham.main.nc.us/~bhammel/graham/ftp.html
 * http://www.codeguru.com/csharp/csharp/cs_network/sockets/article.php/c7409/A-C-FTP-Server.htm
 *
 * Mounting requires CurlFtpFS
 * $ mkdir mnt
 * $ curlftpfs -o umask=0000,uid=1000,gid=1000,allow_other localhost:2121 mnt
 * $ cd mnt
 * $ ls -l
 * $ cd ..
 * $ fusermount -u mnt
 * <p/>
 * ncdu
 * press 'a' for apparent size
 *
 * @author TimePath
 */
public class FTPFS extends VFSStub implements Runnable {

    private static final Logger LOG = Logger.getLogger(FTPFS.class.getName());

    private ExecutorService pool = Executors.newFixedThreadPool(10, new ThreadFactory() {

        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(false);
            return t;
        }
    });

    public FTPFS() throws IOException {
        this(2121);
    }

    /**
     * Creates a local-only server
     * <p>
     * @param port
     *             <p>
     * @throws java.io.IOException
     */
    public FTPFS(int port) throws IOException {
        this(port, null); // cannot use java7 InetAddress.getLoopbackAddress(). On windows, this prevents firewall warnings. It's also good for security in general
    }

    /**
     * Creates a server on the specified address.
     * <p>
     * @param port
     * @param addr If null, listen on all available interfaces
     * <p>
     * @throws java.io.IOException
     */
    public FTPFS(int port, InetAddress addr) throws IOException {
        this.port = port;
        this.address = addr;
        this.bind();
    }

    private static String toFTPString(SimpleVFile file) {
        char spec = '-'; // TODO: links
        char[][] f = {{'r', '-', '-'}, {'r', '-', '-'}, {'r', '-', '-'}}; // RWX: User, Group, Everybody
        if(file.isDirectory()) {
            spec = 'd';
            f[0][1] = 'w';
            f[0][2] = 'x';
        }
        long fileSize = file.length();

        String perms = "" + spec
                           + f[0][0] + f[0][1] + f[0][2]
                           + f[1][0] + f[1][1] + f[1][2]
                           + f[2][0] + f[2][1] + f[2][2];

        StringBuilder sb = new StringBuilder();
        sb.append(perms);
        sb.append(" ");
        sb.append(String.format("%4s", fileSize)); // >= 4 left
        sb.append(" ");
        sb.append(String.format("%-8s", file.owner())); // >= 8 right
        sb.append(" ");
        sb.append(String.format("%-8s", file.group())); // >= 8 right
        sb.append(" ");
        sb.append(String.format("%8s", fileSize)); // >= 8 left
        sb.append(" ");
        Calendar cal = Calendar.getInstance();

        int y1 = cal.get(Calendar.YEAR);

        cal.setTimeInMillis(file.lastModified());
        int y2 = cal.get(Calendar.YEAR);

        String sameYear = "MMM d HH:mm";
        String diffYear = "MMM d yyyy";

        SimpleDateFormat df = new SimpleDateFormat(y1 == y2 ? sameYear : diffYear);

        sb.append(df.format(cal.getTime()));
        sb.append(" ");
        sb.append(file.getName());
        String str = sb.toString();

        return str;
    }

    public static void main(String[] args) throws IOException {
        FTPFS f = new FTPFS(2121, null);
        f.add(new MockFile("test.txt", "It works!"));
        f.add(new MockFile("world.txt", "Hello world"));
        f.run();
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

    /**
     * Invoked when a file is modified
     * <p>
     * @param f
     */
    protected void fileModified(SimpleVFile f) {
        for(FileChangeListener listener : listeners) {
            listener.fileModified(f);
        }
    }

    private InetAddress address;

    private int port;

    private ServerSocket servsock;

    private void bind() throws IOException {
        if(address == null) {
            address = InetAddress.getByName(null);
        }
        servsock = new ServerSocket(port, 0, address);
    }

    public void run() {

        LOG.log(Level.INFO, "Listening on {0}:{1}", new Object[] {
            servsock.getInetAddress().getHostAddress(), servsock.getLocalPort()});

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                LOG.log(Level.INFO, "FTP server shutting down...");
            }
        });
        for(;;) {
//            LOG.log(Level.INFO, "Waiting for client...");
            try {
                pool.submit(new FTPConnection(servsock.accept()));

            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    private static final DateFormat mdtm = new SimpleDateFormat("yyyyMMddhhmmss");

    private Comparator<SimpleVFile> nameComparator = new Comparator<SimpleVFile>() {

        public int compare(SimpleVFile o1, SimpleVFile o2) {
            return o1.getName().compareTo(o2.getName());

        }
    };

    private class FTPConnection implements Runnable {

        private final Socket client;

        private Socket data;

        private ServerSocket pasv;

        private String cwd = "/";

        private FTPConnection(Socket s) {
            LOG.log(Level.FINE, "{0} connected.", s);
            client = s;
        }

        public void run() {
            try {
                byte[] h = servsock.getInetAddress().getAddress();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
                PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
                out(pw, "220 Welcome");
                long skip = 0;
                while(!client.isClosed()) {
                    try {
                        String cmd = in(br);
                        if(cmd == null) {
                            break;
                        } else if(cmd.toUpperCase().startsWith("GET")) {
                            out(pw, "This is an FTP server.");
                            break;
                        }

                        if(cmd.toUpperCase().startsWith("USER")) {
                            out(pw, "331 Please specify the password.");
                        } else if(cmd.toUpperCase().startsWith("PASS")) {
                            out(pw, "230 Login successful.");
                        } else if(cmd.toUpperCase().startsWith("SYST")) {
                            out(pw, "215 UNIX Type: L8");
                        } else if(cmd.toUpperCase().startsWith("PWD")) {
                            boolean dirKnowable = true;
                            if(dirKnowable) {
                                out(pw, "257 \"" + cwd + "\"");
                            } else {
                                out(pw, "550 Error");
                            }
                        } else if(cmd.toUpperCase().startsWith("TYPE")) {
                            char c = cmd.charAt(5);
                            if(c == 'I') {
                                out(pw, "200 Switching to Binary mode.");
                            } else if(c == 'A') {
                                out(pw, "200 Switching to ASCII mode.");
                            }
                        } else if(cmd.toUpperCase().startsWith("PORT")) {
                            String[] args = cmd.substring(5).split(",");
                            String sep = ".";
                            String dataAddress = args[0] + sep + args[1] + sep + args[2] + sep
                                                     + args[3];
                            int dataPort = (Integer.parseInt(args[4]) * 256) + Integer.parseInt(
                                args[5]);
                            data = new Socket(InetAddress.getByName(dataAddress), dataPort);
                            LOG.log(Level.INFO, "*** Data receiver: {0}", data);
                            out(pw, "200 PORT command successful.");
                        } else if(cmd.toUpperCase().startsWith("EPRT")) {
                            String payload = cmd.substring(5);
//                            String delimeter = "\\x" + Integer.toHexString((int) payload.charAt(0));
                            String delimeter = Pattern.quote(payload.charAt(0) + "");
                            String[] args = payload.substring(1).split(delimeter);
                            int type = Integer.parseInt(args[0]);
                            String dataAddress = args[1];
                            int dataPort = Integer.parseInt(args[2]);
                            data = new Socket(InetAddress.getByName(dataAddress), dataPort);
                            LOG.log(Level.INFO, "*** Data receiver: {0}", data);
                            out(pw, "200 PORT command successful.");
                        } else if(cmd.toUpperCase().startsWith("PASV")) {
                            if(pasv != null) {
                                pasv.close();
                            }
                            pasv = new ServerSocket(0);
                            int[] p = {pasv.getLocalPort() / 256,
                                       pasv.getLocalPort() % 256};
                            String con = String.format("%s,%s,%s,%s,%s,%s",
                                                       h[0] & 0xFF, h[1] & 0xFF,
                                                       h[2] & 0xFF, h[3] & 0xFF,
                                                       p[0] & 0xFF, p[1] & 0xFF);
                            out(pw, "227 Entering Passive Mode (" + con + ").");
                        } else if(cmd.toUpperCase().startsWith("EPSV")) {
                            if(pasv != null) {
                                pasv.close();
                            }
                            pasv = new ServerSocket(0);
                            int p = pasv.getLocalPort();
                            out(pw, "229 Entering Extended Passive Mode (|||" + p + "|).");
                        } else if(cmd.toUpperCase().startsWith("SIZE")) {
                            String req = cmd.substring(5);
                            String ch;
                            if(req.startsWith("/")) {
                                ch = req;
                            } else {
                                ch = canonicalize(cwd + "/" + req);
                            }
                            SimpleVFile f = get(ch);
                            if(f == null || f.isDirectory()) {
                                out(pw, "550 Could not get file size.");
                            } else {
                                out(pw, "213 " + f.length());
                            }
                        } else if(cmd.toUpperCase().startsWith("MODE")) {
                            String[] modes = new String[] {"S", "B", "C"};
                            String mode = cmd.substring(5);
                            boolean has = Arrays.asList(modes).contains(mode);
                            if(has) {
                                out(pw, "200 Mode set to " + mode + ".");
                            } else {
                                out(pw, "504 Bad MODE command.");
                            }
                        } else if(cmd.toUpperCase().startsWith("CWD") || cmd.toUpperCase().startsWith("CDUP")) {
                            LOG.log(Level.FINE, "Changing from: {0}", cwd);
                            String ch;
                            if(cmd.toUpperCase().startsWith("CDUP")) {
                                ch = canonicalize(cwd + "/..");
                            } else {
                                String dir = canonicalize(cmd.substring(4));
                                if(dir.length() > 1) {
                                    dir = dir.substring(1);
                                }
                                if(!dir.endsWith("/")) {
                                    dir += "/";
                                }
                                if(dir.startsWith("/")) {
                                    ch = dir;
                                } else {
                                    ch = canonicalize(cwd + "/" + dir);
                                }
                            }
                            SimpleVFile f = get(ch);
                            if(f != null && f.isDirectory()) {
                                out(pw, "250 Directory successfully changed.");
                                cwd = ch;
                            } else {
                                out(pw, "550 Failed to change directory.");
                            }
                        } else if(cmd.toUpperCase().startsWith("LIST")) {
                            out(pw, "150 Here comes the directory listing.");
                            if(pasv != null) {
                                data = pasv.accept();
                            }
                            PrintWriter out = new PrintWriter(data.getOutputStream(), true);

                            SimpleVFile v = get(cwd);
                            final List<SimpleVFile> files = new LinkedList<SimpleVFile>(v.list());
                            Collections.sort(files, nameComparator);

                            ExecutorService executor = Executors.newCachedThreadPool();
                            final CountDownLatch cdl = new CountDownLatch(files.size());
                            final String[] lines = new String[files.size()];
                            for(int i = 0; i < lines.length; i++) {
                                final int j = i;
                                executor.submit(new Runnable() {
                                    public void run() {
                                        lines[j] = toFTPString(files.get(j));
                                        cdl.countDown();
                                    }
                                });
                            }
                            cdl.await();
                            
                            for(String line : lines) {
                                out(out, line);
                            }

                            out.close();
                            out(pw, "226 Directory send OK.");
                        } else if(cmd.toUpperCase().startsWith("QUIT")) {
                            out(pw, "221 Goodbye");
                            break;
                        } else if(cmd.toUpperCase().startsWith("MDTM")) {
                            String req = cmd.substring(5);
                            String ch;
                            if(req.startsWith("/")) {
                                ch = req;
                            } else {
                                ch = canonicalize(cwd + "/" + req);
                            }
                            SimpleVFile f = get(ch);
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(f.lastModified());
                            out(pw, "200 " + mdtm.format(cal.getTime()));
                        } else if(cmd.toUpperCase().startsWith("REST")) {
                            skip = Long.parseLong(cmd.substring(5));
                            out(pw, "350 Skipped " + skip + " bytes");
                        } else if(cmd.toUpperCase().startsWith("RETR")) {
                            long toSkip = skip;
                            skip = 0;

                            String req = cmd.substring(5);
                            String ch;
                            if(req.startsWith("/")) {
                                ch = req;
                            } else {
                                ch = canonicalize(cwd + "/" + req);
                            }
                            SimpleVFile f = get(ch);
                            if(f != null && !f.isDirectory()) {
                                out(pw, "150 Opening BINARY mode data connection for file");
                                if(pasv != null) {
                                    data = pasv.accept();
                                }
                                try {
                                    InputStream is = f.stream();
                                    is.skip(toSkip);
                                    OutputStream os = data.getOutputStream();
                                    // anonymous clients seem to request this much and then quit
                                    byte[] buf = new byte[131072];
                                    int read;
                                    while((read = is.read(buf)) > -1) {
                                        os.write(buf, 0, read);
                                        os.flush();
                                    }
                                    is.close();
                                    os.close();
                                } catch(SocketException se) {
                                    if(!("Connection reset".equals(se.getMessage())
                                         || "Broken pipe".equals(se.getMessage()))) {
                                        LOG.log(Level.SEVERE, "Error serving " + ch, se);
                                    }
                                    break;
                                }
                                out(pw, "226 File sent");
                            } else {
                                out(pw, "550 Failed to open file.");
                            }
                        } else if(cmd.toUpperCase().startsWith("DELE")) {
                            out(pw, "550 Permission denied.");
                        } else if(cmd.toUpperCase().startsWith("FEAT")) {
                            out(pw, "211-Features:");
                            String[] features = {"MDTM", "PASV"};
                            Arrays.sort(features);
                            for(String feature : features) {
                                out(pw, " " + feature);
                            }
                            out(pw, "211 end");
                        } else if(cmd.toUpperCase().startsWith("HELP")) {
                            out(pw, "214-Commands supported:");
                            out(pw, "MDTM PASV");
                            out(pw, "214 End");
                        } else if(cmd.toUpperCase().startsWith("SITE")) {
                            out(pw, "200 Nothing to see here");
                        } else if(cmd.toUpperCase().startsWith("RNFR")) {
                            //<editor-fold defaultstate="collapsed" desc="Rename file">
                            String from = cmd.substring(5);
                            out(pw, "350 Okay");
                            String to = in(br).substring(5);
                            out(pw, "250 Renamed");
                            //</editor-fold>
                        } else if(cmd.toUpperCase().startsWith("MKD")) {
                            String folder = cmd.substring(4);
                            SimpleVFile f = get(folder);
                            if(f != null && f.isDirectory()) {
                                out(pw, "550 Failed to create directory. (it exists)");
                            } else {
                                out(pw, "200 created directory.");
                            }
                            files.put(folder, new MockFile(folder, null));
                        } else if(cmd.toUpperCase().startsWith("STOR")) {
                            //<editor-fold defaultstate="collapsed" desc="Upload file">
                            String file = cmd.substring(5);
                            String text = "";
                            out(pw, "150 Entering Transfer Mode");
                            if(pasv != null) {
                                data = pasv.accept();
                            }
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                data.getInputStream()));
                            PrintWriter out = new PrintWriter(data.getOutputStream(), true);
                            String line;
                            while((line = in.readLine()) != null) {
                                LOG.log(Level.FINE, "=== {0}", line);
                                if(text.length() == 0) {
                                    text = line;
                                } else {
                                    text += "\r\n" + line;
                                }
                            }
                            data.close();
                            SimpleVFile f = new MockFile(file, text);
                            files.put(file, f);
                            fileModified(f);
                            LOG.log(Level.INFO, "***\r\n{0}", text);
                            out(pw, "226 File uploaded successfully");
                            //</editor-fold>
                        } else if(cmd.toUpperCase().startsWith("NOOP")) {
                            out(pw, "200 NOOP ok.");
                        } else if(cmd.toUpperCase().startsWith("OPTS")) {
                            String[] args = cmd.toUpperCase().substring(5).split(" ");
                            String opt = args[0];
                            String status = args[1];
                            out(pw, "200 " + opt + " always " + status + ".");
                        } else {
                            LOG.log(Level.WARNING, "Unsupported operation {0}", cmd);
                            out(pw, "502 " + cmd.split(" ")[0] + " not implemented.");
//                            out(pw, "500 Unknown command.");
                        }
                    } catch(Exception ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        break;
                    }
                }
                client.close();
                LOG.log(Level.FINE, "{0} closed.", client);
            } catch(IOException ex) {
                Logger.getLogger(FTPFS.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private String canonicalize(String string) {
            if(string.endsWith("/")) {
                string = string.substring(0, string.length() - 1);
            }
            String[] split = string.split("/");
            List<String> pieces = new LinkedList<String>();
            for(String s : split) {
                if(s.length() == 0) {
                } else if(s.equals("..")) {
                    if(pieces.size() > 2) {
                        pieces.remove(pieces.size() - 1);
                    }
                } else {
                    pieces.add(s);
                }
            }
            StringBuilder sb = new StringBuilder();
            for(String s : pieces) {
                sb.append("/").append(s);
            }
            String ret = sb.toString();
            LOG.log(Level.FINE, ret);
            return ret;
        }

    }

}
