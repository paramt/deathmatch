package me.param.plugins.deathmatch;

import me.param.plugins.deathmatch.events.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.HashMap;

public final class Deathmatch extends JavaPlugin {
    public boolean inProgress = false;
    public ArrayList<Player> alivePlayers;
    public Scoreboard scoreboard;
    public HashMap<String, Integer> kills = new HashMap<>();

    private World world;
    private WorldBorder border;
    private Objective stats;
    private Team countdownDisplay, borderSizeDisplay;

    private int timeUntilNextEvent, delayTask, countdownTask;

    @Override
    public void onEnable() {
        getLogger().info("Deathmatch has been enabled!");
        getCommand("deathmatch").setExecutor(new Commands(this));
        getCommand("deathmatch").setTabCompleter(new CommandsAutocompleter());

        getServer().getPluginManager().registerEvents(new OnDeath(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerLeave(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new OnBreakBlock(this), this);
        getServer().getPluginManager().registerEvents(new OnPlayerHit(), this);

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        world = Bukkit.getWorld("world");
        border = world.getWorldBorder();

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective("Health", Criterias.HEALTH, "Health", RenderType.HEARTS)
                .setDisplaySlot(DisplaySlot.PLAYER_LIST);
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
        stats.setDisplaySlot(DisplaySlot.SIDEBAR);

        countdownDisplay = scoreboard.registerNewTeam("countdown");
        countdownDisplay.addEntry("Border");

        borderSizeDisplay = scoreboard.registerNewTeam("border size");
        borderSizeDisplay.addEntry("Border size: ");

        timeUntilNextEvent = getConfig().getInt("border.delay");
        stats.getScore("Border size: ").setScore(4);
        stats.getScore("Border").setScore(3);
        stats.getScore("").setScore(2);
        stats.getScore(ChatColor.BOLD + "" + ChatColor.UNDERLINE + "Kills").setScore(1);

        countdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if(timeUntilNextEvent > 0) {
                scoreboard.getTeam("countdown").setSuffix(" shrinks in: " + timeUntilNextEvent + "s");
            } else if (timeUntilNextEvent > -1 * getConfig().getInt("border.shrink time")){
                scoreboard.getTeam("countdown").setSuffix(" is shrinking!");
            } else {
                scoreboard.getTeam("countdown").setSuffix(" has shrunk");
            }

            updateBorderSizeDisplay();
            timeUntilNextEvent--;
        }, 0, 20);

        for(int i = 0; i < alivePlayers.size(); i++) {
            Player player = alivePlayers.get(i);
            String playerName = alivePlayers.get(i).getDisplayName();
            Team playerTeam = scoreboard.registerNewTeam(playerName);

            playerTeam.addEntry(playerName + ": ");
            stats.getScore(playerName + ": ").setScore(-i);
            playerTeam.setSuffix("0");

            player.setScoreboard(scoreboard);
            kills.put(playerName, 0);
        }

        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "Deathmatch Rules" +
                "\n" + ChatColor.RESET + "Gather resources and fend off other players. " +
                ChatColor.RESET + "Killing a player will give you " + ChatColor.GOLD +
                "1 Golden Apple. " + ChatColor.RESET + "Last player standing wins!");

        int borderSize = getConfig().getInt("border.size");
        border.setSize(borderSize);
        border.setDamageBuffer(0);
        border.setCenter(0, 0);
        world.setTime(0);
        world.setStorm(false);
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

        delayTask = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            if(inProgress) {
                border.setSize(getConfig().getInt("border.shrink size"), getConfig().getInt("border.shrink time"));
                sendTitleToEveryone("", "The border is shrinking!", 10, 60, 10);
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

    public void handleDeath(Player player, Player killer) {
        player.setGameMode(GameMode.SPECTATOR);

        // Remove player from alivePlayers
        for(int i = 0; i < alivePlayers.size(); i++) {
            if(alivePlayers.get(i).equals(player)) {
                alivePlayers.remove(i);
                i--;
            }
        }

        // Remove player from scoreboard
        scoreboard.resetScores(player.getDisplayName() + ": ");

        // End the game if only 1 player is alive
        if(alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);
            stop(winner);
        }

        // End the game if no players are alive
        else if(alivePlayers.size() == 0) {
            stop();
        }

        // Reward the player
        else if(killer != null) {
            player.teleport(killer.getLocation().add(0, 5, 0));
            killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));
            killer.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);

            // Update kill count
            int killCount = kills.get(killer.getDisplayName()) + 1;
            kills.put(killer.getDisplayName(), killCount);
            scoreboard.getTeam(killer.getDisplayName()).setSuffix(Integer.toString(killCount));
        }
    }

    private void updateBorderSizeDisplay() {
        int size = (int) border.getSize();
        scoreboard.getTeam("border size").setSuffix(size + ", " + size);
    }

    private boolean sendTitleToEveryone(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }

        return true;
    }

    private void gameEndProcedure() {
        inProgress = false;

        for(Team team : scoreboard.getTeams()){
            team.unregister();
        }

        Bukkit.getServer().getScheduler().cancelTask(delayTask);
        Bukkit.getServer().getScheduler().cancelTask(countdownTask);

        stats.unregister();

        alivePlayers.clear();
        kills.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }

        border.setSize(getConfig().getInt("border.size")); // Reset world border

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

}
