package tech.macil.minecraft.rcon.util

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

object ByteArrayQueue {
    // Thread safety: Input is not thread-safe. Output may be written to from multiple threads,
    // but close() may not happen concurrently with writes.
    fun makePair(): Pair<InputStream, OutputStream> {
        val input = Input()
        val output = Output(input)
        return Pair(input, output)
    }

    private class Output internal constructor(private val input: Input) : OutputStream() {
        override fun write(b: Int) {
            val dst = ByteArray(1)
            dst[0] = b.toByte()
            write(dst)
        }

        override fun write(b: ByteArray) {
            input.receive(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            val dst = ByteArray(len)
            System.arraycopy(b, off, dst, 0, len)
            write(dst)
        }

        override fun flush() {
        }

        override fun close() {
            input.receiveEnd()
        }
    }

    private class Input internal constructor() : InputStream() {
        companion object {
            private val END = Item(true, ByteArray(0))
        }

        private class Item(
                val isEnd: Boolean,
                val byteArray: ByteArray
        )

        private val queue: BlockingQueue<Item> = LinkedBlockingQueue()
        private var current: ByteArray? = null
        private var offset: Int = 0
        private var readToEnd: Boolean = false
        private var receivedEnd: Boolean = false
        private var availableBytes = AtomicInteger(0)

        override fun read(): Int {
            if (readToEnd) return -1
            var current = this.current
            if (current == null) {
                val item = queue.take()
                if (item.isEnd) {
                    readToEnd = true
                    return -1
                }
                current = item.byteArray
                this.current = current
            }
            val ret = current[offset++].toInt()
            if (offset >= current.size) {
                availableBytes.addAndGet(-current.size)
                this.current = null
                offset = 0
            }
            return ret
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (readToEnd) return -1
            var current = this.current
            if (current == null) {
                val item = queue.take()
                if (item.isEnd) {
                    readToEnd = true
                    return -1
                }
                current = item.byteArray
                this.current = current
            }
            val amountToCopy = Math.min(len, current.size - offset)
            System.arraycopy(current, offset, b, off, amountToCopy)
            offset += amountToCopy
            if (offset >= current.size) {
                availableBytes.addAndGet(-current.size)
                this.current = null
                offset = 0
            }
            return amountToCopy
        }

        override fun available(): Int {
            return availableBytes.get() - offset
        }

        internal fun receive(byteArray: ByteArray) {
            if (receivedEnd) {
                throw IllegalStateException("Can't write after close")
            }
            if (byteArray.isNotEmpty()) {
                availableBytes.addAndGet(byteArray.size)
                queue.offer(Item(false, byteArray))
            }
        }

        internal fun receiveEnd() {
            receivedEnd = true
            queue.put(END)
        }
    }
}