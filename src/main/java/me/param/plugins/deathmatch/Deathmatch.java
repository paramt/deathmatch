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
    public Scoreboard scoreboard;

    private World world;
    private WorldBorder border;
    private Objective stats;
    private Team countdownDisplay, borderSizeDisplay;

    private int timeUntilNextEvent, delayTask, countdownTask;
    private boolean statsRegistered, countdownDisplayRegistered, borderSizeDisplayRegistered = false;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

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
        if(Bukkit.getOnlinePlayers().size() < getConfig().getInt("minimum players")) {
            Bukkit.broadcastMessage(ChatColor.RED + "There aren't enough players to start the game!");
            return;
        }

        inProgress = true;
        alivePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        stats = scoreboard.registerNewObjective("stats", "dummy",
                ChatColor.GOLD + "DEATHMATCH");
        statsRegistered = true;
        stats.setDisplaySlot(DisplaySlot.SIDEBAR);

        countdownDisplay = scoreboard.registerNewTeam("countdown");
        countdownDisplayRegistered = true;
        countdownDisplay.addEntry("Border shrinks in: ");

        borderSizeDisplay = scoreboard.registerNewTeam("border size");
        borderSizeDisplayRegistered = true;
        borderSizeDisplay.addEntry("Border size: ");

        timeUntilNextEvent = getConfig().getInt("border.delay");
        stats.getScore("Border size: ").setScore(2);
        stats.getScore("Border shrinks in: ").setScore(1);

        countdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
            @Override
            public void run() {
                if(timeUntilNextEvent > 0) {
                    scoreboard.getTeam("countdown").setSuffix(timeUntilNextEvent + "s");
                    timeUntilNextEvent--;
                } else {
//                    countdown.unregister();
//                    Bukkit.getScheduler().cancelTask(countdownTask);
                    if(countdownDisplayRegistered)
                        countdownDisplay.unregister();
                        countdownDisplayRegistered = false;
                    updateBorderSizeDisplay();
                }
            }
        }, 0, 20);


        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }

        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "Deathmatch Rules" +
                "\n" + ChatColor.RESET + "Gather resources and fend off other players. " +
                ChatColor.RESET + "Killing a player will give you " + ChatColor.GOLD +
                "1 Golden Apple. " + ChatColor.RESET + "Last player standing wins!");

        int borderSize = getConfig().getInt("border.size");
        border.setSize(borderSize);
        border.setDamageBuffer(0);
        border.setCenter(0, 0);
        updateBorderSizeDisplay();

        for(Player player : Bukkit.getOnlinePlayers()) {
            // Clear inventory, reset health and food bars, apply 60 seconds of speed and 5 seconds of invincibility
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(5);

            for(PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());

            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*5, 100));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20*60, 1));

            double x = Math.random() * borderSize - borderSize / 2.0;
            double z = Math.random() * borderSize - borderSize / 2.0;

            player.teleport(new Location(world, x, world.getHighestBlockYAt((int) x, (int) z) + 1, z));
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1, 1);
            player.sendTitle(ChatColor.RED + "Deathmatch", "", 10, 60, 10);
            player.setGameMode(GameMode.SURVIVAL);
        }

        delayTask = Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                if(inProgress) {
                    border.setSize(getConfig().getInt("border.shrink size"), 60*5);
                    sendTitleToEveryone("", "The border is shrinking!", 10, 60, 10);
                }
            }
        }, 20*getConfig().getInt("border.delay"));
    }

    public void stop() {
        gameEndProcedure();
        sendTitleToEveryone(ChatColor.RED + "Game Over!", "Nobody wins",
                10, 100, 10);
    }

    public void stop(Player winner) {
        gameEndProcedure();
        sendTitleToEveryone(ChatColor.GOLD + winner.getDisplayName() + ChatColor.RESET + " wins!",
                "", 10, 200, 10);

        for(Player player : Bukkit.getOnlinePlayers())
            player.playSound(winner.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1);
    }

    public boolean sendTitleToEveryone(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }

        return true;
    }

    private void gameEndProcedure() {
        inProgress = false;

        if(statsRegistered)
            stats.unregister();

        if(borderSizeDisplayRegistered)
            borderSizeDisplay.unregister();

        if(countdownDisplayRegistered)
            countdownDisplay.unregister();

        statsRegistered = false;
        borderSizeDisplayRegistered = false;
        countdownDisplayRegistered = false;

        Bukkit.getServer().getScheduler().cancelTask(delayTask);
        Bukkit.getServer().getScheduler().cancelTask(countdownTask);

        alivePlayers.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        border.setSize(30000000); // Reset world border

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private void updateBorderSizeDisplay() {
        if(!borderSizeDisplayRegistered)
            return;

        int size = (int) border.getSize();
        scoreboard.getTeam("border size").setSuffix(size + ", " + size);
    }

}
