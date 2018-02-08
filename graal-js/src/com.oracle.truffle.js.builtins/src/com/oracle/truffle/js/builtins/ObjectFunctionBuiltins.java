/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperation;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectAssignNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectCreateNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertiesNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertyNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetOwnPropertyDescriptorNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetOwnPropertyDescriptorsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetOwnPropertyNamesOrSymbolsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectIsExtensibleNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectIsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectKeysNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectPreventExtensionsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectSetIntegrityLevelNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectSetPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectTestIntegrityLevelNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectValuesOrEntriesNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltins.ObjectOperation;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode;
import com.oracle.truffle.js.nodes.access.ToPropertyDescriptorNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * Contains builtins for {@linkplain DynamicObject} function (constructor).
 */
public final class ObjectFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<ObjectFunctionBuiltins.ObjectFunction> {
    protected ObjectFunctionBuiltins() {
        super(JSUserObject.CLASS_NAME, ObjectFunction.class);
    }

    public enum ObjectFunction implements BuiltinEnum<ObjectFunction> {
        create(2),
        defineProperties(2),
        defineProperty(3),
        freeze(1),
        getOwnPropertyDescriptor(2),
        getOwnPropertyNames(1),
        getPrototypeOf(1),
        isExtensible(1),
        isFrozen(1),
        isSealed(1),
        keys(1),
        preventExtensions(1),
        seal(1),
        setPrototypeOf(2),

        // ES6
        is(2),
        getOwnPropertySymbols(1),
        assign(2),

        // ES8
        getOwnPropertyDescriptors(1),
        values(1),
        entries(1);

        private final int length;

        ObjectFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (EnumSet.of(is, getOwnPropertySymbols, assign).contains(this)) {
                return 6;
            } else if (EnumSet.of(getOwnPropertyDescriptors, values, entries).contains(this)) {
                return 8;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ObjectFunction builtinEnum) {
        switch (builtinEnum) {
            case create:
                return ObjectCreateNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case defineProperties:
                return ObjectDefinePropertiesNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case defineProperty:
                return ObjectDefinePropertyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case freeze:
                return ObjectSetIntegrityLevelNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case getOwnPropertyDescriptor:
                return ObjectGetOwnPropertyDescriptorNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case getOwnPropertyDescriptors:
                return ObjectGetOwnPropertyDescriptorsNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case getOwnPropertyNames:
                return ObjectGetOwnPropertyNamesOrSymbolsNodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case getPrototypeOf:
                return ObjectGetPrototypeOfNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isExtensible:
                return ObjectIsExtensibleNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isFrozen:
                return ObjectTestIntegrityLevelNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case isSealed:
                return ObjectTestIntegrityLevelNodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case keys:
                return ObjectKeysNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case preventExtensions:
                return ObjectPreventExtensionsNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case seal:
                return ObjectSetIntegrityLevelNodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case setPrototypeOf:
                return ObjectSetPrototypeOfNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));

            case is:
                return ObjectIsNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case getOwnPropertySymbols:
                return ObjectGetOwnPropertyNamesOrSymbolsNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
            case assign:
                return ObjectAssignNodeGen.create(context, builtin, args().fixedArgs(1).varArgs().createArgumentNodes(context));
            case values:
                return ObjectValuesOrEntriesNodeGen.create(context, builtin, false, args().fixedArgs(1).createArgumentNodes(context));
            case entries:
                return ObjectValuesOrEntriesNodeGen.create(context, builtin, true, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ObjectGetPrototypeOfNode extends ObjectOperation {

        public ObjectGetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "!isJSObject(object)")
        protected DynamicObject getPrototypeOf(Object object) {
            if (getContext().getEcmaScriptVersion() < 6) {
                if (JSRuntime.isJSPrimitive(object)) {
                    throw Errors.createTypeErrorNotAnObject(object);
                } else {
                    return Null.instance;
                }
            } else {
                TruffleObject tobject = toTruffleObject(object);
                if (JSObject.isJSObject(tobject)) {
                    return getPrototypeOf((DynamicObject) tobject);
                } else {
                    return Null.instance;
                }
            }
        }

        @Specialization(guards = "isJSObject(object)")
        protected DynamicObject getPrototypeOf(DynamicObject object) {
            return JSObject.getPrototype(object);
        }
    }

    public abstract static class ObjectGetOwnPropertyDescriptorNode extends ObjectOperation {
        public ObjectGetOwnPropertyDescriptorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        private final JSClassProfile classProfile = JSClassProfile.create();

