package tech.macil.minecraft.rcon.web

import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AntiCsrfFilter : Filter {
    companion object {
        private val protectMethods = setOf("POST", "PUT", "DELETE", "PATCH")
    }

    override fun init(filterConfig: FilterConfig) {
    }

    override fun destroy() {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        request as HttpServletRequest
        if (request.method in protectMethods && request.getHeader("X-Anti-Csrf") != "1") {
            response as HttpServletResponse
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "X-Anti-CSRF header is required for mutative requests.")
            return
        }
        return chain.doFilter(request, response)
    }
}
