package tech.macil.minecraft.rcon;

import com.google.common.base.Charsets;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tech.macil.util.NLRequiringBufferedReader;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.*;

public class RconPlugin extends JavaPlugin {
    private static final int SOCKET_BACKLOG = 20;
    private static final int THREAD_COUNT = 6;
    private static final String EXPECTED_GREETING = "Minecraft-Rcon";
    private ServerSocket socket;
    private static final Executor connectionHandler = Executors.newWorkStealingPool(THREAD_COUNT);
    private static final Executor outputFlusher = Executors.newWorkStealingPool(THREAD_COUNT);

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String listenAddress = getConfig().getString("listenAddress");
        int port = getConfig().getInt("port");

        try {
            if (listenAddress.equals("all")) {
                socket = new ServerSocket(port, SOCKET_BACKLOG);
            } else {
                socket = new ServerSocket(port, SOCKET_BACKLOG, InetAddress.getByName(listenAddress));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            try {
                while (true) {
                    Socket connection = socket.accept();
                    connectionHandler.execute(new ClientConnectionRunnable(connection));
                }
            } catch (SocketException e) {
                // ignore, happens when socket is closed
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Unknown error in server thread", e);
            }
        }).start();
    }

    @Override
    public void onDisable() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                socket = null;
            }
        }
    }

    private class ClientConnectionRunnable implements Runnable {
        private final Socket connection;

        ClientConnectionRunnable(Socket connection) {
            this.connection = connection;
        }

        @Override
        public void run() {
            try {
                try {
                    // Use NLRequiringBufferedReader so if the connection dies part way through, we don't
                    // execute half of a command.
                    BufferedReader input = new NLRequiringBufferedReader(
                            new InputStreamReader(connection.getInputStream(), Charsets.UTF_8));
                    PrintWriter outputPrintWriter = new PrintWriter(connection.getOutputStream(), false);

                    String greeting = input.readLine();
                    if (!EXPECTED_GREETING.equals(greeting)) {
                        outputPrintWriter.write("Bad header\n");
                        outputPrintWriter.flush();
                        return;
                    }

                    try (OffThreadWriter output = new OffThreadWriter(
                            outputPrintWriter,
                            getLogger(),
                            outputFlusher
                    )) {
                        Appender appender = new RconAppender(connection, output);

                        try {
                            ((Logger) LogManager.getRootLogger()).addAppender(appender);

                            input.lines().forEach(line -> {
                                getLogger().log(Level.INFO, "rcon(" + connection.getRemoteSocketAddress() + "): " + line);
                                try {
                                    if (getServer().getScheduler().callSyncMethod(RconPlugin.this, () ->
                                            !getServer().dispatchCommand(Bukkit.getConsoleSender(), line)
                                    ).get()) {
                                        output.writeLnWithoutFlush("Command not found");
                                        output.flush();
                                    }
                                } catch (Exception e) {
                                    output.writeLnWithoutFlush(ExceptionUtils.getStackTrace(e));
                                    output.flush();
                                }
                            });
                        } finally {
                            ((Logger) LogManager.getRootLogger()).removeAppender(appender);
                        }
                    }
                } finally {
                    connection.close();
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Unknown error in connection thread", e);
            }
        }

    }
}
