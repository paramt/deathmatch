package me.param.plugins.deathmatch.events;

import me.param.plugins.deathmatch.Deathmatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnPlayerLeave implements Listener {
    private Deathmatch game;

    public OnPlayerLeave(Deathmatch plugin) {
        this.game = plugin;
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if(!game.inProgress)
            return;

        Player player = event.getPlayer();
        game.handleDeath(player, null);

    }
}
