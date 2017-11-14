package tech.macil.minecraft.rcon;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

import java.io.Serializable;
import java.net.Socket;
import java.util.UUID;

public class RconAppender implements Appender {
    private final Socket connection;
    private final OffThreadWriter output;
    private final UUID uuid = UUID.randomUUID();

    public RconAppender(Socket connection, OffThreadWriter output) {
        this.connection = connection;
        this.output = output;
    }

    @Override
    public void append(LogEvent event) {
        output.writeLn(event.getMessage().getFormattedMessage());
    }

    @Override
    public String getName() {
        return "$RconSession:" + uuid.toString();
    }

    public Layout<? extends Serializable> getLayout() {
        return null;
    }

    public boolean ignoreExceptions() {
        return false;
    }

    public ErrorHandler getHandler() {
        return null;
    }

    public void setHandler(ErrorHandler handler) {
    }

    public State getState() {
        return null;
    }

    public void initialize() {
    }

    public void start() {
    }

    public void stop() {
    }

    public boolean isStarted() {
        return true;
    }

    public boolean isStopped() {
        return connection.isClosed();
    }
}
