package me.param.plugins.deathmatch;

import me.param.plugins.deathmatch.events.OnBreakBlock;
import me.param.plugins.deathmatch.events.OnDeath;
import me.param.plugins.deathmatch.events.OnPlayerJoin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;

public final class Deathmatch extends JavaPlugin {
    public boolean inProgress = false;
    private List<Player> playerList = new ArrayList<>(Bukkit.getOnlinePlayers());
    private World world;
    private WorldBorder border;
    private int taskID;

    @Override
    public void onEnable() {
        world = Bukkit.getWorld("world");
        border = world.getWorldBorder();

        getLogger().info("Deathmatch has been enabled!");
        getCommand("deathmatch").setExecutor(new Commands(this));
        getCommand("deathmatch").setTabCompleter(new CommandsAutocompleter());

        getServer().getPluginManager().registerEvents(new OnDeath(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new OnBreakBlock(this), this);
    }

    public void start() {
        inProgress = true;

        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "Deathmatch Rules" +
                "\n" + ChatColor.RESET + "Gather resources and fend off other players. " +
                ChatColor.RESET + "Killing a player will give you " + ChatColor.GOLD +
                "1 Golden Apple. " + ChatColor.RESET + "Last player standing wins!");

        border.setSize(100);
        border.setDamageBuffer(0);
        border.setCenter(0, 0);

        playerList = new ArrayList<>(Bukkit.getOnlinePlayers());

        for(Player player : playerList) {
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(5);

            //TODO randomize spawn locations
            player.teleport(new Location(world, 0, world.getHighestBlockYAt(0, 0), 0));
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
        inProgress = false;
        Bukkit.getServer().getScheduler().cancelTask(taskID);
        Bukkit.broadcastMessage("The game has ended!");
        border.setSize(30000000); //Reset worldborder

        for(Player player : playerList) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private boolean sendTitleToEveryone(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        playerList = new ArrayList<>(Bukkit.getOnlinePlayers());

        for(Player player : playerList) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }

        return true;
    }

}
