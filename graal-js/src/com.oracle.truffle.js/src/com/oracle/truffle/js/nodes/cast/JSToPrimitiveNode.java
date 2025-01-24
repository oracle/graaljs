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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Implements ToPrimitive.
 *
 * @see OrdinaryToPrimitiveNode
 */
@ImportStatic({JSConfig.class})
public abstract class JSToPrimitiveNode extends JavaScriptBaseNode {

    @Child private OrdinaryToPrimitiveNode ordinaryToPrimitiveNode;

    public enum Hint {
        Default(Strings.HINT_DEFAULT),
        Number(Strings.HINT_NUMBER),
        String(Strings.HINT_STRING);

        private final TruffleString hintName;

        Hint(TruffleString hintName) {
            this.hintName = hintName;
        }

        public TruffleString getHintName() {
            return hintName;
        }
    }

    protected final Hint hint;

    protected JSToPrimitiveNode(Hint hint) {
        this.hint = hint;
    }

    public abstract Object execute(Object value);

    @NeverDefault
    public static JSToPrimitiveNode createHintDefault() {
        return create(Hint.Default);
    }

    @NeverDefault
    public static JSToPrimitiveNode createHintString() {
        return create(Hint.String);
    }

    @NeverDefault
    public static JSToPrimitiveNode createHintNumber() {
        return create(Hint.Number);
    }

    @NeverDefault
    public static JSToPrimitiveNode create(Hint hint) {
        return JSToPrimitiveNodeGen.create(hint);
    }

    @Specialization
    protected int doInt(int value) {
        return value;
    }

    @Specialization
    protected SafeInteger doSafeInteger(SafeInteger value) {
        return value;
    }

    @Specialization
    protected long doLong(long value) {
        return value;
    }

    @Specialization
    protected double doDouble(double value) {
        return value;
    }

    @Specialization
    protected boolean doBoolean(boolean value) {
        return value;
    }

    @Specialization
    protected Object doString(TruffleString value) {
        return value;
    }

    @Specialization
    protected Symbol doSymbol(Symbol value) {
        return value;
    }

