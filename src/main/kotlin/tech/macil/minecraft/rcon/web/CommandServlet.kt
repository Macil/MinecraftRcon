package tech.macil.minecraft.rcon.web

import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

typealias CommandServletHandler = (command: String, output: ServletOutputStream, remoteAddr: String) -> Unit

class CommandServlet(
        private val handler: CommandServletHandler
) : HttpServlet() {
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val command = req.getParameter("command")
                ?: return resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing command parameter")

        resp.contentType = "text/plain"

        handler(command, resp.outputStream, req.remoteAddr)
    }
}