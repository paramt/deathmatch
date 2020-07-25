package me.param.plugins.deathmatch.events;

import me.param.plugins.deathmatch.Deathmatch;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnPlayerJoin implements Listener {
    private Deathmatch game;

    public OnPlayerJoin(Deathmatch plugin) {
        this.game = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(game.inProgress) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
            event.getPlayer().sendMessage(ChatColor.RED + "The game is already in progress! You are spectating.");
        }
    }
}
