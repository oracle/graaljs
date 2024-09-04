/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Comparator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import com.oracle.truffle.api.strings.TruffleStringBuilderUTF16;
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
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToSplicedNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToStringNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayUnshiftNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayWithNodeGen;
import com.oracle.truffle.js.builtins.sort.SortComparator;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JSNodeUtil;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
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
import com.oracle.truffle.js.nodes.array.TypedArrayLengthNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.LongToIntOrDoubleNode;
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
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSArrayIterator;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
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

        // ES2023
        findLast(1),
        findLastIndex(1),
        toReversed(0),
        toSorted(1),
        toSpliced(2),
        with(2);

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
            return switch (this) {
                case find, findIndex, fill, copyWithin, keys, values, entries -> JSConfig.ECMAScript2015;
                case includes -> JSConfig.ECMAScript2016;
                case flat, flatMap -> JSConfig.ECMAScript2019;
                case at -> JSConfig.ECMAScript2022;
                case findLast, findLastIndex, toReversed, toSorted, toSpliced, with -> JSConfig.ECMAScript2023;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
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
                return JSArraySortNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case reduce:
                return JSArrayReduceNodeGen.create(context, builtin, false, true, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case reduceRight:
                return JSArrayReduceNodeGen.create(context, builtin, false, false, args().withThis().fixedArgs(1).varArgs().createArgumentNodes(context));
            case reverse:
                return JSArrayReverseNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));

            case find:
                return JSArrayFindNodeGen.create(context, builtin, false, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findIndex:
                return JSArrayFindIndexNodeGen.create(context, builtin, false, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findLast:
                return JSArrayFindNodeGen.create(context, builtin, false, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case findLastIndex:
                return JSArrayFindIndexNodeGen.create(context, builtin, false, true, args().withThis().fixedArgs(2).createArgumentNodes(context));
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

            case toReversed:
                return ArrayPrototypeBuiltinsFactory.JSArrayToReversedNodeGen.create(context, builtin, false, args().withThis().fixedArgs(0).createArgumentNodes(context));
            case toSorted:
                return ArrayPrototypeBuiltinsFactory.JSArrayToSortedNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toSpliced:
                return JSArrayToSplicedNodeGen.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case with:
                return JSArrayWithNodeGen.create(context, builtin, false, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class BasicArrayOperation extends JSBuiltinNode {

        protected final boolean isTypedArrayImplementation; // for reusing array code on TypedArrays
        @Child private JSToObjectNode toObjectNode;
        @Child private JSGetLengthNode getLengthNode;
        @Child private TypedArrayLengthNode typedArrayLengthNode;
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
                toObjectNode = insert(JSToObjectNode.create());
            }
            return toObjectNode.execute(target);
        }

        protected long getLength(Object thisObject) {
            if (isTypedArrayImplementation) {
                // %TypedArray%.prototype.* don't access the "length" property
                if (typedArrayLengthNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    typedArrayLengthNode = insert(TypedArrayLengthNode.create());
                }
                return typedArrayLengthNode.execute(null, (JSTypedArrayObject) thisObject, getContext());
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

        protected final void checkOutOfBounds(JSTypedArrayObject view) {
            if (JSArrayBufferView.isOutOfBounds(view, getContext())) {
                errorBranch.enter();
                throw Errors.createTypeErrorOutOfBoundsTypedArray();
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
            if (JSArrayBufferView.isOutOfBounds(typedArrayObject, getContext())) {
                errorBranch.enter();
                throw Errors.createTypeErrorOutOfBoundsTypedArray();
            }
            return typedArrayObject;
        }

        protected void reportLoopCount(long count) {
            reportLoopCount(this, count);
        }
    }

    public static class ArraySpeciesConstructorNode extends JavaScriptBaseNode {
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
        private final ConditionProfile arraySpeciesEmpty = ConditionProfile.create();
        private final JSContext context;

        protected ArraySpeciesConstructorNode(JSContext context, boolean isTypedArrayImplementation) {
            this.context = context;
            this.isTypedArrayImplementation = isTypedArrayImplementation;
            this.isArrayNode = JSIsArrayNode.createIsArray();
            this.constructorCall = JSFunctionCallNode.createNew();
        }

        @NeverDefault
        public static ArraySpeciesConstructorNode create(JSContext context, boolean isTypedArrayImplementation) {
            return new ArraySpeciesConstructorNode(context, isTypedArrayImplementation);
        }

        protected final Object createEmptyContainer(Object thisObj, long size) {
            if (isTypedArrayImplementation) {
                // ValidateTypedArray already performed in the caller.
                return typedArraySpeciesCreate((JSTypedArrayObject) thisObj, JSRuntime.longToIntOrDouble(size));
            } else {
                return arraySpeciesCreate(thisObj, size);
            }
        }

        protected final JSTypedArrayObject typedArraySpeciesCreate(JSTypedArrayObject thisObj, Object... args) {
            var constr = speciesConstructor(thisObj, getDefaultConstructor(getRealm(), thisObj));
            return typedArrayCreate(constr, args);
        }

        protected final JSTypedArrayObject typedArrayCreateSameType(JSTypedArrayObject thisObj, Object... args) {
            var constr = getDefaultConstructor(getRealm(), thisObj);
            return typedArrayCreate(constr, args);
        }

        /**
         * 22.2.4.6 TypedArrayCreate().
         */
        public final JSTypedArrayObject typedArrayCreate(Object constr, Object... args) {
            Object newObject = construct(constr, args);
            if (!JSArrayBufferView.isJSArrayBufferView(newObject)) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferViewExpected();
            }
            JSTypedArrayObject newTypedArray = (JSTypedArrayObject) newObject;
            if (JSArrayBufferView.isOutOfBounds(newTypedArray, context)) {
                errorBranch.enter();
                throw Errors.createTypeErrorOutOfBoundsTypedArray();
            }
            if (args.length == 1 && JSRuntime.isNumber(args[0])) {
                if (newTypedArray.getLength() < JSRuntime.doubleValue((Number) args[0])) {
                    errorBranch.enter();
                    throw Errors.createTypeError("invalid TypedArray created");
                }
            }
            return newTypedArray;
        }

        /**
         * ES6, 9.4.2.3 ArraySpeciesCreate(originalArray, length).
         */
        protected final Object arraySpeciesCreate(Object originalArray, long length) {
            Object ctor = Undefined.instance;
            if (isArray(originalArray)) {
                arraySpeciesIsArray.enter();
                ctor = getConstructorProperty(originalArray);
                if (ctor instanceof JSObject) {
                    JSObject ctorObj = (JSObject) ctor;
                    if (JSFunction.isJSFunction(ctorObj) && JSFunction.isConstructor(ctorObj)) {
                        JSRealm thisRealm = getRealm();
                        JSRealm ctorRealm = JSFunction.getRealm((JSFunctionObject) ctorObj);
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
                    arraySpeciesGetSymbol.enter();
                    ctor = getSpeciesProperty(ctor);
                    ctor = ctor == Null.instance ? Undefined.instance : ctor;
                }
            }
            if (arraySpeciesEmpty.profile(ctor == Undefined.instance)) {
                return arrayCreate(length);
            }
            if (!isConstructorNode.executeBoolean(ctor)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAConstructor(ctor, context);
            }
            return construct(ctor, JSRuntime.longToIntOrDouble(length));
        }

        protected final boolean isArray(Object thisObj) {
            return isArrayNode.execute(thisObj);
        }

        private JSArrayObject arrayCreate(long length) {
            if (arrayCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayCreateNode = insert(ArrayCreateNode.create(context));
            }
            return arrayCreateNode.execute(length);
        }

        protected Object construct(Object constructor, Object... userArgs) {
            Object[] args = JSArguments.createInitial(JSFunction.CONSTRUCT, constructor, userArgs.length);
            System.arraycopy(userArgs, 0, args, JSArguments.RUNTIME_ARGUMENT_COUNT, userArgs.length);
            return constructorCall.executeCall(args);
        }

        protected static final JSFunctionObject getDefaultConstructor(JSRealm realm, JSTypedArrayObject thisObj) {
            TypedArray arrayType = JSArrayBufferView.typedArrayGetArrayType(thisObj);
            return realm.getArrayBufferViewConstructor(arrayType.getFactory());
        }

        /**
         * Implement 7.3.20 SpeciesConstructor.
         */
        protected final Object speciesConstructor(JSDynamicObject thisObj, JSDynamicObject defaultConstructor) {
            Object c = getConstructorProperty(thisObj);
            if (c == Undefined.instance) {
                defaultConstructorBranch.enter();
                return defaultConstructor;
            }
            if (!(c instanceof JSObject)) {
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
            return speciesConstructor;
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

        @Child private ArrayCreateNode arrayCreateNode;

        protected void setLength(Object thisObject, int length) {
            setLengthIntl(thisObject, length);
        }

        protected void setLength(Object thisObject, long length) {
            setLengthIntl(thisObject, JSRuntime.longToIntOrDouble(length));
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

        protected final JSObject createEmpty(Object thisObj, long length) {
            if (isTypedArrayImplementation) {
                // ValidateTypedArray already performed in the caller.
                return typedArrayCreateSameType((JSTypedArrayObject) thisObj, length);
            } else {
                return arrayCreate(length);
            }
        }

        protected final JSTypedArrayObject typedArrayCreateSameType(JSTypedArrayObject thisObj, long length) {
            return getArraySpeciesConstructorNode().typedArrayCreateSameType(thisObj, JSRuntime.longToIntOrDouble(length));
        }

        protected JSArrayObject arrayCreate(long length) {
            if (arrayCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayCreateNode = insert(ArrayCreateNode.create(getContext()));
            }
            return arrayCreateNode.execute(length);
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
        protected Object pushArrayNone(JSDynamicObject thisObject, @SuppressWarnings("unused") Object[] args) {
            long len = getLength(thisObject);
            setLength(thisObject, len); // could have side effect / throw
            if (len >= Integer.MAX_VALUE) {
                return (double) len;
            } else {
                return (int) len;
            }

        }

        @Specialization(guards = {"isJSArray(thisObject)", "args.length == 1"}, rewriteOn = SlowPathException.class)
        protected int pushArraySingle(JSDynamicObject thisObject, Object[] args) throws SlowPathException {
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
        protected double pushArraySingleLong(JSDynamicObject thisObject, Object[] args) {
            long len = getLength(thisObject);
            checkLength(args, len);
            write(thisObject, len, args[0]);
            long newLength = len + 1;
            setLength(thisObject, newLength);
            return newLength;
        }

        @Specialization(guards = {"isJSArray(thisObject)", "args.length >= 2"}, rewriteOn = SlowPathException.class)
        protected int pushArrayAll(JSDynamicObject thisObject, Object[] args) throws SlowPathException {
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
        protected double pushArrayAllLong(JSDynamicObject thisObject, Object[] args) {
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
                        @Cached InlinedConditionProfile lengthIsZero) {
            final Object thisObject = toObject(thisObj);
            final long length = getLength(thisObject);
            if (lengthIsZero.profile(this, length > 0)) {
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

        public abstract void executeVoid(Object target, long newLength);

        @NeverDefault
        protected final PropertySetNode createSetLengthProperty() {
            return PropertySetNode.create(JSArray.LENGTH, false, context, THROW_ERROR);
        }

        protected static boolean isArray(JSDynamicObject object) {
            // currently, must be fast array
            return JSArray.isJSFastArray(object);
        }

        @Specialization(guards = {"isArray(object)", "longIsRepresentableAsInt(longLength)"})
        protected static void setArrayLength(JSObject object, long longLength,
                        @Cached("createSetOrDelete(THROW_ERROR)") ArrayLengthWriteNode arrayLengthWriteNode) {
            arrayLengthWriteNode.executeVoid(object, (int) longLength);
        }

        @Specialization(guards = {"longIsRepresentableAsInt(longLength)"})
        protected static void setIntLength(JSObject object, long longLength,
                        @Shared @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode,
                        @Shared @Cached("createSetLengthProperty()") PropertySetNode setLengthProperty) {
            int intLength = (int) longLength;
            deletePropertyNode.executeEvaluated(object, intLength);
            setLengthProperty.setValueInt(object, intLength);
        }

        @Specialization(replaces = "setIntLength")
        protected void setLength(JSObject object, long longLength,
                        @Shared @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode,
                        @Shared @Cached("createSetLengthProperty()") PropertySetNode setLengthProperty,
                        @Cached(inline = true) LongToIntOrDoubleNode indexToNumber) {
            Object boxedLength = indexToNumber.fromIndex(this, longLength);
            deletePropertyNode.executeEvaluated(object, boxedLength);
            setLengthProperty.setValue(object, boxedLength);
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

        @Specialization
        protected Object sliceGeneric(Object thisObj, Object begin, Object end,
                        @Cached JSToIntegerAsLongNode toIntegerAsLong,
                        @Cached InlinedConditionProfile sizeIsZero,
                        @Cached InlinedConditionProfile offsetProfile1,
                        @Cached InlinedConditionProfile offsetProfile2) {
            Object thisArrayObj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(thisArrayObj);
            long startPos = begin != Undefined.instance ? JSRuntime.getOffset(toIntegerAsLong.executeLong(begin), len, this, offsetProfile1) : 0;

            long endPos;
            if (end == Undefined.instance) {
                endPos = len;
            } else {
                endPos = JSRuntime.getOffset(toIntegerAsLong.executeLong(end), len, this, offsetProfile2);
            }

            long size = startPos <= endPos ? endPos - startPos : 0;
            Object resultArray = getArraySpeciesConstructorNode().createEmptyContainer(thisArrayObj, size);
            if (sizeIsZero.profile(this, size > 0)) {
                if (isTypedArrayImplementation) {
                    checkOutOfBounds((JSTypedArrayObject) thisObj);
                    endPos = Math.min(endPos, getLength(thisArrayObj));
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

        protected static boolean isSparseArray(JSDynamicObject thisObj) {
            return arrayGetArrayType(thisObj) instanceof SparseArray;
        }

        protected static boolean isArrayWithoutHolesAndNotSealed(JSDynamicObject thisObj, IsArrayNode isArrayNode, TestArrayNode hasHolesNode, TestArrayNode isSealedNode) {
            boolean isArray = isArrayNode.execute(thisObj);
            return isArray && !hasHolesNode.executeBoolean(thisObj) && !isSealedNode.executeBoolean(thisObj);
        }

        @Specialization(guards = {"isArrayWithoutHolesAndNotSealed(thisObj, isArrayNode, hasHolesNode, isSealedNode)"})
        protected Object shiftWithoutHoles(JSDynamicObject thisObj,
                        @Shared @Cached("createIsArray()") @SuppressWarnings("unused") IsArrayNode isArrayNode,
                        @Shared @Cached("createHasHolesOrUnused()") @SuppressWarnings("unused") TestArrayNode hasHolesNode,
                        @Shared @Cached("createIsSealed()") @SuppressWarnings("unused") TestArrayNode isSealedNode,
                        @Cached InlinedExactClassProfile arrayTypeProfile,
                        @Shared @Cached InlinedConditionProfile lengthIsZero,
                        @Cached @Exclusive InlinedConditionProfile lengthLargerOne) {
            long len = getLength(thisObj);

            if (lengthIsZero.profile(this, len == 0)) {
                setLength(thisObj, 0);
                return Undefined.instance;
            } else {
                Object firstElement = read(thisObj, 0);
                if (lengthLargerOne.profile(this, len > 1)) {
                    ScriptArray array = arrayTypeProfile.profile(this, arrayGetArrayType(thisObj));
                    arraySetArrayType(thisObj, array.shiftRange(thisObj, 1));
                }
                setLength(thisObj, len - 1);
                return firstElement;
            }
        }

        protected static boolean isArrayWithHolesOrSealed(JSDynamicObject thisObj, IsArrayNode isArrayNode, TestArrayNode hasHolesNode, TestArrayNode isSealedNode) {
            boolean isArray = isArrayNode.execute(thisObj);
            return isArray && (hasHolesNode.executeBoolean(thisObj) || isSealedNode.executeBoolean(thisObj)) && !isSparseArray(thisObj);
        }

        @Specialization(guards = {"isArrayWithHolesOrSealed(thisObj, isArrayNode, hasHolesNode, isSealedNode)"})
        protected Object shiftWithHoles(JSDynamicObject thisObj,
                        @Shared @Cached("createIsArray()") @SuppressWarnings("unused") IsArrayNode isArrayNode,
                        @Shared @Cached("createHasHolesOrUnused()") @SuppressWarnings("unused") TestArrayNode hasHolesNode,
                        @Shared @Cached("createIsSealed()") @SuppressWarnings("unused") TestArrayNode isSealedNode,
                        @Shared @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode,
                        @Shared @Cached InlinedConditionProfile lengthIsZero) {
            long len = getLength(thisObj);
            if (lengthIsZero.profile(this, len > 0)) {
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
                setLength(thisObj, 0);
                return Undefined.instance;
            }
        }

        @Specialization(guards = {"isArrayNode.execute(thisObj)", "isSparseArray(thisObj)"})
        protected Object shiftSparse(JSDynamicObject thisObj,
                        @Shared @Cached("createIsArray()") @SuppressWarnings("unused") IsArrayNode isArrayNode,
                        @Shared @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode,
                        @Shared @Cached InlinedConditionProfile lengthIsZero,
                        @Cached("create(getContext())") JSArrayFirstElementIndexNode firstElementIndexNode,
                        @Cached("create(getContext())") JSArrayLastElementIndexNode lastElementIndexNode) {
            long len = getLength(thisObj);
            if (lengthIsZero.profile(this, len > 0)) {
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
                setLength(thisObj, 0);
                return Undefined.instance;
            }
        }

        @Specialization(guards = {"!isJSArray(thisObj)", "!isForeignObject(thisObj)"})
        protected Object shiftGeneric(Object thisObj,
                        @Shared @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode,
                        @Shared @Cached InlinedConditionProfile lengthIsZero) {
            Object thisJSObj = toObject(thisObj);
            long len = getLength(thisJSObj);

            if (lengthIsZero.profile(this, len == 0)) {
                setLength(thisJSObj, 0);
                return Undefined.instance;
            }

            Object firstObj = read(thisJSObj, 0);
            for (long i = 1; i < len; i++) {
                if (hasProperty(thisJSObj, i)) {
                    write(thisJSObj, i - 1, read(thisObj, i));
                } else {
                    deletePropertyNode.executeEvaluated(thisJSObj, i - 1);
                }
            }
            deletePropertyNode.executeEvaluated(thisJSObj, len - 1);
            setLength(thisJSObj, len - 1);
            reportLoopCount(len);
            return firstObj;
        }

        @InliningCutoff
        @Specialization(guards = {"isForeignObject(thisObj)"})
        protected Object shiftForeign(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary arrays,
                        @Shared @Cached InlinedConditionProfile lengthIsZero) {
            long len = JSInteropUtil.getArraySize(thisObj, arrays, this);
            if (lengthIsZero.profile(this, len == 0)) {
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
        @Child protected TestArrayNode hasHolesNode = TestArrayNode.createHasHolesOrUnused();

        protected boolean isFastPath(Object thisObj) {
            boolean isArray = isArrayNode.execute(thisObj);
            return isArray && !hasHolesNode.executeBoolean((JSDynamicObject) thisObj);
        }

        private long unshiftHoleless(JSDynamicObject thisObj, Object[] args) {
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
        protected int unshiftInt(JSDynamicObject thisObj, Object[] args) throws UnexpectedResultException {
            long newLen = unshiftHoleless(thisObj, args);
            if (JSRuntime.longIsRepresentableAsInt(newLen)) {
                return (int) newLen;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException((double) newLen);
            }
        }

        @Specialization(guards = "isFastPath(thisObj)", replaces = "unshiftInt")
        protected double unshiftDouble(JSDynamicObject thisObj, Object[] args) {
            return unshiftHoleless(thisObj, args);
        }

        @Specialization(guards = "!isFastPath(thisObjParam)")
        protected double unshiftHoles(Object thisObjParam, Object[] args,
                        @Cached("create(THROW_ERROR)") DeletePropertyNode deletePropertyNode,
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

        public JSArrayToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.joinPropertyNode = PropertyNode.createProperty(context, null, Strings.JOIN);
        }

        private Object getJoinProperty(Object target, Object receiver) {
            return joinPropertyNode.executeWithTarget(target, receiver);
        }

        private Object getToStringProperty(Object target) {
            if (toStringPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringPropertyNode = insert(PropertyNode.createProperty(getContext(), null, Strings.TO_STRING));
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

        private JSDynamicObject getForeignObjectPrototype(Object truffleObject) {
            assert JSRuntime.isForeignObject(truffleObject);
            if (foreignObjectPrototypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
            }
            return foreignObjectPrototypeNode.execute(truffleObject);
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
            if (shouldTryOwnJoin(arrayObj) && interop.isMemberInvocable(arrayObj, Strings.JOIN_JLS)) {
                Object result;
                try {
                    try {
                        result = interop.invokeMember(arrayObj, Strings.JOIN_JLS);
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

            Object join = getJoinProperty(getForeignObjectPrototype(arrayObj), arrayObj);
            if (isCallable(join)) {
                return callJoin(arrayObj, join);
            } else {
                Object toString = getToStringProperty(getRealm().getObjectPrototype());
                return callToString(arrayObj, toString);
            }
        }

        private boolean shouldTryOwnJoin(Object arrayObj) {
            // It is unlikely that an array-like object from a different language
            // has join() with the right semantics (namely, join() of ruby arrays
            // has a slightly different semantic). On the other hand, user-provided
            // objects like ProxyArray/ProxyObjects may have one.
            InteropLibrary interop = getInterop();
            try {
                return !interop.hasLanguage(arrayObj) || (interop.getLanguage(arrayObj) == getHostLanguageClass());
            } catch (UnsupportedMessageException umex) {
                throw CompilerDirectives.shouldNotReachHere();
            }
        }

        @CompilationFinal private Class<?> hostLanguageClass;

        private Class<?> getHostLanguageClass() {
            if (hostLanguageClass == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                try {
                    hostLanguageClass = InteropLibrary.getUncached().getLanguage(getRealm().getEnv().asGuestValue(new Object()));
                } catch (UnsupportedMessageException umex) {
                    throw CompilerDirectives.shouldNotReachHere();
                } catch (UnsupportedOperationException uoex) {
                    // Fallback: asGuestValue() is not supported in a spawned isolate.
                    hostLanguageClass = Object.class;
                }
            }
            return hostLanguageClass;
        }

        @Specialization
        protected Object toString(Object thisObj,
                        @Cached InlinedConditionProfile isJSObjectProfile) {
            Object arrayObj = toObject(thisObj);
            if (isJSObjectProfile.profile(this, JSObject.isJSObject(arrayObj))) {
                Object join = getJoinProperty(arrayObj, arrayObj);
                if (isCallable(join)) {
                    return callJoin(arrayObj, join);
                } else {
                    return JSObject.defaultToString((JSDynamicObject) arrayObj);
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
        @Child private JSArrayFirstElementIndexNode firstElementIndexNode;
        @Child private JSArrayLastElementIndexNode lastElementIndexNode;
        @Child private PropertyGetNode getSpreadableNode;
        @Child private JSIsArrayNode isArrayNode;

        private final ConditionProfile isFirstSpreadable = ConditionProfile.create();
        private final ConditionProfile hasFirstElements = ConditionProfile.create();
        private final ConditionProfile isSecondSpreadable = ConditionProfile.create();
        private final ConditionProfile hasSecondElements = ConditionProfile.create();
        private final ConditionProfile lengthErrorProfile = ConditionProfile.create();
        private final ConditionProfile hasMultipleArgs = ConditionProfile.create();
        private final ConditionProfile hasOneArg = ConditionProfile.create();
        private final ConditionProfile optimizationsObservable = ConditionProfile.create();
        private final ConditionProfile hasFirstOneElement = ConditionProfile.create();
        private final ConditionProfile hasSecondOneElement = ConditionProfile.create();

        protected boolean toBoolean(Object target) {
            if (toBooleanNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBooleanNode = insert(JSToBooleanNode.create());
            }
            return toBooleanNode.executeBoolean(target);
        }

        @Specialization
        protected Object concat(Object thisObj, Object[] args) {
            Object thisJSObj = toObject(thisObj);
            Object retObj = getArraySpeciesConstructorNode().createEmptyContainer(thisJSObj, 0);

            long n = concatElementIntl(retObj, thisJSObj, 0, isFirstSpreadable, hasFirstElements, hasFirstOneElement);
            long resultLen = concatIntl(retObj, n, args);

            // the last set element could be non-existent
            setLength(retObj, resultLen);
            return retObj;
        }

        private long concatIntl(Object retObj, long initialLength, Object[] args) {
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

        private long concatElementIntl(Object retObj, Object el, final long n, final ConditionProfile isSpreadable, final ConditionProfile hasElements,
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

        private long concatSpreadable(Object retObj, long n, Object elObj, long len2, final ConditionProfile hasOneElement) {
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
                long k = firstElementIndex((JSDynamicObject) elObj, len2);
                long lastI = lastElementIndex((JSDynamicObject) elObj, len2);
                for (; k <= lastI; k = nextElementIndex(elObj, k, len2)) {
                    writeOwn(retObj, n + k, read(elObj, k));
                }
            }
            reportLoopCount(len2);
            return n + len2;
        }

        // ES2015, 22.1.3.1.1
        private boolean isConcatSpreadable(Object el) {
            if (el instanceof JSObject) {
                JSObject obj = (JSObject) el;
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

        private long firstElementIndex(JSDynamicObject target, long length) {
            if (firstElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                firstElementIndexNode = insert(JSArrayFirstElementIndexNode.create(getContext()));
            }
            return firstElementIndexNode.executeLong(target, length);
        }

        private long lastElementIndex(JSDynamicObject target, long length) {
            if (lastElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                lastElementIndexNode = insert(JSArrayLastElementIndexNode.create(getContext()));
            }
            return lastElementIndexNode.executeLong(target, length);
        }
    }

    public abstract static class JSArrayIndexOfNode extends ArrayForEachIndexCallOperation {
        private final boolean isForward;

        public JSArrayIndexOfNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation, boolean isForward) {
            super(context, builtin, isTypedArrayImplementation);
            this.isForward = isForward;
        }

        @Specialization
        protected Object indexOf(Object thisObj, Object[] args,
                        @Cached InlinedBranchProfile arrayWithContentBranch,
                        @Cached JSToIntegerAsLongNode toInteger) {
            Object thisJSObject = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(thisJSObject);
            if (len == 0) {
                return -1;
            }
            arrayWithContentBranch.enter(this);
            Object searchElement = JSRuntime.getArgOrUndefined(args, 0);
            Object fromIndex = JSRuntime.getArgOrUndefined(args, 1);

            long fromIndexValue = isForward()
                            ? calcFromIndexForward(args, len, fromIndex, toInteger)
                            : calcFromIndexBackward(args, len, fromIndex, toInteger);
            if (fromIndexValue < 0) {
                return -1;
            }
            return forEachIndexCall(thisJSObject, Undefined.instance, searchElement, fromIndexValue, len, -1);
        }

        // for indexOf()
        private static long calcFromIndexForward(Object[] args, long len, Object fromIndex, JSToIntegerAsLongNode toInteger) {
            if (args.length <= 1) {
                return 0;
            } else {
                long fromIndexValue = toInteger.executeLong(fromIndex);
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
        private static long calcFromIndexBackward(Object[] args, long len, Object fromIndex, JSToIntegerAsLongNode toInteger) {
            if (args.length <= 1) {
                return len - 1;
            } else {
                long fromIndexInt = toInteger.executeLong(fromIndex);
                if (fromIndexInt >= 0) {
                    return Math.min(fromIndexInt, len - 1);
                } else {
                    return fromIndexInt + len;
                }
            }
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
                @Child protected LongToIntOrDoubleNode indexToNumber = LongToIntOrDoubleNode.create();

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    return doIdenticalNode.executeBoolean(value, callbackResult)
                                    ? MaybeResult.returnResult(boxIndex(index))
                                    : MaybeResult.continueResult(currentResult);
                }

                private Number boxIndex(long index) {
                    return indexToNumber.fromIndex(null, index);
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
        @Child private TruffleString.ConcatNode stringConcatNode;
        @Child private InteropLibrary interopLibrary;
        private final StringBuilderProfile stringBuilderProfile;

        @Child private TruffleStringBuilder.AppendStringNode appendStringNode;
        @Child private TruffleStringBuilder.ToStringNode builderToStringNode;

        public JSArrayJoinNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.elementToStringNode = JSToStringNode.create();
            this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
        }

        @Specialization
        protected TruffleString join(Object thisObj, Object joinStr,
                        @Cached InlinedConditionProfile separatorNotEmpty,
                        @Cached InlinedConditionProfile isZero,
                        @Cached InlinedConditionProfile isOne,
                        @Cached InlinedConditionProfile isTwo,
                        @Cached InlinedConditionProfile isSparse,
                        @Cached InlinedBranchProfile growProfile,
                        @Cached InlinedBranchProfile stackGrowProfile) {
            final Object thisJSObject = toObjectOrValidateTypedArray(thisObj);
            final long length = getLength(thisJSObject);
            final TruffleString joinSeparator = joinStr == Undefined.instance ? Strings.COMMA : getSeparatorToString().executeString(joinStr);

            JSRealm realm = getRealm();
            if (!realm.joinStackPush(thisObj, this, stackGrowProfile)) {
                // join is in progress on thisObj already => break the cycle
                return Strings.EMPTY_STRING;
            }
            try {
                if (isZero.profile(this, length == 0)) {
                    return Strings.EMPTY_STRING;
                } else if (isOne.profile(this, length == 1)) {
                    return joinOne(thisJSObject);
                } else {
                    final boolean appendSep = separatorNotEmpty.profile(this, Strings.length(joinSeparator) > 0);
                    if (isTwo.profile(this, length == 2)) {
                        return joinTwo(thisJSObject, joinSeparator, appendSep);
                    } else if (isSparse.profile(this, JSArray.isJSArray(thisJSObject) && arrayGetArrayType((JSDynamicObject) thisJSObject) instanceof SparseArray)) {
                        return joinSparse(thisJSObject, length, joinSeparator, appendSep, this, growProfile);
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

        private TruffleString concat(TruffleString a, TruffleString b) {
            if (stringConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringConcatNode = insert(TruffleString.ConcatNode.create());
            }
            return Strings.concat(stringConcatNode, a, b);
        }

        private void append(TruffleStringBuilderUTF16 sb, TruffleString s) {
            if (appendStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendStringNode = insert(TruffleStringBuilder.AppendStringNode.create());
            }
            stringBuilderProfile.append(appendStringNode, sb, s);
        }

        private TruffleString builderToString(TruffleStringBuilderUTF16 sb) {
            if (builderToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                builderToStringNode = insert(TruffleStringBuilder.ToStringNode.create());
            }
            return StringBuilderProfile.toString(builderToStringNode, sb);
        }

        private TruffleString joinOne(Object thisObject) {
            Object value = read(thisObject, 0);
            return toStringOrEmpty(value);
        }

        private TruffleString joinTwo(Object thisObject, final TruffleString joinSeparator, final boolean appendSep) {
            TruffleString first = toStringOrEmpty(read(thisObject, 0));
            TruffleString second = toStringOrEmpty(read(thisObject, 1));

            long resultLength = Strings.length(first) + (appendSep ? Strings.length(joinSeparator) : 0L) + Strings.length(second);
            if (resultLength > getContext().getStringLengthLimit()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }

            TruffleString res = first;
            if (appendSep) {
                res = concat(res, joinSeparator);
            }
            return concat(res, second);
        }

        private TruffleString joinLoop(Object thisJSObject, final long length, final TruffleString joinSeparator, final boolean appendSep) {
            var sb = stringBuilderProfile.newStringBuilder();
            long i = 0;
            while (i < length) {
                if (appendSep && i != 0) {
                    append(sb, joinSeparator);
                }
                Object value = read(thisJSObject, i);
                TruffleString str = toStringOrEmpty(value);
                append(sb, str);

                if (appendSep) {
                    i++;
                } else {
                    i = nextElementIndex(thisJSObject, i, length);
                }
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(length);
            return builderToString(sb);
        }

        private TruffleString toStringOrEmpty(Object value) {
            if (isValidEntry(value)) {
                return elementToStringNode.executeString(value);
            } else {
                return Strings.EMPTY_STRING;
            }
        }

        private boolean isValidEntry(Object value) {
            return value != Undefined.instance && value != Null.instance && !isForeignNull(value);
        }

        private boolean isForeignNull(Object value) {
            if (value instanceof JSDynamicObject) {
                return false;
            }
            if (value instanceof TruffleObject) {
                if (interopLibrary == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    interopLibrary = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                return interopLibrary.isNull(value);
            } else {
                return false;
            }
        }

        private TruffleString joinSparse(Object thisObject, long length, TruffleString joinSeparator, final boolean appendSep,
                        Node node, InlinedBranchProfile growProfile) {
            SimpleArrayList<Object> converted = SimpleArrayList.create(length);
            long calculatedLength = 0;
            long i = 0;
            while (i < length) {
                Object value = read(thisObject, i);
                if (isValidEntry(value)) {
                    TruffleString string = elementToStringNode.executeString(value);
                    int stringLength = Strings.length(string);
                    if (stringLength > 0) {
                        calculatedLength += stringLength;
                        converted.add(i, node, growProfile);
                        converted.add(string, node, growProfile);
                    }
                }
                i = nextElementIndex(thisObject, i, length);
                TruffleSafepoint.poll(this);
            }
            if (appendSep) {
                calculatedLength += (length - 1) * Strings.length(joinSeparator);
            }
            if (calculatedLength > getContext().getStringLengthLimit()) {
                errorBranch.enter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            assert calculatedLength <= Integer.MAX_VALUE;
            var sb = stringBuilderProfile.newStringBuilder((int) calculatedLength);
            int convertedSize = converted.size();
            long lastIndex = 0;
            for (int j = 0; j < convertedSize; j += 2) {
                long index = (long) converted.get(j);
                TruffleString value = (TruffleString) converted.get(j + 1);
                if (appendSep) {
                    for (long k = lastIndex; k < index; k++) {
                        append(sb, joinSeparator);
                    }
                }
                append(sb, value);
                lastIndex = index;
            }
            if (appendSep) {
                for (long k = lastIndex; k < length - 1; k++) {
                    append(sb, joinSeparator);
                }
                reportLoopCount(length);
            } else {
                reportLoopCount(convertedSize / 2);
            }
            assert StringBuilderProfile.length(sb) == calculatedLength;
            return builderToString(sb);
        }
    }

    public abstract static class JSArrayToLocaleStringNode extends JSArrayOperation {
        private final boolean passArguments;
        private final StringBuilderProfile stringBuilderProfile;
        @Child private PropertyGetNode getToLocaleStringNode;
        @Child private JSFunctionCallNode callToLocaleStringNode;

        public JSArrayToLocaleStringNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.passArguments = context.isOptionIntl402();
            this.stringBuilderProfile = StringBuilderProfile.create(context.getStringLengthLimit());
        }

        @Specialization
        protected TruffleString toLocaleString(VirtualFrame frame, Object thisObj,
                        @Cached JSToStringNode toStringNode,
                        @Cached TruffleStringBuilder.AppendCharUTF16Node appendCharNode,
                        @Cached TruffleStringBuilder.AppendStringNode appendStringNode,
                        @Cached TruffleStringBuilder.ToStringNode builderToStringNode,
                        @Cached InlinedBranchProfile stackGrowProfile) {
            Object arrayObj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(arrayObj);
            if (len == 0) {
                return Strings.EMPTY_STRING;
            }
            JSRealm realm = getRealm();
            if (!realm.joinStackPush(thisObj, this, stackGrowProfile)) {
                // join is in progress on thisObj already => break the cycle
                return Strings.EMPTY_STRING;
            }
            try {
                Object[] userArguments;
                if (passArguments) {
                    Object[] args = frame.getArguments();
                    int argc = JSArguments.getUserArgumentCount(args);
                    Object locales = (argc > 0) ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                    Object options = (argc > 1) ? JSArguments.getUserArgument(args, 1) : Undefined.instance;
                    userArguments = new Object[]{locales, options};
                } else {
                    userArguments = JSArguments.EMPTY_ARGUMENTS_ARRAY;
                }
                var sb = stringBuilderProfile.newStringBuilder();
                for (long k = 0; k < len; k++) {
                    if (k > 0) {
                        stringBuilderProfile.append(appendCharNode, sb, ',');
                    }
                    Object nextElement = read(arrayObj, k);
                    if (nextElement != Null.instance && nextElement != Undefined.instance) {
                        Object result = callToLocaleString(nextElement, userArguments);
                        TruffleString resultString = toStringNode.executeString(result);
                        stringBuilderProfile.append(appendStringNode, sb, resultString);
                    }
                    TruffleSafepoint.poll(this);
                }
                reportLoopCount(len);
                return StringBuilderProfile.toString(builderToStringNode, sb);
            } finally {
                realm.joinStackPop();
            }
        }

        private Object callToLocaleString(Object nextElement, Object[] userArguments) {
            if (getToLocaleStringNode == null || callToLocaleStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getToLocaleStringNode = insert(PropertyGetNode.create(Strings.TO_LOCALE_STRING, false, getContext()));
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
        private final BranchProfile needMoveDeleteBranch = BranchProfile.create();
        @Child private InteropLibrary arrayInterop;

        public JSArraySpliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.deletePropertyNode = DeletePropertyNode.create(THROW_ERROR);
        }

        @Specialization
        protected Object splice(Object thisArg, Object[] args,
                        @Cached SpliceJSArrayNode spliceJSArray,
                        @Cached InlinedBranchProfile branchDelete,
                        @Cached InlinedBranchProfile objectBranch,
                        @Cached InlinedConditionProfile argsLength0Profile,
                        @Cached InlinedConditionProfile argsLength1Profile,
                        @Cached InlinedConditionProfile offsetProfile,
                        @Cached InlinedBranchProfile needInsertBranch) {
            Object thisObj = toObject(thisArg);
            long len = getLength(thisObj);

            long actualStart = JSRuntime.getOffset(toIntegerAsLong(JSRuntime.getArgOrUndefined(args, 0)), len, this, offsetProfile);
            long insertCount;
            long actualDeleteCount;
            if (argsLength0Profile.profile(this, args.length == 0)) {
                insertCount = 0;
                actualDeleteCount = 0;
            } else if (argsLength1Profile.profile(this, args.length == 1)) {
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

            Object aObj = getArraySpeciesConstructorNode().createEmptyContainer(thisObj, actualDeleteCount);

            if (actualDeleteCount > 0) {
                // copy deleted elements into result array
                branchDelete.enter(this);
                spliceRead(thisObj, actualStart, actualDeleteCount, aObj, len);
            }
            setLength(aObj, actualDeleteCount);

            long itemCount = insertCount;
            boolean isJSArray = JSArray.isJSArray(thisObj);
            if (isJSArray) {
                JSArrayObject dynObj = (JSArrayObject) thisObj;
                ScriptArray arrayType = arrayGetArrayType(dynObj);
                spliceJSArray.execute(dynObj, len, actualStart, actualDeleteCount, itemCount, arrayType, this);
            } else if (JSDynamicObject.isJSDynamicObject(thisObj)) {
                objectBranch.enter(this);
                spliceJSObject(thisObj, len, actualStart, actualDeleteCount, itemCount);
            } else {
                spliceForeignArray(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }

            if (itemCount > 0) {
                needInsertBranch.enter(this);
                spliceInsert(thisObj, actualStart, args);
            }

            long newLength = len - actualDeleteCount + itemCount;
            setLength(thisObj, newLength);
            reportLoopCount(len);
            return aObj;
        }

        abstract static class SpliceJSArrayNode extends JavaScriptBaseNode {

            SpliceJSArrayNode() {
            }

            abstract void execute(JSDynamicObject array, long len, long actualStart, long actualDeleteCount, long itemCount, ScriptArray arrayType, JSArraySpliceNode parent);

            @Specialization(guards = {"cachedArrayType.isInstance(arrayType)"}, limit = "5")
            static void doCached(JSDynamicObject array, long len, long actualStart, long actualDeleteCount, long itemCount, ScriptArray arrayType, JSArraySpliceNode parent,
                            @Cached("arrayType") ScriptArray cachedArrayType,
                            @Bind("this") Node node,
                            @Cached @Shared GetPrototypeNode getPrototypeNode,
                            @Cached @Shared InlinedConditionProfile arrayElementwise) {
                if (arrayElementwise.profile(node, parent.mustUseElementwise(array, len, actualDeleteCount, itemCount, cachedArrayType.cast(arrayType), getPrototypeNode))) {
                    parent.spliceJSArrayElementwise(array, len, actualStart, actualDeleteCount, itemCount);
                } else {
                    parent.spliceJSArrayBlockwise(array, actualStart, actualDeleteCount, itemCount, cachedArrayType.cast(arrayType));
                }
            }

            @Specialization(replaces = "doCached")
            static void doUncached(JSDynamicObject array, long len, long actualStart, long actualDeleteCount, long itemCount, ScriptArray arrayType, JSArraySpliceNode parent,
                            @Bind("this") Node node,
                            @Cached @Shared GetPrototypeNode getPrototypeNode,
                            @Cached @Shared InlinedConditionProfile arrayElementwise) {
                if (arrayElementwise.profile(node, parent.mustUseElementwise(array, len, actualDeleteCount, itemCount, arrayType, getPrototypeNode))) {
                    parent.spliceJSArrayElementwise(array, len, actualStart, actualDeleteCount, itemCount);
                } else {
                    parent.spliceJSArrayBlockwise(array, actualStart, actualDeleteCount, itemCount, arrayType);
                }
            }
        }

        final boolean mustUseElementwise(JSDynamicObject obj, long expectedLength, long deleteCount, long itemCount, ScriptArray array, GetPrototypeNode getPrototypeNode) {
            return array instanceof SparseArray ||
                            array.isLengthNotWritable() ||
                            getPrototypeNode.execute(obj) != getRealm().getArrayPrototype() ||
                            !getContext().getArrayPrototypeNoElementsAssumption().isValid() ||
                            (!getContext().getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(obj)) ||
                            array.length(obj) != expectedLength ||
                            expectedLength + itemCount - deleteCount >= Integer.MAX_VALUE;
        }

        private void spliceRead(Object thisObj, long actualStart, long actualDeleteCount, Object aObj, long length) {
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

        final void spliceJSArrayElementwise(JSDynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            assert JSArray.isJSArray(thisObj); // contract
            if (itemCount < actualDeleteCount) {
                branchA.enter();
                spliceJSArrayElementwiseWalkUp(thisObj, len, actualStart, actualDeleteCount, itemCount);
            } else if (itemCount > actualDeleteCount) {
                branchB.enter();
                spliceJSArrayElementwiseWalkDown(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }
        }

        private void spliceJSArrayElementwiseWalkDown(JSDynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
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

        private void spliceJSArrayElementwiseWalkUp(JSDynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
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

        final void spliceJSArrayBlockwise(JSDynamicObject thisObj, long actualStart, long actualDeleteCount, long itemCount, ScriptArray array) {
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
            @Child private LongToIntOrDoubleNode indexToJSNumber = LongToIntOrDoubleNode.create();

            @Override
            public Object apply(long index, Object value, Object target, Object callback, Object callbackThisArg, Object currentResult) {
                return callNode.executeCall(JSArguments.create(callbackThisArg, callback, value, boxIndex(index), target));
            }

            protected final Number boxIndex(long index) {
                return indexToJSNumber.fromIndex(null, index);
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
        protected Object filter(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            Object resultArray;
            if (isTypedArrayImplementation) {
                resultArray = JSArray.createEmpty(getContext(), getRealm(), 0);
            } else {
                resultArray = getArraySpeciesConstructorNode().arraySpeciesCreate(thisJSObj, 0);
            }
            forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, new FilterState(resultArray, 0));

            if (isTypedArrayImplementation) {
                return getTypedResult((JSTypedArrayObject) thisJSObj, (JSArrayObject) resultArray);
            } else {
                return resultArray;
            }
        }

        private JSTypedArrayObject getTypedResult(JSTypedArrayObject thisJSObj, JSArrayObject resultArray) {
            long resultLen = arrayGetLength(resultArray);

            JSTypedArrayObject typedResult = getArraySpeciesConstructorNode().typedArraySpeciesCreate(thisJSObj, JSRuntime.longToIntOrDouble(resultLen));
            TypedArray typedArray = arrayTypeProfile.profile(JSArrayBufferView.typedArrayGetArrayType(typedResult));
            ScriptArray array = resultArrayTypeProfile.profile(arrayGetArrayType(resultArray));
            for (long i = 0; i < resultLen; i++) {
                typedArray.setElement(typedResult, i, array.getElement(resultArray, i), true);
                TruffleSafepoint.poll(this);
            }
            return typedResult;
        }

        static final class FilterState {
            final Object resultArray;
            long toIndex;

            FilterState(Object resultArray, long toIndex) {
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
                @Child private WriteElementNode writeOwnNode = WriteElementNode.create(getContext(), true, !isTypedArrayImplementation);

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
                JSDynamicObject resultArray = (JSDynamicObject) arguments[0];
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

        @NeverDefault
        public static FlattenIntoArrayNode create(JSContext context, boolean withCallback) {
            return FlattenIntoArrayNodeGen.create(context, withCallback);
        }

        protected abstract long executeLong(Object target, Object source, long sourceLen, long start, long depth, Object callback, Object thisArg);

        @Specialization
        protected long flatten(Object resultArray, Object source, long sourceLen, long start, long depth, Object callback, Object thisArg) {

            boolean callbackUndefined = callback == null;

            FlattenState flattenState = new FlattenState(resultArray, start, depth, callbackUndefined);
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
                toObjectNode = insert(JSToObjectNode.create());
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
            final Object resultArray;
            final boolean callbackUndefined;
            final long depth;
            long targetIndex;

            FlattenState(Object resultArray, long toIndex, long depth, boolean callbackUndefined) {
                this.resultArray = resultArray;
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

                private long makeFlattenCall(Object targetArray, Object element, long elementLength, long targetIndex, long depth) {
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
            return JSFunctionData.createCallOnly(context, new InnerFlattenCallNode(context, FlattenIntoArrayNode.create(context, false)).getCallTarget(), 0, Strings.EMPTY_STRING);
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
            flattenIntoArrayNode.flatten(resultArray, thisJSObj, length, 0, 1, callbackFn, thisArg);
            return resultArray;
        }

        @NeverDefault
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
            flattenIntoArrayNode.flatten(resultArray, thisJSObj, length, 0, depthNum, null, null);
            return resultArray;
        }

        private int toIntegerAsInt(Object depth) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerAsIntNode.create());
            }
            return toIntegerNode.executeInt(depth);
        }

        @NeverDefault
        protected static final FlattenIntoArrayNode createFlattenIntoArrayNode(JSContext context) {
            return FlattenIntoArrayNodeGen.create(context, false);
        }
    }

    public abstract static class JSArrayFindNode extends JSArrayOperation {
        @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();
        @Child private JSFunctionCallNode callNode = JSFunctionCallNode.createCall();
        private final boolean isLast; // Array.prototype.find() vs .findLast()

        public JSArrayFindNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation, boolean isLast) {
            super(context, builtin, isTypedArrayImplementation);
            this.isLast = isLast;
        }

        private Object callPredicate(Object function, Object target, Object value, double index, Object thisObj) {
            return callNode.executeCall(JSArguments.create(target, function, value, index, thisObj));
        }

        @Specialization
        protected Object find(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);
            long idx = isLast ? length - 1 : 0;
            while (isLast ? idx >= 0 : idx < length) {
                Object value = read(thisObj, idx);
                Object callbackResult = callPredicate(callbackFn, thisArg, value, idx, thisJSObj);
                boolean testResult = toBooleanNode.executeBoolean(callbackResult);
                if (testResult) {
                    reportLoopCount(isLast ? length - idx - 1 : idx);
                    return value;
                }
                idx = isLast ? idx - 1 : idx + 1;
            }
            reportLoopCount(length);
            return Undefined.instance;
        }
    }

    public abstract static class JSArrayFindIndexNode extends JSArrayOperation {
        @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();
        @Child private JSFunctionCallNode callNode = JSFunctionCallNode.createCall();
        private final boolean isLast; // Array.prototype.findIndex() vs .findLastIndex()

        public JSArrayFindIndexNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation, boolean isLast) {
            super(context, builtin, isTypedArrayImplementation);
            this.isLast = isLast;
        }

        private Object callPredicate(Object function, Object target, Object value, double index, Object thisObj) {
            return callNode.executeCall(JSArguments.create(target, function, value, index, thisObj));
        }

        @Specialization
        protected Object findIndex(Object thisObj, Object callback, Object thisArg) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);
            long idx = isLast ? length - 1 : 0;
            while (isLast ? idx >= 0 : idx < length) {
                Object value = read(thisObj, idx);
                Object callbackResult = callPredicate(callbackFn, thisArg, value, idx, thisJSObj);
                boolean testResult = toBooleanNode.executeBoolean(callbackResult);
                if (testResult) {
                    reportLoopCount(isLast ? length - idx - 1 : idx);
                    return JSRuntime.positiveLongToIntOrDouble(idx);
                }
                idx = isLast ? idx - 1 : idx + 1;
            }
            reportLoopCount(length);
            return -1;
        }
    }

    public abstract static class JSArrayToReversedNode extends JSArrayOperation {
        public JSArrayToReversedNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected Object reverse(final Object thisObj) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object result = createEmpty(thisJSObj, length);
            for (long i = 0; i < length; i++) {
                var value = read(thisJSObj, length - 1 - i);
                write(result, i, value);
            }
            reportLoopCount(length);
            return result;
        }
    }

    public abstract static class AbstractArraySortNode extends JSArrayOperation {

        public AbstractArraySortNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        protected final void checkCompareCallableOrUndefined(Object compare) {
            if (!(compare == Undefined.instance || isCallable(compare))) {
                errorBranch.enter();
                throw Errors.createTypeError("The comparison function must be either a function or undefined");
            }
        }
    }

    public abstract static class JSArrayToSortedNode extends AbstractArraySortNode {
        protected JSArrayToSortedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization
        protected final JSArrayObject toSorted(Object thisObj, Object compare,
                        @Cached JSToObjectArrayNode toObjectArray) {
            checkCompareCallableOrUndefined(compare);
            Object obj = toObject(thisObj);
            // Performs LengthOfArrayLike(obj), too.
            Object[] array = toObjectArray.executeObjectArray(obj);
            int length = array.length;
            /*
             * According to the spec, ArrayCreate should be performed before getting the elements,
             * but since ArrayCreate is side-effect-free (not considering out-of-memory errors) for
             * valid array lengths, it is alright to do it after.
             */
            JSArrayObject result = arrayCreate(length);

            Comparator<Object> comparator = compare == Undefined.instance
                            ? JSArray.DEFAULT_JSARRAY_COMPARATOR
                            : new SortComparator(compare);
            Boundaries.arraySort(array, comparator);

            for (int i = 0; i < length; i++) {
                write(result, i, array[i]);
            }
            reportLoopCount(length);
            return result;
        }
    }

    public abstract static class JSArraySortNode extends AbstractArraySortNode {

        @Child private DeletePropertyNode deletePropertyNode; // DeletePropertyOrThrow
        @Child private InteropLibrary interopNode;
        @Child private ImportValueNode importValueNode;
        private final ConditionProfile isSparse = ConditionProfile.create();
        private final BranchProfile hasCompareFnBranch = BranchProfile.create();
        private final BranchProfile noCompareFnBranch = BranchProfile.create();

        public JSArraySortNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin, false);
        }

        @Specialization(guards = {"isJSFastArray(thisObj)"}, assumptions = "getContext().getArrayPrototypeNoElementsAssumption()")
        protected JSArrayObject sortArray(JSArrayObject thisObj, Object compare,
                        @Cached JSArrayToDenseObjectArrayNode arrayToObjectArrayNode,
                        @Cached("create(getContext(), true)") JSArrayDeleteRangeNode arrayDeleteRangeNode) {
            checkCompareCallableOrUndefined(compare);
            long len = getLength(thisObj);

            if (len < 2) {
                // nothing to do
                return thisObj;
            }

            ScriptArray scriptArray = arrayGetArrayType(thisObj);
            Object[] array = arrayToObjectArrayNode.executeObjectArray(thisObj, scriptArray, len);

            sortAndWriteBack(array, thisObj, compare);

            if (isSparse.profile(array.length < len)) {
                arrayDeleteRangeNode.execute(thisObj, scriptArray, array.length, len);
            }
            return thisObj;
        }

        private void delete(Object obj, Object i) {
            if (deletePropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                deletePropertyNode = insert(DeletePropertyNode.create(true));
            }
            deletePropertyNode.executeEvaluated(obj, i);
        }

        @Specialization
        protected final Object sort(Object thisObj, final Object comparefn,
                        @Cached InlinedConditionProfile isJSObject,
                        @Cached InlinedBranchProfile growProfile) {
            checkCompareCallableOrUndefined(comparefn);
            Object obj = toObject(thisObj);
            if (isJSObject.profile(this, obj instanceof JSObject)) {
                return sortJSObject(comparefn, (JSObject) obj, growProfile);
            } else {
                return sortForeignObject(comparefn, obj);
            }
        }

        private JSDynamicObject sortJSObject(Object comparefn, JSObject thisJSObj, InlinedBranchProfile growProfile) {
            long len = getLength(thisJSObj);

            if (len == 0) {
                // nothing to do
                return thisJSObj;
            }

            Object[] array = jsobjectToArray(thisJSObj, len, true, this, growProfile);

            sortAndWriteBack(array, thisJSObj, comparefn);

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
                throw Errors.createRangeErrorInvalidArrayLength(this);
            }

            Object[] array = foreignArrayToObjectArray(thisObj, (int) len);

            sortAndWriteBack(array, thisObj, comparefn);

            return thisObj;
        }

        private void sortAndWriteBack(Object[] array, Object thisObj, Object comparefn) {
            Comparator<Object> comparator = getComparator(thisObj, comparefn);
            Boundaries.arraySort(array, comparator);

            for (int i = 0; i < array.length; i++) {
                write(thisObj, i, array[i]);
            }

            reportLoopCount(array.length); // best effort guess, let's not go for n*log(n)
        }

        @TruffleBoundary
        private static Object[] jsobjectToArray(JSDynamicObject thisObj, long len, boolean skipHoles, Node node, InlinedBranchProfile growProfile) {
            SimpleArrayList<Object> list = SimpleArrayList.create(len);
            for (long k = 0; k < len; k++) {
                if (!skipHoles || JSObject.hasProperty(thisObj, k)) {
                    list.add(JSObject.get(thisObj, k), node, growProfile);
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
                array[index] = JSInteropUtil.readArrayElementOrDefault(thisObj, index, Undefined.instance, interop, importValue);
                TruffleSafepoint.poll(this);
            }
            return array;
        }

        private Comparator<Object> getComparator(Object thisObj, Object compare) {
            if (compare == Undefined.instance) {
                noCompareFnBranch.enter();
                return getDefaultComparator(thisObj);
            } else {
                assert isCallable(compare);
                hasCompareFnBranch.enter();
                return new SortComparator(compare);
            }
        }

        private Comparator<Object> getDefaultComparator(Object thisObj) {
            return SortComparator.getDefaultComparator(getContext(), thisObj, isTypedArrayImplementation);
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
    }

    public abstract static class JSArrayReduceNode extends ArrayForEachIndexCallOperation {
        private final boolean isForward;

        public JSArrayReduceNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation, boolean isForward) {
            super(context, builtin, isTypedArrayImplementation);
            this.isForward = isForward;
        }

        @Child private ForEachIndexCallNode forEachIndexFindInitialNode;

        @Specialization
        protected Object reduce(Object thisObj, Object callback, Object[] initialValueOpt,
                        @Cached InlinedBranchProfile findInitialValueBranch) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long length = getLength(thisJSObj);
            Object callbackFn = checkCallbackIsFunction(callback);

            Object currentValue = initialValueOpt.length > 0 ? initialValueOpt[0] : null;
            long currentIndex = isForward() ? 0 : length - 1;
            if (currentValue == null) {
                findInitialValueBranch.enter(this);
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
                    return callNode.executeCall(JSArguments.create(callbackThisArg, callback, currentResult, value, boxIndex(index), target));
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

        public JSArrayFillNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected Object fill(Object thisObj, Object value, Object start, Object end,
                        @Cached InlinedConditionProfile offsetProfile1,
                        @Cached InlinedConditionProfile offsetProfile2) {
            Object thisJSObj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(thisJSObj);
            long lStart = JSRuntime.getOffset(toIntegerAsLong(start), len, this, offsetProfile1);
            long lEnd = end == Undefined.instance ? len : JSRuntime.getOffset(toIntegerAsLong(end), len, this, offsetProfile2);

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

        public JSArrayCopyWithinNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.deletePropertyNode = DeletePropertyNode.create(THROW_ERROR);
        }

        @Specialization
        protected Object copyWithin(Object thisObj, Object target, Object start, Object end,
                        @Cached InlinedConditionProfile offsetProfile1,
                        @Cached InlinedConditionProfile offsetProfile2,
                        @Cached InlinedConditionProfile offsetProfile3) {
            Object obj = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(obj);
            long to = JSRuntime.getOffset(toIntegerAsLong(target), len, this, offsetProfile1);
            long from = JSRuntime.getOffset(toIntegerAsLong(start), len, this, offsetProfile2);

            long finalIdx;
            if (end == Undefined.instance) {
                finalIdx = len;
            } else {
                finalIdx = JSRuntime.getOffset(toIntegerAsLong(end), len, this, offsetProfile3);
            }
            long count = Math.min(finalIdx - from, len - to);
            long expectedCount = count;
            if (count > 0) {
                if (isTypedArrayImplementation) {
                    checkOutOfBounds((JSTypedArrayObject) thisObj);
                    len = getLength(obj);
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
                        if (isTypedArrayImplementation && (from >= len || to >= len)) {
                            break;
                        }
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
        @Child private DeletePropertyNode deletePropertyNode;

        public JSArrayReverseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private boolean deleteProperty(Object array, long index) {
            if (deletePropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                deletePropertyNode = insert(DeletePropertyNode.create(true));
            }
            return deletePropertyNode.executeEvaluated(array, index);
        }

        @Specialization
        protected final Object reverseJSArray(JSArrayObject thisObj,
                        @Shared @Cached("createHasHolesOrUnused()") TestArrayNode hasHolesNode,
                        @Shared @Cached InlinedConditionProfile bothExistProfile,
                        @Shared @Cached InlinedConditionProfile onlyUpperExistsProfile,
                        @Shared @Cached InlinedConditionProfile onlyLowerExistsProfile) {
            return reverse(thisObj, true,
                            hasHolesNode, bothExistProfile, onlyUpperExistsProfile, onlyLowerExistsProfile);
        }

        @Specialization(replaces = "reverseJSArray")
        protected final Object reverseGeneric(Object thisObj,
                        @Shared @Cached("createHasHolesOrUnused()") TestArrayNode hasHolesNode,
                        @Shared @Cached InlinedConditionProfile bothExistProfile,
                        @Shared @Cached InlinedConditionProfile onlyUpperExistsProfile,
                        @Shared @Cached InlinedConditionProfile onlyLowerExistsProfile) {
            final Object array = toObject(thisObj);
            return reverse(array, JSArray.isJSArray(array),
                            hasHolesNode, bothExistProfile, onlyUpperExistsProfile, onlyLowerExistsProfile);
        }

        private Object reverse(Object array, boolean isArray,
                        TestArrayNode hasHolesNode,
                        InlinedConditionProfile bothExistProfile,
                        InlinedConditionProfile onlyUpperExistsProfile,
                        InlinedConditionProfile onlyLowerExistsProfile) {
            final long length = getLength(array);
            long lower = 0;
            long upper = length - 1;
            boolean hasHoles = isArray && hasHolesNode.executeBoolean((JSDynamicObject) array);

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

                if (bothExistProfile.profile(this, lowerExists && upperExists)) {
                    write(array, lower, upperValue);
                    write(array, upper, lowerValue);
                } else if (onlyUpperExistsProfile.profile(this, !lowerExists && upperExists)) {
                    write(array, lower, upperValue);
                    deleteProperty(array, upper);
                } else if (onlyLowerExistsProfile.profile(this, lowerExists && !upperExists)) {
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

    public abstract static class JSArrayIteratorNode extends JSBuiltinNode {
        private final int iterationKind;

        public JSArrayIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.iterationKind = iterationKind;
        }

        private JSObject createArrayIterator(Object thisObj) {
            return JSArrayIterator.create(getContext(), getRealm(), thisObj, 0L, iterationKind);
        }

        @Specialization
        protected final JSObject doJSObject(JSObject thisObj) {
            return createArrayIterator(thisObj);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected final JSObject doNotJSObject(Object thisObj,
                        @Cached JSToObjectNode toObjectNode) {
            return createArrayIterator(toObjectNode.execute(thisObj));
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

    public abstract static class JSArrayToSplicedNode extends JSArrayOperationWithToInt {

        @Child private InteropLibrary arrayInterop;

        @Child private ImportValueNode importValueNode;

        public JSArrayToSplicedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject toSpliced(Object thisArg, Object[] args,
                        @Cached InlinedConditionProfile offsetProfile,
                        @Cached InlinedConditionProfile argsLength0Profile,
                        @Cached InlinedConditionProfile argsLength1Profile,
                        @Cached InlinedBranchProfile objectBranch) {
            Object thisObj = toObject(thisArg);
            long len = getLength(thisObj);
            long actualStart = JSRuntime.getOffset(toIntegerAsLong(JSRuntime.getArgOrUndefined(args, 0)), len, this, offsetProfile);

            long insertCount;
            long actualDeleteCount;
            if (argsLength0Profile.profile(this, args.length == 0)) {
                insertCount = 0;
                actualDeleteCount = 0;
            } else if (argsLength1Profile.profile(this, args.length == 1)) {
                insertCount = 0;
                actualDeleteCount = len - actualStart;
            } else {
                assert args.length >= 2;
                insertCount = args.length - 2;
                long deleteCount = toIntegerAsLong(JSRuntime.getArgOrUndefined(args, 1));
                actualDeleteCount = Math.min(Math.max(deleteCount, 0), len - actualStart);
            }

            long newLen = len + insertCount - actualDeleteCount;
            if (newLen > JSRuntime.MAX_SAFE_INTEGER_LONG) {
                errorBranch.enter();
                throwLengthError();
            }

            JSArrayObject resObj = getArraySpeciesConstructorNode().arrayCreate(newLen);

            if (JSDynamicObject.isJSDynamicObject(thisObj)) {
                objectBranch.enter(this);
                spliceJSObject(resObj, thisObj, len, actualStart, actualDeleteCount, args);
            } else {
                spliceForeignArray(resObj, thisObj, len, actualStart, actualDeleteCount, args);
            }

            reportLoopCount(len);
            return resObj;
        }

        private long spliceInsert(JSDynamicObject dstObj, long toIndex, Object[] args) {
            int itemOffset = 2; // toSpliced(start, deleteCount, ...args)
            long dstIdx = toIndex;
            for (int argIdx = itemOffset; argIdx < args.length; argIdx++) {
                writeOwn(dstObj, dstIdx++, args[argIdx]);
            }
            return dstIdx;
        }

        private void spliceJSObject(JSArrayObject dstObj, Object srcObj, long len, long actualStart, long actualDeleteCount, Object[] args) {
            long dstIdx = 0;
            for (long srcIdx = 0; srcIdx < actualStart; srcIdx++) {
                writeOwn(dstObj, dstIdx++, read(srcObj, srcIdx));
            }
            dstIdx = spliceInsert(dstObj, dstIdx, args);
            for (long srcIdx = actualStart + actualDeleteCount; srcIdx < len; srcIdx++) {
                writeOwn(dstObj, dstIdx++, read(srcObj, srcIdx));
            }
        }

        private void spliceForeignArray(JSArrayObject dstObj, Object srcObj, long len, long actualStart, long actualDeleteCount, Object[] args) {
            InteropLibrary arrays = arrayInterop;
            if (arrays == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.arrayInterop = arrays = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            long dstIdx = 0;
            for (long srcIdx = 0; srcIdx < actualStart; srcIdx++) {
                spliceForeignMoveValue(dstObj, srcObj, srcIdx, dstIdx++, arrays);
            }
            dstIdx = spliceInsert(dstObj, dstIdx, args);
            for (long srcIdx = actualStart + actualDeleteCount; srcIdx < len; srcIdx++) {
                spliceForeignMoveValue(dstObj, srcObj, srcIdx, dstIdx++, arrays);
            }
        }

        private void spliceForeignMoveValue(JSArrayObject destObj, Object srcObj, long fromIndex, long toIndex, InteropLibrary arrays) {
            if (importValueNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                importValueNode = insert(ImportValueNode.create());
            }
            Object val = JSInteropUtil.readArrayElementOrDefault(srcObj, fromIndex, Undefined.instance, arrays, importValueNode);
            writeOwn(destObj, toIndex, val);
        }
    }

    public abstract static class JSArrayWithNode extends JSArrayOperationWithToInt {

        @Child private JSToNumberNode toNumberNode;
        @Child private JSToBigIntNode toBigIntNode;

        public JSArrayWithNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        protected Object toNumber(Object value) {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode.execute(value);
        }

        protected Object toBigInt(Object value) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            return toBigIntNode.execute(value);
        }

        @Specialization
        protected Object withGeneric(Object thisObj, Object index, Object valueParam) {
            Object value = valueParam;
            final Object array = toObjectOrValidateTypedArray(thisObj);
            long len = getLength(array);
            long relativeIndex = toIntegerAsLong(index);
            long actualIndex;
            if (relativeIndex >= 0) {
                actualIndex = relativeIndex;
            } else {
                actualIndex = len + relativeIndex;
            }

            if (isTypedArrayImplementation) {
                if (JSArrayBufferView.isBigIntArrayBufferView((JSDynamicObject) array)) {
                    value = toBigInt(value);
                } else {
                    value = toNumber(value);
                }
            }

            long lengthForCheck = isTypedArrayImplementation ? (JSArrayBufferView.isOutOfBounds((JSTypedArrayObject) array, getContext()) ? 0 : getLength(thisObj)) : len;
            if (actualIndex >= lengthForCheck || actualIndex < 0) {
                errorBranch.enter();
                throw Errors.createRangeError("invalid index");
            }
            JSDynamicObject resultArray = createEmpty(array, len);

            long k;
            for (k = 0; k < len; k++) {
                Object val;
                if (k == actualIndex) {
                    val = value;
                } else {
                    val = read(array, k);
                }

                writeOwn(resultArray, k, val);
                TruffleSafepoint.poll(this);
            }
            reportLoopCount(k);
            return resultArray;
        }
    }
}
