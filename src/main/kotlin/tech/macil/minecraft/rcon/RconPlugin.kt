package tech.macil.minecraft.rcon

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import tech.macil.minecraft.rcon.util.ByteArrayQueue
import tech.macil.minecraft.rcon.util.StandardDataStream

import java.io.*
import java.util.logging.*
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream
import kotlin.concurrent.thread

class RconPlugin : JavaPlugin() {
    private var webServer: WebServer? = null

    override fun onEnable() {
        saveDefaultConfig()

        Metrics(this)

        var listenAddress: String? = config.getString("listenAddress")
        if (listenAddress == "all") listenAddress = null
        val port = config.getInt("port")

        val htpasswdFile = File(dataFolder, "htpasswd")
        val authChecker: AuthChecker? = if (htpasswdFile.exists()) {
            logger.log(Level.INFO, "Found htpasswd file, loading it.")
            AuthChecker(htpasswdFile)
        } else {
            null
        }

        val webServer = WebServer(listenAddress, port, this, authChecker, this::handleCommand)
        this.webServer = webServer

        webServer.start()
    }

    override fun onDisable() {
        val webServer = this.webServer
        if (webServer != null) {
            this.webServer = null
            webServer.stop()
        }
    }

    private fun handleCommand(command: String, output: ServletOutputStream, async: AsyncContext) {
        logger.log(Level.INFO, "rcon(${async.request.remoteAddr}): $command")

        val (queuedInput, queuedOutput) = ByteArrayQueue.makePair()

        output.setWriteListener(StandardDataStream(queuedInput, async, output))

        thread {
            PrintWriter(queuedOutput).use { queuedPrintWriter ->
                val appender = RconAppender(queuedPrintWriter)
                try {
                    (LogManager.getRootLogger() as Logger).addAppender(appender)
                    try {
                        server.scheduler.callSyncMethod(this) {
                            server.dispatchCommand(Bukkit.getConsoleSender(), command)
                        }.get()
                        waitForLogsToFlush()
                    } catch (e: Exception) {
                        e.printStackTrace(queuedPrintWriter)
                    }
                } finally {
                    (LogManager.getRootLogger() as Logger).removeAppender(appender)
                }
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
        // much for those, and I'm not sure if I intend to address that.
        server.scheduler.callSyncMethod(this) { null }.get()
    }
}
