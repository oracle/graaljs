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
package com.oracle.truffle.js.builtins;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectAssignNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectCreateNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertiesNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectDefinePropertyNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectFromEntriesNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetOwnPropertyDescriptorNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetOwnPropertyDescriptorsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetOwnPropertyNamesOrSymbolsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectGetPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectHasOwnNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectIsExtensibleNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectIsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectKeysNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectPreventExtensionsNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectSetIntegrityLevelNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectSetPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectTestIntegrityLevelNodeGen;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltinsFactory.ObjectValuesOrEntriesNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltins.ObjectOperation;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.builtins.helper.ListSizeNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.FromPropertyDescriptorNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.IsExtensibleNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode;
import com.oracle.truffle.js.nodes.access.ToPropertyDescriptorNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;

/**
 * Contains builtins for {@linkplain DynamicObject} function (constructor).
 */
public final class ObjectFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<ObjectFunctionBuiltins.ObjectFunction> {
    public static final JSBuiltinsContainer BUILTINS = new ObjectFunctionBuiltins();
    public static final JSBuiltinsContainer BUILTINS_NASHORN_COMPAT = new ObjectFunctionNashornCompatBuiltins();

    protected ObjectFunctionBuiltins() {
        super(JSOrdinary.CLASS_NAME, ObjectFunction.class);
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
        fromEntries(1),

        // ES2022
        hasOwn(2);

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
                return JSConfig.ECMAScript2017;
            } else if (this == fromEntries) {
                return JSConfig.ECMAScript2019;
            } else if (this == hasOwn) {
                return JSConfig.ECMAScript2022;
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
            case hasOwn:
                return ObjectHasOwnNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public static final class ObjectFunctionNashornCompatBuiltins extends JSBuiltinsContainer.SwitchEnum<ObjectFunctionNashornCompatBuiltins.ObjectNashornCompat> {
        protected ObjectFunctionNashornCompatBuiltins() {
            super(ObjectNashornCompat.class);
        }

        public enum ObjectNashornCompat implements BuiltinEnum<ObjectNashornCompat> {
            bindProperties(2);

            private final int length;

            ObjectNashornCompat(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ObjectNashornCompat builtinEnum) {
            switch (builtinEnum) {
                case bindProperties:
                    return ObjectFunctionBuiltinsFactory.ObjectBindPropertiesNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            }
            return null;
        }
    }

    public abstract static class ObjectGetPrototypeOfNode extends ObjectOperation {
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;

        public ObjectGetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "!isJSObject(object)")
        protected DynamicObject getPrototypeOfNonObject(Object object) {
            if (getContext().getEcmaScriptVersion() < 6) {
                if (JSRuntime.isJSPrimitive(object)) {
                    throw Errors.createTypeErrorNotAnObject(object);
                } else {
                    return Null.instance;
                }
            } else {
                Object tobject = toObject(object);
                if (JSDynamicObject.isJSDynamicObject(tobject)) {
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

        private DynamicObject getForeignObjectPrototype(Object truffleObject) {
            assert JSRuntime.isForeignObject(truffleObject);
            if (foreignObjectPrototypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
            }
            return foreignObjectPrototypeNode.executeDynamicObject(truffleObject);
        }

        @Specialization(guards = "isJSObject(object)")
        protected DynamicObject getPrototypeOfJSObject(DynamicObject object,
                        @Cached("create()") GetPrototypeNode getPrototypeNode) {
            return getPrototypeNode.executeJSObject(object);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectGetOwnPropertyDescriptorNode extends ObjectOperation {
        public ObjectGetOwnPropertyDescriptorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        @Child private JSGetOwnPropertyNode getOwnPropertyNode = JSGetOwnPropertyNode.create();
        @Child private FromPropertyDescriptorNode fromPropertyDescriptorNode = FromPropertyDescriptorNode.create();

        @Specialization(guards = {"isJSObject(thisObj)"})
        protected DynamicObject getJSObject(DynamicObject thisObj, Object property) {
            Object propertyKey = toPropertyKeyNode.execute(property);
            PropertyDescriptor desc = getOwnPropertyNode.execute(thisObj, propertyKey);
            return fromPropertyDescriptorNode.execute(desc, getContext());
        }

        @Specialization(guards = {"isForeignObject(thisObj)"}, limit = "InteropLibraryLimit")
        protected DynamicObject getForeignObject(Object thisObj, Object property,
                        @CachedLibrary("thisObj") InteropLibrary interop,
                        @Cached("create()") ImportValueNode toJSType) {
            Object propertyKey = toPropertyKeyNode.execute(property);
            if (propertyKey instanceof String) {
                try {
                    String member = (String) propertyKey;
                    if (interop.hasMembers(thisObj)) {
                        if (interop.isMemberExisting(thisObj, member) && interop.isMemberReadable(thisObj, member)) {
                            PropertyDescriptor desc = PropertyDescriptor.createData(
                                            toJSType.executeWithTarget(interop.readMember(thisObj, member)),
                                            !interop.isMemberInternal(thisObj, member),
                                            interop.isMemberWritable(thisObj, member),
                                            interop.isMemberRemovable(thisObj, member));
                            return fromPropertyDescriptorNode.execute(desc, getContext());
                        }
                    }
                    long index = JSRuntime.propertyNameToArrayIndex(member);
                    if (JSRuntime.isArrayIndex(index) && interop.hasArrayElements(thisObj)) {
                        if (interop.isArrayElementExisting(thisObj, index) && interop.isArrayElementReadable(thisObj, index)) {
                            PropertyDescriptor desc = PropertyDescriptor.createData(
                                            toJSType.executeWithTarget(interop.readArrayElement(thisObj, index)),
                                            true,
                                            interop.isArrayElementWritable(thisObj, index),
                                            interop.isArrayElementRemovable(thisObj, index));
                            return fromPropertyDescriptorNode.execute(desc, getContext());
                        }
                    }
                } catch (InteropException iex) {
                }
            }
            return Undefined.instance;
        }

        @Specialization(guards = {"!isJSObject(thisObj)", "!isForeignObject(thisObj)"})
        protected DynamicObject getDefault(Object thisObj, Object property) {
            Object object = toObject(thisObj);
            assert JSDynamicObject.isJSDynamicObject(object);
            return getJSObject((DynamicObject) object, property);
        }

    }

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectGetOwnPropertyDescriptorsNode extends ObjectOperation {

        @Child private FromPropertyDescriptorNode fromPropertyDescriptorNode = FromPropertyDescriptorNode.create();
        @Child private DynamicObjectLibrary putPropDescNode = DynamicObjectLibrary.getFactory().createDispatched(JSConfig.PropertyCacheLimit);

        public ObjectGetOwnPropertyDescriptorsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected abstract DynamicObject executeEvaluated(Object obj);

        @Specialization(guards = {"isJSObject(thisObj)"})
        protected DynamicObject getJSObject(DynamicObject thisObj,
                        @Cached JSGetOwnPropertyNode getOwnPropertyNode,
                        @Cached ListSizeNode listSize,
                        @Cached ListGetNode listGet,
                        @Cached JSClassProfile classProfile) {
            DynamicObject retObj = JSOrdinary.create(getContext(), getRealm());

            List<Object> ownPropertyKeys = JSObject.ownPropertyKeys(thisObj, classProfile);
            int size = listSize.execute(ownPropertyKeys);
            for (int i = 0; i < size; i++) {
                Object key = listGet.execute(ownPropertyKeys, i);
                assert JSRuntime.isPropertyKey(key);
                PropertyDescriptor desc = getOwnPropertyNode.execute(thisObj, key);
                if (desc != null) {
                    DynamicObject propDesc = fromPropertyDescriptorNode.execute(desc, getContext());
                    putPropDescNode.putWithFlags(retObj, key, propDesc, JSAttributes.configurableEnumerableWritable());
                }
            }
            return retObj;
        }

        @Specialization(guards = {"isForeignObject(thisObj)"}, limit = "InteropLibraryLimit")
        protected DynamicObject getForeignObject(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary members,
                        @Cached("create()") ImportValueNode toJSType,
                        @Cached BranchProfile errorBranch) {
            DynamicObject result = JSOrdinary.create(getContext(), getRealm());

            try {
                if (interop.hasMembers(thisObj)) {
                    Object keysObj = interop.getMembers(thisObj);
                    long size = members.getArraySize(keysObj);
                    if (size < 0 || size >= Integer.MAX_VALUE) {
                        errorBranch.enter();
                        throw Errors.createRangeErrorInvalidArrayLength();
                    }
                    for (int i = 0; i < size; i++) {
                        String member = (String) members.readArrayElement(keysObj, i);
                        if (interop.isMemberReadable(thisObj, member)) {
                            PropertyDescriptor desc = PropertyDescriptor.createData(
                                            toJSType.executeWithTarget(interop.readMember(thisObj, member)),
                                            !interop.isMemberInternal(thisObj, member),
                                            interop.isMemberWritable(thisObj, member),
                                            interop.isMemberRemovable(thisObj, member));
                            DynamicObject propDesc = fromPropertyDescriptorNode.execute(desc, getContext());
                            putPropDescNode.putWithFlags(result, member, propDesc, JSAttributes.configurableEnumerableWritable());
                        }
                    }
                }
                if (interop.hasArrayElements(thisObj)) {
                    long size = interop.getArraySize(thisObj);
                    if (size < 0 || size >= Integer.MAX_VALUE) {
                        errorBranch.enter();
                        throw Errors.createRangeErrorInvalidArrayLength();
                    }
                    for (long i = 0; i < size; i++) {
                        if (interop.isArrayElementExisting(thisObj, i) && interop.isArrayElementReadable(thisObj, i)) {
                            PropertyDescriptor desc = PropertyDescriptor.createData(
                                            toJSType.executeWithTarget(interop.readArrayElement(thisObj, i)),
                                            true,
                                            interop.isArrayElementWritable(thisObj, i),
                                            interop.isArrayElementRemovable(thisObj, i));
                            DynamicObject propDesc = fromPropertyDescriptorNode.execute(desc, getContext());
                            putPropDescNode.putWithFlags(result, Boundaries.stringValueOf(i), propDesc, JSAttributes.configurableEnumerableWritable());
                        }
                    }
                }
            } catch (InteropException iex) {
            }

            return result;
        }

        @Specialization(guards = {"!isJSObject(thisObj)", "!isForeignObject(thisObj)"})
        protected DynamicObject getDefault(Object thisObj,
                        @Cached("createRecursive()") ObjectGetOwnPropertyDescriptorsNode recursive) {
            Object object = toObject(thisObj);
            return recursive.executeEvaluated(object);
        }

        ObjectGetOwnPropertyDescriptorsNode createRecursive() {
            return ObjectGetOwnPropertyDescriptorsNodeGen.create(getContext(), getBuiltin(), new JavaScriptNode[0]);
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectGetOwnPropertyNamesOrSymbolsNode extends ObjectOperation {
        protected final boolean symbols;

        public ObjectGetOwnPropertyNamesOrSymbolsNode(JSContext context, JSBuiltin builtin, boolean symbols) {
            super(context, builtin);
            this.symbols = symbols;
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject getJSObject(DynamicObject thisObj,
                        @Cached @Shared("jsclassProfile") JSClassProfile jsclassProfile,
                        @Cached @Shared("listSize") ListSizeNode listSize) {
            List<Object> ownPropertyKeys = jsclassProfile.getJSClass(thisObj).getOwnPropertyKeys(thisObj, !symbols, symbols);
            return JSArray.createLazyArray(getContext(), getRealm(), ownPropertyKeys, listSize.execute(ownPropertyKeys));
        }

        @Specialization(guards = {"!isJSObject(thisObj)", "!isForeignObject(thisObj)"})
        protected DynamicObject getDefault(Object thisObj,
                        @Cached @Shared("jsclassProfile") JSClassProfile jsclassProfile,
                        @Cached @Shared("listSize") ListSizeNode listSize) {
            DynamicObject object = toOrAsJSObject(thisObj);
            return getJSObject(object, jsclassProfile, listSize);
        }

        @Specialization(guards = {"isForeignObject(thisObj)", "symbols"})
        protected DynamicObject getForeignObjectSymbols(@SuppressWarnings("unused") Object thisObj) {
            // TruffleObjects can never have symbols.
            return JSArray.createConstantEmptyArray(getContext(), getRealm());
        }

        @Specialization(guards = {"isForeignObject(thisObj)", "!symbols"}, limit = "InteropLibraryLimit")
        protected DynamicObject getForeignObjectNames(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary members,
                        @Cached BranchProfile errorBranch) {
            Object[] array;
            if (interop.hasMembers(thisObj)) {
                try {
                    Object keysObj = interop.getMembers(thisObj);
                    long size = members.getArraySize(keysObj);
                    if (size < 0 || size >= Integer.MAX_VALUE) {
                        errorBranch.enter();
                        throw Errors.createRangeErrorInvalidArrayLength();
                    }
                    array = new Object[(int) size];
                    for (int i = 0; i < size; i++) {
                        Object key = members.readArrayElement(keysObj, i);
                        assert InteropLibrary.getUncached().isString(key);
                        array[i] = key;
                    }
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    array = ScriptArray.EMPTY_OBJECT_ARRAY;
                }
            } else {
                array = ScriptArray.EMPTY_OBJECT_ARRAY;
            }
            return JSArray.createConstant(getContext(), getRealm(), array);
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
                if (keyDesc != null && keyDesc.getEnumerable()) {
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

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectCreateNode extends ObjectDefineOperation {
        public ObjectCreateNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private CreateObjectNode.CreateObjectWithPrototypeNode objectCreateNode;
        private final BranchProfile needDefineProperties = BranchProfile.create();

        @SuppressWarnings("unused")
        @Specialization(guards = "isJSNull(prototype)")
        protected DynamicObject createPrototypeNull(Object prototype, Object properties) {
            DynamicObject ret = JSOrdinary.createWithNullPrototype(getContext());
            return objectDefineProperties(ret, properties);
        }

        @Specialization(guards = {"!isJSNull(prototype)", "!isJSObject(prototype)"}, limit = "InteropLibraryLimit")
        protected DynamicObject createForeignNullOrInvalidPrototype(Object prototype, Object properties,
                        @CachedLibrary("prototype") InteropLibrary interop,
                        @Cached("createBinaryProfile()") ConditionProfile isNull) {
            assert prototype != null;
            if (isNull.profile(prototype != Undefined.instance && interop.isNull(prototype))) {
                return createPrototypeNull(Null.instance, properties);
            } else {
                throw Errors.createTypeErrorInvalidPrototype(prototype);
            }
        }

        @Specialization(guards = {"isJSObject(prototype)", "isJSObject(properties)"})
        protected DynamicObject createObjectObject(DynamicObject prototype, DynamicObject properties) {
            DynamicObject ret = createObjectWithPrototype(prototype);
            intlDefineProperties(ret, properties);
            return ret;
        }

        @Specialization(guards = {"isJSObject(prototype)", "!isJSNull(properties)"})
        protected DynamicObject createObjectNotNull(DynamicObject prototype, Object properties) {
            DynamicObject ret = createObjectWithPrototype(prototype);
            return objectDefineProperties(ret, properties);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSObject(prototype)", "isJSNull(properties)"})
        protected DynamicObject createObjectNull(DynamicObject prototype, Object properties) {
            throw Errors.createTypeErrorNotObjectCoercible(properties, null, getContext());
        }

        private DynamicObject objectDefineProperties(DynamicObject ret, Object properties) {
            if (properties != Undefined.instance) {
                needDefineProperties.enter();
                intlDefineProperties(ret, toJSObject(properties));
            }
            return ret;
        }

        private DynamicObject createObjectWithPrototype(DynamicObject prototype) {
            if (objectCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                objectCreateNode = insert(CreateObjectNode.createOrdinaryWithPrototype(getContext()));
            }
            return objectCreateNode.execute(prototype);
        }
    }

    public abstract static class ObjectDefinePropertyNode extends ObjectDefineOperation {
        public ObjectDefinePropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject definePropertyJSObjectString(DynamicObject thisObj, String property, Object attributes) {
            PropertyDescriptor desc = toPropertyDescriptor(attributes);
            JSRuntime.definePropertyOrThrow(thisObj, property, desc);
            return thisObj;
        }

        @Specialization(replaces = "definePropertyJSObjectString")
        protected DynamicObject definePropertyGeneric(Object thisObj, Object property, Object attributes) {
            DynamicObject object = asJSObject(thisObj);
            PropertyDescriptor desc = toPropertyDescriptor(attributes);
            Object propertyKey = toPropertyKeyNode.execute(property);
            JSRuntime.definePropertyOrThrow(object, propertyKey, desc);
            return object;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return ObjectDefinePropertyNodeGen.create(getContext(), getBuiltin(), cloneUninitialized(getArguments(), materializedTags));
        }
    }

    public abstract static class ObjectDefinePropertiesNode extends ObjectDefineOperation {

        public ObjectDefinePropertiesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSObject(thisObj)", "isJSObject(properties)"})
        protected DynamicObject definePropertiesObjectObject(DynamicObject thisObj, DynamicObject properties) {
            return intlDefineProperties(thisObj, properties);
        }

        @Specialization(replaces = "definePropertiesObjectObject")
        protected DynamicObject definePropertiesGeneric(Object thisObj, Object properties) {
            DynamicObject object = asJSObject(thisObj);
            return intlDefineProperties(object, toJSObject(properties));
        }
    }

    public abstract static class ObjectIsExtensibleNode extends ObjectOperation {
        public ObjectIsExtensibleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected boolean isExtensibleObject(DynamicObject thisObj,
                        @Cached IsExtensibleNode isExtensibleNode) {
            return isExtensibleNode.executeBoolean(thisObj);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected boolean isExtensibleNonObject(Object thisObj) {
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
        protected DynamicObject preventExtensionsObject(DynamicObject thisObj) {
            JSObject.preventExtensions(thisObj, true);
            return thisObj;
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object preventExtensionsNonObject(Object thisObj) {
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
                JSObject.setIntegrityLevel((DynamicObject) thisObj, freeze, true);
            } else {
                if (getContext().getEcmaScriptVersion() < 6) {
                    throw createTypeErrorCalledOnNonObject(thisObj);
                }
            }
            return thisObj;
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectKeysNode extends ObjectOperation {
        @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
        @Child private InteropLibrary asString;
        private final ConditionProfile hasElements = ConditionProfile.createBinaryProfile();

        public ObjectKeysNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSDynamicObject(thisObj)")
        protected DynamicObject keysDynamicObject(DynamicObject thisObj) {
            UnmodifiableArrayList<? extends Object> keyList = enumerableOwnPropertyNames(toOrAsJSObject(thisObj));
            int len = keyList.size();
            JSRealm realm = getRealm();
            if (hasElements.profile(len > 0)) {
                assert keyList.stream().allMatch(String.class::isInstance);
                return JSArray.createConstant(getContext(), realm, keyList.toArray());
            }
            return JSArray.createEmptyChecked(getContext(), realm, 0);
        }

        @Specialization
        protected DynamicObject keysSymbol(Symbol symbol) {
            return keysDynamicObject(toOrAsJSObject(symbol));
        }

        @Specialization
        protected DynamicObject keysString(JSLazyString string) {
            return keysDynamicObject(toOrAsJSObject(string));
        }

        @Specialization
        protected DynamicObject keysSafeInt(SafeInteger largeInteger) {
            return keysDynamicObject(toOrAsJSObject(largeInteger));
        }

        @Specialization
        protected DynamicObject keysBigInt(BigInt bigInt) {
            return keysDynamicObject(toOrAsJSObject(bigInt));
        }

        @Specialization(guards = "!isTruffleObject(thisObj)")
        protected DynamicObject keysOther(Object thisObj) {
            return keysDynamicObject(toOrAsJSObject(thisObj));
        }

        @Specialization(guards = "isForeignObject(obj)", limit = "InteropLibraryLimit")
        protected DynamicObject keysForeign(Object obj,
                        @CachedLibrary("obj") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary members,
                        @Cached BranchProfile growProfile,
                        @Cached BranchProfile errorBranch) {
            if (interop.hasMembers(obj)) {
                try {
                    Object keysObj = interop.getMembers(obj);
                    long size = members.getArraySize(keysObj);
                    if (size < 0 || size >= Integer.MAX_VALUE) {
                        errorBranch.enter();
                        throw Errors.createRangeErrorInvalidArrayLength();
                    }
                    if (size > 0) {
                        SimpleArrayList<String> keys = SimpleArrayList.create(size);
                        for (int i = 0; i < size; i++) {
                            Object key = members.readArrayElement(keysObj, i);
                            assert InteropLibrary.getUncached().isString(key);
                            keys.add(asStringKey(key), growProfile);
                        }
                        return JSArray.createConstant(getContext(), getRealm(), keys.toArray());
                    }
                    // fall through
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    // fall through
                }
            }
            return JSArray.createEmptyZeroLength(getContext(), getRealm());
        }

        private UnmodifiableArrayList<? extends Object> enumerableOwnPropertyNames(DynamicObject obj) {
            if (enumerableOwnPropertyNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                enumerableOwnPropertyNamesNode = insert(EnumerableOwnPropertyNamesNode.createKeys(getContext()));
            }
            return enumerableOwnPropertyNamesNode.execute(obj);
        }

        private String asStringKey(Object key) throws UnsupportedMessageException {
            assert InteropLibrary.getUncached().isString(key);
            if (key instanceof String) {
                return (String) key;
            } else {
                if (asString == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    asString = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                return asString.asString(key);
            }
        }
    }

    public abstract static class ObjectSetPrototypeOfNode extends ObjectOperation {
        private final BranchProfile errorBranch = BranchProfile.create();
        private final JSClassProfile classProfile = JSClassProfile.create();

        public ObjectSetPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isValidPrototype(newProto)"})
        final Object setPrototypeOfJSObject(JSObject object, JSDynamicObject newProto) {
            if (!JSObject.setPrototype(object, newProto, classProfile)) {
                errorBranch.enter();
                throw Errors.createTypeError("setPrototype failed");
            }
            return object;
        }

        @Specialization(guards = {"!isValidPrototype(newProto)"})
        static Object setPrototypeOfJSObjectToInvalidNewProto(@SuppressWarnings("unused") JSObject object, Object newProto) {
            throw Errors.createTypeErrorInvalidPrototype(newProto);
        }

        @Specialization(guards = {"isNullOrUndefined(object)"})
        final Object setPrototypeOfNonObjectCoercible(Object object, @SuppressWarnings("unused") Object newProto) {
            // ? RequireObjectCoercible(O).
            throw createTypeErrorCalledOnNonObject(object);
        }

        @Specialization(guards = {"!isJSObject(object)", "!isNullOrUndefined(object)", "!isForeignObject(object)"})
        static Object setPrototypeOfValue(Object object, @SuppressWarnings("unused") Object newProto) {
            // If Type(O) is not Object, return O.
            return object;
        }

        @Specialization(guards = {"isForeignObject(object)"})
        final Object setPrototypeOfForeignObject(Object object, @SuppressWarnings("unused") Object newProto) {
            throw createTypeErrorCalledOnNonObject(object);
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

    public abstract static class ObjectAssignNode extends JSBuiltinNode {

        protected static final boolean STRICT = true;

        public ObjectAssignNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object assign(Object target, Object[] sources,
                        @Cached("createToObject(getContext())") JSToObjectNode toObjectNode,
                        @Cached("create(getContext(), STRICT)") WriteElementNode write,
                        @Cached("create(getContext())") AssignPropertiesNode assignProperties) {
            Object to = toObjectNode.execute(target);
            if (sources.length == 0) {
                return to;
            }
            for (Object o : sources) {
                if (!JSRuntime.isNullOrUndefined(o)) {
                    Object from = toObjectNode.execute(o);
                    assignProperties.executeVoid(to, from, write);
                }
            }
            return to;
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class AssignPropertiesNode extends JavaScriptBaseNode {
        protected final JSContext context;

        protected AssignPropertiesNode(JSContext context) {
            this.context = context;
        }

        abstract void executeVoid(Object to, Object from, WriteElementNode write);

        @Specialization(guards = {"isJSObject(from)"})
        protected static void copyPropertiesFromJSObject(Object to, DynamicObject from, WriteElementNode write,
                        @Cached("create(context)") ReadElementNode read,
                        @Cached("create(false)") JSGetOwnPropertyNode getOwnProperty,
                        @Cached ListSizeNode listSize,
                        @Cached ListGetNode listGet,
                        @Cached JSClassProfile classProfile) {
            List<Object> ownPropertyKeys = JSObject.ownPropertyKeys(from, classProfile);
            int size = listSize.execute(ownPropertyKeys);
            for (int i = 0; i < size; i++) {
                Object nextKey = listGet.execute(ownPropertyKeys, i);
                assert JSRuntime.isPropertyKey(nextKey);
                PropertyDescriptor desc = getOwnProperty.execute(from, nextKey);
                if (desc != null && desc.getEnumerable()) {
                    Object propValue = read.executeWithTargetAndIndex(from, nextKey);
                    write.executeWithTargetAndIndexAndValue(to, nextKey, propValue);
                }
            }
        }

        @Specialization(guards = {"!isJSObject(from)"}, limit = "InteropLibraryLimit")
        protected final void doObject(Object to, Object from, WriteElementNode write,
                        @CachedLibrary("from") InteropLibrary fromInterop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary keysInterop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary stringInterop) {
            if (fromInterop.isNull(from)) {
                return;
            }
            try {
                Object members = fromInterop.getMembers(from);
                long length = JSInteropUtil.getArraySize(members, keysInterop, this);
                for (long i = 0; i < length; i++) {
                    Object key = keysInterop.readArrayElement(members, i);
                    String stringKey = key instanceof String ? (String) key : stringInterop.asString(key);
                    Object value = fromInterop.readMember(from, stringKey);
                    write.executeWithTargetAndIndexAndValue(to, stringKey, value);
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                throw Errors.createTypeErrorInteropException(from, e, "CopyDataProperties", this);
            }
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectValuesOrEntriesNode extends ObjectOperation {
        protected final boolean entries;

        @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
        @Child private InteropLibrary asString;

        public ObjectValuesOrEntriesNode(JSContext context, JSBuiltin builtin, boolean entries) {
            super(context, builtin);
            this.entries = entries;
        }

        protected abstract DynamicObject executeEvaluated(Object obj);

        @Specialization(guards = "isJSObject(obj)")
        protected DynamicObject valuesOrEntriesJSObject(DynamicObject obj,
                        @Cached("createBinaryProfile()") ConditionProfile lengthZero) {
            UnmodifiableArrayList<? extends Object> list = enumerableOwnPropertyNames(obj);
            int len = list.size();
            JSRealm realm = getRealm();
            if (lengthZero.profile(len == 0)) {
                return JSArray.createEmptyChecked(getContext(), realm, 0);
            }
            return JSArray.createConstant(getContext(), realm, list.toArray());
        }

        protected UnmodifiableArrayList<? extends Object> enumerableOwnPropertyNames(DynamicObject obj) {
            if (enumerableOwnPropertyNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                enumerableOwnPropertyNamesNode = insert(entries ? EnumerableOwnPropertyNamesNode.createKeysValues(getContext()) : EnumerableOwnPropertyNamesNode.createValues(getContext()));
            }
            return enumerableOwnPropertyNamesNode.execute(obj);
        }

        @Specialization(guards = {"isForeignObject(thisObj)"}, limit = "InteropLibraryLimit")
        protected DynamicObject enumerableOwnPropertyNamesForeign(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary members,
                        @Cached ImportValueNode importValue,
                        @Cached BranchProfile growProfile,
                        @Cached BranchProfile errorBranch) {
            JSRealm realm = getRealm();
            try {
                Object keysObj = interop.getMembers(thisObj);
                long size = members.getArraySize(keysObj);
                if (size < 0 || size >= Integer.MAX_VALUE) {
                    errorBranch.enter();
                    throw Errors.createRangeErrorInvalidArrayLength();
                }
                SimpleArrayList<Object> values = SimpleArrayList.create(size);
                for (int i = 0; i < size; i++) {
                    Object key = members.readArrayElement(keysObj, i);
                    String stringKey = asStringKey(key);
                    Object value = importValue.executeWithTarget(interop.readMember(thisObj, stringKey));
                    if (entries) {
                        value = JSArray.createConstant(getContext(), realm, new Object[]{key, value});
                    }
                    values.add(value, growProfile);
                }
                return JSArray.createConstant(getContext(), realm, values.toArray());
            } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                return JSArray.createEmptyZeroLength(getContext(), realm);
            }
        }

        @Specialization(guards = {"!isJSObject(obj)", "!isForeignObject(obj)"})
        protected DynamicObject valuesOrEntriesGeneric(Object obj,
                        @Cached("createRecursive()") ObjectValuesOrEntriesNode recursive) {
            Object thisObj = toObject(obj);
            return recursive.executeEvaluated(thisObj);
        }

        private String asStringKey(Object key) throws UnsupportedMessageException {
            assert InteropLibrary.getUncached().isString(key);
            if (key instanceof String) {
                return (String) key;
            } else {
                if (asString == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    asString = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                return asString.asString(key);
            }
        }

        ObjectValuesOrEntriesNode createRecursive() {
            return ObjectValuesOrEntriesNodeGen.create(getContext(), getBuiltin(), entries, new JavaScriptNode[0]);
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
            requireObjectCoercibleNode.executeVoid(iterable);
            DynamicObject obj = JSOrdinary.create(getContext(), getRealm());
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
                    Object nextItem = iteratorValueNode.execute(next);
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

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectBindPropertiesNode extends ObjectOperation {
        @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;
        private final JSClassProfile sourceProfile = JSClassProfile.create();
        private final JSClassProfile targetProfile = JSClassProfile.create();

        public ObjectBindPropertiesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "!isJSObject(target)")
        protected DynamicObject bindPropertiesInvalidTarget(Object target, @SuppressWarnings("unused") Object source) {
            throw Errors.createTypeErrorNotAnObject(target, this);
        }

        @Specialization(guards = {"isJSObject(target)", "isJSDynamicObject(source)"})
        protected DynamicObject bindPropertiesDynamicObject(DynamicObject target, DynamicObject source) {
            DynamicObject sourceObject = toJSObject(source);
            boolean extensible = JSObject.isExtensible(target, targetProfile);
            JSClass sourceClass = sourceProfile.getJSClass(sourceObject);
            UnmodifiableArrayList<? extends Object> keys = enumerableOwnPropertyNames(sourceObject);
            int length = keys.size();
            for (int i = 0; i < length; i++) {
                Object key = keys.get(i);
                if (!JSObject.hasOwnProperty(target, key, targetProfile)) {
                    if (!extensible) {
                        throw Errors.createTypeErrorNotExtensible(target, key);
                    }
                    PropertyDescriptor desc = JSObject.getOwnProperty(sourceObject, key, sourceProfile);
                    if (desc.isAccessorDescriptor()) {
                        JSObject.defineOwnProperty(target, key, desc);
                    } else {
                        JSObjectUtil.defineProxyProperty(target, key, new BoundProperty(source, key, sourceClass), desc.getFlags());
                    }
                }
            }
            return target;
        }

        @Specialization(guards = "isJSObject(target)")
        protected DynamicObject bindProperties(DynamicObject target, Symbol source) {
            return bindPropertiesDynamicObject(target, toJSObject(source));
        }

        @Specialization(guards = "isJSObject(target)")
        protected DynamicObject bindProperties(DynamicObject target, JSLazyString source) {
            return bindPropertiesDynamicObject(target, toJSObject(source));
        }

        @Specialization(guards = "isJSObject(target)")
        protected DynamicObject bindProperties(DynamicObject target, SafeInteger source) {
            return bindPropertiesDynamicObject(target, toJSObject(source));
        }

        @Specialization(guards = "isJSObject(target)")
        protected DynamicObject bindProperties(DynamicObject target, BigInt source) {
            return bindPropertiesDynamicObject(target, toJSObject(source));
        }

        @Specialization(guards = {"isJSObject(target)", "!isTruffleObject(source)"})
        protected DynamicObject bindProperties(DynamicObject target, Object source) {
            return bindPropertiesDynamicObject(target, toJSObject(source));
        }

        @Specialization(guards = {"isJSObject(target)", "isForeignObject(source)"}, limit = "InteropLibraryLimit")
        protected DynamicObject bindProperties(DynamicObject target, Object source,
                        @CachedLibrary("source") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary members) {
            if (interop.hasMembers(source)) {
                try {
                    boolean extensible = JSObject.isExtensible(target, targetProfile);
                    boolean hostObject = getRealm().getEnv().isHostObject(source);
                    Object keysObj = interop.getMembers(source);
                    long size = members.getArraySize(keysObj);
                    for (int i = 0; i < size; i++) {
                        Object key = members.readArrayElement(keysObj, i);
                        String stringKey;
                        if (key instanceof String) {
                            stringKey = (String) key;
                        } else {
                            stringKey = InteropLibrary.getUncached().asString(key);
                        }
                        if (!JSObject.hasOwnProperty(target, key, targetProfile)) {
                            if (!extensible) {
                                throw Errors.createTypeErrorNotExtensible(target, key);
                            }
                            JSObjectUtil.defineProxyProperty(target, key, new ForeignBoundProperty(source, stringKey), JSAttributes.getDefault());
                        }
                        if (hostObject) {
                            // Special handling of bean properties: when there is "getProp",
                            // "setProp" or "isProp" but not "prop" in the source then define also
                            // "prop" in the target (unless the target has "prop" already).
                            String beanProperty;
                            if (stringKey.length() > 3 && (stringKey.charAt(0) == 's' || stringKey.charAt(0) == 'g') && stringKey.charAt(1) == 'e' && stringKey.charAt(2) == 't' &&
                                            Boundaries.characterIsUpperCase(stringKey.charAt(3))) {
                                beanProperty = beanProperty(stringKey, 3);
                            } else if (stringKey.length() > 2 && stringKey.charAt(0) == 'i' && stringKey.charAt(1) == 's' && Boundaries.characterIsUpperCase(stringKey.charAt(2))) {
                                beanProperty = beanProperty(stringKey, 2);
                            } else {
                                continue;
                            }
                            if (!JSObject.hasOwnProperty(target, beanProperty, targetProfile) && !interop.isMemberExisting(source, beanProperty)) {
                                String getKey = beanAccessor("get", beanProperty);
                                String getter;
                                if (interop.isMemberExisting(source, getKey)) {
                                    getter = getKey;
                                } else {
                                    String isKey = beanAccessor("is", beanProperty);
                                    if (interop.isMemberExisting(source, isKey)) {
                                        getter = isKey;
                                    } else {
                                        getter = null;
                                    }
                                }
                                String setKey = beanAccessor("set", beanProperty);
                                String setter = interop.isMemberExisting(source, setKey) ? setKey : null;
                                JSObjectUtil.defineProxyProperty(target, beanProperty, new ForeignBoundBeanProperty(source, getter, setter), JSAttributes.getDefault());
                            }
                        }
                    }
                } catch (UnsupportedMessageException | InvalidArrayIndexException ex) {
                }
            } else {
                throw Errors.createTypeErrorNotAnObject(target, this);
            }
            return target;
        }

        private UnmodifiableArrayList<? extends Object> enumerableOwnPropertyNames(DynamicObject obj) {
            if (enumerableOwnPropertyNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                enumerableOwnPropertyNamesNode = insert(EnumerableOwnPropertyNamesNode.createKeys(getContext()));
            }
            return enumerableOwnPropertyNamesNode.execute(obj);
        }

        @TruffleBoundary
        private static String beanProperty(String accessor, int prefixLength) {
            char c = accessor.charAt(prefixLength);
            return Character.toLowerCase(c) + accessor.substring(prefixLength + 1);
        }

        @TruffleBoundary
        private static String beanAccessor(String prefix, String beanProperty) {
            return prefix + Character.toUpperCase(beanProperty.charAt(0)) + beanProperty.substring(1);
        }

        static class BoundProperty implements PropertyProxy {
            private final DynamicObject source;
            private final Object key;
            private final JSClass sourceClass;

            BoundProperty(DynamicObject source, Object key, JSClass sourceClass) {
                this.source = source;
                this.key = key;
                this.sourceClass = sourceClass;
            }

            @Override
            public Object get(DynamicObject store) {
                return sourceClass.get(source, key);
            }

            @Override
            public boolean set(DynamicObject store, Object value) {
                return sourceClass.set(source, key, value, source, false, null);
            }

        }

        static class ForeignBoundProperty implements PropertyProxy {
            private final Object source;
            private final String key;

            ForeignBoundProperty(Object source, String key) {
                this.source = source;
                this.key = key;
            }

            @Override
            public Object get(DynamicObject store) {
                InteropLibrary library = InteropLibrary.getFactory().getUncached(source);
                if (library.isMemberReadable(source, key)) {
                    try {
                        return JSRuntime.importValue(library.readMember(source, key));
                    } catch (UnsupportedMessageException | UnknownIdentifierException ex) {
                    }
                }
                return Undefined.instance;
            }

            @Override
            public boolean set(DynamicObject store, Object value) {
                InteropLibrary library = InteropLibrary.getFactory().getUncached(source);
                if (library.isMemberWritable(source, key)) {
                    try {
                        library.writeMember(source, key, JSRuntime.exportValue(value));
                        return true;
                    } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException ex) {
                    }
                }
                return false;
            }
        }

        static class ForeignBoundBeanProperty implements PropertyProxy {
            private final Object source;
            private final String getKey;
            private final String setKey;

            ForeignBoundBeanProperty(Object source, String getKey, String setKey) {
                assert getKey != null || setKey != null;
                this.source = source;
                this.getKey = getKey;
                this.setKey = setKey;
            }

            @Override
            public Object get(DynamicObject store) {
                if (getKey != null) {
                    InteropLibrary library = InteropLibrary.getFactory().getUncached(source);
                    if (library.isMemberInvocable(source, getKey)) {
                        try {
                            return JSRuntime.importValue(library.invokeMember(source, getKey));
                        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException | ArityException ex) {
                        }
                    }
                }
                return Undefined.instance;
            }

            @Override
            public boolean set(DynamicObject store, Object value) {
                if (setKey != null) {
                    InteropLibrary library = InteropLibrary.getFactory().getUncached(source);
                    if (library.isMemberInvocable(source, setKey)) {
                        try {
                            library.invokeMember(source, setKey, JSRuntime.exportValue(value));
                            return true;
                        } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException | ArityException ex) {
                        }
                    }
                }
                return false;
            }
        }

    }

    public abstract static class ObjectHasOwnNode extends ObjectOperation {
        @Child JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        @Child JSHasPropertyNode hasOwnPropertyNode = JSHasPropertyNode.create(true);

        public ObjectHasOwnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean hasOwn(Object o, Object p) {
            Object obj = toObject(o);
            Object key = toPropertyKeyNode.execute(p);
            return hasOwnPropertyNode.executeBoolean(obj, key);
        }

    }

}
