package com.oracle.truffle.js.nodes.decorators;

public class JSPlacement {
    private static final int PLACEMENT_STATIC = 1 << 0;
    private static final int PLACEMENT_PROTOTYPE = 1 << 1;
    private static final int PLACEMENT_OWN = 1 << 2;

    public static int getStatic() {
        return PLACEMENT_STATIC;
    }

    public static int getPrototype() {
        return PLACEMENT_PROTOTYPE;
    }

    public static int getOwn() {
        return PLACEMENT_OWN;
    }
}
