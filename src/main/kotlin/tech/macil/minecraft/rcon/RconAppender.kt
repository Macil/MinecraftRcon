package tech.macil.minecraft.rcon

import org.apache.logging.log4j.core.*

import java.io.Serializable
import java.net.Socket
import java.util.UUID

class RconAppender(
        private val connection: Socket,
        private val output: OffThreadWriter
) : Appender {
    private val uuid = UUID.randomUUID()

    override fun append(event: LogEvent) {
        output.writeLn(event.message.formattedMessage)
    }

    override fun getName(): String {
        return "\$RconSession:" + uuid.toString()
    }

    override fun getLayout(): Layout<out Serializable>? {
        return null
    }

    override fun ignoreExceptions(): Boolean {
        return false
    }

    override fun getHandler(): ErrorHandler? {
        return null
    }

    override fun setHandler(handler: ErrorHandler?) {}

    override fun getState(): LifeCycle.State? {
        return null
    }

    override fun initialize() {}

    override fun start() {}

    override fun stop() {}

    override fun isStarted(): Boolean {
        return true
    }

    override fun isStopped(): Boolean {
        return connection.isClosed
    }
}
