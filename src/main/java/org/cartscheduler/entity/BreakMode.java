package org.cartscheduler.entity;

/**
 * A participant's preference concerning a break in a proposal. The numeric
 * values are part of the REST contract and are intentionally explicit rather
 * than relying on an enum ordinal.
 */
public enum BreakMode {
    NO_BREAK((short) 0),
    REQUIRE_BREAK((short) 1),
    INDIFFERENT((short) 2);

    private final short code;

    BreakMode(short code) {
        this.code = code;
    }

    public short getCode() {
        return code;
    }

    public static BreakMode fromCode(int code) {
        for (BreakMode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported break mode: " + code);
    }
}
