package tech.macil.minecraft.rcon;

import org.junit.Test;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class OffThreadWriterTest {
    @Test
    public void test() {
        CharArrayWriter written = new CharArrayWriter();
        LoggingLogger logger = new LoggingLogger();
        LoggingExecutor executor = new LoggingExecutor();

        OffThreadWriter otw = new OffThreadWriter(new PrintWriter(written), logger, executor);

        otw.writeLn("foo");
        assertEquals("", written.toString());

        assertEquals(1, executor.runnables.size());
        executor.runnables.clear();
        otw.flush();

        assertEquals("foo\n", written.toString());

        otw.writeLn("bar");
        otw.writeLn("baz");

        assertEquals("foo\n", written.toString());

        assertEquals(1, executor.runnables.size());
        executor.runnables.get(0).run();
        assertEquals(1, executor.runnables.size());
        executor.runnables.clear();

        assertEquals(written.toString(), "foo\nbar\nbaz\n");

        otw.flush();

        assertEquals(written.toString(), "foo\nbar\nbaz\n");

        otw.close();

        otw.writeLn("blah");

        assertEquals(0, logger.logs.size());

        assertEquals(1, executor.runnables.size());
        executor.runnables.get(0).run();
        assertEquals(1, executor.runnables.size());
        executor.runnables.clear();

        assertEquals(written.toString(), "foo\nbar\nbaz\n");
        assertEquals(1, logger.logs.size());
        assertEquals("Message for disconnected session: blah", logger.logs.get(0).getMessage());
    }

    private class LoggingLogger extends Logger {
        List<LogRecord> logs = Collections.synchronizedList(new ArrayList<>());

        LoggingLogger() {
            super("test:name", null);
        }

        public void log(LogRecord record) {
            logs.add(record);
        }
    }

    private class LoggingExecutor implements Executor {
        List<Runnable> runnables = Collections.synchronizedList(new ArrayList<>());

        public void execute(Runnable runnable) {
            runnables.add(runnable);
        }
    }
}