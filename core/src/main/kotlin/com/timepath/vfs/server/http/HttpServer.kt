package com.timepath.vfs.server.http

import com.timepath.util.concurrent.DaemonThreadFactory
import com.timepath.vfs.provider.ProviderStub
import com.timepath.vfs.SimpleVFile

import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger
import com.timepath.vfs.VFile

/**
 * @author TimePath
 */

throws(javaClass<IOException>(), javaClass<UnknownHostException>())
public fun HttpServer(): HttpServer {
    return HttpServer(8000)
}

throws(javaClass<IOException>(), javaClass<UnknownHostException>())
public fun HttpServer(port: Int): HttpServer {
    return HttpServer(port, null)
}

public class HttpServer [throws(javaClass<IOException>(), javaClass<UnknownHostException>())]
(port: Int, addr: InetAddress?) : ProviderStub(), Runnable {
    private val pool = Executors.newFixedThreadPool(10, DaemonThreadFactory())
    private val servsock: ServerSocket

    init {
        // On windows, this prevents firewall warnings. It's also good for security in general
        val loopback = InetAddress.getByName(null) // cannot use java7 InetAddress.getLoopbackAddress().
        servsock = ServerSocket(port, 0, addr ?: loopback)
        LOG.log(Level.INFO, "Listening on {0}:{1}", array<Any>(servsock.getInetAddress().getHostAddress(), servsock.getLocalPort()))
        Runtime.getRuntime().addShutdownHook(Thread {
            LOG.info("HTTP server shutting down...")
        })
    }

    override fun run() {
        while (true) {
            try {
                pool.submit(HTTPConnection(servsock.accept()))
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
            }

        }
    }

    private inner class HTTPConnection (private val client: Socket) : Runnable {

        init {
            LOG.log(Level.FINE, "{0} connected.", client)
        }

        override fun run() {
            try {
                val `is` = BufferedInputStream(client.getInputStream())
                val os = BufferedOutputStream(client.getOutputStream())
                val br = BufferedReader(InputStreamReader(`is`))
                val pw = PrintWriter(os, true)
                while (!client.isClosed()) {
                    try {
                        val cmd = `in`(br)
                        if (cmd == null) {
                            client.close()
                            break
                        } else if (cmd.startsWith("GET")) {
                            val args = cmd.substring(4).split(" ")
                            var req = args[0]
                            val http = args[1]
                            if (req == VFile.SEPARATOR) {
                                req = "/index.html"
                            }
                            val file = query(req)
                            LOG.log(Level.FINE, "*** GETing {0}", req)
                            if (file != null) {
                                val stream = file.openStream()
                                if (stream != null) {
                                    out(pw, "$http 200 OK")
                                    out(pw, "")
                                    stream.copyTo(os)
                                    os.flush()
                                    stream.close()
                                }
                            } else {
                                out(pw, "$http 404 Not Found")
                                out(pw, "")
                            }
                        }
                    } catch (ex: Exception) {
                        LOG.log(Level.SEVERE, null, ex)
                    }

                    client.close()
                }
                LOG.log(Level.FINE, "{0} closed.", client)
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
            }

        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<HttpServer>().getName())

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
