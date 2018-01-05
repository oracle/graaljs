/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
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
                    @Cached("createRead()") Node readNode) {
        try {
            ForeignAccess.sendRead(readNode, object, JSRuntime.toPropertyKey(propertyName));
            return true; // read worked, so HAS == true;
        } catch (UnknownIdentifierException e) {
            return false; // read did not work, so HAS == false;
        } catch (UnsupportedMessageException e) {
            throw Errors.createTypeError("cannot read from foreign object due to: " + e.getMessage());
        }
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
