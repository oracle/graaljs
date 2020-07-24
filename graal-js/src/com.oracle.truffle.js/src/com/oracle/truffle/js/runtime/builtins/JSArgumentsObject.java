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

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.LanguageReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.interop.KeyInfoNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.InteropArray;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public final class JSArgumentsObject extends JSAbstractArgumentsObject {
    public static final JSArgumentsObject INSTANCE = new JSArgumentsObject();

    private JSArgumentsObject() {
    }

    @ExportLibrary(InteropLibrary.class)
    public static class AbstractArgumentsObjectImpl extends JSArrayBase {

        protected AbstractArgumentsObjectImpl(Shape shape, ScriptArray arrayType, Object array, int length) {
            super(shape, arrayType, array, null, length, 0, 0, 0, 0);
        }

        @Override
        public final String getClassName() {
            return CLASS_NAME;
        }

        @ExportMessage
        public final Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            // Do not include array indices
            assert JSObject.getJSClass(this) == JSArgumentsObject.INSTANCE;
            return InteropArray.create(filterEnumerableNames(this, JSObject.ownPropertyKeys(this), JSArgumentsObject.INSTANCE));
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public final boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public final long getArraySize() {
            return JSRuntime.toInteger(JSObject.get(this, JSAbstractArray.LENGTH));
        }

        @ExportMessage
        public final Object readArrayElement(long index,
                        @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                        @Cached(value = "create(languageRef.get().getJSContext())", uncached = "getUncachedRead()") ReadElementNode readNode,
                        @Cached ExportValueNode exportNode,
                        @CachedLibrary("this") InteropLibrary thisLibrary) throws InvalidArrayIndexException, UnsupportedMessageException {
            if (!hasArrayElements()) {
                throw UnsupportedMessageException.create();
            }
            DynamicObject target = this;
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
        public final boolean isArrayElementReadable(long index,
                        @CachedLibrary("this") InteropLibrary thisLibrary) {
            try {
                return hasArrayElements() && (index >= 0 && index < thisLibrary.getArraySize(this));
            } catch (UnsupportedMessageException e) {
                throw Errors.shouldNotReachHere(e);
            }
        }

        @ExportMessage
        public final void writeArrayElement(long index, Object value,
                        @Shared("keyInfo") @Cached KeyInfoNode keyInfo,
                        @Cached ImportValueNode castValueNode,
                        @CachedLanguage @SuppressWarnings("unused") LanguageReference<JavaScriptLanguage> languageRef,
                        @Cached(value = "createCachedInterop(languageRef)", uncached = "getUncachedWrite()") WriteElementNode writeNode)
                        throws InvalidArrayIndexException, UnsupportedMessageException {
            if (!hasArrayElements() || testIntegrityLevel(true)) {
                throw UnsupportedMessageException.create();
            }
            DynamicObject target = this;
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
        public final boolean isArrayElementModifiable(long index,
                        @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
            return hasArrayElements() && keyInfo.execute(this, index, KeyInfoNode.MODIFIABLE);
        }

        @ExportMessage
        public final boolean isArrayElementInsertable(long index,
                        @Shared("keyInfo") @Cached KeyInfoNode keyInfo) {
            return hasArrayElements() && keyInfo.execute(this, index, KeyInfoNode.INSERTABLE);
        }
    }

    public static class UnmappedArgumentsObjectImpl extends AbstractArgumentsObjectImpl {

        protected UnmappedArgumentsObjectImpl(Shape shape, ScriptArray arrayType, Object array, int length) {
            super(shape, arrayType, array, length);
        }
    }

    public static class MappedArgumentsObjectImpl extends AbstractArgumentsObjectImpl {

        protected MappedArgumentsObjectImpl(Shape shape, ScriptArray arrayType, Object array, int length) {
            super(shape, arrayType, array, length);
            this.connectedArgumentCount = length;
        }

        protected int connectedArgumentCount;
        protected Map<Long, Object> disconnectedIndices;

        public static int getConnectedArgumentCount(DynamicObject argumentsArray) {
            assert JSArgumentsObject.isJSArgumentsObject(argumentsArray);
            return ((MappedArgumentsObjectImpl) argumentsArray).connectedArgumentCount;
        }

        @TruffleBoundary
        public static Map<Long, Object> getDisconnectedIndices(DynamicObject argumentsArray) {
            assert hasDisconnectedIndices(argumentsArray);
            return ((MappedArgumentsObjectImpl) argumentsArray).disconnectedIndices;
        }

        @TruffleBoundary
        public static void initDisconnectedIndices(DynamicObject argumentsArray) {
            assert hasDisconnectedIndices(argumentsArray);
            ((MappedArgumentsObjectImpl) argumentsArray).disconnectedIndices = new HashMap<>();
        }

        public static boolean hasDisconnectedIndices(DynamicObject argumentsArray) {
            return JSSlowArgumentsObject.isJSSlowArgumentsObject(argumentsArray);
        }
    }

    public static UnmappedArgumentsObjectImpl createUnmapped(Shape shape, Object[] elements) {
        return new UnmappedArgumentsObjectImpl(shape, ScriptArray.createConstantArray(elements), elements, elements.length);
    }

    public static MappedArgumentsObjectImpl createMapped(Shape shape, Object[] elements) {
        return new MappedArgumentsObjectImpl(shape, ScriptArray.createConstantArray(elements), elements, elements.length);
    }

    @TruffleBoundary
    public static DynamicObject createStrictSlow(JSRealm realm, Object[] elements) {
        JSContext context = realm.getContext();
        JSObjectFactory factory = context.getStrictArgumentsFactory();
        DynamicObject argumentsObject = createUnmapped(factory.getShape(realm), elements);
        factory.initProto(argumentsObject, realm);

        JSObjectUtil.putDataProperty(context, argumentsObject, LENGTH, elements.length, JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.configurableNotEnumerableWritable());

        Accessor throwerAccessor = realm.getThrowerAccessor();
        JSObjectUtil.putBuiltinAccessorProperty(argumentsObject, CALLEE, throwerAccessor, JSAttributes.notConfigurableNotEnumerable());
        if (context.getEcmaScriptVersion() < JSConfig.ECMAScript2017) {
            JSObjectUtil.putBuiltinAccessorProperty(argumentsObject, CALLER, throwerAccessor, JSAttributes.notConfigurableNotEnumerable());
        }

        return context.trackAllocation(argumentsObject);
    }

    @TruffleBoundary
    public static DynamicObject createNonStrictSlow(JSRealm realm, Object[] elements, DynamicObject callee) {
        JSContext context = realm.getContext();
        JSObjectFactory factory = context.getNonStrictArgumentsFactory();
        DynamicObject argumentsObject = createMapped(factory.getShape(realm), elements);
        factory.initProto(argumentsObject, realm);

        JSObjectUtil.putDataProperty(context, argumentsObject, LENGTH, elements.length, JSAttributes.configurableNotEnumerableWritable());
        JSObjectUtil.putDataProperty(context, argumentsObject, Symbol.SYMBOL_ITERATOR, realm.getArrayProtoValuesIterator(), JSAttributes.configurableNotEnumerableWritable());

        JSObjectUtil.putDataProperty(context, argumentsObject, CALLEE, callee, JSAttributes.configurableNotEnumerableWritable());
        return context.trackAllocation(argumentsObject);
    }

    public static boolean isJSArgumentsObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE) || isInstance(obj, JSSlowArgumentsObject.INSTANCE);
    }

    public static boolean isJSArgumentsObject(Object obj) {
        return isInstance(obj, INSTANCE) || isInstance(obj, JSSlowArgumentsObject.INSTANCE);
    }

    public static boolean isJSFastArgumentsObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    public static boolean isJSFastArgumentsObject(Object obj) {
        return isInstance(obj, INSTANCE);
    }
}
