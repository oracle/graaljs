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
package com.oracle.truffle.js.builtins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.JSArrayOperation;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectAssignNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectCreateNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertiesNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertyNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectFromEntriesNodeGen;
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
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode;
import com.oracle.truffle.js.nodes.access.ToPropertyDescriptorNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.array.dyn.LazyArrayGenerator;

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
        entries(1),

        // ES2019
        fromEntries(1);

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
                return JSTruffleOptions.ECMAScript2017;
            } else if (this == fromEntries) {
                return JSTruffleOptions.ECMAScript2019;
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
            case fromEntries:
                return ObjectFromEntriesNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ObjectGetPrototypeOfNode extends ObjectOperation {
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;

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
                    return JSObject.getPrototype((DynamicObject) tobject);
                } else {
                    if (getContext().getContextOptions().hasForeignObjectPrototype()) {
                        return getForeignObjectPrototype(tobject);
                    } else {
                        return Null.instance;
                    }
                }
            }
        }

        private DynamicObject getForeignObjectPrototype(TruffleObject truffleObject) {
            assert JSRuntime.isForeignObject(truffleObject);
            if (foreignObjectPrototypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
            }
            return foreignObjectPrototypeNode.executeDynamicObject(truffleObject);
        }

        @Specialization(guards = "isJSObject(object)")
        protected DynamicObject getPrototypeOf(DynamicObject object,
                        @Cached("create()") JSClassProfile classProfile) {
            return JSObject.getPrototype(object, classProfile);
        }
    }

    public abstract static class ObjectGetOwnPropertyDescriptorNode extends ObjectOperation {
        public ObjectGetOwnPropertyDescriptorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        @Child private JSGetOwnPropertyNode getOwnPropertyNode = JSGetOwnPropertyNode.create();

        @Specialization
        protected DynamicObject getOwnPropertyDescriptor(Object thisObj, Object propertyKey) {
            TruffleObject tobject = toTruffleObject(thisObj);
            if (JSObject.isJSObject(tobject)) {
                DynamicObject object = (DynamicObject) tobject;
                PropertyDescriptor desc = getOwnPropertyNode.execute(object, toPropertyKeyNode.execute(propertyKey));
                return JSRuntime.fromPropertyDescriptor(desc, getContext());
            } else {
                return Undefined.instance;
            }
        }
    }

    public abstract static class ObjectGetOwnPropertyDescriptorsNode extends ObjectOperation {
        private final JSClassProfile classProfile = JSClassProfile.create();
        @Child private JSGetOwnPropertyNode getOwnPropertyNode = JSGetOwnPropertyNode.create();

        public ObjectGetOwnPropertyDescriptorsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject getOwnPropertyDescriptors(Object obj) {
            TruffleObject thisTObj = toTruffleObject(obj);
            if (JSObject.isJSObject(thisTObj)) {
                DynamicObject thisObj = (DynamicObject) thisTObj;
                DynamicObject retObj = JSUserObject.create(getContext());

                for (Iterator<Object> iterator = Boundaries.iterator(JSObject.ownPropertyKeys(thisObj, classProfile)); Boundaries.iteratorHasNext(iterator);) {
                    Object key = Boundaries.iteratorNext(iterator);
                    assert JSRuntime.isPropertyKey(key);
                    PropertyDescriptor desc = getOwnPropertyNode.execute(thisObj, key);
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

    public abstract static class ObjectGetOwnPropertyNamesOrSymbolsNode extends ObjectOperation {
        protected final boolean symbols;
        final ConditionProfile isJSString = ConditionProfile.createBinaryProfile();

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
            if (!symbols && isJSString.profile(JSString.isJSString(object))) {
                LazyArrayGenerator gen = JSString.ownPropertyKeysGenerator(object);
                return JSArray.createLazyArray(getContext(), gen);
            }
            return JSRuntime.getOwnPropertyKeys(getContext(), object, symbols);
        }

        @Specialization(guards = {"isForeignObject(thisObj)", "symbols"})
        protected DynamicObject getForeignObjectSymbols(@SuppressWarnings("unused") TruffleObject thisObj) {
            // TrufleObjects can never have symbols.
            return JSArray.createConstantEmptyArray(getContext());
        }

        @Specialization(guards = {"isForeignObject(thisObj)", "!symbols"}, limit = "3")
        protected DynamicObject getForeignObjectNames(TruffleObject thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop,
                        @CachedLibrary(limit = "3") InteropLibrary members) {
            Object[] array;
            if (interop.hasMembers(thisObj)) {
                try {
                    Object keysObj = interop.getMembers(thisObj);
                    long size = members.getArraySize(keysObj);
                    if (size < 0 || size >= Integer.MAX_VALUE) {
                        throw Errors.createRangeErrorInvalidArrayLength();
                    }
                    array = new Object[(int) size];
                    for (int i = 0; i < size; i++) {
                        Object key = members.readArrayElement(keysObj, i);
                        assert InteropLibrary.getFactory().getUncached().isString(key);
                        array[i] = key;
                    }
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    array = ScriptArray.EMPTY_OBJECT_ARRAY;
                }
            } else {
                array = ScriptArray.EMPTY_OBJECT_ARRAY;
            }
            return JSArray.createConstant(getContext(), array);
        }
    }

    protected abstract static class ObjectDefineOperation extends ObjectOperation {

        @Child private ToPropertyDescriptorNode toPropertyDescriptorNode;

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

        @TruffleBoundary
        protected DynamicObject intlDefineProperties(DynamicObject obj, DynamicObject descs) {
            List<Pair<Object, PropertyDescriptor>> descriptors = new ArrayList<>();
            JSClass descsClass = JSObject.getJSClass(descs);
            for (Object key : descsClass.ownPropertyKeys(descs)) {
                PropertyDescriptor keyDesc = descsClass.getOwnProperty(descs, key);
                if (keyDesc.getEnumerable()) {
                    PropertyDescriptor desc = toPropertyDescriptor(descsClass.get(descs, key));
                    Boundaries.listAdd(descriptors, new Pair<>(key, desc));
                }
            }
            for (Pair<Object, PropertyDescriptor> descPair : descriptors) {
                JSRuntime.definePropertyOrThrow(obj, descPair.getFirst(), descPair.getSecond());
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
            DynamicObject ret = JSObject.create(getContext(), getContext().getEmptyShapeNullPrototype());
            return objectDefineProperties(ret, properties);
        }

        @TruffleBoundary
        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSNull(prototype)", "!isJSObject(prototype)"})
        protected DynamicObject createInvalidPrototype(Object prototype, Object properties) {
            assert prototype != null;
            throw Errors.createTypeErrorInvalidPrototype(prototype);
        }

        @Specialization(guards = {"isJSObject(prototype)", "isJSObject(properties)"})
        protected DynamicObject create(VirtualFrame frame, DynamicObject prototype, DynamicObject properties) {
            DynamicObject ret = createObjectWithPrototype(frame, prototype);
            intlDefineProperties(ret, properties);
            return ret;
        }

        @Specialization(guards = {"isJSObject(prototype)", "!isJSNull(properties)"})
        protected DynamicObject create(VirtualFrame frame, DynamicObject prototype, Object properties) {
            DynamicObject ret = createObjectWithPrototype(frame, prototype);
            return objectDefineProperties(ret, properties);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSObject(prototype)", "isJSNull(properties)"})
        protected DynamicObject createNull(DynamicObject prototype, Object properties) {
            throw Errors.createTypeErrorNotObjectCoercible(properties);
        }

        private DynamicObject objectDefineProperties(DynamicObject ret, Object properties) {
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
            JSRuntime.definePropertyOrThrow(object, propertyKey, desc);
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

    public abstract static class ObjectKeysNode extends ObjectOperation {
        @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
        private final ConditionProfile hasElements = ConditionProfile.createBinaryProfile();
        private final ConditionProfile oneElement = ConditionProfile.createBinaryProfile();
        private final ValueProfile listClassProfile = ValueProfile.createClassProfile();

        public ObjectKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSType(thisObj)")
        protected DynamicObject keysDynamicObject(DynamicObject thisObj) {
            List<? extends Object> propertyList = listClassProfile.profile(enumerableOwnPropertyNames(toOrAsObject(thisObj)));
            return keysIntl(propertyList);
        }

        @Specialization
        protected DynamicObject keys(Symbol symbol) {
            return keysDynamicObject(toOrAsObject(symbol));
        }

        @Specialization
        protected DynamicObject keys(JSLazyString string) {
            return keysDynamicObject(toOrAsObject(string));
        }

        @Specialization
        protected DynamicObject keys(LargeInteger largeInteger) {
            return keysDynamicObject(toOrAsObject(largeInteger));
        }

        @Specialization
        protected DynamicObject keys(BigInt bigInt) {
            return keysDynamicObject(toOrAsObject(bigInt));
        }

        @Specialization(guards = "!isTruffleObject(thisObj)")
        protected DynamicObject keys(Object thisObj) {
            return keysDynamicObject(toOrAsObject(thisObj));
        }

        @Specialization(guards = "isForeignObject(thisObj)", limit = "3")
        protected DynamicObject keys(TruffleObject thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop,
                        @CachedLibrary(limit = "3") InteropLibrary members) {
            return keysIntl(getKeysList(interop, members, thisObj));
        }

        private static List<Object> getKeysList(InteropLibrary interop, InteropLibrary members, TruffleObject obj) {
            if (interop.hasMembers(obj)) {
                try {
                    Object keysObj = interop.getMembers(obj);
                    long size = members.getArraySize(keysObj);
                    if (size < 0 || size >= Integer.MAX_VALUE) {
                        throw Errors.createRangeErrorInvalidArrayLength();
                    }
                    List<Object> keys = new ArrayList<>((int) size);
                    for (int i = 0; i < size; i++) {
                        Object key = members.readArrayElement(keysObj, i);
                        assert InteropLibrary.getFactory().getUncached().isString(key);
                        keys.add(key);
                    }
                    return keys;
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        }

        private DynamicObject keysIntl(List<? extends Object> propertyList) {
            int len = Boundaries.listSize(propertyList);
            if (hasElements.profile(len > 0)) {
                final Object[] arr = new Object[len];
                if (oneElement.profile(len == 1)) {
                    arr[0] = Boundaries.listGet(propertyList, 0);
                    assert arr[0] instanceof String;
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
                assert propName instanceof String;
                arr[i++] = propName;
            }
        }

        private List<? extends Object> enumerableOwnPropertyNames(DynamicObject obj) {
            if (enumerableOwnPropertyNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                enumerableOwnPropertyNamesNode = insert(EnumerableOwnPropertyNamesNode.createKeys(getContext()));
            }
            return enumerableOwnPropertyNamesNode.execute(obj);
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
            throw Errors.createTypeErrorInvalidPrototype(newProto);
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
        @Child private JSGetOwnPropertyNode getOwnPropertyNode;

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
                    for (Iterator<Object> iterator = Boundaries.iterator(JSObject.ownPropertyKeys(from, classProfile)); Boundaries.iteratorHasNext(iterator);) {
                        Object nextKey = Boundaries.iteratorNext(iterator);
                        PropertyDescriptor desc = getOwnProperty(from, nextKey);
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

        private PropertyDescriptor getOwnProperty(DynamicObject obj, Object key) {
            if (getOwnPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getOwnPropertyNode = insert(JSGetOwnPropertyNode.create());
            }
            return getOwnPropertyNode.execute(obj, key);
        }
    }

    public abstract static class ObjectValuesOrEntriesNode extends ObjectOperation {
        private final boolean entries;

        @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
        @Child private InteropLibrary interop;
        @Child private InteropLibrary members;
        @Child private JSForeignToJSTypeNode importValue;
        @Child private InteropLibrary asString;

        public ObjectValuesOrEntriesNode(JSContext context, JSBuiltin builtin, boolean entries) {
            super(context, builtin);
            this.entries = entries;
        }

        @Specialization(guards = "isJSObject(obj)")
        protected DynamicObject valuesOrEntriesJSObject(DynamicObject obj) {
            List<? extends Object> list = enumerableOwnPropertyNames(obj);
            return JSRuntime.createArrayFromList(getContext(), list);
        }

        @Specialization(replaces = "valuesOrEntriesJSObject")
        protected DynamicObject valuesOrEntriesGeneric(Object obj,
                        @Cached("createBinaryProfile()") ConditionProfile isJSObjectProfile) {
            TruffleObject thisObj = toTruffleObject(obj);
            List<? extends Object> list;
            if (isJSObjectProfile.profile(JSObject.isJSObject(thisObj))) {
                list = enumerableOwnPropertyNames((DynamicObject) thisObj);
            } else {
                list = enumerableOwnPropertyNamesForeign(thisObj);
            }
            return JSRuntime.createArrayFromList(getContext(), list);
        }

        protected List<? extends Object> enumerableOwnPropertyNames(DynamicObject obj) {
            if (enumerableOwnPropertyNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                enumerableOwnPropertyNamesNode = insert(entries ? EnumerableOwnPropertyNamesNode.createKeysValues(getContext()) : EnumerableOwnPropertyNamesNode.createValues(getContext()));
            }
            return enumerableOwnPropertyNamesNode.execute(obj);
        }

        protected List<Object> enumerableOwnPropertyNamesForeign(TruffleObject thisObj) {
            assert JSRuntime.isForeignObject(thisObj);
            if (interop == null || members == null || importValue == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interop = insert(InteropLibrary.getFactory().createDispatched(3));
                members = insert(InteropLibrary.getFactory().createDispatched(3));
                importValue = insert(JSForeignToJSTypeNode.create());
            }
            try {
                Object keysObj = interop.getMembers(thisObj);
                long size = members.getArraySize(keysObj);
                if (size < 0 || size >= Integer.MAX_VALUE) {
                    throw Errors.createRangeErrorInvalidArrayLength();
                }
                List<Object> values = new ArrayList<>((int) size);
                for (int i = 0; i < size; i++) {
                    Object key = members.readArrayElement(keysObj, i);
                    String stringKey = asStringKey(key);
                    Object value = importValue.executeWithTarget(interop.readMember(thisObj, stringKey));
                    if (entries) {
                        value = JSArray.createConstant(getContext(), new Object[]{key, value});
                    }
                    values.add(value);
                }
                return values;
            } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                return Collections.emptyList();
            }
        }

        private String asStringKey(Object key) throws UnsupportedMessageException {
            assert InteropLibrary.getFactory().getUncached().isString(key);
            if (key instanceof String) {
                return (String) key;
            } else {
                if (asString == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    asString = insert(InteropLibrary.getFactory().createDispatched(3));
                }
                return asString.asString(key);
            }
        }
    }

    public abstract static class ObjectFromEntriesNode extends ObjectOperation {
        @Child private RequireObjectCoercibleNode requireObjectCoercibleNode = RequireObjectCoercibleNode.create();
        @Child private GetIteratorNode getIteratorNode;
        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private IsObjectNode isObjectNode = IsObjectNode.create();
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        @Child private ReadElementNode readElementNode;
        private final BranchProfile errorBranch = BranchProfile.create();

        public ObjectFromEntriesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getIteratorNode = GetIteratorNode.create(context);
            this.iteratorStepNode = IteratorStepNode.create(context);
            this.iteratorValueNode = IteratorValueNode.create(context);
            this.readElementNode = ReadElementNode.create(context);
        }

        @Specialization
        protected DynamicObject entries(Object iterable) {
            requireObjectCoercibleNode.execute(iterable);
            DynamicObject obj = JSUserObject.create(getContext());
            return addEntriesFromIterable(obj, iterable);
        }

        private DynamicObject addEntriesFromIterable(DynamicObject target, Object iterable) {
            assert !JSRuntime.isNullOrUndefined(target);
            IteratorRecord iteratorRecord = getIteratorNode.execute(iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return target;
                    }
                    Object nextItem = iteratorValueNode.execute((DynamicObject) next);
                    if (!isObjectNode.executeBoolean(nextItem)) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorIteratorResultNotObject(nextItem, this);
                    }
                    Object k = readElementNode.executeWithTargetAndIndex(nextItem, 0);
                    Object v = readElementNode.executeWithTargetAndIndex(nextItem, 1);
                    createDataPropertyOnObject(target, k, v);
                }
            } catch (Exception ex) {
                errorBranch.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        private void createDataPropertyOnObject(DynamicObject thisObject, Object key, Object value) {
            assert JSRuntime.isObject(thisObject);
            Object propertyKey = toPropertyKeyNode.execute(key);
            JSRuntime.createDataPropertyOrThrow(thisObject, propertyKey, value);
        }

        private void iteratorCloseAbrupt(DynamicObject iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }
    }

}
