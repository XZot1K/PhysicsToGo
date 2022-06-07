/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import me.angeschossen.lands.api.integration.LandsIntegration;
import xzot1k.plugins.ptg.PhysicsToGo;

public class LandsHook {

    private LandsIntegration landsIntegration;

    public LandsHook(PhysicsToGo pluginInstance) {
        setLandsIntegration(new LandsIntegration(pluginInstance));
    }

    public LandsIntegration getLandsIntegration() {
        return landsIntegration;
    }

    private void setLandsIntegration(LandsIntegration landsIntegration) {
        this.landsIntegration = landsIntegration;
    }
}