        @Specialization
        protected DynamicObject getOwnPropertyDescriptor(Object thisObj, Object propertyKey) {
            TruffleObject tobject = toTruffleObject(thisObj);
            if (JSObject.isJSObject(tobject)) {
                DynamicObject object = (DynamicObject) tobject;
                PropertyDescriptor desc = JSObject.getOwnProperty(object, toPropertyKeyNode.execute(propertyKey), classProfile);
                return JSRuntime.fromPropertyDescriptor(desc, getContext());
            } else {
                return Undefined.instance;
            }
        }
    }

    public abstract static class ObjectGetOwnPropertyDescriptorsNode extends ObjectOperation {
        private final JSClassProfile classProfile = JSClassProfile.create();

        public ObjectGetOwnPropertyDescriptorsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject getOwnPropertyDescriptor(Object obj) {
            TruffleObject thisTObj = toTruffleObject(obj);
            if (JSObject.isJSObject(thisTObj)) {
                DynamicObject thisObj = (DynamicObject) thisTObj;
                DynamicObject retObj = JSUserObject.create(getContext());

                for (Object key : JSObject.ownPropertyKeys(thisObj)) {
                    assert JSRuntime.isPropertyKey(key);
                    PropertyDescriptor desc = JSObject.getOwnProperty(thisObj, key, classProfile);
                    if (desc != null) {
                        DynamicObject propDesc = JSRuntime.fromPropertyDescriptor(desc, getContext());
                        retObj.define(key, propDesc, JSAttributes.configurableEnumerableWritable());
                    }
                }
                return retObj;
            } else {
                return Undefined.instance;
            }
        }
    }

    @ImportStatic(JSInteropUtil.class)
    public abstract static class ObjectGetOwnPropertyNamesOrSymbolsNode extends ObjectOperation {
        protected final boolean symbols;

        public ObjectGetOwnPropertyNamesOrSymbolsNode(JSContext context, JSBuiltin builtin, boolean symbols) {
            super(context, builtin);
            this.symbols = symbols;
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject getJSObject(DynamicObject thisObj) {
            return JSRuntime.getOwnPropertyKeys(getContext(), thisObj, symbols);
        }

        @Specialization(guards = {"!isJSObject(thisObj)", "!isForeignObject(thisObj)"})
        protected DynamicObject getDefault(Object thisObj) {
            DynamicObject object = toOrAsObject(thisObj);
            return JSRuntime.getOwnPropertyKeys(getContext(), object, symbols);
        }

        @Specialization(guards = {"isForeignObject(thisObj)", "symbols"})
        protected DynamicObject getForeignObjectSymbols(@SuppressWarnings("unused") TruffleObject thisObj) {
            // TrufleObjects can never have symbols.
            return JSArray.createConstantEmptyArray(getContext());
        }

        @Specialization(guards = {"isForeignObject(thisObj)", "!symbols"})
        protected DynamicObject getForeignObjectNames(TruffleObject thisObj,
                        @Cached("createKeys()") Node keysNode,
                        @Cached("createRead()") Node readNode,
                        @Cached("createGetSize()") Node getSizeNode) {
            return JSArray.createConstant(getContext(), Boundaries.listToArray(JSInteropNodeUtil.keys(thisObj, keysNode, readNode, getSizeNode, true)));
        }
    }

    protected abstract static class ObjectDefineOperation extends ObjectOperation {

        @Child private ToPropertyDescriptorNode toPropertyDescriptorNode;
        private final ValueProfile descClassProfile = ValueProfile.createClassProfile();

        public ObjectDefineOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected PropertyDescriptor toPropertyDescriptor(Object target) {
            if (toPropertyDescriptorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toPropertyDescriptorNode = insert(ToPropertyDescriptorNode.create(getContext()));
            }
            return (PropertyDescriptor) toPropertyDescriptorNode.execute(target);
        }

        protected DynamicObject intlDefineProperties(DynamicObject obj, DynamicObject descs) {
            List<Pair<Object, PropertyDescriptor>> descriptors = new ArrayList<>();
            JSClass descClass = descClassProfile.profile(JSObject.getJSClass(descs));
            for (Object key : descClass.ownPropertyKeys(descs)) {
                PropertyDescriptor keyDesc = descClass.getOwnProperty(descs, key);
                if (keyDesc.getEnumerable()) {
                    PropertyDescriptor desc = toPropertyDescriptor(descClass.get(descs, key));
                    Boundaries.listAdd(descriptors, new Pair<>(key, desc));
                }
            }
            JSContext context = JSObject.getJSContext(obj);
            for (Pair<Object, PropertyDescriptor> descPair : descriptors) {
                JSRuntime.definePropertyOrThrow(obj, descPair.getFirst(), descPair.getSecond(), context);
            }
            return obj;
        }
    }

