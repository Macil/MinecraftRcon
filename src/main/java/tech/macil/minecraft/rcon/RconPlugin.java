package tech.macil.minecraft.rcon;

import com.google.common.base.Charsets;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import tech.macil.util.NLRequiringBufferedReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

public class RconPlugin extends JavaPlugin {
    private static final int SOCKET_BACKLOG = 20;
    private static final String EXPECTED_GREETING = "Minecraft-Rcon";
    private ServerSocket socket;

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
                    try {
                        new Thread(new ClientConnectionRunnable(connection)).start();
                    } catch (Exception e) {
                        connection.close();
                    }
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
                    PrintWriter output = new PrintWriter(connection.getOutputStream(), false);

                    CommandSender sender = new RconCommandSender(RconPlugin.this, output);

                    String greeting = input.readLine();
                    if (!EXPECTED_GREETING.equals(greeting)) {
                        return;
                    }

                    input.lines().forEach(line -> {
                        getLogger().log(Level.INFO, "rcon("+connection.getRemoteSocketAddress()+"): " + line);
                        try {
                            if (!getServer().dispatchCommand(sender, line)) {
                                output.write("Command not found\n");
                                output.flush();
                            }
                        } catch (CommandException e) {
                            output.write(e.toString());
                            output.write('\n');
                            output.flush();
                        }
                    });
                } finally {
                    connection.close();
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Unknown error in connection thread", e);
            }
        }
    }
}
