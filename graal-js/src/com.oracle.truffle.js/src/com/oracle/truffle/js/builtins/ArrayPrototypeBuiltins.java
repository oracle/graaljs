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
package com.oracle.truffle.js.builtins;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetLength;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetLength;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.DeleteAndSetLengthNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.FlattenIntoArrayNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayAtNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayConcatNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayCopyWithinNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayEveryNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFillNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFilterNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFindIndexNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFindNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFlatMapNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFlatNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayForEachNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayGroupByNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayGroupByToMapNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayIncludesNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayIndexOfNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayIteratorNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayJoinNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayMapNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayPopNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayPushNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayReduceNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayReverseNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayShiftNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySliceNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySomeNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySortNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArraySpliceNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToStringNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayUnshiftNodeGen;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.CallbackNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResult;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResultNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.array.ArrayCreateNode;
import com.oracle.truffle.js.nodes.array.ArrayLengthNode.ArrayLengthWriteNode;
import com.oracle.truffle.js.nodes.array.JSArrayDeleteRangeNode;
import com.oracle.truffle.js.nodes.array.JSArrayFirstElementIndexNode;
import com.oracle.truffle.js.nodes.array.JSArrayLastElementIndexNode;
import com.oracle.truffle.js.nodes.array.JSArrayNextElementIndexNode;
import com.oracle.truffle.js.nodes.array.JSArrayPreviousElementIndexNode;
import com.oracle.truffle.js.nodes.array.JSArrayToDenseObjectArrayNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.array.JSSetLengthNode;
import com.oracle.truffle.js.nodes.array.TestArrayNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantByteArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantIntArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSlowArray;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.StringBuilderProfile;

/**
 * Contains builtins for {@linkplain JSArray}.prototype.
 */
