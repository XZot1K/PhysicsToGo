package XZot1K.plugins.ptg.core.internals;

import XZot1K.plugins.ptg.PhysicsToGo;
import me.angeschossen.lands.api.integration.LandsIntegration;

public class LandsHook {
    private LandsIntegration landsAddon;

    public LandsHook(PhysicsToGo pluginInstance) {
        setLandsAddon(new LandsIntegration(pluginInstance, false));
        getLandsAddon().initialize();
        pluginInstance.setLandsHook(this);
    }

    public LandsIntegration getLandsAddon() {
        return landsAddon;
    }

    private void setLandsAddon(LandsIntegration landsAddon) {
        this.landsAddon = landsAddon;
    }
}
