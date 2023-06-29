/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import xzot1k.plugins.ptg.PhysicsToGo;
import xzot1k.plugins.ptg.core.enums.TreeType;
import xzot1k.plugins.ptg.core.objects.LocationClone;
import xzot1k.plugins.ptg.core.objects.SaplingData;

import java.util.*;

public class Manager {

    private PhysicsToGo pluginInstance;
    private Random random;

    private List<BlockState> savedBlockStates;
    private HashMap<LocationClone, ItemStack[]> savedContainerContents;
    private HashMap<LocationClone, Object> savedSignData;

    public Manager(PhysicsToGo pluginInstance) {
        setPluginInstance(pluginInstance);
        setRandom(new Random());
        setSavedBlockStates(new ArrayList<>());
        setSavedContainerContents(new HashMap<>());
        setSavedSignData(new HashMap<>());
    }

    /**
     * Colors the fed text using the Bukkit color class.
     *
     * @param text The text to translate color codes.
     * @return The translated text.
     */
    public String colorText(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Gets a random double between two values.
     *
     * @param min The minimum.
     * @param max The maximum.
     * @return The found value.
     */
    public double getRandomInRange(double min, double max) {
        return (min + (max - min) * getRandom().nextDouble());
    }

    /**
     * Gets a random int between two values.
     *
     * @param min The minimum.
     * @param max The maximum.
     * @return The found value.
     */
    public int getRandomInRange(int min, int max) {
        return getRandom().nextInt((max - min) + 1) + min;
    }

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    public boolean isNotNumeric(String string) {
        final char[] chars = string.toCharArray();
        for (int i = -1; ++i < string.length(); ) {
            final char c = chars[i];
            if (!Character.isDigit(c) && c != '.' && c != '-') return true;
        }

        return false;
    }

    /**
     * Determines if the server version is post 1.13 or not.
     *
     * @return If the server is post 1.13.
     */
    public boolean isBlockDataVersion() {return getPluginInstance().getServerVersion() >= 1_13;}

    /**
     * Determines if the server version is post 1.8 or not.
     *
     * @return If the server is post 1.8.
     */
    public boolean isOffHandVersion() {return getPluginInstance().getServerVersion() > 1_8;}


    /**
     * @param block The block to check
     * @return Whether the partnered chest has its inventory stored already.
     */
    public boolean isDoubleChestPartnerStored(Block block) {
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return false;

        final Chest chest = (Chest) block.getState();
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            Block relative = block.getRelative(face);
            if (relative.getType() != block.getType()) continue;
            if (chest.getInventory().equals(((Chest) relative.getState()).getInventory())) {
                return getSavedContainerContents().entrySet().parallelStream().anyMatch(entry -> entry.getKey().isIdentical(relative.getLocation()));
            }
        }

        return false;
    }


    /**
     * Determines if the passed block is a material is NOT a natural tree material.
     *
     * @param block The block to analyze.
     * @return If the block is NOT a natural tree material.
     */
    public boolean isNotTreeBlock(Block block) {
        return !block.getType().name().contains("LOG") && !block.getType().name().contains("LEAVES") && !block.getType().name().contains("VINE")
                && (!block.getType().name().contains("SHROOM") || !block.getType().name().contains("LIGHT"))
                && (!block.getType().name().contains("WARPED") || !block.getType().name().contains("WART"));
    }

    /**
     * Checks to see if the material is a configured material that should be avoided.
     *
     * @param material The material to check.
     * @return If it is a material to avoid.
     */
    public boolean isAvoidedMaterial(Material material) {
        List<String> materialNames = getPluginInstance().getConfig().getStringList("avoided-materials");
        if (materialNames.isEmpty()) return false;

        for (int i = -1; ++i < materialNames.size(); ) {
            String materialName = materialNames.get(i);
            if (materialName != null && material.name().contains(materialName.toUpperCase().replace(" ", "_").replace("-", "_")))
                return true;
        }
        return false;
    }

    /**
     * Checks to see if the material is whitelisted to be only effected (Block Placing).
     *
     * @param material The material to check.
     * @return If it is a whitelisted material.
     */
    public boolean isWhitelistedPlaceMaterial(Material material) {
        List<String> effectedMaterials = getPluginInstance().getConfig().getStringList("place-only-effected");
        if (effectedMaterials.isEmpty()) return true;

        for (int i = -1; ++i < effectedMaterials.size(); ) {
            String materialName = effectedMaterials.get(i);
            if (material != null && material.name().contains(materialName.toUpperCase().replace(" ", "_").replace("-", "_")))
                return true;
        }
        return false;
    }

