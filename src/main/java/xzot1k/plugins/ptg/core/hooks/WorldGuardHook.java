/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public class WorldGuardHook {

    private static com.sk89q.worldguard.protection.flags.StateFlag PTG_ALLOW;
    private final com.sk89q.worldguard.bukkit.WorldGuardPlugin worldGuardPlugin;

    public WorldGuardHook() {
        com.sk89q.worldguard.protection.flags.registry.FlagRegistry registry = null;
        worldGuardPlugin = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst();
        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getFlagRegistry");
                registry = (com.sk89q.worldguard.protection.flags.registry.FlagRegistry) method.invoke(worldGuardPlugin);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();

        if (registry == null) return;
        try {
            com.sk89q.worldguard.protection.flags.StateFlag flag = new com.sk89q.worldguard.protection.flags.StateFlag("ptg-allow", false);
            registry.register(flag);
            PTG_ALLOW = flag;
        } catch (com.sk89q.worldguard.protection.flags.registry.FlagConflictException e) {
            com.sk89q.worldguard.protection.flags.Flag<?> existing = registry.get("ptg-allow");
            if (existing instanceof com.sk89q.worldguard.protection.flags.StateFlag)
                PTG_ALLOW = (com.sk89q.worldguard.protection.flags.StateFlag) existing;
        }
    }

    public boolean passedWorldGuardHook(Location location) {
        if (worldGuardPlugin == null) return true;

        com.sk89q.worldguard.protection.ApplicableRegionSet applicableRegionSet = null;
        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getRegionManager", World.class);

                com.sk89q.worldguard.protection.managers.RegionManager regionManager =
                        (com.sk89q.worldguard.protection.managers.RegionManager) method.invoke(worldGuardPlugin, location.getWorld());
                if (regionManager == null) return true;

                Method applicableRegionsMethod = worldGuardPlugin.getClass().getMethod("getApplicableRegions", Location.class);
                applicableRegionSet = (com.sk89q.worldguard.protection.ApplicableRegionSet) applicableRegionsMethod.invoke(regionManager, location);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            com.sk89q.worldguard.protection.regions.RegionQuery query =
                    com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location worldEditLocation = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            applicableRegionSet = query.getApplicableRegions(worldEditLocation);
        }

        if (applicableRegionSet == null) return true;
        System.out.println("test 1");

        Set<com.sk89q.worldguard.protection.regions.ProtectedRegion> regions = applicableRegionSet.getRegions();
        if (regions.isEmpty()) return true;

        System.out.println("test 2");

        return regions.parallelStream().anyMatch(protectedRegion -> (protectedRegion.getFlags().containsKey(PTG_ALLOW)
                && (protectedRegion.getFlags().get(PTG_ALLOW) instanceof Boolean && ((boolean) protectedRegion.getFlags().get(PTG_ALLOW)))));
    }
}