/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.events;

import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xzot1k.plugins.ptg.PhysicsToGo;
import xzot1k.plugins.ptg.core.enums.ActionType;

public class PhysicsActionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private PhysicsToGo pluginInstance;
    private boolean cancelled;

    private BlockState blockState;
    private ActionType actionType;

    public PhysicsActionEvent(PhysicsToGo pluginInstance, BlockState blockState, ActionType actionType) {
        setPluginInstance(pluginInstance);
        setBlockState(blockState);
        setActionType(actionType);
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

    public ActionType getActionType() {
        return actionType;
    }

    private void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}
