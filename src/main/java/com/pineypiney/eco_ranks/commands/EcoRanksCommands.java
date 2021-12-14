package com.pineypiney.eco_ranks.commands;

import com.pineypiney.eco_ranks.EcoRanks;
import com.pineypiney.eco_ranks.ranks.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class EcoRanksCommands implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {

        // All commands are subcommands of ecoranks
        if(command.getName().equalsIgnoreCase("ecoranks")){

            // If no subcommand is specified then return information
            // about the version of ecoranks currently being used
            if(args.length == 0){
                if(sender.hasPermission("ecoranks.ecoranks"))
                    sender.sendMessage("Using " + EcoRanks.modName + " version " + EcoRanks.getVersion());
            }
            else{
                switch (args[0]) {

                    // '/ecoranks ranks' will return a list of rank names and their associated bal values
                    case "ranks" -> {
                        if(sender.hasPermission("ecoranks.ranks")){
                            Set<Rank> ranks = EcoRanks.getRanks();
                            sender.sendMessage("Ranks");
                            for(Rank rank : ranks){
                                sender.sendMessage("Rank " + rank.getRankName() + " -> " + rank.getValue());
                            }
                        }
                        else sender.sendMessage("You do not have permission to view the ranks");
                    }

                    // '/ecoranks reload' will reload all ranks from config.yml
                    case "reload" -> {
                        if(sender.hasPermission("ecoranks.reload")) {
                            sender.sendMessage("Reloading Ranks...");
                            EcoRanks.getInstance().reloadConfigFile();
                        }
                        else sender.sendMessage("You do not have permission to reload the ranks");
                    }

                    // If there are subcommands but none of them are defined the user probably made a typo
                    default -> sender.sendMessage("This command does not exist");
                }
            }

            return true;
        }

        return false;
    }


}
