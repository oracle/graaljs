/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * [[GetOwnProperty]] (O, K). Has a mode of not providing all entries in the property descriptor for
 * better performance.
 */
public abstract class GetOwnPropertyNode extends JavaScriptBaseNode {
    private final boolean needValue;
    private final boolean needEnumerability;
    private final boolean needConfigurability;
    private final boolean needWritability;
    private final ConditionProfile hasShapeProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile isDataPropertyBranch = BranchProfile.create();
    private final BranchProfile isAccessorPropertyBranch = BranchProfile.create();
    private final BranchProfile isOtherPropertyBranch = BranchProfile.create();

    protected GetOwnPropertyNode(boolean needValue, boolean needEnumerability, boolean needConfigurability, boolean needWritability) {
        this.needValue = needValue;
        this.needEnumerability = needEnumerability;
        this.needConfigurability = needConfigurability;
        this.needWritability = needWritability;
    }

    public static GetOwnPropertyNode create() {
        return GetOwnPropertyNodeGen.create(true, true, true, true);
    }

    public static GetOwnPropertyNode create(boolean needValue, boolean needEnumerability, boolean needConfigurability, boolean needWritability) {
        return GetOwnPropertyNodeGen.create(needValue, needEnumerability, needConfigurability, needWritability);
    }

    public abstract PropertyDescriptor execute(DynamicObject obj, Object key);

    @Specialization(guards = "jsclassProfile.getJSClass(thisObj).usesOrdinaryGetOwnProperty()", limit = "1")
    protected PropertyDescriptor getOwnPropertyBuiltinObject(DynamicObject thisObj, Object key,
                    @Cached @Shared("jsclassProfile") @SuppressWarnings("unused") JSClassProfile jsclassProfile) {
        return getOwnPropertyBuiltinObject(thisObj, key, false);
    }

    @Specialization(guards = "isJSString(thisObj)")
    protected PropertyDescriptor getOwnPropertyString(DynamicObject thisObj, Object key,
                    @Cached("createBinaryProfile()") ConditionProfile stringCaseProfile) {
        PropertyDescriptor desc = getOwnPropertyBuiltinObject(thisObj, key, false);
        if (stringCaseProfile.profile(desc == null)) {
            return JSString.stringGetIndexProperty(thisObj, key);
        } else {
            return desc;
        }
    }

    /** @see JSArray#ordinaryGetOwnPropertyArray */
    @Specialization(guards = "isJSArray(thisObj)")
    protected PropertyDescriptor getOwnPropertyArray(DynamicObject thisObj, Object key,
                    @Cached("create()") BranchProfile isArrayBranch,
                    @Cached("create()") BranchProfile hasElementsBranch,
                    @Cached("create()") BranchProfile defaultBranch) {
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey;
        long idx = JSRuntime.propertyKeyToArrayIndex(key);
        if (JSRuntime.isArrayIndex(idx)) {
            isArrayBranch.enter();
            boolean isArray = JSArray.isJSArray(thisObj);
            ScriptArray array = JSAbstractArray.arrayGetArrayType(thisObj, isArray);
            if (array.hasElement(thisObj, idx, isArray)) {
                hasElementsBranch.enter();
                Object value = needValue ? array.getElement(thisObj, idx, isArray) : null;
                return PropertyDescriptor.createData(value, true, needWritability && !array.isFrozen(), needConfigurability && !array.isSealed());
            }
        }
        defaultBranch.enter();
        return getOwnPropertyBuiltinObject(thisObj, key, false);
    }

    @Specialization(guards = {"!jsclassProfile.getJSClass(thisObj).usesOrdinaryGetOwnProperty()", "!isJSArray(thisObj)", "!isJSString(thisObj)"}, limit = "1")
    protected PropertyDescriptor getOwnPropertyGeneric(DynamicObject thisObj, Object key,
                    @Cached @Shared("jsclassProfile") JSClassProfile jsclassProfile) {
        return JSObject.getOwnProperty(thisObj, key, jsclassProfile);
    }

    /** @see JSBuiltinObject#ordinaryGetOwnProperty */
    private PropertyDescriptor getOwnPropertyBuiltinObject(DynamicObject thisObj, Object key, boolean isProxy) {
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey;
        Property prop = thisObj.getShape().getProperty(key);
        if (hasShapeProfile.profile(prop == null)) {
            return null;
        } else {
            return ordinaryGetOwnPropertyIntl(thisObj, key, prop, isProxy);
        }
    }

    private PropertyDescriptor ordinaryGetOwnPropertyIntl(DynamicObject thisObj, Object key, Property prop, boolean isProxy) {
        PropertyDescriptor desc = null;
        if (JSProperty.isData(prop)) {
            isDataPropertyBranch.enter();
            // only execute when needed or potential side-effect
            Object value = (needValue || isProxy) ? JSObject.get(thisObj, key) : null;
            desc = PropertyDescriptor.createData(value);
            if (needWritability) {
                desc.setWritable(JSProperty.isWritable(prop));
            }
        } else if (JSProperty.isAccessor(prop)) {
            isAccessorPropertyBranch.enter();
            if (needValue) {
                Accessor acc = (Accessor) prop.get(thisObj, false);
                desc = PropertyDescriptor.createAccessor(acc.getGetter(), acc.getSetter());
            } else {
                desc = PropertyDescriptor.createAccessor(null, null);
            }
        } else {
            isOtherPropertyBranch.enter();
            desc = PropertyDescriptor.createEmpty();
        }
        if (needEnumerability) {
            desc.setEnumerable(JSProperty.isEnumerable(prop));
        }
        if (needConfigurability) {
            desc.setConfigurable(JSProperty.isConfigurable(prop));
        }
        return desc;
    }
}
