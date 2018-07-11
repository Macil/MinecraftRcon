package tech.macil.minecraft.rcon

import tech.macil.minecraft.rcon.util.ByteArrayQueue
import java.io.OutputStream
import com.google.gson.GsonBuilder
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WebServer(
        hostName: String?,
        port: Int,
        private val plugin: RconPlugin,
        private val handler: (command: String, output: OutputStream, remoteIp: String) -> Unit
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

        servletHandler.addServletWithMapping(ServletHolder(Servlet()), "/")
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

    private class Servlet : HttpServlet() {
        override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
            println("got request")
            resp.writer.println("woot")
        }
    }

//    override fun serve(session: IHTTPSession): Response {
//        val uri = session.uri
//        when (session.method) {
//            Method.GET -> {
//                when (uri) {
//                    "/healthcheck" -> {
//                        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Alive!\n")
//                    }
//                    "/info" -> {
//                        val obj = plugin.server.scheduler.callSyncMethod(plugin) {
//                            mapOf(
//                                    "name" to plugin.server.serverName,
//                                    "version" to plugin.server.version,
//                                    "motd" to plugin.server.motd,
//                                    "currentPlayerCount" to plugin.server.onlinePlayers.size,
//                                    "maxPlayerCount" to plugin.server.maxPlayers,
//                                    "players" to plugin.server.onlinePlayers.map { player ->
//                                        mapOf("name" to player.name)
//                                    }
//                            )
//                        }.get()
//                        val jsonString = gson.toJson(obj) + "\n"
//                        return newFixedLengthResponse(Response.Status.OK, "application/json", jsonString)
//                    }
//                    else -> {
//                        throw ResponseException(Response.Status.NOT_FOUND, "Not Found\n")
//                    }
//                }
//            }
//            Method.POST -> {
//                if (session.headers["x-anti-csrf"] != "1") {
//                    throw ResponseException(Response.Status.FORBIDDEN, "X-Anti-CSRF header is required for POST requests.\n")
//                }
//
//                session.parseBody(mutableMapOf<String, String>())
//
//                if (uri == "/command") {
//                    val command = session.parameters["command"]?.get(0)
//                            ?: throw ResponseException(Response.Status.BAD_REQUEST, "command parameter missing\n")
//                    val (input, output) = ByteArrayQueue.makePair()
//                    handler(command, output, session.remoteIpAddress)
//                    return newChunkedResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, input)
//                } else {
//                    throw ResponseException(Response.Status.NOT_FOUND, "Not Found\n")
//                }
//            }
//            else -> {
//                throw ResponseException(Response.Status.METHOD_NOT_ALLOWED, "BAD REQUEST: Only GET and POST are supported.\n")
//            }
//        }
//    }
}
