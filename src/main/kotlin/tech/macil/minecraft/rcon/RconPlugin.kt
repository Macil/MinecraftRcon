package tech.macil.minecraft.rcon

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import tech.macil.minecraft.rcon.util.BlockerCounter
import tech.macil.minecraft.rcon.util.ByteArrayQueue

import java.io.*
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.*
import javax.servlet.ServletOutputStream
import kotlin.concurrent.thread

class RconPlugin : JavaPlugin() {
    private var webServer: WebServer? = null
    private val disablingCf = CompletableFuture<Unit>()
    private val runningRequestsCounter = BlockerCounter()

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
        // Signal to any current requests that the plugin is shutting down.
        disablingCf.complete(null)

        // Wait on any current requests to finish before killing the webserver.
        runningRequestsCounter.await(Duration.ofSeconds(10).toMillis())

        val webServer = this.webServer
        if (webServer != null) {
            this.webServer = null
            webServer.stop()
        }
    }

    private fun handleCommand(command: String, output: ServletOutputStream, remoteAddr: String) {
        runningRequestsCounter.acquire().use {
            logger.log(Level.INFO, "rcon($remoteAddr): $command")

            val (queuedInput, queuedOutput) = ByteArrayQueue.makePair()

            val writerThread = thread {
                output.use {
                    queuedInput.use {
                        val buffer = ByteArray(4096)
                        while (true) {
                            val len = queuedInput.read(buffer)
                            if (len < 0) break
                            output.write(buffer, 0, len)
                        }
                    }
                }
            }

            PrintWriter(queuedOutput).use { queuedPrintWriter ->
                val appender = RconAppender(queuedPrintWriter)
                try {
                    (LogManager.getRootLogger() as Logger).addAppender(appender)
                    try {
                        val dispatchCf = CompletableFuture<Unit>()
                        server.scheduler.callSyncMethod(this) {
                            server.dispatchCommand(Bukkit.getConsoleSender(), command)
                            dispatchCf.complete(null)
                        }
                        // If the command causes this plugin to unload, then dispatchCommand()
                        // will call into onDisable() which will call webServer.stop() which
                        // will wait on this thread to end, so we need to bail on waiting on
                        // the dispatchCf future if onDisable() is called.
                        CompletableFuture.anyOf(dispatchCf, disablingCf).get()

                        waitForLogsToFlush()
                    } catch (e: Exception) {
                        e.printStackTrace(queuedPrintWriter)
                    }
                } finally {
                    (LogManager.getRootLogger() as Logger).removeAppender(appender)
                }
            }
            writerThread.join()
        }
    }

    private fun waitForLogsToFlush() {
        if (disablingCf.isDone) return

        // It seems like the output of some commands is only delivered to our appender
        // asynchronously. I couldn't find a direct way to wait on whatever loggers involved
        // to flush, but I found that just waiting on the next game tick seems to (maybe just
        // mostly) solve the problem in practice.

        // There's a similar issue that some plugins' commands (especially any plugins using a
        // database) don't output any results until some unknown time later. This doesn't help
        // much for those, and I'm not sure if I intend to address that.

        // The timeout is because when the server is shutting down, the scheduled method never
        // runs.
        try {
            server.scheduler.callSyncMethod(this) { null }.get(500, TimeUnit.MILLISECONDS)
        } catch (e: CancellationException) {
            // pass
        } catch (e: TimeoutException) {
            // pass
        }
    }
}
