package me.param.plugins.deathmatch;

import me.param.plugins.deathmatch.events.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;

public final class Deathmatch extends JavaPlugin {
    public boolean inProgress = false;
    public ArrayList<Player> alivePlayers;

    private World world;
    private WorldBorder border;
    private Scoreboard scoreboard;
    private int taskID;

    @Override
    public void onEnable() {
        world = Bukkit.getWorld("world");
        border = world.getWorldBorder();

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective health = scoreboard.registerNewObjective("Health", Criterias.HEALTH, "Health", RenderType.HEARTS);
        health.setDisplaySlot(DisplaySlot.PLAYER_LIST);

        getLogger().info("Deathmatch has been enabled!");
        getCommand("deathmatch").setExecutor(new Commands(this));
        getCommand("deathmatch").setTabCompleter(new CommandsAutocompleter());

        getServer().getPluginManager().registerEvents(new OnDeath(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerLeave(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new OnBreakBlock(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerHit(), this);
    }

    public void start() {
        if(Bukkit.getOnlinePlayers().size() < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "There aren't enough players to start the game!");
            return;
        }

        inProgress = true;
        alivePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }

        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "Deathmatch Rules" +
                "\n" + ChatColor.RESET + "Gather resources and fend off other players. " +
                ChatColor.RESET + "Killing a player will give you " + ChatColor.GOLD +
                "1 Golden Apple. " + ChatColor.RESET + "Last player standing wins!");

        int borderSize = 200;
        border.setSize(borderSize);
        border.setDamageBuffer(0);
        border.setCenter(0, 0);

        for(Player player : Bukkit.getOnlinePlayers()) {
            // Clear inventory, reset health and food bars, apply 60 seconds of speed and 5 seconds of invincibility
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(5);
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*5, 100));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20*60, 1));

            //TODO randomize spawn locations
            double x = Math.random() * borderSize - borderSize / 2.0;
            double z = Math.random() * borderSize - borderSize / 2.0;

            player.teleport(new Location(world, x, world.getHighestBlockYAt((int) x, (int) z) + 1, z));
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);
            player.sendTitle(ChatColor.RED + "Deathmatch", "", 10, 60, 10);
            player.setGameMode(GameMode.SURVIVAL);
        }

        taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                if(inProgress) {
                    border.setSize(10, 60*5);
                    sendTitleToEveryone("", "The border is shrinking!", 10, 60, 10);
                }
            }
        }, 20*60);
    }

    public void stop() {
        gameEndProcedure();
        sendTitleToEveryone(ChatColor.RED + "Game Over!", "Nobody wins",
                10, 100, 10);
    }

    public void stop(Player winner) {
        gameEndProcedure();
        sendTitleToEveryone(ChatColor.GOLD + winner.getDisplayName() + ChatColor.RESET + " wins!",
                "", 10, 100, 10);
        winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP,1, 1);
    }

    public boolean sendTitleToEveryone(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }

        return true;
    }

    private void gameEndProcedure() {
        inProgress = false;
        Bukkit.getServer().getScheduler().cancelTask(taskID);
        alivePlayers.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        border.setSize(30000000); //Reset worldborder

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

}
