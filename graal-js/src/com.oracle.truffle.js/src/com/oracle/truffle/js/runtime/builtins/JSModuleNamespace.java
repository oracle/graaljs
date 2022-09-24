/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;

/**
 * Module Namespace Exotic Objects.
 */
public final class JSModuleNamespace extends JSNonProxy {

    public static final JSModuleNamespace INSTANCE = new JSModuleNamespace();

    public static final TruffleString CLASS_NAME = Strings.constant("Module");

    private JSModuleNamespace() {
    }

    /**
     * [[Module]]. Module Record.
     *
     * The Module Record whose exports this namespace exposes.
     */
    public static JSModuleRecord getModule(JSDynamicObject obj) {
        assert isJSModuleNamespace(obj);
        return ((JSModuleNamespaceObject) obj).getModule();
    }

    /**
     * [[Exports]]. List of String.
     *
     * A List containing the String values of the exported names exposed as own properties of this
     * object. The list is ordered as if an Array of those String values had been sorted using
     * Array.prototype.sort using SortCompare as comparefn.
     */
    public static Map<TruffleString, ExportResolution> getExports(JSDynamicObject obj) {
        assert isJSModuleNamespace(obj);
        return ((JSModuleNamespaceObject) obj).getExports();
    }

    public static JSModuleNamespaceObject create(JSContext context, JSRealm realm, JSModuleRecord module, Map<TruffleString, ExportResolution> exports) {
        JSObjectFactory factory = context.getModuleNamespaceFactory();
        JSModuleNamespaceObject obj = JSModuleNamespaceObject.create(realm, factory, module, exports);
        assert !JSObject.isExtensible(obj);
        return context.trackAllocation(obj);
    }

