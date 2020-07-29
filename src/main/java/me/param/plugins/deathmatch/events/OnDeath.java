package me.param.plugins.deathmatch.events;

import me.param.plugins.deathmatch.Deathmatch;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

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
        player.setGameMode(GameMode.SPECTATOR);

        // Remove player from game.alivePlayers
        for(int i = 0; i < game.alivePlayers.size(); i++) {
            if(game.alivePlayers.get(i).equals(player)) {
                game.alivePlayers.remove(i);
                i--;
            }
        }

        // End the game if only 1 player is alive
        if(game.alivePlayers.size() == 1) {
            Player winner = game.alivePlayers.get(0);
            game.stop(winner);
        }

        // End the game if no players are alive
        else if(game.alivePlayers.size() == 0) {
            game.stop();
        }

        // Reward the killer
        else if(killer != null){
            player.teleport(killer.getLocation().add(0, 5, 0));
            killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));
            killer.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
        }
    }
}
