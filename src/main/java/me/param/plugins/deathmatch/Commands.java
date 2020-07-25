package me.param.plugins.deathmatch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {
    private Deathmatch game;

    public Commands(Deathmatch plugin) {
        this.game = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("deathmatch.start")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to run that command!");
            return true;
        }

        try {
            if(args[0].equalsIgnoreCase("start")) {
                if(!game.inProgress)
                    game.start();
                else
                    sender.sendMessage(ChatColor.RED + "The game is already in progress!");
                return true;

            } else if(args[0].equalsIgnoreCase("stop")) {
                if(game.inProgress) {
                    game.stop();
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Stopped the game.");
                } else {
                    sender.sendMessage(ChatColor.RED + "The game has not started yet!");
                }
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        return false;
    }
}
