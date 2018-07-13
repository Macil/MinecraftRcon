package tech.macil.minecraft.rcon.web

import com.google.gson.Gson
import tech.macil.minecraft.rcon.RconPlugin
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class InfoServlet(
        private val plugin: RconPlugin,
        private val gson: Gson
) : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val obj = plugin.server.scheduler.callSyncMethod(plugin) {
            mapOf(
                    "name" to plugin.server.serverName,
                    "version" to plugin.server.version,
                    "motd" to plugin.server.motd,
                    "currentPlayerCount" to plugin.server.onlinePlayers.size,
                    "maxPlayerCount" to plugin.server.maxPlayers,
                    "players" to plugin.server.onlinePlayers.map { player ->
                        mapOf("name" to player.name)
                    }
            )
        }.get()

        resp.contentType = "application/json"
        resp.writer.println(gson.toJson(obj))
    }
}