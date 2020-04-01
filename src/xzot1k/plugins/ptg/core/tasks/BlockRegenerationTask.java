/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.tasks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import xzot1k.plugins.ptg.PhysicsToGo;
import xzot1k.plugins.ptg.core.objects.LocationClone;
import xzot1k.plugins.ptg.events.RegenerateEvent;

import java.util.ArrayList;
import java.util.List;

public class BlockRegenerationTask implements Runnable {

    private PhysicsToGo pluginInstance;
    private Block block;
    private BlockState blockState;
    private boolean isPlacement;

    public BlockRegenerationTask(PhysicsToGo pluginInstance, Block block, BlockState blockState, boolean isPlacement) {
        setPluginInstance(pluginInstance);
        setBlock(block);
        setBlockState(blockState);
        setPlacement(isPlacement);
    }

    @Override
    public void run() {
        if (getBlockState() == null) return;
        getPluginInstance().getManager().getSavedBlockStates().remove(getBlockState()); // clears the saved state.

        if (getBlock() == null) return;
        if (!getPluginInstance().getConfig().getBoolean("state-override") && !isPlacement())
            if (getBlock().getType() != Material.AIR && !getBlock().getType().name().contains("WATER") && !getBlock().getType().name().contains("LAVA"))
                return;

        RegenerateEvent regenerateEvent = new RegenerateEvent(getPluginInstance(), getBlockState());
        getPluginInstance().getServer().getPluginManager().callEvent(regenerateEvent);
        if (regenerateEvent.isCancelled()) return;

        getBlockState().update(true, false);
        getPluginInstance().getManager().playNaturalBlockPlaceEffect(getBlock());

        if (getBlockState() instanceof InventoryHolder) {
            InventoryHolder ih = (InventoryHolder) getBlockState();

            List<LocationClone> locationClones = new ArrayList<>(getPluginInstance().getManager().getSavedContainerContents().keySet());
            for (int i = -1; ++i < locationClones.size(); ) {
                LocationClone locationClone = locationClones.get(i);
                if (locationClone == null || !locationClone.isIdentical(getBlock().getLocation())) continue;

                ItemStack[] items = getPluginInstance().getManager().getSavedContainerContents().get(locationClone);
                if (items == null || items.length <= 0) continue;

                ih.getInventory().setContents(items);
                getPluginInstance().getManager().getSavedContainerContents().remove(locationClone);
            }

        } else if (getBlockState() instanceof Sign) {
            Sign sign = (Sign) getBlockState();
            List<LocationClone> locationClones = new ArrayList<>(getPluginInstance().getManager().getSavedSignData().keySet());
            for (int i = -1; ++i < locationClones.size(); ) {
                LocationClone locationClone = locationClones.get(i);
                if (locationClone == null || !locationClone.isIdentical(getBlock().getLocation())) continue;

                String[] lines = getPluginInstance().getManager().getSavedSignData().get(locationClone);
                if (lines == null || lines.length <= 0) continue;

                for (int j = -1; ++j < lines.length; ) sign.setLine(j, lines[j]);
                sign.update(true, false);

                getPluginInstance().getManager().getSavedSignData().remove(locationClone);
            }
        }
    }

    public Block getBlock() {
        return block;
    }

    private void setBlock(Block block) {
        this.block = block;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    private void setBlockState(BlockState blockState) {
        this.blockState = blockState;
    }

    private PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public boolean isPlacement() {
        return isPlacement;
    }

    private void setPlacement(boolean placement) {
        isPlacement = placement;
    }
}
