/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.enums;

public enum TreeType {
    OAK((byte) 0), DARK_OAK((byte) 0), ACACIA((byte) 0), BIRCH((byte) 0), JUNGLE((byte) 0);

    private byte dataValue;

    TreeType(byte dataValue) {
        setDataValue(dataValue);
    }

    public byte getDataValue() {
        return dataValue;
    }

    private void setDataValue(byte dataValue) {
        this.dataValue = dataValue;
    }
}
