/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.objects;

import org.bukkit.Material;
import xzot1k.plugins.ptg.core.enums.TreeType;

public class SaplingData {
    private byte dataValue;
    private Material material;

    public SaplingData(TreeType treeType) {

        Material material = Material.getMaterial(treeType.name() + "_SAPLING");
        if (material == null) material = Material.getMaterial("SAPLING");
        setMaterial(material);

        setDataValue((getMaterial() != null && !getMaterial().name().startsWith(treeType.name())) ? treeType.getDataValue() : -1);
    }

    public Material getMaterial() {
        return material;
    }

    private void setMaterial(Material material) {
        this.material = material;
    }

    public byte getDataValue() {
        return dataValue;
    }

    private void setDataValue(byte dataValue) {
        this.dataValue = dataValue;
    }
}
