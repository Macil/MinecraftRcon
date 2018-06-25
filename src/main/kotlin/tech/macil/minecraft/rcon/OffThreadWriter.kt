package tech.macil.minecraft.rcon

import java.io.Closeable
import java.io.PrintWriter
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

class OffThreadWriter(
        private val output: PrintWriter,
        private val logger: Logger,
        private val executor: Executor
) : Closeable {
    private val buffer = LinkedBlockingQueue<String>()
    private val flushScheduled = AtomicBoolean(false)
    private var closed = false

    fun writeLn(line: String) {
        buffer.add(line)

        if (!flushScheduled.getAndSet(true)) {
            executor.execute { this.flush() }
        }
    }

    fun writeLnWithoutFlush(line: String) {
        buffer.add(line)
    }

    @Synchronized
    fun flush() {
        flushScheduled.set(false)

        if (closed) {
            while (true) {
                val line: String = buffer.poll() ?: break
                logger.log(Level.WARNING, "Message for disconnected session: $line")
            }
        } else {
            while (true) {
                val line = buffer.poll() ?: break
                output.write(line)
                output.write('\n'.toInt())
            }
            output.flush()
        }
    }

    @Synchronized
    override fun close() {
        closed = true
        flush()
        output.close()
    }
}
