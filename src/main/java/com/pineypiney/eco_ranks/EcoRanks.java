package com.pineypiney.eco_ranks;

import com.pineypiney.eco_ranks.commands.EcoRanksCommands;
import com.pineypiney.eco_ranks.events.BalChangeEvent;
import com.pineypiney.eco_ranks.events.EcoRanksEvents;
import com.pineypiney.eco_ranks.ranks.Rank;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Logger;

public class EcoRanks extends JavaPlugin {

    public static final String modName = "EcoRanks";
    public static final String modVersion;

    private static final Logger logger = Logger.getLogger("Minecraft");
    private final PluginManager manager = getServer().getPluginManager();
    private static Economy economy = null;
    private static LuckPerms luckPerms = null;

    private final Map<String, Float> bals = new HashMap<>();

    private static final Set<Rank> ranks = new HashSet<>();

    private static String rankupMessage = "You have been added to rank ${rank}!";
    private static String rankupMessage_ = "You have been removed from rank ${rank}!";
    private static boolean globalBroadcast = false;
    private static boolean globalBroadcast_ = false;
    private static boolean actionBar = false;
    private static boolean actionBar_ = false;

    private static boolean removable = false;

    @Override
    public void onLoad() {

        // Loading config file
        loadConfigFile();
    }

    @Override
    public void onEnable() {

        // Loading APIs

        if(!setupVault()){
            logError("Could not load the economy, disabling plugin");
            manager.disablePlugin(this);
            return;
        }

        if(!setupLuckPerms()){
            logError("Could not load LuckPerms, disabling plugin");
            manager.disablePlugin(this);
            return;
        }


        // Custom Events

        if(!setupBalListener()){
            logError("Could not make an economy listener event, disabling plugin");
            manager.disablePlugin(this);
            return;
        }

        // Register Events

        manager.registerEvents(new EcoRanksEvents(), this);

        loadCommand();

        logInfo(ChatColor.GREEN, "Plugin is Enabled");
    }

    @Override
    public void onDisable() {
        bals.clear();
        ranks.clear();
        logInfo(ChatColor.RED, "Plugin is Disabled");
    }

    // Static functions to get the running version and its instance
    public static EcoRanks getInstance(){
        return (EcoRanks)Bukkit.getPluginManager().getPlugin(modName);
    }

    public static String getVersion(){
        return modVersion;
    }

    private void loadConfigFile(){
        FileConfiguration config = this.getConfig();

        // First read all the generic data
        rankupMessage = getDefaultVariable(config, "rankup_message", String.class, rankupMessage);
        rankupMessage_ = getDefaultVariable(config, "rankup_message_", String.class, rankupMessage_);
        globalBroadcast = getDefaultVariable(config, "global_broadcast", Boolean.class, globalBroadcast);
        globalBroadcast_ = getDefaultVariable(config, "global_broadcast_", Boolean.class, globalBroadcast_);
        actionBar = getDefaultVariable(config, "action_bar", Boolean.class, actionBar);
        actionBar_ = getDefaultVariable(config, "action_bar_", Boolean.class, actionBar_);
        removable = getDefaultVariable(config, "removable", Boolean.class, removable);



        // Empty current values in ranks
        ranks.clear();

        // Get a MemorySection from the list of ranks in the config file
        if(config.get("ranks") instanceof MemorySection configRanks){

            Set<String> ids = configRanks.getKeys(false);
            for(String id : ids){

                Class<List<String>> listClass = (Class<List<String>>)(Class<?>)List.class;

                // Get variables from the config file
                String name = getRankVariable(configRanks, id, "name", String.class, id);
                float value = getRankVariable(configRanks, id, "value", Number.class, Float.MAX_VALUE).floatValue();
                List<String> serverCommands = getRankVariable(configRanks, id, "server_commands", listClass, new ArrayList<>());
                List<String> serverCommands_ = getRankVariable(configRanks, id, "server_commands_", listClass, new ArrayList<>());
                List<String> playerCommands = getRankVariable(configRanks, id, "player_commands", listClass, new ArrayList<>());
                List<String> playerCommands_ = getRankVariable(configRanks, id, "player_commands_", listClass, new ArrayList<>());
                String message = getRankVariable(configRanks, id, "rankup_message", String.class, EcoRanks.rankupMessage);
                String message_ = getRankVariable(configRanks, id, "rankup_message_", String.class, EcoRanks.rankupMessage_);
                boolean broadcast = getRankVariable(configRanks, id, "global_broadcast", Boolean.class, EcoRanks.globalBroadcast);
                boolean broadcast_ = getRankVariable(configRanks, id, "global_broadcast_", Boolean.class, EcoRanks.globalBroadcast_);
                boolean actionBar = getRankVariable(configRanks, id, "action_bar", Boolean.class, EcoRanks.actionBar);
                boolean actionBar_ = getRankVariable(configRanks, id, "action_bar_", Boolean.class, EcoRanks.actionBar_);

                boolean removable = getRankVariable(configRanks, id, "removable", Boolean.class, EcoRanks.removable);

                // Create a new rank out of those variables and add it to the set
                Rank newRank = new Rank(name, value, serverCommands, serverCommands_, playerCommands, playerCommands_, message, message_, broadcast, broadcast_, actionBar, actionBar_, removable);
                logInfo(ChatColor.AQUA, "Created rank " + newRank);
                ranks.add(newRank);
            }
        }


        // If the file is currently empty, put in some placeholder values
        if(ranks.isEmpty()){

            logInfo("Cannot find any ranks to load, generating some example ranks");

            ranks.add(new Rank("example_rank", 1000, new String[]{"give ${player} diamond 1", "time set 6000"}, new String[]{"give ${player} dead_bush 1"}, new String[]{"me earned a new rank"}, new String[]{}, "&6You earned the rank ${rank_display} and now have the ${rank_prefix} prefix!", "&7You lost the rank ${rank_display} :(", false, false, true, false, true));
            ranks.add(new Rank("another_rank", 7500));
            ranks.add(new Rank("camel_rank", 300, new String[]{}, new String[]{}, "${player_display} earned a Rank!", true, false, false));

            for(Rank rank : ranks){
                switch (rank.getRankName()) {
                    case "example_rank" -> config.addDefaults(rank.toHashMap("ranks.Example Rank"));
                    case "another_rank" -> config.addDefaults(rank.toHashMap("ranks.Another Rank"));
                    default -> config.addDefaults(rank.toHashMap("ranks.Camel Rank"));
                }
            }

            if(ranks.isEmpty()) EcoRanks.logWarn("Could not load ranks from config file");


            config.options().copyDefaults(true);
            saveConfig();
        }
    }

