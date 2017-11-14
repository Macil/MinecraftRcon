package tech.macil.minecraft.rcon;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class RconPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "onEnable");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "onDisable");
    }
}
