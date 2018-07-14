package tech.macil.minecraft.rcon.util

import java.io.Closeable

class BlockerCounter {
    private val notifier = java.lang.Object()
    private var counter = 0

    fun await(timeout: Long = 0) {
        synchronized(notifier) {
            while (counter != 0) {
                notifier.wait(timeout)
                if (timeout > 0) break
            }
        }
    }

    fun acquire(): Releaser {
        synchronized(notifier) {
            counter++
        }
        return Releaser()
    }

    inner class Releaser : Closeable {
        private var closed = false

        override fun close() {
            if (closed) return
            closed = true

            synchronized(notifier) {
                counter--
                if (counter == 0) {
                    notifier.notifyAll()
                }
            }
        }
    }
}