    // This function is public so that the config file can be reloaded at any time
    public void reloadConfigFile(){
        reloadConfig();
        loadConfigFile();
    }

    // Functions to set up APIs
    private boolean setupVault(){

        // If vault is not provided
        if(manager.getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> econProv = getServer().getServicesManager().getRegistration(Economy.class);
        if(econProv == null) return false;

        economy = econProv.getProvider();
        return true;
    }

    private boolean setupLuckPerms(){
        if(manager.getPlugin("LuckPerms") == null) return false;

        RegisteredServiceProvider<LuckPerms> luckProv = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if(luckProv == null) return false;

        luckPerms = luckProv.getProvider();
        return true;

    }

    // Set up the listener for the custom event called whenever a player's bal changes
    private boolean setupBalListener(){
        int s = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {

            // Loop through every online player, and if their bals have changed
            // then call a new event and update the bals map
            for(Player player : getServer().getOnlinePlayers()){
                float newBal = (float)economy.getBalance(player);
                if(bals.containsKey(player.getName())){
                    if(economy.getBalance(player) != bals.get(player.getName())){
                        BalChangeEvent event = new BalChangeEvent(player, bals.get(player.getName()), newBal);

                        Bukkit.getServer().getPluginManager().callEvent(event);

                        bals.put(player.getName(), newBal);
                    }
                }
                else bals.put(player.getName(), newBal);

            }
        }, 20, 20);

        return s != -1;
    }

    // Set the executor for a command
    private void loadCommand(){
        PluginCommand command = getCommand("ecoranks");
        if(command != null) command.setExecutor(new EcoRanksCommands());
        else logWarn("Could not find definition for command /ecoranks");

    }

    // Add and remove players from the map of online players and their balances
    public void addPlayer(Player player){
        bals.put(player.getName(), (float)economy.getBalance(player));
    }

    public void removePlayer(Player player){
        bals.remove(player.getName());
    }

    // Logging functions
    public static void logInfo(String message){
        logger.info("[" + modName + "] " + message);
    }
    public static void logInfo(ChatColor colour, String message){
        logger.info(colour + "[" + modName + "] " + message);
    }
    public static void logWarn(String message){
        logger.warning(ChatColor.YELLOW + "[" + modName + "] " + message);
    }
    public static void logError(String message){
        logger.severe(ChatColor.RED + "[" + modName + "] " + message);
    }

    // Get the instance of luckperms
    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }

    // Get all loaded ranks
    public static Set<Rank> getRanks() {
        return ranks;
    }

    public static <T> T getDefaultVariable(MemoryConfiguration config, String variable, Class<T> clazz, T defaultValue){
        Object value = config.get(variable);
        if(value == null) return defaultValue;

        try{
            return clazz.cast(value);
        }
        catch (ClassCastException e){
            logWarn("Cannot cast " + value + " to " + clazz);
            return defaultValue;
        }
    }

    // Retrieve a variable for a rank from a yml format
    public static <T> T getRankVariable(MemorySection ranks, String rankId, String variableName, Class<T> clazz, T defaultValue){
        Object value = ranks.get(rankId + "." + variableName);
        if(value == null) return defaultValue;

        try{
            return clazz.cast(value);
        }
        catch (ClassCastException e){
            logWarn("Cannot cast " + value + " to " + defaultValue.getClass());
            return defaultValue;
        }

    }

    // Getters for the default values in ranks
    public static String getRankupMessage() {
        return rankupMessage;
    }

    public static String getRankupMessage_() {
        return rankupMessage_;
    }

    public static boolean isBroadcastGlobal() {
        return globalBroadcast;
    }

    public static boolean isBroadcastGlobal_() {
        return globalBroadcast_;
    }

    public static boolean isActionBar() {
        return actionBar;
    }

    public static boolean isActionBar_() {
        return actionBar_;
    }

    public static boolean isRemovable() {
        return removable;
    }

    static {
        String version;
        try{
            Yaml yaml = new Yaml();
            String s = File.pathSeparator;
            File yamlFile = new File("src" + s + "main" + s + "resource" + s + "plugin.yml");
            version = ((Map<String, String>)yaml.load(new FileInputStream(yamlFile))).get("version");
        }
        catch(FileNotFoundException e){
            version = "1.0.0";
        }

        modVersion = version;
    }
}
