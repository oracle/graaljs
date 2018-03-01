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

    final Type type;
    final Object value;

    Completion(Type completionType, Object completionValue) {
        this.type = completionType;
        this.value = completionValue;
    }

    public Object getValue() {
        return value;
    }

    public boolean isNormal() {
        return type == Type.Normal;
    }

    public boolean isAbruptCompletion() {
        return type != Type.Normal;
    }

    public boolean isReturn() {
        return type == Type.Return;
    }

    public boolean isThrow() {
        return type == Type.Throw;
    }

    public static Completion forNormal(Object value) {
        return new Completion(Type.Normal, value);
    }

    public static Completion forReturn(Object value) {
        return new Completion(Type.Return, value);
    }

    public static Completion forThrow(Object value) {
        return new Completion(Type.Throw, value);
    }

    public static Completion create(Type type, Object value) {
        return new Completion(type, value);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "Completion[type=" + type + ", value=" + value + "]";
    }
}
