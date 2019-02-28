/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * This node wraps part of the [[GetOwnProperty]] function of ECMAScript.
 *
 */
public abstract class JSGetOwnPropertyNode extends JavaScriptBaseNode {

    @Child private ToArrayIndexNode toArrayIndexNode;
    private final BranchProfile exceptional1 = BranchProfile.create();
    private final ValueProfile typeProfile = ValueProfile.createClassProfile();
    @CompilationFinal private boolean seenNonArrayIndex = false;

    protected JSGetOwnPropertyNode() {
    }

    public static JSGetOwnPropertyNode create() {
        return JSGetOwnPropertyNodeGen.create();
    }

    public abstract PropertyDescriptor execute(DynamicObject object, Object key);

    @Specialization(guards = "isJSArray(thisObj)")
    // from JSArray.ordinaryGetOwnPropertyArray
    public PropertyDescriptor array(DynamicObject thisObj, Object propertyKey) {
        assert JSRuntime.isPropertyKey(propertyKey) || propertyKey instanceof HiddenKey;

        long idx = toArrayIndex(propertyKey);
        if (JSRuntime.isArrayIndex(idx)) {
            ScriptArray array = typeProfile.profile(JSAbstractArray.arrayGetArrayType(thisObj, false));
            if (array.hasElement(thisObj, idx)) {
                Object value = array.getElement(thisObj, idx);
                return PropertyDescriptor.createData(value, true, !array.isFrozen(), !array.isSealed());
            }
        }
        exceptional1.enter();
        Property prop = thisObj.getShape().getProperty(propertyKey);
        if (prop == null) {
            return null;
        }
        return JSBuiltinObject.ordinaryGetOwnPropertyIntl(thisObj, propertyKey, prop);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSUserObject(thisObj)", "cachedPropertyKey.equals(propertyKey)", "cachedShape == thisObj.getShape()", "cachedProperty != null"}, assumptions = {
                    "cachedShape.getValidAssumption()"}, limit = "1")
    public PropertyDescriptor userObjectCacheShape(DynamicObject thisObj, Object propertyKey,
                    @Cached("thisObj.getShape()") Shape cachedShape,
                    @Cached("propertyKey") Object cachedPropertyKey,
                    @Cached("cachedShape.getProperty(propertyKey)") Property cachedProperty) {
        assert cachedProperty == thisObj.getShape().getProperty(propertyKey);
        return getUserObjectIntl(thisObj, cachedProperty);

    }

    @Specialization(guards = "isJSUserObject(thisObj)", replaces = "userObjectCacheShape")
    public PropertyDescriptor userObject(DynamicObject thisObj, Object propertyKey) {
        assert JSRuntime.isPropertyKey(propertyKey) || propertyKey instanceof HiddenKey;
        Property prop = thisObj.getShape().getProperty(propertyKey);
        if (prop == null) {
            return null;
        }
        return getUserObjectIntl(thisObj, prop);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSFunction(thisObj)", "cachedPropertyKey.equals(propertyKey)", "cachedShape == thisObj.getShape()", "cachedProperty != null"}, assumptions = {
                    "cachedShape.getValidAssumption()"}, limit = "1")
    public PropertyDescriptor jsFunctionCacheShape(DynamicObject thisObj, Object propertyKey,
                    @Cached("thisObj.getShape()") Shape cachedShape,
                    @Cached("propertyKey") Object cachedPropertyKey,
                    @Cached("cachedShape.getProperty(propertyKey)") Property cachedProperty) {
        assert cachedProperty == thisObj.getShape().getProperty(propertyKey);
        return getUserObjectIntl(thisObj, cachedProperty);

    }

    @Specialization(guards = "isJSFunction(thisObj)", replaces = "jsFunctionCacheShape")
    public PropertyDescriptor jsFunction(DynamicObject thisObj, Object propertyKey) {
        assert JSRuntime.isPropertyKey(propertyKey) || propertyKey instanceof HiddenKey;
        Property prop = thisObj.getShape().getProperty(propertyKey);
        if (prop == null) {
            return null;
        }
        return getUserObjectIntl(thisObj, prop);
    }

    private static PropertyDescriptor getUserObjectIntl(DynamicObject thisObj, Property prop) {
        PropertyDescriptor d = null;
        if (JSProperty.isData(prop)) {
            Object value = JSProperty.getValue(prop, thisObj, thisObj, false);
            d = PropertyDescriptor.createData(value);
            d.setWritable(JSProperty.isWritable(prop));
        } else if (JSProperty.isAccessor(prop)) {
            Accessor acc = (Accessor) prop.get(thisObj, false);
            d = PropertyDescriptor.createAccessor(acc.getGetter(), acc.getSetter());
        } else {
            d = PropertyDescriptor.createEmpty();
        }
        d.setEnumerable(JSProperty.isEnumerable(prop));
        d.setConfigurable(JSProperty.isConfigurable(prop));
        return d;
    }

    @Specialization(guards = {"!isJSArray(thisObj)", "!isJSUserObject(thisObj)"})
    public PropertyDescriptor generic(DynamicObject thisObj, Object key,
                    @Cached("create()") JSClassProfile profile) {
        return profile.getJSClass(thisObj).getOwnProperty(thisObj, key);
    }

    private long toArrayIndex(Object propertyKey) {
        if (toArrayIndexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toArrayIndexNode = insert(ToArrayIndexNode.create());
        }
        if (seenNonArrayIndex) {
            Object result = toArrayIndexNode.execute(propertyKey);
            return result instanceof Long ? (long) result : JSRuntime.INVALID_ARRAY_INDEX;
        }
        try {
            return toArrayIndexNode.executeLong(propertyKey);
        } catch (UnexpectedResultException ex) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            seenNonArrayIndex = true;
            return JSRuntime.INVALID_ARRAY_INDEX;
        }
    }
}
