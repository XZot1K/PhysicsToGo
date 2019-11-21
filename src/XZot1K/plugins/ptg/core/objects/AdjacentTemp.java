package XZot1K.plugins.ptg.core.objects;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class AdjacentTemp {
    private boolean hasTreeBlockOrSimilar;
    private List<Block> foundAdjacentTreeBlocks;

    public AdjacentTemp() {
        setHasTreeBlockOrSimilar(false);
        setFoundAdjacentTreeBlocks(new ArrayList<>());
    }

    public boolean hasTreeBlockOrSimilar() {
        return hasTreeBlockOrSimilar;
    }

    public void setHasTreeBlockOrSimilar(boolean hasTreeBlockOrSimilar) {
        this.hasTreeBlockOrSimilar = hasTreeBlockOrSimilar;
    }

    public List<Block> getFoundAdjacentTreeBlocks() {
        return foundAdjacentTreeBlocks;
    }

    private void setFoundAdjacentTreeBlocks(List<Block> foundAdjacentTreeBlocks) {
        this.foundAdjacentTreeBlocks = foundAdjacentTreeBlocks;
    }
}
