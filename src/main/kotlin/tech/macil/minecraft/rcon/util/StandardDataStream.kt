package tech.macil.minecraft.rcon.util

import java.io.InputStream
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener

// Heavily inspired by https://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-servlets/src/main/java/org/eclipse/jetty/servlets/DataRateLimitedServlet.java
// and https://webtide.com/servlet-3-1-async-io-and-jetty/
class StandardDataStream(
        private val content: InputStream,
        private val async: AsyncContext,
        private val out: ServletOutputStream
) : WriteListener {
    override fun onWritePossible() {
        val buffer = ByteArray(4096)

        while (out.isReady) {
            val len = content.read(buffer)
            if (len < 0) {
                async.complete()
                return
            }
            out.write(buffer, 0, len)
        }
    }

    override fun onError(t: Throwable) {
        async.request.servletContext.log("Async Error", t)
        async.complete()
    }
}