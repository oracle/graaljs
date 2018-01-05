/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.builtins;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Module Namespace Exotic Objects.
 */
public final class JSModuleNamespace extends JSBuiltinObject {

    public static final JSModuleNamespace INSTANCE = new JSModuleNamespace();

    public static final String CLASS_NAME = "Module";

    private static final HiddenKey MODULE_ID = new HiddenKey("module");
    private static final HiddenKey EXPORTS_ID = new HiddenKey("exports");

    /**
     * [[Module]]. Module Record.
     *
     * The Module Record whose exports this namespace exposes.
     */
    private static final Property MODULE_PROPERTY;

    /**
     * [[Exports]]. List of String.
     *
     * A List containing the String values of the exported names exposed as own properties of this
     * object. The list is ordered as if an Array of those String values had been sorted using
     * Array.prototype.sort using SortCompare as comparefn.
     */
    private static final Property EXPORTS_PROPERTY;

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        MODULE_PROPERTY = JSObjectUtil.makeHiddenProperty(MODULE_ID, allocator.locationForType(Object.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
        EXPORTS_PROPERTY = JSObjectUtil.makeHiddenProperty(EXPORTS_ID, allocator.locationForType(Object.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)));
    }

    private JSModuleNamespace() {
    }

    public static JSModuleRecord getModule(DynamicObject obj) {
        assert isJSModuleNamespace(obj);
        return (JSModuleRecord) MODULE_PROPERTY.get(obj, isJSModuleNamespace(obj));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ExportResolution> getExports(DynamicObject obj) {
        assert isJSModuleNamespace(obj);
        return (Map<String, ExportResolution>) EXPORTS_PROPERTY.get(obj, isJSModuleNamespace(obj));
    }

    public static DynamicObject create(JSContext context, Object module, Map<String, ExportResolution> exports) {
        DynamicObject obj = JSObject.create(context, context.getModuleNamespaceFactory(), module, exports);
        assert isJSModuleNamespace(obj);
        return obj;
    }

    public static Shape makeInitialShape(JSContext context) {
        Shape initialShape = JSShape.makeEmptyRoot(JSObject.LAYOUT, INSTANCE, context);
        initialShape = initialShape.addProperty(MODULE_PROPERTY);
        initialShape = initialShape.addProperty(EXPORTS_PROPERTY);

        /*
         * The initial value of the @@toStringTag property is the String value "Module".
         *
         * This property has the attributes { [[Writable]]: false, [[Enumerable]]: false,
         * [[Configurable]]: true }.
         */
        Property toStringTagProperty = JSObjectUtil.makeDataProperty(Symbol.SYMBOL_TO_STRING_TAG, initialShape.allocator().constantLocation(CLASS_NAME),
                        JSAttributes.configurableNotEnumerableNotWritable());
        initialShape = initialShape.addProperty(toStringTagProperty);

        return JSShape.makeNotExtensible(initialShape);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    @TruffleBoundary
    public String safeToString(DynamicObject obj) {
        return "[" + CLASS_NAME + "]";
    }

    @Override
    @TruffleBoundary
    public Object getOwnHelper(DynamicObject store, Object thisObj, Object key) {
        if (!(key instanceof String)) {
            return super.getOwnHelper(store, thisObj, key);
        }
        Map<String, ExportResolution> exports = getExports(store);
        ExportResolution binding = exports.get(key);
        if (binding != null) {
            return getBindingValue(binding);
        } else {
            return Undefined.instance;
        }
    }

    private static Object getBindingValue(ExportResolution binding) {
        JSModuleRecord targetModule = binding.getModule();
        MaterializedFrame targetEnv = targetModule.getEnvironment();
        FrameSlot frameSlot = targetEnv.getFrameDescriptor().findFrameSlot(binding.getBindingName());
        assert frameSlot != null;
        if (JSFrameUtil.hasTemporalDeadZone(frameSlot) && targetEnv.isObject(frameSlot) && FrameUtil.getObjectSafe(targetEnv, frameSlot) == Dead.instance()) {
            // If it is an uninitialized binding, throw a ReferenceError
            throw Errors.createReferenceErrorNotDefined(frameSlot.getIdentifier(), null);
        }
        return targetEnv.getValue(frameSlot);
    }

    @Override
    public boolean hasOwnProperty(DynamicObject thisObj, Object key) {
        if (!(key instanceof String)) {
            return super.hasOwnProperty(thisObj, key);
        }
        Map<String, ExportResolution> exports = getExports(thisObj);
        return Boundaries.mapContainsKey(exports, key);
    }

    @Override
    public boolean delete(DynamicObject thisObj, long index, boolean isStrict) {
        return true;
    }

    @Override
    public boolean delete(DynamicObject thisObj, Object key, boolean isStrict) {
        return !Boundaries.mapContainsKey(getExports(thisObj), key);
    }

    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        return false;
    }

    @Override
    public boolean defineOwnProperty(DynamicObject thisObj, Object key, PropertyDescriptor desc, boolean doThrow) {
        return false;
    }

    @Override
    @TruffleBoundary
    public PropertyDescriptor getOwnProperty(DynamicObject thisObj, Object key) {
        if (!(key instanceof String)) {
            return super.getOwnProperty(thisObj, key);
        }
        Map<String, ExportResolution> exports = getExports(thisObj);
        ExportResolution binding = exports.get(key);
        if (binding != null) {
            Object value = getBindingValue(binding);
            return PropertyDescriptor.createData(value, true, true, false);
        } else {
            return null;
        }
    }

    public static boolean isJSModuleNamespace(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSModuleNamespace((DynamicObject) obj);
    }

    public static boolean isJSModuleNamespace(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @TruffleBoundary
    @Override
    public List<Object> ownPropertyKeys(DynamicObject thisObj) {
        Map<String, ExportResolution> exports = getExports(thisObj);
        List<Object> symbolKeys = super.ownPropertyKeysList(thisObj);
        List<Object> keys = new ArrayList<>(exports.size() + symbolKeys.size());
        keys.addAll(exports.keySet());
        keys.addAll(symbolKeys);
        return keys;
    }

}
