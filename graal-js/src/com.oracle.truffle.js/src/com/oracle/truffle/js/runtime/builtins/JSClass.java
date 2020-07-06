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
package com.oracle.truffle.js.runtime.builtins;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage.LanguageReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInstantiateNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInvokeNode;
import com.oracle.truffle.js.nodes.interop.KeyInfoNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropArray;
import com.oracle.truffle.js.runtime.truffleinterop.InteropFunction;
import com.oracle.truffle.js.runtime.truffleinterop.JSMetaType;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * Basic interface for all JavaScript "classes". A JSClass defines the internal and access methods
 * of a JSObject and allows for overriding their behavior for different types of objects.
 *
 * See also ECMA 8.6.2 "Object Internal Properties and Methods" for a list of internal properties
 * and methods.
 *
 * <pre>
 * Implementation notes:
 * - keep parameter order consistent: JSObject receiver[, the rest...].
 * - keep interface clean, avoid redundant methods, maximize consistency with JSObject and ECMAScript
 * </pre>
 */
@ExportLibrary(value = InteropLibrary.class, receiverType = DynamicObject.class)
public abstract class JSClass extends ObjectType {
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected JSClass() {
    }

    /**
     * 9.1.1 [[GetPrototypeOf]] ().
     */
    @TruffleBoundary
    public abstract DynamicObject getPrototypeOf(DynamicObject thisObj);

