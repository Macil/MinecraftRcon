package tech.macil.minecraft.rcon

import org.apache.logging.log4j.core.*
import java.io.PrintWriter

import java.io.Serializable
import java.net.Socket
import java.util.UUID

class RconAppender(
        private val output: PrintWriter
) : Appender {
    private val uuid = UUID.randomUUID()

    override fun append(event: LogEvent) {
        try {
            output.println(event.message.formattedMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getName(): String {
        return "\$RconSession:" + uuid.toString()
    }

    override fun getLayout(): Layout<out Serializable>? = null

    override fun ignoreExceptions(): Boolean = false

    override fun getHandler(): ErrorHandler? = null

    override fun setHandler(handler: ErrorHandler?) {}

    override fun getState(): LifeCycle.State? = null

    override fun initialize() {}

    override fun start() {}

    override fun stop() {}

    override fun isStarted(): Boolean = true

    override fun isStopped(): Boolean = false
}
