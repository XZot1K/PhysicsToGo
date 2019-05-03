package XZot1K.plugins.ptg.core;

import XZot1K.plugins.ptg.PhysicsToGo;
import XZot1K.plugins.ptg.api.events.HookCallEvent;
import XZot1K.plugins.ptg.core.internals.LandsHook;
import XZot1K.plugins.ptg.core.internals.WG_6;
import XZot1K.plugins.ptg.core.internals.WG_7;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.massivecore.ps.PS;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.SimpleChunkLocation;
import us.forseth11.feudal.core.Feudal;
import us.forseth11.feudal.kingdoms.Kingdom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Listeners implements Listener
{

    private PhysicsToGo plugin;
    private Random random;

    private ArrayList<Location> blockLocationMemory, placedLocationMemory;
    private HashMap<Location, ItemStack[]> containers;
    private HashMap<Location, String[]> signs;
    private List<Location> restoreLocations = new ArrayList<>();


    public Listeners(PhysicsToGo plugin)
    {
        this.plugin = plugin;
        random = new Random();
        setPlacedLocationMemory(new ArrayList<>());
        blockLocationMemory = new ArrayList<>();
        containers = new HashMap<>();
        signs = new HashMap<>();
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlace(BlockPlaceEvent e)
    {
        if (e.getPlayer().hasPermission("ptg.bypass.place") || !plugin.getConfig().getBoolean("block-place-options.block-place-event")
                || isInList("block-place-options.blacklisted-worlds", e.getBlock().getWorld().getName())
                || isInMaterialList("block-place-options.effected-material-blacklist", e.getBlock())
                || !passedHooks(e.getBlock().getLocation()))
            return;
        if (plugin.getConfig().getBoolean("block-place-options.block-place-cancel"))
        {
            e.setCancelled(true);
            return;
        }

        int delay = plugin.getConfig().getInt("block-place-options.block-reversion-options.delay");
        boolean blockReversion = plugin.getConfig().getBoolean("block-place-options.block-reversion"),
                reversionAboveHeight = plugin.getConfig().getBoolean("block-place-options.reversion-above-height");

        int heightLimit = plugin.getConfig().getInt("block-place-options.reversion-height");
        if (blockReversion && (reversionAboveHeight ? (heightLimit <= -1 || e.getBlock().getY() >= heightLimit) : (heightLimit <= -1 || e.getBlock().getY() <= heightLimit)))
        {
            Material previousMaterial = e.getBlockReplacedState().getType();
            byte previousData = e.getBlockReplacedState().getRawData();
            getPlacedLocationMemory().add(e.getBlock().getLocation());
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                Material placedMaterial = e.getBlock().getType();
                e.getBlock().setType(previousMaterial);
                if (!plugin.getServerVersion().startsWith("v1_13") && !plugin.getServerVersion().startsWith("v1_14"))
                {
                    try
                    {
                        Method closeMethod = e.getBlock().getClass().getMethod("setData", Short.class);
                        if (closeMethod != null)
                            closeMethod.invoke(e.getBlock().getClass(), (short) previousData);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
                }

                if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                        || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                    e.getBlock().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND, e.getBlock().getType() == Material.AIR ? placedMaterial.getId() : e.getBlock().getType().getId());
            }, delay);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBreak(BlockBreakEvent e)
    {
        if (plugin.getConfig().getBoolean("tree-physic-options.tree-physics"))
        {
            if (isInMaterialList("tree-physic-options.effected-break-materials", e.getBlock()))
            {
                boolean blockRegeneration = plugin.getConfig().getBoolean("tree-physic-options.tree-regeneration.regeneration");
                int radius = plugin.getConfig().getInt("tree-physic-options.tree-physics-radius"), delay = plugin.getConfig().getInt("tree-physic-options.tree-regeneration.delay"),
                        speed = plugin.getConfig().getInt("tree-physic-options.tree-regeneration.speed");
                for (int i = -1; ++i < (e.getBlock().getWorld().getMaxHeight() - e.getBlock().getY()); )
                {
                    for (int x = -radius; ++x < radius; )
                        for (int z = -radius; ++z < radius; )
                        {
                            Block block = e.getBlock().getRelative(x, i, z);
                            if (isInMaterialList("tree-physic-options.effected-physic-materials", block))
                            {
                                BlockState blockState = block.getState();
                                if (blockRegeneration) plugin.savedStates.add(blockState);

                                FallingBlock fallingBlock = e.getBlock().getWorld().spawnFallingBlock(block.getLocation().clone().add(0.5, 0, 0.5), block.getType(), block.getData());
                                fallingBlock.setMetadata("P_T_G={'TREE_FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                                fallingBlock.setDropItem(false);
                                plugin.savedTreeFallingBlocks.add(fallingBlock.getUniqueId());

                                if (blockRegeneration)
                                {
                                    new BukkitRunnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            blockState.update(true, false);
                                            if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                                                    || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                                                block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType().getId());
                                        }
                                    }.runTaskLater(plugin, delay);
                                    delay += speed;
                                }

                                block.setType(Material.AIR);
                            }
                        }
                }
            }
        }

        if (e.getPlayer().hasPermission("ptg.bypass.break") || !plugin.getConfig().getBoolean("block-break-options.block-break-event")
                || isInList("block-break-options.blacklisted-worlds", e.getBlock().getWorld().getName())
                || !passedHooks(e.getBlock().getLocation())) return;
        if (isInMaterialList("block-break-options.effected-material-blacklist", e.getBlock())) return;

        int delay = plugin.getConfig().getInt("block-break-options.block-regeneration-options.delay");
        boolean dropItems = plugin.getConfig().getBoolean("block-break-options.block-drops"),
                restorationMemory = plugin.getConfig().getBoolean("block-break-options.block-restoration-memory"),
                containerDrops = plugin.getConfig().getBoolean("block-break-options.container-drops"),
                blockRegeneration = plugin.getConfig().getBoolean("block-break-options.block-regeneration");

        BlockState blockState = e.getBlock().getState();
        if (blockRegeneration)
            plugin.savedStates.add(blockState);

        if (blockRegeneration && restorationMemory)
            if (blockState instanceof InventoryHolder)
            {
                InventoryHolder ih = (InventoryHolder) blockState;
                containers.put(e.getBlock().getLocation(), ih.getInventory().getContents().clone());
                if (!containerDrops) ih.getInventory().clear();
            } else if (blockState instanceof Sign)
            {
                Sign sign = (Sign) blockState;
                signs.put(e.getBlock().getLocation(), sign.getLines());
            }

        if (!dropItems || getPlacedLocationMemory().contains(e.getBlock().getLocation()))
        {
            if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                    || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                Objects.requireNonNull(e.getBlock().getLocation().getWorld()).playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND, 1);
            e.getBlock().setType(Material.AIR);
            if (getPlacedLocationMemory().contains(e.getBlock().getLocation()))
            {
                getPlacedLocationMemory().remove(e.getBlock().getLocation());
                return;
            }
        }

        boolean regenerationAboveHeight = plugin.getConfig().getBoolean("block-break-options.regeneration-above-height");
        int heightLimit = plugin.getConfig().getInt("block-break-options.regeneration-height");
        if (blockRegeneration && (regenerationAboveHeight ? (heightLimit <= -1 || e.getBlock().getY() >= heightLimit) : (heightLimit <= -1 || e.getBlock().getY() <= heightLimit)))
        {
            if (!blockLocationMemory.contains(e.getBlock().getLocation()))
                blockLocationMemory.add(e.getBlock().getLocation());
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                blockState.update(true, false);
                if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                        || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                    e.getBlock().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());
                Block relative1 = e.getBlock().getRelative(BlockFace.DOWN),
                        relative2 = e.getBlock().getRelative(BlockFace.UP);
                relative1.getState().update(true, false);
                relative2.getState().update(true, false);

                if (restorationMemory)
                    if (blockState instanceof InventoryHolder)
                    {
                        InventoryHolder ih = (InventoryHolder) blockState;
                        if (!containers.isEmpty() && containers.containsKey(e.getBlock().getLocation()))
                        {
                            ih.getInventory().setContents(containers.get(e.getBlock().getLocation()));
                            containers.remove(e.getBlock().getLocation());
                        }
                    } else if (blockState instanceof Sign)
                    {
                        Sign sign = (Sign) blockState;
                        if (!signs.isEmpty() && signs.containsKey(e.getBlock().getLocation()))
                        {
                            int j = 0;
                            for (String line : signs.get(e.getBlock().getLocation()))
                            {
                                sign.setLine(j, line);
                                j += 1;
                            }

                            sign.update();
                            signs.remove(e.getBlock().getLocation());
                        }
                    }

                blockLocationMemory.remove(e.getBlock().getLocation());
                plugin.savedStates.remove(blockState);
            }, delay);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e)
    {
        if (isInList("explosive-options.blacklisted-worlds", Objects.requireNonNull(e.getBlock().getWorld()).getName()))
            return;
        if (!plugin.getConfig().getBoolean("explosive-options.block-damage"))
            e.blockList().clear();
        else
        {
            boolean restorationMemory = plugin.getConfig().getBoolean("explosive-options.block-restoration-memory");
            int delay = plugin.getConfig().getInt("explosive-options.block-regeneration-options.delay"),
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed"),
                    tntFuse = plugin.getConfig().getInt("explosive-options.tnt-fuse");
            final int[] regenerationCounter = {0};

            List<Block> blocks = new ArrayList<>(e.blockList());
            sortFromLowestToHighest(blocks);

            boolean blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                    containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                    blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");

            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                if (isInMaterialList("explosive-options.effected-material-blacklist", b))
                {
                    e.blockList().remove(b);
                    blocks.remove(b);
                    continue;
                }

                if (isInMaterialList("explosive-options.help-needed-material", b.getRelative(BlockFace.UP)))
                {
                    state.update(true, false);
                    b.getRelative(BlockFace.UP).getState().update(true, false);
                    e.blockList().remove(b.getRelative(BlockFace.UP));
                    e.blockList().remove(b);
                    continue;
                }

                if (isInMaterialList("explosive-options.help-needed-material", b))
                {
                    Block downBlock = b.getRelative(BlockFace.DOWN);
                    downBlock.getState().update(true, false);
                    e.blockList().remove(downBlock);
                    blocks.remove(downBlock);

                    if (downBlock.getType() == b.getType())
                    {
                        Block downBlock2 = downBlock.getRelative(BlockFace.DOWN);
                        downBlock2.getState().update(true, false);
                        state.update(true, false);
                        e.blockList().remove(downBlock2);
                        blocks.remove(downBlock2);
                    }
                }

                if (isInMaterialList("explosive-options.help-needed-material", b.getRelative(BlockFace.UP)))
                {
                    Block downBlock = b.getRelative(BlockFace.UP).getRelative(BlockFace.DOWN);
                    downBlock.getState().update(true, false);
                    e.blockList().remove(downBlock);
                    blocks.remove(downBlock);
                }


                if (!passedHooks(b.getLocation()))
                    continue;

                if (b.getType() == Material.TNT)
                {
                    b.setType(Material.AIR);
                    state.setType(Material.AIR);
                    TNTPrimed primed = b.getWorld().spawn(b.getLocation().add(0.0D, 1.0D, 0.0D), TNTPrimed.class);
                    primed.setFuseTicks(tntFuse);
                }

                if (blockRegeneration)
                {
                    plugin.savedStates.add(state);

                    if (restorationMemory)
                    {
                        if (state instanceof InventoryHolder)
                        {
                            if (!restoreLocations.contains(b.getLocation())) ;
                            {
                                InventoryHolder ih = (InventoryHolder) state;
                                if (ih.getInventory().getHolder() instanceof DoubleChest && (plugin.getServerVersion().startsWith("v1_13")
                                        || plugin.getServerVersion().startsWith("v1_14")))
                                {
                                    Chest chest = (Chest) state;
                                    DoubleChest dc = (DoubleChest) ih.getInventory().getHolder();
                                    Chest left = (Chest) dc.getLeftSide(), right = (Chest) dc.getRightSide();
                                    XZot1K.plugins.ptg.core.objects.DoubleChest doubleChest = new XZot1K.plugins.ptg.core.objects.DoubleChest(plugin,
                                            Objects.requireNonNull(left).getLocation(), Objects.requireNonNull(right).getLocation(),
                                            ((org.bukkit.block.data.type.Chest) chest.getBlockData()).getFacing(), dc.getInventory().getContents().clone());
                                    plugin.getSavedDoubleChests().add(doubleChest);
                                } else
                                {
                                    restoreLocations.add(b.getLocation());
                                    containers.put(b.getLocation(), ih.getInventory().getContents().clone());
                                    restoreLocations.add(b.getLocation());
                                }
                            }
                        } else if (b.getState() instanceof Sign)
                        {
                            Sign sign = (Sign) state;
                            signs.put(b.getLocation(), sign.getLines());
                        }
                    }
                }

                if (restorationMemory && !containerDrops)
                {
                    if (state instanceof InventoryHolder)
                    {
                        InventoryHolder ih = (InventoryHolder) state;
                        if (!containerDrops) ih.getInventory().clear();
                    }
                }

                if (blockPhysics)
                {
                    FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation().clone().add(0.5, 0, 0.5), b.getType(), b.getData());
                    fallingBlock.setDropItem(false);
                    fallingBlock.setVelocity(new Vector(getRandomInRange(-0.5, 0.5), getRandomInRange(0.1, 0.6), getRandomInRange(-0.5, 0.5)));
                    fallingBlock.setMetadata("P_T_G={'EXPLOSIVE_FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                    plugin.savedExplosiveFallingBlocks.add(fallingBlock.getUniqueId());
                }

                if (!plugin.getConfig().getBoolean("explosive-options.block-drops"))
                {
                    e.setYield(0);
                    b.setType(Material.AIR);
                }

                boolean regenerationAboveHeight = plugin.getConfig().getBoolean("explosive-options.regeneration-above-height");
                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (regenerationAboveHeight ? (heightLimit <= -1 || b.getY() >= heightLimit) : (heightLimit <= -1 || b.getY() <= heightLimit)))
                {
                    if (!blockLocationMemory.contains(b.getLocation()))
                        blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {

                        Block relative1 = b.getRelative(BlockFace.DOWN), relative2 = b.getRelative(BlockFace.UP);
                        relative1.getState().update(true, false);
                        relative2.getState().update(true, false);
                        state.update(true, false);

                        if (state instanceof InventoryHolder && !restorationMemory)
                        {
                            InventoryHolder ih = (InventoryHolder) state;
                            ih.getInventory().getHolder().getInventory().clear();
                        }

                        if (state instanceof Chest && plugin.getServerVersion().startsWith("v1_13") || plugin.getServerVersion().startsWith("v1_14")
                                && restorationMemory)
                            restoreDoubleChestAtLocation(b.getLocation());

                        if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                                || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());

                        if (restorationMemory)
                            if (state instanceof InventoryHolder)
                            {
                                InventoryHolder ih = (InventoryHolder) state;
                                ItemStack[] items = containers.get(b.getLocation());
                                if (containers.containsKey(b.getLocation()) && (ih.getInventory().getSize() >= items.length))
                                {
                                    ih.getInventory().setContents(items);
                                    containers.remove(b.getLocation());
                                }
                            } else if (state instanceof Sign)
                            {
                                Sign sign = (Sign) state;
                                if (signs.containsKey(b.getLocation()))
                                {
                                    int j = 0;
                                    for (String line : signs.get(b.getLocation()))
                                    {
                                        sign.setLine(j, line);
                                        j += 1;
                                    }

                                    sign.update();
                                    signs.remove(b.getLocation());
                                }
                            }

                        plugin.savedStates.remove(state);
                        regenerationCounter[0] += 1;
                    }, delay);
                    delay += speed;
                }
            }

            if (restorationMemory)
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        if (regenerationCounter[0] >= e.blockList().size())
                        {
                            for (int i = -1; ++i < restoreLocations.size(); )
                            {
                                Location location = restoreLocations.get(i);
                                Block block = location.getBlock();
                                if (block.getState() instanceof InventoryHolder && containers.containsKey(location))
                                {
                                    InventoryHolder ih = (InventoryHolder) block.getState();
                                    ih.getInventory().setContents(containers.get(location));
                                    containers.remove(location);
                                }
                            }

                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 60, 60);

            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    for (int i = -1; ++i < blocks.size(); )
                        blockLocationMemory.remove(blocks.get(i).getLocation());
                }
            }.runTaskLater(plugin, speed * (blocks.size() / 2));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onExplodeEntity(EntityExplodeEvent e)
    {
        if (isInList("explosive-options.blacklisted-worlds", Objects.requireNonNull(e.getLocation().getWorld()).getName())
                || isInList("explosive-options.entity-explosion-blacklist", e.getEntity().getType().name()))
            return;
        if (!plugin.getConfig().getBoolean("explosive-options.block-damage"))
            e.blockList().clear();
        else
        {
            boolean restorationMemory = plugin.getConfig().getBoolean("explosive-options.block-restoration-memory");
            int delay = plugin.getConfig().getInt("explosive-options.block-regeneration-options.delay"),
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed"),
                    tntFuse = plugin.getConfig().getInt("explosive-options.tnt-fuse");
            final int[] regenerationCounter = {0};

            List<Block> blocks = new ArrayList<>(e.blockList());
            sortFromLowestToHighest(blocks);

            boolean blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                    containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                    blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");

            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                if (isInMaterialList("explosive-options.effected-material-blacklist", b))
                {
                    e.blockList().remove(b);
                    blocks.remove(b);
                    continue;
                }

                if (isInMaterialList("explosive-options.help-needed-material", b.getRelative(BlockFace.UP)))
                {
                    state.update(true, false);
                    b.getRelative(BlockFace.UP).getState().update(true, false);
                    e.blockList().remove(b.getRelative(BlockFace.UP));
                    e.blockList().remove(b);
                    continue;
                }

                if (isInMaterialList("explosive-options.help-needed-material", b))
                {
                    Block downBlock = b.getRelative(BlockFace.DOWN);
                    downBlock.getState().update(true, false);
                    e.blockList().remove(downBlock);
                    blocks.remove(downBlock);

                    if (downBlock.getType() == b.getType())
                    {
                        Block downBlock2 = downBlock.getRelative(BlockFace.DOWN);
                        downBlock2.getState().update(true, false);
                        state.update(true, false);
                        e.blockList().remove(downBlock2);
                        blocks.remove(downBlock2);
                    }
                }

                if (isInMaterialList("explosive-options.help-needed-material", b.getRelative(BlockFace.UP)))
                {
                    Block downBlock = b.getRelative(BlockFace.UP).getRelative(BlockFace.DOWN);
                    downBlock.getState().update(true, false);
                    e.blockList().remove(downBlock);
                    blocks.remove(downBlock);
                }


                if (!passedHooks(b.getLocation()))
                    continue;

                if (b.getType() == Material.TNT)
                {
                    b.setType(Material.AIR);
                    state.setType(Material.AIR);
                    TNTPrimed primed = b.getWorld().spawn(b.getLocation().add(0.0D, 1.0D, 0.0D), TNTPrimed.class);
                    primed.setFuseTicks(tntFuse);
                }

                if (blockRegeneration)
                {
                    plugin.savedStates.add(state);

                    if (restorationMemory)
                    {
                        if (state instanceof InventoryHolder)
                        {
                            if (!restoreLocations.contains(b.getLocation())) ;
                            {
                                InventoryHolder ih = (InventoryHolder) state;
                                if (ih.getInventory().getHolder() instanceof DoubleChest && (plugin.getServerVersion().startsWith("v1_13")
                                        || plugin.getServerVersion().startsWith("v1_14")))
                                {
                                    Chest chest = (Chest) state;
                                    DoubleChest dc = (DoubleChest) ih.getInventory().getHolder();
                                    Chest left = (Chest) dc.getLeftSide(), right = (Chest) dc.getRightSide();
                                    XZot1K.plugins.ptg.core.objects.DoubleChest doubleChest = new XZot1K.plugins.ptg.core.objects.DoubleChest(plugin,
                                            Objects.requireNonNull(left).getLocation(), Objects.requireNonNull(right).getLocation(),
                                            ((org.bukkit.block.data.type.Chest) chest.getBlockData()).getFacing(), dc.getInventory().getContents().clone());
                                    plugin.getSavedDoubleChests().add(doubleChest);
                                } else
                                {
                                    restoreLocations.add(b.getLocation());
                                    containers.put(b.getLocation(), ih.getInventory().getContents().clone());
                                    restoreLocations.add(b.getLocation());
                                }
                            }
                        } else if (b.getState() instanceof Sign)
                        {
                            Sign sign = (Sign) state;
                            signs.put(b.getLocation(), sign.getLines());
                        }
                    }
                }

                if (restorationMemory && !containerDrops)
                {
                    if (state instanceof InventoryHolder)
                    {
                        InventoryHolder ih = (InventoryHolder) state;
                        if (!containerDrops) ih.getInventory().clear();
                    }
                }

                if (blockPhysics)
                {
                    FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation().clone().add(0.5, 0, 0.5), b.getType(), b.getData());
                    fallingBlock.setDropItem(false);
                    fallingBlock.setVelocity(new Vector(getRandomInRange(-0.5, 0.5), getRandomInRange(0.1, 0.6), getRandomInRange(-0.5, 0.5)));
                    fallingBlock.setMetadata("P_T_G={'EXPLOSIVE_FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                    plugin.savedExplosiveFallingBlocks.add(fallingBlock.getUniqueId());
                }

                if (!plugin.getConfig().getBoolean("explosive-options.block-drops"))
                {
                    e.setYield(0);
                    b.setType(Material.AIR);
                }

                boolean regenerationAboveHeight = plugin.getConfig().getBoolean("explosive-options.regeneration-above-height");
                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (regenerationAboveHeight ? (heightLimit <= -1 || b.getY() >= heightLimit) : (heightLimit <= -1 || b.getY() <= heightLimit)))
                {
                    if (!blockLocationMemory.contains(b.getLocation()))
                        blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {

                        Block relative1 = b.getRelative(BlockFace.DOWN), relative2 = b.getRelative(BlockFace.UP);
                        relative1.getState().update(true, false);
                        relative2.getState().update(true, false);
                        state.update(true, false);

                        if (state instanceof InventoryHolder && !restorationMemory)
                        {
                            InventoryHolder ih = (InventoryHolder) state;
                            ih.getInventory().getHolder().getInventory().clear();
                        }

                        if (state instanceof Chest && plugin.getServerVersion().startsWith("v1_13") || plugin.getServerVersion().startsWith("v1_14")
                                && restorationMemory)
                            restoreDoubleChestAtLocation(b.getLocation());

                        if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                                || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());

                        if (restorationMemory)
                            if (state instanceof InventoryHolder)
                            {
                                InventoryHolder ih = (InventoryHolder) state;
                                ItemStack[] items = containers.get(b.getLocation());
                                if (containers.containsKey(b.getLocation()) && (ih.getInventory().getSize() >= items.length))
                                {
                                    ih.getInventory().setContents(items);
                                    containers.remove(b.getLocation());
                                }
                            } else if (state instanceof Sign)
                            {
                                Sign sign = (Sign) state;
                                if (signs.containsKey(b.getLocation()))
                                {
                                    int j = 0;
                                    for (String line : signs.get(b.getLocation()))
                                    {
                                        sign.setLine(j, line);
                                        j += 1;
                                    }

                                    sign.update();
                                    signs.remove(b.getLocation());
                                }
                            }

                        plugin.savedStates.remove(state);
                        regenerationCounter[0] += 1;
                    }, delay);
                    delay += speed;
                }
            }

            if (restorationMemory)
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        if (regenerationCounter[0] >= e.blockList().size())
                        {
                            for (int i = -1; ++i < restoreLocations.size(); )
                            {
                                Location location = restoreLocations.get(i);
                                Block block = location.getBlock();
                                if (block.getState() instanceof InventoryHolder && containers.containsKey(location))
                                {
                                    InventoryHolder ih = (InventoryHolder) block.getState();
                                    ih.getInventory().setContents(containers.get(location));
                                    containers.remove(location);
                                }
                            }

                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 60, 60);

            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    for (int i = -1; ++i < blocks.size(); )
                        blockLocationMemory.remove(blocks.get(i).getLocation());
                }
            }.runTaskLater(plugin, speed * (blocks.size() / 2));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void EntityChangeBlockEvent(EntityChangeBlockEvent e)
    {
        if (e.getEntity() instanceof FallingBlock)
        {
            if (plugin.getConfig().getBoolean("tree-physic-options.tree-physics")
                    && plugin.savedTreeFallingBlocks.contains(e.getEntity().getUniqueId()) || e.getEntity().hasMetadata("P_T_G={'TREE_FALLING_BLOCK'}"))
            {
                if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                        || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                    e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());

                if (plugin.getConfig().getBoolean("tree-physic-options.physics-drops"))
                    ((FallingBlock) e.getEntity()).setDropItem(true);

                if (!plugin.getConfig().getBoolean("tree-physic-options.physics-form"))
                {

                    if (plugin.getConfig().getBoolean("tree-physic-options.tree-drops"))
                    {
                        BlockState blockState = e.getBlock().getState();
                        plugin.savedStates.add(blockState);

                        e.getBlock().setType(e.getTo());
                        List<ItemStack> itemStacks = new ArrayList<>(e.getBlock().getDrops());
                        for (int i = -1; ++i < itemStacks.size(); )
                            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), itemStacks.get(i));

                        blockState.update(true, true);
                    }

                    e.setCancelled(true);
                    return;
                }

                if (plugin.getConfig().getBoolean("tree-physic-options.physics-removal"))
                    new BukkitRunnable()
                    {

                        @Override
                        public void run()
                        {
                            if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                                    || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                                e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());
                            e.getBlock().setType(Material.AIR);
                        }
                    }.runTaskLater(plugin, plugin.getConfig().getInt("tree-physic-options.physics-removal-delay"));
            }

            if (plugin.savedExplosiveFallingBlocks.contains(e.getEntity().getUniqueId()) || e.getEntity().hasMetadata("P_T_G={'EXPLOSION_FALLING_BLOCK'}"))
            {
                if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                        || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                    e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());

                if (plugin.getConfig().getBoolean("explosive-options.block-drops"))
                    ((FallingBlock) e.getEntity()).setDropItem(false);

                if (!plugin.getConfig().getBoolean("explosive-options.block-physics-form"))
                {
                    e.setCancelled(true);
                    return;
                }

                if (plugin.getConfig().getBoolean("explosive-options.block-physics-removal"))
                    new BukkitRunnable()
                    {

                        @Override
                        public void run()
                        {
                            if (plugin.getServerVersion().startsWith("v1_7") || plugin.getServerVersion().startsWith("v1_8") || plugin.getServerVersion().startsWith("v1_9")
                                    || plugin.getServerVersion().startsWith("v1_10") || plugin.getServerVersion().startsWith("v1_11") || plugin.getServerVersion().startsWith("v1_12"))
                                e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());
                            e.getBlock().setType(Material.AIR);
                        }
                    }.runTaskLater(plugin, plugin.getConfig().getInt("explosive-options.block-physics-removal-delay"));
            }
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent e)
    {
        if (blockLocationMemory.contains(e.getBlock().getLocation()))
            e.setCancelled(true);
    }

    private boolean isInList(String configurationPath, String name)
    {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(configurationPath));
        for (int i = -1; ++i < list.size(); )
            if (list.get(i).equalsIgnoreCase(name))
                return true;
        return false;
    }

    private boolean isInMaterialList(String configurationPath, Material material, short durability)
    {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(configurationPath));
        for (int i = -1; ++i < list.size(); )
        {
            String line = list.get(i);
            if (line.contains(":"))
            {
                String[] lineArgs = line.split(":");
                if (lineArgs[0] != null && !lineArgs[0].equalsIgnoreCase(""))
                {
                    Material material2 = Material.getMaterial(lineArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
                    short data = (short) Integer.parseInt(lineArgs[1]);
                    if (material == material2 && (durability == data || data <= -1)) return true;
                }

                continue;
            }

            if (!line.equalsIgnoreCase("") && Material.getMaterial(line.toUpperCase().replace(" ", "_").replace("-", "_")) == material)
                return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isInMaterialList(String configurationPath, Block block)
    {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(configurationPath));
        for (int i = -1; ++i < list.size(); )
        {
            String line = list.get(i);
            if (line.contains(":"))
            {
                String[] lineArgs = line.split(":");
                if (lineArgs[0] != null && !lineArgs[0].equalsIgnoreCase(""))
                {
                    Material material = Material.getMaterial(lineArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
                    short data = (short) Integer.parseInt(lineArgs[1]);
                    if (block.getType() == material && (block.getData() == data || data <= -1)) return true;
                }

                continue;
            }

            if (!line.equalsIgnoreCase("") && Material.getMaterial(line.toUpperCase().replace(" ", "_").replace("-", "_")) == block.getType())
                return true;
        }
        return false;
    }

    private boolean passedHooks(Location location)
    {
        boolean safeLocation = true;

        if (plugin.getConfig().getBoolean("hooks-options.world-guard.use-hook"))
        {
            if (plugin.getWorldGuard().getDescription().getVersion().toLowerCase().startsWith("6"))
            {
                if (!WG_6.passedWorldGuardHook(location)) safeLocation = false;
            } else if (plugin.getWorldGuard().getDescription().getVersion().toLowerCase().startsWith("7"))
            { if (!WG_7.passedWorldGuardHook(location)) safeLocation = false; }
        }

        if (plugin.getConfig().getBoolean("hooks-options.lands.use-lands"))
        {
            LandsHook landsHook;
            if (plugin.getLandsHook() == null) landsHook = new LandsHook(plugin);
            else landsHook = plugin.getLandsHook();

            if (landsHook.getLandsAddon().getLandChunkHard(Objects.requireNonNull(location.getWorld()).getName(), location.getChunk().getX(), location.getChunk().getZ()) != null)
                safeLocation = false;
        }

        if (plugin.getConfig().getBoolean("hooks-options.feudal.use-hook"))
        {
            Kingdom kingdom = Feudal.getAPI().getKingdom(location);
            if (kingdom != null) safeLocation = false;
        }

        if (plugin.getConfig().getBoolean("hooks-options.kingdoms.use-hook"))
        {
            if (plugin.getServer().getPluginManager().getPlugin("Kingdoms") != null)
            {
                Land land = new Land(new SimpleChunkLocation(location.getChunk()));
                if (land.getOwner() != null) safeLocation = false;
            }
        }

        if (plugin.getConfig().getBoolean("hooks-options.factions.use-factions"))
        {
            if (plugin.getConfig().getBoolean("hooks-options.factions.factions-uuid"))
            {
                FLocation fLocation = new FLocation(location);
                com.massivecraft.factions.Faction factionAtLocation = Board.getInstance().getFactionAt(fLocation);
                if (factionAtLocation != null && !factionAtLocation.isWilderness() && !factionAtLocation.isWarZone())
                    safeLocation = false;
            } else
            {
                com.massivecraft.factions.entity.Faction factionAtLocation = BoardColl.get().getFactionAt(PS.valueOf(location));
                if (factionAtLocation != null && !factionAtLocation.getComparisonName().equals(FactionColl.get().getSafezone().getComparisonName())
                        && !factionAtLocation.getComparisonName().equals(FactionColl.get().getWarzone().getComparisonName()))
                    safeLocation = false;
            }
        }

        if (plugin.getConfig().getBoolean("hooks-options.askyblock.use-askyblock"))
        {
            Plugin aSkyBlock = plugin.getServer().getPluginManager().getPlugin("ASkyBlock");
            if (aSkyBlock != null)
            {
                Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
                if (island != null) safeLocation = false;
            }
        }

        if (plugin.getConfig().getBoolean("hooks-options.grief-preventation.use-grief-preventation"))
        {
            Plugin griefPrevention = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
            if (griefPrevention != null)
            {
                Claim claimAtLocation = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
                if (claimAtLocation != null) safeLocation = false;
            }
        }

        if (plugin.getConfig().getBoolean("hooks-options.residence.use-residence"))
        {
            Plugin residence = plugin.getServer().getPluginManager().getPlugin("Residence");
            if (residence != null)
            {
                ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
                if (res != null) safeLocation = false;
            }
        }

        if (plugin.getConfig().getBoolean("hooks-options.towny.use-towny"))
        {
            Plugin towny = plugin.getServer().getPluginManager().getPlugin("Towny");
            if (towny != null)
            {
                try
                {
                    Town town = WorldCoord.parseWorldCoord(location).getTownBlock().getTown();
                    if (town != null) safeLocation = false;
                } catch (Exception ignored) {}
            }
        }

        HookCallEvent hookCallEvent = new HookCallEvent(location, safeLocation);
        plugin.getServer().getPluginManager().callEvent(hookCallEvent);
        safeLocation = hookCallEvent.isSafeLocation();

        return safeLocation;
    }

    public double getRandomInRange(double min, double max)
    {
        return (min + (max - min) * random.nextDouble());
    }

    private ArrayList<Location> getPlacedLocationMemory()
    {
        return placedLocationMemory;
    }

    private void setPlacedLocationMemory(ArrayList<Location> placedLocationMemory)
    {
        this.placedLocationMemory = placedLocationMemory;
    }

    private void restoreDoubleChestAtLocation(Location location)
    {
        for (int i = -1; ++i < plugin.getSavedDoubleChests().size(); )
        {
            XZot1K.plugins.ptg.core.objects.DoubleChest doubleChest = plugin.getSavedDoubleChests().get(i);
            if ((doubleChest.getLeftSide().getWorld().getName().equals(location.getWorld().getName()) && doubleChest.getLeftSide().getBlockX() == location.getBlockX()
                    && doubleChest.getLeftSide().getBlockY() == location.getBlockY() && doubleChest.getLeftSide().getBlockZ() == location.getBlockZ()) ||
                    (doubleChest.getRightSide().getWorld().getName().equals(location.getWorld().getName()) && doubleChest.getRightSide().getBlockX() == location.getBlockX()
                            && doubleChest.getRightSide().getBlockY() == location.getBlockY() && doubleChest.getRightSide().getBlockZ() == location.getBlockZ()))
            {
                doubleChest.restore();
                break;
            }
        }
    }

    // sorting
    public void sortFromLowestToHighest(List<Block> blockList)
    {
        sortFromLowestToHighest(blockList, 0, (blockList.size() - 1));
    }

    private void sortFromLowestToHighest(List<Block> blockList, int low, int high)
    {
        if (low < high + 1)
        {
            int p = partition(blockList, low, high);
            sortFromLowestToHighest(blockList, low, p - 1);
            sortFromLowestToHighest(blockList, p + 1, high);
        }
    }

    private void swap(List<Block> blockList, int index1, int index2)
    {
        Block temp = blockList.get(index1);
        blockList.set(index1, blockList.get(index2));
        blockList.set(index2, temp);
    }

    private int getPivot(int low, int high)
    {
        return random.nextInt((high - low) + 1) + low;
    }

    private int partition(List<Block> blockList, int low, int high)
    {
        swap(blockList, low, getPivot(low, high));
        int border = low + 1;
        for (int i = border; i <= high; i++)
        {
            if (blockList.get(i).getY() < blockList.get(low).getY())
            {
                swap(blockList, i, border++);
            }
        }

        swap(blockList, low, border - 1);
        return border - 1;
    }
}
