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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ArrayLengthNode.ArrayLengthWriteNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArray;

public abstract class JSSetLengthNode extends JavaScriptBaseNode {
    private final JSContext context;
    protected final boolean isStrict;

    protected JSSetLengthNode(JSContext context, boolean isStrict) {
        this.context = context;
        this.isStrict = isStrict;
    }

    public static JSSetLengthNode create(JSContext context, boolean strict) {
        return JSSetLengthNodeGen.create(context, strict);
    }

    public abstract Object execute(TruffleObject target, Object value);

    protected final WritePropertyNode createWritePropertyNode() {
        return NodeFactory.getInstance(context).createWriteProperty(null, JSArray.LENGTH, null, context, isStrict);
    }

    protected static boolean isArray(TruffleObject object) {
        // currently, must be fast array
        return JSArray.isJSFastArray(object);
    }

    @Specialization(guards = "isArray(object)")
    protected static int setArrayLength(DynamicObject object, int length,
                    @Cached("create(isStrict)") ArrayLengthWriteNode arrayLengthWriteNode) {
        arrayLengthWriteNode.executeVoid(object, length, isArray(object));
        return length;
    }

    @Specialization
    protected static int setIntLength(DynamicObject object, int length,
                    @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty) {
        setLengthProperty.executeIntWithValue(object, length);
        return length;
    }

    @Specialization(replaces = "setIntLength")
    protected static Object setLength(DynamicObject object, Object length,
                    @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty) {
        setLengthProperty.executeWithValue(object, length);
        return length;
    }

    @Specialization(guards = "!isDynamicObject(object)")
    protected static Object setLengthForeign(@SuppressWarnings("unused") TruffleObject object, Object length) {
        // there is no SET_SIZE message. Let's assume WRITE already has done the job
        return length;
    }
}
