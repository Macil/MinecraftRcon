package tech.macil.minecraft.rcon

import fi.iki.elonen.NanoHTTPD
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import tech.macil.minecraft.rcon.util.ByteArrayQueue
import java.io.OutputStream

class WebServer(
        hostName: String?,
        port: Int,
        private val plugin: RconPlugin,
        private val handler: (command: String, output: OutputStream, remoteIp: String) -> Unit
) : NanoHTTPD(hostName, port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        when (session.method) {
            Method.GET -> {
                when (uri) {
                    "/healthcheck" -> {
                        return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Alive!\n")
                    }
                    "/info" -> {
                        val obj = plugin.server.scheduler.callSyncMethod(plugin) {
                            val players = JSONArray()
                            for (player in plugin.server.onlinePlayers) {
                                val playerObj = JSONObject()
                                playerObj["name"] = player.name
                                players.add(playerObj)
                            }
                            JSONObject(mapOf(
                                    "name" to plugin.server.serverName,
                                    "version" to plugin.server.version,
                                    "motd" to plugin.server.motd,
                                    "currentPlayerCount" to plugin.server.onlinePlayers.size,
                                    "maxPlayerCount" to plugin.server.maxPlayers,
                                    "players" to players
                            ))
                        }.get()
                        return newFixedLengthResponse(Response.Status.OK, "application/json", obj.toJSONString())
                    }
                    else -> {
                        throw ResponseException(Response.Status.NOT_FOUND, "Not Found\n")
                    }
                }
            }
            Method.POST -> {
                session.parseBody(mutableMapOf<String, String>())

                if (uri == "/command") {
                    val command = session.parameters["command"]?.get(0)
                            ?: throw ResponseException(Response.Status.BAD_REQUEST, "command parameter missing\n")
                    val (input, output) = ByteArrayQueue.makePair()
                    handler(command, output, session.remoteIpAddress)
                    return newChunkedResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, input)
                } else {
                    throw ResponseException(Response.Status.NOT_FOUND, "Not Found\n")
                }
            }
            else -> {
                throw ResponseException(Response.Status.METHOD_NOT_ALLOWED, "BAD REQUEST: Only GET and POST are supported.\n")
            }
        }
    }
}
