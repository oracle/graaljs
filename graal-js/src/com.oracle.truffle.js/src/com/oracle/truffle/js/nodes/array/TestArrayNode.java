/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

public abstract class TestArrayNode extends JavaScriptBaseNode {

    protected enum Test {
        HasHoles,
        HasHolesOrUnused,
        IsSealed,
    }

    protected static final int MAX_TYPE_COUNT = 4;

    protected final Test test;

    protected TestArrayNode(Test test) {
        this.test = test;
    }

    protected static ScriptArray getArrayType(JSDynamicObject target) {
        return JSObject.getArray(target);
    }

    protected static TestArrayNode create(Test test) {
        return TestArrayNodeGen.create(test);
    }

    @NeverDefault
    public static TestArrayNode createHasHoles() {
        return create(Test.HasHoles);
    }

    @NeverDefault
    public static TestArrayNode createHasHolesOrUnused() {
        return create(Test.HasHolesOrUnused);
    }

    @NeverDefault
    public static TestArrayNode createIsSealed() {
        return create(Test.IsSealed);
    }

    public abstract boolean executeBoolean(JSDynamicObject target);

    @Specialization(guards = {"arrayType.isInstance(getArrayType(target))"}, limit = "MAX_TYPE_COUNT")
    protected final boolean doCached(JSDynamicObject target,
                    @Cached("getArrayType(target)") ScriptArray arrayType) {
        if (test == Test.HasHoles) {
            return arrayType.hasHoles(target);
        } else if (test == Test.HasHolesOrUnused) {
            return arrayType.hasHolesOrUnused(target);
        } else if (test == Test.IsSealed) {
            return arrayType.isSealed();
        } else {
            throw Errors.shouldNotReachHere();
        }
    }

    @Specialization(replaces = "doCached")
    protected final boolean doUncached(JSDynamicObject target) {
        ScriptArray arrayType = getArrayType(target);
        return doCached(target, arrayType);
    }
}
