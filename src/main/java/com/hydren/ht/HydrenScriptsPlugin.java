package com.hydren.ht;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public class HydrenScriptsPlugin extends JavaPlugin implements Listener {
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path scriptsDir = getDataFolder().toPath().resolve("scripts");
        scriptManager = new ScriptManager(this, scriptsDir);
        if (getConfig().getBoolean("reload-on-start", true)) {
            scriptManager.loadAll();
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("HydrenScripts enabled.");
    }

    @Override
    public void onDisable() {
        if (scriptManager != null) scriptManager.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("htreload")) {
            scriptManager.reloadAll();
            sender.sendMessage("HydrenScripts: scripts reloaded.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        scriptManager.callHandler("onPlayerJoin", e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        scriptManager.callHandler("onPlayerQuit", e.getPlayer());
    }
}
