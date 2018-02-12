/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.ValueType;

@ValueType
public final class Completion {
    public enum Type {
        Normal,
        Return,
        Throw,
    }

    static final int NORMAL = Type.Normal.ordinal();
    static final int RETURN = Type.Return.ordinal();
    static final int THROW = Type.Throw.ordinal();

    final int type;
    final Object value;

    Completion(int completionType, Object completionValue) {
        this.type = completionType;
        this.value = completionValue;
    }

    public Object getValue() {
        return value;
    }

    public boolean isNormal() {
        return type == NORMAL;
    }

    public boolean isAbruptCompletion() {
        return type != NORMAL;
    }

    public boolean isReturn() {
        return type == RETURN;
    }

    public boolean isThrow() {
        return type == THROW;
    }

    public static Completion forNormal(Object value) {
        return new Completion(NORMAL, value);
    }

    public static Completion forReturn(Object value) {
        return new Completion(RETURN, value);
    }

    public static Completion forThrow(Object value) {
        return new Completion(THROW, value);
    }

    public static Completion create(Type type, Object value) {
        return new Completion(type.ordinal(), value);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "Completion[type=" + Type.values()[type].name() + ", value=" + value + "]";
    }
}
