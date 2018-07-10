package tech.macil.minecraft.rcon

import fi.iki.elonen.NanoHTTPD
import tech.macil.minecraft.rcon.util.ByteArrayQueue
import java.io.OutputStream

class WebServer(
        hostName: String?,
        port: Int,
        private val handler: (command: String, output: OutputStream, remoteIp: String) -> Unit
) : NanoHTTPD(hostName, port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        when (session.method) {
            Method.GET -> {
                if (uri == "/healthcheck") {
                    return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Alive!\n")
                } else {
                    throw ResponseException(Response.Status.NOT_FOUND, "Not Found\n")
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
