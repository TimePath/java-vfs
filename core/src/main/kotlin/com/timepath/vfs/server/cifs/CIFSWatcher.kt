package com.timepath.vfs.server.cifs

import com.timepath.vfs.FileChangeListener

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger

/**
 * http://www.codefx.com/CIFS_Explained.htm
 * smbclient --ip-address=localhost --port=8000 -M hi
 *
 * @author TimePath
 */
class CIFSWatcher private(port: Int) {
    private val listeners = LinkedList<FileChangeListener>()

    ;{
        var port = port
        try {
            // On windows, the loopback address does not prompt the firewall
            // Also good for security in general
            val sock = ServerSocket(port, 0, InetAddress.getLoopbackAddress())
            port = sock.getLocalPort()
            LOG.log(Level.INFO, "Listening on port {0}", port)
            Runtime.getRuntime().addShutdownHook(Thread(object : Runnable {
                override fun run() {
                    LOG.info("CIFS server shutting down...")
                }
            }))
            Thread(object : Runnable {
                private val data: Socket? = null
                private val pasv: ServerSocket? = null

                override fun run() {
                    while (true) {
                        val client: Socket
                        try {
                            LOG.info("Waiting for client...")
                            client = sock.accept()
                            LOG.info("Connected")
                        } catch (ex: IOException) {
                            Logger.getLogger(javaClass<CIFSWatcher>().getName()).log(Level.SEVERE, null, ex)
                            continue
                        }

                        Thread(object : Runnable {
                            override fun run() {
                                try {
                                    val `is` = client.getInputStream()
                                    val os = client.getOutputStream()
                                    while (!client.isClosed()) {
                                        try {
                                            val buf = ByteArray(200)
                                            while (`is`.read(buf) != -1) {
                                                val text = String(buf).trim()
                                                LOG.info(Arrays.toString(text.getBytes()))
                                                LOG.info(text)
                                            }
                                            // TODO: Packet handling
                                        } catch (ex: Exception) {
                                            LOG.log(Level.SEVERE, null, ex)
                                            client.close()
                                            break
                                        }

                                    }
                                    LOG.info("Socket closed")
                                } catch (ex: IOException) {
                                    Logger.getLogger(javaClass<CIFSWatcher>().getName()).log(Level.SEVERE, null, ex)
                                }

                            }

                            inner class Packet {

                                private var header: Int = 0 // \0xFF S M B

                                throws(javaClass<IOException>())
                                private fun read(`is`: InputStream): Packet {
                                    val buf = ByteBuffer.allocate(42) // Average CIFS header size
                                    val head = ByteArray(24)
                                    `is`.read(head)
                                    LOG.info(Arrays.toString(head))
                                    LOG.info(String(head))
                                    buf.put(head)
                                    buf.flip()
                                    header = buf.getInt()
                                    command = buf.get()
                                    errorClass = buf.get()
                                    buf.get() // == 0
                                    errorCode = buf.getShort()
                                    flags = buf.get()
                                    flags2 = buf.getShort()
                                    secure = buf.getLong() // or padding
                                    tid = buf.getShort() // Tree ID
                                    pid = buf.getShort() // Process ID
                                    uid = buf.getShort() // User ID
                                    mid = buf.getShort() // Multiplex ID
                                    val wordCount = `is`.read()
                                    val words = ByteArray(wordCount * 2)
                                    `is`.read(words)
                                    val wordBuffer = ByteBuffer.wrap(words)
                                    val arr = ShortArray(wordCount)
                                    parameterWords = arr
                                    for (i in words.indices) {
                                        arr[i] = wordBuffer.getShort()
                                    }
                                    val payloadLength = `is`.read()
                                    buffer = ByteArray(payloadLength)
                                    `is`.read(buffer)
                                    return this
                                }

                                private var command: Byte = 0
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
                                private var errorClass: Byte = 0
                                /**
                                 * As specified in
                                 * CIFS1.0 draft
                                 */
                                private var errorCode: Short = 0
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
                                private var flags: Byte = 0
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
                                private var flags2: Short = 0
                                /**
                                 * Typically zero
                                 */
                                private var secure: Long = 0
                                private var tid: Short = 0
                                private var buffer: ByteArray? = null
                                private var parameterWords: ShortArray? = null
                                private var pid: Short = 0
                                private var uid: Short = 0
                                private var mid: Short = 0

                                fun getBytes(): ByteArray? {
                                    return null
                                }
                            }
                        }).start()
                    }
                }
            }, "CIFS Server").start()
        } catch (ex: IOException) {
            Logger.getLogger(javaClass<CIFSWatcher>().getName()).log(Level.SEVERE, null, ex)
        }

    }

    public fun addFileChangeListener(listener: FileChangeListener) {
        listeners.add(listener)
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<CIFSWatcher>().getName())
        private var instance: CIFSWatcher? = null

        public fun main(args: Array<String>) {
            var port = 8000
            if (args.size() >= 1) {
                port = Integer.parseInt(args[0])
            }
            getInstance(port)
        }

        private fun getInstance(port: Int): CIFSWatcher {
            if (instance == null) {
                instance = CIFSWatcher(port)
            }
            return instance!!
        }
    }
}

fun main(args: Array<String>) = CIFSWatcher.main(args)
