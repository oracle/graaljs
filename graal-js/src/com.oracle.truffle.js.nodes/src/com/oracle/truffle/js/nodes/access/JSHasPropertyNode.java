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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
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
@ImportStatic(value = JSInteropUtil.class)
public abstract class JSHasPropertyNode extends JavaScriptBaseNode {

    private final JSClassProfile classProfile = JSClassProfile.create();
    private final ValueProfile arrayType = ValueProfile.createClassProfile();
    private final ConditionProfile simpleCheck = ConditionProfile.createBinaryProfile();

    public static JSHasPropertyNode create() {
        return JSHasPropertyNodeGen.create();
    }

    public abstract boolean executeBoolean(TruffleObject object, Object propertyName);

    @Specialization(guards = "isJSFastArray(object)")
    public boolean arrayInt(DynamicObject object, int propertyIdx) {
        return checkInteger(object, propertyIdx);
    }

    @Specialization(guards = "isJSFastArray(object)")
    public boolean arrayLong(DynamicObject object, long propertyIdx) {
        return checkInteger(object, propertyIdx);
    }

    private boolean checkInteger(DynamicObject object, long propertyIdx) {
        ScriptArray array = arrayType.profile(JSAbstractArray.arrayGetArrayType(object));
        if (simpleCheck.profile(array.hasElement(object, propertyIdx, JSArray.isJSFastArray(object)))) {
            return true;
        } else {
            return objectLong(object, propertyIdx);
        }
    }

    @Specialization(guards = {"acceptProperty(propertyName,cachedName)", "acceptObjectType(object)"}, limit = "1")
    public boolean objectOrArrayStringCached(DynamicObject object,
                    @SuppressWarnings("unused") String propertyName,
                    @SuppressWarnings("unused") @Cached("propertyName") String cachedName,
                    @Cached("getCachedPropertyGetter(object,propertyName)") HasPropertyCacheNode propertyGetter) {
        return propertyGetter.hasProperty(object);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSJavaWrapper(object)"}, replaces = "objectOrArrayStringCached")
    public boolean objectOrArrayString(DynamicObject object, String propertyName) {
        return JSObject.hasProperty(object, propertyName, classProfile);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSFastArray(object)", "!isJSJavaWrapper(object)"})
    public boolean objectInt(DynamicObject object, int propertyIdx) {
        return objectLong(object, propertyIdx);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSFastArray(object)", "!isJSJavaWrapper(object)"})
    public boolean objectLong(DynamicObject object, long propertyIdx) {
        return JSObject.hasProperty(object, propertyIdx, classProfile);
    }

    @Specialization(guards = {"isJSType(object)", "!isJSJavaWrapper(object)"})
    public boolean objectSymbol(DynamicObject object, Symbol propertyName) {
        return JSObject.hasProperty(object, propertyName, classProfile);
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
        return result != 0;
    }

    @Specialization(guards = "isJSType(object)")
    public boolean objectObject(DynamicObject object, Object propertyName,
                    @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
        return JSObject.hasProperty(object, toPropertyKeyNode.execute(propertyName), classProfile);
    }

    protected static boolean acceptProperty(String key, Object expected) {
        return key.equals(expected);
    }

    protected static boolean acceptObjectType(Object obj) {
        return JSObject.isJSObject(obj) && (!JSString.isJSString(obj) && !JSArray.isJSArray(obj) && !JSArgumentsObject.isJSArgumentsObject(obj) && !JSJavaWrapper.isJSJavaWrapper(obj));
    }

    protected static HasPropertyCacheNode getCachedPropertyGetter(DynamicObject object, Object key) {
        return HasPropertyCacheNode.create(key, JSObject.getJSContext(object));
    }
}
