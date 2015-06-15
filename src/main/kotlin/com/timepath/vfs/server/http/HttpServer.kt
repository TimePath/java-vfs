package com.timepath.vfs.server.http

import com.timepath.util.concurrent.DaemonThreadFactory
import com.timepath.vfs.VFile
import com.timepath.vfs.provider.ProviderStub
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger

public class HttpServer(port: Int = 8000, addr: InetAddress? = null)
: ProviderStub(), Runnable {
    private val pool = Executors.newFixedThreadPool(10, DaemonThreadFactory())
    private val servsock: ServerSocket

    init {
        // On windows, this prevents firewall warnings. It's also good for security in general
        val loopback = InetAddress.getByName(null) // cannot use java7 InetAddress.getLoopbackAddress().
        servsock = ServerSocket(port, 0, addr ?: loopback)
        LOG.log(Level.INFO, "Listening on {0}:{1}", arrayOf<Any>(servsock.getInetAddress().getHostAddress(), servsock.getLocalPort()))
        Runtime.getRuntime().addShutdownHook(Thread {
            LOG.info("HTTP server shutting down...")
        })
    }

    override fun run() {
        while (true) {
            try {
                val socket = servsock.accept()
                LOG.log(Level.FINE, "{0} connected.", socket)
                pool.submit(HTTPConnection(socket))
                LOG.log(Level.FINE, "{0} closed.", socket)
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
            }
        }
    }

    inner class HTTPConnection(private val client: Socket) : Runnable {
        override fun run() {
            val input = client.getInputStream().buffered()
            val output = client.getOutputStream().buffered()
            val br = input.reader().buffered()
            val pw = output.writer().buffered().let { PrintWriter(it, true) }
            while (!client.isClosed()) {
                try {
                    val cmd = recv(br)
                    if (cmd == null) {
                        client.close()
                        break
                    } else if (cmd.startsWith("GET")) {
                        val args = cmd.substring(4).splitBy(" ")
                        var req = args[0]
                        val http = args[1]
                        if (req == VFile.SEPARATOR) {
                            req = "/index.html"
                        }
                        LOG.log(Level.FINE, "*** GETing {0}", req)
                        val file = this@HttpServer.query(req)?.openStream()
                        if (file != null) {
                            send(pw, "$http 200 OK")
                            send(pw, "")
                            file.copyTo(output)
                            output.flush()
                            file.close()
                        } else {
                            send(pw, "$http 404 Not Found")
                            send(pw, "")
                        }
                    }
                } catch (ex: Exception) {
                    LOG.log(Level.SEVERE, null, ex)
                }
                client.close()
            }
        }
    }

    companion object {

        internal val LOG = Logger.getLogger(javaClass<HttpServer>().getName())

        internal fun recv(input: BufferedReader): String? {
            val s = input.readLine()
            LOG.log(Level.FINE, "<<< {0}", s)
            return s
        }

        internal fun send(output: PrintWriter, cmd: String) {
            output.print("$cmd\r\n")
            output.flush()
            LOG.log(Level.FINE, ">>> {0}", cmd)
        }
    }
}
