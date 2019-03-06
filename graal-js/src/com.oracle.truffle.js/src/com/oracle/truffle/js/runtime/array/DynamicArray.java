/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.runtime.array;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

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

    @SuppressWarnings({"unchecked"})
    protected final <T extends ScriptArray> T setIntegrityLevel(int integrityLevel) {
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
    public boolean isExtensible() {
        return !(integrityLevel >= INTEGRITY_LEVEL_NOT_EXTENSIBLE);
    }

    @Override
    public boolean isLengthNotWritable() {
        return (integrityLevel & LENGTH_WRITABLE_MASK) != 0;
    }

    @Override
    public ScriptArray seal() {
        return isSealed() ? this : setIntegrityLevel(INTEGRITY_LEVEL_SEALED | (integrityLevel & ~INTEGRITY_LEVEL_MASK));
    }

    @Override
    public ScriptArray freeze() {
        return isFrozen() ? this : setIntegrityLevel(INTEGRITY_LEVEL_FROZEN_LENGTH_READONLY | (integrityLevel & ~INTEGRITY_LEVEL_MASK));
    }

    @Override
    public ScriptArray preventExtensions() {
        return !isExtensible() ? this : setIntegrityLevel(INTEGRITY_LEVEL_NOT_EXTENSIBLE | (integrityLevel & ~INTEGRITY_LEVEL_MASK));
    }

    @Override
    public ScriptArray setLengthNotWritable() {
        return isLengthNotWritable() ? this : setIntegrityLevel(LENGTH_NOT_WRITABLE | (integrityLevel & ~LENGTH_WRITABLE_MASK));
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