    /**
     * Checks to see if the material is whitelisted to be only effected (Block Breaking).
     *
     * @param material The material to check.
     * @return If it is a whitelisted material.
     */
    public boolean isWhitelistedBreakMaterial(Material material) {
        List<String> effectedMaterials = getPluginInstance().getConfig().getStringList("break-only-effected");
        if (effectedMaterials.isEmpty()) return true;

        for (int i = -1; ++i < effectedMaterials.size(); ) {
            String materialName = effectedMaterials.get(i);
            if (material != null && material.name().contains(materialName.toUpperCase().replace(" ", "_").replace("-", "_")))
                return true;
        }
        return false;
    }

    /**
     * Checks to see if the entity can avoid damage and effects from explosives.
     *
     * @param entityType The entity type to check for.
     * @return Whether it can take the damage or not.
     */
    public boolean isBlockedExplosiveEntity(EntityType entityType) {
        List<String> blockedEntities = getPluginInstance().getConfig().getStringList("explosive-blocked-entities");
        if (blockedEntities.isEmpty()) return false;

        for (int i = -1; ++i < blockedEntities.size(); ) {
            String entityTypeName = blockedEntities.get(i);
            if (entityTypeName != null && entityType.name().contains(entityTypeName.toUpperCase().replace(" ", "_").replace("-", "_")))
                return true;
        }
        return false;
    }

    /**
     * Checks to see if the entity can regenerate blocks it exploded.
     *
     * @param entityType The entity type to check for.
     * @return Whether it can regenerate blocks or not.
     */
    public boolean isBlockedExplosiveRegenEntity(EntityType entityType) {
        List<String> blockedEntities = getPluginInstance().getConfig().getStringList("blocked-entity-regeneration");
        if (blockedEntities.isEmpty()) return false;

        for (int i = -1; ++i < blockedEntities.size(); ) {
            String entityTypeName = blockedEntities.get(i);
            if (entityTypeName != null && entityType.name().contains(entityTypeName.toUpperCase().replace(" ", "_").replace("-", "_")))
                return true;
        }
        return false;
    }

    /**
     * Checks to see if the passed material is a blocked regeneration material.
     *
     * @param material The material to check for.
     * @return Whether it is blocked.
     */
    public boolean isBlockedRegenMaterial(Material material) {
        List<String> effectedMaterials = getPluginInstance().getConfig().getStringList("blocked-material-regeneration");
        if (effectedMaterials.isEmpty()) return false;

        for (int i = -1; ++i < effectedMaterials.size(); ) {
            String materialName = effectedMaterials.get(i);
            if (materialName != null && material.name().contains(materialName.toUpperCase().replace(" ", "_").replace("-", "_")))
                return true;
        }
        return false;
    }

    /**
     * Checks to see if the world is blocked.
     *
     * @return Whether the world is blocked or not.
     */
    public boolean isBlockedWorld(World world) {
        if (world == null) return false;

        for (String worldName : getPluginInstance().getConfig().getStringList("world-blacklist"))
            if (worldName.equalsIgnoreCase(world.getName())) return true;

        return false;
    }

