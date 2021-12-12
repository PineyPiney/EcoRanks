package com.pineypiney.eco_ranks.events;

import com.pineypiney.eco_ranks.EcoRanks;
import com.pineypiney.eco_ranks.ranks.Rank;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EcoRanksEvents implements Listener {

    private static final EcoRanks plugin;
    private static Set<Rank> ranks;
    private static final LuckPerms luckPerms = EcoRanks.getLuckPerms();

    @EventHandler
    public static void onBalChange(BalChangeEvent event){
        // Get LuckPerms variables User and a list of names of groups they are already part of
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(event.getPlayer());
        Collection<Group> groups = user.getInheritedGroups(user.getQueryOptions());
        List<String> groupNames = groups.stream().map(Group::getName).collect(Collectors.toList());

        double newBal = event.getNewBal();

        for(Rank rank : ranks){

            // If the player's bal has exceeded or equaled requirements
            if(newBal >= rank.getValue()){
                String rankName = rank.getRankName();

                // If the player is not already in the rank
                if(!groupNames.contains(rankName)){

                    // Add player to new group
                    InheritanceNode node = InheritanceNode.builder(rankName).value(true).build();
                    DataMutateResult result = user.data().add(node);
                    luckPerms.getUserManager().saveUser(user);

                    // Report success of operation
                    if(result == DataMutateResult.SUCCESS) {

                        String[] serverCommands = rank.getServerCommands();
                        String[] playerCommands = rank.getPlayerCommands();

                        String message = replaceValues(rank.getRankupMessage(), event, rank);
                        EcoRanks.logInfo("Rank Up message is " + message);
                        EcoRanks.logInfo("Players ranks are now " + user.getInheritedGroups(user.getQueryOptions()).stream().map(Group::getName).collect(Collectors.toList()));

                        for(String com : serverCommands){
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaceValues(com, event, rank));
                        }

                        for(String com : playerCommands){
                            event.getPlayer().performCommand(replaceValues(com, event, rank));
                        }

                        if(rank.isBroadcastGlobal()) Bukkit.broadcastMessage(message);
                        else event.getPlayer().spigot().sendMessage(rank.isActionBar() ? ChatMessageType.ACTION_BAR : ChatMessageType.CHAT, new TextComponent(message));
                    }

                    // If the operation failed for any reason other than the player already being in the group,
                    // log the failure in the console
                    else if(result != DataMutateResult.FAIL_ALREADY_HAS) EcoRanks.logWarn("Attempted to give rank " + rankName + " to " + event.getPlayer().getName() + " but something went wrong with result " + result);

                }
            }
        }
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent event){
        plugin.addPlayer(event.getPlayer());
    }

    @EventHandler
    public static void onPlayerQuit(PlayerQuitEvent event){
        plugin.removePlayer(event.getPlayer());
    }

    public static String replaceValues(String string, BalChangeEvent event, Rank rank){

        // This function replaces placeholder values within strings with the values they represent
        string = string.replace("${rank}", rank.getRankName())
                .replace("${player}", event.getPlayer().getName())
                .replace("${player_display}", event.getPlayer().getDisplayName());

        Group g = luckPerms.getGroupManager().getGroup(rank.getRankName());
        if(g != null){
            // If the group exists then replace values accordingly
            string = string.replace("${rank_display}", g.getDisplayName() != null ? g.getDisplayName() : g.getName());

            CachedMetaData meta = g.getCachedData().getMetaData();
            string = string.replace("${rank_prefix}", meta.getPrefix() != null ? meta.getPrefix() : "")
                    .replace("${rank_suffix}", meta.getSuffix() != null ? meta.getSuffix() : "");
        }
        else{
            // If group could not be found then remove all placeholders
            string = string.replace("${rank_display}", "")
                    .replace("${rank_prefix}", "")
                    .replace("${rank_suffix}", "");
        }

        // Replace all & that aren't followed by spaces with §, unicode U+00A7
        char[] chars = string.toCharArray();
        for(int i = 0; i < chars.length; i++){
            if(chars[i] == '&' && chars[i + 1] != ' ') chars[i] = '\u00A7';
        }

        return String.copyValueOf(chars);
    }

    static {
        plugin = EcoRanks.getInstance();
        if (plugin != null) {

            ranks = EcoRanks.getRanks();
            if(ranks != null){
                EcoRanks.logInfo(ChatColor.GREEN, "Loaded ranks from config file: " + ranks.stream().reduce("", (s, rank) -> s + rank.getRankName(), String::concat));
            }
            else EcoRanks.logWarn("Could not load ranks from config file");

        }
    }
}