    public abstract static class ObjectCreateNode extends ObjectDefineOperation {
        public ObjectCreateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private CreateObjectNode.CreateObjectWithPrototypeNode objectCreateNode;
        private final BranchProfile needDefineProperties = BranchProfile.create();

        @SuppressWarnings("unused")
        @Specialization(guards = "isJSNull(prototype)")
        protected DynamicObject createPrototypeNull(Object prototype, Object properties) {
            DynamicObject ret = JSObject.create(getContext(), getContext().getEmptyShape());
            return createIntl(properties, ret);
        }

        @TruffleBoundary
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNull(prototype)", "!isJSObject(prototype)"})
        protected DynamicObject createInvalidPrototype(Object prototype, Object properties) {
            assert prototype != null;
            throw Errors.createTypeError("Object prototype may only be an Object or null: " + JSRuntime.safeToString(prototype));
        }

        @Specialization(guards = {"isJSObject(prototype)", "isJSObject(properties)"})
        protected DynamicObject create(VirtualFrame frame, DynamicObject prototype, DynamicObject properties) {
            DynamicObject ret = createObjectWithPrototype(frame, prototype);
            intlDefineProperties(ret, properties);
            return ret;
        }

        @Specialization(guards = {"isJSObject(prototype)", "!isJSNull(prototype)"})
        protected DynamicObject create(VirtualFrame frame, DynamicObject prototype, Object properties) {
            DynamicObject ret = createObjectWithPrototype(frame, prototype);
            return createIntl(properties, ret);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSObject(prototype)", "isJSNull(prototype)"})
        protected DynamicObject createNull(DynamicObject prototype, Object properties) {
            throw Errors.createTypeError("cannot convert to object");
        }

        private DynamicObject createIntl(Object properties, DynamicObject ret) {
            if (properties != Undefined.instance) {
                needDefineProperties.enter();
                intlDefineProperties(ret, toObject(properties));
            }
            return ret;
        }

        private DynamicObject createObjectWithPrototype(VirtualFrame frame, DynamicObject prototype) {
            if (objectCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                objectCreateNode = insert(CreateObjectNode.createWithCachedPrototype(getContext(), null));
            }
            return objectCreateNode.executeDynamicObject(frame, prototype);
        }
    }

    public abstract static class ObjectDefinePropertyNode extends ObjectDefineOperation {
        public ObjectDefinePropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();

        @Specialization
        protected DynamicObject defineProperty(Object thisObj, Object property, Object attributes) {
            DynamicObject object = asObject(thisObj);
            PropertyDescriptor desc = toPropertyDescriptor(attributes);
            Object propertyKey = toPropertyKeyNode.execute(property);
            JSRuntime.definePropertyOrThrow(object, propertyKey, desc, getContext());
            return object;
        }

        @Override
        protected JavaScriptNode copyUninitialized() {
            return ObjectDefinePropertyNodeGen.create(getContext(), getBuiltin(), cloneUninitialized(getArguments()));
        }
    }

    public abstract static class ObjectDefinePropertiesNode extends ObjectDefineOperation {

        public ObjectDefinePropertiesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject defineProperties(Object thisObj, Object properties) {
            DynamicObject object = asObject(thisObj);
            return intlDefineProperties(object, toObject(properties));
        }
    }

    public abstract static class ObjectIsExtensibleNode extends ObjectOperation {
        public ObjectIsExtensibleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected boolean isExtensible(DynamicObject thisObj,
                        @Cached("create()") JSClassProfile classProfile) {
            return JSObject.isExtensible(thisObj, classProfile);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected boolean isExtensible(Object thisObj) {
            if (getContext().getEcmaScriptVersion() < 6) {
                throw createTypeErrorCalledOnNonObject(thisObj);
            }
            return false;
        }
    }

    public abstract static class ObjectPreventExtensionsNode extends ObjectOperation {
        public ObjectPreventExtensionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject preventExtensions(DynamicObject thisObj) {
            JSObject.preventExtensions(thisObj);
            return thisObj;
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object preventExtensions(Object thisObj) {
            if (getContext().getEcmaScriptVersion() < 6) {
                throw createTypeErrorCalledOnNonObject(thisObj);
            }
            return thisObj;
        }
    }

