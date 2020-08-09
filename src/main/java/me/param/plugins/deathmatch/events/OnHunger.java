package me.param.plugins.deathmatch.events;

import me.param.plugins.deathmatch.Deathmatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class OnHunger implements Listener {
    private Deathmatch game;

    public OnHunger(Deathmatch plugin) {
        this.game = plugin;
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if(!game.getConfig().getBoolean("start.hunger"))
            event.setCancelled(true);
    }
}