    /**
     * 9.1.2 [[SetPrototypeOf]] (V).
     */
    @TruffleBoundary
    public abstract boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype);

    /**
     * 9.1.3 [[IsExtensible]] ().
     */
    @TruffleBoundary
    public abstract boolean isExtensible(DynamicObject thisObj);

    /**
     * 9.1.4 [[PreventExtensions]] ().
     */
    @TruffleBoundary
    public abstract boolean preventExtensions(DynamicObject thisObj, boolean doThrow);

    /**
     * 9.1.5 [[GetOwnProperty]] (P).
     */
    @TruffleBoundary
    public abstract PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key);

    /**
     * 9.1.6 [[DefineOwnProperty]] (P, Desc).
     */
    @TruffleBoundary
    public abstract boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor value, boolean doThrow);

    /**
     * 9.1.7 [[HasProperty]] (P).
     */
    @TruffleBoundary
    public abstract boolean hasProperty(DynamicObject thisObj, Object key);

    @TruffleBoundary
    public abstract boolean hasProperty(DynamicObject thisObj, long index);

    @TruffleBoundary
    public abstract boolean hasOwnProperty(DynamicObject thisObj, Object key);

    @TruffleBoundary
    public abstract boolean hasOwnProperty(DynamicObject thisObj, long index);

    /**
     * 9.1.8 [[Get]] (P, Receiver).
     */
    public final Object get(DynamicObject thisObj, Object key) {
        return JSRuntime.nullToUndefined(getHelper(thisObj, thisObj, key));
    }

    public Object get(DynamicObject thisObj, long index) {
        return JSRuntime.nullToUndefined(getHelper(thisObj, thisObj, index));
    }

    @TruffleBoundary
    public abstract Object getHelper(DynamicObject store, Object thisObj, Object key);

    @TruffleBoundary
    public abstract Object getHelper(DynamicObject store, Object thisObj, long index);

    @TruffleBoundary
    public abstract Object getOwnHelper(DynamicObject store, Object thisObj, Object key);

    @TruffleBoundary
    public abstract Object getOwnHelper(DynamicObject store, Object thisObj, long index);

    @TruffleBoundary
    public abstract Object getMethodHelper(DynamicObject store, Object thisObj, Object key);

    /**
     * 9.1.9 [[Set]] (P, V, Receiver).
     */
    @TruffleBoundary
    public abstract boolean set(DynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict);

    @TruffleBoundary
    public abstract boolean set(DynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict);

    /**
     * 9.1.10 [[Delete]] (P).
     */
    @TruffleBoundary
    public abstract boolean delete(DynamicObject thisObj, Object key, boolean isStrict);

    @TruffleBoundary
    public abstract boolean delete(DynamicObject thisObj, long index, boolean isStrict);

    /**
     * 9.1.12 [[OwnPropertyKeys]]().
     *
     * Provides all <em>own</em> properties of this object with a <em>String</em> or <em>Symbol</em>
     * key. Represents the [[OwnPropertyKeys]] internal method.
     *
     * @return a List of the keys of all own properties of that object
     */
    public final List<Object> ownPropertyKeys(DynamicObject obj) {
        return getOwnPropertyKeys(obj, true, true);
    }

    /**
     * GetOwnPropertyKeys (O, type).
     *
     * @return a List of the keys of all own properties of that object with the specified types
     */
    @TruffleBoundary
    public abstract List<Object> getOwnPropertyKeys(DynamicObject obj, boolean strings, boolean symbols);

    @TruffleBoundary
    public static List<Object> filterOwnPropertyKeys(List<Object> ownPropertyKeys, boolean strings, boolean symbols) {
        if (strings && symbols) {
            return ownPropertyKeys;
        }
        List<Object> names = new ArrayList<>();
        for (Object key : ownPropertyKeys) {
            if ((!symbols && key instanceof Symbol) || (!strings && key instanceof String)) {
                continue;
            }
            names.add(key);
        }
        return names;
    }

    /**
     * If true, {@link #ownPropertyKeys} and {@link JSShape#getProperties} enumerate the same keys.
     */
    @TruffleBoundary
    public abstract boolean hasOnlyShapeProperties(DynamicObject obj);

    /**
     * The [[Class]] internal property.
     *
     * For ES5, this is the second part of what Object.prototype.toString.call(myObj) returns, e.g.
     * "[object Array]".
     *
     * @param object object to be used
     */
    @TruffleBoundary
    public abstract String getClassName(DynamicObject object);

    @Override
    @TruffleBoundary
    public abstract String toString();

    /**
     * Follows 19.1.3.6 Object.prototype.toString(), basically: "[object " + [[Symbol.toStringTag]]
     * + "]" or typically "[object Object]" (for non built-in types) if [[Symbol.toStringTag]] is
     * not present.
     * <p>
     * For ES5, if follows 15.2.4.2 Object.prototype.toString(), basically: "[object " + [[Class]] +
     * "]".
     *
     * @see #getBuiltinToStringTag(DynamicObject)
     */
    @TruffleBoundary
    public String defaultToString(DynamicObject object) {
        JSContext context = JSObject.getJSContext(object);
        if (context.getEcmaScriptVersion() <= 5) {
            return formatToString(getClassName(object));
        }
        String result = getToStringTag(object);
        return formatToString(result);
    }

    protected String getToStringTag(DynamicObject object) {
        String result = null;
        if (JSRuntime.isObject(object)) {
            Object toStringTag = JSObject.get(object, Symbol.SYMBOL_TO_STRING_TAG);
            if (JSRuntime.isString(toStringTag)) {
                result = JSRuntime.toStringIsString(toStringTag);
            }
        }
        if (result == null) {
            result = getBuiltinToStringTag(object);
        }
        return result;
    }

    /**
     * Returns builtinTag from step 14 of ES6+ 19.1.3.6. By default returns "Object".
     *
     * @param object object to be used
     * @return "Object" by default
     * @see #defaultToString(DynamicObject)
     */
    @TruffleBoundary
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }

    /**
     * Formats {@link #defaultToString(DynamicObject)}, by default returns "[object ...]".
     *
     * @param object object to be used
     * @return "[object ...]" by default
     */
    @TruffleBoundary
    protected String formatToString(String object) {
        return "[object " + object + "]";
    }

    /**
     * A more informative but side-effect-free toString variant, mainly used for error messages.
     *
     * @param depth allowed nesting depth
     * @param context the current language context
     */
    @TruffleBoundary
    public abstract String safeToString(DynamicObject object, int depth, JSContext context);

    public final boolean isInstance(DynamicObject object) {
        return isInstance(object, this);
    }

    public final boolean isInstance(Object object) {
        return isInstance(object, this);
    }

    public static boolean isInstance(Object object, JSClass jsclass) {
        return JSObject.isDynamicObject(object) && isInstance((DynamicObject) object, jsclass);
    }

    public static boolean isInstance(DynamicObject object, JSClass jsclass) {
        return object.getShape().getObjectType() == jsclass;
    }

    /**
     * ES2015 7.3.15 TestIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean testIntegrityLevel(DynamicObject obj, boolean frozen) {
        assert JSRuntime.isObject(obj);
        boolean status = isExtensible(obj);
        if (status) {
            return false;
        }
        for (Object key : ownPropertyKeys(obj)) {
            PropertyDescriptor desc = getOwnProperty(obj, key);
            if (desc != null) {
                if (desc.getConfigurable()) {
                    return false;
                }
                if (frozen && desc.isDataDescriptor() && desc.getWritable()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This class is used to break the class initialization cycle JSClass -> PropertyDescriptor ->
     * Undefined -> Null -> NullClass -> AbstractJSClass -> JSClass.
     */
    private static final class FreezeHolder {
        private static final PropertyDescriptor FREEZE_ACC_DESC;
        private static final PropertyDescriptor FREEZE_DATA_DESC;

        static {
            FREEZE_ACC_DESC = PropertyDescriptor.createEmpty();
            FREEZE_ACC_DESC.setConfigurable(false);

            FREEZE_DATA_DESC = PropertyDescriptor.createEmpty();
            FREEZE_DATA_DESC.setConfigurable(false);
            FREEZE_DATA_DESC.setWritable(false);
        }
    }

    /**
     * ES2015 7.3.14 SetIntegrityLevel(O, level).
     */
    @TruffleBoundary
    public boolean setIntegrityLevel(DynamicObject obj, boolean freeze, boolean doThrow) {
        assert JSRuntime.isObject(obj);
        if (!preventExtensions(obj, doThrow)) {
            return false;
        }
        Iterable<Object> keys = ownPropertyKeys(obj);
        if (freeze) {
            // FREEZE
            for (Object key : keys) {
                PropertyDescriptor currentDesc = getOwnProperty(obj, key);
                if (currentDesc != null) {
                    PropertyDescriptor newDesc = null;
                    if (currentDesc.isAccessorDescriptor()) {
                        newDesc = FreezeHolder.FREEZE_ACC_DESC;
                    } else {
                        newDesc = FreezeHolder.FREEZE_DATA_DESC;
                    }
                    JSRuntime.definePropertyOrThrow(obj, key, newDesc);
                }
            }
        } else {
            // SEAL
            for (Object key : keys) {
                JSRuntime.definePropertyOrThrow(obj, key, FreezeHolder.FREEZE_ACC_DESC);
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        throw Errors.shouldNotReachHere(getClass().getName());
    }

    public abstract boolean usesOrdinaryGetOwnProperty();

    public abstract boolean usesOrdinaryIsExtensible();

    @Override
    public final Class<?> dispatch() {
        return JSClass.class;
    }

    @ExportMessage
    static boolean isNull(DynamicObject target) {
        return JSShape.getJSClassNoCast(target.getShape()) == Null.NULL_CLASS;
    }

    @ExportMessage
    static boolean hasMembers(DynamicObject target) {
        return JSRuntime.isObject(target);
    }

    private static void ensureHasMembers(DynamicObject target) throws UnsupportedMessageException {
        if (!hasMembers(target)) {
            throw UnsupportedMessageException.create();
        }
    }

    @SuppressWarnings("unused")
    @ImportStatic({JSGuards.class, JSObject.class})
    @ExportMessage
    abstract static class GetMembers {
        @Specialization(guards = "isJSFastArray(target)")
        static Object fastArray(DynamicObject target, boolean internal) {
            // Do not include array indices
            return InteropArray.create(filterEnumerableNames(target, JSBuiltinObject.ordinaryOwnPropertyKeys(target), JSObject.getJSClass(target)));
        }

        @Specialization(guards = {"isJSArray(target)", "!isJSFastArray(target)"})
        static Object slowArray(DynamicObject target, boolean internal) {
            // Do not include array indices
            return InteropArray.create(filterEnumerableNames(target, JSObject.ownPropertyKeys(target), JSObject.getJSClass(target)));
        }

        @Specialization(guards = "isJSArrayBufferView(target)")
        static Object typedArray(DynamicObject target, boolean internal) {
            return fastArray(target, internal);
        }

        @Specialization(guards = "isJSArgumentsObject(target)")
        static Object argumentsObject(DynamicObject target, boolean internal) {
            return slowArray(target, internal);
        }

        @Specialization(guards = {"cachedJSClass != null", "getJSClass(target) == cachedJSClass"})
        static Object nonArrayCached(DynamicObject target, boolean internal,
                        @Cached("getNonArrayJSClass(target)") JSClass cachedJSClass) throws UnsupportedMessageException {
            return InteropArray.create(JSObject.enumerableOwnNames(target));
        }

        @Specialization(guards = {"!isJSArray(target)", "!isJSArrayBufferView(target)", "!isJSArgumentsObject(target)"}, replaces = "nonArrayCached")
        static Object nonArray(DynamicObject target, boolean internal) throws UnsupportedMessageException {
            ensureHasMembers(target);
            return InteropArray.create(JSObject.enumerableOwnNames(target));
        }

        @TruffleBoundary
        private static String[] filterEnumerableNames(DynamicObject target, Iterable<Object> ownKeys, JSClass jsclass) {
            List<String> names = new ArrayList<>();
            for (Object obj : ownKeys) {
                if (obj instanceof String && !JSRuntime.isArrayIndex((String) obj)) {
                    PropertyDescriptor desc = jsclass.getOwnProperty(target, obj);
                    if (desc != null && desc.getEnumerable()) {
                        names.add((String) obj);
                    }
                }
            }
            return names.toArray(EMPTY_STRING_ARRAY);
        }

        static JSClass getNonArrayJSClass(DynamicObject object) {
            if (hasArrayElements(object) || isNull(object)) {
                return null;
            }
            return JSObject.getJSClass(object);
        }
    }

    @ExportMessage
    static Object readMember(DynamicObject target, String key,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "create(languageRef.get().getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Cached(value = "languageRef.get().bindMemberFunctions()", allowUncached = true) boolean bindMemberFunctions,
                    @Cached @Exclusive ExportValueNode exportNode) throws UnknownIdentifierException, UnsupportedMessageException {
        ensureHasMembers(target);
        Object result;
        if (readNode == null) {
            result = JSObject.getOrDefault(target, key, target, null, JSClassProfile.getUncached());
        } else {
            result = readNode.executeWithTargetAndIndexOrDefault(target, key, null);
        }
        if (result == null) {
            throw UnknownIdentifierException.create(key);
        }
        return exportNode.execute(result, target, bindMemberFunctions);
    }

    @ExportMessage
    static boolean isMemberReadable(DynamicObject target, String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        if (!hasMembers(target)) {
            return false;
        }
        return keyInfo.execute(target, key, KeyInfoNode.READABLE);
    }

    @ExportMessage
    static void writeMember(DynamicObject target, String key, Object value,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo,
                    @Shared("importValue") @Cached ImportValueNode castValueNode,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "createCachedInterop(languageRef)", uncached = "getUncachedWrite()") WriteElementNode writeNode)
                    throws UnknownIdentifierException, UnsupportedMessageException {
        ensureHasMembers(target);
        if (!keyInfo.execute(target, key, KeyInfoNode.WRITABLE)) {
            throw UnknownIdentifierException.create(key);
        }
        Object importedValue = castValueNode.executeWithTarget(value);
        if (writeNode == null) {
            JSObject.set(target, key, importedValue, true);
        } else {
            writeNode.executeWithTargetAndIndexAndValue(target, key, importedValue);
        }
    }

    @ExportMessage
    static boolean isMemberModifiable(DynamicObject target, String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        if (!hasMembers(target)) {
            return false;
        }
        return keyInfo.execute(target, key, KeyInfoNode.MODIFIABLE);
    }

    @ExportMessage
    static boolean isMemberInsertable(DynamicObject target, String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        if (!hasMembers(target)) {
            return false;
        }
        return keyInfo.execute(target, key, KeyInfoNode.INSERTABLE);
    }

    @ExportMessage
    static void removeMember(DynamicObject target, String key) throws UnsupportedMessageException {
        ensureHasMembers(target);
        JSObject.delete(target, key, true);
    }

    @ExportMessage
    static boolean isMemberRemovable(DynamicObject target, String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        if (!hasMembers(target)) {
            return false;
        }
        return keyInfo.execute(target, key, KeyInfoNode.REMOVABLE);
    }

    @ExportMessage
    static boolean hasArrayElements(DynamicObject target) {
        ObjectType objectType = JSShape.getJSClassNoCast(target.getShape());
        return objectType instanceof JSAbstractArray || objectType instanceof JSArrayBufferView;
    }

    @ImportStatic({JSGuards.class})
    @ExportMessage
    abstract static class GetArraySize {
        @Specialization(guards = "isJSArray(target)")
        static long array(DynamicObject target) {
            return JSArray.arrayGetLength(target);
        }

        @Specialization(guards = "isJSArrayBufferView(target)")
        static long typedArray(DynamicObject target) {
            return JSArrayBufferView.typedArrayGetLength(target);
        }

        @Specialization(guards = "isJSArgumentsObject(target)")
        static long argumentsObject(DynamicObject target) {
            return JSRuntime.toInteger(JSObject.get(target, JSAbstractArray.LENGTH));
        }

        @Fallback
        static long unsupported(@SuppressWarnings("unused") DynamicObject target) throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static Object readArrayElement(DynamicObject target, long index,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "create(languageRef.get().getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Shared("exportValue") @Cached ExportValueNode exportNode,
                    @CachedLibrary("target") InteropLibrary thisLibrary) throws InvalidArrayIndexException, UnsupportedMessageException {
        if (!hasArrayElements(target)) {
            throw UnsupportedMessageException.create();
        }
        if (index < 0 || index >= thisLibrary.getArraySize(target)) {
            throw InvalidArrayIndexException.create(index);
        }
        Object result;
        if (readNode == null) {
            result = JSObject.getOrDefault(target, index, target, Undefined.instance, JSClassProfile.getUncached());
        } else {
            result = readNode.executeWithTargetAndIndexOrDefault(target, index, Undefined.instance);
        }
        return exportNode.execute(result);
    }

    @ExportMessage
    static boolean isArrayElementReadable(DynamicObject target, long index,
                    @CachedLibrary("target") InteropLibrary thisLibrary) {
        try {
            return hasArrayElements(target) && (index >= 0 && index < thisLibrary.getArraySize(target));
        } catch (UnsupportedMessageException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    @ExportMessage
    static void writeArrayElement(DynamicObject target, long index, Object value,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo,
                    @Shared("importValue") @Cached ImportValueNode castValueNode,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "createCachedInterop(languageRef)", uncached = "getUncachedWrite()") WriteElementNode writeNode) throws InvalidArrayIndexException, UnsupportedMessageException {
        if (!hasArrayElements(target)) {
            throw UnsupportedMessageException.create();
        }
        if (!keyInfo.execute(target, index, KeyInfoNode.WRITABLE)) {
            throw InvalidArrayIndexException.create(index);
        }
        Object importedValue = castValueNode.executeWithTarget(value);
        if (writeNode == null) {
            JSObject.set(target, index, importedValue, true);
        } else {
            writeNode.executeWithTargetAndIndexAndValue(target, index, importedValue);
        }
    }

    @ExportMessage
    static boolean isArrayElementModifiable(DynamicObject target, long index,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return hasArrayElements(target) && keyInfo.execute(target, index, KeyInfoNode.MODIFIABLE);
    }

    @ExportMessage
    static boolean isArrayElementInsertable(DynamicObject target, long index,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return hasArrayElements(target) && keyInfo.execute(target, index, KeyInfoNode.INSERTABLE);
    }

    @ExportMessage
    static boolean isArrayElementRemovable(DynamicObject target, long index) {
        if (JSArray.isJSArray(target)) {
            ScriptArray arrayType = JSObject.getArray(target);
            if (!arrayType.isSealed() && !arrayType.isLengthNotWritable()) {
                long length = arrayType.length(target);
                if (index >= 0 && index < length) {
                    return true;
                }
            }
        }
        return false;
    }

    @ExportMessage
    static void removeArrayElement(DynamicObject target, long index) throws UnsupportedMessageException, InvalidArrayIndexException {
        if (JSArray.isJSArray(target)) {
            ScriptArray arrayType = JSObject.getArray(target);
            if (!arrayType.isSealed() && !arrayType.isLengthNotWritable()) {
                long length = arrayType.length(target);
                if (index >= 0 && index < length) {
                    arrayType = arrayType.removeRange(target, index, index + 1);
                    JSObject.setArray(target, arrayType);
                    arrayType = arrayType.setLength(target, length - 1, true);
                    JSObject.setArray(target, arrayType);
                    return;
                } else {
                    throw InvalidArrayIndexException.create(index);
                }
            }
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    static Object execute(DynamicObject target, Object[] args,
                    @CachedLanguage JavaScriptLanguage language,
                    @CachedContext(JavaScriptLanguage.class) JSRealm realm,
                    @Cached JSInteropExecuteNode callNode,
                    @Shared("exportValue") @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(target, Undefined.instance, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    static boolean isExecutable(DynamicObject target,
                    @Cached IsCallableNode isCallable) {
        return isCallable.executeBoolean(target);
    }

    @ExportMessage
    static Object instantiate(DynamicObject target, Object[] args,
                    @CachedLanguage JavaScriptLanguage language,
                    @CachedContext(JavaScriptLanguage.class) JSRealm realm,
                    @Cached JSInteropInstantiateNode callNode,
                    @Shared("exportValue") @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(target, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    static boolean isInstantiable(DynamicObject target) {
        return JSRuntime.isConstructor(target);
    }

    @ExportMessage
    static Object invokeMember(DynamicObject target, String id, Object[] args,
                    @CachedLanguage JavaScriptLanguage language,
                    @CachedContext(JavaScriptLanguage.class) JSRealm realm,
                    @Cached JSInteropInvokeNode callNode,
                    @Shared("exportValue") @Cached ExportValueNode exportNode) throws UnsupportedMessageException, UnknownIdentifierException {
        ensureHasMembers(target);
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(target, id, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    static boolean isMemberInvocable(DynamicObject target, String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        if (!hasMembers(target)) {
            return false;
        }
        return keyInfo.execute(target, key, KeyInfoNode.INVOCABLE);
    }

    @ExportMessage
    static boolean hasMemberReadSideEffects(DynamicObject target, String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        if (!hasMembers(target)) {
            return false;
        }
        return keyInfo.execute(target, key, KeyInfoNode.READ_SIDE_EFFECTS);
    }

    @ExportMessage
    static boolean hasMemberWriteSideEffects(DynamicObject target, String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        if (!hasMembers(target)) {
            return false;
        }
        return keyInfo.execute(target, key, KeyInfoNode.WRITE_SIDE_EFFECTS);
    }

    @ExportMessage
    static boolean isString(DynamicObject target) {
        JSClass builtinClass = JSObject.getJSClass(target);
        if (builtinClass == JSString.INSTANCE) {
            return true;
        }
        return false;
    }

    @ExportMessage
    static String asString(DynamicObject target) throws UnsupportedMessageException {
        JSClass builtinClass = JSObject.getJSClass(target);
        if (builtinClass == JSString.INSTANCE) {
            return JSString.getString(target);
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    static boolean isBoolean(DynamicObject target) {
        JSClass builtinClass = JSObject.getJSClass(target);
        if (builtinClass == JSBoolean.INSTANCE) {
            return true;
        }
        return false;
    }

    @ExportMessage
    static boolean asBoolean(DynamicObject target) throws UnsupportedMessageException {
        JSClass builtinClass = JSObject.getJSClass(target);
        if (builtinClass == JSBoolean.INSTANCE) {
            return JSBoolean.valueOf(target);
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    static boolean isNumber(DynamicObject target) {
        JSClass builtinClass = JSObject.getJSClass(target);
        if (builtinClass == JSNumber.INSTANCE) {
            return true;
        }
        return false;
    }

    @ExportMessage
    static boolean fitsInByte(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return isNumber(target) && numberLib.fitsInByte(JSNumber.valueOf(target));
    }

    @ExportMessage
    static boolean fitsInShort(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return isNumber(target) && numberLib.fitsInShort(JSNumber.valueOf(target));
    }

    @ExportMessage
    static boolean fitsInInt(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return isNumber(target) && numberLib.fitsInInt(JSNumber.valueOf(target));
    }

    @ExportMessage
    static boolean fitsInLong(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return isNumber(target) && numberLib.fitsInLong(JSNumber.valueOf(target));
    }

    @ExportMessage
    static boolean fitsInFloat(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return isNumber(target) && numberLib.fitsInFloat(JSNumber.valueOf(target));
    }

    @ExportMessage
    static boolean fitsInDouble(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) {
        return isNumber(target) && numberLib.fitsInDouble(JSNumber.valueOf(target));
    }

    @ExportMessage
    static byte asByte(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInByte(target, numberLib)) {
            return numberLib.asByte(JSNumber.valueOf(target));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static short asShort(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInShort(target, numberLib)) {
            return numberLib.asShort(JSNumber.valueOf(target));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static int asInt(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInInt(target, numberLib)) {
            return numberLib.asInt(JSNumber.valueOf(target));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static long asLong(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInLong(target, numberLib)) {
            return numberLib.asLong(JSNumber.valueOf(target));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static float asFloat(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInFloat(target, numberLib)) {
            return numberLib.asFloat(JSNumber.valueOf(target));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static double asDouble(DynamicObject target,
                    @Shared("numberLib") @CachedLibrary(limit = "1") InteropLibrary numberLib) throws UnsupportedMessageException {
        if (fitsInDouble(target, numberLib)) {
            return numberLib.asDouble(JSNumber.valueOf(target));
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage(name = "isDate")
    @ExportMessage(name = "isTime")
    @ExportMessage(name = "isTimeZone")
    static boolean isDate(DynamicObject target) {
        return JSDate.isValidDate(target);
    }

    @ExportMessage
    static Instant asInstant(DynamicObject target) throws UnsupportedMessageException {
        if (isDate(target)) {
            return JSDate.asInstant(target);
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static LocalDate asDate(DynamicObject target,
                    @CachedContext(JavaScriptLanguage.class) ContextReference<JSRealm> contextRef) throws UnsupportedMessageException {
        if (isDate(target)) {
            return JSDate.asLocalDate(target, contextRef.get());
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static LocalTime asTime(DynamicObject target,
                    @CachedContext(JavaScriptLanguage.class) ContextReference<JSRealm> contextRef) throws UnsupportedMessageException {
        if (isDate(target)) {
            return JSDate.asLocalTime(target, contextRef.get());
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static ZoneId asTimeZone(DynamicObject target,
                    @CachedContext(JavaScriptLanguage.class) ContextReference<JSRealm> contextRef) throws UnsupportedMessageException {
        if (isDate(target)) {
            return contextRef.get().getLocalTimeZoneId();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    static boolean hasLanguage(@SuppressWarnings("unused") DynamicObject receiver) {
        return true;
    }

    @ExportMessage
    static Class<? extends TruffleLanguage<?>> getLanguage(@SuppressWarnings("unused") DynamicObject receiver) {
        return JavaScriptLanguage.class;
    }

    @ExportMessage
    static Object toDisplayString(DynamicObject receiver, @SuppressWarnings("unused") boolean allowSideEffects) {
        return JSRuntime.safeToString(receiver);
    }

    @ExportMessage
    static boolean hasSourceLocation(DynamicObject receiver) {
        return getSourceLocationImpl(receiver) != null;
    }

    @ExportMessage
    static SourceSection getSourceLocation(DynamicObject receiver) throws UnsupportedMessageException {
        SourceSection sourceSection = getSourceLocationImpl(receiver);
        if (sourceSection == null) {
            throw UnsupportedMessageException.create();
        }
        return sourceSection;
    }

    @TruffleBoundary
    private static SourceSection getSourceLocationImpl(DynamicObject receiver) {
        if (JSFunction.isJSFunction(receiver)) {
            DynamicObject func = receiver;
            CallTarget ct = JSFunction.getCallTarget(func);
            if (JSFunction.isBoundFunction(func)) {
                func = JSFunction.getBoundTargetFunction(func);
                ct = JSFunction.getCallTarget(func);
            }

            if (ct instanceof RootCallTarget) {
                return ((RootCallTarget) ct).getRootNode().getSourceSection();
            }
        }
        return null;
    }

    @ExportMessage
    static boolean hasMetaObject(DynamicObject receiver) {
        return getMetaObjectImpl(receiver) != null;
    }

    @ExportMessage
    static Object getMetaObject(DynamicObject receiver) throws UnsupportedMessageException {
        Object metaObject = getMetaObjectImpl(receiver);
        if (metaObject != null) {
            return metaObject;
        }
        throw UnsupportedMessageException.create();
    }

    @TruffleBoundary
    private static Object getMetaObjectImpl(DynamicObject receiver) {
        if (JSGuards.isJSNull(receiver)) {
            return JSMetaType.JS_NULL;
        } else if (JSGuards.isUndefined(receiver)) {
            return JSMetaType.JS_UNDEFINED;
        } else if (JSGuards.isJSProxy(receiver)) {
            return JSMetaType.JS_PROXY;
        } else {
            assert JSObject.isJSObject(receiver) && !JSGuards.isJSProxy(receiver);
            DynamicObject proto = JSObject.getPrototype(receiver);
            Object metaObject = JSRuntime.getDataProperty(proto, JSObject.CONSTRUCTOR);
            if (metaObject != null && metaObject instanceof DynamicObject && isMetaObject((DynamicObject) metaObject)) {
                return metaObject;
            }
        }
        return null;
    }

    @ExportMessage
    static boolean isMetaObject(DynamicObject receiver) {
        return JSFunction.isJSFunction(receiver);
    }

    @TruffleBoundary
    @ExportMessage(name = "getMetaQualifiedName")
    @ExportMessage(name = "getMetaSimpleName")
    static Object getMetaObjectName(DynamicObject receiver) throws UnsupportedMessageException {
        if (isMetaObject(receiver)) {
            Object name = JSRuntime.getDataProperty(receiver, JSFunction.NAME);
            if (JSRuntime.isString(name)) {
                return JSRuntime.javaToString(name);
            }
            return "";
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @TruffleBoundary
    @ExportMessage
    static boolean isMetaInstance(DynamicObject receiver, Object instance) throws UnsupportedMessageException {
        if (!isMetaObject(receiver)) {
            throw UnsupportedMessageException.create();
        }
        Object constructorPrototype = JSRuntime.getDataProperty(receiver, JSObject.PROTOTYPE);
        if (JSGuards.isJSObject(constructorPrototype)) {
            Object obj = instance;
            if (obj instanceof InteropFunction) {
                obj = ((InteropFunction) obj).getFunction();
            }
            if (JSGuards.isJSObject(obj) && !JSProxy.isProxy(obj)) {
                DynamicObject proto = JSObject.getPrototype((DynamicObject) obj);
                while (proto != Null.instance) {
                    if (proto == constructorPrototype) {
                        return true;
                    }
                    if (JSProxy.isProxy(proto)) {
                        break;
                    }
                    proto = JSObject.getPrototype(proto);
                }
            }
        }
        return false;
    }

    @ExportMessage
    static final class IsIdenticalOrUndefined {
        @Specialization
        static TriState doHostObject(DynamicObject receiver, DynamicObject other) {
            return TriState.valueOf(receiver == other);
        }

        @SuppressWarnings("unused")
        @Fallback
        static TriState doOther(DynamicObject receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    @TruffleBoundary
    static int identityHashCode(DynamicObject receiver) {
        return System.identityHashCode(receiver);
    }

    static ReadElementNode getUncachedRead() {
        return null;
    }

    static WriteElementNode getUncachedWrite() {
        return null;
    }
}
