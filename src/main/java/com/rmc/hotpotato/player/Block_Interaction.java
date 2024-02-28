package com.rmc.hotpotato.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author angel
 */
public class Block_Interaction implements Listener {
    final Set<Material> lockedMaterials = new HashSet<>(Arrays.asList(
            Material.CHEST, Material.TRAPPED_CHEST, Material.HOPPER,
            Material.FURNACE, Material.DISPENSER, Material.DROPPER,
            Material.BREWING_STAND, Material.ENCHANTMENT_TABLE
    ));

    public Block_Interaction(Plugin plugin){
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }

    public boolean checkPlayer(Player player) {
        return player.getGameMode() == GameMode.CREATIVE && player.hasPermission("hotpotato.admin");
    }

    @EventHandler
    public void onPlayerBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (checkPlayer(player)) {
            return;
        }
        e.setCancelled(true);
    }


    @EventHandler
    public void onPlayerPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (checkPlayer(player)) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (checkPlayer(player)) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        if (checkPlayer(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null || checkPlayer(player)) {
            return;
        }

        Material blockType = clickedBlock.getType();

        if (lockedMaterials.contains(blockType)) {
            event.setCancelled(true);
            player.sendMessage("§c该物品被锁住了");
        }
    }


}
