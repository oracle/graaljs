package com.oracle.truffle.js.nodes.decorators;

public class JSKind {
    private static final int KIND_METHOD = 1 << 0;
    private static final int KIND_ACCESSOR = 1 << 1;
    private static final int KIND_FIELD = 1 << 2;
    private static final int KIND_HOOK = 1 << 3;
    private static final int KIND_CLASS = 1 << 4;

    private static final String METHOD_STRING = "method";
    private static final String ACCESSOR_STRING = "accessor";
    private static final String FIELD_STRING = "field";
    private static final String HOOK_STRING = "hook";
    private static final String CLASS_STRING = "class";

    public static int getKindMethod() { return KIND_METHOD; }
    public static int getKindAccessor() { return KIND_ACCESSOR; }
    public static int getKindField() { return KIND_FIELD; }
    public static int getKindHook() { return KIND_HOOK; }
    public static int getKindClass() { return KIND_CLASS; }

    public static int fromString(String k) {
        if(k.equals(METHOD_STRING)) {
            return getKindMethod();
        }
        if(k.equals(ACCESSOR_STRING)) {
            return  getKindAccessor();
        }
        if(k.equals(FIELD_STRING)) {
            return getKindField();
        }
        if(k.equals(HOOK_STRING)) {
            return getKindHook();
        }
        if(k.equals(CLASS_STRING)) {
            return getKindClass();
        }
        return 0;
    }

    public static String toString(int kind) {
        if(isMethod(kind)) {
            return METHOD_STRING;
        }
        if(isAccessor(kind)) {
            return ACCESSOR_STRING;
        }
        if(isField(kind)) {
            return FIELD_STRING;
        }
        if(isHook(kind)){
            return HOOK_STRING;
        }
        if(isClass(kind)) {
            return CLASS_STRING;
        }
        return null;
    }

    public static boolean isMethod(int kind) {
        return (kind & KIND_METHOD) != 0;
    }

    public static boolean isAccessor(int kind) {
        return (kind & KIND_ACCESSOR) != 0;
    }

    public static boolean isField(int kind) {
        return (kind & KIND_FIELD) != 0;
    }

    public static boolean isHook(int kind) {
        return (kind & KIND_HOOK) != 0;
    }

    public static boolean isClass(int kind) {
        return (kind & KIND_CLASS) != 0;
    }
}
