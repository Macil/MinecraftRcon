package tech.macil.minecraft.rcon.web

import tech.macil.minecraft.rcon.AuthChecker
import java.util.*
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthFilter(
        private val authChecker: AuthChecker
) : Filter {
    override fun init(filterConfig: FilterConfig) {
    }

    override fun destroy() {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        request as HttpServletRequest
        response as HttpServletResponse

        if (!isRequestAuthed(request)) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Protected\"")
            return response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        }
        return chain.doFilter(request, response)
    }

    private fun isRequestAuthed(request: HttpServletRequest): Boolean {
        val authHeader = request.getHeader("Authorization") ?: return false
        val split = authHeader.split(' ', limit = 2)
        if (split.size != 2) return false
        val (type, credentials) = split
        if (type != "Basic") return false

        val credSplit = try {
            String(Base64.getDecoder().decode(credentials)).split(':', limit = 2)
        } catch (e: IllegalArgumentException) {
            return false
        }
        if (credSplit.size != 2) return false

        val (username, password) = credSplit
        return authChecker.check(username, password)
    }
}
