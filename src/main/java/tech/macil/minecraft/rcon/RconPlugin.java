package tech.macil.minecraft.rcon;

import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class RconPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().log(Level.INFO, "onEnable");

        String listenAddress = getConfig().getString("listenAddress");
        int port = getConfig().getInt("port");

        CommandSender sender = new RconCommandSender(this);
        try {
            if (!getServer().dispatchCommand(sender, "version")) {
                getLogger().log(Level.SEVERE, "Command not found");
            }
        } catch (CommandException e) {
            getLogger().log(Level.SEVERE, "Error running command", e);
        }
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "onDisable");
    }
}
