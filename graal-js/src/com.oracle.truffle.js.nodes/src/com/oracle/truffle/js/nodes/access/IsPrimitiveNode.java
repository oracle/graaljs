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
package com.oracle.truffle.js.nodes.access;

import static com.oracle.truffle.js.nodes.JSGuards.isBoolean;
import static com.oracle.truffle.js.nodes.JSGuards.isNumber;
import static com.oracle.truffle.js.nodes.JSGuards.isString;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;

public abstract class IsPrimitiveNode extends JavaScriptBaseNode {

    protected static final int MAX_CLASSES = 3;

    public abstract boolean executeBoolean(Object operand);

    @Specialization(guards = {"isJSNull(operand)"})
    protected static boolean doNull(@SuppressWarnings("unused") DynamicObject operand) {
        return true;
    }

    @Specialization(guards = {"isUndefined(operand)"})
    protected static boolean doUndefined(@SuppressWarnings("unused") DynamicObject operand) {
        return true;
    }

    @Specialization
    protected static boolean doBigInt(@SuppressWarnings("unused") BigInt operand) {
        return true;
    }

    @Specialization(guards = {"isNullOrUndefined(operand)"}, replaces = {"doNull", "doUndefined"})
    protected static boolean doNullOrUndefined(@SuppressWarnings("unused") DynamicObject operand) {
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"operand != null", "cachedClass != null", "cachedClass == operand.getClass()"}, limit = "MAX_CLASSES")
    protected static boolean doCached(Object operand,
                    @Cached("getNonDynamicObjectClass(operand)") Class<?> cachedClass,
                    @Cached("doGeneric(operand)") boolean cachedResult) {
        return cachedResult;
    }

    @Specialization(guards = {"isJSObject(operand)"})
    protected static boolean doIsObject(@SuppressWarnings("unused") DynamicObject operand) {
        return false;
    }

    @Specialization(replaces = {"doNull", "doUndefined", "doNullOrUndefined", "doCached", "doIsObject"})
    protected static boolean doGeneric(Object operand) {
        if (isNumber(operand) || isBoolean(operand) || isString(operand) || operand instanceof Symbol) {
            return true;
        } else if (JSObject.isDynamicObject(operand) && !JSGuards.isJSObject((DynamicObject) operand)) {
            return true;
        }
        return false;
    }

    public static IsPrimitiveNode create() {
        return IsPrimitiveNodeGen.create();
    }
}
