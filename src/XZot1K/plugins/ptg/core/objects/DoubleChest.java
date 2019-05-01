package XZot1K.plugins.ptg.core.objects;

import XZot1K.plugins.ptg.PhysicsToGo;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class DoubleChest
{
    private PhysicsToGo pluginInstance;
    private BlockFace direction;
    private Location leftSide, rightSide;
    private ItemStack[] itemStacks;

    public DoubleChest(PhysicsToGo pluginInstance, Location leftSide, Location rightSide, BlockFace direction, ItemStack[] itemStacks)
    {
        setPluginInstance(pluginInstance);
        setLeftSide(leftSide.clone());
        setRightSide(rightSide.clone());
        setItemStacks(itemStacks);
        setDirection(direction);
    }

    public void restore()
    {
        getLeftSide().getBlock().setType(getLeftSide().getBlock().getState().getType());
        getRightSide().getBlock().setType(getRightSide().getBlock().getState().getType());

        if (getLeftSide().getBlock().getState() instanceof Chest && getRightSide().getBlock().getState() instanceof Chest)
        {
            Chest leftChest = (Chest) getLeftSide().getBlock().getState(), rightChest = (Chest) getLeftSide().getBlock().getState();
            org.bukkit.block.data.type.Chest chestBlockState1 = (org.bukkit.block.data.type.Chest) leftChest.getBlockData();
            chestBlockState1.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
            chestBlockState1.setFacing(getDirection());
            getLeftSide().getBlock().setBlockData(chestBlockState1, true);

            org.bukkit.block.data.type.Chest chestBlockState2 = (org.bukkit.block.data.type.Chest) rightChest.getBlockData();
            chestBlockState2.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
            chestBlockState2.setFacing(getDirection());
            getRightSide().getBlock().setBlockData(chestBlockState2, true);

            InventoryHolder inventoryHolder = (InventoryHolder) getLeftSide().getBlock().getState();
            org.bukkit.block.DoubleChest doubleChest = (org.bukkit.block.DoubleChest) inventoryHolder.getInventory().getHolder();
            doubleChest.getInventory().setContents(getItemStacks());
            getPluginInstance().getSavedDoubleChests().remove(this);

            getPluginInstance().savedStates.remove(getLeftSide().getBlock().getState());
            getPluginInstance().savedStates.remove(getRightSide().getBlock().getState());
        }
    }

    private PhysicsToGo getPluginInstance()
    {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance)
    {
        this.pluginInstance = pluginInstance;
    }

    public Location getLeftSide()
    {
        return leftSide;
    }

    public void setLeftSide(Location leftSide)
    {
        this.leftSide = leftSide;
    }

    public Location getRightSide()
    {
        return rightSide;
    }

    public void setRightSide(Location rightSide)
    {
        this.rightSide = rightSide;
    }

    public ItemStack[] getItemStacks()
    {
        return itemStacks;
    }

    public void setItemStacks(ItemStack[] itemStacks)
    {
        this.itemStacks = itemStacks;
    }

    public BlockFace getDirection()
    {
        return direction;
    }

    public void setDirection(BlockFace direction)
    {
        this.direction = direction;
    }
}
