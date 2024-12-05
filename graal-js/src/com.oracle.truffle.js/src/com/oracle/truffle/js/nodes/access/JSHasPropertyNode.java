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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.IntToLongTypeSystem;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.array.TypedArrayLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * This node wraps part of the [[HasProperty]] function of ECMAScript. Its purpose is to specialize
 * on features of the object and/or the property sought.
 *
 */
@TypeSystemReference(IntToLongTypeSystem.class)
@ImportStatic(value = {JSRuntime.class, JSInteropUtil.class, JSConfig.class, Strings.class})
public abstract class JSHasPropertyNode extends JavaScriptBaseNode {

    private final boolean hasOwnProperty;
    private final JSClassProfile classProfile = JSClassProfile.create();

    static final int MAX_ARRAY_TYPES = 3;

    protected JSHasPropertyNode(boolean hasOwnProperty) {
        this.hasOwnProperty = hasOwnProperty;
    }

    @NeverDefault
    public static JSHasPropertyNode create() {
        return JSHasPropertyNodeGen.create(false);
    }

    @NeverDefault
    public static JSHasPropertyNode create(boolean hasOwnProperty) {
        return JSHasPropertyNodeGen.create(hasOwnProperty);
    }

    public abstract boolean executeBoolean(Object object, Object propertyName);

    public abstract boolean executeBoolean(Object object, long index);

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"isJSFastArray(object)", "isArrayIndex(index)", "cachedArrayType.isInstance(getArrayType(object))"}, limit = "MAX_ARRAY_TYPES")
    public boolean arrayLongCached(JSDynamicObject object, long index,
                    @Bind Node node,
                    @Cached("getArrayType(object)") ScriptArray cachedArrayType,
                    @Shared @Cached InlinedConditionProfile hasElementProfile) {
        return checkInteger(object, index, cachedArrayType.cast(getArrayType(object)), node, hasElementProfile);
    }

    @Specialization(guards = {"isJSFastArray(object)", "isArrayIndex(index)"}, replaces = {"arrayLongCached"})
    public boolean arrayLong(JSDynamicObject object, long index,
                    @Shared @Cached InlinedConditionProfile hasElementProfile) {
        return checkInteger(object, index, getArrayType(object), this, hasElementProfile);
    }

    private boolean checkInteger(JSDynamicObject object, long index, ScriptArray arrayType, Node node, InlinedConditionProfile hasElementProfile) {
        if (hasElementProfile.profile(node, arrayType.hasElement(object, index))) {
            return true;
        } else {
            return objectLong(object, index);
        }
    }

    @Specialization
    public boolean typedArray(JSTypedArrayObject object, long index,
                    @Cached TypedArrayLengthNode typedArrayLengthNode) {
        // If IsTypedArrayOutOfBounds(), TypedArrayLength() == 0.
        return index >= 0 && index < typedArrayLengthNode.execute(this, object, getJSContext());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedObjectType != null", "cachedObjectType.isInstance(object)", "equals(equalNode, cachedName, propertyName)"}, limit = "1")
    public boolean objectStringCached(JSDynamicObject object, TruffleString propertyName,
                    @Cached("getCacheableObjectType(object)") JSClass cachedObjectType,
                    @Cached("propertyName") TruffleString cachedName,
                    @Cached("getCachedPropertyGetter(object, propertyName)") HasPropertyCacheNode hasPropertyNode,
                    @Cached @Shared TruffleString.EqualNode equalNode) {
        return hasPropertyNode.hasProperty(object);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSArray(object)", "!isArrayIndex(cachedName)", "equals(equalNode, cachedName, propertyName)"}, limit = "1")
    public boolean arrayStringCached(JSDynamicObject object, TruffleString propertyName,
                    @Cached("propertyName") TruffleString cachedName,
                    @Cached("getCachedPropertyGetter(object, propertyName)") HasPropertyCacheNode hasPropertyNode,
                    @Cached @Shared TruffleString.EqualNode equalNode) {
        return hasPropertyNode.hasProperty(object);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = {"objectStringCached", "arrayStringCached"})
    public boolean objectString(JSDynamicObject object, TruffleString propertyName) {
        return hasPropertyGeneric(object, propertyName);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization
    public boolean objectSymbol(JSDynamicObject object, Symbol propertyName) {
        return hasPropertyGeneric(object, propertyName);
    }

    @Specialization(guards = {"!isJSFastArray(object)", "!isJSArrayBufferView(object)"})
    public boolean objectLong(JSDynamicObject object, long propertyIdx) {
        if (hasOwnProperty) {
            return JSObject.hasOwnProperty(object, propertyIdx, classProfile);
        } else {
            return JSObject.hasProperty(object, propertyIdx, classProfile);
        }
    }

    private boolean hasPropertyGeneric(JSDynamicObject object, Object propertyKey) {
        assert JSRuntime.isPropertyKey(propertyKey);
        if (hasOwnProperty) {
            return JSObject.hasOwnProperty(object, propertyKey, classProfile);
        } else {
            return JSObject.hasProperty(object, propertyKey, classProfile);
        }
    }

    @InliningCutoff
    @Specialization(guards = "isForeignObject(object)", limit = "InteropLibraryLimit")
    public boolean foreignObject(Object object, Object propertyName,
                    @CachedLibrary("object") InteropLibrary interop,
                    @Cached JSToStringNode toStringNode,
                    @Cached ForeignObjectPrototypeNode foreignObjectPrototypeNode,
                    @Cached JSHasPropertyNode hasInPrototype) {
        if (propertyName instanceof Number && interop.hasArrayElements(object)) {
            long index = JSRuntime.longValue((Number) propertyName);
            return index >= 0 && index < JSInteropUtil.getArraySize(object, interop, this);
        } else {
            if (!(propertyName instanceof Symbol) && interop.isMemberExisting(object, Strings.toJavaString(toStringNode.executeString(propertyName)))) {
                return true;
            }
            if (getLanguage().getJSContext().getLanguageOptions().hasForeignObjectPrototype()) {
                JSDynamicObject prototype = foreignObjectPrototypeNode.execute(object);
                return hasInPrototype.executeBoolean(prototype, propertyName);
            } else {
                return false;
            }
        }
    }

    @ReportPolymorphism.Megamorphic
    @Specialization
    public boolean objectObject(JSDynamicObject object, Object propertyName,
                    @Cached JSToPropertyKeyNode toPropertyKeyNode) {
        Object propertyKey = toPropertyKeyNode.execute(propertyName);
        return hasPropertyGeneric(object, propertyKey);
    }

    protected static boolean isCacheableObjectType(JSDynamicObject obj) {
        return JSDynamicObject.isJSDynamicObject(obj) && (!JSRuntime.isNullOrUndefined(obj) &&
                        !JSString.isJSString(obj) &&
                        !JSArray.isJSArray(obj) &&
                        !JSArgumentsArray.isJSArgumentsObject(obj) &&
                        !JSArrayBufferView.isJSArrayBufferView(obj));
    }

    protected static JSClass getCacheableObjectType(JSDynamicObject obj) {
        if (isCacheableObjectType(obj)) {
            return JSObject.getJSClass(obj);
        }
        return null;
    }

    protected static ScriptArray getArrayType(JSDynamicObject object) {
        return JSAbstractArray.arrayGetArrayType(object);
    }

    protected HasPropertyCacheNode getCachedPropertyGetter(JSDynamicObject object, Object key) {
        return HasPropertyCacheNode.create(key, JSObject.getJSContext(object), hasOwnProperty);
    }
}
