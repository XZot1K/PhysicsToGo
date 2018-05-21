package XZot1K.plugins.ptg.core;

import XZot1K.plugins.ptg.PhysicsToGo;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Listeners implements Listener
{

    private static PhysicsToGo plugin;
    private final ArrayList<UUID> FallingSands;
    private final ArrayList<Location> blockLocationMemory;
    private final HashMap<Location, ItemStack[]> containers;
    private final HashMap<Location, String[]> signs;
    private List<Block> blockList;

    public Listeners(PhysicsToGo plugin)
    {
        Listeners.plugin = plugin;
        FallingSands = new ArrayList<>();
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplodeEntity(EntityExplodeEvent e)
    {
        if (plugin.getConfig().getBoolean("per-world-support"))
        {
            if (isPerWorld(e.getEntity().getWorld().getName()))
            {
                e.setCancelled(true);
                return;
            }
        }

        if (plugin.getConfig().getBoolean("use-worldguard"))
        {
            RegionContainer container = plugin.getWorldGuard().getRegionContainer();
            RegionManager regions = container.get(e.getEntity().getWorld());
            if (regions != null)
            {
                List<String> regionList = new ArrayList<>(regions.getRegions().keySet());
                for (int i = -1; ++i < regionList.size(); )
                {
                    String r = regionList.get(i);
                    List<String> onlyList = plugin.getConfig().getStringList("only-worldguard-regions");
                    if (!(onlyList.size() <= 0) && !isRegionOnly(r))
                    {
                        if (plugin.getConfig().getBoolean("cancel-event")) e.setCancelled(true);
                        return;
                    }

                    if (isRegionBlocked(r))
                    {
                        ProtectedRegion region = regions.getRegion(r);
                        if (region != null)
                        {
                            com.sk89q.worldedit.Vector location = new com.sk89q.worldedit.Vector(
                                    e.getEntity().getLocation().getX(),
                                    e.getEntity().getLocation().getY(),
                                    e.getEntity().getLocation().getZ());
                            if (region.contains(location))
                            {
                                if (plugin.getConfig().getBoolean("cancel-event")) e.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
        }

        if (!plugin.getConfig().getBoolean("block-damage"))
        {
            e.blockList().clear();
        } else
        {
            int delay = plugin.getConfig().getInt("regeneration-delay-ticks");
            List<Block> blocks = new ArrayList<>(e.blockList());
            for (int i = -1; ++i < blocks.size(); )
            {
                Block b = blocks.get(i);
                BlockState state = b.getState();
                plugin.savedStates.add(state);

                if (plugin.getConfig().getBoolean("auto-pickup-exploded"))
                {
                    ItemStack item = new ItemStack(b.getType(), 1, b.getData());
                    List<Entity> entities = new ArrayList<>(e.getEntity().getNearbyEntities(5, 5, 5));
                    for (int j = -1; ++j < entities.size(); )
                    {
                        Entity entity = entities.get(j);
                        if (entity instanceof Player)
                        {
                            if (((Player) entity).getInventory().firstEmpty() == -1)
                            {
                                entity.getWorld().dropItem(entity.getLocation(), item);
                            } else
                            {
                                ((Player) entity).getInventory().addItem(item);
                            }
                            break;
                        }
                    }
                }

                boolean dropItems = plugin.getConfig().getBoolean("block-drops");
                boolean containerDrops = plugin.getConfig().getBoolean("container-drops");
                boolean saveContainer = plugin.getConfig().getBoolean("save-container-contents");
                boolean saveSign = plugin.getConfig().getBoolean("save-sign-information");
                boolean convertTNT = plugin.getConfig().getBoolean("convert-tnt");
                if (!dropItems) blockList.add(b);

                float offx = -1.0F + (float) (Math.random() * plugin.getConfig().getInt("physics-offset-x")),
                        offy = -1.0F + (float) (Math.random() * plugin.getConfig().getInt("physics-offset-y")),
                        offz = -1.0F + (float) (Math.random() * plugin.getConfig().getInt("physics-offset-z"));

                if (b.getType() == Material.TNT)
                {
                    if (convertTNT)
                    {
                        b.setType(Material.AIR);
                        state.setType(Material.AIR);
                        Entity primed = b.getWorld().spawn(b.getLocation().add(0.0D, 1.0D, 0.0D), TNTPrimed.class);
                        ((TNTPrimed) primed).setFuseTicks(plugin.getConfig().getInt("convert-tnt-fuse"));
                    }
                }

                if (b.getState() instanceof InventoryHolder)
                {
                    InventoryHolder ih = (InventoryHolder) b.getState();
                    if (saveContainer) containers.put(b.getLocation(), ih.getInventory().getContents().clone());
                    if (!containerDrops) ih.getInventory().clear();
                } else if (b.getState() instanceof Sign)
                {
                    Sign sign = (Sign) b.getState();
                    if (saveSign) signs.put(b.getLocation(), sign.getLines());
                }

                if (plugin.getConfig().getBoolean("block-physics"))
                {
                    if (!isInPhysicsBlacklist(b.getType().name()))
                    {
                        try
                        {
                            FallingBlock fallingBlock = b.getWorld().spawnFallingBlock(b.getLocation(), b.getType(), b.getData());
                            fallingBlock.setDropItem(false);
                            fallingBlock.setVelocity(new Vector(offx, offy, offz));
                            FallingSands.add(fallingBlock.getUniqueId());
                            if ((plugin.getConfig().getBoolean("block-physics-particles")) && (fallingBlock.isOnGround()))
                                fallingBlock.getWorld().playEffect(fallingBlock.getLocation(), Effect.STEP_SOUND, b.getType());
                        } catch (IllegalArgumentException ignored) {}
                    }
                }

                if (plugin.getConfig().getBoolean("block-regeneration"))
                {
                    if (isRegenerationWorld(b.getWorld().getName()))
                    {
                        if (!isInRegenerationBlacklist(b.getType().name()))
                        {
                            if (blockLocationMemory.isEmpty() || !blockLocationMemory.contains(b.getLocation()))
                            {
                                blockLocationMemory.add(b.getLocation());
                            }
                            boolean regenEffect = plugin.getConfig().getBoolean("block-regeneration-effects");
                            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                            {
                                try
                                {
                                    state.update(true, false);
                                    state.update();

                                    if (b.getState() instanceof InventoryHolder)
                                    {
                                        InventoryHolder ih = (InventoryHolder) b.getState();
                                        if (!containers.isEmpty() && containers.containsKey(b.getLocation()))
                                        {
                                            ih.getInventory().setContents(containers.get(b.getLocation()));
                                            containers.remove(b.getLocation());
                                        }
                                    } else if (b.getState() instanceof Sign)
                                    {
                                        Sign sign = (Sign) b.getState();
                                        if (!signs.isEmpty() && signs.containsKey(b.getLocation()))
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

                                    if (regenEffect)
                                        b.getLocation().getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType(), 10);

                                    if (!blockLocationMemory.isEmpty() || blockLocationMemory.contains(b.getLocation()))
                                        blockLocationMemory.remove(b.getLocation());
                                } catch (IllegalArgumentException | IndexOutOfBoundsException ignored)
                                {
                                }
                            }, delay);
                        } else b.setType(Material.AIR);
                    }
                }

                delay += plugin.getConfig().getInt("regeneration-speed");
            }
        }

        blockList.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void EntityChangeBlockEvent(EntityChangeBlockEvent e)
    {
        if (plugin.getConfig().getBoolean("per-world-support"))
        {
            if (isPerWorld(e.getEntity().getWorld().getName()))
            {
                e.setCancelled(true);
                return;
            }
        }
        if (e.getEntity() instanceof FallingBlock)
        {
            if (FallingSands.contains(e.getEntity().getUniqueId()))
            {
                if (plugin.getConfig().getBoolean("block-physics-particles"))
                    e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType());
                if (!plugin.getConfig().getBoolean("block-form")) e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPhysics(BlockPhysicsEvent e)
    {
        if (blockLocationMemory.contains(e.getBlock().getLocation())) e.setCancelled(true);
    }

    private boolean isRegenerationWorld(String name)
    {
        List<String> worlds = plugin.getConfig().getStringList("regeneration-worlds");
        for (int i = -1; ++i < worlds.size(); )
        {
            String world = worlds.get(i);
            if (world.equalsIgnoreCase(name)) return true;
        }

        return false;
    }

    private static boolean isRegionBlocked(String name)
    {
        List<String> regions = plugin.getConfig().getStringList("blocked-worldguard-regions");
        for (int i = -1; ++i < regions.size(); )
        {
            String r = regions.get(i);
            if (r.equalsIgnoreCase(name)) return true;
        }

        return false;
    }

    private static boolean isRegionOnly(String name)
    {
        List<String> regions = plugin.getConfig().getStringList("only-worldguard-regions");
        for (int i = -1; ++i < regions.size(); )
        {
            String r = regions.get(i);
            if (r.equalsIgnoreCase(name))
                return true;
        }

        return false;
    }

    private boolean isPerWorld(String name)
    {
        List<String> worlds = new ArrayList<>(plugin.getConfig().getStringList("worlds"));
        for (int i = -1; ++i < worlds.size(); )
        {
            String world = worlds.get(i);
            if (world.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isInPhysicsBlacklist(String materialName)
    {
        List<String> materialNames = plugin.getConfig().getStringList("block-physic-blacklist");
        for (int i = -1; ++i < materialNames.size(); )
            if (materialNames.get(i).replace(" ", "_").replace("-", "_")
                    .equalsIgnoreCase(materialName.replace(" ", "_").replace("-", "_")))
                return true;

        return false;
    }

    private boolean isInRegenerationBlacklist(String materialName)
    {
        List<String> materialNames = plugin.getConfig().getStringList("block-regeneration-blacklist");
        for (int i = -1; ++i < materialNames.size(); )
            if (materialNames.get(i).replace(" ", "_").replace("-", "_")
                    .equalsIgnoreCase(materialName.replace(" ", "_").replace("-", "_")))
                return true;

        return false;
    }

}