    /**
     * Implementing isFrozen, isSealed via testIntegrityLevel().
     *
     */
    public abstract static class ObjectTestIntegrityLevelNode extends ObjectOperation {
        private final boolean frozen;
        private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();

        public ObjectTestIntegrityLevelNode(JSContext context, JSBuiltin builtin, boolean frozen) {
            super(context, builtin);
            this.frozen = frozen;
        }

        @Specialization
        protected boolean testIntegrityLevel(Object thisObj) {
            if (isObject.profile(JSRuntime.isObject(thisObj))) {
                return JSObject.testIntegrityLevel((DynamicObject) thisObj, frozen);
            } else {
                if (getContext().getEcmaScriptVersion() < 6) {
                    throw createTypeErrorCalledOnNonObject(thisObj);
                }
                return true;
            }
        }
    }

    /**
     * SetIntegrityLevel, implements freeze() and seal().
     *
     */
    public abstract static class ObjectSetIntegrityLevelNode extends ObjectOperation {
        private final boolean freeze;
        private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();

        public ObjectSetIntegrityLevelNode(JSContext context, JSBuiltin builtin, boolean freeze) {
            super(context, builtin);
            this.freeze = freeze;
        }

        @Specialization
        protected Object setIntegrityLevel(Object thisObj) {
            if (isObject.profile(JSRuntime.isObject(thisObj))) {
                JSObject.setIntegrityLevel((DynamicObject) thisObj, freeze);
            } else {
                if (getContext().getEcmaScriptVersion() < 6) {
                    throw createTypeErrorCalledOnNonObject(thisObj);
                }
            }
            return thisObj;
        }
    }

    @ImportStatic(value = JSInteropUtil.class)
    public abstract static class ObjectKeysNode extends ObjectOperation {
        private final ConditionProfile hasElements = ConditionProfile.createBinaryProfile();
        private final ConditionProfile oneElement = ConditionProfile.createBinaryProfile();
        private final ValueProfile listClassProfile = ValueProfile.createClassProfile();

        public ObjectKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject keysDynamicObject(DynamicObject thisObj) {
            List<? extends Object> propertyList = listClassProfile.profile(JSObject.enumerableOwnNames(toOrAsObject(thisObj)));
            return keysIntl(propertyList);
        }

        @Specialization
        protected DynamicObject keys(Symbol symbol) {
            return keysDynamicObject(toOrAsObject(symbol));
        }

        @Specialization(guards = "!isTruffleObject(thisObj)")
        protected DynamicObject keys(Object thisObj) {
            return keysDynamicObject(toOrAsObject(thisObj));
        }

        @Specialization(guards = "isForeignObject(thisObj)")
        protected DynamicObject keys(TruffleObject thisObj,
                        @Cached("createKeys()") Node keysNode,
                        @Cached("createRead()") Node readNode,
                        @Cached("createGetSize()") Node getSizeNode) {
            List<Object> propertyList = listClassProfile.profile(JSInteropNodeUtil.keys(thisObj, keysNode, readNode, getSizeNode, true));
            return keysIntl(propertyList);
        }

        private DynamicObject keysIntl(List<? extends Object> propertyList) {
            int len = Boundaries.listSize(propertyList);
            if (hasElements.profile(len > 0)) {
                final Object[] arr = new Object[len];
                if (oneElement.profile(len == 1)) {
                    arr[0] = Boundaries.listGet(propertyList, 0);
                } else {
                    fillArrayFromList(propertyList, arr);
                }
                return JSArray.createConstant(getContext(), arr);
            }
            return JSArray.createEmptyChecked(getContext(), 0);
        }

        @TruffleBoundary
        private static void fillArrayFromList(List<? extends Object> propertyList, Object[] arr) {
            int i = 0;
            for (Object propName : propertyList) {
                arr[i++] = propName;
            }
        }
    }

    public abstract static class ObjectSetPrototypeOfNode extends ObjectOperation {
        @Child private RequireObjectCoercibleNode objectCoercibleNode;
        private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile errorBranch = BranchProfile.create();
        private final JSClassProfile classProfile = JSClassProfile.create();

        public ObjectSetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSObject(newProto)"})
        protected Object setPrototypeOf(Object thisObj, DynamicObject newProto) {
            return setPrototypeOfImpl(thisObj, newProto);
        }

        @Specialization(guards = {"isJSNull(newProto)"})
        protected Object setPrototypeOfNull(Object thisObj, @SuppressWarnings("unused") DynamicObject newProto) {
            return setPrototypeOfImpl(thisObj, Null.instance);
        }

