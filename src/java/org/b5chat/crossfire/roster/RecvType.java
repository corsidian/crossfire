package org.b5chat.crossfire.roster;

import org.b5chat.util.IntEnum;

public class RecvType extends IntEnum {
    protected RecvType(String name, int value) {
        super(name, value);
        register(this);
    }

    public static RecvType getTypeFromInt(int value) {
        return (RecvType)getEnumFromInt(RecvType.class, value);
    }
}

