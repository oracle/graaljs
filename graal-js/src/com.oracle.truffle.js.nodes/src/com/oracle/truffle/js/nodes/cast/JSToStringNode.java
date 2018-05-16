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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNodeGen.JSToStringWrapperNodeGen;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This implements ECMA 9.8. ToString.
 */
@ImportStatic(JSObject.class)
public abstract class JSToStringNode extends JavaScriptBaseNode {
    protected static final int MAX_CLASSES = 3;

    final boolean undefinedToEmpty;
    final boolean symbolToString;
    @Child private JSToStringNode toStringNode;

    protected JSToStringNode(boolean undefinedToEmpty, boolean symbolToString) {
        this.undefinedToEmpty = undefinedToEmpty;
        this.symbolToString = symbolToString;
    }

    public static JSToStringNode create() {
        return JSToStringNodeGen.create(false, false);
    }

    /**
     * Creates a node that returns the empty string for {@code undefined}.
     */
    public static JSToStringNode createUndefinedToEmpty() {
        return JSToStringNodeGen.create(true, false);
    }

    /**
     * Creates a ToString node that returns the SymbolDescriptiveString for a symbol.
     *
     * Used by the String function if called without new (ES6 21.1.1.1 "String(value)").
     */
    public static JSToStringNode createSymbolToString() {
        return JSToStringNodeGen.create(false, true);
    }

    public abstract String executeString(Object operand);

    @Specialization
    protected String doLazyString(JSLazyString value,
                    @Cached("createBinaryProfile()") ConditionProfile flattenProfile) {
        return value.toString(flattenProfile);
    }

    @Specialization
    protected String doString(String value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected String doNull(@SuppressWarnings("unused") Object value) {
        return Null.NAME;
    }

    @Specialization(guards = "isUndefined(value)")
    protected String doUndefined(@SuppressWarnings("unused") Object value) {
        return undefinedToEmpty ? "" : Undefined.NAME;
    }

    @Specialization
    protected String doInteger(int value) {
        return Boundaries.stringValueOf(value);
    }

    @Specialization
    protected String doBigInt(BigInt value) {
        return Boundaries.stringValueOf(value);
    }

    @Specialization
    protected String doDouble(double d, @Cached("create()") JSDoubleToStringNode doubleToStringNode) {
        return doubleToStringNode.executeString(d);
    }

    @Specialization
    protected String doBoolean(boolean value) {
        return JSRuntime.booleanToString(value);
    }

    @Specialization
    protected String doLong(long value) {
        return Boundaries.stringValueOf(value);
    }

    @Specialization(guards = "isJSObject(value)")
    protected String doJSObject(DynamicObject value,
                    @Cached("createHintString()") JSToPrimitiveNode toPrimitiveHintStringNode) {
        return getToStringNode().executeString(toPrimitiveHintStringNode.execute(value));
    }

    @TruffleBoundary
    @Specialization
    protected String doSymbol(Symbol value) {
        if (symbolToString) {
            return value.toString();
        } else {
            throw Errors.createTypeErrorCannotConvertToString("a Symbol value", this);
        }
    }

    @Specialization(guards = "isTruffleJavaObject(object)")
    protected String doTruffleJavaObject(TruffleObject object) {
        String result = null;
        TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
        Object javaObject = env.asHostObject(object);
        if (javaObject != null) {
            result = Boundaries.javaToString(javaObject);
        }
        return (result == null) ? Null.NAME : result;
    }

    @Specialization(guards = {"isForeignObject(object)", "!isTruffleJavaObject(object)"})
    protected String doTruffleObject(TruffleObject object,
                    @Cached("create()") JSUnboxOrGetNode interopUnboxNode) {
        return getToStringNode().executeString(interopUnboxNode.executeWithTarget(object));
    }

    @TruffleBoundary
    @Specialization
    protected String doJavaClass(JavaClass value) {
        return value.toString();
    }

    @TruffleBoundary
    @Specialization
    protected String doJavaMethod(JavaMethod value) {
        return value.toString();
    }

    @Specialization(guards = {"cachedClass != null", "object.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected String doJavaObject(Object object, @Cached("getJavaObjectClass(object)") Class<?> cachedClass) {
        return doJavaGeneric(cachedClass.cast(object));
    }

    @Specialization(guards = {"!isBoolean(object)", "!isNumber(object)", "!isString(object)", "!isSymbol(object)", "!isJSObject(object)", "!isForeignObject(object)"}, replaces = "doJavaObject")
    protected String doJavaGeneric(Object object) {
        assert object != null && !JSRuntime.isJSNative(object);
        return Boundaries.stringValueOf(object);
    }

    protected JSToStringNode getToStringNode() {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStringNode = insert(JSToStringNode.create());
        }
        return toStringNode;
    }

    public abstract static class JSToStringWrapperNode extends JSUnaryNode {

        @Child private JSToStringNode toStringNode;

        protected JSToStringWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        public static JSToStringWrapperNode create(JavaScriptNode child) {
            return JSToStringWrapperNodeGen.create(child);
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return clazz == String.class;
        }

        @Specialization
        protected String doDefault(Object value) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return JSToStringWrapperNodeGen.create(cloneUninitialized(getOperand()));
        }
    }
}
