/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;


import me.invertmc.feudal.api.FeudalAPI;
import me.invertmc.feudal.core.Feudal;

public class FeudalHook {

    private FeudalAPI feudalAPI;

    public FeudalHook() {
        setFeudalAPI(Feudal.getAPI());
    }

    public FeudalAPI getFeudalAPI() {
        return feudalAPI;
    }

    private void setFeudalAPI(FeudalAPI feudalAPI) {
        this.feudalAPI = feudalAPI;
    }
}
