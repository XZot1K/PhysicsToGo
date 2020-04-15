/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import de.browniecodez.feudal.main.Main;
import us.forseth11.feudal.api.FeudalAPI;

public class FeudalHook {

    private FeudalAPI feudalAPI;

    public FeudalHook() {
        setFeudalAPI(Main.getAPI());
    }

    public FeudalAPI getFeudalAPI() {
        return feudalAPI;
    }

    private void setFeudalAPI(FeudalAPI feudalAPI) {
        this.feudalAPI = feudalAPI;
    }
}
