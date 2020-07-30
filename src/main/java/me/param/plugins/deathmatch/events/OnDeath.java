package me.param.plugins.deathmatch.events;

import me.param.plugins.deathmatch.Deathmatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class OnDeath implements Listener {
    private Deathmatch game;

    public OnDeath(Deathmatch plugin) {
        this.game = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!game.inProgress)
            return;

        Player player = event.getEntity();
        Player killer = event.getEntity().getKiller();

        game.handleDeath(player, killer);
    }
}
