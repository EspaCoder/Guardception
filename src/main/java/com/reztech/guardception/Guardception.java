package com.reztech.guardception;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class Guardception extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "Guardception successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Guardception successfully disabled!");
    }
}