        private Object setPrototypeOfImpl(Object thisObj, DynamicObject newProto) {
            requireObjectCoercible(thisObj);
            if (isObjectProfile.profile(JSObject.isDynamicObject(thisObj))) {
                DynamicObject object = asObject(thisObj);
                if (!JSObject.setPrototype(object, newProto, classProfile)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("setPrototype failed");
                }
                return object;
            } else {
                return thisObj;
            }
        }

        @Specialization(guards = {"!isJSObject(newProto)", "!isJSNull(newProto)"})
        protected Object setPrototypeOfInvalidNewProto(Object thisObj, Object newProto) {
            assert newProto != null;
            asObject(thisObj);
            throw Errors.createTypeErrorNotAnObject(newProto);
        }

        protected final Object requireObjectCoercible(Object target) {
            if (objectCoercibleNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                objectCoercibleNode = insert(RequireObjectCoercibleNode.create());
            }
            return objectCoercibleNode.execute(target);
        }
    }

    public abstract static class ObjectIsNode extends ObjectOperation {
        public ObjectIsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isInt(int a, int b) {
            return a == b;
        }

        @Specialization
        protected boolean isDouble(double a, double b) {
            if (a == 0 && b == 0) {
                return JSRuntime.isNegativeZero(a) == JSRuntime.isNegativeZero(b);
            }
            if (Double.isNaN(a)) {
                return Double.isNaN(b);
            }
            return a == b;
        }

        @Specialization(guards = "isNumberNumber(a,b)") // GR-7577
        protected boolean isNumberNumber(Number a, Number b,
                        @Cached("createSameValue()") JSIdenticalNode doIdenticalNode) {
            return doIdenticalNode.executeBoolean(JSRuntime.doubleValue(a), JSRuntime.doubleValue(b));
        }

        @Specialization(guards = "!isNumberNumber(a, b)")
        protected boolean isObject(Object a, Object b,
                        @Cached("createSameValue()") JSIdenticalNode doIdenticalNode) {
            return doIdenticalNode.executeBoolean(a, b);
        }

        protected boolean isNumberNumber(Object a, Object b) {
            return a instanceof Number && b instanceof Number;
        }
    }

    public abstract static class ObjectAssignNode extends JSArrayOperation {

        private final BranchProfile listProfile = BranchProfile.create();
        private final BranchProfile elementProfile = BranchProfile.create();
        private final JSClassProfile classProfile = JSClassProfile.create();
        private final BranchProfile notAJSObjectBranch = BranchProfile.create();

        public ObjectAssignNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleObject assign(Object target, Object[] sources) {
            TruffleObject to = toObject(target);
            if (sources.length == 0) {
                return to;
            }
            for (Object o : sources) {
                if (o != Undefined.instance && o != Null.instance) {
                    listProfile.enter();
                    DynamicObject from = JSRuntime.expectJSObject(toObject(o), notAJSObjectBranch);
                    for (Object nextKey : JSObject.ownPropertyKeys(from, classProfile)) {
                        PropertyDescriptor desc = JSObject.getOwnProperty(from, nextKey, classProfile);
                        if (desc != null && desc.getEnumerable()) {
                            elementProfile.enter();
                            Object propValue = readAny(from, nextKey);
                            write(to, nextKey, propValue);
                        }
                    }
                }
            }
            return to;
        }
    }

    public abstract static class ObjectValuesOrEntriesNode extends ObjectOperation {
        private final boolean entries;

        public ObjectValuesOrEntriesNode(JSContext context, JSBuiltin builtin, boolean entries) {
            super(context, builtin);
            this.entries = entries;
        }

        @Specialization
        protected DynamicObject valuesOrEntries(Object obj) {
            DynamicObject thisObj = toObject(obj);
            List<Object> list = enumerableOwnProperties(thisObj);
            return JSRuntime.createArrayFromList(getContext(), list);
        }

        @TruffleBoundary
        protected List<Object> enumerableOwnProperties(DynamicObject thisObj) {
            List<Object> properties = new ArrayList<>();
            for (Object key : JSObject.ownPropertyKeys(thisObj)) {
                if (key instanceof String) {
                    String propertyKey = (String) key;
                    PropertyDescriptor desc = JSObject.getOwnProperty(thisObj, propertyKey);
                    if (desc != null && desc.getEnumerable()) {
                        Object value = JSObject.get(thisObj, propertyKey);
                        if (entries) {
                            properties.add(JSArray.createConstant(getContext(), new Object[]{propertyKey, value}));
                        } else {
                            properties.add(value);
                        }

                    }
                }
            }
            return properties;
        }
    }

}
