/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNodeGen.JSToObjectWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

/**
 * Implementation of the ECMAScript abstract operation ToObject(argument).
 *
 * Converts the argument to a JSDynamicObject or TruffleObject.
 */
@ImportStatic({CompilerDirectives.class, JSConfig.class})
public abstract class JSToObjectNode extends JavaScriptBaseNode {

    protected final JSContext context;
    protected final boolean checkForNullOrUndefined;

    protected JSToObjectNode(JSContext context, boolean checkForNullOrUndefined) {
        this.context = context;
        this.checkForNullOrUndefined = checkForNullOrUndefined;
    }

    public abstract Object execute(Object value);

    @NeverDefault
    public static JSToObjectNode createToObject(JSContext context) {
        return createToObject(context, true);
    }

    @NeverDefault
    public static JSToObjectNode createToObjectNoCheck(JSContext context) {
        return createToObject(context, false);
    }

    @NeverDefault
    protected static JSToObjectNode createToObject(JSContext context, boolean checkForNullOrUndefined) {
        return JSToObjectNodeGen.create(context, checkForNullOrUndefined);
    }

    protected final JSContext getContext() {
        return context;
    }

    @Idempotent
    protected final boolean isCheckForNullOrUndefined() {
        return checkForNullOrUndefined;
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doBoolean(boolean value) {
        return JSBoolean.create(context, getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doString(TruffleString value) {
        return JSString.create(getContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doInt(int value) {
        return JSNumber.create(getContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doDouble(double value) {
        return JSNumber.create(getContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doBigInt(BigInt value) {
        return JSBigInt.create(getContext(), getRealm(), value);
    }

    @InliningCutoff
    @Specialization
    protected JSDynamicObject doSymbol(Symbol value) {
        return JSSymbol.create(getContext(), getRealm(), value);
    }

    @Specialization(guards = {"cachedClass != null", "isExact(object, cachedClass)"}, limit = "1")
    protected static Object doJSObjectCached(Object object,
                    @Cached(value = "getClassIfObject(object)") Class<?> cachedClass) {
        return cachedClass.cast(object);
    }

    final Class<?> getClassIfObject(Object object) {
        if (isCheckForNullOrUndefined() && JSGuards.isJSObject(object)) {
            return object.getClass();
        } else if (!isCheckForNullOrUndefined() && JSGuards.isJSDynamicObject(object)) {
            return object.getClass();
        } else {
            return null;
        }
    }

    @Specialization(guards = {"!isCheckForNullOrUndefined()", "isJSDynamicObject(object)"}, replaces = "doJSObjectCached")
    protected Object doJSObjectNoCheck(Object object) {
        return object;
    }

    @Specialization(guards = {"isCheckForNullOrUndefined()", "isJSObject(object)"}, replaces = "doJSObjectCached")
    protected Object doJSObjectCheck(Object object) {
        return object;
    }

    @Specialization(guards = {"isCheckForNullOrUndefined()", "isNullOrUndefined(object)"})
    protected JSDynamicObject doNullOrUndefined(Object object) {
        throw Errors.createTypeErrorNotObjectCoercible(object, this, context);
    }

    @InliningCutoff
    @Specialization(guards = {"isForeignTruffleObject || isForeignNumber(value)"}, limit = "InteropLibraryLimit")
    protected final Object doForeignObject(Object value,
                    @CachedLibrary("value") InteropLibrary interop,
                    @Bind("isForeignObject(value)") boolean isForeignTruffleObject) {
        if (interop.isNull(value)) {
            throw Errors.createTypeErrorNotObjectCoercible(value, this, context);
        }
        try {
            if (interop.isBoolean(value)) {
                return doBoolean(interop.asBoolean(value));
            } else if (interop.isString(value)) {
                return doString(interop.asTruffleString(value));
            } else if (interop.isNumber(value)) {
                if (interop.fitsInInt(value)) {
                    return doInt(interop.asInt(value));
                } else if (interop.fitsInDouble(value)) {
                    return doDouble(interop.asDouble(value));
                } else {
                    // Ambiguous numeric type; leave as foreign object.
                    if (isForeignTruffleObject) {
                        assert value instanceof TruffleObject;
                        return value;
                    } else {
                        // Wrap Java primitive numbers in TruffleObject.
                        Object result = getRealm().getEnv().asBoxedGuestValue(value);
                        assert JSRuntime.isForeignObject(result);
                        return result;
                    }
                }
            } else {
                return value;
            }
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(value, e, "ToObject", this);
        }
    }

    public abstract static class JSToObjectWrapperNode extends JSUnaryNode {
        @Child private JSToObjectNode toObjectNode;

        protected JSToObjectWrapperNode(JavaScriptNode operand, JSToObjectNode toObjectNode) {
            super(operand);
            this.toObjectNode = toObjectNode;
        }

        /**
         * This factory method forces the creation of an JSObjectCastNode; in contrast to
         * {@code create} it does not check the child and try to omit unnecessary cast nodes.
         */
        public static JSToObjectWrapperNode createToObject(JSContext context, JavaScriptNode child) {
            return JSToObjectWrapperNodeGen.create(child, JSToObjectNode.createToObject(context));
        }

        @Specialization
        protected Object doDefault(Object value) {
            return toObjectNode.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            JSToObjectNode clonedToObject = JSToObjectNodeGen.create(toObjectNode.getContext(), toObjectNode.isCheckForNullOrUndefined());
            return JSToObjectWrapperNodeGen.create(cloneUninitialized(getOperand(), materializedTags), clonedToObject);
        }
    }
}
