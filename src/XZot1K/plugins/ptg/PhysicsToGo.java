package XZot1K.plugins.ptg;

import XZot1K.plugins.ptg.core.Listeners;
import XZot1K.plugins.ptg.core.PhysicsToGoCommand;
import XZot1K.plugins.ptg.core.checkers.Metrics;
import XZot1K.plugins.ptg.core.checkers.UpdateChecker;
import XZot1K.plugins.ptg.core.internals.LandsHook;
import XZot1K.plugins.ptg.core.objects.DoubleChest;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class PhysicsToGo extends JavaPlugin {

    private static PhysicsToGo pluginInstance;
    public List<BlockState> savedStates;
    private List<DoubleChest> savedDoubleChests;
    public ArrayList<UUID> savedExplosiveFallingBlocks, savedTreeFallingBlocks;
    private LandsHook landsHook;
    private String serverVersion;

    private FileConfiguration langConfig;
    private File langFile;

    @Override
    public void onEnable() {
        setServerVersion(getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);
        savedStates = new ArrayList<>();
        savedExplosiveFallingBlocks = new ArrayList<>();
        savedTreeFallingBlocks = new ArrayList<>();
        setSavedDoubleChests(new ArrayList<>());
        pluginInstance = this;
        saveDefaultConfig();

        File file = new File(getDataFolder(), "/config.yml");
        if (file.exists()) {
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection cs = yaml.getConfigurationSection("");
            if (cs != null && cs.contains("messages"))
                file.renameTo(new File(getDataFolder(), "/old-config.yml"));
        }

        updateConfigs();

        log(Level.INFO, "Setting up required requisites...");
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        Objects.requireNonNull(getCommand("ptg")).setExecutor(new PhysicsToGoCommand(this));

        if (getConfig().getBoolean("general-options.update-checker")) {
            UpdateChecker updateChecker = new UpdateChecker(getPluginInstance(), 17181);
            if (updateChecker.checkForUpdates())
                log(Level.INFO, "There seems to be a new version on the PhysicsToGo page.");
            else log(Level.INFO, "Everything is up to date!");
        }
        log(Level.INFO, "Version " + getDescription().getVersion() + " has been successfully enabled!");
        new Metrics(pluginInstance);
    }

    @Override
    public void onDisable() {
        log(Level.INFO, "Please wait while a couple tasks are processed...");
        int restoreCounter = 0, removedFBCounter = 0;

        for (int i = -1; ++i < getServer().getWorlds().size(); ) {
            World world = getServer().getWorlds().get(i);
            for (int k = -1; ++k < world.getEntities().size(); ) {
                Entity entity = world.getEntities().get(k);
                if (entity.hasMetadata("P_T_G={'EXPLOSIVE_FALLING_BLOCK'}") || entity.hasMetadata("P_T_G={'TREE_FALLING_BLOCK'}")) {
                    entity.remove();
                    removedFBCounter += 1;
                }
            }
        }

        for (BlockState state : savedStates) {
            if (state.getType() == Material.AIR) continue;
            state.update(true, false);
            state.update();
            restoreCounter += 1;
        }

        for (DoubleChest doubleChest : getSavedDoubleChests()) doubleChest.restore();

        savedStates.clear();
        getSavedDoubleChests().clear();
        log(Level.INFO, removedFBCounter + " falling blocks were successfully removed!");
        log(Level.INFO, restoreCounter + " block states were successfully restored!");
    }

    private void updateConfigs() {
        long startTime = System.currentTimeMillis();
        int totalUpdates = 0;

        String[] configNames = {"config", "lang"};
        for (int i = -1; ++i < configNames.length; ) {
            String name = configNames[i];

            InputStream inputStream = getClass().getResourceAsStream("/" + name + ".yml");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
            int updateCount = updateKeys(yaml, name.equalsIgnoreCase("config") ? getConfig() : getLangConfig());

            try {
                inputStream.close();
                reader.close();
            } catch (IOException e) {
                log(Level.WARNING, e.getMessage());
            }

            if (updateCount > 0)
                switch (name) {
                    case "config":
                        saveConfig();
                        break;
                    case "lang":
                        saveLangConfig();
                        break;
                    default:
                        break;
                }

            if (updateCount > 0) {
                totalUpdates += updateCount;
                log(Level.INFO, updateCount + " things were fixed, updated, or removed in the '" + name
                        + ".yml' configuration file. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
            }
        }

        if (totalUpdates > 0) {
            reloadConfig();
            reloadLangConfig();
            log(Level.INFO, "A total of " + totalUpdates + " thing(s) were fixed, updated, or removed from all the " +
                    "configuration together. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
            log(Level.WARNING, "Please go checkout the configuration files as they are no longer the same as their default counterparts.");
        } else
            log(Level.INFO, "Everything inside the configuration seems to be up to date. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    private int updateKeys(FileConfiguration jarYaml, FileConfiguration currentYaml) {
        int updateCount = 0;
        ConfigurationSection currentConfigurationSection = currentYaml.getConfigurationSection(""),
                latestConfigurationSection = jarYaml.getConfigurationSection("");
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

    private void log(Level level, String message) {
        getServer().getLogger().log(level, "[" + getDescription().getName() + "] " + message);
    }

    // custom configurations
    private void saveLangConfig() {
        if (langConfig == null || langFile == null) return;
        try {
            getLangConfig().save(langFile);
        } catch (IOException e) {
            log(Level.WARNING, e.getMessage());
        }
    }

    public FileConfiguration getLangConfig() {
        if (langConfig == null) reloadLangConfig();
        return langConfig;
    }

    public void reloadLangConfig() {
        if (langFile == null) langFile = new File(getDataFolder(), "lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream path = this.getResource("lang.yml");
        Reader defConfigStream;
        if (path != null) {
            defConfigStream = new InputStreamReader(path, StandardCharsets.UTF_8);
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            langConfig.setDefaults(defConfig);

            try {
                path.close();
                defConfigStream.close();
            } catch (IOException e) {
                log(Level.WARNING, e.getMessage());
            }
        }
    }

    public WorldGuardPlugin getWorldGuard() {
        Plugin p = getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(p instanceof WorldGuardPlugin)) return null;
        return (WorldGuardPlugin) p;
    }

    public static PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    private void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public LandsHook getLandsHook() {
        return landsHook;
    }

    public void setLandsHook(LandsHook landsHook) {
        this.landsHook = landsHook;
    }

    private void setSavedDoubleChests(List<DoubleChest> savedDoubleChests) {
        this.savedDoubleChests = savedDoubleChests;
    }

    public List<DoubleChest> getSavedDoubleChests() {
        return savedDoubleChests;
    }

}
