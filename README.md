https://camo.githubusercontent.com/5c86724ea4aabd9559a1b8881621485d86c0c0aa7dfd8fc560742bc67637096a/68747470733a2f2f696d6775722e636f6d2f63436a69686e752e706e67

*Bugs to be fixed*
***

 - Blocks don't register properly on fastbreak, leads to holes.
 - Placing lava will make it "stick" to the world.
 - Players underground on regeneration gets suffocated,
    - Teleport players underground to top when regen process starts to avoid suffocation.
 - Blockstates get reset. When a block with a state (REDSTONE_LAMP) gets destroyed, it regenerates without any blockstate.

***
*Features to be added*

 - Particle effect on regeneration.
 - Rregen regions (WorldGuard/WorldEdit implementation)
 - Saving state of world/region
