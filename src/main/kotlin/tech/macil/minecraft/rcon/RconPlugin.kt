package tech.macil.minecraft.rcon

import com.google.common.base.Charsets
import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import tech.macil.minecraft.rcon.util.NLRequiringBufferedReader

import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.logging.*

class RconPlugin : JavaPlugin() {
    companion object {
        private const val SOCKET_BACKLOG = 20
        private const val THREAD_COUNT = 6
        private const val EXPECTED_GREETING = "Minecraft-Rcon"
        private val connectionHandler = Executors.newWorkStealingPool(THREAD_COUNT)
        private val outputFlusher = Executors.newWorkStealingPool(THREAD_COUNT)
    }

    private var socket: ServerSocket? = null

    override fun onEnable() {
        saveDefaultConfig()

        Metrics(this)

        val listenAddress = config.getString("listenAddress")
        val port = config.getInt("port")

        socket = if (listenAddress == "all") {
            ServerSocket(port, SOCKET_BACKLOG)
        } else {
            ServerSocket(port, SOCKET_BACKLOG, InetAddress.getByName(listenAddress))
        }

        Thread {
            try {
                while (true) {
                    val connection = socket!!.accept()
                    connectionHandler.execute(ClientConnectionRunnable(connection))
                }
            } catch (e: SocketException) {
                // ignore, happens when socket is closed
            } catch (e: IOException) {
                logger.log(Level.SEVERE, "Unknown error in server thread", e)
            }
        }.start()
    }

    override fun onDisable() {
        if (socket != null) {
            try {
                socket!!.close()
            } finally {
                socket = null
            }
        }
    }

    private inner class ClientConnectionRunnable(private val connection: Socket) : Runnable {

        override fun run() {
            try {
                try {
                    // Use NLRequiringBufferedReader so if the connection dies part way through, we don't
                    // execute half of a command.
                    val input = NLRequiringBufferedReader(
                            InputStreamReader(connection.getInputStream(), Charsets.UTF_8))
                    val outputPrintWriter = PrintWriter(connection.getOutputStream(), false)

                    val greeting = input.readLine()
                    if (EXPECTED_GREETING != greeting) {
                        outputPrintWriter.write("Bad header\n")
                        outputPrintWriter.flush()
                        return
                    }

                    OffThreadWriter(
                            outputPrintWriter,
                            logger,
                            outputFlusher
                    ).use { output ->
                        val appender = RconAppender(connection, output)

                        try {
                            (LogManager.getRootLogger() as Logger).addAppender(appender)

                            input.lines().forEach { line ->
                                logger.log(Level.INFO, "rcon(" + connection.remoteSocketAddress + "): " + line)
                                try {
                                    if (server.scheduler.callSyncMethod(this@RconPlugin
                                            ) { !server.dispatchCommand(Bukkit.getConsoleSender(), line) }.get()) {
                                        output.writeLnWithoutFlush("Command not found")
                                        output.flush()
                                    } else {
                                        waitForLogsToFlush()
                                    }
                                } catch (e: Exception) {
                                    output.writeLnWithoutFlush(ExceptionUtils.getStackTrace(e))
                                    output.flush()
                                }
                            }
                        } finally {
                            (LogManager.getRootLogger() as Logger).removeAppender(appender)
                        }
                    }
                } finally {
                    connection.close()
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Unknown error in connection thread", e)
            }
        }
    }

    private fun waitForLogsToFlush() {
        // It seems like the output of some commands is only delivered to our appender
        // asynchronously. I couldn't find a direct way to wait on whatever loggers involved
        // to flush, but I found that just waiting on the next game tick seems to (maybe just
        // mostly) solve the problem in practice.

        // There's a similar issue that some plugins' commands (especially any plugins using a
        // database) don't output any results until some unknown time later. This doesn't help
        // much for those and I don't really intend for that case to get fixed. It's up to the
        // client to hold the connection open longer in those cases.
        try {
            server.scheduler.callSyncMethod(this) { null }.get()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }
    }
}
