package me.param.plugins.deathmatch.events;

import me.param.plugins.deathmatch.Deathmatch;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

        // Remove player from game.alivePlayers
        for(int i = 0; i < game.alivePlayers.size(); i++) {
            if(game.alivePlayers.get(i).equals(event.getPlayer())) {
                game.alivePlayers.remove(i);
                i--;
            }
        }

        // End the game if only 1 player is alive
        if(game.alivePlayers.size() == 1) {
            Player winner = game.alivePlayers.get(0);
            game.stop(winner);
        }

    }
}
