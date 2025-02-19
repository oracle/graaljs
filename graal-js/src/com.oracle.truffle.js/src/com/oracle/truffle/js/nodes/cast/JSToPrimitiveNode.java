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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
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
 * Abstract base class of {@link ToPrimitiveNode} and {@link AsPrimitiveNode} that defines the
 * common primitive specializations shared by these nodes. {@link JSObject} and foreign object
 * specializations are to be implemented in each subclass. The hint is not used in this class.
 */
@SuppressWarnings("unused")
@GenerateCached(false)
abstract class ToPrimitiveBaseNode extends JavaScriptBaseNode {

    @Specialization
    protected static int doInt(int value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization
    protected static SafeInteger doSafeInteger(SafeInteger value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization
    protected static long doLong(long value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization
    protected static double doDouble(double value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization
    protected static boolean doBoolean(boolean value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization
    protected static TruffleString doString(TruffleString value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization
    protected static Symbol doSymbol(Symbol value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization
    protected static BigInt doBigInt(BigInt value, JSToPrimitiveNode.Hint hint) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static JSDynamicObject doNull(Object value, JSToPrimitiveNode.Hint hint) {
        return Null.instance;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static JSDynamicObject doUndefined(Object value, JSToPrimitiveNode.Hint hint) {
        return Undefined.instance;
    }

    @Fallback
    protected Object doFallback(Object value, JSToPrimitiveNode.Hint hint, @Bind Node node) {
        assert value != null;
        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(node);
    }
}

/**
 * Actual implementation of ToPrimitive (input, hint).
 */
@GenerateCached(false)
@GenerateInline
@GenerateUncached
@ImportStatic({JSConfig.class, JSToPrimitiveNode.Hint.class, Symbol.class})
abstract class ToPrimitiveNode extends ToPrimitiveBaseNode {

    public abstract Object execute(Node node, Object value, JSToPrimitiveNode.Hint hint);

    @Specialization
    protected static Object doJSObject(Node node, JSObject object, JSToPrimitiveNode.Hint hint,
                    @Shared @Cached(value = "createGetMethod(SYMBOL_TO_PRIMITIVE, getJSContext())", uncached = "getNullNode()", inline = false) PropertyGetNode getToPrimitive,
                    @Shared @Cached InlinedConditionProfile exoticToPrimProfile,
                    @Shared @Cached(value = "createCall()", uncached = "getUncachedCall()", inline = false) JSFunctionCallNode callExoticToPrim,
                    @Shared @Cached AsPrimitiveNode asPrimitiveNode,
                    @Shared @Cached(inline = false) OrdinaryToPrimitiveNode ordinaryToPrimitiveNode) {
        Object exoticToPrim = getMethod(object, Symbol.SYMBOL_TO_PRIMITIVE, getToPrimitive);
        Object result;
        if (exoticToPrimProfile.profile(node, !JSRuntime.isNullOrUndefined(exoticToPrim))) {
            result = callExoticToPrim.executeCall(JSArguments.createOneArg(object, exoticToPrim, hint.getHintName()));
        } else {
            result = ordinaryToPrimitiveNode.execute(object, hint.getOrdinaryToPrimitiveHint());
            assert IsPrimitiveNode.getUncached().executeBoolean(result) : result;
        }
        // Throws for non-primitive-coercible and unsupported primitive types.
        // Also converts foreign null values to JS null.
        return asPrimitiveNode.execute(node, result, hint);
    }

    @InliningCutoff
    @Specialization(guards = "isForeignObject(object)", limit = "InteropLibraryLimit")
    protected static Object doForeignObject(Node node, Object object, JSToPrimitiveNode.Hint hint,
                    @CachedLibrary("object") InteropLibrary interop,
                    @Shared @Cached InlinedConditionProfile exoticToPrimProfile,
                    @Shared @Cached(inline = false) ForeignObjectPrototypeNode foreignObjectPrototypeNode,
                    @Shared @Cached(value = "createGetMethod(SYMBOL_TO_PRIMITIVE, getJSContext())", uncached = "getNullNode()", inline = false) PropertyGetNode getToPrimitive,
                    @Shared @Cached(value = "createCall()", uncached = "getUncachedCall()", inline = false) JSFunctionCallNode callExoticToPrim,
                    @Shared @Cached AsPrimitiveNode asPrimitiveNode,
                    @Shared @Cached(inline = false) OrdinaryToPrimitiveNode ordinaryToPrimitiveNode,
                    @Shared @Cached(inline = false) TruffleString.SwitchEncodingNode switchEncoding) {
        Object primitive = JSInteropUtil.toPrimitiveOrDefaultLossless(object, null, interop, switchEncoding, node);
        if (primitive != null) {
            return primitive;
        }

        // Try foreign object prototype [Symbol.toPrimitive] property first.
        // e.g.: Instant and ZonedDateTime use Date.prototype[@@toPrimitive].
        JSDynamicObject proto = foreignObjectPrototypeNode.execute(object);
        Object exoticToPrim = getPrototypeMethod(proto, object, Symbol.SYMBOL_TO_PRIMITIVE, getToPrimitive);
        Object result;
        if (exoticToPrimProfile.profile(node, !JSRuntime.isNullOrUndefined(exoticToPrim))) {
            result = callExoticToPrim.executeCall(JSArguments.createOneArg(object, exoticToPrim, hint.getHintName()));
        } else {
            JSRealm realm = JSRealm.get(node);
            TruffleLanguage.Env env = realm.getEnv();
            if (env.isHostObject(object)) {
                Object maybeResult = JSToPrimitiveNode.tryHostObjectToPrimitive(object, hint, interop);
                if (maybeResult != null) {
                    return maybeResult;
                }
            }

            // Try toString() and valueOf(), in hint order.
            result = ordinaryToPrimitiveNode.execute(object, hint.getOrdinaryToPrimitiveHint());
            assert IsPrimitiveNode.getUncached().executeBoolean(result) : result;
        }
        // Throws for non-primitive-coercible and unsupported primitive types.
        // Also converts foreign null values to JS null.
        return asPrimitiveNode.execute(node, result, hint);
    }

    static Object getMethod(JSDynamicObject obj, Object propertyKey, PropertyGetNode getNode) {
        if (getNode != null) {
            return getNode.getValue(obj);
        } else {
            return JSObject.getMethod(obj, propertyKey);
        }
    }

    static Object getPrototypeMethod(JSDynamicObject proto, Object receiver, Object propertyKey, PropertyGetNode getNode) {
        if (getNode != null) {
            return getNode.getValueOrUndefined(proto, receiver);
        } else {
            return JSObject.getMethod(proto, receiver, propertyKey);
        }
    }
}

/**
 * Implements ToPrimitive.
 *
 * @see OrdinaryToPrimitiveNode
 */
@SuppressWarnings("hiding")
@ImportStatic({JSConfig.class})
public abstract class JSToPrimitiveNode extends ToPrimitiveBaseNode {

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

        public Hint getOrdinaryToPrimitiveHint() {
            return this == String ? Hint.String : Hint.Number;
        }
    }

    protected final Hint hint;

    protected JSToPrimitiveNode(Hint hint) {
        this.hint = hint;
    }

    public final Object execute(Object value) {
        return execute(value, hint);
    }

    protected abstract Object execute(Object value, Hint hint);

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
    protected final Object doJSObject(JSObject value, Hint hint,
                    @Cached @Shared ToPrimitiveNode toPrimitiveNode) {
        return toPrimitiveNode.execute(this, value, hint);
    }

    @Specialization(guards = "isForeignObject(value)")
    protected final Object doForeignObject(Object value, Hint hint,
                    @Cached @Shared ToPrimitiveNode toPrimitiveNode) {
        return toPrimitiveNode.execute(this, value, hint);
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

    @DenyReplace
    @GenerateCached(false)
    private static final class Uncached extends JSToPrimitiveNode {

        private static final JSToPrimitiveNode HINT_DEFAULT = new Uncached(Hint.Default);
        private static final JSToPrimitiveNode HINT_NUMBER = new Uncached(Hint.Number);
        private static final JSToPrimitiveNode HINT_STRING = new Uncached(Hint.String);

        Uncached(Hint hint) {
            super(hint);
        }

        @Override
        public Object execute(Object value, Hint hint) {
            return ToPrimitiveNodeGen.getUncached().execute(null, value, hint);
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

    @NeverDefault
    public static JSToPrimitiveNode getUncached(Hint hint) {
        return switch (hint) {
            case Number -> getUncachedHintNumber();
            case String -> getUncachedHintString();
            case Default -> getUncachedHintDefault();
        };
    }
}

/**
 * Implements the exoticToPrim result validation part of ToPrimitive (if result is not an Object,
 * return result else throw a TypeError).
 *
 * Also converts foreign primitive values to JS primitive values (such as foreign null to JS null).
 */
@GenerateCached(false)
@GenerateInline
@GenerateUncached
@ImportStatic({JSConfig.class})
abstract class AsPrimitiveNode extends ToPrimitiveBaseNode {

    public abstract Object execute(Node node, Object value, JSToPrimitiveNode.Hint hint);

    @Specialization
    protected static Object doJSObject(Node node, @SuppressWarnings("unused") JSObject object, @SuppressWarnings("unused") JSToPrimitiveNode.Hint hint) {
        throw Errors.createTypeError("[Symbol.toPrimitive] method returned a non-primitive object", node);
    }

    @InliningCutoff
    @Specialization(guards = "isForeignObject(object)")
    protected static Object doForeignObject(Node node, Object object, @SuppressWarnings("unused") JSToPrimitiveNode.Hint hint,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached(inline = false) TruffleString.SwitchEncodingNode switchEncoding) {
        Object primitive = JSInteropUtil.toPrimitiveOrDefaultLossless(object, null, interop, switchEncoding, node);
        if (primitive != null) {
            return primitive;
        } else {
            errorBranch.enter(node);
            throw Errors.createTypeError("[Symbol.toPrimitive] method returned a non-primitive object", node);
        }
    }
}
