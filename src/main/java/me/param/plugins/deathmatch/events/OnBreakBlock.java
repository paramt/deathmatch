package me.param.plugins.deathmatch.events;

import me.param.plugins.deathmatch.Deathmatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class OnBreakBlock implements Listener {
    private Deathmatch game;

    public OnBreakBlock(Deathmatch plugin) {
        this.game = plugin;
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        if(!game.inProgress) {
            event.setCancelled(true);
        }
    }
}
