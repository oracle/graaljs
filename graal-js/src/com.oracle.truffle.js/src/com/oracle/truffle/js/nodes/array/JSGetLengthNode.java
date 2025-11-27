/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.array.ArrayLengthNode.ArrayLengthReadNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToUInt32Node;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public abstract class JSGetLengthNode extends JavaScriptBaseNode {

    private final JSContext context;
    /** Apply ES6 ToLength. */
    private final boolean toLength;
    @Child private JSToUInt32Node toUInt32Node;
    @Child private JSToLengthNode toLengthNode;

    protected JSGetLengthNode(JSContext context) {
        this.context = context;
        this.toLength = context.getEcmaScriptVersion() >= 6;
    }

    @NeverDefault
    public static JSGetLengthNode create(JSContext context) {
        return JSGetLengthNodeGen.create(context);
    }

    public abstract Object execute(Object value);

    public final long executeLong(Object value) {
        return toLengthLong(execute(value));
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    public int getArrayLengthInt(JSArrayObject target,
                    @Cached @Shared ArrayLengthReadNode arrayLengthReadNode) throws UnexpectedResultException {
        return arrayLengthReadNode.executeInt(target);
    }

    @Specialization(replaces = "getArrayLengthInt")
    public double getArrayLength(JSArrayObject target,
                    @Cached @Shared ArrayLengthReadNode arrayLengthReadNode) {
        return arrayLengthReadNode.executeDouble(target);
    }

    @Specialization(guards = "!isJSArray(target)")
    public double getNonArrayLength(JSDynamicObject target,
                    @Cached("createLengthProperty()") PropertyGetNode getLengthPropertyNode) {
        return toLengthDouble(getLengthPropertyNode.getValue(target));
    }

    @InliningCutoff
    @Specialization(guards = "!isJSDynamicObject(target)", limit = "3")
    public double getLengthForeign(Object target,
                    @CachedLibrary("target") InteropLibrary interop,
                    @Cached ImportValueNode importValueNode,
                    @Cached TruffleString.ToJavaStringNode toJavaStringNode) {
        if (interop.hasArrayElements(target)) {
            return JSInteropUtil.getArraySize(target, interop, this);
        } else {
            return toLengthDouble(JSInteropUtil.readMemberOrDefault(target, JSAbstractArray.LENGTH, 0, interop, importValueNode, toJavaStringNode));
        }
    }

    @NeverDefault
    protected PropertyGetNode createLengthProperty() {
        return PropertyGetNode.create(JSArray.LENGTH, context);
    }

    private double toUInt32Double(Object target) {
        return JSRuntime.doubleValue(getUInt32Node().executeNumber(target));
    }

    private long toUInt32Long(Object target) {
        return JSRuntime.longValue(getUInt32Node().executeNumber(target));
    }

    private double toLengthDouble(Object target) {
        if (toLength) {
            return getToLengthNode().executeLong(target);
        } else {
            return toUInt32Double(target);
        }
    }

    private long toLengthLong(Object target) {
        if (toLength) {
            return getToLengthNode().executeLong(target);
        } else {
            return toUInt32Long(target);
        }
    }

    private JSToLengthNode getToLengthNode() {
        if (toLengthNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toLengthNode = insert(JSToLengthNode.create());
        }
        return toLengthNode;
    }

    private JSToUInt32Node getUInt32Node() {
        if (toUInt32Node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toUInt32Node = insert(JSToUInt32Node.create());
        }
        return toUInt32Node;
    }
}
