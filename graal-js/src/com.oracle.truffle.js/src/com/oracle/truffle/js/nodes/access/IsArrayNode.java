/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Non-standard IsArray. Checks for array(-like) exotic objects.
 *
 * @see JSIsArrayNode
 */
@ImportStatic({IsArrayNode.Kind.class, CompilerDirectives.class})
public abstract class IsArrayNode extends JavaScriptBaseNode {

    protected static final int MAX_SHAPE_COUNT = 1;

    final Kind kind;

    protected enum Kind {
        /** Fast Array, Arguments, or Typed Array exotic object. */
        FastOrTypedArray,
        /** Fast Array exotic object. */
        FastArray,
        /** Array exotic object. */
        Array,
        /** Array, Typed Array, or Arguments exotic object, or Object.prototype. */
        AnyArray,
    }

    protected IsArrayNode(Kind kind) {
        this.kind = kind;
    }

    public abstract boolean execute(Object operand);

    @Specialization(guards = {"kind == Array || kind == AnyArray"})
    protected final boolean doJSArray(JSArrayObject object) {
        return checkResult(object, true);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"kind == FastArray || kind == FastOrTypedArray", "object.getShape() == cachedShape"}, limit = "MAX_SHAPE_COUNT")
    protected final boolean doJSFastArrayShape(JSArrayObject object,
                    @Cached("object.getShape()") Shape cachedShape,
                    @Cached("isArray(object)") boolean cachedResult) {
        return checkResult(object, cachedResult);
    }

    @Specialization(guards = {"kind == FastArray || kind == FastOrTypedArray"}, replaces = "doJSFastArrayShape")
    protected final boolean doJSFastArray(JSArrayObject object) {
        return checkResult(object, JSArray.isJSFastArray(object));
    }

    @Specialization(guards = {"kind == AnyArray || kind == FastOrTypedArray"})
    protected final boolean doJSTypedArray(JSTypedArrayObject object) {
        return checkResult(object, true);
    }

    @Specialization(guards = {"kind == AnyArray || kind == FastOrTypedArray", "isJSArgumentsObject(object)"})
    protected final boolean doJSArgumentsObject(JSArgumentsObject object) {
        return checkResult(object, kind == Kind.AnyArray || (kind == Kind.FastOrTypedArray && JSArgumentsArray.isJSFastArgumentsObject(object)));
    }

    @Specialization(guards = {"kind == AnyArray", "isJSObjectPrototype(object)"})
    protected final boolean doJSObjectPrototype(Object object) {
        return checkResult(object, true);
    }

    protected final boolean isArray(Object object) {
        if (kind == Kind.FastOrTypedArray) {
            return JSArray.isJSFastArray(object) || JSArgumentsArray.isJSFastArgumentsObject(object) || JSArrayBufferView.isJSArrayBufferView(object);
        } else if (kind == Kind.FastArray) {
            return JSArray.isJSFastArray(object);
        } else if (kind == Kind.Array) {
            return JSArray.isJSArray(object);
        } else {
            assert kind == Kind.AnyArray;
            return JSObject.hasArray(object);
        }
    }

    protected final boolean checkResult(Object object, boolean result) {
        assert isArray(object) == result;
        return result;
    }

    @Specialization(guards = {"kind == Array || kind == FastArray", "!isJSArray(object)"})
    protected final boolean doNotJSArray(Object object) {
        return checkResult(object, false);
    }

    @Specialization(guards = {"isExact(object, cachedClass)"}, limit = "1")
    protected final boolean doOtherCached(Object object,
                    @Cached("object.getClass()") @SuppressWarnings("unused") Class<?> cachedClass) {
        return checkResult(object, false);
    }

    @Specialization(replaces = {"doOtherCached", "doJSArray", "doJSFastArray", "doJSTypedArray", "doJSArgumentsObject", "doJSObjectPrototype"})
    protected final boolean doOther(Object object) {
        return checkResult(object, isArray(object));
    }

    public static IsArrayNode createIsAnyArray() {
        return IsArrayNodeGen.create(Kind.AnyArray);
    }

    public static IsArrayNode createIsArray() {
        return IsArrayNodeGen.create(Kind.Array);
    }

    public static IsArrayNode createIsFastArray() {
        return IsArrayNodeGen.create(Kind.FastArray);
    }

    public static IsArrayNode createIsFastOrTypedArray() {
        return IsArrayNodeGen.create(Kind.FastOrTypedArray);
    }
}
