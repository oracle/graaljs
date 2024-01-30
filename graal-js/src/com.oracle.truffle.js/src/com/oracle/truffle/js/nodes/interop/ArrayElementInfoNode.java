/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBase;

/**
 * This node implements the {@code isArrayElement*} messages for arrays.
 */
@GenerateUncached
public abstract class ArrayElementInfoNode extends JavaScriptBaseNode {
    public static final int READABLE = 1 << 0;
    public static final int MODIFIABLE = 1 << 1;
    public static final int INSERTABLE = 1 << 2;
    public static final int REMOVABLE = 1 << 3;
    public static final int WRITABLE = INSERTABLE | MODIFIABLE;

    ArrayElementInfoNode() {
    }

    /**
     * Returns:
     * <ul>
     * <li>{@link TriState#TRUE} if this array element is readable/modifiable/insertable/removable.
     * <li>{@link TriState#FALSE} if the operation cannot be performed for this array element.
     * <li>{@link TriState#UNDEFINED} if the message is not supported for this array instance.
     * </ul>
     */
    public abstract TriState execute(JSArrayBase receiver, long index, int query);

    public final boolean executeBoolean(JSArrayBase receiver, long index, int query) {
        return execute(receiver, index, query) == TriState.TRUE;
    }

    public final void executeCheck(JSArrayBase receiver, long index, int query) throws UnsupportedMessageException, InvalidArrayIndexException {
        TriState result = execute(receiver, index, query);
        if (result != TriState.TRUE) {
            if (result == TriState.UNDEFINED) {
                throw UnsupportedMessageException.create();
            } else {
                throw InvalidArrayIndexException.create(index);
            }
        }
    }

    @Specialization(guards = {"arrayType.isInstance(target.getArrayType())"}, limit = "5")
    static TriState doCached(JSArrayBase target, long index, int query,
                    @Cached(value = "target.getArrayType()") ScriptArray arrayType) {
        if ((query & (MODIFIABLE | INSERTABLE | REMOVABLE)) != 0) {
            if (arrayType.isFrozen()) {
                // UnsupportedMessageException
                return TriState.UNDEFINED;
            }
        }
        if (index >= 0 && index < JSAbstractArray.arrayGetLength(target)) {
            if ((query & READABLE) != 0) {
                return TriState.TRUE;
            }
            if ((query & MODIFIABLE) != 0) {
                assert !arrayType.isFrozen();
                return TriState.TRUE;
            }
            if ((query & REMOVABLE) != 0 && !arrayType.isSealed() && !arrayType.isLengthNotWritable()) {
                return TriState.TRUE;
            }
            return TriState.FALSE;
        } else {
            if ((query & INSERTABLE) != 0 && JSRuntime.isArrayIndex(index) && !arrayType.isSealed() && !arrayType.isLengthNotWritable()) {
                return TriState.TRUE;
            }
            return TriState.FALSE;
        }
    }

    @Specialization(replaces = "doCached")
    static TriState doUncached(JSArrayBase target, long index, int query) {
        return doCached(target, index, query, target.getArrayType());
    }
}
