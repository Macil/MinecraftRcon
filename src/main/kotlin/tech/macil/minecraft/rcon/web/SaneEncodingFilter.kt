package tech.macil.minecraft.rcon.web

import javax.servlet.*

class SaneEncodingFilter : Filter {
    override fun init(filterConfig: FilterConfig?) {
    }

    override fun destroy() {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request.characterEncoding == null) {
            request.characterEncoding = "UTF-8"
        }
        response.characterEncoding = "UTF-8"
        return chain.doFilter(request, response)
    }
}
