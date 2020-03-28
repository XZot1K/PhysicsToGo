/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import xzot1k.plugins.ptg.core.Commands;
import xzot1k.plugins.ptg.core.Listeners;
import xzot1k.plugins.ptg.core.Manager;
import xzot1k.plugins.ptg.core.objects.LocationClone;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class PhysicsToGo extends JavaPlugin {

    private static PhysicsToGo pluginInstance;
    private Manager manager;

    private String serverVersion;
    private boolean particleAPI;

    private FileConfiguration advancedConfig, langConfig;
    private File advancedFile, langFile;

    public static PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private static void setPluginInstance(PhysicsToGo pluginInstance) {
        PhysicsToGo.pluginInstance = pluginInstance;
    }

    @Override
    public void onEnable() {
        setPluginInstance(this);

        saveDefaultConfigs();
        updateConfigs();

        // very simply loop that runs once to see if current server version World class has the new particle API.
        setParticleAPI(false);
        for (Method method : World.class.getMethods()) {
            if (method.getName().contains("spawnParticle")) {
                setParticleAPI(true);
                break;
            }
        }

        // identifies and initializes the server's version.
        setServerVersion(getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);

        // creates and registers a new instance of the Manager class.
        setManager(new Manager(this));

        // registers the command class and sets up the tab completion.
        PluginCommand command = getCommand("physicstogo");
        if (command != null) {
            Commands commands = new Commands(this);
            command.setExecutor(commands);
            command.setTabCompleter(commands);
        }

        // registers the listeners class
        getServer().getPluginManager().registerEvents(new Listeners(this), this);

    }

    @Override
    public void onDisable() {
        initiateCleanUpTime();
    }

    // configurations moderation methods

    /**
     * Cleans up everything, regenerating saved states and cancelling currently active events.
     */
    public void initiateCleanUpTime() {
        for (BlockState blockState : getManager().getSavedBlockStates())
            blockState.update(true, false);

        for (Map.Entry<LocationClone, ItemStack[]> contents : getManager().getSavedContainerContents().entrySet()) {
            Location location = contents.getKey().asBukkitLocation();
            if (location.getBlock().getState() instanceof InventoryHolder) {
                InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
                ih.getInventory().setContents(contents.getValue());
            }
        }

        for (Map.Entry<LocationClone, String[]> contents : getManager().getSavedSignData().entrySet()) {
            Location location = contents.getKey().asBukkitLocation();
            if (location.getBlock().getState() instanceof Sign) {
                Sign sign = (Sign) location.getBlock().getState();
                for (int i = -1; ++i < contents.getValue().length; )
                    sign.setLine(i, contents.getValue()[i]);
                sign.update(true, false);
            }
        }

        getServer().getScheduler().cancelTasks(this);
    }

    // general helper methods
    /*
     * Sends a formatted message with a logging level to the console.
     */
    public void log(Level level, String message) {
        getPluginInstance().getServer().getLogger().log(level, "[" + getPluginInstance().getDescription().getName() + "] " + message);
    }

    private void updateConfigs() {
        long startTime = System.currentTimeMillis();
        int totalUpdates = 0;

        final String[] configNames = {"config", "advanced", "lang"};
        for (int i = -1; ++i < configNames.length; ) {
            String name = configNames[i];

            InputStream inputStream = getClass().getResourceAsStream("/" + name + ".yml");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
            int updateCount = updateKeys(yaml, name.equalsIgnoreCase("lang") ? getLangConfig() : name.equalsIgnoreCase("config") ? getConfig() : getAdvancedConfig());

            try {
                inputStream.close();
                reader.close();
            } catch (IOException e) {
                log(Level.WARNING, e.getMessage());
            }

            if (updateCount > 0) {
                switch (name) {
                    case "config":
                        saveConfig();
                        break;
                    case "advanced":
                        saveAdvancedConfig();
                        break;
                    case "lang":
                        saveLangConfig();
                        break;
                    default:
                        break;
                }

                totalUpdates += updateCount;
            }
        }

        final boolean wasUpdated = (totalUpdates > 0);
        if (wasUpdated) reloadConfigs();
        log(Level.INFO, (wasUpdated ? (totalUpdates + " thing(s) were fixed, updated, or removed from all the configuration together.")
                : "Everything inside the configuration seems to be up to date. (Took ") + (System.currentTimeMillis() - startTime) + "ms)");
    }

    private int updateKeys(FileConfiguration jarYaml, FileConfiguration currentYaml) {
        int updateCount = 0;
        ConfigurationSection currentConfigurationSection = currentYaml.getConfigurationSection(""), latestConfigurationSection = jarYaml.getConfigurationSection("");
        if (currentConfigurationSection != null && latestConfigurationSection != null) {
            Set<String> newKeys = latestConfigurationSection.getKeys(true), currentKeys = currentConfigurationSection.getKeys(true);
            for (String updatedKey : newKeys)
                if (!currentKeys.contains(updatedKey)) {
                    currentYaml.set(updatedKey, jarYaml.get(updatedKey));
                    updateCount++;
                }

            for (String currentKey : currentKeys)
                if (!newKeys.contains(currentKey)) {
                    currentYaml.set(currentKey, null);
                    updateCount++;
                }
        }

        return updateCount;
    }

    /**
     * Saves the language configuration to the disk.
     */
    public void saveLangConfig() {
        if (langConfig != null && langFile != null)
            try {
                getLangConfig().save(langFile);
            } catch (IOException e) {
                log(Level.WARNING, e.getMessage());
            }
    }

    /**
     * Saves the advanced configuration to the disk.
     */
    public void saveAdvancedConfig() {
        if (advancedConfig != null && advancedFile != null)
            try {
                getAdvancedConfig().save(advancedFile);
            } catch (IOException e) {
                log(Level.WARNING, e.getMessage());
            }
    }

    /**
     * Obtains the language configuration instance.
     *
     * @return the language file configuration.
     */
    public FileConfiguration getLangConfig() {
        if (langConfig == null) reloadLangConfig();
        return langConfig;
    }

    /**
     * Obtains the advanced configuration instance.
     *
     * @return the advanced file configuration.
     */
    public FileConfiguration getAdvancedConfig() {
        if (advancedConfig == null) reloadAdvancedConfig();
        return advancedConfig;
    }

    /**
     * Reloads the current instance of the language configuration.
     */
    public void reloadLangConfig() {
        if (langFile == null) langFile = new File(getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        configReloadHelper(getResource("lang.yml"), langConfig);
    }

    /**
     * Reloads the current instance of the advanced configuration.
     */
    public void reloadAdvancedConfig() {
        if (advancedFile == null) advancedFile = new File(getDataFolder(), "advanced.yml");
        advancedConfig = YamlConfiguration.loadConfiguration(advancedFile);
        configReloadHelper(getResource("advanced.yml"), advancedConfig);
    }

    /**
     * Reloads all loaded configurations.
     */
    public void reloadConfigs() {
        reloadConfig();

        if (langFile == null) langFile = new File(getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream path = this.getResource("lang.yml");
        configReloadHelper(path, langConfig);

        if (advancedFile == null) advancedFile = new File(getDataFolder(), "advanced.yml");
        advancedConfig = YamlConfiguration.loadConfiguration(advancedFile);

        InputStream path2 = this.getResource("advanced.yml");
        configReloadHelper(path2, advancedConfig);
    }

    public void saveDefaultConfigs() {
        saveDefaultConfig();
        if (langFile == null) langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) saveResource("lang.yml", false);
        if (advancedFile == null) advancedFile = new File(getDataFolder(), "advanced.yml");
        if (!advancedFile.exists()) saveResource("advanced.yml", false);
        reloadConfigs();
    }

    private void configReloadHelper(InputStream path, FileConfiguration fileConfiguration) {
        if (path != null) {
            Reader defConfigStream = new InputStreamReader(path, StandardCharsets.UTF_8);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            fileConfiguration.setDefaults(defConfig);

            try {
                path.close();
                defConfigStream.close();
            } catch (IOException e) {
                log(Level.WARNING, e.getMessage());
            }
        }
    }

    /**
     * Obtains the manager class containing all important methods and API functions.
     *
     * @return The manager class.
     */
    public Manager getManager() {
        return manager;
    }

    private void setManager(Manager manager) {
        this.manager = manager;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    private void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public boolean hasParticleAPI() {
        return particleAPI;
    }

    private void setParticleAPI(boolean particleAPI) {
        this.particleAPI = particleAPI;
    }

}
