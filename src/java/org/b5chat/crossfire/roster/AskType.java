package org.b5chat.crossfire.roster;

import org.b5chat.util.IntEnum;

public class AskType extends IntEnum {
    protected AskType(String name, int value) {
        super(name, value);
        register(this);
    }

    public static AskType getTypeFromInt(int value) {
        return (AskType)getEnumFromInt(AskType.class, value);
    }
}