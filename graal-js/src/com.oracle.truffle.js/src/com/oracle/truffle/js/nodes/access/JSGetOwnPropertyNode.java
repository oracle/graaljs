/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNodeGen.GetPropertyProxyValueNodeGen;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNodeGen.UsesOrdinaryGetOwnPropertyNodeGen;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * [[GetOwnProperty]] (O, K) internal method.
 *
 * Property descriptor entries not requested may be omitted for better performance.
 */
@ImportStatic({JSRuntime.class})
public abstract class JSGetOwnPropertyNode extends JavaScriptBaseNode {
    private final boolean needValue;
    private final boolean needEnumerability;
    private final boolean needConfigurability;
    private final boolean needWritability;
    final boolean allowCaching;
    @CompilationFinal private boolean seenNonArrayIndex;
    private final ConditionProfile hasPropertyBranch = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isDataPropertyBranch = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isAccessorPropertyBranch = ConditionProfile.createBinaryProfile();
    @Child private GetPropertyProxyValueNode getPropertyProxyValueNode;

    protected JSGetOwnPropertyNode(boolean needValue, boolean needEnumerability, boolean needConfigurability, boolean needWritability, boolean allowCaching) {
        this.needValue = needValue;
        this.needEnumerability = needEnumerability;
        this.needConfigurability = needConfigurability;
        this.needWritability = needWritability;
        this.allowCaching = allowCaching;
    }

    public static JSGetOwnPropertyNode create() {
        return create(true);
    }

    public static JSGetOwnPropertyNode create(boolean needValue) {
        return create(needValue, true, true, true, true);
    }

    public static JSGetOwnPropertyNode create(boolean needValue, boolean needEnumerability, boolean needConfigurability, boolean needWritability, boolean allowCaching) {
        return JSGetOwnPropertyNodeGen.create(needValue, needEnumerability, needConfigurability, needWritability, allowCaching);
    }

    public abstract PropertyDescriptor execute(DynamicObject object, Object key);

    /** @see JSArray#getOwnProperty */
    @Specialization(guards = {"isJSArray(thisObj)"})
    PropertyDescriptor array(DynamicObject thisObj, Object propertyKey,
                    @Cached ToArrayIndexNode toArrayIndexNode,
                    @Cached BranchProfile noSuchElementBranch,
                    @Cached("createIdentityProfile()") ValueProfile typeProfile) {
        assert JSRuntime.isPropertyKey(propertyKey);
        long idx = toArrayIndex(propertyKey, toArrayIndexNode);
        if (JSRuntime.isArrayIndex(idx)) {
            ScriptArray array = typeProfile.profile(JSAbstractArray.arrayGetArrayType(thisObj));
            if (array.hasElement(thisObj, idx)) {
                Object value = needValue ? array.getElement(thisObj, idx) : null;
                return PropertyDescriptor.createData(value, true, needWritability && !array.isFrozen(), needConfigurability && !array.isSealed());
            }
        }
        noSuchElementBranch.enter();
        Property prop = thisObj.getShape().getProperty(propertyKey);
        return ordinaryGetOwnProperty(thisObj, prop);
    }

