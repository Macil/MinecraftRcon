package tech.macil.minecraft.rcon;

import java.io.Closeable;
import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OffThreadWriter implements Closeable {
    private final PrintWriter output;
    private final Logger logger;
    private final Executor executor;
    private final LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>();
    private AtomicBoolean flushScheduled = new AtomicBoolean(false);
    private boolean closed = false;

    public OffThreadWriter(PrintWriter output, Logger logger, Executor executor) {
        this.output = output;
        this.logger = logger;
        this.executor = executor;
    }

    public void writeLn(String line) {
        buffer.add(line);

        if (!flushScheduled.getAndSet(true)) {
            executor.execute(this::flush);
        }
    }

    public void writeLnWithoutFlush(String line) {
        buffer.add(line);
    }

    synchronized public void flush() {
        flushScheduled.set(false);

        if (closed) {
            while (true) {
                String line = buffer.poll();
                if (line == null) {
                    break;
                }
                logger.log(Level.WARNING, "Message for disconnected session: " + line);
            }
        } else {
            while (true) {
                String line = buffer.poll();
                if (line == null) {
                    break;
                }
                output.write(line);
                output.write('\n');
            }
            output.flush();
        }
    }

    synchronized public void close() {
        closed = true;
        flush();
        output.close();
    }
}
