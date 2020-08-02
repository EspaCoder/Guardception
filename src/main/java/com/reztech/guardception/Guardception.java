package com.reztech.guardception;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class Guardception extends JavaPlugin implements Listener {

    private GuardceptionPermissions gcpermissions;
    private WorldGuard worldguardInstance;

    @Override
    public void onEnable() {
        this.gcpermissions = new GuardceptionPermissions();
        gcpermissions.init();
        worldguardInstance = WorldGuard.getInstance();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().log(Level.INFO, "Guardception successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Guardception successfully disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gception")) {
            if (args.length < 3) {
                sender.sendMessage(Color.RED + "Invalid number of arguments!");
                return false;
            }
            // Only check permission if it is a player
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("guardception.subreg")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to do such an action!");
                    return false;
                }
            }
            String option = args[0];
            String targetPlayer = args[1];
            String regionName = args[2];
            List<org.bukkit.World> worlds = Bukkit.getWorlds();
            boolean found = false;
            for (org.bukkit.World world : worlds) {
                World currentWorld = BukkitAdapter.adapt(world);
                Map<String, ProtectedRegion> regions = worldguardInstance.getPlatform().getRegionContainer().get(currentWorld).getRegions();
                // Check if the region exists or if it is the __global__ region
                if (regions.containsKey(regionName) || regionName.equalsIgnoreCase("__global__")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                sender.sendMessage(ChatColor.RED + regionName + " region does not exist!");
                return false;
            }
            boolean success;
            if (option.equalsIgnoreCase("add")) {
                success = gcpermissions.addPlayerPermissionInRegion(targetPlayer, regionName);
                if (success)
                    sender.sendMessage(ChatColor.AQUA + "Permission to create regions inside " + regionName + " given to " + targetPlayer);
                else
                    sender.sendMessage(ChatColor.RED + "Something went wrong while giving " + targetPlayer + " permision to create regions inside " + regionName);
            }
            else if (option.equalsIgnoreCase("del")) {
                success = gcpermissions.delPlayerPermissionInRegion(targetPlayer, regionName);
                if (success)
                    sender.sendMessage(ChatColor.AQUA + "Permission to create regions inside " + regionName + " denied to " + targetPlayer);
                else
                    sender.sendMessage(ChatColor.RED + "Something went wrong while denying " + targetPlayer + " permision to create regions inside " + regionName);
            }
            else if (option.equalsIgnoreCase("check")) {
                boolean hasPermission = gcpermissions.hasPlayerPermissionInRegion(targetPlayer, regionName);
                String doesOrNot = hasPermission ? "does" : "does not";
                ChatColor color = hasPermission ? ChatColor.AQUA : ChatColor.RED;
                sender.sendMessage(color + targetPlayer + " " + doesOrNot + " have permission to create regions inside " + regionName);
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void commandInterception(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        Player player = event.getPlayer();
        if (command.length() >= 3) {
            command = command.substring(1);
            String[] args = command.split(" ");
            // Only perform logic if it is the region (rg) command
            if (args[0].equalsIgnoreCase("region") || args[0].equalsIgnoreCase("rg")) {
                if (args[1].equalsIgnoreCase("define")) {
                    WorldEdit worldEditInstance = WorldEdit.getInstance();
                    LocalSession session = worldEditInstance.getSessionManager().get(BukkitAdapter.adapt(player));
                    Region selection;
                    try {
                        selection = session.getSelection(session.getSelectionWorld());
                    } catch (IncompleteRegionException e) {
                        player.sendMessage(ChatColor.RED + "Complete the region selection before creating a region!");
                        return;
                    }
                    RegionContainer regionContainer = worldguardInstance.getPlatform().getRegionContainer();
                    RegionManager manager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
                    int xMin = selection.getMinimumPoint().getBlockX();
                    int zMin = selection.getMinimumPoint().getBlockZ();
                    int xMax = selection.getMaximumPoint().getBlockX();
                    int zMax = selection.getMaximumPoint().getBlockZ();
                    int y = selection.getMaximumPoint().getY();
                    Set<String> regions = new HashSet<String>();
                    for (int x = xMin; x <= xMax; ++x) { //Check all the blocks inside the selection to get all the overlapping regions
                        for (int z = zMin; z <= zMax; ++z) {
                            BlockVector3 block = BlockVector3.at(x, y, z);
                            List<String> regionList = manager.getApplicableRegionsIDs(block);
                            // If no region appears, then it is the __global__ region
                            if (regionList.isEmpty())
                                regions.add("__global__");
                            else
                                regions.addAll(regionList);
                        }
                    }
                    for (String region : regions) {
                        // If the player has no permission to edit one of the overlapping regions, the region cannot be defined
                        if (!gcpermissions.hasPlayerPermissionInRegion(player.getName(), region)) {
                            player.sendMessage(ChatColor.RED + "You cannot create a region here because you have no permission to define a region inside " + region + " region!");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                else if (args[1].equalsIgnoreCase("del")) {
                    String regionName = args[2];
                    boolean success = gcpermissions.deleteRegion(regionName);
                    if (!success) {
                        player.sendMessage(ChatColor.RED + "Could not delete the region, something went wrong with Guardception!");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

}