    /**
     * Attempts to play a cool looking natural block break effect with sound.
     *
     * @param block The block to use for data and location.
     */
    public void playNaturalBlockBreakEffect(Block block) {
        if (getPluginInstance().getManager().isBlockDataVersion()) {
            block.getWorld().spawnParticle(Particle.BLOCK_DUST, ((block.getLocation().getX() + 0.5) + getPluginInstance().getManager().getRandomInRange(-1, 1)),
                    ((block.getLocation().getY() + 0.5) + getPluginInstance().getManager().getRandomInRange(-1, 1)),
                    ((block.getLocation().getZ() + 0.5) + getPluginInstance().getManager().getRandomInRange(-1, 1)), 10, block.getBlockData());

            Sound sound = Sound.BLOCK_METAL_BREAK;
            if (block.getType().name().contains("LOG") || block.getType().name().contains("WOOD")
                    || block.getType().name().contains("BARREL") || block.getType().name().contains("CHEST")
                    || block.getType().name().contains("BENCH") || block.getType().name().contains("OAK")
                    || block.getType().name().contains("BIRCH") || block.getType().name().contains("DARK_OAK")
                    || block.getType().name().contains("ACACIA") || block.getType().name().contains("SPRUCE")
                    || block.getType().name().contains("JUNGLE"))
                sound = Sound.BLOCK_WOOD_BREAK;
            else if (block.getType().name().contains("GLASS"))
                sound = Sound.BLOCK_GLASS_BREAK;
            else if (block.getType().name().contains("STONE") || block.getType().name().contains("FURNACE"))
                sound = Sound.BLOCK_STONE_BREAK;
            else if (block.getType().name().contains("WET") && block.getType().name().contains("GRASS"))
                sound = Sound.BLOCK_WET_GRASS_BREAK;
            else if (block.getType().name().contains("GRASS"))
                sound = Sound.BLOCK_GRASS_BREAK;
            else if (block.getType().name().contains("WOOL"))
                sound = Sound.BLOCK_WOOL_BREAK;
            else if (block.getType().name().contains("BAMBOO"))
                sound = Sound.BLOCK_BAMBOO_BREAK;
            else if (block.getType().name().contains("CROP") || (block.getType().name().contains("SUGAR") && block.getType().name().contains("CANE"))
                    || block.getType().name().contains("CARROT") || block.getType().name().contains("POTATO") || block.getType().name().contains("BEET")
                    || block.getType().name().contains("STEM") || block.getType().name().contains("SAPLING") || block.getType().name().contains("LEAVES"))

                sound = Sound.BLOCK_CROP_BREAK;
            else if (block.getType().name().contains("BERRY") && block.getType().name().contains("BUSH"))
                sound = Sound.BLOCK_SWEET_BERRY_BUSH_BREAK;
            else if (block.getType().name().contains("LADDER"))
                sound = Sound.BLOCK_LADDER_BREAK;
            else if (block.getType().name().contains("CORAL"))
                sound = Sound.BLOCK_CORAL_BLOCK_BREAK;
            else if (block.getType().name().contains("GRAVEL"))
                sound = Sound.BLOCK_GRAVEL_BREAK;
            else if (block.getType().name().contains("SAND"))
                sound = Sound.BLOCK_SAND_BREAK;
            else if (block.getType().name().contains("HONEY"))
                sound = Sound.BLOCK_HONEY_BLOCK_BREAK;
            else if (block.getType().name().contains("ANVIL"))
                sound = Sound.BLOCK_ANVIL_BREAK;
            else if (block.getType().name().contains("BEACON"))
                sound = Sound.BLOCK_BEACON_DEACTIVATE;
            else if (block.getType().name().contains("CONDUIT"))
                sound = Sound.BLOCK_CONDUIT_DEACTIVATE;
            else if (block.getType().name().contains("LANTERN"))
                sound = Sound.BLOCK_LANTERN_BREAK;
            else if (block.getType().name().contains("SNOW"))
                sound = Sound.BLOCK_SNOW_BREAK;
            else if (block.getType().name().contains("NETHER") && block.getType().name().contains("WART"))
                sound = Sound.BLOCK_NETHER_WART_BREAK;
            else if (block.getType().name().contains("SCAFFOLD"))
                sound = Sound.BLOCK_SCAFFOLDING_BREAK;

            block.getWorld().playSound(block.getLocation(), sound, 1, 1);
        } else block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType().getId());
    }

    /**
     * Attempts to play a cool looking natural block place effect with sound.
     *
     * @param block The block to use for data and location.
     */
    public void playNaturalBlockPlaceEffect(Block block) {
        if (getPluginInstance().getManager().isBlockDataVersion()) {
            block.getWorld().spawnParticle(Particle.BLOCK_DUST, ((block.getLocation().getX() + 0.5) + getPluginInstance().getManager().getRandomInRange(-1, 1)),
                    ((block.getLocation().getY() + 0.5) + getPluginInstance().getManager().getRandomInRange(-1, 1)),
                    ((block.getLocation().getZ() + 0.5) + getPluginInstance().getManager().getRandomInRange(-1, 1)), 10, block.getBlockData());

            Sound sound = Sound.BLOCK_METAL_PLACE;
            if (block.getType().name().contains("LOG") || block.getType().name().contains("WOOD")
                    || block.getType().name().contains("BARREL") || block.getType().name().contains("CHEST")
                    || block.getType().name().contains("BENCH") || block.getType().name().contains("OAK")
                    || block.getType().name().contains("BIRCH") || block.getType().name().contains("DARK_OAK")
                    || block.getType().name().contains("ACACIA") || block.getType().name().contains("SPRUCE")
                    || block.getType().name().contains("JUNGLE"))
                sound = Sound.BLOCK_WOOD_PLACE;
            else if (block.getType().name().contains("GLASS"))
                sound = Sound.BLOCK_GLASS_PLACE;
            else if (block.getType().name().contains("STONE") || block.getType().name().contains("FURNACE"))
                sound = Sound.BLOCK_STONE_PLACE;
            else if (block.getType().name().contains("WET") && block.getType().name().contains("GRASS"))
                sound = Sound.BLOCK_WET_GRASS_PLACE;
            else if (block.getType().name().contains("GRASS") || block.getType().name().contains("DIRT")
                    || block.getType().name().contains("LEAVES"))
                sound = Sound.BLOCK_GRASS_PLACE;
            else if (block.getType().name().contains("WOOL"))
                sound = Sound.BLOCK_WOOL_PLACE;
            else if (block.getType().name().contains("BAMBOO"))
                sound = Sound.BLOCK_BAMBOO_PLACE;
            else if (block.getType().name().contains("CROP") || (block.getType().name().contains("SUGAR") && block.getType().name().contains("CANE"))
                    || block.getType().name().contains("CARROT") || block.getType().name().contains("POTATO") || block.getType().name().contains("BEET")
                    || block.getType().name().contains("STEM") || block.getType().name().contains("SAPLING"))
                sound = Sound.ITEM_CROP_PLANT;
            else if (block.getType().name().contains("BERRY") && block.getType().name().contains("BUSH"))
                sound = Sound.BLOCK_SWEET_BERRY_BUSH_PLACE;
            else if (block.getType().name().contains("LADDER"))
                sound = Sound.BLOCK_LADDER_PLACE;
            else if (block.getType().name().contains("CORAL"))
                sound = Sound.BLOCK_CORAL_BLOCK_PLACE;
            else if (block.getType().name().contains("GRAVEL"))
                sound = Sound.BLOCK_GRAVEL_PLACE;
            else if (block.getType().name().contains("SAND"))
                sound = Sound.BLOCK_SAND_PLACE;
            else if (block.getType().name().contains("HONEY"))
                sound = Sound.BLOCK_HONEY_BLOCK_PLACE;
            else if (block.getType().name().contains("ANVIL"))
                sound = Sound.BLOCK_ANVIL_PLACE;
            else if (block.getType().name().contains("BEACON"))
                sound = Sound.BLOCK_BEACON_DEACTIVATE;
            else if (block.getType().name().contains("CONDUIT"))
                sound = Sound.BLOCK_CONDUIT_DEACTIVATE;
            else if (block.getType().name().contains("LANTERN"))
                sound = Sound.BLOCK_LANTERN_PLACE;
            else if (block.getType().name().contains("SNOW"))
                sound = Sound.BLOCK_SNOW_PLACE;
            else if (block.getType().name().contains("NETHER") && block.getType().name().contains("WART"))
                sound = Sound.ITEM_NETHER_WART_PLANT;
            else if (block.getType().name().contains("SCAFFOLD"))
                sound = Sound.BLOCK_SCAFFOLDING_PLACE;

            block.getWorld().playSound(block.getLocation(), sound, 1, 1);
        } else block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType().getId());
    }

    /**
     * Creates a cool falling block physics animation.
     *
     * @param block                   The block to copy and use the location of.
     * @param blockState              The original state of the block.
     * @param randomizeOffset         Whether it will be flung randomly using velocity.
     * @param hasNoGravityTemporarily Whether it will stick in the air for a second or not.
     */
    public void createFallingBlock(Block block, BlockState blockState, boolean randomizeOffset, boolean hasNoGravityTemporarily) {
        FallingBlock fallingBlock;
        if (!getPluginInstance().getManager().isBlockDataVersion())
            fallingBlock = block.getWorld().spawnFallingBlock(block.getLocation().clone().add(0.5, 0, 0.5), block.getType(), block.getData());
        else
            fallingBlock = block.getWorld().spawnFallingBlock(block.getLocation().clone().add(0.5, 0, 0.5), blockState.getBlockData());
        fallingBlock.setDropItem(false);

        if (randomizeOffset)
            fallingBlock.setVelocity(new Vector(getPluginInstance().getManager().getRandomInRange(-0.65, 0.65),
                    getPluginInstance().getManager().getRandomInRange(0.05, 0.7), getPluginInstance().getManager().getRandomInRange(-0.65, 0.65)));
        fallingBlock.setCustomNameVisible(false);
        fallingBlock.setCustomName("PTG_FALLING_BLOCK");

        if (hasNoGravityTemporarily) {
            fallingBlock.setGravity(false);
            fallingBlock.setVelocity(new Vector(0, 0, 0));
            getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(), () -> fallingBlock.setGravity(true), getPluginInstance().getAdvancedConfig().getInt(
                    "gravity-effect-delay"));
        }
    }

    /**
     * Gets the sapling data that will be used for tree replanting.
     *
     * @param material The block material (Log Type).
     * @return A new instance of SaplingData containing appropriate sapling values based on material.
     */
    public SaplingData getReplantSapling(Material material) {
        TreeType treeType = TreeType.OAK;
        if (material.name().contains("DARK_OAK")) treeType = TreeType.DARK_OAK;
        else if (material.name().contains("ACACIA")) treeType = TreeType.ACACIA;
        else if (material.name().contains("BIRCH")) treeType = TreeType.BIRCH;
        else if (material.name().contains("JUNGLE")) treeType = TreeType.JUNGLE;
        return new SaplingData(treeType);
    }

    // block sorting algorithm methods

    /**
     * Sorts a list of blocks from lowest to highest in terms of the Y-axis with priority based on configuration values.
     *
     * @param blockList The block list to sort.
     */
    public void sortFromLowestToHighest(List<Block> blockList) {
        sort(blockList, 0, (blockList.size() - 1));
    }

    private void sort(List<Block> blockList, int low, int high) {
        if (low < high + 1) {
            int p = partition(blockList, low, high);
            sort(blockList, low, p - 1);
            sort(blockList, p + 1, high);
        }
    }

    private void swap(List<Block> blockList, int index1, int index2) {
        Block temp = blockList.get(index1);
        blockList.set(index1, blockList.get(index2));
        blockList.set(index2, temp);
    }

    private int getPivot(int low, int high) {
        return getRandom().nextInt((high - low) + 1) + low;
    }

    private int partition(List<Block> blockList, int low, int high) {
        swap(blockList, low, getPivot(low, high));
        int border = low + 1;

        for (int i = (border - 1); ++i <= high; ) {
            Block currentBlock = blockList.get(i), nextBlock = blockList.get(low);
            int currentPriority = getMaterialSortPriority(currentBlock.getType()), nextPriority = getMaterialSortPriority(nextBlock.getType());
            if (currentBlock.getY() < nextBlock.getY() && currentPriority >= nextPriority) swap(blockList, i, border++);
        }

        swap(blockList, low, border - 1);
        return border - 1;
    }

    /**
     * Gets the sorting priority of a material based on the advanced configuration.
     *
     * @param material The material to get the priority of.
     * @return The priority ranging from 0 to 10 (10 being HIGH priority).
     */
    public int getMaterialSortPriority(Material material) {
        final ConfigurationSection cs = getPluginInstance().getAdvancedConfig().getConfigurationSection("block-sorting-priorities");
        if (cs == null) return 10;

        Collection<String> materialNames = cs.getKeys(false);
        if (materialNames.isEmpty()) return 10;

        for (String materialName : materialNames)
            if (material.name().contains(materialName.toUpperCase().replace(" ", "_").replace("-", "_")))
                return getPluginInstance().getAdvancedConfig().getInt("block-sorting-priorities." + materialName);

        return 10;
    }

    // getters & setters
    private PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    /**
     * Gets the Random class instance.
     *
     * @return the random class.
     */
    public Random getRandom() {
        return this.random;
    }

    private void setRandom(Random random) {
        this.random = random;
    }

    /**
     * This gets a list of all block states currently awaiting action. This is stored for the onDisable() method.
     *
     * @return The list of stored block states.
     */
    public List<BlockState> getSavedBlockStates() {
        return savedBlockStates;
    }

    private void setSavedBlockStates(List<BlockState> savedBlockStates) {
        this.savedBlockStates = savedBlockStates;
    }

    /**
     * Gets the container contents of stored blocks.
     *
     * @return The container set.
     */
    public HashMap<LocationClone, ItemStack[]> getSavedContainerContents() {
        return savedContainerContents;
    }

    private void setSavedContainerContents(HashMap<LocationClone, ItemStack[]> savedContainerContents) {
        this.savedContainerContents = savedContainerContents;
    }

    /**
     * Gets all saved sign data.
     *
     * @return The sign data map.
     */
    public HashMap<LocationClone, Object> getSavedSignData() {
        return savedSignData;
    }

    private void setSavedSignData(HashMap<LocationClone, Object> savedSignData) {
        this.savedSignData = savedSignData;
    }
}