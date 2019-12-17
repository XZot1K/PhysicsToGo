package XZot1K.plugins.ptg;

import XZot1K.plugins.ptg.core.Listeners;
import XZot1K.plugins.ptg.core.PhysicsToGoCommand;
import XZot1K.plugins.ptg.core.checkers.Metrics;
import XZot1K.plugins.ptg.core.checkers.UpdateChecker;
import XZot1K.plugins.ptg.core.internals.LandsHook;
import XZot1K.plugins.ptg.core.objects.DoubleChest;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class PhysicsToGo extends JavaPlugin {

    private static PhysicsToGo pluginInstance;
    public List<BlockState> savedStates;
    private List<DoubleChest> savedDoubleChests;
    public ArrayList<UUID> savedExplosiveFallingBlocks, savedTreeFallingBlocks;
    private UpdateChecker updateChecker;
    private LandsHook landsHook;
    private String serverVersion;

    @Override
    public void onEnable() {
        setServerVersion(getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);
        savedStates = new ArrayList<>();
        savedExplosiveFallingBlocks = new ArrayList<>();
        savedTreeFallingBlocks = new ArrayList<>();
        setSavedDoubleChests(new ArrayList<>());
        pluginInstance = this;
        updateChecker = new UpdateChecker(getPluginInstance(), 17181);
        saveDefaultConfig();
        updateConfig();

        log(Level.INFO, "Setting up required requisites...");
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        Objects.requireNonNull(getCommand("ptg")).setExecutor(new PhysicsToGoCommand(this));

        if (getConfig().getBoolean("general-options.update-checker"))
            if (updateChecker.checkForUpdates())
                log(Level.INFO, "There seems to be a new version on the PhysicsToGo page.");
            else log(Level.INFO, "Everything is up to date!");
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

    private void updateConfig() {
        int updateCount = 0;

        saveResource("config_latest.yml", true);
        File latestConfigFile = new File(getDataFolder(), "config_latest.yml");

        FileConfiguration updatedYaml = YamlConfiguration.loadConfiguration(latestConfigFile);
        List<String> currentKeys = new ArrayList<>(Objects.requireNonNull(getConfig().getConfigurationSection("")).getKeys(true)),
                updatedKeys = new ArrayList<>(Objects.requireNonNull(updatedYaml.getConfigurationSection("")).getKeys(true));
        for (int i = -1; ++i < updatedKeys.size(); ) {
            String updatedKey = updatedKeys.get(i);
            if (!currentKeys.contains(updatedKey) && !updatedKey.contains(".items.") && !updatedKey.contains("custom-menus-section.")) {
                getConfig().set(updatedKey, updatedYaml.get(updatedKey));
                updateCount += 1;
                log(Level.INFO, "Updated the '" + updatedKey + "' key within the configuration since it wasn't found.");
            }
        }

        for (int i = -1; ++i < currentKeys.size(); ) {
            String currentKey = currentKeys.get(i);
            if (!updatedKeys.contains(currentKey)) {
                getConfig().set(currentKey, null);
                updateCount += 1;
                log(Level.INFO, "Removed the '" + currentKey + "' key within the configuration since it was invalid.");
            }
        }

        if (updateCount > 0) {
            saveConfig();
            log(Level.INFO, "The configuration has been updated using the " + latestConfigFile.getName() + " file.");
            log(Level.WARNING, "Please go check out the configuration and customize these newly generated options to your liking. " +
                    "Messages and similar values may not appear the same as they did in the default configuration " +
                    "(P.S. Configuration comments have more than likely been removed to ensure proper syntax).");
        } else log(Level.INFO, "Everything inside the configuration seems to be up to date.");
        latestConfigFile.delete();
    }

    private void log(Level level, String message) {
        getServer().getLogger().log(level, "[" + getDescription().getName() + "] " + message);
    }

    public WorldGuardPlugin getWorldGuard() {
        Plugin p = getServer().getPluginManager().getPlugin("WorldGuard");
        if (!(p instanceof WorldGuardPlugin)) return null;
        return (WorldGuardPlugin) p;
    }

    public static PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private String colorText(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
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
