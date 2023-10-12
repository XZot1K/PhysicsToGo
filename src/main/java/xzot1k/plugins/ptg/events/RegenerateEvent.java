/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.events;

import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xzot1k.plugins.ptg.PhysicsToGo;

public class RegenerateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private PhysicsToGo pluginInstance;
    private boolean cancelled;
    private BlockState blockState;

    public RegenerateEvent(PhysicsToGo pluginInstance, BlockState blockState) {
        setPluginInstance(pluginInstance);
        setBlockState(blockState);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    private void setBlockState(BlockState blockState) {
        this.blockState = blockState;
    }
}
