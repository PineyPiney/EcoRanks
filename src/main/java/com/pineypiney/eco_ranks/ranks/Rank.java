package com.pineypiney.eco_ranks.ranks;

import com.pineypiney.eco_ranks.EcoRanks;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


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
    private final String[] serverCommands_;
    @NotNull
    private final String[] playerCommands;
    @NotNull
    private final String[] playerCommands_;

    @NotNull
    private final String rankupMessage;
    @NotNull
    private final String rankupMessage_;
    private final boolean globalBroadcast;
    private final boolean globalBroadcast_;
    private final boolean actionBar;
    private final boolean actionBar_;

    private final boolean removable;

    // A list of processes, ordered by the strings they should be processed on
    // Each process takes a location as a parameter, and outputs the string that should replace the key
    private final Map<String, Function<Location, String>> processes = new HashMap<>();

    public Rank(@NotNull String rankName, float value, String[] serverCommands, String[] serverCommands_, String[] playerCommands, String[] playerCommands_, String rankupMessage, String rankupMessage_, boolean globalBroadcast, boolean globalBroadcast_, boolean actionBar, boolean actionBar_, boolean removable) {
        this.rankName = rankName;
        this.value = value;
        this.serverCommands = preprocessStrings(serverCommands);
        this.serverCommands_ = preprocessStrings(serverCommands_);
        this.playerCommands = preprocessStrings(playerCommands);
        this.playerCommands_ = preprocessStrings(playerCommands_);
        this.rankupMessage = preprocessString(rankupMessage);
        this.rankupMessage_ = preprocessString(rankupMessage_);
        this.globalBroadcast = globalBroadcast;
        this.globalBroadcast_ = globalBroadcast_;
        this.actionBar = actionBar;
        this.actionBar_ = actionBar_;

        this.removable = removable;
    }

    public Rank(@NotNull String rankName, float value, Collection<String> serverCommands, Collection<String> serverCommands_, Collection<String> playerCommands, Collection<String> playerCommands_, String rankupMessage, String rankupMessage_, boolean globalBroadcast, boolean globalBroadcast_, boolean actionBar, boolean actionBar_, boolean removable) {
        this(rankName, value, serverCommands.toArray(new String[0]), serverCommands_.toArray(new String[0]), playerCommands.toArray(new String[0]), playerCommands_.toArray(new String[0]), rankupMessage, rankupMessage_, globalBroadcast, globalBroadcast_, actionBar, actionBar_, removable);
    }

    public Rank(@NotNull String rankName, float value, String[] serverCommands, String[] playerCommands, @NotNull String rankupMessage, boolean globalBroadcast, boolean actionBar, boolean removable) {
        this(rankName, value, serverCommands, new String[0], playerCommands, new String[0], rankupMessage, EcoRanks.getRankupMessage_(), globalBroadcast, EcoRanks.isBroadcastGlobal_(), actionBar, EcoRanks.isActionBar_(), removable);
    }

    public Rank(@NotNull String rankName, float value, Collection<String> serverCommands, Collection<String> playerCommands, String rankupMessage, boolean globalBroadcast, boolean actionBar, boolean removable) {
        this(rankName, value, serverCommands.toArray(new String[0]), playerCommands.toArray(new String[0]), rankupMessage, globalBroadcast, actionBar, removable);
    }

    public Rank(@NotNull String rankName, float value){
        this(rankName, value, new String[0], new String[0], EcoRanks.getRankupMessage(), EcoRanks.isBroadcastGlobal(), EcoRanks.isActionBar(), EcoRanks.isRemovable());
    }

    private String preprocessString(String string){

        char[] chars = string.toCharArray();
        for(int i = 0; i < chars.length; i++){
            // Replace all & that aren't followed by spaces with §, unicode U+00A7
            if(chars[i] == '&' && chars[i + 1] != ' ') chars[i] = '\u00A7';

            // If the next few characters indicate the beginning of a placeholders
            // The placeholders processed here have the form ${<letter><operator><number>}
            // e.g. ${x+2}

            // Where the letter is the position of the player in 1 dimension
            // (capitals are integer i.e. block pos, lowercase is exact position as a double)
            // And the number is operated on the value of that position according to the given operator

            if(chars[i] == '$' &&
                    chars[i + 1] == '{' &&
                    "xyzXYZ".contains(String.valueOf(chars[i+2])) &&
                    "+-*/".contains(String.valueOf(chars[i+3]))){

                int j = i + 4;
                boolean integer = true;

                // Find the index of the closing '}',
                // and determine whether the parameter is an integer or not along the way
                while(j < string.length() && chars[j] != '}'){
                    j++;
                    if(chars[j] == '.') integer = false;
                }

                // The string that represents the parameter
                String number = String.copyValueOf(chars).substring(i+4, j);

                // Try to parse the number to a double,
                // if needed it will be cast back to an integer later
                double param;
                try{
                    param = Double.parseDouble(number);
                }
                catch (NumberFormatException e){
                    EcoRanks.logWarn("Could not parse " + number);
                    e.printStackTrace();
                    continue;
                }

                // replace is the full string that should be replaced
                String replace;
                try{
                    replace = string.substring(i, j+1);
                }
                catch(IndexOutOfBoundsException e){
                    e.printStackTrace();
                    continue;
                }

                // Create the process
                int finalI = i;
                boolean finalInteger = integer;
                processes.put(replace, (location) -> {

                    // pos is the variable represented by the letter at the beginning
                    // capitalised means it is the block position i.e. an integer,
                    // otherwise it is the precise position of the player, a double
                    double pos = switch (chars[finalI + 2]) {
                        case 'x' -> location.getX();
                        case 'y' -> location.getY();
                        case 'z' -> location.getZ();
                        case 'X' -> (double)location.getBlockX();
                        case 'Y' -> (double)location.getBlockY();
                        case 'Z' -> (double)location.getBlockZ();

                        // default is never reached, but java requires something goes here
                        default -> 0.0;
                    };

                    // Perform the final operation and put the output in the value variable
                    double value = switch(chars[finalI + 3]){
                        case '+' -> pos + param;
                        case '-' -> pos - param;
                        case '*' -> pos * param;
                        case '/' -> pos / param;
                        default -> pos;
                    };

                    // If the ascii value of the character is less than 91
                    // then it is a capital, and therefore represent an integer value

                    // Value is cast to int before being cast to a string
                    if(chars[finalI + 2] < 91 && finalInteger){
                        return String.valueOf((int) value);
                    }

                    // If either values is a double then leave the output as a double
                    return String.valueOf(value);
                });
            }
        }

        return String.copyValueOf(chars);
    }

    private String[] preprocessStrings(String[] strings){

        String[] processedStrings = new String[strings.length];
        for(int i = 0; i < strings.length; i++){
            processedStrings[i] = preprocessString(strings[i]);
        }
        return processedStrings;
    }

    // Gets a HashMap of this rank's variables, used to generate yml files with a ranks data
    public HashMap<String, Object> toHashMap(String name){
        return new HashMap<>(){{
            if(!getRankName().equals(name.substring(name.lastIndexOf(".") + 1))) put(name + "." + "name", getRankName());
            if(getValue() != Float.MAX_VALUE) put(name + "." + "value", getValue());
            if(getServerCommands().length > 0) put(name + "." + "server_commands", getServerCommands());
            if(getServerCommands_().length > 0) put(name + "." + "server_commands_", getServerCommands_());
            if(getPlayerCommands().length > 0) put(name + "." + "player_commands", getPlayerCommands());
            if(getPlayerCommands_().length > 0) put(name + "." + "player_commands_", getPlayerCommands_());
            if(!getRankupMessage().equals(EcoRanks.getRankupMessage())) put(name + "." + "rankup_message", getRankupMessage());
            if(!getRankupMessage_().equals(EcoRanks.getRankupMessage_())) put(name + "." + "rankup_message_", getRankupMessage_());
            if(isBroadcastGlobal() != EcoRanks.isBroadcastGlobal()) put(name + "." + "global_broadcast", isBroadcastGlobal());
            if(isBroadcastGlobal_() != EcoRanks.isBroadcastGlobal_()) put(name + "." + "global_broadcast_", isBroadcastGlobal_());
            if(isActionBar() != EcoRanks.isActionBar()) put(name + "." + "action_bar", isActionBar());
            if(isActionBar_() != EcoRanks.isActionBar_()) put(name + "." + "action_bar_", isActionBar_());

            if(isRemovable() != EcoRanks.isRemovable()) put(name + "." + "removable", isRemovable());
        }};
    }

    // Getters for all the ranks variables
    public @NotNull String getRankName() {
        return rankName;
    }

    public float getValue() {
        return value;
    }

    public @NotNull String[] getServerCommands() {
        return serverCommands;
    }

    public @NotNull String[] getServerCommands_() {
        return serverCommands_;
    }

    public @NotNull String[] getPlayerCommands() {
        return playerCommands;
    }

    public @NotNull String[] getPlayerCommands_() {
        return playerCommands_;
    }

    public @NotNull String getRankupMessage() {
        return rankupMessage;
    }

    public @NotNull String getRankupMessage_() {
        return rankupMessage_;
    }

    public boolean isBroadcastGlobal() {
        return globalBroadcast;
    }

    public boolean isBroadcastGlobal_() {
        return globalBroadcast_;
    }

    public boolean isActionBar() {
        return actionBar;
    }

    public boolean isActionBar_() {
        return actionBar_;
    }

    public boolean isRemovable() {
        return removable;
    }

    public Map<String, Function<Location, String>> getProcesses() {
        return processes;
    }

    // Override toString for debugging purposes

    @Override
    public String toString() {
        String s = "";
        for(Map.Entry<String, Object> entry : toHashMap("").entrySet()){
            s = s.concat("\u00A7r \n" + entry.getKey().substring(1) + ": " + entry.getValue());
        }
        return s;
    }
}
