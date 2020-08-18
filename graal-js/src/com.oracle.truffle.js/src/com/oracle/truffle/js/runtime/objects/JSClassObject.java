/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.LanguageReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInvokeNode;
import com.oracle.truffle.js.nodes.interop.KeyInfoNode;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.truffleinterop.InteropArray;
import com.oracle.truffle.js.runtime.truffleinterop.JSMetaType;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

@ExportLibrary(InteropLibrary.class)
public abstract class JSClassObject extends JSObject {
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected JSClassObject(Shape shape) {
        super(shape);
    }

    protected JSClassObject(JSRealm realm, JSObjectFactory factory) {
        super(factory.getShape(realm));
        CompilerAsserts.partialEvaluationConstant(factory);
        factory.initProto(this, realm);
    }

    @Override
    public String getClassName() {
        return getJSClass().getClassName(this);
    }

    @Override
    public JSDynamicObject getPrototypeOf() {
        return (JSDynamicObject) getJSClass().getPrototypeOf(this);
    }

    @Override
    public boolean setPrototypeOf(JSDynamicObject newPrototype) {
        return getJSClass().setPrototypeOf(this, newPrototype);
    }

    @Override
    public boolean isExtensible() {
        return getJSClass().isExtensible(this);
    }

    @Override
    public boolean preventExtensions(boolean doThrow) {
        return getJSClass().preventExtensions(this, doThrow);
    }

    @Override
    public PropertyDescriptor getOwnProperty(Object propertyKey) {
        return getJSClass().getOwnProperty(this, propertyKey);
    }

    @Override
    public boolean defineOwnProperty(Object key, PropertyDescriptor value, boolean doThrow) {
        return getJSClass().defineOwnProperty(this, key, value, doThrow);
    }

    @Override
    public boolean hasProperty(Object key) {
        return getJSClass().hasProperty(this, key);
    }

    @Override
    public boolean hasProperty(long index) {
        return getJSClass().hasProperty(this, index);
    }

    @Override
    public boolean hasOwnProperty(Object key) {
        return getJSClass().hasOwnProperty(this, key);
    }

    @Override
    public boolean hasOwnProperty(long index) {
        return getJSClass().hasOwnProperty(this, index);
    }

    @Override
    public Object getHelper(Object receiver, Object key) {
        return getJSClass().getHelper(this, receiver, key);
    }

    @Override
    public Object getHelper(Object receiver, long index) {
        return getJSClass().getHelper(this, receiver, index);
    }

    @Override
    public Object getOwnHelper(Object receiver, Object key) {
        return getJSClass().getOwnHelper(this, receiver, key);
    }

    @Override
    public Object getOwnHelper(Object receiver, long index) {
        return getJSClass().getOwnHelper(this, receiver, index);
    }

    @Override
    public Object getMethodHelper(Object receiver, Object key) {
        return getJSClass().getMethodHelper(this, receiver, key);
    }

    @Override
    public boolean set(Object key, Object value, Object receiver, boolean isStrict) {
        return getJSClass().set(this, key, value, receiver, isStrict);
    }

    @Override
    public boolean set(long index, Object value, Object receiver, boolean isStrict) {
        return getJSClass().set(this, index, value, receiver, isStrict);
    }

    @Override
    public boolean delete(Object key, boolean isStrict) {
        return getJSClass().delete(this, key, isStrict);
    }

    @Override
    public boolean delete(long index, boolean isStrict) {
        return getJSClass().delete(this, index, isStrict);
    }

    @Override
    public List<Object> getOwnPropertyKeys(boolean strings, boolean symbols) {
        return getJSClass().getOwnPropertyKeys(this, strings, symbols);
    }

    @Override
    public boolean hasOnlyShapeProperties() {
        return getJSClass().hasOnlyShapeProperties(this);
    }

    @Override
    public String toDisplayStringImpl(int depth, boolean allowSideEffects) {
        return getJSClass().toDisplayStringImpl(this, depth, allowSideEffects, JSObject.getJSContext(this));
    }

    @Override
    public String getBuiltinToStringTag() {
        return getJSClass().getBuiltinToStringTag(this);
    }

    @Override
    public boolean setIntegrityLevel(boolean freeze, boolean doThrow) {
        return getJSClass().setIntegrityLevel(this, freeze, doThrow);
    }

