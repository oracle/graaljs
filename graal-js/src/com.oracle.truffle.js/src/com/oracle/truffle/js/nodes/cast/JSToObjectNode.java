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
package com.oracle.truffle.js.nodes.cast;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNodeGen.JSToObjectWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Implementation of the ECMAScript abstract operation ToObject(argument).
 *
 * Converts the argument to a JSDynamicObject or TruffleObject.
 */
@GenerateUncached
@ImportStatic({CompilerDirectives.class, JSConfig.class})
public abstract class JSToObjectNode extends JavaScriptBaseNode {

    protected JSToObjectNode() {
    }

    public abstract Object execute(Object value);

    @NeverDefault
    public static JSToObjectNode create() {
        return JSToObjectNodeGen.create();
    }

    @NeverDefault
    public static JSToObjectNode getUncached() {
        return JSToObjectNodeGen.getUncached();
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doBoolean(boolean value) {
        return JSBoolean.create(getJSContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doString(TruffleString value) {
        return JSString.create(getJSContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doInt(int value) {
        return JSNumber.create(getJSContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doDouble(double value) {
        return JSNumber.create(getJSContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doBigInt(BigInt value) {
        return JSBigInt.create(getJSContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doSymbol(Symbol value) {
        return JSSymbol.create(getJSContext(), getRealm(), value);
    }

    @Specialization(guards = {"cachedClass != null", "isExact(object, cachedClass)"}, limit = "1")
    protected static Object doJSObjectCached(Object object,
                    @Cached(value = "getClassIfObject(object)") Class<?> cachedClass) {
        return cachedClass.cast(object);
    }

    static Class<?> getClassIfObject(Object object) {
        if (JSGuards.isJSObject(object)) {
            return object.getClass();
        } else {
            return null;
        }
    }

    @Specialization(replaces = "doJSObjectCached")
    protected Object doJSObject(JSObject object) {
        return object;
    }

    @Specialization(guards = {"isNullOrUndefined(object)"})
    protected JSDynamicObject doNullOrUndefined(Object object) {
        throw Errors.createTypeErrorNotObjectCoercible(object, this);
    }

    @InliningCutoff
    @Specialization(guards = {"isForeignObjectOrNumber(value)"}, limit = "InteropLibraryLimit")
    protected final Object doForeignObject(Object value,
                    @CachedLibrary("value") InteropLibrary interop) {
        if (interop.isNull(value)) {
            throw Errors.createTypeErrorNotObjectCoercible(value, this);
        }
        return doForeignObjectNonNull(value, interop, this);
    }

    static Object doForeignObjectNonNull(Object value, InteropLibrary interop, JavaScriptBaseNode node) {
        assert !interop.isNull(value);
        try {
            if (!interop.hasMembers(value)) {
                if (interop.isBoolean(value)) {
                    return JSBoolean.create(JavaScriptLanguage.get(node).getJSContext(), JSRealm.get(node), interop.asBoolean(value));
                } else if (interop.isString(value)) {
                    return JSString.create(JavaScriptLanguage.get(node).getJSContext(), JSRealm.get(node), interop.asTruffleString(value));
                } else if (interop.isNumber(value)) {
                    return doForeignNumber(value, interop, node);
                }
            }
            assert value instanceof TruffleObject : value;
            return value;
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(value, e, "ToObject", node);
        }
    }

    private static Object doForeignNumber(Object value, InteropLibrary interop, JavaScriptBaseNode node) throws UnsupportedMessageException {
        Number number;
        if (interop.fitsInInt(value)) {
            number = interop.asInt(value);
        } else if (interop.fitsInDouble(value)) {
            number = interop.asDouble(value);
        } else if (interop.fitsInLong(value)) {
            number = interop.asLong(value);
        } else if (interop.fitsInBigInteger(value)) {
            /*
             * The only way to get the value out of a JS Number object again is through
             * Number.prototype methods which expect the value to already be a Number, so we might
             * as well lossily convert Long and BigInteger to Double eagerly.
             */
            number = BigInt.doubleValueOf(interop.asBigInteger(value));
        } else {
            // Java primitive numbers always fit in either Double or BigInteger
            assert value instanceof TruffleObject && !(value instanceof Number) : value;
            return value;
        }
        return JSNumber.create(JavaScriptLanguage.get(node).getJSContext(), JSRealm.get(node), number);
    }

    public abstract static class JSToObjectWrapperNode extends JSUnaryNode {

        protected JSToObjectWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        /**
         * This factory method forces the creation of an JSObjectCastNode; in contrast to
         * {@code create} it does not check the child and try to omit unnecessary cast nodes.
         */
        public static JSToObjectWrapperNode createToObject(JavaScriptNode child) {
            return JSToObjectWrapperNodeGen.create(child);
        }

        @Specialization
        protected Object doDefault(Object value,
                        @Cached JSToObjectNode toObjectNode) {
            return toObjectNode.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return JSToObjectWrapperNodeGen.create(cloneUninitialized(getOperand(), materializedTags));
        }
    }
}
