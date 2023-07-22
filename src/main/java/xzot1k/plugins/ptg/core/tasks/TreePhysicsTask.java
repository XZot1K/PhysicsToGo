/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.tasks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xzot1k.plugins.ptg.PhysicsToGo;
import xzot1k.plugins.ptg.core.objects.SaplingData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class TreePhysicsTask implements Runnable {

    private PhysicsToGo pluginInstance;

    private BlockState initialBlockState;
    private Material logMaterial;

    private BlockFace[] blockFaces;
    private int currentHeight, treeRegenDelay, taskId;
    private boolean animation, gravity, drops, regeneration;
    private Player player;
    private ItemStack itemStack;

    public TreePhysicsTask(PhysicsToGo pluginInstance, Player player, BlockState initialBlockState, boolean drops, boolean regeneration) {
        setPluginInstance(pluginInstance);
        setInitialBlockState(initialBlockState);
        setLogMaterial(getInitialBlockState().getType());
        setTreeRegenDelay(getPluginInstance().getConfig().getInt("tree-regeneration-delay"));
        setAnimation(getPluginInstance().getConfig().getBoolean("tree-animation"));
        setGravity(getPluginInstance().getConfig().getBoolean("tree-gravity-effect"));
        setBlockFaces(new BlockFace[]{BlockFace.NORTH, BlockFace.NORTH_WEST, BlockFace.NORTH_EAST, BlockFace.WEST,
                BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST, BlockFace.SOUTH});
        setCanDrop(drops);
        setCanRegenerate(regeneration);
        setPlayer(player);
        setCurrentHeight(0);
        setItemStack(getPluginInstance().getManager().isOffHandVersion() ? player.getInventory().getItemInMainHand() : player.getItemInHand());
    }

    @Override
    public void run() {
        Block currentTrunkBlock = getInitialBlockState().getBlock().getRelative(0, getCurrentHeight(), 0);
        if (getPluginInstance().getManager().isNotTreeBlock(currentTrunkBlock) || getPluginInstance().getManager().getSavedBlockStates().contains(currentTrunkBlock.getState())
                || getPluginInstance().doesNotPassHooksCheck(currentTrunkBlock.getLocation())) {
            takeActionOnBlock(currentTrunkBlock, getTreeRegenDelay());
            workAdjacents(currentTrunkBlock, getTreeRegenDelay());
            plantSapling();

            getPluginInstance().getServer().getScheduler().cancelTask(getTaskId());
            return;
        }

        takeActionOnBlock(currentTrunkBlock, getTreeRegenDelay());
        workAdjacents(currentTrunkBlock, getTreeRegenDelay());

        setCurrentHeight(getCurrentHeight() + 1);
    }

    private void plantSapling() {
        if (!canRegenerate() && getPluginInstance().getConfig().getBoolean("tree-replant")) {

            if (!getInitialBlockState().getBlock().getType().name().contains("AIR")
                    || !(getInitialBlockState().getBlock().getRelative(BlockFace.DOWN).getType().name().contains("DIRT")
                    || getInitialBlockState().getBlock().getRelative(BlockFace.DOWN).getType().name().contains("GRASS")
                    || getInitialBlockState().getBlock().getRelative(BlockFace.DOWN).getType().name().contains("NYLIUM")))
                return;

            SaplingData saplingData = getPluginInstance().getManager().getReplantSapling(getLogMaterial());
            if (saplingData != null) {
                getInitialBlockState().getBlock().setType(saplingData.getMaterial());
                if (saplingData.getDataValue() != -1) {
                    try {
                        Method dataMethod = getInitialBlockState().getBlock().getClass().getMethod("setData", Byte.class);
                        dataMethod.setAccessible(true);
                        dataMethod.invoke(getInitialBlockState().getBlock().getClass(), saplingData.getDataValue());
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        getPluginInstance().log(Level.WARNING, "Unable to set block data due to the 'setData' "
                                + "method not being found (If needed, send the developer the error above).");
                    }

                    getPluginInstance().getManager().playNaturalBlockPlaceEffect(getInitialBlockState().getBlock());
                }
            }
        }
    }

    private void workAdjacents(Block centerBlock, int counter) {
        for (BlockFace blockFace : shuffleBlockFaceList()) {
            Block adjacentBlock = centerBlock.getRelative(blockFace);
            if (getPluginInstance().getManager().isNotTreeBlock(adjacentBlock) || getPluginInstance().getManager().getSavedBlockStates().contains(adjacentBlock.getState())
                    || getPluginInstance().doesNotPassHooksCheck(adjacentBlock.getLocation()))
                continue;

            if (adjacentBlock.getType().name().contains("LOG")) {
                TreePhysicsTask treePhysicsTask = new TreePhysicsTask(getPluginInstance(), getPlayer(), adjacentBlock.getState(), canDrop(), canRegenerate());
                treePhysicsTask.setTaskId(getPluginInstance().getServer().getScheduler().runTaskTimer(getPluginInstance(), treePhysicsTask, 0,
                        getPluginInstance().getConfig().getInt("tree-regeneration-speed")).getTaskId());
                return;
            }

            takeActionOnBlock(adjacentBlock, getTreeRegenDelay() + (adjacentBlock.getType().name().contains("LOG") ? 0 : getPluginInstance().getManager().getRandomInRange(20,
                    30)));
            workAdjacents(adjacentBlock, counter + 1); // nested adjacent checks.
        }
    }

    private BlockFace[] shuffleBlockFaceList() {
        for (int i = -1; ++i < getBlockFaces().length; ) {
            int randomIndexToSwap = getPluginInstance().getManager().getRandom().nextInt(getBlockFaces().length);
            BlockFace tempFace = getBlockFaces()[randomIndexToSwap];
            getBlockFaces()[randomIndexToSwap] = getBlockFaces()[i];
            getBlockFaces()[i] = tempFace;
        }

        return getBlockFaces();
    }

    private void takeActionOnBlock(Block block, int delay) {
        if (getPluginInstance().getCoreProtectHook() != null && getPluginInstance().getConfig().getBoolean("core-protect"))
            getPluginInstance().getCoreProtectHook().logLocation(block.getLocation()); // log to core protect.
        final BlockState blockState = block.getState();
        if (canRegenerate()) getPluginInstance().getManager().getSavedBlockStates().add(blockState);

        if (!canDrop()) {
            getPluginInstance().getManager().playNaturalBlockBreakEffect(block);
            block.setType(Material.AIR);
        } else block.breakNaturally();

        if (useAnimation())
            getPluginInstance().getManager().createFallingBlock(block, blockState, false, useGravity());

        if (canRegenerate())
            getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(), new BlockRegenerationTask(getPluginInstance(), block, blockState, false), delay);

    }

    // getters & setters
    private PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public boolean useAnimation() {
        return animation;
    }

    private void setAnimation(boolean animation) {
        this.animation = animation;
    }

    public boolean useGravity() {
        return gravity;
    }

    private void setGravity(boolean gravity) {
        this.gravity = gravity;
    }

    public boolean canDrop() {
        return drops;
    }

    private void setCanDrop(boolean drops) {
        this.drops = drops;
    }

    public boolean canRegenerate() {
        return regeneration;
    }

    private void setCanRegenerate(boolean regeneration) {
        this.regeneration = regeneration;
    }

    public Material getLogMaterial() {
        return logMaterial;
    }

    private void setLogMaterial(Material logMaterial) {
        this.logMaterial = logMaterial;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public BlockState getInitialBlockState() {
        return initialBlockState;
    }

    public void setInitialBlockState(BlockState initialBlockState) {
        this.initialBlockState = initialBlockState;
    }

    public int getCurrentHeight() {
        return currentHeight;
    }

    private void setCurrentHeight(int currentHeight) {
        this.currentHeight = currentHeight;
    }

    public int getTreeRegenDelay() {
        return treeRegenDelay;
    }

    private void setTreeRegenDelay(int treeRegenDelay) {
        this.treeRegenDelay = treeRegenDelay;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public BlockFace[] getBlockFaces() {
        return blockFaces;
    }

    private void setBlockFaces(BlockFace[] blockFaces) {
        this.blockFaces = blockFaces;
    }
}
