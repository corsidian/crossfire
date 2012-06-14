package org.b5chat.crossfire.roster;

import org.b5chat.util.IntEnum;

public class SubType extends IntEnum {
    protected SubType(String name, int value) {
        super(name, value);
        register(this);
    }

    public static SubType getTypeFromInt(int value) {
        return (SubType)getEnumFromInt(SubType.class, value);
    }
}
