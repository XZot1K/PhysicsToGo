package XZot1K.plugins.ptg.core.internals;

import XZot1K.plugins.ptg.PhysicsToGo;
import me.angeschossen.lands.api.landsaddons.LandsAddon;

public class LandsHook
{
    private LandsAddon landsAddon;

    public LandsHook(PhysicsToGo pluginInstance)
    {
        setLandsAddon(new LandsAddon(pluginInstance, false));
        getLandsAddon().initialize();
        pluginInstance.setLandsHook(this);
    }

    public LandsAddon getLandsAddon()
    {
        return landsAddon;
    }

    private void setLandsAddon(LandsAddon landsAddon)
    {
        this.landsAddon = landsAddon;
    }
}