    @Specialization
    protected BigInt doBigInt(BigInt value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected JSDynamicObject doNull(@SuppressWarnings("unused") Object value) {
        return Null.instance;
    }

    @Specialization(guards = "isUndefined(value)")
    protected JSDynamicObject doUndefined(@SuppressWarnings("unused") Object value) {
        return Undefined.instance;
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization
    protected final Object doJSObject(JSObject object,
                    @Bind Node node,
                    @Exclusive @Cached("createGetToPrimitive()") PropertyGetNode getToPrimitive,
                    @Exclusive @Cached IsPrimitiveNode isPrimitive,
                    @Exclusive @Cached InlinedConditionProfile exoticToPrimProfile,
                    @Exclusive @Cached("createCall()") JSFunctionCallNode callExoticToPrim) {
        Object exoticToPrim = getToPrimitive.getValue(object);
        if (exoticToPrimProfile.profile(node, !JSRuntime.isNullOrUndefined(exoticToPrim))) {
            Object result = callExoticToPrim.executeCall(JSArguments.createOneArg(object, exoticToPrim, hint.getHintName()));
            if (isPrimitive.executeBoolean(result)) {
                return result;
            }
            throw Errors.createTypeError("[Symbol.toPrimitive] method returned a non-primitive object", this);
        }

        return ordinaryToPrimitive(object);
    }

    protected final boolean isHintString() {
        return hint == Hint.String;
    }

    @SuppressWarnings("truffle-static-method")
    @InliningCutoff
    @Specialization(guards = "isForeignObject(object)", limit = "InteropLibraryLimit")
    protected final Object doForeignObject(Object object,
                    @Bind Node node,
                    @CachedLibrary("object") InteropLibrary interop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary resultInterop,
                    @Exclusive @Cached InlinedConditionProfile exoticToPrimProfile,
                    @Exclusive @Cached ForeignObjectPrototypeNode foreignObjectPrototypeNode,
                    @Exclusive @Cached("createGetToPrimitive()") PropertyGetNode getToPrimitive,
                    @Exclusive @Cached IsPrimitiveNode isPrimitive,
                    @Exclusive @Cached("createCall()") JSFunctionCallNode callExoticToPrim,
                    @Exclusive @Cached InlinedBranchProfile errorBranch,
                    @Cached TruffleString.SwitchEncodingNode switchEncoding) {
        Object primitive = JSInteropUtil.toPrimitiveOrDefaultLossless(object, null, interop, switchEncoding, this);
        if (primitive != null) {
            return primitive;
        }

        // Try foreign object prototype [Symbol.toPrimitive] property first.
        // e.g.: Instant and ZonedDateTime use Date.prototype[@@toPrimitive].
        Object exoticToPrim = getToPrimitive.getValueOrUndefined(foreignObjectPrototypeNode.execute(object), object);
        if (exoticToPrimProfile.profile(node, !JSRuntime.isNullOrUndefined(exoticToPrim))) {
            Object result = callExoticToPrim.executeCall(JSArguments.createOneArg(object, exoticToPrim, hint.getHintName()));
            if (isPrimitive.executeBoolean(result)) {
                primitive = result;
            } else {
                errorBranch.enter(node);
                throw Errors.createTypeError("[Symbol.toPrimitive] method returned a non-primitive object", this);
            }
        } else {
            JSRealm realm = getRealm();
            TruffleLanguage.Env env = realm.getEnv();
            if (env.isHostObject(object)) {
                Object maybeResult = tryHostObjectToPrimitive(object, hint, interop);
                if (maybeResult != null) {
                    return maybeResult;
                }
            }

            // Try toString() and valueOf(), in hint order.
            primitive = ordinaryToPrimitive(object);
        }

        assert IsPrimitiveNode.getUncached().executeBoolean(primitive) : primitive;
        if (JSRuntime.isJSPrimitive(primitive)) {
            return primitive;
        }
        primitive = JSInteropUtil.toPrimitiveOrDefaultLossless(primitive, null, resultInterop, switchEncoding, this);
        if (primitive != null) {
            return primitive;
        } else {
            // Throw for non-primitive-coercible and unsupported primitive types.
            errorBranch.enter(node);
            throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
        }
    }

    public static Object tryHostObjectToPrimitive(Object object, Hint hint, InteropLibrary interop) {
        if (hint != Hint.String && JavaScriptLanguage.get(interop).getJSContext().isOptionNashornCompatibilityMode() &&
                        interop.isMemberInvocable(object, "doubleValue")) {
            try {
                return interop.invokeMember(object, "doubleValue");
            } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(object, e, "doubleValue()", interop);
            }
        } else if (interop.isMetaObject(object)) {
            return javaClassToString(object, interop);
        }
        return null;
    }

    @TruffleBoundary
    private static TruffleString javaClassToString(Object object, InteropLibrary interop) {
        try {
            String qualifiedName = InteropLibrary.getUncached().asString(interop.getMetaQualifiedName(object));
            if (JavaScriptLanguage.get(interop).getJSContext().isOptionNashornCompatibilityMode() && qualifiedName.endsWith("[]")) {
                Object hostObject = JSRealm.get(interop).getEnv().asHostObject(object);
                qualifiedName = ((Class<?>) hostObject).getName();
            }
            return Strings.fromJavaString("class " + qualifiedName);
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeErrorInteropException(object, e, "getTypeName", interop);
        }
    }

    @Fallback
    protected TruffleString doFallback(Object value) {
        assert value != null;
        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
    }

    private Object ordinaryToPrimitive(Object object) {
        if (ordinaryToPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ordinaryToPrimitiveNode = insert(createOrdinaryToPrimitive());
        }
        return ordinaryToPrimitiveNode.execute(object);
    }

    protected PropertyGetNode createGetToPrimitive() {
        return PropertyGetNode.createGetMethod(Symbol.SYMBOL_TO_PRIMITIVE, getLanguage().getJSContext());
    }

    protected OrdinaryToPrimitiveNode createOrdinaryToPrimitive() {
        return OrdinaryToPrimitiveNode.create(isHintString() ? Hint.String : Hint.Number);
    }

    @DenyReplace
    @GenerateCached(false)
    private static final class Uncached extends JSToPrimitiveNode {

        private static final JSToPrimitiveNode HINT_DEFAULT = new Uncached(Hint.Default);
        private static final JSToPrimitiveNode HINT_NUMBER = new Uncached(Hint.Number);
        private static final JSToPrimitiveNode HINT_STRING = new Uncached(Hint.String);

        protected Uncached(Hint hint) {
            super(hint);
        }

        @Override
        public Object execute(Object value) {
            return JSRuntime.toPrimitive(value, hint);
        }

    }

    @NeverDefault
    public static JSToPrimitiveNode getUncachedHintDefault() {
        return Uncached.HINT_DEFAULT;
    }

    @NeverDefault
    public static JSToPrimitiveNode getUncachedHintNumber() {
        return Uncached.HINT_NUMBER;
    }

    @NeverDefault
    public static JSToPrimitiveNode getUncachedHintString() {
        return Uncached.HINT_STRING;
    }
}
