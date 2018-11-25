package XZot1K.plugins.ptg.core;

import java.util.*;

import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.SimpleChunkLocation;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.massivecore.ps.PS;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;

import XZot1K.plugins.ptg.PhysicsToGo;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import us.forseth11.feudal.core.Feudal;
import us.forseth11.feudal.kingdoms.Kingdom;

public class Listeners implements Listener
{

    private PhysicsToGo plugin;

    private ArrayList<Location> blockLocationMemory, placedLocationMemory;
    private HashMap<Location, ItemStack[]> containers;
    private HashMap<Location, String[]> signs;
    private List<Block> blockList;

    public Listeners(PhysicsToGo plugin)
    {
        this.plugin = plugin;
        setPlacedLocationMemory(new ArrayList<>());
        blockLocationMemory = new ArrayList<>();
        containers = new HashMap<>();
        signs = new HashMap<>();
        blockList = new ArrayList<>();
    }

    @EventHandler
    public void itemSpawn(ItemSpawnEvent e)
    {
        if (blockList.contains(e.getLocation().getBlock()))
            e.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlace(BlockPlaceEvent e)
    {
        if (!plugin.getConfig().getBoolean("block-place-options.block-place-event")
                || isInList("block-place-options.blacklisted-worlds", e.getBlock().getWorld().getName())
                || isInMaterialList("block-place-options.effected-material-blacklist", e.getBlock())
                || !passedHooks(e.getBlock().getLocation(), true, true, true, true, true, true, true, true))
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
                    e.getBlock().setData(previousData);
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
        if (!plugin.getConfig().getBoolean("block-break-options.block-break-event")
                || isInList("block-break-options.blacklisted-worlds", e.getBlock().getWorld().getName())
                || !passedHooks(e.getBlock().getLocation(), true, true, true, true, true, true, true, true))
            return;
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
            e.getBlock().getLocation().getWorld().playEffect(e.getBlock().getLocation(), Effect.STEP_SOUND, 1);
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
                try
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
                } catch (IllegalArgumentException | IndexOutOfBoundsException ignored)
                {
                }
            }, delay);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e)
    {
        if (isInList("explosive-options.blacklisted-worlds", e.getBlock().getWorld().getName()))
            return;
        if (!plugin.getConfig().getBoolean("explosive-options.block-damage"))
            e.blockList().clear();
        else
        {
            int delay = plugin.getConfig().getInt("explosive-options.block-regeneration-options.delay"),
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed");
            List<Block> blocks = new ArrayList<>(e.blockList());
            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                if (isInMaterialList("explosive-options.effected-material-blacklist", b))
                {
                    e.blockList().remove(b);
                    continue;
                }

                if (!passedHooks(b.getLocation(), true, true, true, true, true, true, true, true))
                    continue;

                boolean dropItems = plugin.getConfig().getBoolean("explosive-options.block-drops"),
                        restorationMemory = plugin.getConfig().getBoolean("explosive-options.block-restoration-memory"),
                        containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                        blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                        blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");
                if (blockRegeneration)
                    plugin.savedStates.add(state);

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
                        if (!containerDrops)
                            ih.getInventory().clear();
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
                    } catch (IllegalArgumentException ignored)
                    {
                    }
                }

                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (heightLimit <= -1 || b.getY() <= heightLimit))
                {
                    if (!blockLocationMemory.contains(b.getLocation()))
                        blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {
                        try
                        {
                            state.update(true, false);
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());
                            Block relative1 = e.getBlock().getRelative(BlockFace.DOWN),
                                    relative2 = e.getBlock().getRelative(BlockFace.UP);
                            relative1.getState().update(true, false);
                            relative2.getState().update(true, false);

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
                        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored)
                        {
                        }
                    }, delay);
                    delay += speed;
                }
            }
        }

        blockList.clear();
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onExplodeEntity(EntityExplodeEvent e)
    {
        if (isInList("explosive-options.blacklisted-worlds", e.getLocation().getWorld().getName())
                || isInList("explosive-options.entity-explosion-blacklist", e.getEntity().getType().name()))
            return;
        if (!plugin.getConfig().getBoolean("explosive-options.block-damage"))
            e.blockList().clear();
        else
        {
            int delay = plugin.getConfig().getInt("explosive-options.block-regeneration-options.delay"),
                    speed = plugin.getConfig().getInt("explosive-options.block-regeneration-options.speed");
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

                if (!passedHooks(b.getLocation(), true, true, true, true, true, true, true, true))
                    continue;

                boolean dropItems = plugin.getConfig().getBoolean("explosive-options.block-drops"),
                        restorationMemory = plugin.getConfig().getBoolean("explosive-options.block-restoration-memory"),
                        containerDrops = plugin.getConfig().getBoolean("explosive-options.container-drops"),
                        blockPhysics = plugin.getConfig().getBoolean("explosive-options.block-physics"),
                        blockRegeneration = plugin.getConfig().getBoolean("explosive-options.block-regeneration");
                if (blockRegeneration)
                    plugin.savedStates.add(state);

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
                        if (!containerDrops)
                            ih.getInventory().clear();
                    } else if (b.getState() instanceof Sign)
                    {
                        Sign sign = (Sign) state;
                        signs.put(b.getLocation(), sign.getLines());
                    }

                if (blockPhysics)
                {
                    try
                    {
                        @SuppressWarnings("deprecation")
                        FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation(), b.getType(),
                                b.getData());
                        fallingBlock.setDropItem(false);
                        fallingBlock.setVelocity(
                                new Vector((Math.random() < 0.5) ? 0 : 1, 1, (Math.random() < 0.5) ? 0 : 1));
                        fallingBlock.setMetadata("P_T_G={'FALLING_BLOCK'}", new FixedMetadataValue(plugin, ""));
                        plugin.savedFallingBlocks.add(fallingBlock.getUniqueId());
                    } catch (IllegalArgumentException ignored)
                    {
                    }
                }

                int heightLimit = plugin.getConfig().getInt("explosive-options.regeneration-height");
                if (blockRegeneration && (heightLimit <= -1 || b.getY() <= heightLimit))
                {
                    if (!blockLocationMemory.contains(b.getLocation()))
                        blockLocationMemory.add(b.getLocation());
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                    {
                        try
                        {
                            state.update(true, false);
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType().getId());
                            Block relative1 = b.getRelative(BlockFace.DOWN), relative2 = b.getRelative(BlockFace.UP);
                            relative1.getState().update(true, false);
                            relative2.getState().update(true, false);

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
                        } catch (IllegalArgumentException | IndexOutOfBoundsException ignored)
                        {
                        }
                    }, delay);
                    delay += speed;
                }
            }
        }

        blockList.clear();
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void EntityChangeBlockEvent(EntityChangeBlockEvent e)
    {
        if (e.getEntity() instanceof FallingBlock)
        {
            if (plugin.savedFallingBlocks.contains(e.getEntity().getUniqueId())
                    || e.getEntity().hasMetadata("P_T_G={'FALLING_BLOCK'}"))
            {
                e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND,
                        e.getBlock().getType().getId());
                if (!plugin.getConfig().getBoolean("explosive-options.block-physics-form"))
                    e.setCancelled(true);
                if (!plugin.getConfig().getBoolean("explosive-options.block-drops"))
                    ((FallingBlock) e.getEntity()).setDropItem(false);
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

    @SuppressWarnings("deprecation")
    private boolean isInMaterialList(String configurationPath, Block block)
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
                    Material material = Material
                            .getMaterial(lineArgs[0].toUpperCase().replace(" ", "_").replace("-", "_"));
                    short data = (short) Integer.parseInt(lineArgs[1]);
                    if (block.getType() == material && (block.getData() == data || data <= -1))
                        return true;
                    continue;
                }

                if (Material.getMaterial(line.toUpperCase().replace(" ", "_").replace("-", "_")) == block.getType())
                    return true;
            } catch (Exception ignored)
            {
            }
        }
        return false;
    }

    private boolean passedHooks(Location location, boolean useWorldGuard, boolean useFeudal, boolean useKingdoms,
                                boolean useFactions, boolean useGP, boolean useASkyBlock, boolean useResidence, boolean useTowny)
    {
        if (useWorldGuard && plugin.getConfig().getBoolean("hooks-options.world-guard.use-hook"))
        {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (container != null)
            {
                RegionManager regions = container.get(BukkitUtil.getLocalWorld(location.getWorld()));
                if (regions != null)
                {
                    List<String> regionList = new ArrayList<>(regions.getRegions().keySet());
                    for (int i = -1; ++i < regionList.size(); )
                    {
                        String r = regionList.get(i);
                        ProtectedRegion region = regions.getRegion(r);
                        if (region != null)
                        {
                            com.sk89q.worldedit.Vector loc = new com.sk89q.worldedit.Vector(location.getX(),
                                    location.getY(), location.getZ());
                            if (region.contains(loc) && !isInList("hooks-options.world-guard.region-whitelist", r))
                                return false;
                        }
                    }
                }
            }

                /*
                RegionContainer container = plugin.getWorldGuard().getRegionContainer();
            if (container != null)
            {
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
                            com.sk89q.worldedit.Vector loc = new com.sk89q.worldedit.Vector(location.getX(),
                                    location.getY(), location.getZ());
                            if (region.contains(loc) && !isInList("hooks-options.world-guard.region-whitelist", r))
                                return false;
                        }
                    }
                }
            }
                 */
        }

        if (useFeudal && plugin.getConfig().getBoolean("hooks-options.feudal.use-hook"))
        {
            Kingdom kingdom = Feudal.getAPI().getKingdom(location);
            return kingdom == null;
        }

        if (useKingdoms && plugin.getConfig().getBoolean("hooks-options.kingdoms.use-hook"))
        {
            if (plugin.getServer().getPluginManager().getPlugin("Kingdoms") != null)
            {
                Land land = new Land(new SimpleChunkLocation(location.getChunk()));
                if (land.getOwner() != null)
                    return false;
            }
        }

        if (useFactions && plugin.getConfig().getBoolean("hooks-options.factions.use-factions"))
        {
            if (plugin.getConfig().getBoolean("hooks-options.factions.factions-uuid"))
            {
                try
                {
                    FLocation fLocation = new FLocation(location);
                    com.massivecraft.factions.Faction factionAtLocation = Board.getInstance().getFactionAt(fLocation);
                    if (factionAtLocation != null
                            && !(factionAtLocation.isWilderness() || factionAtLocation.isWarZone()))
                        return false;
                } catch (Exception ignored)
                {
                }
            } else
            {
                try
                {
                    com.massivecraft.factions.entity.Faction factionAtLocation = BoardColl.get()
                            .getFactionAt(PS.valueOf(location));
                    if (factionAtLocation != null && !(factionAtLocation.getComparisonName()
                            .equals(FactionColl.get().getSafezone().getComparisonName())
                            || factionAtLocation.getComparisonName()
                            .equals(FactionColl.get().getWarzone().getComparisonName())))
                        return false;
                } catch (Exception ignored)
                {
                }
            }
        }

        if (useASkyBlock && plugin.getConfig().getBoolean("hooks-options.askyblock.use-askyblock"))
        {
            Plugin aSkyBlock = plugin.getServer().getPluginManager().getPlugin("ASkyBlock");
            if (aSkyBlock != null)
            {
                Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
                if (island != null)
                    return false;
            }
        }

        if (useGP && plugin.getConfig().getBoolean("hooks-options.grief-preventation.use-grief-preventation"))
        {
            Plugin griefPrevention = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
            if (griefPrevention != null)
            {
                Claim claimAtLocation = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
                if (claimAtLocation != null)
                    return false;
            }
        }

        if (useResidence && plugin.getConfig().getBoolean("hooks-options.residence.use-residence"))
        {
            Plugin residence = plugin.getServer().getPluginManager().getPlugin("Residence");
            if (residence != null)
            {
                ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(location);
                if (res != null)
                    return false;
            }
        }

        if (useTowny && plugin.getConfig().getBoolean("hooks-options.towny.use-towny"))
        {
            Plugin towny = plugin.getServer().getPluginManager().getPlugin("Towny");
            if (towny != null)
            {
                try
                {
                    Town town = WorldCoord.parseWorldCoord(location).getTownBlock().getTown();
                    if (town != null)
                        return false;
                } catch (Exception ignored)
                {
                }
            }
        }

        return true;
    }

    public ArrayList<Location> getPlacedLocationMemory()
    {
        return placedLocationMemory;
    }

    private void setPlacedLocationMemory(ArrayList<Location> placedLocationMemory)
    {
        this.placedLocationMemory = placedLocationMemory;
    }
}