public final class ArrayPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ArrayPrototypeBuiltins.ArrayPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new ArrayPrototypeBuiltins();

    protected ArrayPrototypeBuiltins() {
        super(JSArray.PROTOTYPE_NAME, ArrayPrototype.class);
    }

    public enum ArrayPrototype implements BuiltinEnum<ArrayPrototype> {
        push(1),
        pop(0),
        slice(2),
        shift(0),
        unshift(1),
        toString(0),
        concat(1),
        indexOf(1),
        lastIndexOf(1),
        join(1),
        toLocaleString(0),
        splice(2),
        every(1),
        filter(1),
        forEach(1),
        some(1),
        map(1),
        sort(1),
        reduce(1),
        reduceRight(1),
        reverse(0),

        // ES6 / ES2015
        find(1),
        findIndex(1),
        fill(1),
        copyWithin(2),
        keys(0),
        values(0),
        entries(0),

        // ES2016
        includes(1),

        // ES2019
        flat(0),
        flatMap(1),

        // ES2022
        at(1),
        groupBy(1),
        groupByToMap(1);

        private final int length;

        ArrayPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (EnumSet.of(find, findIndex, fill, copyWithin, keys, values, entries).contains(this)) {
                return JSConfig.ECMAScript2015;
            } else if (this == includes) {
                return JSConfig.ECMAScript2016;
            } else if (EnumSet.of(flat, flatMap).contains(this)) {
                return JSConfig.ECMAScript2019;
            } else if (this == at) {
                return JSConfig.ECMAScript2022;
            } else if (EnumSet.of(groupBy, groupByToMap).contains(this)) {
                return JSConfig.StagingECMAScriptVersion;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ArrayPrototype builtinEnum) {
        switch (builtinEnum) {
            case push:
                return JSArrayPushNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case pop:
                return JSArrayPopNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case slice:
                return JSArraySliceNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case shift:
                return JSArrayShiftNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case unshift:
                return JSArrayUnshiftNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case toString:
                return JSArrayToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case concat:
                return JSArrayConcatNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case indexOf:
                return JSArrayIndexOfNodeGen.create(context, builtin, false, true, args().withThis().varArgs().createArgumentNodes(context));
            case lastIndexOf:
                return JSArrayIndexOfNodeGen.create(context, builtin, false, false, args().withThis().varArgs().createArgumentNodes(context));
            case join:
                return JSArrayJoinNodeGen.create(context, builtin, false, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
                return JSArrayToLocaleStringNodeGen.create(context, builtin, false, args().withThis().createArgumentNodes(context));
            case splice:
                return JSArraySpliceNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case every:
                return JSArrayEveryNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case filter:
                return JSArrayFilterNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case forEach:
                return JSArrayForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case some:
                return JSArraySomeNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case map:
                return JSArrayMapNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case sort:
                return JSArraySortNodeGen.create(context, builtin, false, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reduce:
                return JSArrayReduceNodeGen.create(context, builtin, false, true, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case reduceRight:
                return JSArrayReduceNodeGen.create(context, builtin, false, false, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case reverse:
                return JSArrayReverseNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));

            case find:
                return JSArrayFindNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findIndex:
                return JSArrayFindIndexNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case fill:
                return JSArrayFillNodeGen.create(context, builtin, false, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case copyWithin:
                return JSArrayCopyWithinNodeGen.create(context, builtin, false, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case keys:
                return JSArrayIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_KEY, args().withThis().createArgumentNodes(context));
            case values:
                return JSArrayIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_VALUE, args().withThis().createArgumentNodes(context));
            case entries:
                return JSArrayIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE, args().withThis().createArgumentNodes(context));

            case includes:
                return JSArrayIncludesNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));

            case flatMap:
                return JSArrayFlatMapNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case flat:
                return JSArrayFlatNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));

            case at:
                return JSArrayAtNodeGen.create(context, builtin, false, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case groupBy:
                return JSArrayGroupByNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case groupByToMap:
                return JSArrayGroupByToMapNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class BasicArrayOperation extends JSBuiltinNode {

        protected final boolean isTypedArrayImplementation; // for reusing array code on TypedArrays
        @Child private JSToObjectNode toObjectNode;
        @Child private JSGetLengthNode getLengthNode;
        @Child private ArraySpeciesConstructorNode arraySpeciesCreateNode;
        @Child private IsCallableNode isCallableNode;
        protected final BranchProfile errorBranch = BranchProfile.create();

        public BasicArrayOperation(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin);
            this.isTypedArrayImplementation = isTypedArrayImplementation;
        }

        public BasicArrayOperation(JSContext context, JSBuiltin builtin) {
            this(context, builtin, false);
        }

        protected final Object toObject(Object target) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return toObjectNode.execute(target);
        }

        protected long getLength(Object thisObject) {
            if (isTypedArrayImplementation) {
                // %TypedArray%.prototype.* don't access the "length" property
                return typedArrayGetLength((JSTypedArrayObject) thisObject);
            } else {
                if (getLengthNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getLengthNode = insert(JSGetLengthNode.create(getContext()));
                }
                return getLengthNode.executeLong(thisObject);
            }
        }

        protected final Object toObjectOrValidateTypedArray(Object thisObj) {
            return isTypedArrayImplementation ? validateTypedArray(thisObj) : toObject(thisObj);
        }

        protected final boolean isCallable(Object callback) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(callback);
        }

        protected final Object checkCallbackIsFunction(Object callback) {
            if (!isCallable(callback)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAFunction(callback, this);
            }
            return callback;
        }

        protected final ArraySpeciesConstructorNode getArraySpeciesConstructorNode() {
            if (arraySpeciesCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arraySpeciesCreateNode = insert(ArraySpeciesConstructorNode.create(getContext(), isTypedArrayImplementation));
            }
            return arraySpeciesCreateNode;
        }

        protected final void checkHasDetachedBuffer(DynamicObject view) {
            if (JSArrayBufferView.hasDetachedBuffer(view, getContext())) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }

        /**
         * ES2016, 22.2.3.5.1 ValidateTypedArray(O).
         */
        protected final JSTypedArrayObject validateTypedArray(Object obj) {
            if (!JSArrayBufferView.isJSArrayBufferView(obj)) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferViewExpected();
            }
            JSTypedArrayObject typedArrayObject = (JSTypedArrayObject) obj;
            if (JSArrayBufferView.hasDetachedBuffer(typedArrayObject, getContext())) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
            return typedArrayObject;
        }

        protected void reportLoopCount(long count) {
            reportLoopCount(this, count);
        }

        public static void reportLoopCount(Node node, long count) {
            if (count > 0) {
                LoopNode.reportLoopCount(node, count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count);
            }
        }
    }

    protected static class ArraySpeciesConstructorNode extends JavaScriptBaseNode {
        private final boolean isTypedArrayImplementation; // for reusing array code on TypedArrays
        @Child private JSFunctionCallNode constructorCall;
        @Child private PropertyGetNode getConstructorNode;
        @Child private PropertyGetNode getSpeciesNode;
        @Child private JSIsArrayNode isArrayNode;
        @Child private IsConstructorNode isConstructorNode = IsConstructorNode.create();
        @Child private ArrayCreateNode arrayCreateNode;
        private final BranchProfile errorBranch = BranchProfile.create();
        private final BranchProfile arraySpeciesIsArray = BranchProfile.create();
        private final BranchProfile arraySpeciesGetSymbol = BranchProfile.create();
        private final BranchProfile differentRealm = BranchProfile.create();
        private final BranchProfile defaultConstructorBranch = BranchProfile.create();
        private final ConditionProfile arraySpeciesEmpty = ConditionProfile.createBinaryProfile();
        private final BranchProfile notAJSObjectBranch = BranchProfile.create();
        private final JSContext context;

        protected ArraySpeciesConstructorNode(JSContext context, boolean isTypedArrayImplementation) {
            this.context = context;
            this.isTypedArrayImplementation = isTypedArrayImplementation;
            this.isArrayNode = JSIsArrayNode.createIsArray();
            this.constructorCall = JSFunctionCallNode.createNew();
        }

        protected static ArraySpeciesConstructorNode create(JSContext context, boolean isTypedArrayImplementation) {
            return new ArraySpeciesConstructorNode(context, isTypedArrayImplementation);
        }

        protected final Object createEmptyContainer(Object thisObj, long size) {
            if (isTypedArrayImplementation) {
                return typedArraySpeciesCreate(JSRuntime.expectJSObject(thisObj, notAJSObjectBranch), JSRuntime.longToIntOrDouble(size));
            } else {
                return arraySpeciesCreate(thisObj, size);
            }
        }

        protected final DynamicObject typedArraySpeciesCreate(DynamicObject thisObj, Object... args) {
            DynamicObject constr = speciesConstructor(thisObj, getDefaultConstructor(thisObj));
            return typedArrayCreate(constr, args);
        }

        /**
         * 22.2.4.6 TypedArrayCreate().
         */
        public final DynamicObject typedArrayCreate(DynamicObject constr, Object... args) {
            Object newTypedArray = construct(constr, args);
            if (!JSArrayBufferView.isJSArrayBufferView(newTypedArray)) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferViewExpected();
            }
            if (JSArrayBufferView.hasDetachedBuffer((DynamicObject) newTypedArray, context)) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
            }
            if (args.length == 1 && JSRuntime.isNumber(args[0])) {
                if (JSArrayBufferView.typedArrayGetLength((DynamicObject) newTypedArray) < JSRuntime.doubleValue((Number) args[0])) {
                    errorBranch.enter();
                    throw Errors.createTypeError("invalid TypedArray created");
                }
            }
            return (DynamicObject) newTypedArray;
        }

        /**
         * ES6, 9.4.2.3 ArraySpeciesCreate(originalArray, length).
         */
        protected final Object arraySpeciesCreate(Object originalArray, long length) {
            Object ctor = Undefined.instance;
            if (isArray(originalArray)) {
                arraySpeciesIsArray.enter();
                ctor = getConstructorProperty(originalArray);
                if (JSDynamicObject.isJSDynamicObject(ctor)) {
                    DynamicObject ctorObj = (DynamicObject) ctor;
                    if (JSFunction.isJSFunction(ctorObj) && JSFunction.isConstructor(ctorObj)) {
                        JSRealm thisRealm = getRealm();
                        JSRealm ctorRealm = JSFunction.getRealm(ctorObj);
                        if (thisRealm != ctorRealm) {
                            differentRealm.enter();
                            if (ctorRealm.getArrayConstructor() == ctor) {
                                /*
                                 * If originalArray was created using the standard built-in Array
                                 * constructor for a realm that is not the realm of the running
                                 * execution context, then a new Array is created using the realm of
                                 * the running execution context.
                                 */
                                return arrayCreate(length);
                            }
                        }
                    }
                    if (ctor != Undefined.instance) {
                        arraySpeciesGetSymbol.enter();
                        ctor = getSpeciesProperty(ctor);
                        ctor = ctor == Null.instance ? Undefined.instance : ctor;
                    }
                }
            }
            if (arraySpeciesEmpty.profile(ctor == Undefined.instance)) {
                return arrayCreate(length);
            }
            if (!isConstructorNode.executeBoolean(ctor)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAConstructor(ctor, context);
            }
            return construct((DynamicObject) ctor, JSRuntime.longToIntOrDouble(length));
        }

        protected final boolean isArray(Object thisObj) {
            return isArrayNode.execute(thisObj);
        }

        private Object arrayCreate(long length) {
            if (arrayCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayCreateNode = insert(ArrayCreateNode.create(context));
            }
            return arrayCreateNode.execute(length);
        }

        protected Object construct(DynamicObject constructor, Object... userArgs) {
            Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, userArgs.length);
            System.arraycopy(userArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, userArgs.length);
            return constructorCall.executeCall(args);
        }

        protected final DynamicObject getDefaultConstructor(DynamicObject thisObj) {
            assert JSArrayBufferView.isJSArrayBufferView(thisObj);
            TypedArray arrayType = JSArrayBufferView.typedArrayGetArrayType(thisObj);
            return getRealm().getArrayBufferViewConstructor(arrayType.getFactory());
        }

        /**
         * Implement 7.3.20 SpeciesConstructor.
         */
        protected final DynamicObject speciesConstructor(DynamicObject thisObj, DynamicObject defaultConstructor) {
            Object c = getConstructorProperty(thisObj);
            if (c == Undefined.instance) {
                defaultConstructorBranch.enter();
                return defaultConstructor;
            }
            if (!JSDynamicObject.isJSDynamicObject(c)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAnObject(c);
            }
            Object speciesConstructor = getSpeciesProperty(c);
            if (speciesConstructor == Undefined.instance || speciesConstructor == Null.instance) {
                defaultConstructorBranch.enter();
                return defaultConstructor;
            }
            if (!isConstructorNode.executeBoolean(speciesConstructor)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAConstructor(speciesConstructor, context);
            }
            return (DynamicObject) speciesConstructor;
        }

        private Object getConstructorProperty(Object obj) {
            if (getConstructorNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getConstructorNode = insert(PropertyGetNode.create(JSObject.CONSTRUCTOR, false, context));
            }
            return getConstructorNode.getValue(obj);
        }

        private Object getSpeciesProperty(Object obj) {
            if (getSpeciesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSpeciesNode = insert(PropertyGetNode.create(Symbol.SYMBOL_SPECIES, false, context));
            }
            return getSpeciesNode.getValue(obj);
        }
    }

    public abstract static class JSArrayOperation extends BasicArrayOperation {

        public JSArrayOperation(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        public JSArrayOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        protected static final boolean THROW_ERROR = true;

        @Child private JSSetLengthNode setLengthNode;
        @Child private WriteElementNode writeNode;
        @Child private WriteElementNode writeOwnNode;
        @Child private ReadElementNode readNode;
        @Child private JSHasPropertyNode hasPropertyNode;
        @Child private JSArrayNextElementIndexNode nextElementIndexNode;
        @Child private JSArrayPreviousElementIndexNode previousElementIndexNode;

        protected void setLength(Object thisObject, int length) {
            setLengthIntl(thisObject, length);
        }

        protected void setLength(Object thisObject, long length) {
            setLengthIntl(thisObject, JSRuntime.longToIntOrDouble(length));
        }

        protected void setLength(Object thisObject, double length) {
            setLengthIntl(thisObject, length);
        }

        private void setLengthIntl(Object thisObject, Object length) {
            assert !(length instanceof Long);
            if (setLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setLengthNode = insert(JSSetLengthNode.create(getContext(), THROW_ERROR));
            }
            setLengthNode.execute(thisObject, length);
        }

        private ReadElementNode getOrCreateReadNode() {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(ReadElementNode.create(getContext()));
            }
            return readNode;
        }

        protected Object read(Object target, int index) {
            return getOrCreateReadNode().executeWithTargetAndIndex(target, index);
        }

        protected Object read(Object target, long index) {
            ReadElementNode read = getOrCreateReadNode();
            return read.executeWithTargetAndIndex(target, index);
        }

        private WriteElementNode getOrCreateWriteNode() {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNode = insert(WriteElementNode.create(getContext(), THROW_ERROR));
            }
            return writeNode;
        }

        protected void write(Object target, int index, Object value) {
            getOrCreateWriteNode().executeWithTargetAndIndexAndValue(target, index, value);
        }

        protected void write(Object target, long index, Object value) {
            WriteElementNode write = getOrCreateWriteNode();
            write.executeWithTargetAndIndexAndValue(target, index, value);
        }

        // represent the 7.3.6 CreateDataPropertyOrThrow(O, P, V)
        private WriteElementNode getOrCreateWriteOwnNode() {
            if (writeOwnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeOwnNode = insert(WriteElementNode.create(getContext(), THROW_ERROR, true));
            }
            return writeOwnNode;
        }

        protected void writeOwn(Object target, int index, Object value) {
            getOrCreateWriteOwnNode().executeWithTargetAndIndexAndValue(target, index, value);
        }

        protected void writeOwn(Object target, long index, Object value) {
            WriteElementNode write = getOrCreateWriteOwnNode();
            write.executeWithTargetAndIndexAndValue(target, index, value);
        }

        private JSHasPropertyNode getOrCreateHasPropertyNode() {
            if (hasPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasPropertyNode = insert(JSHasPropertyNode.create());
            }
            return hasPropertyNode;
        }

        protected boolean hasProperty(Object target, long propertyIdx) {
            return getOrCreateHasPropertyNode().executeBoolean(target, propertyIdx);
        }

        protected boolean hasProperty(Object target, Object propertyName) {
            return getOrCreateHasPropertyNode().executeBoolean(target, propertyName);
        }

        protected long nextElementIndex(Object target, long currentIndex, long length) {
            if (nextElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nextElementIndexNode = insert(JSArrayNextElementIndexNode.create(getContext()));
            }
            return nextElementIndexNode.executeLong(target, currentIndex, length);
        }

        protected long previousElementIndex(Object target, long currentIndex) {
            if (previousElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                previousElementIndexNode = insert(JSArrayPreviousElementIndexNode.create(getContext()));
            }
            return previousElementIndexNode.executeLong(target, currentIndex);
        }

        protected static final void throwLengthError() {
            throw Errors.createTypeError("length too big");
        }
    }

    public abstract static class JSArrayOperationWithToInt extends JSArrayOperation {
        public JSArrayOperationWithToInt(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        public JSArrayOperationWithToInt(JSContext context, JSBuiltin builtin) {
            this(context, builtin, false);
        }

        @Child private JSToIntegerAsLongNode toIntegerAsLongNode;

        protected long toIntegerAsLong(Object target) {
            if (toIntegerAsLongNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerAsLongNode = insert(JSToIntegerAsLongNode.create());
            }
            return toIntegerAsLongNode.executeLong(target);
        }
    }

    public abstract static class JSArrayPushNode extends JSArrayOperation {
        public JSArrayPushNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSArray(thisObject)", "args.length == 0"})
        protected Object pushArrayNone(DynamicObject thisObject, @SuppressWarnings("unused") Object[] args) {
            long len = getLength(thisObject);
            setLength(thisObject, len); // could have side effect / throw
            if (len >= Integer.MAX_VALUE) {
                return (double) len;
            } else {
                return (int) len;
            }

        }

        @Specialization(guards = {"isJSArray(thisObject)", "args.length == 1"}, rewriteOn = SlowPathException.class)
        protected int pushArraySingle(DynamicObject thisObject, Object[] args) throws SlowPathException {
            long len = getLength(thisObject);
            if (len >= Integer.MAX_VALUE) {
                throw JSNodeUtil.slowPathException();
            }
            int iLen = (int) len;
            write(thisObject, iLen, args[0]);
            int newLength = iLen + 1;
            setLength(thisObject, newLength);
            return newLength;
        }

        @Specialization(guards = {"isJSArray(thisObject)", "args.length == 1"})
        protected double pushArraySingleLong(DynamicObject thisObject, Object[] args) {
            long len = getLength(thisObject);
            checkLength(args, len);
            write(thisObject, len, args[0]);
            long newLength = len + 1;
            setLength(thisObject, newLength);
            return newLength;
        }

        @Specialization(guards = {"isJSArray(thisObject)", "args.length >= 2"}, rewriteOn = SlowPathException.class)
        protected int pushArrayAll(DynamicObject thisObject, Object[] args) throws SlowPathException {
            long len = getLength(thisObject);
            if (len + args.length >= Integer.MAX_VALUE) {
                throw JSNodeUtil.slowPathException();
            }
            int ilen = (int) len;
            for (int i = 0; i < args.length; i++) {
                write(thisObject, ilen + i, args[i]);
            }
            setLength(thisObject, ilen + args.length);
            return ilen + args.length;
        }

        @Specialization(guards = {"isJSArray(thisObject)", "args.length >= 2"})
        protected double pushArrayAllLong(DynamicObject thisObject, Object[] args) {
            long len = getLength(thisObject);
            checkLength(args, len);
            for (int i = 0; i < args.length; i++) {
                write(thisObject, len + i, args[i]);
            }
            setLength(thisObject, len + args.length);
            return (double) len + args.length;
        }

        @Specialization(guards = "!isJSArray(thisObject)")
        protected double pushProperty(Object thisObject, Object[] args) {
            Object thisObj = toObject(thisObject);
            long len = getLength(thisObj);
            checkLength(args, len);
            for (int i = 0; i < args.length; i++) {
                write(thisObj, len + i, args[i]);
            }
            long newLength = len + args.length;
            setLength(thisObj, newLength);
            return newLength;
        }

        private void checkLength(Object[] args, long len) {
            if (len + args.length > JSRuntime.MAX_SAFE_INTEGER) {
                errorBranch.enter();
                throwLengthError();
            }
        }
    }

    public abstract static class JSArrayPopNode extends JSArrayOperation {
        public JSArrayPopNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object popGeneric(Object thisObj,
                        @Cached("create(getContext())") DeleteAndSetLengthNode deleteAndSetLength,
                        @Cached("createBinaryProfile()") ConditionProfile lengthIsZero) {
            final Object thisObject = toObject(thisObj);
            final long length = getLength(thisObject);
            if (lengthIsZero.profile(length > 0)) {
                long newLength = length - 1;
                Object result = read(thisObject, newLength);
                deleteAndSetLength.executeVoid(thisObject, newLength);
                return result;
            } else {
                assert length == 0;
                setLength(thisObject, 0);
                return Undefined.instance;
            }
        }
    }

    @ImportStatic({JSRuntime.class, JSConfig.class})
    protected abstract static class DeleteAndSetLengthNode extends JavaScriptBaseNode {
        protected static final boolean THROW_ERROR = true;  // DeletePropertyOrThrow

        protected final JSContext context;

        protected DeleteAndSetLengthNode(JSContext context) {
            this.context = context;
        }

        public static DeleteAndSetLengthNode create(JSContext context) {
            return DeleteAndSetLengthNodeGen.create(context);
        }

        public abstract void executeVoid(Object target, long newLength);

        protected final WritePropertyNode createWritePropertyNode() {
            return WritePropertyNode.create(null, JSArray.LENGTH, null, context, THROW_ERROR);
        }

        protected static boolean isArray(DynamicObject object) {
            // currently, must be fast array
            return JSArray.isJSFastArray(object);
        }

        @Specialization(guards = {"isArray(object)", "longIsRepresentableAsInt(longLength)"})
        protected static void setArrayLength(DynamicObject object, long longLength,
                        @Cached("createArrayLengthWriteNode()") ArrayLengthWriteNode arrayLengthWriteNode) {
            arrayLengthWriteNode.executeVoid(object, (int) longLength);
        }

        protected static final ArrayLengthWriteNode createArrayLengthWriteNode() {
            return ArrayLengthWriteNode.createSetOrDelete(THROW_ERROR);
        }

        @Specialization(guards = {"isJSObject(object)", "longIsRepresentableAsInt(longLength)"})
        protected static void setIntLength(DynamicObject object, long longLength,
                        @Cached("create(THROW_ERROR, context)") DeletePropertyNode deletePropertyNode,
                        @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty) {
            int intLength = (int) longLength;
            deletePropertyNode.executeEvaluated(object, intLength);
            setLengthProperty.executeIntWithValue(object, intLength);
        }

        @Specialization(guards = {"isJSObject(object)"}, replaces = "setIntLength")
        protected static void setLength(DynamicObject object, long longLength,
                        @Cached("create(THROW_ERROR, context)") DeletePropertyNode deletePropertyNode,
                        @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty,
                        @Cached("createBinaryProfile()") ConditionProfile indexInIntRangeCondition) {
            Object boxedLength = JSRuntime.boxIndex(longLength, indexInIntRangeCondition);
            deletePropertyNode.executeEvaluated(object, boxedLength);
            setLengthProperty.executeWithValue(object, boxedLength);
        }

        @Specialization(guards = {"!isJSObject(object)"})
        protected static void foreignArray(Object object, long newLength,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary arrays) {
            try {
                arrays.removeArrayElement(object, newLength);
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw Errors.createTypeErrorInteropException(object, e, "removeArrayElement", null);
            }
        }
    }

    public abstract static class JSArraySliceNode extends ArrayForEachIndexCallOperation {

        public JSArraySliceNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        private final ConditionProfile sizeIsZero = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile2 = ConditionProfile.createBinaryProfile();

        @Specialization
        protected Object sliceGeneric(Object thisObj, Object begin, Object end,
                        @Cached("create()") JSToIntegerAsLongNode toIntegerAsLong) {
            Object thisArrayObj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(thisArrayObj);
            long startPos = begin != Undefined.instance ? JSRuntime.getOffset(toIntegerAsLong.executeLong(begin), len, offsetProfile1) : 0;

            long endPos;
            if (end == Undefined.instance) {
                endPos = len;
            } else {
                endPos = JSRuntime.getOffset(toIntegerAsLong.executeLong(end), len, offsetProfile2);
            }

            long size = startPos <= endPos ? endPos - startPos : 0;
            Object resultArray = getArraySpeciesConstructorNode().createEmptyContainer(thisArrayObj, size);
            if (sizeIsZero.profile(size > 0)) {
                if (isTypedArrayImplementation) {
                    checkHasDetachedBuffer((DynamicObject) thisObj);
                }
                forEachIndexCall(thisArrayObj, null, startPos, startPos, endPos, resultArray);
            }
            if (!isTypedArrayImplementation) {
                setLength(resultArray, size);
            }
            return resultArray;
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {

                @Child private WriteElementNode writeOwnNode = WriteElementNode.create(getContext(), true, true);

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    long startIndex = (long) callbackResult;
                    writeOwnNode.executeWithTargetAndIndexAndValue(currentResult, index - startIndex, value);
                    return MaybeResult.continueResult(currentResult);
                }
            };
        }

        @Override
        protected CallbackNode makeCallbackNode() {
            return null;
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class JSArrayShiftNode extends JSArrayOperation {

        public JSArrayShiftNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static boolean isSparseArray(DynamicObject thisObj) {
            return arrayGetArrayType(thisObj) instanceof SparseArray;
        }

        protected static boolean isArrayWithoutHolesAndNotSealed(DynamicObject thisObj, IsArrayNode isArrayNode, TestArrayNode hasHolesNode, TestArrayNode isSealedNode) {
            boolean isArray = isArrayNode.execute(thisObj);
            return isArray && !hasHolesNode.executeBoolean(thisObj) && !isSealedNode.executeBoolean(thisObj);
        }

        @Specialization(guards = {"isArrayWithoutHolesAndNotSealed(thisObj, isArrayNode, hasHolesNode, isSealedNode)"}, limit = "1")
        protected Object shiftWithoutHoles(DynamicObject thisObj,
                        @Shared("isArray") @Cached("createIsArray()") @SuppressWarnings("unused") IsArrayNode isArrayNode,
                        @Shared("hasHoles") @Cached("createHasHoles()") @SuppressWarnings("unused") TestArrayNode hasHolesNode,
                        @Shared("isSealed") @Cached("createIsSealed()") @SuppressWarnings("unused") TestArrayNode isSealedNode,
                        @Cached("createClassProfile()") ValueProfile arrayTypeProfile,
                        @Shared("lengthIsZero") @Cached("createBinaryProfile()") ConditionProfile lengthIsZero,
                        @Cached("createBinaryProfile()") ConditionProfile lengthLargerOne) {
            long len = getLength(thisObj);

            if (lengthIsZero.profile(len == 0)) {
                return Undefined.instance;
            } else {
                Object firstElement = read(thisObj, 0);
                if (lengthLargerOne.profile(len > 1)) {
                    ScriptArray array = arrayTypeProfile.profile(arrayGetArrayType(thisObj));
                    arraySetArrayType(thisObj, array.shiftRange(thisObj, 1));
                }
                setLength(thisObj, len - 1);
                return firstElement;
            }
        }

        protected static boolean isArrayWithHolesOrSealed(DynamicObject thisObj, IsArrayNode isArrayNode, TestArrayNode hasHolesNode, TestArrayNode isSealedNode) {
            boolean isArray = isArrayNode.execute(thisObj);
            return isArray && (hasHolesNode.executeBoolean(thisObj) || isSealedNode.executeBoolean(thisObj)) && !isSparseArray(thisObj);
        }

        @Specialization(guards = {"isArrayWithHolesOrSealed(thisObj, isArrayNode, hasHolesNode, isSealedNode)"}, limit = "1")
        protected Object shiftWithHoles(DynamicObject thisObj,
                        @Shared("isArray") @Cached("createIsArray()") @SuppressWarnings("unused") IsArrayNode isArrayNode,
                        @Shared("hasHoles") @Cached("createHasHoles()") @SuppressWarnings("unused") TestArrayNode hasHolesNode,
                        @Shared("isSealed") @Cached("createIsSealed()") @SuppressWarnings("unused") TestArrayNode isSealedNode,
                        @Shared("deleteProperty") @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deletePropertyNode,
                        @Shared("lengthIsZero") @Cached("createBinaryProfile()") ConditionProfile lengthIsZero) {
            long len = getLength(thisObj);
            if (lengthIsZero.profile(len > 0)) {
                Object firstElement = read(thisObj, 0);

                for (long i = 0; i < len - 1; i++) {
                    if (hasProperty(thisObj, i + 1)) {
                        write(thisObj, i, read(thisObj, i + 1));
                    } else {
                        deletePropertyNode.executeEvaluated(thisObj, i);
                    }
                }
                deletePropertyNode.executeEvaluated(thisObj, len - 1);
                setLength(thisObj, len - 1);
                reportLoopCount(len - 1);
                return firstElement;
            } else {
                return Undefined.instance;
            }
        }

        @Specialization(guards = {"isArrayNode.execute(thisObj)", "isSparseArray(thisObj)"}, limit = "1")
        protected Object shiftSparse(DynamicObject thisObj,
                        @Shared("isArray") @Cached("createIsArray()") @SuppressWarnings("unused") IsArrayNode isArrayNode,
                        @Shared("deleteProperty") @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deletePropertyNode,
                        @Shared("lengthIsZero") @Cached("createBinaryProfile()") ConditionProfile lengthIsZero,
                        @Cached("create(getContext())") JSArrayFirstElementIndexNode firstElementIndexNode,
                        @Cached("create(getContext())") JSArrayLastElementIndexNode lastElementIndexNode) {
            long len = getLength(thisObj);
            if (lengthIsZero.profile(len > 0)) {
                Object firstElement = read(thisObj, 0);
                long count = 0;
                for (long i = firstElementIndexNode.executeLong(thisObj, len); i <= lastElementIndexNode.executeLong(thisObj, len); i = nextElementIndex(thisObj, i, len)) {
                    if (i > 0) {
                        write(thisObj, i - 1, read(thisObj, i));
                    }
                    if (!hasProperty(thisObj, i + 1)) {
                        deletePropertyNode.executeEvaluated(thisObj, i);
                    }
                    count++;
                }
                setLength(thisObj, len - 1);
                reportLoopCount(count);
                return firstElement;
            } else {
                return Undefined.instance;
            }
        }

        @Specialization(guards = {"!isJSArray(thisObj)", "!isForeignObject(thisObj)"})
        protected Object shiftGeneric(Object thisObj,
                        @Shared("deleteProperty") @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deleteNode,
                        @Shared("lengthIsZero") @Cached("createBinaryProfile()") ConditionProfile lengthIsZero) {
            Object thisJSObj = toObject(thisObj);
            long len = getLength(thisJSObj);

            if (lengthIsZero.profile(len == 0)) {
                setLength(thisJSObj, 0);
                return Undefined.instance;
            }

            Object firstObj = read(thisJSObj, 0);
            for (long i = 1; i < len; i++) {
                if (hasProperty(thisJSObj, i)) {
                    write(thisJSObj, i - 1, read(thisObj, i));
                } else {
                    deleteNode.executeEvaluated(thisJSObj, i - 1);
                }
            }
            deleteNode.executeEvaluated(thisJSObj, len - 1);
            setLength(thisJSObj, len - 1);
            reportLoopCount(len);
            return firstObj;
        }

        @Specialization(guards = {"isForeignObject(thisObj)"})
        protected Object shiftForeign(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary arrays,
                        @Shared("lengthIsZero") @Cached("createBinaryProfile()") ConditionProfile lengthIsZero) {
            long len = JSInteropUtil.getArraySize(thisObj, arrays, this);
            if (lengthIsZero.profile(len == 0)) {
                return Undefined.instance;
            }

            try {
                Object firstObj = arrays.readArrayElement(thisObj, 0);
                for (long i = 1; i < len; i++) {
                    Object val = arrays.readArrayElement(thisObj, i);
                    arrays.writeArrayElement(thisObj, i - 1, val);
                }
                arrays.removeArrayElement(thisObj, len - 1);
                reportLoopCount(len);
                return firstObj;
            } catch (UnsupportedMessageException | InvalidArrayIndexException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "shift", this);
            }
        }
    }

    public abstract static class JSArrayUnshiftNode extends JSArrayOperation {
        public JSArrayUnshiftNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child protected IsArrayNode isArrayNode = IsArrayNode.createIsArray();
        @Child protected TestArrayNode hasHolesNode = TestArrayNode.createHasHoles();

        protected boolean isFastPath(Object thisObj) {
            boolean isArray = isArrayNode.execute(thisObj);
            return isArray && !hasHolesNode.executeBoolean((DynamicObject) thisObj);
        }

        private long unshiftHoleless(DynamicObject thisObj, Object[] args) {
            long len = getLength(thisObj);
            if (getContext().getEcmaScriptVersion() <= 5 || args.length > 0) {
                for (long l = len - 1; l >= 0; l--) {
                    write(thisObj, l + args.length, read(thisObj, l));
                }
                for (int i = 0; i < args.length; i++) {
                    write(thisObj, i, args[i]);
                }
                reportLoopCount(len + args.length);
            }
            long newLen = len + args.length;
            setLength(thisObj, newLen);
            return newLen;
        }

        @Specialization(guards = "isFastPath(thisObj)", rewriteOn = UnexpectedResultException.class)
        protected int unshiftInt(DynamicObject thisObj, Object[] args) throws UnexpectedResultException {
            long newLen = unshiftHoleless(thisObj, args);
            if (JSRuntime.longIsRepresentableAsInt(newLen)) {
                return (int) newLen;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException((double) newLen);
            }
        }

        @Specialization(guards = "isFastPath(thisObj)", replaces = "unshiftInt")
        protected double unshiftDouble(DynamicObject thisObj, Object[] args) {
            return unshiftHoleless(thisObj, args);
        }

        @Specialization(guards = "!isFastPath(thisObjParam)")
        protected double unshiftHoles(Object thisObjParam, Object[] args,
                        @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deletePropertyNode,
                        @Cached("create(getContext())") JSArrayLastElementIndexNode lastElementIndexNode,
                        @Cached("create(getContext())") JSArrayFirstElementIndexNode firstElementIndexNode) {
            Object thisObj = toObject(thisObjParam);
            long len = getLength(thisObj);

            if (getContext().getEcmaScriptVersion() <= 5 || args.length > 0) {
                if (args.length + len > JSRuntime.MAX_SAFE_INTEGER_LONG) {
                    errorBranch.enter();
                    throwLengthError();
                }
                long lastIdx = lastElementIndexNode.executeLong(thisObj, len);
                long firstIdx = firstElementIndexNode.executeLong(thisObj, len);
                long count = 0;
                for (long i = lastIdx; i >= firstIdx; i = previousElementIndex(thisObj, i)) {
                    count++;
                    if (hasProperty(thisObj, i)) {
                        write(thisObj, i + args.length, read(thisObj, i));
                        if (args.length > 0 && i >= args.length && !hasProperty(thisObj, i - args.length)) {
                            // delete the source if it is not identical to the sink
                            // and no other element will copy here later in the loop
                            deletePropertyNode.executeEvaluated(thisObj, i);
                        }
                    }
                }
                for (int i = 0; i < args.length; i++) {
                    write(thisObj, i, args[i]);
                }
                reportLoopCount(count + args.length);
            }
            long newLen = len + args.length;
            setLength(thisObj, newLen);
            return newLen;
        }
    }

    public abstract static class JSArrayToStringNode extends BasicArrayOperation {
        @Child private PropertyNode joinPropertyNode;
        @Child private PropertyNode toStringPropertyNode;
        @Child private JSFunctionCallNode callJoinNode;
        @Child private JSFunctionCallNode callToStringNode;
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;
        @Child private InteropLibrary interopLibrary;
        @Child private ImportValueNode importValueNode;

        private final ConditionProfile isJSObjectProfile = ConditionProfile.createBinaryProfile();

        private static final String JOIN = "join";

        public JSArrayToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.joinPropertyNode = PropertyNode.createProperty(context, null, JOIN);
        }

        private Object getJoinProperty(Object target) {
            return joinPropertyNode.executeWithTarget(target);
        }

        private Object getToStringProperty(Object target) {
            if (toStringPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringPropertyNode = insert(PropertyNode.createProperty(getContext(), null, JSRuntime.TO_STRING));
            }
            return toStringPropertyNode.executeWithTarget(target);
        }

        private Object callJoin(Object target, Object function) {
            if (callJoinNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callJoinNode = insert(JSFunctionCallNode.createCall());
            }
            return callJoinNode.executeCall(JSArguments.createZeroArg(target, function));
        }

        private Object callToString(Object target, Object function) {
            if (callToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callToStringNode = insert(JSFunctionCallNode.createCall());
            }
            return callToStringNode.executeCall(JSArguments.createZeroArg(target, function));
        }

        private DynamicObject getForeignObjectPrototype(Object truffleObject) {
            assert JSRuntime.isForeignObject(truffleObject);
            if (foreignObjectPrototypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
            }
            return foreignObjectPrototypeNode.executeDynamicObject(truffleObject);
        }

        private InteropLibrary getInterop() {
            if (interopLibrary == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interopLibrary = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            return interopLibrary;
        }

        private Object importValue(Object value) {
            if (importValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                importValueNode = insert(ImportValueNode.create());
            }
            return importValueNode.executeWithTarget(value);
        }

        private Object toStringForeign(Object arrayObj) {
            InteropLibrary interop = getInterop();
            if (interop.isMemberInvocable(arrayObj, JOIN)) {
                Object result;
                try {
                    try {
                        result = interop.invokeMember(arrayObj, JOIN);
                    } catch (AbstractTruffleException e) {
                        if (InteropLibrary.getUncached(e).getExceptionType(e) == ExceptionType.RUNTIME_ERROR) {
                            result = null;
                        } else {
                            throw e;
                        }
                    }
                } catch (InteropException e) {
                    result = null;
                }
                if (result != null) {
                    return importValue(result);
                }
            }

            Object join = getJoinProperty(getForeignObjectPrototype(arrayObj));
            if (isCallable(join)) {
                return callJoin(arrayObj, join);
            } else {
                Object toString = getToStringProperty(getRealm().getObjectPrototype());
                return callToString(arrayObj, toString);
            }
        }

        @Specialization
        protected Object toString(Object thisObj) {
            Object arrayObj = toObject(thisObj);
            if (isJSObjectProfile.profile(JSDynamicObject.isJSDynamicObject(arrayObj))) {
                Object join = getJoinProperty(arrayObj);
                if (isCallable(join)) {
                    return callJoin(arrayObj, join);
                } else {
                    return JSObject.defaultToString((DynamicObject) arrayObj);
                }
            } else {
                return toStringForeign(arrayObj);
            }
        }
    }

    public abstract static class JSArrayConcatNode extends JSArrayOperation {
        public JSArrayConcatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToBooleanNode toBooleanNode;
        @Child private JSToStringNode toStringNode;
        @Child private JSArrayFirstElementIndexNode firstElementIndexNode;
        @Child private JSArrayLastElementIndexNode lastElementIndexNode;
        @Child private PropertyGetNode getSpreadableNode;
        @Child private JSIsArrayNode isArrayNode;

        private final ConditionProfile isFirstSpreadable = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasFirstElements = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isSecondSpreadable = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasSecondElements = ConditionProfile.createBinaryProfile();
        private final ConditionProfile lengthErrorProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasMultipleArgs = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasOneArg = ConditionProfile.createBinaryProfile();
        private final ConditionProfile optimizationsObservable = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasFirstOneElement = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasSecondOneElement = ConditionProfile.createBinaryProfile();

        protected boolean toBoolean(Object target) {
            if (toBooleanNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBooleanNode = insert(JSToBooleanNode.create());
            }
            return toBooleanNode.executeBoolean(target);
        }

        protected String toString(Object target) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(target);
        }

        @Specialization
        protected DynamicObject concat(Object thisObj, Object[] args) {
            Object thisJSObj = toObject(thisObj);
            DynamicObject retObj = (DynamicObject) getArraySpeciesConstructorNode().createEmptyContainer(thisJSObj, 0);

            long n = concatElementIntl(retObj, thisJSObj, 0, isFirstSpreadable, hasFirstElements, hasFirstOneElement);
            long resultLen = concatIntl(retObj, n, args);

            // the last set element could be non-existent
            setLength(retObj, resultLen);
            return retObj;
        }

        private long concatIntl(DynamicObject retObj, long initialLength, Object[] args) {
            long n = initialLength;
            if (hasOneArg.profile(args.length == 1)) {
                n = concatElementIntl(retObj, args[0], n, isSecondSpreadable, hasSecondElements, hasSecondOneElement);
            } else if (hasMultipleArgs.profile(args.length > 1)) {
                for (int i = 0; i < args.length; i++) {
                    n = concatElementIntl(retObj, args[i], n, isSecondSpreadable, hasSecondElements, hasSecondOneElement);
                }
            }
            return n;
        }

        private long concatElementIntl(DynamicObject retObj, Object el, final long n, final ConditionProfile isSpreadable, final ConditionProfile hasElements,
                        final ConditionProfile hasOneElement) {
            if (isSpreadable.profile(isConcatSpreadable(el))) {
                long len2 = getLength(el);
                if (hasElements.profile(len2 > 0)) {
                    return concatSpreadable(retObj, n, el, len2, hasOneElement);
                }
            } else {
                if (lengthErrorProfile.profile(n > JSRuntime.MAX_SAFE_INTEGER)) {
                    errorBranch.enter();
                    throwLengthError();
                }
                writeOwn(retObj, n, el);
                return n + 1;
            }
            return n;
        }

        private long concatSpreadable(DynamicObject retObj, long n, Object elObj, long len2, final ConditionProfile hasOneElement) {
            if (lengthErrorProfile.profile((n + len2) > JSRuntime.MAX_SAFE_INTEGER)) {
                errorBranch.enter();
                throwLengthError();
            }
            if (optimizationsObservable.profile(JSProxy.isJSProxy(elObj) || !JSDynamicObject.isJSDynamicObject(elObj))) {
                // strictly to the standard implementation; traps could expose optimizations!
                for (long k = 0; k < len2; k++) {
                    if (hasProperty(elObj, k)) {
                        writeOwn(retObj, n + k, read(elObj, k));
                    }
                }
            } else if (hasOneElement.profile(len2 == 1)) {
                // fastpath for 1-element entries
                if (hasProperty(elObj, 0)) {
                    writeOwn(retObj, n, read(elObj, 0));
                }
            } else {
                long k = firstElementIndex((DynamicObject) elObj, len2);
                long lastI = lastElementIndex((DynamicObject) elObj, len2);
                for (; k <= lastI; k = nextElementIndex(elObj, k, len2)) {
                    writeOwn(retObj, n + k, read(elObj, k));
                }
            }
            return n + len2;
        }

        // ES2015, 22.1.3.1.1
        private boolean isConcatSpreadable(Object el) {
            if (el == Undefined.instance || el == Null.instance) {
                return false;
            }
            if (JSDynamicObject.isJSDynamicObject(el)) {
                DynamicObject obj = (DynamicObject) el;
                Object spreadable = getSpreadableProperty(obj);
                if (spreadable != Undefined.instance) {
                    return toBoolean(spreadable);
                }
            }
            return isArray(el);
        }

        private boolean isArray(Object object) {
            if (isArrayNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isArrayNode = insert(JSIsArrayNode.createIsArrayLike());
            }
            return isArrayNode.execute(object);
        }

        private Object getSpreadableProperty(Object obj) {
            if (getSpreadableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSpreadableNode = insert(PropertyGetNode.create(Symbol.SYMBOL_IS_CONCAT_SPREADABLE, false, getContext()));
            }
            return getSpreadableNode.getValue(obj);
        }

        private long firstElementIndex(DynamicObject target, long length) {
            if (firstElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                firstElementIndexNode = insert(JSArrayFirstElementIndexNode.create(getContext()));
            }
            return firstElementIndexNode.executeLong(target, length);
        }

        private long lastElementIndex(DynamicObject target, long length) {
            if (lastElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lastElementIndexNode = insert(JSArrayLastElementIndexNode.create(getContext()));
            }
            return lastElementIndexNode.executeLong(target, length);
        }
    }

    public abstract static class JSArrayIndexOfNode extends ArrayForEachIndexCallOperation {
        private final boolean isForward;

        @Child private JSToIntegerAsLongNode toIntegerNode;
        private final BranchProfile arrayWithContentBranch = BranchProfile.create();
        private final BranchProfile fromConversionBranch = BranchProfile.create();

        public JSArrayIndexOfNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation, boolean isForward) {
            super(context, builtin, isTypedArrayImplementation);
            this.isForward = isForward;
        }

        @Specialization
        protected Object indexOf(Object thisObj, Object[] args) {
            Object thisJSObject = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(thisJSObject);
            if (len == 0) {
                return -1;
            }
            arrayWithContentBranch.enter();
            Object searchElement = JSRuntime.getArgOrUndefined(args, 0);
            Object fromIndex = JSRuntime.getArgOrUndefined(args, 1);

            long fromIndexValue = isForward() ? calcFromIndexForward(args, len, fromIndex) : calcFromIndexBackward(args, len, fromIndex);
            if (fromIndexValue < 0) {
                return -1;
            }
            return forEachIndexCall(thisJSObject, Undefined.instance, searchElement, fromIndexValue, len, -1);
        }

        // for indexOf()
        private long calcFromIndexForward(Object[] args, long len, Object fromIndex) {
            if (args.length <= 1) {
                return 0;
            } else {
                fromConversionBranch.enter();
                long fromIndexValue = toInteger(fromIndex);
                if (fromIndexValue > len) {
                    return -1;
                }
                if (fromIndexValue < 0) {
                    fromIndexValue += len;
                    fromIndexValue = (fromIndexValue < 0) ? 0 : fromIndexValue;
                }
                return fromIndexValue;
            }
        }

        // for lastIndexOf()
        private long calcFromIndexBackward(Object[] args, long len, Object fromIndex) {
            if (args.length <= 1) {
                return len - 1;
            } else {
                fromConversionBranch.enter();
                long fromIndexInt = toInteger(fromIndex);
                if (fromIndexInt >= 0) {
                    return Math.min(fromIndexInt, len - 1);
                } else {
                    return fromIndexInt + len;
                }
            }
        }

        private long toInteger(Object operand) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerAsLongNode.create());
            }
            return toIntegerNode.executeLong(operand);
        }

        @Override
        protected boolean isForward() {
            return isForward;
        }

        @Override
        protected boolean shouldCheckHasProperty() {
            return true;
        }

        @Override
        protected final MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Child private JSIdenticalNode doIdenticalNode = JSIdenticalNode.createStrictEqualityComparison();
                private final ConditionProfile indexInIntRangeCondition = ConditionProfile.createBinaryProfile();

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    return doIdenticalNode.executeBoolean(value, callbackResult) ? MaybeResult.returnResult(JSRuntime.boxIndex(index, indexInIntRangeCondition))
                                    : MaybeResult.continueResult(currentResult);
                }
            };
        }

        @Override
        protected final CallbackNode makeCallbackNode() {
            return null;
        }
    }

    public abstract static class JSArrayJoinNode extends JSArrayOperation {
        @Child private JSToStringNode separatorToStringNode;
        @Child private JSToStringNode elementToStringNode;
        private final ConditionProfile separatorNotEmpty = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isZero = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isOne = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isTwo = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isSparse = ConditionProfile.createBinaryProfile();
        private final BranchProfile growProfile = BranchProfile.create();
        private final BranchProfile stackGrowProfile = BranchProfile.create();
        private final StringBuilderProfile stringBuilderProfile;

        public JSArrayJoinNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.elementToStringNode = JSToStringNode.create();
            this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
        }

        @Specialization
        protected String join(Object thisObj, Object joinStr) {
            final Object thisJSObject = toObjectOrValidateTypedArray(thisObj);
            final long length = getLength(thisJSObject);
            final String joinSeparator = joinStr == Undefined.instance ? "," : getSeparatorToString().executeString(joinStr);

            JSRealm realm = getRealm();
            if (!realm.joinStackPush(thisObj, stackGrowProfile)) {
                // join is in progress on thisObj already => break the cycle
                return "";
            }
            try {
                if (isZero.profile(length == 0)) {
                    return "";
                } else if (isOne.profile(length == 1)) {
                    return joinOne(thisJSObject);
                } else {
                    final boolean appendSep = separatorNotEmpty.profile(joinSeparator.length() > 0);
                    if (isTwo.profile(length == 2)) {
                        return joinTwo(thisJSObject, joinSeparator, appendSep);
                    } else if (isSparse.profile(JSArray.isJSArray(thisJSObject) && arrayGetArrayType((DynamicObject) thisJSObject) instanceof SparseArray)) {
                        return joinSparse(thisJSObject, length, joinSeparator, appendSep);
                    } else {
                        return joinLoop(thisJSObject, length, joinSeparator, appendSep);
                    }
                }
            } finally {
                realm.joinStackPop();
            }
        }

        private JSToStringNode getSeparatorToString() {
            if (separatorToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                separatorToStringNode = insert(JSToStringNode.create());
            }
            return separatorToStringNode;
        }

        private String joinOne(Object thisObject) {
            Object value = read(thisObject, 0);
            return toStringOrEmpty(value);
        }

        private String joinTwo(Object thisObject, final String joinSeparator, final boolean appendSep) {
            String first = toStringOrEmpty(read(thisObject, 0));
            String second = toStringOrEmpty(read(thisObject, 1));

            long resultLength = first.length() + (appendSep ? joinSeparator.length() : 0L) + second.length();
            if (resultLength > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }

            final StringBuilder res = new StringBuilder((int) resultLength);
            Boundaries.builderAppend(res, first);
            if (appendSep) {
                Boundaries.builderAppend(res, joinSeparator);
            }
            Boundaries.builderAppend(res, second);
            return Boundaries.builderToString(res);
        }

        private String joinLoop(Object thisJSObject, final long length, final String joinSeparator, final boolean appendSep) {
            final StringBuilder res = stringBuilderProfile.newStringBuilder();
            long i = 0;
            while (i < length) {
                if (appendSep && i != 0) {
                    stringBuilderProfile.append(res, joinSeparator);
                }
                Object value = read(thisJSObject, i);
                String str = toStringOrEmpty(value);
                stringBuilderProfile.append(res, str);

                if (appendSep) {
                    i++;
                } else {
                    i = nextElementIndex(thisJSObject, i, length);
                }
            }
            return stringBuilderProfile.toString(res);
        }

        private String toStringOrEmpty(Object value) {
            if (isValidEntry(value)) {
                return elementToStringNode.executeString(value);
            } else {
                return "";
            }
        }

        private static boolean isValidEntry(Object value) {
            return value != Undefined.instance && value != Null.instance;
        }

        private String joinSparse(Object thisObject, long length, String joinSeparator, final boolean appendSep) {
            SimpleArrayList<Object> converted = SimpleArrayList.create(length);
            long calculatedLength = 0;
            long i = 0;
            while (i < length) {
                Object value = read(thisObject, i);
                if (isValidEntry(value)) {
                    String string = elementToStringNode.executeString(value);
                    int stringLength = string.length();
                    if (stringLength > 0) {
                        calculatedLength += stringLength;
                        converted.add(i, growProfile);
                        converted.add(string, growProfile);
                    }
                }
                i = nextElementIndex(thisObject, i, length);
            }
            if (appendSep) {
                calculatedLength += (length - 1) * joinSeparator.length();
            }
            if (calculatedLength > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            assert calculatedLength <= Integer.MAX_VALUE;
            final StringBuilder res = stringBuilderProfile.newStringBuilder((int) calculatedLength);
            long lastIndex = 0;
            for (int j = 0; j < converted.size(); j += 2) {
                long index = (long) converted.get(j);
                String value = (String) converted.get(j + 1);
                if (appendSep) {
                    for (long k = lastIndex; k < index; k++) {
                        stringBuilderProfile.append(res, joinSeparator);
                    }
                }
                stringBuilderProfile.append(res, value);
                lastIndex = index;
            }
            if (appendSep) {
                for (long k = lastIndex; k < length - 1; k++) {
                    stringBuilderProfile.append(res, joinSeparator);
                }
            }
            assert res.length() == calculatedLength;
            return stringBuilderProfile.toString(res);
        }
    }

    public abstract static class JSArrayToLocaleStringNode extends JSArrayOperation {

        private final StringBuilderProfile stringBuilderProfile;
        private final BranchProfile stackGrowProfile = BranchProfile.create();
        @Child private PropertyGetNode getToLocaleStringNode;
        @Child private JSFunctionCallNode callToLocaleStringNode;

        public JSArrayToLocaleStringNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
        }

        @Specialization
        protected String toLocaleString(VirtualFrame frame, Object thisObj,
                        @Cached("create()") JSToStringNode toStringNode) {
            Object arrayObj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(arrayObj);
            if (len == 0) {
                return "";
            }
            JSRealm realm = getRealm();
            if (!realm.joinStackPush(thisObj, stackGrowProfile)) {
                // join is in progress on thisObj already => break the cycle
                return "";
            }
            try {
                Object[] userArguments = JSArguments.extractUserArguments(frame.getArguments());
                long k = 0;
                StringBuilder r = stringBuilderProfile.newStringBuilder();
                while (k < len) {
                    if (k > 0) {
                        stringBuilderProfile.append(r, ',');
                    }
                    Object nextElement = read(arrayObj, k);
                    if (nextElement != Null.instance && nextElement != Undefined.instance) {
                        Object result = callToLocaleString(nextElement, userArguments);
                        String resultString = toStringNode.executeString(result);
                        stringBuilderProfile.append(r, resultString);
                    }
                    k++;
                }
                return stringBuilderProfile.toString(r);
            } finally {
                realm.joinStackPop();
            }
        }

        private Object callToLocaleString(Object nextElement, Object[] userArguments) {
            if (getToLocaleStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getToLocaleStringNode = insert(PropertyGetNode.create("toLocaleString", false, getContext()));
                callToLocaleStringNode = insert(JSFunctionCallNode.createCall());
            }

            Object toLocaleString = getToLocaleStringNode.getValue(nextElement);
            return callToLocaleStringNode.executeCall(JSArguments.create(nextElement, toLocaleString, userArguments));
        }
    }

    public abstract static class JSArraySpliceNode extends JSArrayOperationWithToInt {

        @Child private DeletePropertyNode deletePropertyNode; // DeletePropertyOrThrow
        private final BranchProfile branchA = BranchProfile.create();
        private final BranchProfile branchB = BranchProfile.create();
        private final BranchProfile branchDelete = BranchProfile.create();
        private final BranchProfile objectBranch = BranchProfile.create();
        private final ConditionProfile argsLength0Profile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile argsLength1Profile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile needMoveDeleteBranch = BranchProfile.create();
        private final BranchProfile needInsertBranch = BranchProfile.create();
        @Child private InteropLibrary arrayInterop;

        public JSArraySpliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.deletePropertyNode = DeletePropertyNode.create(THROW_ERROR, context);
        }

        @Specialization
        protected DynamicObject splice(Object thisArg, Object[] args,
                        @Cached("create(getContext())") SpliceJSArrayNode spliceJSArray) {
            Object thisObj = toObject(thisArg);
            long len = getLength(thisObj);

            long actualStart = JSRuntime.getOffset(toIntegerAsLong(JSRuntime.getArgOrUndefined(args, 0)), len, offsetProfile);
            long insertCount;
            long actualDeleteCount;
            if (argsLength0Profile.profile(args.length == 0)) {
                insertCount = 0;
                actualDeleteCount = 0;
            } else if (argsLength1Profile.profile(args.length == 1)) {
                insertCount = 0;
                actualDeleteCount = len - actualStart;
            } else {
                assert args.length >= 2;
                insertCount = args.length - 2;
                long deleteCount = toIntegerAsLong(JSRuntime.getArgOrUndefined(args, 1));
                actualDeleteCount = Math.min(Math.max(deleteCount, 0), len - actualStart);
            }

            if (len + insertCount - actualDeleteCount > JSRuntime.MAX_SAFE_INTEGER_LONG) {
                errorBranch.enter();
                throwLengthError();
            }

            DynamicObject aObj = (DynamicObject) getArraySpeciesConstructorNode().createEmptyContainer(thisObj, actualDeleteCount);

            if (actualDeleteCount > 0) {
                // copy deleted elements into result array
                branchDelete.enter();
                spliceRead(thisObj, actualStart, actualDeleteCount, aObj, len);
            }
            setLength(aObj, actualDeleteCount);

            long itemCount = insertCount;
            boolean isJSArray = JSArray.isJSArray(thisObj);
            if (isJSArray) {
                DynamicObject dynObj = (DynamicObject) thisObj;
                ScriptArray arrayType = arrayGetArrayType(dynObj);
                spliceJSArray.execute(dynObj, len, actualStart, actualDeleteCount, itemCount, arrayType, this);
            } else if (JSDynamicObject.isJSDynamicObject(thisObj)) {
                objectBranch.enter();
                spliceJSObject(thisObj, len, actualStart, actualDeleteCount, itemCount);
            } else {
                spliceForeignArray(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }

            if (itemCount > 0) {
                needInsertBranch.enter();
                spliceInsert(thisObj, actualStart, args);
            }

            long newLength = len - actualDeleteCount + itemCount;
            setLength(thisObj, newLength);
            reportLoopCount(len);
            return aObj;
        }

        abstract static class SpliceJSArrayNode extends JavaScriptBaseNode {
            final JSContext context;

            SpliceJSArrayNode(JSContext context) {
                this.context = context;
            }

            abstract void execute(DynamicObject array, long len, long actualStart, long actualDeleteCount, long itemCount, ScriptArray arrayType, JSArraySpliceNode parent);

            @Specialization(guards = {"cachedArrayType.isInstance(arrayType)"}, limit = "5")
            static void doCached(DynamicObject array, long len, long actualStart, long actualDeleteCount, long itemCount, ScriptArray arrayType, JSArraySpliceNode parent,
                            @Cached("arrayType") ScriptArray cachedArrayType,
                            @Cached GetPrototypeNode getPrototypeNode,
                            @Cached ConditionProfile arrayElementwise) {
                if (arrayElementwise.profile(parent.mustUseElementwise(array, cachedArrayType.cast(arrayType), getPrototypeNode))) {
                    parent.spliceJSArrayElementwise(array, len, actualStart, actualDeleteCount, itemCount);
                } else {
                    parent.spliceJSArrayBlockwise(array, actualStart, actualDeleteCount, itemCount, cachedArrayType.cast(arrayType));
                }
            }

            @Specialization(replaces = "doCached")
            static void doUncached(DynamicObject array, long len, long actualStart, long actualDeleteCount, long itemCount, ScriptArray arrayType, JSArraySpliceNode parent,
                            @Cached GetPrototypeNode getPrototypeNode,
                            @Cached ConditionProfile arrayElementwise) {
                if (arrayElementwise.profile(parent.mustUseElementwise(array, arrayType, getPrototypeNode))) {
                    parent.spliceJSArrayElementwise(array, len, actualStart, actualDeleteCount, itemCount);
                } else {
                    parent.spliceJSArrayBlockwise(array, actualStart, actualDeleteCount, itemCount, arrayType);
                }
            }
        }

        final boolean mustUseElementwise(DynamicObject obj, ScriptArray array, GetPrototypeNode getPrototypeNode) {
            return array instanceof SparseArray ||
                            array.isLengthNotWritable() ||
                            getPrototypeNode.execute(obj) != getRealm().getArrayPrototype() ||
                            !getContext().getArrayPrototypeNoElementsAssumption().isValid() ||
                            (!getContext().getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(obj));
        }

        private void spliceRead(Object thisObj, long actualStart, long actualDeleteCount, DynamicObject aObj, long length) {
            long kPlusStart = actualStart;
            if (!hasProperty(thisObj, kPlusStart)) {
                kPlusStart = nextElementIndex(thisObj, kPlusStart, length);
            }
            while (kPlusStart < (actualDeleteCount + actualStart)) {
                Object fromValue = read(thisObj, kPlusStart);
                writeOwn(aObj, kPlusStart - actualStart, fromValue);
                kPlusStart = nextElementIndex(thisObj, kPlusStart, length);
            }
        }

        private void spliceInsert(Object thisObj, long actualStart, Object[] args) {
            final int itemOffset = 2;
            for (int i = itemOffset; i < args.length; i++) {
                write(thisObj, actualStart + i - itemOffset, args[i]);
            }
        }

        private void spliceJSObject(Object thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            if (itemCount < actualDeleteCount) {
                branchA.enter();
                spliceJSObjectShrink(thisObj, len, actualStart, actualDeleteCount, itemCount);
            } else if (itemCount > actualDeleteCount) {
                branchB.enter();
                spliceJSObjectMove(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }
        }

        private void spliceJSObjectMove(Object thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            for (long k = len - actualDeleteCount; k > actualStart; k--) {
                spliceMoveValue(thisObj, (k + actualDeleteCount - 1), (k + itemCount - 1));
            }
        }

        private void spliceJSObjectShrink(Object thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            for (long k = actualStart; k < len - actualDeleteCount; k++) {
                spliceMoveValue(thisObj, (k + actualDeleteCount), (k + itemCount));
            }
            for (long k = len; k > len - actualDeleteCount + itemCount; k--) {
                deletePropertyNode.executeEvaluated(thisObj, k - 1);
            }
        }

        private void spliceMoveValue(Object thisObj, long fromIndex, long toIndex) {
            if (hasProperty(thisObj, fromIndex)) {
                Object val = read(thisObj, fromIndex);
                write(thisObj, toIndex, val);
            } else {
                needMoveDeleteBranch.enter();
                deletePropertyNode.executeEvaluated(thisObj, toIndex);
            }
        }

        final void spliceJSArrayElementwise(DynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            assert JSArray.isJSArray(thisObj); // contract
            if (itemCount < actualDeleteCount) {
                branchA.enter();
                spliceJSArrayElementwiseWalkUp(thisObj, len, actualStart, actualDeleteCount, itemCount);
            } else if (itemCount > actualDeleteCount) {
                branchB.enter();
                spliceJSArrayElementwiseWalkDown(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }
        }

        private void spliceJSArrayElementwiseWalkDown(DynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            long k = len - 1;
            long delta = itemCount - actualDeleteCount;
            while (k > (actualStart + actualDeleteCount - 1)) {
                spliceMoveValue(thisObj, k, k + delta);
                if ((k - delta) > (actualStart + actualDeleteCount - 1) && !hasProperty(thisObj, k - delta)) {
                    // previousElementIndex lets us not visit all elements, thus this delete
                    deletePropertyNode.executeEvaluated(thisObj, k);
                }
                k = previousElementIndex(thisObj, k);
            }
        }

        private void spliceJSArrayElementwiseWalkUp(DynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            long k = actualStart + actualDeleteCount;
            long delta = itemCount - actualDeleteCount;
            while (k < len) {
                spliceMoveValue(thisObj, k, k + delta);

                if ((k - delta) < len && !hasProperty(thisObj, k - delta)) {
                    // nextElementIndex lets us not visit all elements, thus this delete
                    deletePropertyNode.executeEvaluated(thisObj, k);
                }
                k = nextElementIndex(thisObj, k, len);
            }

            k = len - 1;
            while (k >= len + delta) {
                deletePropertyNode.executeEvaluated(thisObj, k);
                k = previousElementIndex(thisObj, k);
            }
        }

        final void spliceJSArrayBlockwise(DynamicObject thisObj, long actualStart, long actualDeleteCount, long itemCount, ScriptArray array) {
            assert JSArray.isJSArray(thisObj); // contract
            if (itemCount < actualDeleteCount) {
                branchA.enter();
                arraySetArrayType(thisObj, array.removeRange(thisObj, actualStart + itemCount, actualStart + actualDeleteCount, errorBranch));
            } else if (itemCount > actualDeleteCount) {
                branchB.enter();
                arraySetArrayType(thisObj, array.addRange(thisObj, actualStart, (int) (itemCount - actualDeleteCount)));
            }
        }

        private void spliceForeignArray(Object thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            InteropLibrary arrays = arrayInterop;
            if (arrays == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.arrayInterop = arrays = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            try {
                if (itemCount < actualDeleteCount) {
                    branchA.enter();
                    spliceForeignArrayShrink(thisObj, len, actualStart, actualDeleteCount, itemCount, arrays);
                } else if (itemCount > actualDeleteCount) {
                    branchB.enter();
                    spliceForeignArrayMove(thisObj, len, actualStart, actualDeleteCount, itemCount, arrays);
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "splice", this);
            }
        }

        private static void spliceForeignArrayMove(Object thisObj, long len, long actualStart, long actualDeleteCount, long itemCount, InteropLibrary arrays)
                        throws UnsupportedMessageException, InvalidArrayIndexException, UnsupportedTypeException {
            for (long k = len - actualDeleteCount; k > actualStart; k--) {
                spliceForeignMoveValue(thisObj, (k + actualDeleteCount - 1), (k + itemCount - 1), arrays);
            }
        }

        private static void spliceForeignArrayShrink(Object thisObj, long len, long actualStart, long actualDeleteCount, long itemCount, InteropLibrary arrays)
                        throws UnsupportedMessageException, InvalidArrayIndexException, UnsupportedTypeException {
            for (long k = actualStart; k < len - actualDeleteCount; k++) {
                spliceForeignMoveValue(thisObj, (k + actualDeleteCount), (k + itemCount), arrays);
            }
            for (long k = len; k > len - actualDeleteCount + itemCount; k--) {
                arrays.removeArrayElement(thisObj, k - 1);
            }
        }

        private static void spliceForeignMoveValue(Object thisObj, long fromIndex, long toIndex, InteropLibrary arrays)
                        throws UnsupportedMessageException, InvalidArrayIndexException, UnsupportedTypeException {
            Object val = arrays.readArrayElement(thisObj, fromIndex);
            arrays.writeArrayElement(thisObj, toIndex, val);
        }
    }

    public abstract static class ArrayForEachIndexCallOperation extends JSArrayOperation {
        public ArrayForEachIndexCallOperation(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        public ArrayForEachIndexCallOperation(JSContext context, JSBuiltin builtin) {
            this(context, builtin, false);
        }

        protected static class DefaultCallbackNode extends ForEachIndexCallNode.CallbackNode {
            @Child protected JSFunctionCallNode callNode = JSFunctionCallNode.createCall();
            protected final ConditionProfile indexInIntRangeCondition = ConditionProfile.createBinaryProfile();

            @Override
            public Object apply(long index, Object value, Object target, Object callback, Object callbackThisArg, Object currentResult) {
                return callNode.executeCall(JSArguments.create(callbackThisArg, callback, value, JSRuntime.boxIndex(index, indexInIntRangeCondition), target));
            }
        }

        @Child private ForEachIndexCallNode forEachIndexNode;

        protected final Object forEachIndexCall(Object arrayObj, Object callbackObj, Object thisArg, long fromIndex, long length, Object initialResult) {
            if (forEachIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                forEachIndexNode = insert(makeForEachIndexCallNode());
            }
            return forEachIndexNode.executeForEachIndex(arrayObj, callbackObj, thisArg, fromIndex, length, initialResult);
        }

        private ForEachIndexCallNode makeForEachIndexCallNode() {
            return ForEachIndexCallNode.create(getContext(), makeCallbackNode(), makeMaybeResultNode(), isForward(), shouldCheckHasProperty());
        }

        protected boolean isForward() {
            return true;
        }

        protected boolean shouldCheckHasProperty() {
            return !isTypedArrayImplementation;
        }

        protected CallbackNode makeCallbackNode() {
            return new DefaultCallbackNode();
        }

        protected abstract MaybeResultNode makeMaybeResultNode();
    }

    public abstract static class JSArrayEveryNode extends ArrayForEachIndexCallOperation {
        public JSArrayEveryNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected boolean every(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);
            return (boolean) forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, true);
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    return toBooleanNode.executeBoolean(callbackResult) ? MaybeResult.continueResult(currentResult) : MaybeResult.returnResult(false);
                }
            };
        }
    }

    public abstract static class JSArrayFilterNode extends ArrayForEachIndexCallOperation {
        private final ValueProfile arrayTypeProfile = ValueProfile.createClassProfile();
        private final ValueProfile resultArrayTypeProfile = ValueProfile.createClassProfile();

        public JSArrayFilterNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected DynamicObject filter(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            DynamicObject resultArray;
            if (isTypedArrayImplementation) {
                resultArray = JSArray.createEmpty(getContext(), getRealm(), 0);
            } else {
                resultArray = (DynamicObject) getArraySpeciesConstructorNode().arraySpeciesCreate(thisJSObj, 0);
            }
            forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, new FilterState(resultArray, 0));

            if (isTypedArrayImplementation) {
                return getTypedResult((DynamicObject) thisJSObj, resultArray);
            } else {
                return resultArray;
            }
        }

        private DynamicObject getTypedResult(DynamicObject thisJSObj, DynamicObject resultArray) {
            long resultLen = arrayGetLength(resultArray);

            Object obj = getArraySpeciesConstructorNode().typedArraySpeciesCreate(thisJSObj, JSRuntime.longToIntOrDouble(resultLen));
            if (!JSDynamicObject.isJSDynamicObject(obj)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAnObject(obj);
            }
            DynamicObject typedResult = (DynamicObject) obj;
            TypedArray typedArray = arrayTypeProfile.profile(JSArrayBufferView.typedArrayGetArrayType(typedResult));
            ScriptArray array = resultArrayTypeProfile.profile(arrayGetArrayType(resultArray));
            for (long i = 0; i < resultLen; i++) {
                typedArray.setElement(typedResult, i, array.getElement(resultArray, i), true);
                TruffleSafepoint.poll(this);
            }
            return typedResult;
        }

        static final class FilterState {
            final DynamicObject resultArray;
            long toIndex;

            FilterState(DynamicObject resultArray, long toIndex) {
                this.resultArray = resultArray;
                this.toIndex = toIndex;
            }
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();
                @Child private WriteElementNode writeOwnNode = WriteElementNode.create(getContext(), true, true);

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    if (toBooleanNode.executeBoolean(callbackResult)) {
                        FilterState filterState = (FilterState) currentResult;
                        writeOwnNode.executeWithTargetAndIndexAndValue(filterState.resultArray, filterState.toIndex++, value);
                    }
                    return MaybeResult.continueResult(currentResult);
                }
            };
        }
    }

    public abstract static class JSArrayForEachNode extends ArrayForEachIndexCallOperation {
        public JSArrayForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object forEach(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObject(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);
            return forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, Undefined.instance);
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    return MaybeResult.continueResult(currentResult);
                }
            };
        }
    }

    public abstract static class JSArraySomeNode extends ArrayForEachIndexCallOperation {
        public JSArraySomeNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected boolean some(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);
            return (boolean) forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, false);
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    return toBooleanNode.executeBoolean(callbackResult) ? MaybeResult.returnResult(true) : MaybeResult.continueResult(currentResult);
                }
            };
        }
    }

    public abstract static class JSArrayMapNode extends ArrayForEachIndexCallOperation {
        public JSArrayMapNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected Object map(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            Object resultArray = getArraySpeciesConstructorNode().createEmptyContainer(thisJSObj, length);
            return forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, resultArray);
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Child private WriteElementNode writeOwnNode = WriteElementNode.create(getContext(), true, true);

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    writeOwnNode.executeWithTargetAndIndexAndValue(currentResult, index, callbackResult);
                    return MaybeResult.continueResult(currentResult);
                }
            };
        }
    }

    public abstract static class FlattenIntoArrayNode extends JavaScriptBaseNode {

        private static final class InnerFlattenCallNode extends JavaScriptRootNode {

            @Child private FlattenIntoArrayNode flattenNode;

            InnerFlattenCallNode(JSContext context, FlattenIntoArrayNode flattenNode) {
                super(context.getLanguage(), null, null);
                this.flattenNode = flattenNode;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject resultArray = (DynamicObject) arguments[0];
                Object element = arguments[1];
                long elementLen = (long) arguments[2];
                long targetIndex = (long) arguments[3];
                long depth = (long) arguments[4];
                return flattenNode.flatten(resultArray, element, elementLen, targetIndex, depth, null, null);
            }
        }

        protected final JSContext context;
        protected final boolean withMapCallback;

        @Child private ForEachIndexCallNode forEachIndexNode;
        @Child private JSToObjectNode toObjectNode;
        @Child private JSGetLengthNode getLengthNode;

        protected FlattenIntoArrayNode(JSContext context, boolean withMapCallback) {
            this.context = context;
            this.withMapCallback = withMapCallback;
        }

        public static FlattenIntoArrayNode create(JSContext context, boolean withCallback) {
            return FlattenIntoArrayNodeGen.create(context, withCallback);
        }

        protected abstract long executeLong(DynamicObject target, Object source, long sourceLen, long start, long depth, Object callback, Object thisArg);

        @Specialization
        protected long flatten(DynamicObject target, Object source, long sourceLen, long start, long depth, Object callback, Object thisArg) {

            boolean callbackUndefined = callback == null;

            FlattenState flattenState = new FlattenState(target, start, depth, callbackUndefined);
            Object thisJSObj = toObject(source);
            forEachIndexCall(thisJSObj, callback, thisArg, 0, sourceLen, flattenState);

            return flattenState.targetIndex;
        }

        protected final Object forEachIndexCall(Object arrayObj, Object callbackObj, Object thisArg, long fromIndex, long length, Object initialResult) {
            if (forEachIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                forEachIndexNode = insert(makeForEachIndexCallNode());
            }
            return forEachIndexNode.executeForEachIndex(arrayObj, callbackObj, thisArg, fromIndex, length, initialResult);
        }

        private ForEachIndexCallNode makeForEachIndexCallNode() {
            return ForEachIndexCallNode.create(context, makeCallbackNode(), makeMaybeResultNode(), true, true);
        }

        protected CallbackNode makeCallbackNode() {
            return withMapCallback ? new ArrayForEachIndexCallOperation.DefaultCallbackNode() : null;
        }

        protected final Object toObject(Object target) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(context));
            }
            return toObjectNode.execute(target);
        }

        protected long getLength(Object thisObject) {
            if (getLengthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getLengthNode = insert(JSGetLengthNode.create(context));
            }
            return getLengthNode.executeLong(thisObject);
        }

        static final class FlattenState {
            final DynamicObject resultArray;
            final boolean callbackUndefined;
            final long depth;
            long targetIndex;

            FlattenState(DynamicObject result, long toIndex, long depth, boolean callbackUndefined) {
                this.resultArray = result;
                this.callbackUndefined = callbackUndefined;
                this.targetIndex = toIndex;
                this.depth = depth;
            }
        }

        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {

                protected final BranchProfile errorBranch = BranchProfile.create();

                @Child private WriteElementNode writeOwnNode = WriteElementNode.create(context, true, true);
                @Child private DirectCallNode innerFlattenCall;

                @Override
                public MaybeResult<Object> apply(long index, Object originalValue, Object callbackResult, Object resultState) {
                    boolean shouldFlatten = false;
                    FlattenState state = (FlattenState) resultState;
                    Object value = state.callbackUndefined ? originalValue : callbackResult;
                    if (state.depth > 0) {
                        shouldFlatten = JSRuntime.isArray(value);
                    }
                    if (shouldFlatten) {
                        long elementLen = getLength(toObject(value));
                        state.targetIndex = makeFlattenCall(state.resultArray, value, elementLen, state.targetIndex, state.depth - 1);
                    } else {
                        if (state.targetIndex >= JSRuntime.MAX_SAFE_INTEGER_LONG) { // 2^53-1
                            errorBranch.enter();
                            throw Errors.createTypeError("Index out of bounds in flatten into array");
                        }
                        writeOwnNode.executeWithTargetAndIndexAndValue(state.resultArray, state.targetIndex++, value);
                    }
                    return MaybeResult.continueResult(resultState);
                }

                private long makeFlattenCall(DynamicObject targetArray, Object element, long elementLength, long targetIndex, long depth) {
                    if (innerFlattenCall == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        JSFunctionData flattenFunctionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.ArrayFlattenIntoArray, c -> createOrGetFlattenCallFunctionData(c));
                        innerFlattenCall = insert(DirectCallNode.create(flattenFunctionData.getCallTarget()));
                    }
                    return (long) innerFlattenCall.call(targetArray, element, elementLength, targetIndex, depth);
                }
            };
        }

        private static JSFunctionData createOrGetFlattenCallFunctionData(JSContext context) {
            return JSFunctionData.createCallOnly(context, new InnerFlattenCallNode(context, FlattenIntoArrayNode.create(context, false)).getCallTarget(), 0, "");
        }
    }

    public abstract static class JSArrayFlatMapNode extends JSArrayOperation {
        public JSArrayFlatMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected Object flatMap(Object thisObj, Object callback, Object thisArg,
                        @Cached("createFlattenIntoArrayNode(getContext())") FlattenIntoArrayNode flattenIntoArrayNode) {
            Object thisJSObj = toObject(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            Object resultArray = getArraySpeciesConstructorNode().createEmptyContainer(thisJSObj, 0);
            flattenIntoArrayNode.flatten((DynamicObject) resultArray, thisJSObj, length, 0, 1, callbackFn, thisArg);
            return resultArray;
        }

        protected static final FlattenIntoArrayNode createFlattenIntoArrayNode(JSContext context) {
            return FlattenIntoArrayNodeGen.create(context, true);
        }
    }

    public abstract static class JSArrayFlatNode extends JSArrayOperation {
        @Child private JSToIntegerAsIntNode toIntegerNode;

        public JSArrayFlatNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected Object flat(Object thisObj, Object depth,
                        @Cached("createFlattenIntoArrayNode(getContext())") FlattenIntoArrayNode flattenIntoArrayNode) {
            Object thisJSObj = toObject(thisObj);
            long length = getLength(thisJSObj);
            long depthNum = (depth == Undefined.instance) ? 1 : toIntegerAsInt(depth);

            Object resultArray = getArraySpeciesConstructorNode().createEmptyContainer(thisJSObj, 0);
            flattenIntoArrayNode.flatten((DynamicObject) resultArray, thisJSObj, length, 0, depthNum, null, null);
            return resultArray;
        }

        private int toIntegerAsInt(Object depth) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerAsIntNode.create());
            }
            return toIntegerNode.executeInt(depth);
        }

        protected static final FlattenIntoArrayNode createFlattenIntoArrayNode(JSContext context) {
            return FlattenIntoArrayNodeGen.create(context, false);
        }
    }

    public abstract static class JSArrayFindNode extends JSArrayOperation {
        @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();
        @Child private JSFunctionCallNode callNode = JSFunctionCallNode.createCall();

        public JSArrayFindNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        private Object callPredicate(Object function, Object target, Object value, double index, Object thisObj) {
            return callNode.executeCall(JSArguments.create(target, function, value, index, thisObj));
        }

        @Specialization
        protected Object find(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            for (long idx = 0; idx < length; idx++) {
                Object value = read(thisObj, idx);
                Object callbackResult = callPredicate(callbackFn, thisArg, value, idx, thisJSObj);
                boolean testResult = toBooleanNode.executeBoolean(callbackResult);
                if (testResult) {
                    reportLoopCount(idx);
                    return value;
                }
            }
            reportLoopCount(length);
            return Undefined.instance;
        }
    }

    public abstract static class JSArrayFindIndexNode extends JSArrayOperation {
        @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();
        @Child private JSFunctionCallNode callNode = JSFunctionCallNode.createCall();

        public JSArrayFindIndexNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        private Object callPredicate(Object function, Object target, Object value, double index, Object thisObj) {
            return callNode.executeCall(JSArguments.create(target, function, value, index, thisObj));
        }

        @Specialization
        protected Object findIndex(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            for (long idx = 0; idx < length; idx++) {
                Object value = read(thisObj, idx);
                Object callbackResult = callPredicate(callbackFn, thisArg, value, idx, thisJSObj);
                boolean testResult = toBooleanNode.executeBoolean(callbackResult);
                if (testResult) {
                    reportLoopCount(idx);
                    return JSRuntime.positiveLongToIntOrDouble(idx);
                }
            }
            reportLoopCount(length);
            return -1;
        }
    }

    public abstract static class JSArraySortNode extends JSArrayOperation {

        @Child private DeletePropertyNode deletePropertyNode; // DeletePropertyOrThrow
        private final ConditionProfile isSparse = ConditionProfile.create();
        private final BranchProfile hasCompareFnBranch = BranchProfile.create();
        private final BranchProfile noCompareFnBranch = BranchProfile.create();
        private final BranchProfile growProfile = BranchProfile.create();
        @Child private InteropLibrary interopNode;
        @Child private ImportValueNode importValueNode;

        public JSArraySortNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization(guards = {"!isTypedArrayImplementation", "isJSFastArray(thisObj)"}, assumptions = "getContext().getArrayPrototypeNoElementsAssumption()")
        protected DynamicObject sortArray(final DynamicObject thisObj, final Object compare,
                        @Cached("create(getContext())") JSArrayToDenseObjectArrayNode arrayToObjectArrayNode,
                        @Cached("create(getContext(), true)") JSArrayDeleteRangeNode arrayDeleteRangeNode) {
            checkCompareFunction(compare);
            long len = getLength(thisObj);

            if (len < 2) {
                // nothing to do
                return thisObj;
            }

            ScriptArray scriptArray = arrayGetArrayType(thisObj);
            Object[] array = arrayToObjectArrayNode.executeObjectArray(thisObj, scriptArray, len);

            sortIntl(getComparator(thisObj, compare), array);
            reportLoopCount(len); // best effort guess, let's not go for n*log(n)

            for (int i = 0; i < array.length; i++) {
                write(thisObj, i, array[i]);
            }

            if (isSparse.profile(array.length < len)) {
                arrayDeleteRangeNode.execute(thisObj, scriptArray, array.length, len);
            }
            return thisObj;
        }

        private void delete(Object obj, Object i) {
            if (deletePropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                JSContext context = getContext();
                deletePropertyNode = insert(DeletePropertyNode.create(true, context));
            }
            deletePropertyNode.executeEvaluated(obj, i);
        }

        @Specialization
        protected Object sort(Object thisObj, final Object comparefn,
                        @Cached("createBinaryProfile()") ConditionProfile isJSObject) {
            checkCompareFunction(comparefn);
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            if (isJSObject.profile(JSDynamicObject.isJSDynamicObject(thisJSObj))) {
                return sortJSObject(comparefn, (DynamicObject) thisJSObj);
            } else {
                return sortForeignObject(comparefn, thisJSObj);
            }
        }

        private DynamicObject sortJSObject(final Object comparefn, DynamicObject thisJSObj) {
            long len = getLength(thisJSObj);

            if (len == 0) {
                // nothing to do
                return thisJSObj;
            }

            Object[] array = jsobjectToArray(thisJSObj, len);

            Comparator<Object> comparator = getComparator(thisJSObj, comparefn);
            if (isTypedArrayImplementation && comparefn == Undefined.instance) {
                assert comparator == null;
                prepareForDefaultComparator(array);
            }
            sortIntl(comparator, array);
            reportLoopCount(len);

            for (int i = 0; i < array.length; i++) {
                write(thisJSObj, i, array[i]);
            }

            if (isSparse.profile(array.length < len)) {
                deleteGenericElements(thisJSObj, array.length, len);
            }
            return thisJSObj;
        }

        public Object sortForeignObject(Object comparefn, Object thisObj) {
            assert JSGuards.isForeignObject(thisObj);
            long len = getLength(thisObj);

            if (len < 2) {
                // nothing to do
                return thisObj;
            }

            if (len >= Integer.MAX_VALUE) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidArrayLength();
            }

            Object[] array = foreignArrayToObjectArray(thisObj, (int) len);

            Comparator<Object> comparator = getComparator(thisObj, comparefn);
            sortIntl(comparator, array);
            reportLoopCount(len);

            for (int i = 0; i < array.length; i++) {
                write(thisObj, i, array[i]);
            }
            return thisObj;
        }

        private void checkCompareFunction(Object compare) {
            if (!(compare == Undefined.instance || isCallable(compare))) {
                errorBranch.enter();
                throw Errors.createTypeError("The comparison function must be either a function or undefined");
            }
        }

        private Comparator<Object> getComparator(Object thisObj, Object compare) {
            if (compare == Undefined.instance) {
                noCompareFnBranch.enter();
                return getDefaultComparator(thisObj);
            } else {
                assert isCallable(compare);
                hasCompareFnBranch.enter();
                DynamicObject arrayBufferObj = isTypedArrayImplementation && JSArrayBufferView.isJSArrayBufferView(thisObj) ? JSArrayBufferView.getArrayBuffer((DynamicObject) thisObj) : null;
                return new SortComparator(compare, arrayBufferObj);
            }
        }

        private Comparator<Object> getDefaultComparator(Object thisObj) {
            if (isTypedArrayImplementation) {
                return null; // use Comparable.compareTo (equivalent to Comparator.naturalOrder())
            } else {
                if (JSArray.isJSArray(thisObj)) {
                    ScriptArray array = arrayGetArrayType((DynamicObject) thisObj);
                    if (array instanceof AbstractIntArray || array instanceof ConstantByteArray || array instanceof ConstantIntArray) {
                        return JSArray.DEFAULT_JSARRAY_INTEGER_COMPARATOR;
                    } else if (array instanceof AbstractDoubleArray || array instanceof ConstantDoubleArray) {
                        return JSArray.DEFAULT_JSARRAY_DOUBLE_COMPARATOR;
                    }
                }
                return JSArray.DEFAULT_JSARRAY_COMPARATOR;
            }
        }

        /**
         * In a generic JSObject, this deletes all elements between the actual "size" (i.e., number
         * of non-empty elements) and the "length" (value of the property). I.e., it cleans up
         * garbage remaining after sorting all elements to lower indices (in case there are holes).
         */
        private void deleteGenericElements(Object obj, long fromIndex, long toIndex) {
            for (long index = fromIndex; index < toIndex; index++) {
                delete(obj, index);
            }
        }

        @TruffleBoundary
        private static void sortIntl(Comparator<Object> comparator, Object[] array) {
            try {
                Arrays.sort(array, comparator);
            } catch (IllegalArgumentException e) {
                // Collections.sort throws IllegalArgumentException when
                // Comparison method violates its general contract

                // See ECMA spec 15.4.4.11 Array.prototype.sort (comparefn).
                // If "comparefn" is not undefined and is not a consistent
                // comparison function for the elements of this array, the
                // behaviour of sort is implementation-defined.
            }
        }

        private static void prepareForDefaultComparator(Object[] array) {
            // Default comparator (based on Comparable.compareTo) cannot be used
            // for elements of different type (for example, Integer.compareTo()
            // accepts Integers only, not Doubles).
            boolean needsConversion = false;
            Class<?> clazz = array[0].getClass();
            for (Object element : array) {
                Class<?> c = element.getClass();
                if (clazz != c) {
                    needsConversion = true;
                    break;
                }
            }
            if (needsConversion) {
                for (int i = 0; i < array.length; i++) {
                    array[i] = JSRuntime.toDouble(array[i]);
                }
            }
        }

        private class SortComparator implements Comparator<Object> {
            private final Object compFnObj;
            private final DynamicObject arrayBufferObj;
            private final boolean isFunction;

            SortComparator(Object compFnObj, DynamicObject arrayBufferObj) {
                this.compFnObj = compFnObj;
                this.arrayBufferObj = arrayBufferObj;
                this.isFunction = JSFunction.isJSFunction(compFnObj);
            }

            @Override
            public int compare(Object arg0, Object arg1) {
                if (arg0 == Undefined.instance) {
                    if (arg1 == Undefined.instance) {
                        return 0;
                    }
                    return 1;
                } else if (arg1 == Undefined.instance) {
                    return -1;
                }
                Object retObj;
                if (isFunction) {
                    retObj = JSFunction.call((DynamicObject) compFnObj, Undefined.instance, new Object[]{arg0, arg1});
                } else {
                    retObj = JSRuntime.call(compFnObj, Undefined.instance, new Object[]{arg0, arg1});
                }
                int res = convertResult(retObj);
                if (isTypedArrayImplementation) {
                    if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBufferObj)) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorDetachedBuffer();
                    }
                }
                return res;
            }

            private int convertResult(Object retObj) {
                if (retObj instanceof Integer) {
                    return (int) retObj;
                } else {
                    double d = JSRuntime.toDouble(retObj);
                    if (d < 0) {
                        return -1;
                    } else if (d > 0) {
                        return 1;
                    } else {
                        // +/-0 or NaN
                        return 0;
                    }
                }
            }
        }

        @TruffleBoundary
        private Object[] jsobjectToArray(DynamicObject thisObj, long len) {
            SimpleArrayList<Object> list = SimpleArrayList.create(len);
            for (long k = 0; k < len; k++) {
                if (JSObject.hasProperty(thisObj, k)) {
                    list.add(JSObject.get(thisObj, k), growProfile);
                }
            }
            return list.toArray();
        }

        private Object[] foreignArrayToObjectArray(Object thisObj, int len) {
            InteropLibrary interop = interopNode;
            ImportValueNode importValue = importValueNode;
            if (interop == null || importValue == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                interopNode = interop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                importValueNode = importValue = insert(ImportValueNode.create());
            }
            Object[] array = new Object[len];
            for (int index = 0; index < len; index++) {
                array[index] = JSInteropUtil.readArrayElementOrDefault(thisObj, index, Undefined.instance, interop, importValue, this);
            }
            return array;
        }

    }

    public abstract static class JSArrayReduceNode extends ArrayForEachIndexCallOperation {
        private final boolean isForward;

        public JSArrayReduceNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation, boolean isForward) {
            super(context, builtin, isTypedArrayImplementation);
            this.isForward = isForward;
        }

        private final BranchProfile findInitialValueBranch = BranchProfile.create();
        @Child private ForEachIndexCallNode forEachIndexFindInitialNode;

        @Specialization
        protected Object reduce(Object thisObj, Object callback, Object... initialValueOpt) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            Object currentValue = initialValueOpt.length > 0 ? initialValueOpt[0] : null;
            long currentIndex = isForward() ? 0 : length - 1;
            if (currentValue == null) {
                findInitialValueBranch.enter();
                Pair<Long, Object> res = findInitialValue(thisJSObj, currentIndex, length);
                currentIndex = res.getFirst() + (isForward() ? 1 : -1);
                currentValue = res.getSecond();
            }

            return forEachIndexCall(thisJSObj, callbackFn, Undefined.instance, currentIndex, length, currentValue);
        }

        @Override
        protected boolean isForward() {
            return isForward;
        }

        @SuppressWarnings("unchecked")
        protected final Pair<Long, Object> findInitialValue(Object arrayObj, long fromIndex, long length) {
            if (length >= 0) {
                Pair<Long, Object> res = (Pair<Long, Object>) getForEachIndexFindInitialNode().executeForEachIndex(arrayObj, null, null, fromIndex, length, null);
                if (res != null) {
                    return res;
                }
            }
            errorBranch.enter();
            throw reduceNoInitialValueError();
        }

        private ForEachIndexCallNode getForEachIndexFindInitialNode() {
            if (forEachIndexFindInitialNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                forEachIndexFindInitialNode = insert(ForEachIndexCallNode.create(getContext(), null, new ForEachIndexCallNode.MaybeResultNode() {
                    @Override
                    public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                        return MaybeResult.returnResult(new Pair<>(index, value));
                    }
                }, isForward(), shouldCheckHasProperty()));
            }
            return forEachIndexFindInitialNode;
        }

        @Override
        protected CallbackNode makeCallbackNode() {
            return new DefaultCallbackNode() {
                @Override
                public Object apply(long index, Object value, Object target, Object callback, Object callbackThisArg, Object currentResult) {
                    return callNode.executeCall(JSArguments.create(callbackThisArg, callback, currentResult, value, JSRuntime.boxIndex(index, indexInIntRangeCondition), target));
                }
            };
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    return MaybeResult.continueResult(callbackResult);
                }
            };
        }

        @TruffleBoundary
        protected static RuntimeException reduceNoInitialValueError() {
            throw Errors.createTypeError("Reduce of empty array with no initial value");
        }
    }

    public abstract static class JSArrayFillNode extends JSArrayOperationWithToInt {
        private final ConditionProfile offsetProfile1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile2 = ConditionProfile.createBinaryProfile();

        public JSArrayFillNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected Object fill(Object thisObj, Object value, Object start, Object end) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(thisJSObj);
            long lStart = JSRuntime.getOffset(toIntegerAsLong(start), len, offsetProfile1);
            long lEnd = end == Undefined.instance ? len : JSRuntime.getOffset(toIntegerAsLong(end), len, offsetProfile2);

            for (long idx = lStart; idx < lEnd; idx++) {
                write(thisJSObj, idx, value);
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(lEnd - lStart);
            return thisJSObj;
        }
    }

    public abstract static class JSArrayCopyWithinNode extends JSArrayOperationWithToInt {

        @Child private DeletePropertyNode deletePropertyNode; // DeletePropertyOrThrow
        private final ConditionProfile offsetProfile1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile2 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile3 = ConditionProfile.createBinaryProfile();

        public JSArrayCopyWithinNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.deletePropertyNode = DeletePropertyNode.create(THROW_ERROR, context);
        }

        @Specialization
        protected Object copyWithin(Object thisObj, Object target, Object start, Object end) {
            Object obj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(obj);
            long to = JSRuntime.getOffset(toIntegerAsLong(target), len, offsetProfile1);
            long from = JSRuntime.getOffset(toIntegerAsLong(start), len, offsetProfile2);

            long finalIdx;
            if (end == Undefined.instance) {
                finalIdx = len;
            } else {
                finalIdx = JSRuntime.getOffset(toIntegerAsLong(end), len, offsetProfile3);
            }
            long count = Math.min(finalIdx - from, len - to);
            long expectedCount = count;
            if (count > 0) {
                if (isTypedArrayImplementation) {
                    checkHasDetachedBuffer((DynamicObject) thisObj);
                }

                long direction;
                if (from < to && to < (from + count)) {
                    direction = -1;
                    from = from + count - 1;
                    to = to + count - 1;
                } else {
                    direction = 1;
                }

                while (count > 0) {
                    if (isTypedArrayImplementation || hasProperty(obj, from)) {
                        Object fromVal = read(obj, from);
                        write(obj, to, fromVal);
                    } else {
                        deletePropertyNode.executeEvaluated(obj, to);
                    }
                    from += direction;
                    to += direction;
                    count--;
                    TruffleSafepoint.poll(this);
                }
                reportLoopCount(expectedCount);
            }
            return obj;
        }
    }

    public abstract static class JSArrayIncludesNode extends JSArrayOperationWithToInt {

        public JSArrayIncludesNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected boolean includes(Object thisValue, Object searchElement, Object fromIndex,
                        @Cached("createSameValueZero()") JSIdenticalNode identicalNode) {
            Object thisObj = toObjectOrValidateTypedArray(thisValue);
            long len = getLength(thisObj);
            if (len == 0) {
                return false;
            }

            long n = toIntegerAsLong(fromIndex);
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = len + n;
                if (k < 0) {
                    k = 0;
                }
            }

            if (!identicalNode.executeBoolean(searchElement, searchElement)) {
                return true;
            }

            long startIdx = k;
            while (k < len) {
                Object currentElement = read(thisObj, k);

                if (identicalNode.executeBoolean(searchElement, currentElement)) {
                    reportLoopCount(k - startIdx);
                    return true;
                }
                k++;
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(len - startIdx);
            return false;
        }
    }

    public abstract static class JSArrayReverseNode extends JSArrayOperation {
        @Child private TestArrayNode hasHolesNode;
        @Child private DeletePropertyNode deletePropertyNode;
        private final ConditionProfile bothExistProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile onlyUpperExistsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile onlyLowerExistsProfile = ConditionProfile.createBinaryProfile();

        public JSArrayReverseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.hasHolesNode = TestArrayNode.createHasHoles();
        }

        private boolean deleteProperty(Object array, long index) {
            if (deletePropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                deletePropertyNode = insert(DeletePropertyNode.create(true, getContext()));
            }
            return deletePropertyNode.executeEvaluated(array, index);
        }

        @Specialization
        protected Object reverseJSArray(JSArrayObject thisObj) {
            return reverse(thisObj, true);
        }

        @Specialization(replaces = "reverseJSArray")
        protected Object reverseGeneric(Object thisObj) {
            final Object array = toObject(thisObj);
            return reverse(array, JSArray.isJSArray(array));
        }

        private Object reverse(Object array, boolean isArray) {
            final long length = getLength(array);
            long lower = 0;
            long upper = length - 1;
            boolean hasHoles = isArray && hasHolesNode.executeBoolean((DynamicObject) array);

            while (lower < upper) {
                boolean lowerExists;
                boolean upperExists;
                Object lowerValue = null;
                Object upperValue = null;

                if (getContext().getEcmaScriptVersion() < 6) { // ES5 expects GET before HAS
                    lowerValue = read(array, lower);
                    upperValue = read(array, upper);
                    lowerExists = lowerValue != Undefined.instance || hasProperty(array, lower);
                    upperExists = upperValue != Undefined.instance || hasProperty(array, upper);
                } else { // ES6 expects HAS before GET and tries GET only if HAS succeeds
                    lowerExists = hasProperty(array, lower);
                    if (lowerExists) {
                        lowerValue = read(array, lower);
                    }
                    upperExists = hasProperty(array, upper);
                    if (upperExists) {
                        upperValue = read(array, upper);
                    }
                }

                if (bothExistProfile.profile(lowerExists && upperExists)) {
                    write(array, lower, upperValue);
                    write(array, upper, lowerValue);
                } else if (onlyUpperExistsProfile.profile(!lowerExists && upperExists)) {
                    write(array, lower, upperValue);
                    deleteProperty(array, upper);
                } else if (onlyLowerExistsProfile.profile(lowerExists && !upperExists)) {
                    deleteProperty(array, lower);
                    write(array, upper, lowerValue);
                } else {
                    assert !lowerExists && !upperExists; // No action required.
                }

                if (hasHoles) {
                    long nextLower = nextElementIndex(array, lower, length);
                    long nextUpper = previousElementIndex(array, upper);
                    if ((length - nextLower - 1) >= nextUpper) {
                        lower = nextLower;
                        upper = length - lower - 1;
                    } else {
                        lower = length - nextUpper - 1;
                        upper = nextUpper;
                    }
                } else {
                    lower++;
                    upper--;
                }
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(lower);
            return array;
        }
    }

    public static class CreateArrayIteratorNode extends JavaScriptBaseNode {
        private final int iterationKind;
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private PropertySetNode setIterationKindNode;

        protected CreateArrayIteratorNode(JSContext context, int iterationKind) {
            this.iterationKind = iterationKind;
            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.setIterationKindNode = PropertySetNode.createSetHidden(JSArray.ARRAY_ITERATION_KIND_ID, context);
        }

        public static CreateArrayIteratorNode create(JSContext context, int iterationKind) {
            return new CreateArrayIteratorNode(context, iterationKind);
        }

        public DynamicObject execute(VirtualFrame frame, Object array) {
            assert JSGuards.isJSObject(array) || JSGuards.isForeignObject(array);
            DynamicObject iterator = createObjectNode.execute(frame, getRealm().getArrayIteratorPrototype());
            setIteratedObjectNode.setValue(iterator, array);
            setNextIndexNode.setValue(iterator, 0L);
            setIterationKindNode.setValueInt(iterator, iterationKind);
            return iterator;
        }
    }

    public abstract static class JSArrayIteratorNode extends JSBuiltinNode {
        @Child private CreateArrayIteratorNode createArrayIteratorNode;

        public JSArrayIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.createArrayIteratorNode = CreateArrayIteratorNode.create(context, iterationKind);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject doJSObject(VirtualFrame frame, DynamicObject thisObj) {
            return createArrayIteratorNode.execute(frame, thisObj);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected DynamicObject doNotJSObject(VirtualFrame frame, Object thisObj,
                        @Cached("createToObject(getContext())") JSToObjectNode toObjectNode) {
            return createArrayIteratorNode.execute(frame, toObjectNode.execute(thisObj));
        }
    }

    public abstract static class JSArrayAtNode extends JSArrayOperationWithToInt {
        public JSArrayAtNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected Object at(Object thisObj, Object index) {
            final Object o = toObjectOrValidateTypedArray(thisObj);
            final long length = getLength(o);
            long relativeIndex = toIntegerAsLong(index);
            long k;
            if (relativeIndex >= 0) {
                k = relativeIndex;
            } else {
                k = length + relativeIndex;
            }
            if (k < 0 || k >= length) {
                return Undefined.instance;
            }
            return read(o, k);
        }
    }

    private static class GroupingRecord {

        private final Object key;
        private final List<Object> elements;

        GroupingRecord(Object key, Object value) {
            this.key = key;
            this.elements = new ArrayList<>();
            this.elements.add(value);
        }

        public Object getKey() {
            return key;
        }

        public void appendElement(Object value) {
            elements.add(value);
        }

        @TruffleBoundary
        public Object[] getElements() {
            return elements.toArray(new Object[0]);
        }
    }

    @TruffleBoundary
    private static void addValueToKeyedGroup(List<GroupingRecord> groups, Object key, Object value) {
        for (GroupingRecord record : groups) {
            if (JSRuntime.isSameValue(record.getKey(), key)) {
                record.appendElement(value);
                return;
            }
        }
        groups.add(new GroupingRecord(key, value));
    }

    public abstract static class JSArrayGroupByBaseNode extends JSArrayOperation {
        @Child private JSFunctionCallNode callNode = JSFunctionCallNode.createCall();

        protected JSArrayGroupByBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        protected List<GroupingRecord> collectGroupByResults(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObject(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            List<GroupingRecord> groups = new ArrayList<>();
            for (long k = 0; k < length; k++) {
                Object kValue = read(thisObj, k);
                Object key = toKey(callNode.executeCall(JSArguments.create(thisArg, callbackFn, kValue, k, thisJSObj)));
                addValueToKeyedGroup(groups, key, kValue);
            }

            return groups;
        }

        protected abstract Object toKey(Object callbackResult);

    }

    public abstract static class JSArrayGroupByNode extends JSArrayGroupByBaseNode {

        @Child private JSToPropertyKeyNode toPropertyKeyNode;
        @Child private WriteElementNode writeElementNode;

        public JSArrayGroupByNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toPropertyKeyNode = JSToPropertyKeyNodeGen.create();
            this.writeElementNode = WriteElementNode.create(context, false);
        }

        @Specialization
        protected Object groupBy(Object thisObj, Object callback, Object thisArg) {
            List<GroupingRecord> resultArray = collectGroupByResults(thisObj, callback, thisArg);
            DynamicObject obj = JSOrdinary.createWithNullPrototype(getContext());
            return createGroupByResult(obj, resultArray);
        }

        @Override
        protected final Object toKey(Object callbackResult) {
            return toPropertyKeyNode.execute(callbackResult);
        }

        protected Object createGroupByResult(DynamicObject obj, List<GroupingRecord> resultArray) {
            for (int i = 0; i < resultArray.size(); i++) {
                GroupingRecord r = resultArray.get(i);
                var elements = JSArray.createConstant(getContext(), getRealm(), r.getElements());
                writeElementNode.executeWithTargetAndIndexAndValue(obj, r.getKey(), elements);
            }
            return obj;
        }
    }

    public abstract static class JSArrayGroupByToMapNode extends JSArrayGroupByBaseNode {

        public JSArrayGroupByToMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object groupByToMap(Object thisObj, Object callback, Object thisArg) {
            List<GroupingRecord> resultArray = collectGroupByResults(thisObj, callback, thisArg);
            var map = JSMap.create(getContext(), getRealm());
            return createGroupByResult(map, resultArray);
        }

        @TruffleBoundary
        @Override
        protected final Object toKey(Object callbackResult) {
            if (callbackResult instanceof Double && JSRuntime.isNegativeZero((double) callbackResult)) {
                return 0;
            }
            return callbackResult;
        }

        @TruffleBoundary
        protected Object createGroupByResult(DynamicObject map, List<GroupingRecord> resultArray) {
            for (int i = 0; i < resultArray.size(); i++) {
                GroupingRecord r = resultArray.get(i);
                var elements = JSArray.createConstant(getContext(), getRealm(), r.getElements());
                JSMap.getInternalMap(map).put(r.getKey(), elements);
            }
            return map;
        }
    }
}
