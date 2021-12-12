package com.pineypiney.eco_ranks.ranks;

import com.pineypiney.eco_ranks.EcoRanks;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;


// This class is used to store the functionality of each rank
public class Rank {

    // All the variables are private and final,
    // to make new ranks the '/ecoranks reload' command must be run
    @NotNull
    private final String rankName;
    private final float value;

    @NotNull
    private final String[] serverCommands;
    @NotNull
    private final String[] playerCommands;

    @NotNull
    private final String rankupMessage;
    private final boolean globalBroadcast;
    private final boolean actionBar;

    public Rank(@NotNull String rankName, float value, String[] serverCommands, String[] playerCommands, @NotNull String rankupMessage, boolean globalBroadcast, boolean actionBar) {
        this.rankName = rankName;
        this.value = value;
        this.serverCommands = serverCommands;
        this.playerCommands = playerCommands;
        this.rankupMessage = rankupMessage;
        this.globalBroadcast = globalBroadcast;
        this.actionBar = actionBar;
    }

    public Rank(@NotNull String rankName, float value, Collection<String> serverCommands, Collection<String> playerCommands, String rankupMessage, boolean globalBroadcast, boolean actionBar) {
        this(rankName, value, serverCommands.toArray(new String[0]), playerCommands.toArray(new String[0]), rankupMessage, globalBroadcast, actionBar);
    }

    public Rank(@NotNull String rankName, float value){
        this(rankName, value, new String[0], new String[0], EcoRanks.getRankupMessage(), EcoRanks.isBroadcastGlobal(), EcoRanks.isActionBar());
    }

    // Gets a HashMap of this rank's variables, used to generate yml files with a ranks data
    public HashMap<String, Object> toHashMap(String name){
        return new HashMap<>(){{
            if(!getRankName().equals(name.substring(name.lastIndexOf(".") + 1))) put(name + "." + "name", getRankName());
            if(getValue() != Float.MAX_VALUE) put(name + "." + "value", getValue());
            if(getServerCommands().length > 0) put(name + "." + "server_commands", getServerCommands());
            if(getPlayerCommands().length > 0) put(name + "." + "player_commands", getPlayerCommands());
            if(!getRankupMessage().equals(EcoRanks.getRankupMessage())) put(name + "." + "rankup_message", getRankupMessage());
            if(isBroadcastGlobal() != EcoRanks.isBroadcastGlobal()) put(name + "." + "global_broadcast", isBroadcastGlobal());
            if(isActionBar() != EcoRanks.isActionBar()) put(name + "." + "action_bar", isActionBar());
        }};
    }

    // Getters for all the ranks variables
    public @NotNull String getRankName() {
        return rankName;
    }

    public float getValue() {
        return value;
    }

    public String[] getServerCommands() {
        return serverCommands;
    }

    public String[] getPlayerCommands() {
        return playerCommands;
    }

    public @NotNull String getRankupMessage() {
        return rankupMessage;
    }

    public boolean isBroadcastGlobal() {
        return globalBroadcast;
    }

    public boolean isActionBar() {
        return actionBar;
    }

    // Override toString for debugging purposes
    @Override
    public String toString() {
        return "Rank{" +
                "rankName='" + rankName + '\'' +
                ", value=" + value +
                ", serverCommands=" + Arrays.toString(serverCommands) +
                ", playerCommands=" + Arrays.toString(playerCommands) +
                ", rankupMessage='" + rankupMessage + '\'' +
                ", globalBroadcast=" + globalBroadcast +
                '}';
    }
}
