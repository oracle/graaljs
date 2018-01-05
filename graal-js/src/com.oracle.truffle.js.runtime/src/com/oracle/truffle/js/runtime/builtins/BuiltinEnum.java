/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

public interface BuiltinEnum<E extends Enum<? extends BuiltinEnum<E>>> {
    @SuppressWarnings("unchecked")
    default E asEnum() {
        return (E) this;
    }

    default String getName() {
        return stripName(asEnum().name());
    }

    default Object getKey() {
        return getName();
    }

    default boolean isConstructor() {
        return false;
    }

    default boolean isNewTargetConstructor() {
        return false;
    }

    int getLength();

    default boolean isEnabled() {
        return true;
    }

    default boolean isAOTSupported() {
        return true;
    }

    default int getECMAScriptVersion() {
        return 5;
    }

    default boolean isAnnexB() {
        return false;
    }

    default boolean isWritable() {
        return true;
    }

    default boolean isConfigurable() {
        return true;
    }

    default boolean isEnumerable() {
        return false;
    }

    static String stripName(String name) {
        return name.endsWith("_") ? name.substring(0, name.length() - 1) : name;
    }
}
