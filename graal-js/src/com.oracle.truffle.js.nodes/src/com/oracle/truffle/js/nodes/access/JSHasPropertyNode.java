/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.IntToLongTypeSystem;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * This node wraps part of the [[HasProperty]] function of ECMAScript. Its purpose is to specialize
 * on features of the object and/or the property sought.
 *
 */
@TypeSystemReference(IntToLongTypeSystem.class)
@ImportStatic(value = {JSRuntime.class, JSInteropUtil.class})
public abstract class JSHasPropertyNode extends JavaScriptBaseNode {

    private final boolean hasOwnProperty;
    private final JSClassProfile classProfile = JSClassProfile.create();
    private final ConditionProfile hasElementProfile = ConditionProfile.createBinaryProfile();

    static final int MAX_ARRAY_TYPES = 3;

    protected JSHasPropertyNode(boolean hasOwnProperty) {
        this.hasOwnProperty = hasOwnProperty;
    }

    public static JSHasPropertyNode create() {
        return JSHasPropertyNodeGen.create(false);
    }

    public static JSHasPropertyNode create(boolean hasOwnProperty) {
        return JSHasPropertyNodeGen.create(hasOwnProperty);
    }

    public abstract boolean executeBoolean(TruffleObject object, Object propertyName);

    public abstract boolean executeBoolean(TruffleObject object, long index);

    @Specialization(guards = {"isJSFastArray(object)", "isArrayIndex(index)", "cachedArrayType.isInstance(getArrayType(object))"}, limit = "MAX_ARRAY_TYPES")
    public boolean arrayLongCached(DynamicObject object, long index,
                    @Cached("getArrayType(object)") ScriptArray cachedArrayType) {
        return checkInteger(object, index, cachedArrayType.cast(getArrayType(object)));
    }

    @Specialization(guards = {"isJSFastArray(object)", "isArrayIndex(index)"}, replaces = {"arrayLongCached"})
    public boolean arrayLong(DynamicObject object, long index) {
        return checkInteger(object, index, getArrayType(object));
    }

    private boolean checkInteger(DynamicObject object, long index, ScriptArray arrayType) {
        if (hasElementProfile.profile(arrayType.hasElement(object, index, JSArray.isJSFastArray(object)))) {
            return true;
        } else {
            return objectLong(object, index);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedObjectType != null", "cachedObjectType.isInstance(object)", "cachedName.equals(propertyName)"}, limit = "1")
    public boolean objectStringCached(DynamicObject object, String propertyName,
                    @Cached("getCacheableObjectType(object)") JSClass cachedObjectType,
                    @Cached("propertyName") String cachedName,
                    @Cached("getCachedPropertyGetter(object,propertyName)") HasPropertyCacheNode hasPropertyNode) {
        return hasPropertyNode.hasProperty(object);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSArray(object)", "!isArrayIndex(cachedName)", "cachedName.equals(propertyName)"}, limit = "1")
    public boolean arrayStringCached(DynamicObject object, String propertyName,
                    @Cached("propertyName") String cachedName,
                    @Cached("getCachedPropertyGetter(object,propertyName)") HasPropertyCacheNode hasPropertyNode) {
        return hasPropertyNode.hasProperty(object);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSJavaWrapper(object)"}, replaces = {"objectStringCached", "arrayStringCached"})
    public boolean objectOrArrayString(DynamicObject object, String propertyName) {
        return hasPropertyGeneric(object, propertyName);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSJavaWrapper(object)"})
    public boolean objectSymbol(DynamicObject object, Symbol propertyName) {
        return hasPropertyGeneric(object, propertyName);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSFastArray(object)", "!isJSJavaWrapper(object)"})
    public boolean objectLong(DynamicObject object, long propertyIdx) {
        if (hasOwnProperty) {
            return JSObject.hasOwnProperty(object, propertyIdx, classProfile);
        } else {
            return JSObject.hasProperty(object, propertyIdx, classProfile);
        }
    }

    private boolean hasPropertyGeneric(DynamicObject object, Object propertyKey) {
        assert JSRuntime.isPropertyKey(propertyKey);
        if (hasOwnProperty) {
            return JSObject.hasOwnProperty(object, propertyKey, classProfile);
        } else {
            return JSObject.hasProperty(object, propertyKey, classProfile);
        }
    }

    @Specialization(guards = "isForeignObject(object)")
    public boolean foreignObject(TruffleObject object, Object propertyName,
                    @Cached("createKeyInfo()") Node keyInfoNode,
                    @Cached("create()") JSToStringNode toStringNode) {
        Object key;
        if (propertyName instanceof Symbol) {
            return false;
        } else if (propertyName instanceof Number) {
            key = propertyName;
        } else {
            key = toStringNode.executeString(propertyName);
        }
        int result = ForeignAccess.sendKeyInfo(keyInfoNode, object, key);
        return KeyInfo.isExisting(result);
    }

    @Specialization(guards = "isJSType(object)")
    public boolean objectObject(DynamicObject object, Object propertyName,
                    @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
        Object propertyKey = toPropertyKeyNode.execute(propertyName);
        return hasPropertyGeneric(object, propertyKey);
    }

    protected static boolean isCacheableObjectType(DynamicObject obj) {
        return JSObject.isJSObject(obj) && (!JSRuntime.isNullOrUndefined(obj) &&
                        !JSString.isJSString(obj) &&
                        !JSArray.isJSArray(obj) &&
                        !JSArgumentsObject.isJSArgumentsObject(obj) &&
                        !JSArrayBufferView.isJSArrayBufferView(obj) &&
                        !JSJavaWrapper.isJSJavaWrapper(obj));
    }

    protected static JSClass getCacheableObjectType(DynamicObject obj) {
        if (isCacheableObjectType(obj)) {
            return JSObject.getJSClass(obj);
        }
        return null;
    }

    protected static ScriptArray getArrayType(DynamicObject object) {
        return JSAbstractArray.arrayGetArrayType(object);
    }

    protected HasPropertyCacheNode getCachedPropertyGetter(DynamicObject object, Object key) {
        return HasPropertyCacheNode.create(key, JSObject.getJSContext(object), hasOwnProperty);
    }
}