    /** @see JSString#getOwnProperty */
    @Specialization(guards = "isJSString(thisObj)")
    protected PropertyDescriptor getOwnPropertyString(DynamicObject thisObj, Object key,
                    @Cached("createBinaryProfile()") ConditionProfile stringCaseProfile) {
        assert JSRuntime.isPropertyKey(key);
        Property prop = thisObj.getShape().getProperty(key);
        PropertyDescriptor desc = ordinaryGetOwnProperty(thisObj, prop);
        if (stringCaseProfile.profile(desc == null)) {
            return JSString.stringGetIndexProperty(thisObj, key);
        } else {
            return desc;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {
                    "allowCaching",
                    "cachedJSClass != null",
                    "propertyKeyEquals(cachedPropertyKey, propertyKey)",
                    "cachedShape == thisObj.getShape()"}, assumptions = {"cachedShape.getValidAssumption()"}, limit = "3")
    PropertyDescriptor cachedOrdinary(DynamicObject thisObj, Object propertyKey,
                    @Cached("getJSClassIfOrdinary(thisObj)") JSClass cachedJSClass,
                    @Cached("thisObj.getShape()") Shape cachedShape,
                    @Cached("propertyKey") Object cachedPropertyKey,
                    @Cached("cachedShape.getProperty(propertyKey)") Property cachedProperty) {
        assert JSRuntime.isPropertyKey(propertyKey) && JSObject.getJSClass(thisObj).usesOrdinaryGetOwnProperty();
        return ordinaryGetOwnProperty(thisObj, cachedProperty);
    }

    @Specialization(guards = "usesOrdinaryGetOwnProperty.execute(thisObj)", replaces = "cachedOrdinary", limit = "1")
    PropertyDescriptor uncachedOrdinary(DynamicObject thisObj, Object propertyKey,
                    @Cached @Shared("usesOrdinaryGetOwnProperty") @SuppressWarnings("unused") UsesOrdinaryGetOwnPropertyNode usesOrdinaryGetOwnProperty) {
        assert JSRuntime.isPropertyKey(propertyKey) && JSObject.getJSClass(thisObj).usesOrdinaryGetOwnProperty();
        Property prop = thisObj.getShape().getProperty(propertyKey);
        return ordinaryGetOwnProperty(thisObj, prop);
    }

    static JSClass getJSClassIfOrdinary(DynamicObject obj) {
        JSClass jsclass = JSObject.getJSClass(obj);
        if (jsclass.usesOrdinaryGetOwnProperty()) {
            return jsclass;
        }
        return null;
    }

    /** @see JSNonProxy#ordinaryGetOwnProperty */
    private PropertyDescriptor ordinaryGetOwnProperty(DynamicObject thisObj, Property prop) {
        assert !JSProxy.isJSProxy(thisObj);
        if (hasPropertyBranch.profile(prop == null)) {
            return null;
        }
        PropertyDescriptor d;
        if (isDataPropertyBranch.profile(JSProperty.isData(prop))) {
            Object value = needValue ? getDataPropertyValue(thisObj, prop) : null;
            d = PropertyDescriptor.createData(value);
            if (needWritability) {
                d.setWritable(JSProperty.isWritable(prop));
            }
        } else if (isAccessorPropertyBranch.profile(JSProperty.isAccessor(prop))) {
            if (needValue) {
                Accessor acc = (Accessor) prop.get(thisObj, false);
                d = PropertyDescriptor.createAccessor(acc.getGetter(), acc.getSetter());
            } else {
                d = PropertyDescriptor.createAccessor(null, null);
            }
        } else {
            d = PropertyDescriptor.createEmpty();
        }
        if (needEnumerability) {
            d.setEnumerable(JSProperty.isEnumerable(prop));
        }
        if (needConfigurability) {
            d.setConfigurable(JSProperty.isConfigurable(prop));
        }
        return d;
    }

    private Object getDataPropertyValue(DynamicObject thisObj, Property prop) {
        assert JSProperty.isData(prop);
        Object value = prop.getLocation().get(thisObj, false);
        if (JSProperty.isProxy(prop)) {
            return getPropertyProxyValue(thisObj, value);
        } else {
            return value;
        }
    }

    private Object getPropertyProxyValue(DynamicObject obj, Object propertyProxy) {
        if (getPropertyProxyValueNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getPropertyProxyValueNode = insert(GetPropertyProxyValueNodeGen.create());
        }
        return getPropertyProxyValueNode.execute(obj, propertyProxy);
    }

    @Specialization(guards = {"!usesOrdinaryGetOwnProperty.execute(thisObj)", "!isJSArray(thisObj)", "!isJSString(thisObj)"}, limit = "1")
    static PropertyDescriptor generic(DynamicObject thisObj, Object key,
                    @Cached("create()") JSClassProfile jsclassProfile,
                    @Cached @Shared("usesOrdinaryGetOwnProperty") @SuppressWarnings("unused") UsesOrdinaryGetOwnPropertyNode usesOrdinaryGetOwnProperty) {
        assert !JSObject.getJSClass(thisObj).usesOrdinaryGetOwnProperty();
        return JSObject.getOwnProperty(thisObj, key, jsclassProfile);
    }

    private long toArrayIndex(Object propertyKey, ToArrayIndexNode toArrayIndexNode) {
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

    public abstract static class UsesOrdinaryGetOwnPropertyNode extends JavaScriptBaseNode {

        protected UsesOrdinaryGetOwnPropertyNode() {
        }

        public static UsesOrdinaryGetOwnPropertyNode create() {
            return UsesOrdinaryGetOwnPropertyNodeGen.create();
        }

        public final boolean execute(DynamicObject object) {
            return execute(JSShape.getJSClassNoCast(object.getShape()));
        }

        public abstract boolean execute(Object jsclass);

        @Specialization(guards = {"isReferenceEquals(jsclass, cachedJSClass)"}, limit = "7")
        static boolean doCached(@SuppressWarnings("unused") Object jsclass,
                        @Cached(value = "asJSClass(jsclass)") JSClass cachedJSClass) {
            return cachedJSClass.usesOrdinaryGetOwnProperty();
        }

        @Specialization(replaces = {"doCached"})
        static boolean doObjectPrototype(Object jsclass) {
            return asJSClass(jsclass).usesOrdinaryGetOwnProperty();
        }

        static JSClass asJSClass(Object jsclass) {
            return (JSClass) jsclass;
        }
    }

    public abstract static class GetPropertyProxyValueNode extends JavaScriptBaseNode {

        protected GetPropertyProxyValueNode() {
        }

        public abstract Object execute(DynamicObject obj, Object propertyProxy);

        @Specialization(guards = {"propertyProxy.getClass() == cachedClass"}, limit = "5")
        static Object doCached(DynamicObject obj, Object propertyProxy,
                        @Cached(value = "propertyProxy.getClass()") Class<?> cachedClass) {
            return ((PropertyProxy) cachedClass.cast(propertyProxy)).get(obj);
        }

        @TruffleBoundary
        @Specialization(replaces = {"doCached"})
        static Object doUncached(DynamicObject obj, Object propertyProxy) {
            return ((PropertyProxy) propertyProxy).get(obj);
        }
    }
}
