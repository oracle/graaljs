/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.array;

import java.util.Objects;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.array.ScriptArray;

/**
 * Deletes a range of indices from a JS array. Does not shrink the array.
 *
 * Used by {@code Array.prototype.sort}.
 */
public abstract class JSArrayDeleteRangeNode extends JavaScriptBaseNode {

    protected final JSContext context;
    protected final boolean orThrow;

    protected JSArrayDeleteRangeNode(JSContext context, boolean orThrow) {
        this.context = Objects.requireNonNull(context);
        this.orThrow = orThrow;
    }

    public static JSArrayDeleteRangeNode create(JSContext context, boolean orThrow) {
        return JSArrayDeleteRangeNodeGen.create(context, orThrow);
    }

    public abstract void execute(DynamicObject array, ScriptArray arrayType, long start, long end);

    @Specialization(guards = {"cachedArrayType.isInstance(arrayType)", "!cachedArrayType.isHolesType()"}, limit = "5")
    protected void denseArray(DynamicObject array, @SuppressWarnings("unused") ScriptArray arrayType, long start, long end,
                    @Cached("arrayType") @SuppressWarnings("unused") ScriptArray cachedArrayType,
                    @Cached("create(orThrow, context)") DeletePropertyNode deletePropertyNode) {
        for (long i = start; i < end; i++) {
            deletePropertyNode.executeEvaluated(array, i);
        }
    }

    @Specialization(guards = {"cachedArrayType.isInstance(arrayType)", "cachedArrayType.isHolesType()"}, limit = "5")
    protected void sparseArray(DynamicObject array, @SuppressWarnings("unused") ScriptArray arrayType, long start, long end,
                    @Cached("arrayType") @SuppressWarnings("unused") ScriptArray cachedArrayType,
                    @Cached("create(orThrow, context)") DeletePropertyNode deletePropertyNode,
                    @Cached("create(context)") JSArrayNextElementIndexNode nextElementIndexNode) {
        long pos = start;
        while (pos < end) {
            deletePropertyNode.executeEvaluated(array, pos);
            pos = nextElementIndexNode.executeLong(array, pos, end);
        }
    }

    @Specialization(replaces = {"denseArray", "sparseArray"})
    protected void doUncached(DynamicObject array, ScriptArray arrayType, long start, long end,
                    @Cached("create(orThrow, context)") DeletePropertyNode deletePropertyNode,
                    @Cached("create(context)") JSArrayNextElementIndexNode nextElementIndexNode) {
        if (arrayType.isHolesType()) {
            sparseArray(array, arrayType, start, end, arrayType, deletePropertyNode, nextElementIndexNode);
        } else {
            denseArray(array, arrayType, start, end, arrayType, deletePropertyNode);
        }
    }
}
