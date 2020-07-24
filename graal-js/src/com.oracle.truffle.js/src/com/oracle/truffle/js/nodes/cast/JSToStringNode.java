/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNodeGen.JSToStringWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This implements ECMA 9.8. ToString.
 */
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
    protected String doBoolean(boolean value) {
        return JSRuntime.booleanToString(value);
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
    protected String doLong(long value) {
        return Boundaries.stringValueOf(value);
    }

    @Specialization
    protected String doDouble(double d, @Cached("create()") JSDoubleToStringNode doubleToStringNode) {
        return doubleToStringNode.executeString(d);
    }

    @Specialization(guards = "isJSDynamicObject(value)", replaces = "doUndefined")
    protected String doJSObject(DynamicObject value,
                    @Cached("createHintString()") JSToPrimitiveNode toPrimitiveHintStringNode) {
        return (undefinedToEmpty && (value == Undefined.instance)) ? "" : getToStringNode().executeString(toPrimitiveHintStringNode.execute(value));
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

    @Specialization(guards = {"isForeignObject(object)"})
    protected String doTruffleObject(Object object,
                    @Cached("createHintString()") JSToPrimitiveNode toPrimitiveNode) {
        return getToStringNode().executeString(toPrimitiveNode.execute(object));
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
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return JSToStringWrapperNodeGen.create(cloneUninitialized(getOperand(), materializedTags));
        }
    }
}
