package com.reztech.guardception;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class GuardceptionPermissions {

    // private Map<String, Set<String>> permissions;
    private FileConfiguration regionalPermissions;
    private JavaPlugin guardceptionPlugin;
    private final String filePath = "plugins/Guardception/regionalPermissionsFile.yml";


    public GuardceptionPermissions() {
        this.guardceptionPlugin = JavaPlugin.getPlugin(Guardception.class);
    }

    public boolean init() {
        JavaPlugin guardceptionPlugin = JavaPlugin.getPlugin(Guardception.class);
        File dataFolder = new File("plugins/Guardception");
        if (!dataFolder.exists())
            dataFolder.mkdir();
        File regionalPermissionsFile = new File(filePath);
        if (!regionalPermissionsFile.exists()) {
            try {
                regionalPermissionsFile.createNewFile();
            }
            catch (IOException e) {
                guardceptionPlugin.getLogger().log(Level.SEVERE, "Could not create regionalPermissionsFile.yml under plugins/Guardception");
                return false;
            }
        }
        this.regionalPermissions = new YamlConfiguration();
        try {
            regionalPermissions.load(regionalPermissionsFile);
        } catch (IOException e) {
            guardceptionPlugin.getLogger().log(Level.SEVERE, "Could not load config file " + filePath);
            return false;
        } catch (InvalidConfigurationException e) {
            guardceptionPlugin.getLogger().log(Level.SEVERE, "File in " + filePath + " is corrputed or invalid");
            return false;
        }
        return true;
    }

    public boolean hasPlayerPermissionInRegion(String player, String region) {
        if (regionalPermissions == null) {
            guardceptionPlugin.getLogger().log(Level.SEVERE, "Guardception has not been intiallized!");
            return false;
        }
        List<String> permittedRegions = regionalPermissions.getStringList(player);
        return permittedRegions.contains(region);
    }

    public boolean addPlayerPermissionInRegion(String player, String region) {
        List<String> permittedRegions = regionalPermissions.getStringList(player);
        if (!permittedRegions.contains(region))
            permittedRegions.add(region);
        regionalPermissions.set(player, permittedRegions);
        try {
            regionalPermissions.save(filePath);
        }
        catch (IOException e) {
            guardceptionPlugin.getLogger().log(Level.SEVERE, "Could not write to file " + filePath);
            return false;
        }
        return true;
    }

    public boolean delPlayerPermissionInRegion(String player, String region) {
        List<String> permittedRegions = regionalPermissions.getStringList(player);
        if (permittedRegions.contains(region))
            permittedRegions.remove(region);
        regionalPermissions.set(player, permittedRegions);
        try {
            regionalPermissions.save(filePath);
        }
        catch (IOException e) {
            guardceptionPlugin.getLogger().log(Level.SEVERE, "Could not write to file " + filePath);
            return false;
        }
        return true;
    }

    public boolean deleteRegion(String regionName) {
        Set<String> players = regionalPermissions.getKeys(false);
        List<String> regions;
        for (String player : players) {
            regions = regionalPermissions.getStringList(player);
            if (regions.contains(regionName))
                regions.remove(regionName);
            regionalPermissions.set(player, regions);
        }
        try {
            regionalPermissions.save(filePath);
        }
        catch (IOException e) {
            guardceptionPlugin.getLogger().log(Level.SEVERE, "Could not write to file " + filePath);
            return false;
        }
        return true;
    }

}
