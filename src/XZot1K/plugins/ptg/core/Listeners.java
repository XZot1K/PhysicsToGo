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
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Listeners implements Listener
{

    private PhysicsToGo plugin;
    private Random random;

    private ArrayList<Location> blockLocationMemory, placedLocationMemory;
    private HashMap<Location, ItemStack[]> containers;
    private HashMap<Location, String[]> signs;

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
        boolean blockReversion = plugin.getConfig().getBoolean("block-place-options.block-reversion");

        int heightLimit = plugin.getConfig().getInt("block-place-options.reversion-height");
        if (blockReversion && (heightLimit <= -1 || e.getBlock().getY() <= heightLimit))
        {
            Material previousMaterial = e.getBlockReplacedState().getType();
            byte previousData = e.getBlockReplacedState().getRawData();
            getPlacedLocationMemory().add(e.getBlock().getLocation());
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                Material placedMaterial = e.getBlock().getType();
                e.getBlock().setType(previousMaterial);
                if (!plugin.getServerVersion().startsWith("v1_13"))
                {
                    try
                    {
                        Method closeMethod = e.getBlock().getClass().getMethod("setData", Short.class);
                        if (closeMethod != null) closeMethod.invoke(e.getBlock().getClass(), (short) previousData);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
                }

                e.getBlock().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND,
                        e.getBlock().getType() == Material.AIR ? placedMaterial.getId()
                                : e.getBlock().getType().getId());
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
                if (!containerDrops)
                    ih.getInventory().clear();
            } else if (blockState instanceof Sign)
            {
                Sign sign = (Sign) blockState;
                signs.put(e.getBlock().getLocation(), sign.getLines());
            }

        if (!dropItems || getPlacedLocationMemory().contains(e.getBlock().getLocation()))
        {
            Objects.requireNonNull(e.getBlock().getLocation().getWorld()).playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND, 1);
            e.getBlock().setType(Material.AIR);
            if (getPlacedLocationMemory().contains(e.getBlock().getLocation()))
            {
                getPlacedLocationMemory().remove(e.getBlock().getLocation());
                return;
            }
        }

        int heightLimit = plugin.getConfig().getInt("block-break-options.regeneration-height");
        if (blockRegeneration && (heightLimit <= -1 || e.getBlock().getY() <= heightLimit))
        {
            if (!blockLocationMemory.contains(e.getBlock().getLocation()))
                blockLocationMemory.add(e.getBlock().getLocation());
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                blockState.update(true, false);
                e.getBlock().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND,
                        e.getBlock().getType().getId());
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
        if (isInList("explosive-options.blacklisted-worlds", e.getBlock().getWorld().getName())) return;
        if (!plugin.getConfig().getBoolean("explosive-options.block-damage")) e.blockList().clear();
        else
        {
            boolean restorationMemory = plugin.getConfig().getBoolean("explosive-options.block-restoration-memory");
            int delay = plugin.getConfig().getInt("explosive-options.block-regeneration-options.delay"),
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed");

            List<Location> restoreLocations = new ArrayList<>();
            List<Block> blocks = new ArrayList<>(e.blockList());
            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                if (isInMaterialList("explosive-options.effected-material-blacklist", b))
                {
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

                    state.update(true, false);
                    e.blockList().remove(b);
                    continue;
                }

                if (!passedHooks(b.getLocation()))
                    continue;

                if (!plugin.getConfig().getBoolean("explosive-options.block-drops"))
                {
                    e.setYield(0);
                    if (isInMaterialList("explosive-options.drop-watch", b.getType(), b.getData()))
                        b.setType(Material.AIR);
                }

                boolean containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                        blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                        blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");
                if (blockRegeneration) plugin.savedStates.add(state);

                if (b.getType() == Material.TNT)
                {
                    b.setType(Material.AIR);
                    state.setType(Material.AIR);
                    TNTPrimed primed = b.getWorld().spawn(b.getLocation().add(0.0D, 1.0D, 0.0D), TNTPrimed.class);
                    primed.setFuseTicks(80);
                    plugin.savedStates.remove(state);
                    continue;
                }

                if (blockRegeneration && restorationMemory)
                    if (state instanceof InventoryHolder)
                    {
                        InventoryHolder ih = (InventoryHolder) state;
                        restoreLocations.add(b.getLocation());
                        containers.put(b.getLocation(), ih.getInventory().getContents().clone());
                        restoreLocations.add(b.getLocation());
                        if (!containerDrops) ih.getInventory().clear();
                    } else if (b.getState() instanceof Sign)
                    {
                        Sign sign = (Sign) state;
                        signs.put(b.getLocation(), sign.getLines());
                    }

                if (blockPhysics)
                {
                    FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation().clone().add(0.5, 0, 0.5), b.getType(), b.getData());
                    fallingBlock.setDropItem(false);
                    fallingBlock.setVelocity(new Vector(getRandomInRange(-0.5, 0.5), getRandomInRange(0.1, 0.6), getRandomInRange(-0.5, 0.5)));
                    fallingBlock.setMetadata("P_T_G={'EXPLOSIVE_FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                    plugin.savedExplosiveFallingBlocks.add(fallingBlock.getUniqueId());
                }

                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (heightLimit <= -1 || b.getY() <= heightLimit))
                {
                    if (!blockLocationMemory.contains(b.getLocation()))
                        blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {
                        state.update(true, false);
                        b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());
                        Block relative1 = b.getRelative(BlockFace.DOWN), relative2 = b.getRelative(BlockFace.UP);
                        relative1.getState().update(true, false);
                        relative2.getState().update(true, false);

                        if (restorationMemory)
                            if (state instanceof Sign)
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
                        for (int i = -1; ++i < restoreLocations.size(); )
                        {
                            Location location = restoreLocations.get(i);
                            Block block = location.getBlock();
                            if (block.getState() instanceof InventoryHolder)
                            {
                                InventoryHolder ih = (InventoryHolder) block.getState();
                                ih.getInventory().setContents(containers.get(location));
                                containers.remove(location);
                            }
                        }
                    }
                }.runTaskLater(plugin, delay + plugin.getConfig().getInt("explosive-options.block-regeneration-options.container-fix-rate"));

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
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed");

            List<Location> restoreLocations = new ArrayList<>();
            List<Block> blocks = new ArrayList<>(e.blockList());
            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                if (isInMaterialList("explosive-options.effected-material-blacklist", b))
                {
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

                    state.update(true, false);
                    e.blockList().remove(b);
                    continue;
                }

                if (!passedHooks(b.getLocation()))
                    continue;

                if (!plugin.getConfig().getBoolean("explosive-options.block-drops"))
                {
                    e.setYield(0);
                    if (isInMaterialList("explosive-options.drop-watch", b.getType(), b.getData()))
                        b.setType(Material.AIR);
                }

                boolean containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                        blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                        blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");
                if (blockRegeneration) plugin.savedStates.add(state);

                if (b.getType() == Material.TNT)
                {
                    b.setType(Material.AIR);
                    state.setType(Material.AIR);
                    TNTPrimed primed = b.getWorld().spawn(b.getLocation().add(0.0D, 1.0D, 0.0D), TNTPrimed.class);
                    primed.setFuseTicks(80);
                    plugin.savedStates.remove(state);
                    continue;
                }

                if (blockRegeneration && restorationMemory)
                    if (state instanceof InventoryHolder)
                    {
                        InventoryHolder ih = (InventoryHolder) state;
                        restoreLocations.add(b.getLocation());
                        containers.put(b.getLocation(), ih.getInventory().getContents().clone());
                        restoreLocations.add(b.getLocation());
                        if (!containerDrops) ih.getInventory().clear();
                    } else if (b.getState() instanceof Sign)
                    {
                        Sign sign = (Sign) state;
                        signs.put(b.getLocation(), sign.getLines());
                    }

                if (blockPhysics)
                {
                    FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation().clone().add(0.5, 0, 0.5), b.getType(), b.getData());
                    fallingBlock.setDropItem(false);
                    fallingBlock.setVelocity(new Vector(getRandomInRange(-0.5, 0.5), getRandomInRange(0.1, 0.6), getRandomInRange(-0.5, 0.5)));
                    fallingBlock.setMetadata("P_T_G={'EXPLOSIVE_FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                    plugin.savedExplosiveFallingBlocks.add(fallingBlock.getUniqueId());
                }

                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (heightLimit <= -1 || b.getY() <= heightLimit))
                {
                    if (!blockLocationMemory.contains(b.getLocation()))
                        blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {
                        state.update(true, false);
                        b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());
                        Block relative1 = b.getRelative(BlockFace.DOWN), relative2 = b.getRelative(BlockFace.UP);
                        relative1.getState().update(true, false);
                        relative2.getState().update(true, false);

                        if (restorationMemory)
                            if (state instanceof Sign)
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
                        for (int i = -1; ++i < restoreLocations.size(); )
                        {
                            Location location = restoreLocations.get(i);
                            Block block = location.getBlock();
                            if (block.getState() instanceof InventoryHolder)
                            {
                                InventoryHolder ih = (InventoryHolder) block.getState();
                                ih.getInventory().setContents(containers.get(location));
                                containers.remove(location);
                            }
                        }
                    }
                }.runTaskLater(plugin, delay + plugin.getConfig().getInt("explosive-options.block-regeneration-options.container-fix-rate"));

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
                e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());
                if (!plugin.getConfig().getBoolean("tree-physic-options.physics-form"))
                {
                    e.setCancelled(true);
                    return;
                }

                if (plugin.getConfig().getBoolean("tree-physic-options.physics-drops"))
                    ((FallingBlock) e.getEntity()).setDropItem(true);

                if (plugin.getConfig().getBoolean("tree-physic-options.physics-removal"))
                    new BukkitRunnable()
                    {

                        @Override
                        public void run()
                        {
                            e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());
                            e.getBlock().setType(Material.AIR);
                        }
                    }.runTaskLater(plugin, plugin.getConfig().getInt("tree-physic-options.physics-removal-delay"));
            }

            if (plugin.savedExplosiveFallingBlocks.contains(e.getEntity().getUniqueId()) || e.getEntity().hasMetadata("P_T_G={'EXPLOSION_FALLING_BLOCK'}"))
            {
                e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());
                if (!plugin.getConfig().getBoolean("explosive-options.block-physics-form"))
                {
                    e.setCancelled(true);
                    return;
                }

                if (plugin.getConfig().getBoolean("explosive-options.block-drops"))
                    ((FallingBlock) e.getEntity()).setDropItem(false);
                if (plugin.getConfig().getBoolean("explosive-options.block-physics-removal"))
                    new BukkitRunnable()
                    {

                        @Override
                        public void run()
                        {
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
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(Objects.requireNonNull(location.getWorld())));
            if (regionManager != null)
            {
                List<String> regionList = new ArrayList<>(regionManager.getRegions().keySet());
                for (int i = -1; ++i < regionList.size(); )
                {
                    String regionId = regionList.get(i);
                    ProtectedRegion region = regionManager.getRegions().get(regionId);
                    if (region != null)
                    {
                        if (region.contains((int) location.getX(), (int) location.getY(), (int) location.getZ())
                                && !isInList("hooks-options.world-guard.region-whitelist", region.getId()))
                            safeLocation = false;
                    }
                }
            }

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
}
