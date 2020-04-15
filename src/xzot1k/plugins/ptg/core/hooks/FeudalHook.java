/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import org.bukkit.plugin.Plugin;
import us.forseth11.feudal.api.FeudalAPI;

public class FeudalHook {

    private FeudalAPI feudalAPI;

    public FeudalHook(Plugin feudal) {
        setFeudalAPI((FeudalAPI) feudal);
    }

    public FeudalAPI getFeudalAPI() {
        return feudalAPI;
    }

    private void setFeudalAPI(FeudalAPI feudalAPI) {
        this.feudalAPI = feudalAPI;
    }
}
