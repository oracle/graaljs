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
package com.oracle.truffle.js.runtime.objects;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.interop.JSInteropGetIteratorNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInvokeNode;
import com.oracle.truffle.js.nodes.interop.KeyInfoNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBase;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSObjectPrototype;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.InteropArray;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * The common base class for all JavaScript objects (values of type Object according to the spec).
 *
 * Includes static methods for dealing with JS objects (internal methods).
 */
@ExportLibrary(InteropLibrary.class)
public abstract non-sealed class JSObject extends JSDynamicObject {

    public static final TruffleString CONSTRUCTOR = Strings.constant("constructor");
    public static final TruffleString PROTOTYPE = Strings.constant("prototype");
    public static final TruffleString PROTO = Strings.constant("__proto__");
    public static final TruffleString GET_PROTO_NAME = Strings.constant("get __proto__");
    public static final TruffleString SET_PROTO_NAME = Strings.constant("set __proto__");
    public static final HiddenKey HIDDEN_PROTO = new HiddenKey("[[Prototype]]");

    public static final TruffleString NO_SUCH_PROPERTY_NAME = Strings.constant("__noSuchProperty__");
    public static final TruffleString NO_SUCH_METHOD_NAME = Strings.constant("__noSuchMethod__");
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected JSObject(Shape shape, JSDynamicObject proto) {
        super(shape);
        assert proto != null && (JSObjectFactory.hasInObjectProto(shape) || JSObjectFactory.verifyPrototype(shape, proto));
    }

    protected JSObject copyWithoutProperties(@SuppressWarnings("unused") Shape shape) {
        throw Errors.notImplemented("copy");
    }

    /**
     * Returns whether object is a proper JavaScript Object.
     */
    public static boolean isJSObject(Object object) {
        return JSRuntime.isObject(object);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public abstract static class GetMembers {

        @Specialization
        public static Object nonArray(JSObject target, @SuppressWarnings("unused") boolean internal) {
            return InteropArray.create(JSObject.enumerableOwnNames(target));
        }
    }

    @TruffleBoundary
    protected static String[] filterEnumerableNames(JSDynamicObject target, Iterable<Object> ownKeys, JSClass jsclass) {
        List<String> names = new ArrayList<>();
        for (Object obj : ownKeys) {
            if (obj instanceof TruffleString name && !JSRuntime.isArrayIndex(name)) {
                PropertyDescriptor desc = jsclass.getOwnProperty(target, name);
                if (desc != null && desc.getEnumerable()) {
                    names.add(Strings.toJavaString(name));
                }
            }
        }
        return names.toArray(EMPTY_STRING_ARRAY);
    }

    public static JavaScriptLanguage language(InteropLibrary node) {
        return JavaScriptLanguage.get(node);
    }

    @ExportMessage
    public final Object readMember(String key,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached(value = "create(language(self).getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Cached(value = "language(self).bindMemberFunctions()", allowUncached = true, neverDefault = false) boolean bindMemberFunctions,
                    @Cached @Shared TruffleString.FromJavaStringNode fromJavaString,
                    @Cached @Exclusive ExportValueNode exportNode) throws UnknownIdentifierException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            TruffleString tStringKey = Strings.fromJavaString(fromJavaString, key);
            JSDynamicObject target = this;
            Object result;
            if (readNode == null) {
                result = JSObject.getOrDefault(target, tStringKey, target, null);
            } else {
                result = readNode.executeWithTargetAndIndexOrDefault(target, tStringKey, null);
            }
            if (result == null) {
                throw UnknownIdentifierException.create(key);
            }
            return exportNode.execute(result, target, bindMemberFunctions);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public final boolean isMemberReadable(String key,
                    @Shared @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.READABLE);
    }

    @ExportMessage
    public final void writeMember(String key, Object value,
                    @CachedLibrary("this") InteropLibrary self,
                    @Shared @Cached KeyInfoNode keyInfo,
                    @Cached ImportValueNode castValueNode,
                    @Cached(value = "createCachedInterop()", uncached = "getUncachedWrite()") WriteElementNode writeNode,
                    @Cached @Shared TruffleString.FromJavaStringNode fromJavaString)
                    throws UnknownIdentifierException, UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            JSDynamicObject target = this;
            if (testIntegrityLevel(true)) {
                throw UnsupportedMessageException.create();
            }
            if (!keyInfo.execute(target, key, KeyInfoNode.WRITABLE)) {
                throw UnknownIdentifierException.create(key);
            }
            TruffleString tStringKey = Strings.fromJavaString(fromJavaString, key);
            Object importedValue = castValueNode.executeWithTarget(value);
            if (writeNode == null) {
                JSObject.set(target, tStringKey, importedValue, true, null);
            } else {
                writeNode.executeWithTargetAndIndexAndValue(target, tStringKey, importedValue);
            }
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public final boolean isMemberModifiable(String key,
                    @Shared @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.MODIFIABLE);
    }

    @ExportMessage
    public final boolean isMemberInsertable(String key,
                    @Shared @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.INSERTABLE);
    }

    @ExportMessage
    public final void removeMember(String key,
                    @Cached @Shared TruffleString.FromJavaStringNode fromJavaString) throws UnsupportedMessageException {
        if (testIntegrityLevel(false)) {
            throw UnsupportedMessageException.create();
        }
        JSObject.delete(this, Strings.fromJavaString(fromJavaString, key), true);
    }

    @ExportMessage
    public final boolean isMemberRemovable(String key,
                    @Shared @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.REMOVABLE);
    }

    @ExportMessage
    public final Object invokeMember(String id, Object[] args,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached JSInteropInvokeNode callNode,
                    @Cached @Shared TruffleString.FromJavaStringNode fromJavaString,
                    @Cached @Exclusive ExportValueNode exportNode) throws UnsupportedMessageException, UnknownIdentifierException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(this, Strings.fromJavaString(fromJavaString, id), args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public final boolean isMemberInvocable(String key,
                    @Shared @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.INVOCABLE);
    }

    @ExportMessage
    public final boolean hasMemberReadSideEffects(String key,
                    @Shared @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.READ_SIDE_EFFECTS);
    }

    @ExportMessage
    public final boolean hasMemberWriteSideEffects(String key,
                    @Shared @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.WRITE_SIDE_EFFECTS);
    }

    @ExportMessage
    public boolean hasIterator(
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached @Shared JSInteropGetIteratorNode getIteratorNode) {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        return getIteratorNode.hasIterator(this, language);
    }

    @ExportMessage
    public Object getIterator(
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached @Shared JSInteropGetIteratorNode getIteratorNode) throws UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            return getIteratorNode.getIterator(this, language);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasLanguage() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final Class<? extends TruffleLanguage<?>> getLanguage() {
        return JavaScriptLanguage.class;
    }

    @ExportMessage
    public final Object toDisplayString(boolean allowSideEffects) {
        return JSRuntime.toDisplayString(this, allowSideEffects);
    }

    public static ReadElementNode getUncachedRead() {
        return null;
    }

    public static WriteElementNode getUncachedWrite() {
        return null;
    }

    public static JSClass getJSClass(JSDynamicObject obj) {
        return JSShape.getJSClass(obj.getShape());
    }

    @TruffleBoundary
    public static JSDynamicObject getPrototype(JSDynamicObject obj) {
        return JSObject.getJSClass(obj).getPrototypeOf(obj);
    }

    public static JSDynamicObject getPrototype(JSDynamicObject obj, JSClassProfile jsclassProfile) {
        return jsclassProfile.getJSClass(obj).getPrototypeOf(obj);
    }

    @TruffleBoundary
    public static boolean setPrototype(JSDynamicObject obj, JSDynamicObject newPrototype) {
        assert newPrototype != null;
        return JSObject.getJSClass(obj).setPrototypeOf(obj, newPrototype);
    }

    public static boolean setPrototype(JSDynamicObject obj, JSDynamicObject newPrototype, JSClassProfile jsclassProfile) {
        assert newPrototype != null;
        return jsclassProfile.getJSClass(obj).setPrototypeOf(obj, newPrototype);
    }

    @TruffleBoundary
    public static Object get(JSDynamicObject obj, long index) {
        return JSObject.getJSClass(obj).get(obj, index);
    }

    @TruffleBoundary
    public static Object get(JSDynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).get(obj, key);
    }

    public static Object getMethod(JSDynamicObject obj, Object name) {
        return getMethod(obj, obj, name);
    }

    @TruffleBoundary
    public static Object getMethod(JSDynamicObject obj, Object receiver, Object name) {
        assert JSRuntime.isPropertyKey(name);
        Object result = JSRuntime.nullToUndefined(JSObject.getJSClass(obj).getMethodHelper(obj, receiver, name, null));
        return (result == Null.instance) ? Undefined.instance : result;
    }

    @TruffleBoundary
    public static boolean set(JSDynamicObject obj, long index, Object value) {
        return set(obj, index, value, false, null);
    }

    @TruffleBoundary
    public static boolean set(JSDynamicObject obj, Object key, Object value) {
        return set(obj, key, value, false, null);
    }

    @TruffleBoundary
    public static boolean set(JSDynamicObject obj, long index, Object value, boolean isStrict, Node encapsulatingNode) {
        return JSObject.getJSClass(obj).set(obj, index, value, obj, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    public static boolean set(JSDynamicObject obj, Object key, Object value, boolean isStrict, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        assert !(value instanceof String);
        return JSObject.getJSClass(obj).set(obj, key, value, obj, isStrict, encapsulatingNode);
    }

    /**
     * [[Set]] with a receiver different than the default.
     */
    public static boolean setWithReceiver(JSDynamicObject obj, Object key, Object value, Object receiver, boolean isStrict, JSClassProfile classProfile, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        assert !(value instanceof String);
        return classProfile.getJSClass(obj).set(obj, key, value, receiver, isStrict, encapsulatingNode);
    }

    public static boolean setWithReceiver(JSDynamicObject obj, long index, Object value, Object receiver, boolean isStrict, JSClassProfile classProfile, Node encapsulatingNode) {
        return classProfile.getJSClass(obj).set(obj, index, value, receiver, isStrict, encapsulatingNode);
    }

    @TruffleBoundary
    public static boolean delete(JSDynamicObject obj, long index) {
        return JSObject.getJSClass(obj).delete(obj, index, false);
    }

    @TruffleBoundary
    public static boolean delete(JSDynamicObject obj, long index, boolean isStrict) {
        return JSObject.getJSClass(obj).delete(obj, index, isStrict);
    }

    public static boolean delete(JSDynamicObject obj, long index, boolean isStrict, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).delete(obj, index, isStrict);
    }

    @TruffleBoundary
    public static boolean delete(JSDynamicObject obj, Object key) {
        return delete(obj, key, false);
    }

    @TruffleBoundary
    public static boolean delete(JSDynamicObject obj, Object key, boolean isStrict) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).delete(obj, key, isStrict);
    }

    public static boolean delete(JSDynamicObject obj, Object key, boolean isStrict, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).delete(obj, key, isStrict);
    }

    @TruffleBoundary
    public static boolean hasOwnProperty(JSDynamicObject obj, long index) {
        return JSObject.getJSClass(obj).hasOwnProperty(obj, index);
    }

    public static boolean hasOwnProperty(JSDynamicObject obj, long index, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).hasOwnProperty(obj, index);
    }

    @TruffleBoundary
    public static boolean hasOwnProperty(JSDynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).hasOwnProperty(obj, key);
    }

    public static boolean hasOwnProperty(JSDynamicObject obj, Object key, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).hasOwnProperty(obj, key);
    }

    @TruffleBoundary
    public static boolean hasProperty(JSDynamicObject obj, long index) {
        return JSObject.getJSClass(obj).hasProperty(obj, index);
    }

    public static boolean hasProperty(JSDynamicObject obj, long index, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).hasProperty(obj, index);
    }

    @TruffleBoundary
    public static boolean hasProperty(JSDynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).hasProperty(obj, key);
    }

    public static boolean hasProperty(JSDynamicObject obj, Object key, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).hasProperty(obj, key);
    }

    public static PropertyDescriptor getOwnProperty(JSDynamicObject obj, Object key) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).getOwnProperty(obj, key);
    }

    public static PropertyDescriptor getOwnProperty(JSDynamicObject obj, Object key, JSClassProfile classProfile) {
        assert JSRuntime.isPropertyKey(key);
        return classProfile.getJSClass(obj).getOwnProperty(obj, key);
    }

    /**
     * [[OwnPropertyKeys]]. The returned keys are instanceof (String, Symbol).
     */
    public static List<Object> ownPropertyKeys(JSDynamicObject obj) {
        return JSObject.getJSClass(obj).ownPropertyKeys(obj);
    }

    public static List<Object> ownPropertyKeys(JSDynamicObject obj, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).ownPropertyKeys(obj);
    }

    /**
     * 7.3.21 EnumerableOwnNames (O).
     */
    @TruffleBoundary
    public static List<TruffleString> enumerableOwnNames(JSDynamicObject thisObj) {
        JSClass jsclass = JSObject.getJSClass(thisObj);
        if (JSConfig.FastOwnKeys && jsclass.hasOnlyShapeProperties(thisObj)) {
            return JSShape.getEnumerablePropertyNames(thisObj.getShape());
        }
        Iterable<Object> ownKeys = jsclass.ownPropertyKeys(thisObj);
        List<TruffleString> names = new ArrayList<>();
        for (Object obj : ownKeys) {
            if (obj instanceof TruffleString name) {
                PropertyDescriptor desc = jsclass.getOwnProperty(thisObj, name);
                if (desc != null && desc.getEnumerable()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    @TruffleBoundary
    public static boolean defineOwnProperty(JSDynamicObject obj, Object key, PropertyDescriptor desc) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).defineOwnProperty(obj, key, desc, false);
    }

    @TruffleBoundary
    public static boolean defineOwnProperty(JSDynamicObject obj, Object key, PropertyDescriptor desc, boolean doThrow) {
        assert JSRuntime.isPropertyKey(key);
        return JSObject.getJSClass(obj).defineOwnProperty(obj, key, desc, doThrow);
    }

    public static Object get(JSDynamicObject obj, Object key, JSClassProfile jsclassProfile) {
        assert JSRuntime.isPropertyKey(key);
        return jsclassProfile.getJSClass(obj).get(obj, key);
    }

    public static Object get(JSDynamicObject obj, long index, JSClassProfile jsclassProfile) {
        return jsclassProfile.getJSClass(obj).get(obj, index);
    }

    public static Object getOrDefault(JSDynamicObject obj, Object key, Object receiver, Object defaultValue, JSClassProfile jsclassProfile, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        Object result = jsclassProfile.getJSClass(obj).getHelper(obj, receiver, key, encapsulatingNode);
        return result == null ? defaultValue : result;
    }

    public static Object getOrDefault(JSDynamicObject obj, long index, Object receiver, Object defaultValue, JSClassProfile jsclassProfile, Node encapsulatingNode) {
        Object result = jsclassProfile.getJSClass(obj).getHelper(obj, receiver, index, encapsulatingNode);
        return result == null ? defaultValue : result;
    }

    public static Object getOrDefault(JSDynamicObject obj, Object key, Object receiver, Object defaultValue) {
        return getOrDefault(obj, key, receiver, defaultValue, JSClassProfile.getUncached(), null);
    }

    public static Object getOrDefault(JSDynamicObject obj, long index, Object receiver, Object defaultValue) {
        return getOrDefault(obj, index, receiver, defaultValue, JSClassProfile.getUncached(), null);
    }

    @TruffleBoundary
    public static Object getWithReceiver(JSDynamicObject obj, Object key, Object receiver, Node encapsulatingNode) {
        assert JSRuntime.isPropertyKey(key);
        Object result = getJSClass(obj).getHelper(obj, receiver, key, encapsulatingNode);
        return result == null ? Undefined.instance : result;
    }

    @TruffleBoundary
    public static TruffleString defaultToString(JSDynamicObject obj) {
        return obj.defaultToString();
    }

    /**
     * Returns builtinTag as per Object.prototype.toString(). By default returns "Object".
     *
     * @return built-in toStringTag
     */
    @Override
    public TruffleString getBuiltinToStringTag() {
        return Strings.UC_OBJECT;
    }

    /**
     * ES2015 7.1.1 ToPrimitive in case an Object is passed.
     */
    @TruffleBoundary
    public static Object toPrimitive(JSDynamicObject obj, JSToPrimitiveNode.Hint hint) {
        assert obj != Null.instance && obj != Undefined.instance;
        Object exoticToPrim = JSObject.getMethod(obj, Symbol.SYMBOL_TO_PRIMITIVE);
        if (exoticToPrim != Undefined.instance) {
            Object result = JSRuntime.call(exoticToPrim, obj, new Object[]{hint.getHintName()});
            if (JSRuntime.isObject(result)) {
                throw Errors.createTypeError("[Symbol.toPrimitive] method returned a non-primitive object");
            }
            return result;
        }
        return ordinaryToPrimitive(obj, hint == JSToPrimitiveNode.Hint.Default ? JSToPrimitiveNode.Hint.Number : hint);
    }

    @TruffleBoundary
    public static Object toPrimitive(JSDynamicObject obj) {
        return toPrimitive(obj, JSToPrimitiveNode.Hint.Default);
    }

    /**
     * ES2018 7.1.1.1 OrdinaryToPrimitive.
     */
    @TruffleBoundary
    public static Object ordinaryToPrimitive(JSDynamicObject obj, JSToPrimitiveNode.Hint hint) {
        assert JSRuntime.isObject(obj);
        assert hint == JSToPrimitiveNode.Hint.String || hint == JSToPrimitiveNode.Hint.Number;
        Object[] methodNames;
        if (hint == JSToPrimitiveNode.Hint.String) {
            methodNames = new Object[]{Strings.TO_STRING, Strings.VALUE_OF};
        } else {
            methodNames = new Object[]{Strings.VALUE_OF, Strings.TO_STRING};
        }
        for (Object name : methodNames) {
            Object method = JSObject.getMethod(obj, name);
            if (JSRuntime.isCallable(method)) {
                Object result = JSRuntime.call(method, obj, new Object[]{});
                if (!JSRuntime.isObject(result)) {
                    return result;
                }
            }
        }
        throw Errors.createTypeErrorCannotConvertToPrimitiveValue();
    }

    public static boolean isExtensible(JSDynamicObject obj) {
        if (obj instanceof JSNonProxyObject nonProxyObject) {
            // fast path: only need to check shape flag.
            return nonProxyObject.isExtensible();
        }
        // slow path for Proxy, JSAdapter, and nullish values.
        return isExtensibleSlow(obj);
    }

    @TruffleBoundary
    private static boolean isExtensibleSlow(JSDynamicObject obj) {
        return obj.isExtensible();
    }

    public static boolean isExtensible(JSDynamicObject obj, JSClassProfile classProfile) {
        return classProfile.getJSClass(obj).isExtensible(obj);
    }

    /**
     * The property [[Class]] of the object. This is the second part of what
     * Object.prototype.toString.call(myObj) returns, e.g. "[object Array]".
     *
     * @return the internal property [[Class]] of the object.
     */
    @TruffleBoundary
    public static TruffleString getClassName(JSDynamicObject obj) {
        return obj.getClassName();
    }

    @NeverDefault
    public static ScriptArray getArray(JSDynamicObject obj) {
        assert hasArray(obj);
        if (obj instanceof JSArrayBase) {
            return ((JSArrayBase) obj).getArrayType();
        } else {
            return ((JSTypedArrayObject) obj).getArrayType();
        }
    }

    public static void setArray(JSDynamicObject obj, ScriptArray array) {
        assert hasArray(obj);
        ((JSArrayBase) obj).setArrayType(array);
    }

    public static boolean hasArray(Object obj) {
        return JSArray.isJSArray(obj) || JSArgumentsArray.isJSArgumentsObject(obj) || JSArrayBufferView.isJSArrayBufferView(obj) || JSObjectPrototype.isJSObjectPrototype(obj);
    }

    public static JSContext getJSContext(JSDynamicObject obj) {
        return JSShape.getJSContext(obj.getShape());
    }

}
