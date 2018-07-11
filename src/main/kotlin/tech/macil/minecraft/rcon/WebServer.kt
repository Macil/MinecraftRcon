package tech.macil.minecraft.rcon

import com.google.gson.GsonBuilder
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import tech.macil.minecraft.rcon.web.*
import java.util.*
import javax.servlet.*

class WebServer(
        hostName: String?,
        port: Int,
        plugin: RconPlugin,
        handler: CommandServletHandler
) {
    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private const val MAX_THREADS = 32
    }

    private val server: Server

    init {
        val threadPool = QueuedThreadPool(MAX_THREADS)
        threadPool.isDaemon = true
        server = Server(threadPool)
        val connector = ServerConnector(server)
        connector.host = hostName
        connector.port = port
        server.addConnector(connector)

        val servletHandler = ServletHandler()
        server.handler = servletHandler

        servletHandler.addFilterWithMapping(FilterHolder(SaneEncodingFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))
        servletHandler.addFilterWithMapping(FilterHolder(AntiCsrfFilter()), "/*", EnumSet.of(DispatcherType.REQUEST))

        servletHandler.addServletWithMapping(ServletHolder(HealthCheckServlet()), "/healthcheck")
        servletHandler.addServletWithMapping(ServletHolder(InfoServlet(plugin, gson)), "/info")
        servletHandler.addServletWithMapping(ServletHolder(CommandServlet(handler)), "/command")
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }
}
