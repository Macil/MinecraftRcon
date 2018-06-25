package tech.macil.minecraft.rcon

import java.io.CharArrayWriter
import java.io.PrintWriter
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.Executor
import java.util.logging.LogRecord
import java.util.logging.Logger

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OffThreadWriterTest {
    @Test
    fun test() {
        val written = CharArrayWriter()
        val logger = LoggingLogger()
        val executor = LoggingExecutor()

        val otw = OffThreadWriter(PrintWriter(written), logger, executor)

        otw.writeLn("foo")
        assertEquals("", written.toString())

        assertEquals(1, executor.runnables.size.toLong())
        executor.runnables.clear()
        otw.flush()

        assertEquals("foo\n", written.toString())

        otw.writeLn("bar")
        otw.writeLn("baz")

        assertEquals("foo\n", written.toString())

        assertEquals(1, executor.runnables.size.toLong())
        executor.runnables[0].run()
        assertEquals(1, executor.runnables.size.toLong())
        executor.runnables.clear()

        assertEquals(written.toString(), "foo\nbar\nbaz\n")

        otw.flush()

        assertEquals(written.toString(), "foo\nbar\nbaz\n")

        otw.close()

        otw.writeLn("blah")

        assertEquals(0, logger.logs.size.toLong())

        assertEquals(1, executor.runnables.size.toLong())
        executor.runnables[0].run()
        assertEquals(1, executor.runnables.size.toLong())
        executor.runnables.clear()

        assertEquals(written.toString(), "foo\nbar\nbaz\n")
        assertEquals(1, logger.logs.size.toLong())
        assertEquals("Message for disconnected session: blah", logger.logs[0].message)
    }

    private inner class LoggingLogger : Logger("test:name", null) {
        internal var logs: MutableList<LogRecord> = Collections.synchronizedList(ArrayList())

        override fun log(record: LogRecord) {
            logs.add(record)
        }
    }

    private inner class LoggingExecutor : Executor {
        internal var runnables: MutableList<Runnable> = Collections.synchronizedList(ArrayList())

        override fun execute(runnable: Runnable) {
            runnables.add(runnable)
        }
    }
}