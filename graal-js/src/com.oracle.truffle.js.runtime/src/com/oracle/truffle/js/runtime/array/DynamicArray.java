/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.array;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.object.DynamicObject;

/**
 * Common base class for all dynamic (i.e., non-typed) JavaScript arrays. Encapsulates information
 * about whether the array is non-extensible, sealed, frozen, and whether its length is writable.
 */
public abstract class DynamicArray extends ScriptArray {
    protected static final class DynamicArrayCache {
        @CompilationFinal(dimensions = 1) final DynamicArray[] withIntegrityLevel = new DynamicArray[INTEGRITY_LEVELS];
    }

    protected static final int INTEGRITY_LEVEL_NONE = 0;
    protected static final int INTEGRITY_LEVEL_NONE_LENGTH_READONLY = 1;
    protected static final int INTEGRITY_LEVEL_NOT_EXTENSIBLE = 2;
    protected static final int INTEGRITY_LEVEL_NOT_EXTENSIBLE_LENGTH_READONLY = 3;
    protected static final int INTEGRITY_LEVEL_SEALED = 4;
    protected static final int INTEGRITY_LEVEL_SEALED_LENGTH_READONLY = 5;
    protected static final int INTEGRITY_LEVEL_FROZEN = 6;
    protected static final int INTEGRITY_LEVEL_FROZEN_LENGTH_READONLY = 7;
    protected static final int INTEGRITY_LEVELS = 8;
    protected static final int INTEGRITY_LEVEL_MASK = 6;
    protected static final int LENGTH_WRITABLE_MASK = 1;
    protected static final int LENGTH_NOT_WRITABLE = 1;

    protected final int integrityLevel;
    protected final DynamicArrayCache cache;

    protected DynamicArray(int integrityLevel, DynamicArrayCache cache) {
        CompilerAsserts.neverPartOfCompilation();
        this.integrityLevel = integrityLevel;
        this.cache = cache;
    }

    protected static DynamicArrayCache createCache() {
        return new DynamicArrayCache();
    }

    protected abstract DynamicArray withIntegrityLevel(int newIntegrityLevel);

    @SuppressWarnings({"unchecked", "unused"})
    protected final <T extends ScriptArray> T setIntegrityLevel(DynamicObject object, int integrityLevel) {
        if (this.integrityLevel == integrityLevel) {
            return (T) this;
        } else {
            CompilerAsserts.partialEvaluationConstant(cache);
            DynamicArray cached = cache.withIntegrityLevel[integrityLevel];
            if (cached == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                DynamicArray newArray = withIntegrityLevel(integrityLevel);
                cache.withIntegrityLevel[integrityLevel] = newArray;
                assert newArray.getClass() == this.getClass();
                return (T) newArray;
            } else {
                return (T) cached;
            }
        }
    }

    @Override
    public boolean isSealed() {
        return integrityLevel >= INTEGRITY_LEVEL_SEALED;
    }

    @Override
    public boolean isFrozen() {
        return integrityLevel >= INTEGRITY_LEVEL_FROZEN;
    }

    @Override
    public boolean isLengthNotWritable() {
        return (integrityLevel & LENGTH_WRITABLE_MASK) != 0;
    }

    @Override
    public ScriptArray seal(DynamicObject object) {
        return isSealed() ? this : setIntegrityLevel(object, INTEGRITY_LEVEL_SEALED | (integrityLevel & ~INTEGRITY_LEVEL_MASK));
    }

    @Override
    public ScriptArray freeze(DynamicObject object) {
        return isFrozen() ? this : setIntegrityLevel(object, INTEGRITY_LEVEL_FROZEN | (integrityLevel & ~INTEGRITY_LEVEL_MASK));
    }

    @Override
    public ScriptArray setLengthNotWritable(DynamicObject object) {
        return isLengthNotWritable() ? this : setIntegrityLevel(object, LENGTH_NOT_WRITABLE | (integrityLevel & ~LENGTH_WRITABLE_MASK));
    }

    @Override
    public final boolean isStatelessType() {
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + "[integrityLevel=" + integrityLevel + "]";
    }
}