    public static Shape makeInitialShape(JSContext context) {
        Shape initialShape = JSShape.newBuilder(context, INSTANCE, Null.instance).shapeFlags(JSShape.NOT_EXTENSIBLE_FLAG).build();

        /*
         * The initial value of the @@toStringTag property is the String value "Module".
         *
         * This property has the attributes { [[Writable]]: false, [[Enumerable]]: false,
         * [[Configurable]]: false }.
         */
        initialShape = Shape.newBuilder(initialShape).//
                        addConstantProperty(JSObject.HIDDEN_PROTO, Null.instance, 0).//
                        addConstantProperty(Symbol.SYMBOL_TO_STRING_TAG, CLASS_NAME, JSAttributes.notConfigurableNotEnumerableNotWritable()).build();
        assert !JSShape.isExtensible(initialShape);
        return initialShape;
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    @TruffleBoundary
    public TruffleString toDisplayStringImpl(JSDynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        return Strings.addBrackets(CLASS_NAME);
    }

    @Override
    @TruffleBoundary
    public Object getOwnHelper(JSDynamicObject store, Object thisObj, Object key, Node encapsulatingNode) {
        if (!Strings.isTString(key)) {
            return super.getOwnHelper(store, thisObj, key, encapsulatingNode);
        }
        Map<TruffleString, ExportResolution> exports = getExports(store);
        ExportResolution binding = exports.get(key);
        if (binding != null) {
            return getBindingValue(binding);
        } else {
            return null;
        }
    }

    static Object getBindingValue(ExportResolution binding) {
        TruffleString bindingName = binding.getBindingName();
        JSModuleRecord targetModule = binding.getModule();
        MaterializedFrame targetEnv = targetModule.getEnvironment();
        if (targetEnv == null) {
            // Module has not been linked yet.
            throw Errors.createReferenceErrorNotDefined(bindingName, null);
        }
        if (binding.isNamespace()) {
            return targetModule.getContext().getEvaluator().getModuleNamespace(targetModule);
        }
        FrameDescriptor targetEnvDesc = targetEnv.getFrameDescriptor();
        int slot = JSFrameUtil.findRequiredFrameSlotIndex(targetEnvDesc, bindingName);
        if (JSFrameUtil.hasTemporalDeadZone(targetEnvDesc, slot) && targetEnv.getTag(slot) == FrameSlotKind.Illegal.tag) {
            // If it is an uninitialized binding, throw a ReferenceError
            throw Errors.createReferenceErrorNotDefined(bindingName, null);
        }
        return targetEnv.getValue(slot);
    }

    @Override
    public boolean hasProperty(JSDynamicObject thisObj, Object key) {
        if (!Strings.isTString(key)) {
            return super.hasProperty(thisObj, key);
        }
        Map<TruffleString, ExportResolution> exports = getExports(thisObj);
        return Boundaries.mapContainsKey(exports, (TruffleString) key);
    }

    @Override
    @TruffleBoundary
    public boolean hasOwnProperty(JSDynamicObject thisObj, Object key) {
        if (!Strings.isTString(key)) {
            return super.hasOwnProperty(thisObj, key);
        }
        Map<TruffleString, ExportResolution> exports = getExports(thisObj);
        ExportResolution binding = exports.get(key);
        if (binding != null) {
            // checks for uninitialized bindings
            getBindingValue(binding);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean delete(JSDynamicObject thisObj, long index, boolean isStrict) {
        return true;
    }

    @Override
    public boolean delete(JSDynamicObject thisObj, Object key, boolean isStrict) {
        if (!Strings.isTString(key)) {
            return super.delete(thisObj, key, isStrict);
        }
        if (Boundaries.mapContainsKey(getExports(thisObj), (TruffleString) key)) {
            if (isStrict) {
                throw Errors.createTypeErrorNotConfigurableProperty(key);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean setPrototypeOf(JSDynamicObject thisObj, JSDynamicObject newPrototype) {
        return newPrototype == Null.instance;
    }

    @Override
    public boolean defineOwnProperty(JSDynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        if (!Strings.isTString(key)) {
            return super.defineOwnProperty(thisObj, key, desc, doThrow);
        }
        PropertyDescriptor current = getOwnProperty(thisObj, key);
        if (current != null && !desc.isAccessorDescriptor() && desc.getIfHasWritable(true) && desc.getIfHasEnumerable(true) && !desc.getIfHasConfigurable(false) &&
                        (!desc.hasValue() || JSRuntime.isSameValue(desc.getValue(), current.getValue()))) {
            return true;
        }
        return DefinePropertyUtil.reject(doThrow, "not allowed to defineProperty on a namespace object");
    }

    @Override
    @TruffleBoundary
    public PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        if (!Strings.isTString(key)) {
            return super.getOwnProperty(thisObj, key);
        }
        Map<TruffleString, ExportResolution> exports = getExports(thisObj);
        ExportResolution binding = exports.get(key);
        if (binding != null) {
            Object value = getBindingValue(binding);
            return PropertyDescriptor.createData(value, true, true, false);
        } else {
            return null;
        }
    }

    public static boolean isJSModuleNamespace(Object obj) {
        return obj instanceof JSModuleNamespaceObject;
    }

    @TruffleBoundary
    @Override
    public List<Object> getOwnPropertyKeys(JSDynamicObject thisObj, boolean strings, boolean symbols) {
        List<Object> symbolKeys = symbols ? symbolKeys(thisObj) : Collections.emptyList();
        if (!strings) {
            return symbolKeys;
        }
        Map<TruffleString, ExportResolution> exports = getExports(thisObj);
        List<Object> keys = new ArrayList<>(exports.size() + symbolKeys.size());
        // TODO: convert these keys earlier
        keys.addAll(exports.keySet());
        keys.addAll(symbolKeys);
        return keys;
    }

    private static List<Object> symbolKeys(JSDynamicObject thisObj) {
        // Module Namespace objects only have symbol keys in their shapes.
        return thisObj.getShape().getKeyList();
    }

    @Override
    @TruffleBoundary
    public boolean setIntegrityLevel(JSDynamicObject obj, boolean freeze, boolean doThrow) {
        if (freeze) {
            Map<TruffleString, ExportResolution> exports = getExports(obj);
            if (!exports.isEmpty()) {
                ExportResolution firstBinding = exports.values().iterator().next();
                // Throw ReferenceError if the first binding is uninitialized,
                // throw TypeError otherwise
                // checks for an uninitialized binding
                getBindingValue(firstBinding);
                throw Errors.createTypeError("not allowed to freeze a namespace object");
            }
        } else {
            // Check for uninitialized bindings
            for (ExportResolution binding : getExports(obj).values()) {
                // can throw ReferenceError
                getBindingValue(binding);
            }
        }
        return true;
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, Object key, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (isStrict) {
            throw Errors.createTypeErrorNotExtensible(thisObj, key);
        }
        return false;
    }

    @TruffleBoundary
    @Override
    public boolean set(JSDynamicObject thisObj, long index, Object value, Object receiver, boolean isStrict, Node encapsulatingNode) {
        if (isStrict) {
            throw Errors.createTypeErrorNotExtensible(thisObj, Strings.fromLong(index));
        }
        return false;
    }

    @Override
    public boolean usesOrdinaryGetOwnProperty() {
        return false;
    }
}
