package com.timepath.vfs.server.ftp

import com.timepath.vfs.MockFile
import com.timepath.vfs.provider.ProviderStub
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.VFile

import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern
import java.util.concurrent.Callable

/**
 * With reference to:
 * http://cr.yp.to/ftp.html
 * http://www.nsftools.com/tips/RawFTP.htm
 * http://www.ipswitch.com/support/ws_ftp-server/guide/v5/a_ftpref3.html
 * http://graham.main.nc.us/~bhammel/graham/ftp.html
 * http://www.codeguru.com/csharp/csharp/cs_network/sockets/article.php/c7409/A-C-FTP-Server.htm
 * Mounting requires CurlFtpFS
 * $ mkdir mnt
 * $ curlftpfs -o umask=0000,uid=1000,gid=1000,allow_other localhost:2121 mnt
 * $ cd mnt
 * $ ls -l
 * $ cd ..
 * $ fusermount -u mnt
 * ncdu
 * press 'a' for apparent size
 *
 * @author TimePath
 */
public class FtpServer
/**
 * Creates a server on the specified address.
 *
 * @param port
 * @param addr If null, listen on all available interfaces
 * @throws java.io.IOException
 */
[throws(javaClass<IOException>())]
(private val port: Int = 2121, private var address: InetAddress? = null) : ProviderStub(), Runnable {
    private val pool = Executors.newFixedThreadPool(10, object : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Executors.defaultThreadFactory().newThread(r)
            t.setDaemon(false)
            return t
        }
    })
    private val nameComparator = object : Comparator<SimpleVFile> {
        override fun compare(o1: SimpleVFile, o2: SimpleVFile): Int {
            return o1.name compareTo o2.name
        }
    }
    private var servsock: ServerSocket? = null

    {
        bind()
    }

    throws(javaClass<IOException>())
    private fun bind() {
        if (address == null) {
            address = InetAddress.getByName(null)
        }
        servsock = ServerSocket(port, 0, address)
    }

    override fun run() {
        LOG.log(Level.INFO, "Listening on {0}:{1}", array<Any>(servsock!!.getInetAddress().getHostAddress(), servsock!!.getLocalPort()))
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                LOG.log(Level.INFO, "FTP server shutting down...")
            }
        })
        while (true) {
            //            LOG.log(Level.INFO, "Waiting for client...");
            try {
                pool.submit(FTPConnection(servsock!!.accept()))
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
            }
        }
    }

    inner class FTPConnection (private val client: Socket) : Runnable {
        private var data: Socket? = null
        private var pasv: ServerSocket? = null
        private var cwd = VFile.SEPARATOR

        {
            LOG.log(Level.FINE, "{0} connected.", client)
        }

        override fun run() {
            try {
                val h = servsock!!.getInetAddress().getAddress()
                val br = BufferedReader(InputStreamReader(client.getInputStream()))
                val pw = PrintWriter(client.getOutputStream(), true)
                out(pw, "220 Welcome")
                var skip: Long = 0
                while (!client.isClosed()) {
                    try {
                        val cmd = `in`(br)
                        if (cmd == null) {
                            break
                        } else if (cmd.toUpperCase().startsWith("GET")) {
                            out(pw, "This is an FTP server.")
                            break
                        }
                        if (cmd.toUpperCase().startsWith("USER")) {
                            out(pw, "331 Please specify the password.")
                        } else if (cmd.toUpperCase().startsWith("PASS")) {
                            out(pw, "230 Login successful.")
                        } else if (cmd.toUpperCase().startsWith("SYST")) {
                            out(pw, "215 UNIX Type: L8")
                        } else if (cmd.toUpperCase().startsWith("PWD")) {
                            val dirKnowable = true
                            if (dirKnowable) {
                                out(pw, "257 \"$cwd")
                            } else {
                                out(pw, "550 Error")
                            }
                        } else if (cmd.toUpperCase().startsWith("TYPE")) {
                            val c = cmd.charAt(5)
                            if (c == 'I') {
                                out(pw, "200 Switching to Binary mode.")
                            } else if (c == 'A') {
                                out(pw, "200 Switching to ASCII mode.")
                            }
                        } else if (cmd.toUpperCase().startsWith("PORT")) {
                            val args = cmd.substring(5).split(",")
                            val sep = "."
                            val dataAddress = "${args[0]}$sep${args[1]}$sep${args[2]}$sep${args[3]}"
                            val dataPort = (Integer.parseInt(args[4]) * 256) + Integer.parseInt(args[5])
                            data = Socket(InetAddress.getByName(dataAddress), dataPort)
                            LOG.log(Level.INFO, "*** Data receiver: {0}", data)
                            out(pw, "200 PORT command successful.")
                        } else if (cmd.toUpperCase().startsWith("EPRT")) {
                            val payload = cmd.substring(5)
                            //                            String delimeter = "\\x" + Integer.toHexString((int)
                            // payload.charAt(0));
                            val delimeter = Pattern.quote(payload.charAt(0).toString())
                            val args = payload.substring(1).split(delimeter)
                            val type = Integer.parseInt(args[0])
                            val dataAddress = args[1]
                            val dataPort = Integer.parseInt(args[2])
                            data = Socket(InetAddress.getByName(dataAddress), dataPort)
                            LOG.log(Level.INFO, "*** Data receiver: {0}", data)
                            out(pw, "200 PORT command successful.")
                        } else if (cmd.toUpperCase().startsWith("PASV")) {
                            if (pasv != null) {
                                pasv!!.close()
                            }
                            pasv = ServerSocket(0)
                            val p = intArray(pasv!!.getLocalPort() / 256, pasv!!.getLocalPort() % 256)
                            val con = java.lang.String.format("%s,%s,%s,%s,%s,%s", h[0].toInt() and 255, h[1].toInt() and 255, h[2].toInt() and 255, h[3].toInt() and 255, p[0] and 255, p[1] and 255)
                            out(pw, "227 Entering Passive Mode ($con).")
                        } else if (cmd.toUpperCase().startsWith("EPSV")) {
                            if (pasv != null) {
                                pasv!!.close()
                            }
                            pasv = ServerSocket(0)
                            val p = pasv!!.getLocalPort()
                            out(pw, "229 Entering Extended Passive Mode (|||$p|).")
                        } else if (cmd.toUpperCase().startsWith("SIZE")) {
                            val req = cmd.substring(5)
                            val ch: String
                            ch = if (req.startsWith(VFile.SEPARATOR)) req else canonicalize("$cwd${VFile.SEPARATOR}$req")
                            val f = query(ch)
                            if ((f == null) || f.isDirectory) {
                                out(pw, "550 Could not get file size.")
                            } else {
                                out(pw, "213 ${f.length}")
                            }
                        } else if (cmd.toUpperCase().startsWith("MODE")) {
                            val modes = array<String>("S", "B", "C")
                            val mode = cmd.substring(5)
                            val has = Arrays.asList<String>(*modes).contains(mode)
                            if (has) {
                                out(pw, "200 Mode set to $mode.")
                            } else {
                                out(pw, "504 Bad MODE command.")
                            }
                        } else if (cmd.toUpperCase().startsWith("CWD") || cmd.toUpperCase().startsWith("CDUP")) {
                            LOG.log(Level.FINE, "Changing from: {0}", cwd)
                            val ch: String
                            if (cmd.toUpperCase().startsWith("CDUP")) {
                                ch = canonicalize("$cwd/src/main")
                            } else {
                                var dir = canonicalize(cmd.substring(4))
                                if (!dir.endsWith(VFile.SEPARATOR)) {
                                    dir += VFile.SEPARATOR
                                }
                                ch = if (dir.startsWith(VFile.SEPARATOR)) dir else canonicalize("$cwd${VFile.SEPARATOR}$dir")
                            }
                            val f = query(ch)
                            if ((f != null) && f.isDirectory) {
                                out(pw, "250 Directory successfully changed.")
                                cwd = ch
                            } else {
                                out(pw, "550 Failed to change directory.")
                            }
                        } else if (cmd.toUpperCase().startsWith("LIST")) {
                            out(pw, "150 Here comes the directory listing.")
                            if (pasv != null) {
                                data = pasv!!.accept()
                            }
                            val out = PrintWriter(data!!.getOutputStream(), true)
                            val v = query(cwd)
                            val files = LinkedList(v!!.list())
                            Collections.sort<SimpleVFile>(files, nameComparator)
                            val executor = Executors.newCachedThreadPool()
                            val lines = arrayOfNulls<String>(files.size()).mapIndexed {(i, s) ->
                                executor.submit(object : Callable<String> {
                                    override fun call() = toFTPString(files[i])
                                })
                            }.forEach {
                                out(out, it.get())
                            }
                            out.close()
                            out(pw, "226 Directory send OK.")
                        } else if (cmd.toUpperCase().startsWith("QUIT")) {
                            out(pw, "221 Goodbye")
                            break
                        } else if (cmd.toUpperCase().startsWith("MDTM")) {
                            val req = cmd.substring(5)
                            val ch: String
                            ch = if (req.startsWith(VFile.SEPARATOR)) req else canonicalize("$cwd${VFile.SEPARATOR}$req")
                            val f = query(ch)
                            val cal = Calendar.getInstance()
                            cal.setTimeInMillis(f!!.lastModified)
                            out(pw, "200 ${mdtm.format(cal.getTime())}")
                        } else if (cmd.toUpperCase().startsWith("REST")) {
                            skip = java.lang.Long.parseLong(cmd.substring(5))
                            out(pw, "350 Skipped $skip bytes")
                        } else if (cmd.toUpperCase().startsWith("RETR")) {
                            val toSkip = skip
                            skip = 0
                            val req = cmd.substring(5)
                            val ch: String
                            ch = if (req.startsWith(VFile.SEPARATOR)) req else canonicalize("$cwd${VFile.SEPARATOR}$req")
                            val f = query(ch)
                            if ((f != null) && !f.isDirectory) {
                                out(pw, "150 Opening BINARY mode data connection for file")
                                if (pasv != null) {
                                    data = pasv!!.accept()
                                }
                                try {
                                    val stream = f.openStream()!!
                                    stream.skip(toSkip)
                                    val os = data!!.getOutputStream()
                                    // anonymous clients seem to request this much and then quit
                                    val buf = ByteArray(131072)
                                    val read: Int
                                    while (true) {
                                        read = stream.read(buf)
                                        if (read < 0) break
                                        os.write(buf, 0, read)
                                        os.flush()
                                    }
                                    stream.close()
                                    os.close()
                                } catch (se: SocketException) {
                                    if (!("Connection reset" == se.getMessage() || "Broken pipe" == se.getMessage())) {
                                        LOG.log(Level.SEVERE, "Error serving $ch", se)
                                    }
                                    break
                                }

                                out(pw, "226 File sent")
                            } else {
                                out(pw, "550 Failed to open file.")
                            }
                        } else if (cmd.toUpperCase().startsWith("DELE")) {
                            out(pw, "550 Permission denied.")
                        } else if (cmd.toUpperCase().startsWith("FEAT")) {
                            out(pw, "211-Features:")
                            val features = array<String>("MDTM", "PASV")
                            Arrays.sort(features)
                            for (feature in features) {
                                out(pw, ' ' + feature)
                            }
                            out(pw, "211 end")
                        } else if (cmd.toUpperCase().startsWith("HELP")) {
                            out(pw, "214-Commands supported:")
                            out(pw, "MDTM PASV")
                            out(pw, "214 End")
                        } else if (cmd.toUpperCase().startsWith("SITE")) {
                            out(pw, "200 Nothing to see here")
                        } else if (cmd.toUpperCase().startsWith("RNFR")) {
                            // Rename file
                            val from = cmd.substring(5)
                            out(pw, "350 Okay")
                            val to = `in`(br)!!.substring(5)
                            out(pw, "250 Renamed")
                        } else if (cmd.toUpperCase().startsWith("MKD")) {
                            val folder = cmd.substring(4)
                            val f = query(folder)
                            if ((f != null) && f.isDirectory) {
                                out(pw, "550 Failed to create directory. (it exists)")
                            } else {
                                out(pw, "200 created directory.")
                            }
                            files.put(folder, MockFile(folder, null))
                        } else if (cmd.toUpperCase().startsWith("STOR")) {
                            // Upload file
                            val file = cmd.substring(5)
                            out(pw, "150 Entering Transfer Mode")
                            if (pasv != null) {
                                data = pasv!!.accept()
                            }
                            val `in` = BufferedReader(InputStreamReader(data!!.getInputStream()))
                            val out = PrintWriter(data!!.getOutputStream(), true)
                            var text = ""
                            `in`.forEachLine { line ->
                                LOG.log(Level.FINE, "=== {0}", line)
                                if (text.isEmpty()) {
                                    text = line
                                } else {
                                    text += "\r\n$line"
                                }
                            }
                            data!!.close()
                            val f = MockFile(file, text)
                            files.put(file, f)
                            fileModified(f)
                            LOG.log(Level.INFO, "***\r\n{0}", text)
                            out(pw, "226 File uploaded successfully")
                        } else if (cmd.toUpperCase().startsWith("NOOP")) {
                            out(pw, "200 NOOP ok.")
                        } else if (cmd.toUpperCase().startsWith("OPTS")) {
                            val args = cmd.toUpperCase().substring(5).split(" ")
                            val opt = args[0]
                            val status = args[1]
                            out(pw, "200 $opt always $status.")
                        } else {
                            LOG.log(Level.WARNING, "Unsupported operation {0}", cmd)
                            out(pw, "502 ${cmd.split(" ")[0]} not implemented.")
                            //                            out(pw, "500 Unknown command.");
                        }
                    } catch (ex: Exception) {
                        LOG.log(Level.SEVERE, null, ex)
                        break
                    }

                }
                client.close()
                LOG.log(Level.FINE, "{0} closed.", client)
            } catch (ex: IOException) {
                Logger.getLogger(javaClass<FtpServer>().getName()).log(Level.SEVERE, null, ex)
            }

        }

        private fun canonicalize(string: String): String {
            var string = string
            if (string.endsWith(VFile.SEPARATOR)) {
                string = string.substring(0, string.length() - 1)
            }
            val split = string.split(VFile.SEPARATOR)
            val pieces = LinkedList<String>()
            for (s in split) {
                if (s.isEmpty()) {
                } else if (".." == s) {
                    if (pieces.size() > 2) {
                        pieces.remove(pieces.size() - 1)
                    }
                } else {
                    pieces.add(s)
                }
            }
            val sb = StringBuilder()
            for (s in pieces) {
                sb.append(VFile.SEPARATOR).append(s)
            }
            val ret = sb.toString()
            LOG.log(Level.FINE, ret)
            return ret
        }
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<FtpServer>().getName())
        private val mdtm = SimpleDateFormat("yyyyMMddhhmmss")

        private fun toFTPString(file: SimpleVFile): String {
            var spec = '-' // TODO: links
            val f = array<CharArray>(charArray('r', '-', '-'), charArray('r', '-', '-'), charArray('r', '-', '-')) // RWX: User, Group, Everybody
            if (file.isDirectory) {
                spec = 'd'
                f[0][1] = 'w'
                f[0][2] = 'x'
            }
            val fileSize = file.length
            val perms = "${spec.toString()}${f[0][0]}${f[0][1]}${f[0][2]}${f[1][0]}${f[1][1]}${f[1][2]}${f[2][0]}${f[2][1]}${f[2][2]}"
            val sb = StringBuilder()
            sb.append(perms)
            sb.append(' ')
            sb.append(java.lang.String.format("%4s", fileSize)) // >= 4 left
            sb.append(' ')
            sb.append(java.lang.String.format("%-8s", file.owner)) // >= 8 right
            sb.append(' ')
            sb.append(java.lang.String.format("%-8s", file.group)) // >= 8 right
            sb.append(' ')
            sb.append(java.lang.String.format("%8s", fileSize)) // >= 8 left
            sb.append(' ')
            val cal = Calendar.getInstance()
            val y1 = cal[Calendar.YEAR]
            cal.setTimeInMillis(file.lastModified)
            val y2 = cal[Calendar.YEAR]
            val sameYear = "MMM d HH:mm"
            val diffYear = "MMM d yyyy"
            val df = SimpleDateFormat(if ((y1 == y2)) sameYear else diffYear)
            sb.append(df.format(cal.getTime()))
            sb.append(' ')
            sb.append(file.name)
            return sb.toString()
        }

        throws(javaClass<IOException>())
        private fun `in`(`in`: BufferedReader): String? {
            val s = `in`.readLine()
            LOG.log(Level.FINE, "<<< {0}", s)
            return s
        }

        private fun out(out: PrintWriter, cmd: String) {
            out.print("$cmd\r\n")
            out.flush()
            LOG.log(Level.FINE, ">>> {0}", cmd)
        }
    }
}
