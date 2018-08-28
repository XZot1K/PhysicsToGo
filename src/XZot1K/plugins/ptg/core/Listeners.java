package XZot1K.plugins.ptg.core;

import XZot1K.plugins.ptg.PhysicsToGo;
import com.massivecraft.factions.*;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import me.angeschossen.lands.Lands;
import me.angeschossen.lands.api.LandsAPI;
import me.angeschossen.lands.api.objects.LandChunk;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
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
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import us.forseth11.feudal.core.Feudal;
import us.forseth11.feudal.kingdoms.Kingdom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Listeners implements Listener
{

    private PhysicsToGo plugin;

    private ArrayList<Location> blockLocationMemory;
    private HashMap<Location, ItemStack[]> containers;
    private HashMap<Location, String[]> signs;
    private List<Block> blockList;

    public Listeners(PhysicsToGo plugin)
    {
        this.plugin = plugin;
        blockLocationMemory = new ArrayList<>();
        containers = new HashMap<>();
        signs = new HashMap<>();
        blockList = new ArrayList<>();
    }

    @EventHandler
    public void itemSpawn(ItemSpawnEvent e)
    {
        if (blockList.contains(e.getLocation().getBlock())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e)
    {
        if (!plugin.getConfig().getBoolean("block-place-options.block-place-event")
                || isInList("block-place-options.blacklisted-worlds", e.getBlock().getWorld().getName())
                || isBlacklistedMaterial("block-place-options.effected-material-blacklist", e.getBlock())
                || !passedHooks(e.getBlock().getLocation(), true, false, true, true, true, true)) return;
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
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                Material placedMaterial = e.getBlock().getType();
                e.getBlock().setType(previousMaterial);
                if (!plugin.getServerVersion().equalsIgnoreCase("v1_13_R1"))
                    e.getBlock().setData(previousData);
                e.getBlock().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND,
                        e.getBlock().getType() == Material.AIR ? placedMaterial.getId() :
                                e.getBlock().getType().getId());
            }, delay);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e)
    {
        if (!plugin.getConfig().getBoolean("block-break-options.block-break-event")
                || isInList("block-break-options.blacklisted-worlds", e.getBlock().getWorld().getName())
                || isBlacklistedMaterial("block-break-options.effected-material-blacklist", e.getBlock())
                || !passedHooks(e.getBlock().getLocation(), true, true, true, true, true, true)) return;

        int delay = plugin.getConfig().getInt("block-break-options.block-regeneration-options.delay");
        boolean dropItems = plugin.getConfig().getBoolean("block-break-options.block-drops"),
                restorationMemory = plugin.getConfig().getBoolean("block-break-options.block-restoration-memory"),
                containerDrops = plugin.getConfig().getBoolean("block-break-options.container-drops"),
                blockRegeneration = plugin.getConfig().getBoolean("block-break-options.block-regeneration");
        BlockState blockState = e.getBlock().getState();
        if (blockRegeneration) plugin.savedStates.add(blockState);

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

        if (!dropItems)
        {
            e.getBlock().getLocation().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND, 1);
            e.getBlock().setType(Material.AIR);
        }

        int heightLimit = plugin.getConfig().getInt("block-break-options.regeneration-height");
        if (blockRegeneration && (heightLimit <= -1 || e.getBlock().getY() <= heightLimit))
        {
            if (!blockLocationMemory.contains(e.getBlock().getLocation()))
                blockLocationMemory.add(e.getBlock().getLocation());
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                try
                {
                    blockState.update(true, false);
                    blockState.update();
                    e.getBlock().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND,
                            e.getBlock().getType().getId());

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
                } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {}
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
            int delay = plugin.getConfig().getInt("explosive-options.block-regeneration-options.delay"),
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed");
            List<Block> blocks = new ArrayList<>(e.blockList());
            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                if (isBlacklistedMaterial("explosive-options.effected-material-blacklist", b)
                        || !passedHooks(b.getLocation(), true, true, true, true, true, true))
                    continue;

                boolean dropItems = plugin.getConfig().getBoolean("explosive-options.block-drops"),
                        restorationMemory = plugin.getConfig().getBoolean("explosive-options.block-restoration-memory"),
                        containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                        blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                        blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");
                if (blockRegeneration) plugin.savedStates.add(state);

                if (!dropItems)
                {
                    e.setYield(0);
                    blockList.add(b);
                }

                if (b.getType() == Material.TNT)
                {
                    b.setType(Material.AIR);
                    state.setType(Material.AIR);
                    Entity primed = b.getWorld().spawn(b.getLocation().add(0.0D, 1.0D, 0.0D), TNTPrimed.class);
                    ((TNTPrimed) primed).setFuseTicks(80);
                    plugin.savedStates.remove(state);
                    continue;
                }

                if (blockRegeneration && restorationMemory)
                    if (state instanceof InventoryHolder)
                    {
                        InventoryHolder ih = (InventoryHolder) state;
                        containers.put(b.getLocation(), ih.getInventory().getContents().clone());
                        if (!containerDrops) ih.getInventory().clear();
                    } else if (b.getState() instanceof Sign)
                    {
                        Sign sign = (Sign) state;
                        signs.put(b.getLocation(), sign.getLines());
                    }

                if (blockPhysics)
                {
                    try
                    {
                        FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation(), b.getType(),
                                b.getData());
                        fallingBlock.setDropItem(false);
                        fallingBlock.setVelocity(new Vector(1, 1, 1));
                        fallingBlock.setMetadata("P_T_G={'FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                        plugin.savedFallingBlocks.add(fallingBlock.getUniqueId());
                    } catch (IllegalArgumentException ignored) {}
                }

                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (heightLimit <= -1 || b.getY() <= heightLimit))
                {
                    if (!blockLocationMemory.contains(b.getLocation())) blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {
                        try
                        {
                            state.update(true, false);
                            state.update();
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());

                            if (restorationMemory)
                                if (state instanceof InventoryHolder)
                                {
                                    InventoryHolder ih = (InventoryHolder) state;
                                    if (containers.containsKey(b.getLocation()))
                                    {
                                        ih.getInventory().setContents(containers.get(b.getLocation()));
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

                            blockLocationMemory.remove(b.getLocation());
                            plugin.savedStates.remove(state);
                        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {}
                    }, delay);
                    delay += speed;
                }
            }
        }

        blockList.clear();
    }

    @EventHandler
    public void onExplodeEntity(EntityExplodeEvent e)
    {
        if (isInList("explosive-options.blacklisted-worlds", e.getLocation().getWorld().getName())
                || isInList("explosive-options.entity-explosion-blacklist", e.getEntity().getType().name())) return;
        if (!plugin.getConfig().getBoolean("explosive-options.block-damage")) e.blockList().clear();
        else
        {
            int delay = plugin.getConfig().getInt("explosive-options.block-regeneration-options.delay"),
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed");
            List<Block> blocks = new ArrayList<>(e.blockList());
            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                if (isBlacklistedMaterial("explosive-options.effected-material-blacklist", b)
                        || !passedHooks(b.getLocation(), true, true, true, true, true, true))
                    continue;

                boolean dropItems = plugin.getConfig().getBoolean("explosive-options.block-drops"),
                        restorationMemory = plugin.getConfig().getBoolean("explosive-options.block-restoration-memory"),
                        containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                        blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                        blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");
                if (blockRegeneration) plugin.savedStates.add(state);

                if (!dropItems)
                {
                    e.setYield(0);
                    blockList.add(b);
                }

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
                        containers.put(b.getLocation(), ih.getInventory().getContents().clone());
                        if (!containerDrops) ih.getInventory().clear();
                    } else if (b.getState() instanceof Sign)
                    {
                        Sign sign = (Sign) state;
                        signs.put(b.getLocation(), sign.getLines());
                    }

                if (blockPhysics)
                {
                    try
                    {
                        FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation(), b.getType(), b.getData());
                        fallingBlock.setDropItem(false);
                        fallingBlock.setVelocity(new Vector((Math.random() < 0.5) ? 0 : 1, 1, (Math.random() < 0.5) ?
                                0 : 1));
                        fallingBlock.setMetadata("P_T_G={'FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                        plugin.savedFallingBlocks.add(fallingBlock.getUniqueId());
                    } catch (IllegalArgumentException ignored) {}
                }

                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (heightLimit <= -1 || b.getY() <= heightLimit))
                {
                    if (!blockLocationMemory.contains(b.getLocation())) blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {
                        try
                        {
                            state.update(true, false);
                            state.update();
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());

                            if (restorationMemory)
                                if (state instanceof InventoryHolder)
                                {
                                    InventoryHolder ih = (InventoryHolder) state;
                                    if (containers.containsKey(b.getLocation()))
                                    {
                                        ih.getInventory().setContents(containers.get(b.getLocation()));
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

                            blockLocationMemory.remove(b.getLocation());
                            plugin.savedStates.remove(state);
                        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {}
                    }, delay);
                    delay += speed;
                }
            }
        }

        blockList.clear();
    }

    @EventHandler
    public void EntityChangeBlockEvent(EntityChangeBlockEvent e)
    {
        if (e.getEntity() instanceof FallingBlock)
        {
            if (plugin.savedFallingBlocks.contains(e.getEntity().getUniqueId()) || e.getEntity().hasMetadata("P_T_G={'FALLING_BLOCK'}"))
            {
                e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND,
                        e.getBlock().getType().getId());
                if (!plugin.getConfig().getBoolean("explosive-options.block-physics-form")) e.setCancelled(true);
                if (!plugin.getConfig().getBoolean("explosive-options.block-drops"))
                    ((FallingBlock) e.getEntity()).setDropItem(false);
            }
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent e)
    {
        if (blockLocationMemory.contains(e.getBlock().getLocation())) e.setCancelled(true);
    }

    private boolean isInList(String configurationPath, String name)
    {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(configurationPath));
        for (int i = -1; ++i < list.size(); ) if (list.get(i).equalsIgnoreCase(name)) return true;
        return false;
    }

    private boolean isBlacklistedMaterial(String configurationPath, Block block)
    {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(configurationPath));
        for (int i = -1; ++i < list.size(); )
        {
            String line = list.get(i);
            try
            {
                if (line.contains(":"))
                {
                    String[] lineArgs = line.split(":");
                    Material material = Material.getMaterial(lineArgs[0].toUpperCase().replace(" ", "_").replace("-",
                            "_"));
                    short data = (short) Integer.parseInt(lineArgs[1]);
                    if (block.getType() == material && block.getData() == data) return true;
                    continue;
                }

                if (Material.getMaterial(line.toUpperCase().replace(" ", "_").replace("-", "_")) == block.getType())
                    return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean passedHooks(Location location, boolean useWorldGuard, boolean useFeudal, boolean useFactions, boolean useGP, boolean useASkyBlock, boolean useLands)
    {
        if (useWorldGuard && plugin.getConfig().getBoolean("hooks-options.world-guard.use-hook"))
        {
            RegionContainer container = plugin.getWorldGuard().getRegionContainer();
            RegionManager regions = container.get(location.getWorld());
            if (regions != null)
            {
                List<String> regionList = new ArrayList<>(regions.getRegions().keySet());
                for (int i = -1; ++i < regionList.size(); )
                {
                    String r = regionList.get(i);
                    ProtectedRegion region = regions.getRegion(r);
                    if (region != null)
                    {
                        com.sk89q.worldedit.Vector loc = new com.sk89q.worldedit.Vector(location.getX(), location.getY(), location.getZ());
                        if (region.contains(loc) && isInList("hooks-options.world-guard.region-whitelist", r))
                            return false;
                    }
                }
            }
        }

        if (useFeudal && plugin.getConfig().getBoolean("hooks-options.feudal.use-hook"))
        {
            Kingdom kingdom = Feudal.getAPI().getKingdom(location);
            return kingdom == null;
        }

        if (useFactions && plugin.getConfig().getBoolean("hooks-options.factions.use-factions"))
        {
            if (plugin.getConfig().getBoolean("hooks-options.factions.factions-uuid"))
            {
                try
                {
                    FLocation fLocation = new FLocation(location);
                    com.massivecraft.factions.Faction factionAtLocation = Board.getInstance().getFactionAt(fLocation);
                    if (factionAtLocation != null && !(factionAtLocation.isWilderness() || factionAtLocation.isWarZone()))
                        return false;
                } catch (Exception ignored) {}
            } else
            {
                try
                {
                    com.massivecraft.factions.entity.Faction factionAtLocation = BoardColl.get().getFactionAt(PS.valueOf(location));
                    if (factionAtLocation != null && !(factionAtLocation.getComparisonName().equals(FactionColl.get().getSafezone().getComparisonName())
                            || factionAtLocation.getComparisonName().equals(FactionColl.get().getWarzone().getComparisonName())))
                        return false;
                } catch (Exception ignored) {}
            }
        }

        if (useASkyBlock && plugin.getConfig().getBoolean("hooks-options.askyblock.use-askyblock"))
        {
            Plugin aSkyBlock = plugin.getServer().getPluginManager().getPlugin("ASkyBlock");
            if (aSkyBlock != null)
            {
                Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
                if (island != null) return false;
            }
        }

        if (useGP && plugin.getConfig().getBoolean("hooks-options.grief-preventation.use-grief-preventation"))
        {
            Plugin griefPrevention = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
            if (griefPrevention != null)
            {
                Claim claimAtLocation = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
                if (claimAtLocation != null) return false;
            }
        }

        if (useLands && plugin.getConfig().getBoolean("hooks-options.lands.use-lands"))
        {
            Plugin lands = plugin.getServer().getPluginManager().getPlugin("Lands");
            if (lands != null)
            {
                LandChunk landChunk = Lands.getLandsAPI().getLandChunk(location);
                if (landChunk != null && landChunk.getLand() != null && landChunk.getLand().getOwnerUUID() != null)
                    return false;
            }
        }

        return true;
    }

}