    @Override
    public boolean testIntegrityLevel(boolean frozen) {
        return getJSClass().testIntegrityLevel(this, frozen);
    }

    @Override
    public String toString() {
        return getJSClass().toString(this);
    }

    // --- interop ---

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean hasMembers() {
        return true;
    }

    @ImportStatic({JSGuards.class, JSObject.class})
    @ExportMessage
    public abstract static class GetMembers {
        @Specialization(guards = {"cachedJSClass != null", "getJSClass(target) == cachedJSClass"})
        public static Object nonArrayCached(JSClassObject target, @SuppressWarnings("unused") boolean internal,
                        @Cached("getJSClass(target)") @SuppressWarnings("unused") JSClass cachedJSClass) {
            return InteropArray.create(JSObject.enumerableOwnNames(target));
        }

        @Specialization(replaces = "nonArrayCached")
        public static Object nonArrayUncached(JSClassObject target, @SuppressWarnings("unused") boolean internal) {
            return InteropArray.create(JSObject.enumerableOwnNames(target));
        }
    }

    @TruffleBoundary
    protected static String[] filterEnumerableNames(DynamicObject target, Iterable<Object> ownKeys, JSClass jsclass) {
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

    @ExportMessage
    public final Object readMember(String key,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "create(languageRef.get().getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                    @Cached(value = "languageRef.get().bindMemberFunctions()", allowUncached = true) boolean bindMemberFunctions,
                    @Cached @Exclusive ExportValueNode exportNode) throws UnknownIdentifierException {
        DynamicObject target = this;
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
    public final boolean isMemberReadable(String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.READABLE);
    }

    @ExportMessage
    public final void writeMember(String key, Object value,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo,
                    @Cached ImportValueNode castValueNode,
                    @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                    @Cached(value = "createCachedInterop(languageRef)", uncached = "getUncachedWrite()") WriteElementNode writeNode)
                    throws UnknownIdentifierException, UnsupportedMessageException {
        DynamicObject target = this;
        if (testIntegrityLevel(true)) {
            throw UnsupportedMessageException.create();
        }
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
    public final boolean isMemberModifiable(String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.MODIFIABLE);
    }

    @ExportMessage
    public final boolean isMemberInsertable(String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.INSERTABLE);
    }

    @ExportMessage
    public final void removeMember(String key) throws UnsupportedMessageException {
        if (testIntegrityLevel(false)) {
            throw UnsupportedMessageException.create();
        }
        JSObject.delete(this, key, true);
    }

    @ExportMessage
    public final boolean isMemberRemovable(String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.REMOVABLE);
    }

    @ExportMessage
    public final Object invokeMember(String id, Object[] args,
                    @CachedLanguage JavaScriptLanguage language,
                    @CachedContext(JavaScriptLanguage.class) JSRealm realm,
                    @Cached JSInteropInvokeNode callNode,
                    @Cached @Exclusive ExportValueNode exportNode) throws UnsupportedMessageException, UnknownIdentifierException {
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(this, id, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public final boolean isMemberInvocable(String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.INVOCABLE);
    }

    @ExportMessage
    public final boolean hasMemberReadSideEffects(String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.READ_SIDE_EFFECTS);
    }

    @ExportMessage
    public final boolean hasMemberWriteSideEffects(String key,
                    @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
        return keyInfo.execute(this, key, KeyInfoNode.WRITE_SIDE_EFFECTS);
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

    @ExportMessage
    public final boolean hasMetaObject() {
        return getMetaObjectImpl() != null;
    }

    @ExportMessage
    public final Object getMetaObject() throws UnsupportedMessageException {
        Object metaObject = getMetaObjectImpl();
        if (metaObject != null) {
            return metaObject;
        }
        throw UnsupportedMessageException.create();
    }

    @TruffleBoundary
    public final Object getMetaObjectImpl() {
        if (JSGuards.isJSProxy(this)) {
            return JSMetaType.JS_PROXY;
        } else {
            assert JSObject.isJSDynamicObject(this) && !JSGuards.isJSProxy(this);
            Object metaObject = JSRuntime.getDataProperty(this, JSObject.CONSTRUCTOR);
            if (metaObject != null && metaObject instanceof JSFunctionObject) {
                return metaObject;
            }
        }
        return null;
    }

    public static ReadElementNode getUncachedRead() {
        return null;
    }

    public static WriteElementNode getUncachedWrite() {
        return null;
    }
}
