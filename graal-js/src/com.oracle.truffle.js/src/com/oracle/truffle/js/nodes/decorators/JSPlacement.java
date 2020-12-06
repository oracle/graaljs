package com.oracle.truffle.js.nodes.decorators;

public class JSPlacement {
    private static final int PLACEMENT_STATIC = 1 << 0;
    private static final int PLACEMENT_PROTOTYPE = 1 << 1;
    private static final int PLACEMENT_OWN = 1 << 2;

    private static final String STATIC_STRING = "static";
    private static final String PROTOTYPE_STRING = "prototype";
    private static final String OWN_STRING = "own";

    public static int getStatic() {
        return PLACEMENT_STATIC;
    }
    public static int getPrototype() {
        return PLACEMENT_PROTOTYPE;
    }
    public static int getOwn() {
        return PLACEMENT_OWN;
    }

    public static int fromString(String p) {
        if(p.equals(STATIC_STRING)){
            return getStatic();
        }
        if(p.equals(PROTOTYPE_STRING)) {
            return getPrototype();
        }
        if(p.equals(OWN_STRING)) {
            return getOwn();
        }
        return 0;
    }

    public static String toString(int placement) {
        if(isStatic(placement)) {
            return STATIC_STRING;
        }
        if(isPrototype(placement)) {
            return PROTOTYPE_STRING;
        }
        if(isOwn(placement)) {
            return OWN_STRING;
        }
        return null;
    }

    public static boolean isStatic(int placement) {
        return (placement & PLACEMENT_STATIC) != 0;
    }

    public static boolean isPrototype(int placement) {
        return (placement & PLACEMENT_PROTOTYPE) != 0;
    }

    public static boolean isOwn(int placement) {
        return (placement & PLACEMENT_OWN) != 0;
    }
}
