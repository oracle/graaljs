/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.DeleteAndSetLengthNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayConcatNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayCopyWithinNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayEveryNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFillNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFilterNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFindIndexNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayFindNodeGen;
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
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToStringNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayUnshiftNodeGen;
import com.oracle.truffle.js.builtins.DebugBuiltins.DebugIsHolesArrayNode;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugIsHolesArrayNodeGen;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ArrayCreateNode;
import com.oracle.truffle.js.nodes.access.ArrayLengthNode.ArrayLengthWriteNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.CallbackNode;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResult;
import com.oracle.truffle.js.nodes.access.ForEachIndexCallNode.MaybeResultNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode.IsArrayWrappedNode;
import com.oracle.truffle.js.nodes.access.JSArrayFirstElementIndexNode;
import com.oracle.truffle.js.nodes.access.JSArrayLastElementIndexNode;
import com.oracle.truffle.js.nodes.access.JSArrayNextElementIndexNode;
import com.oracle.truffle.js.nodes.access.JSArrayPreviousElementIndexNode;
import com.oracle.truffle.js.nodes.access.JSGetLengthNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.JSSetLengthNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerSpecialNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
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
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSlowArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DelimitedStringBuilder;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * Contains builtins for {@linkplain JSArray}.prototype.
 */
public final class ArrayPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ArrayPrototypeBuiltins.ArrayPrototype> {
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

        // ES6
        find(1),
        findIndex(1),
        fill(1),
        copyWithin(2),
        keys(0),
        values(0),
        entries(0),

        // ES7
        includes(1);

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
                return 6;
            } else if (this == includes) {
                return 7;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ArrayPrototype builtinEnum) {
        switch (builtinEnum) {
            case push:
                return JSArrayPushNode.create(context, builtin, args().withThis().varArgs().createArgumentNodes(context));
            case pop:
                return JSArrayPopNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case slice:
                return JSArraySliceNodeGen.create(context, builtin, false, args().withThis().varArgs().createArgumentNodes(context));
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
        private final ValueProfile typedArrayTypeProfile;

        public BasicArrayOperation(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin);
            this.isTypedArrayImplementation = isTypedArrayImplementation;
            this.typedArrayTypeProfile = isTypedArrayImplementation ? ValueProfile.createClassProfile() : null;
        }

        public BasicArrayOperation(JSContext context, JSBuiltin builtin) {
            this(context, builtin, false);
        }

        protected final TruffleObject toObject(Object target) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return toObjectNode.executeTruffleObject(target);
        }

        protected long getLength(TruffleObject thisObject) {
            if (isTypedArrayImplementation) {
                // %TypedArray%.prototype.* don't access the "length" property
                if (!JSArrayBufferView.isJSArrayBufferView(thisObject)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("typed array expected");
                }
                DynamicObject dynObj = (DynamicObject) thisObject;
                if (JSArrayBufferView.hasDetachedBuffer(dynObj, getContext())) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorDetachedBuffer();
                }
                TypedArray typedArray = typedArrayTypeProfile.profile(JSArrayBufferView.typedArrayGetArrayType(dynObj));
                return typedArray.length(dynObj);
            } else {
                if (getLengthNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getLengthNode = insert(JSGetLengthNode.create(getContext()));
                }
                return getLengthNode.executeLong(thisObject);
            }
        }

        protected final boolean isCallable(Object callback) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = IsCallableNode.create();
            }
            return isCallableNode.executeBoolean(callback);
        }

        protected final DynamicObject checkCallbackIsFunction(Object callback) {
            if (!isCallable(callback)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAFunction(callback, this);
            }
            return (DynamicObject) callback;
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
        protected final void validateTypedArray(Object obj) {
            if (!JSArrayBufferView.isJSArrayBufferView(obj)) {
                errorBranch.enter();
                throw Errors.createTypeErrorArrayBufferViewExpected();
            }
            if (JSArrayBufferView.hasDetachedBuffer((DynamicObject) obj, getContext())) {
                errorBranch.enter();
                throw Errors.createTypeErrorDetachedBuffer();
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

        protected final Object createEmptyContainer(TruffleObject thisObj, long size) {
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
        protected final Object arraySpeciesCreate(TruffleObject originalArray, long length) {
            Object ctor = Undefined.instance;
            if (isArray(originalArray)) {
                arraySpeciesIsArray.enter();
                ctor = getConstructorProperty(originalArray);
                if (JSObject.isJSObject(ctor)) {
                    DynamicObject ctorObj = (DynamicObject) ctor;
                    if (JSFunction.isJSFunction(ctorObj) && JSFunction.isConstructor(ctorObj)) {
                        JSRealm thisRealm = context.getRealm();
                        JSRealm ctorRealm = JSFunction.getRealm(ctorObj);
                        if (thisRealm != ctorRealm) {
                            differentRealm.enter();
                            if (ctorRealm.getArrayConstructor().getFunctionObject() == ctor) {
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
                throw Errors.createTypeErrorConstructorExpected();
            }
            return construct((DynamicObject) ctor, JSRuntime.longToIntOrDouble(length));
        }

        protected final boolean isArray(TruffleObject thisObj) {
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
            JSConstructor constr = context.getRealm().getArrayBufferViewConstructor(arrayType.getFactory());
            return constr.getFunctionObject();
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
            if (!JSObject.isJSObject(c)) {
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
                throw Errors.createTypeErrorConstructorExpected();
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

        protected void setLength(TruffleObject thisObject, int length) {
            setLengthIntl(thisObject, length);
        }

        protected void setLength(TruffleObject thisObject, long length) {
            setLengthIntl(thisObject, JSRuntime.longToIntOrDouble(length));
        }

        protected void setLength(TruffleObject thisObject, double length) {
            setLengthIntl(thisObject, length);
        }

        private void setLengthIntl(TruffleObject thisObject, Object length) {
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
                readNode = insert(NodeFactory.getInstance(getContext()).createReadElementNode(getContext(), null, null));
            }
            return readNode;
        }

        protected Object read(Object target, int index) {
            return getOrCreateReadNode().executeWithTargetAndIndex(target, index);
        }

        protected Object readAny(Object target, Object index) {
            return getOrCreateReadNode().executeWithTargetAndIndex(target, index);
        }

        protected Object read(Object target, double index) {
            return getOrCreateReadNode().executeWithTargetAndIndex(target, index);
        }

        private WriteElementNode getOrCreateWriteNode() {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                NodeFactory factory = NodeFactory.getInstance(getContext());
                writeNode = insert(factory.createWriteElementNode(getContext(), THROW_ERROR));
            }
            return writeNode;
        }

        protected void write(Object target, int index, Object value) {
            getOrCreateWriteNode().executeWithTargetAndIndexAndValue(target, index, value);
        }

        protected void write(Object target, long index, Object value) {
            getOrCreateWriteNode().executeWithTargetAndIndexAndValue(target, (double) index, value);
        }

        protected void write(Object target, Object index, Object value) {
            assert index instanceof String || index instanceof Symbol;
            getOrCreateWriteNode().executeWithTargetAndIndexAndValue(target, index, value);
        }

        // represent the 7.3.6 CreateDataPropertyOrThrow(O, P, V)
        private WriteElementNode getOrCreateWriteOwnNode() {
            if (writeOwnNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                NodeFactory factory = NodeFactory.getInstance(getContext());
                writeOwnNode = insert(factory.createWriteElementNode(getContext(), THROW_ERROR, true));
            }
            return writeOwnNode;
        }

        protected void writeOwn(Object target, int index, Object value) {
            getOrCreateWriteOwnNode().executeWithTargetAndIndexAndValue(target, index, value);
        }

        protected void writeOwn(Object target, long index, Object value) {
            getOrCreateWriteOwnNode().executeWithTargetAndIndexAndValue(target, (double) index, value);
        }

        protected void writeOwn(Object target, String index, Object value) {
            getOrCreateWriteOwnNode().executeWithTargetAndIndexAndValue(target, index, value);
        }

        private JSHasPropertyNode getOrCreateHasPropertyNode() {
            if (hasPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasPropertyNode = insert(JSHasPropertyNode.create());
            }
            return hasPropertyNode;
        }

        protected boolean hasProperty(TruffleObject target, long propertyIdx) {
            return getOrCreateHasPropertyNode().executeBoolean(target, propertyIdx);
        }

        protected boolean hasProperty(TruffleObject target, Object propertyName) {
            return getOrCreateHasPropertyNode().executeBoolean(target, propertyName);
        }

        protected long nextElementIndex(TruffleObject target, long currentIndex, long length) {
            if (nextElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nextElementIndexNode = insert(JSArrayNextElementIndexNode.create(getContext()));
            }
            return nextElementIndexNode.executeLong(target, currentIndex, length);
        }

        protected long previousElementIndex(TruffleObject target, long currentIndex) {
            if (previousElementIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                previousElementIndexNode = insert(JSArrayPreviousElementIndexNode.create(getContext()));
            }
            return previousElementIndexNode.executeLong(target, currentIndex);
        }

        protected boolean isArrayWithHoles(DynamicObject thisObj, ValueProfile arrayTypeProfile) {
            return arrayTypeProfile.profile(arrayGetArrayType(thisObj)).hasHoles(thisObj);
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

        @Child private JSToIntegerSpecialNode toIntegerSpecialNode;

        protected long toIntegerSpecial(Object target) {
            if (toIntegerSpecialNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerSpecialNode = insert(JSToIntegerSpecialNode.create());
            }
            return toIntegerSpecialNode.executeLong(target);
        }
    }

    public abstract static class JSArrayPushNode extends JSArrayOperation {
        public JSArrayPushNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public static JSArrayPushNode create(JSContext context, JSBuiltin builtin, JavaScriptNode[] args) {
            assert args.length == 2;
            JavaScriptNode[] fullArgs = Arrays.copyOf(args, args.length + 1);
            fullArgs[args.length] = IsArrayWrappedNode.createIsArray(args[0]);
            return JSArrayPushNodeGen.create(context, builtin, fullArgs);
        }

        @Specialization(guards = {"isArray", "args.length == 0"})
        protected Object pushArrayNone(DynamicObject thisObject, @SuppressWarnings("unused") Object[] args, @SuppressWarnings("unused") boolean isArray) {
            assert JSArray.isJSArray(thisObject);
            long len = getLength(thisObject);
            setLength(thisObject, len); // could have side effect / throw
            if (len >= Integer.MAX_VALUE) {
                return (double) len;
            } else {
                return (int) len;
            }

        }

        @Specialization(guards = {"isArray", "args.length == 1"}, rewriteOn = SlowPathException.class)
        protected int pushArraySingle(DynamicObject thisObject, Object[] args, @SuppressWarnings("unused") boolean isArray) throws SlowPathException {
            assert JSArray.isJSArray(thisObject);
            long len = getLength(thisObject);
            if (len >= Integer.MAX_VALUE) {
                throw new SlowPathException();
            }
            int iLen = (int) len;
            write(thisObject, iLen, args[0]);
            int newLength = iLen + 1;
            setLength(thisObject, newLength);
            return newLength;
        }

        @Specialization(guards = {"isArray", "args.length == 1"})
        protected double pushArraySingleLong(DynamicObject thisObject, Object[] args, @SuppressWarnings("unused") boolean isArray) {
            assert JSArray.isJSArray(thisObject);
            long len = getLength(thisObject);
            checkLength(args, len);
            write(thisObject, len, args[0]);
            long newLength = len + 1;
            setLength(thisObject, newLength);
            return newLength;
        }

        @Specialization(guards = {"isArray", "args.length >= 2"}, rewriteOn = SlowPathException.class)
        protected int pushArrayAll(DynamicObject thisObject, Object[] args, @SuppressWarnings("unused") boolean isArray) throws SlowPathException {
            assert JSArray.isJSArray(thisObject);
            long len = getLength(thisObject);
            if (len + args.length >= Integer.MAX_VALUE) {
                throw new SlowPathException();
            }
            int ilen = (int) len;
            for (int i = 0; i < args.length; i++) {
                write(thisObject, ilen + i, args[i]);
            }
            setLength(thisObject, ilen + args.length);
            return ilen + args.length;
        }

        @Specialization(guards = {"isArray", "args.length >= 2"})
        protected double pushArrayAllLong(DynamicObject thisObject, Object[] args, @SuppressWarnings("unused") boolean isArray) {
            assert JSArray.isJSArray(thisObject);
            long len = getLength(thisObject);
            checkLength(args, len);
            for (int i = 0; i < args.length; i++) {
                write(thisObject, len + i, args[i]);
            }
            setLength(thisObject, len + args.length);
            return (double) len + args.length;
        }

        @Specialization(guards = "!isArray")
        protected double pushProperty(Object thisObject, Object[] args, @SuppressWarnings("unused") boolean isArray) {
            assert !JSArray.isJSArray(thisObject);
            TruffleObject thisObj = toObject(thisObject);
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
        protected Object pop(Object thisObj,
                        @Cached("create(getContext())") DeleteAndSetLengthNode deleteAndSetLength,
                        @Cached("createBinaryProfile()") ConditionProfile lengthIsZero,
                        @Cached("createBinaryProfile()") ConditionProfile indexInIntRangeCondition) {
            final TruffleObject thisObject = toObject(thisObj);
            final long length = getLength(thisObject);
            if (lengthIsZero.profile(length > 0)) {
                long newLength = length - 1;
                Object boxedIndex = JSRuntime.boxIndex(newLength, indexInIntRangeCondition);
                Object result = readAny(thisObject, boxedIndex);
                deleteAndSetLength.execute(thisObject, boxedIndex);
                return result;
            } else {
                assert length == 0;
                setLength(thisObject, 0);
                return Undefined.instance;
            }
        }
    }

    @ImportStatic(JSGuards.class)
    protected abstract static class DeleteAndSetLengthNode extends JavaScriptBaseNode {
        protected static final boolean THROW_ERROR = true;  // DeletePropertyOrThrow

        protected final JSContext context;

        protected DeleteAndSetLengthNode(JSContext context) {
            this.context = context;
        }

        public static DeleteAndSetLengthNode create(JSContext context) {
            return DeleteAndSetLengthNodeGen.create(context);
        }

        public abstract Object execute(TruffleObject target, Object value);

        protected final WritePropertyNode createWritePropertyNode() {
            return NodeFactory.getInstance(context).createWriteProperty(null, JSArray.LENGTH, null, context, THROW_ERROR);
        }

        protected static boolean isArray(DynamicObject object) {
            // currently, must be fast array
            return JSArray.isJSFastArray(object);
        }

        @Specialization(guards = "isArray(object)")
        protected static int setArrayLength(DynamicObject object, int length,
                        @Cached("createArrayLengthWriteNode()") ArrayLengthWriteNode arrayLengthWriteNode) {
            arrayLengthWriteNode.executeVoid(object, length, isArray(object));
            return length;
        }

        protected static final ArrayLengthWriteNode createArrayLengthWriteNode() {
            return ArrayLengthWriteNode.createSetOrDelete(THROW_ERROR);
        }

        @Specialization
        protected static int setIntLength(DynamicObject object, int length,
                        @Cached("create(THROW_ERROR, context)") DeletePropertyNode deletePropertyNode,
                        @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty) {
            deletePropertyNode.executeEvaluated(object, length);
            setLengthProperty.executeIntWithValue(object, length);
            return length;
        }

        @Specialization(replaces = "setIntLength")
        protected static Object setLength(DynamicObject object, Object length,
                        @Cached("create(THROW_ERROR, context)") DeletePropertyNode deletePropertyNode,
                        @Cached("createWritePropertyNode()") WritePropertyNode setLengthProperty) {
            deletePropertyNode.executeEvaluated(object, length);
            setLengthProperty.executeWithValue(object, length);
            return length;
        }

        @Specialization(guards = "!isDynamicObject(object)")
        protected static Object setLength(TruffleObject object, Object length,
                        @Cached("create(THROW_ERROR, context)") DeletePropertyNode deletePropertyNode) {
            deletePropertyNode.executeEvaluated(object, length);
            // No SET_SIZE in interop
            return length;
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
        protected Object slice(Object thisObj, Object[] args,
                        @Cached("create()") JSToIntegerSpecialNode toIntegerSpecial) {
            TruffleObject thisArrayObj = toObject(thisObj);
            long len = getLength(thisArrayObj);
            long startPos = args.length > 0 ? JSRuntime.getOffset(toIntegerSpecial.executeLong(args[0]), len, offsetProfile1) : 0;

            long endPos;
            if (args.length <= 1 || args[1] == Undefined.instance) {
                endPos = len;
            } else {
                endPos = JSRuntime.getOffset(toIntegerSpecial.executeLong(args[1]), len, offsetProfile2);
            }

            long size = startPos <= endPos ? endPos - startPos : 0;
            TruffleObject resultArray = (TruffleObject) getArraySpeciesConstructorNode().createEmptyContainer(thisArrayObj, size);
            if (sizeIsZero.profile(size > 0)) {
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

                @Child private WriteElementNode writeOwnNode = NodeFactory.getInstance(getContext()).createWriteElementNode(getContext(), true, true);

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

    public abstract static class JSArrayShiftNode extends JSArrayOperation {
        public JSArrayShiftNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final ConditionProfile lengthIsZero = ConditionProfile.createBinaryProfile();
        protected final ValueProfile arrayTypeProfile = ValueProfile.createClassProfile();

        protected static boolean isSparseArray(DynamicObject thisObj) {
            return arrayGetArrayType(thisObj) instanceof SparseArray;
        }

        @Specialization(guards = {"isJSArray(thisObj)", "!isSparseArray(thisObj)", "!isArrayWithHoles(thisObj, arrayTypeProfile)"})
        protected Object shift(DynamicObject thisObj) {
            long len = getLength(thisObj);
            if (lengthIsZero.profile(len > 0)) {
                Object firstElement = read(thisObj, 0);
                ScriptArray array = arrayTypeProfile.profile(arrayGetArrayType(thisObj));
                arraySetArrayType(thisObj, array.removeRange(thisObj, 0, 1, errorBranch));
                setLength(thisObj, len - 1);
                return firstElement;
            } else {
                return Undefined.instance;
            }
        }

        @Specialization(guards = {"isJSArray(thisObj)", "!isSparseArray(thisObj)", "isArrayWithHoles(thisObj, arrayTypeProfile)"})
        protected Object shiftWithHoles(DynamicObject thisObj,
                        @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deletePropertyNode) {
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
                return firstElement;
            } else {
                return Undefined.instance;
            }
        }

        @Specialization(guards = {"isJSArray(thisObj)", "isSparseArray(thisObj)"})
        protected Object shiftSparse(DynamicObject thisObj,
                        @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deletePropertyNode,
                        @Cached("create(getContext())") JSArrayFirstElementIndexNode firstElementIndexNode,
                        @Cached("create(getContext())") JSArrayLastElementIndexNode lastElementIndexNode) {
            long len = getLength(thisObj);
            if (lengthIsZero.profile(len > 0)) {
                Object firstElement = read(thisObj, 0);
                for (long i = firstElementIndexNode.executeLong(thisObj, len); i <= lastElementIndexNode.executeLong(thisObj, len); i = nextElementIndex(thisObj, i, len)) {
                    if (i > 0) {
                        write(thisObj, i - 1, read(thisObj, i));
                    }
                    if (!hasProperty(thisObj, i + 1)) {
                        deletePropertyNode.executeEvaluated(thisObj, i);
                    }
                }
                setLength(thisObj, len - 1);
                return firstElement;
            } else {
                return Undefined.instance;
            }
        }

        @Specialization(guards = "!isJSArray(thisObj)")
        protected Object shift(Object thisObj,
                        @Cached("createNonStrict(getContext())") DeletePropertyNode deleteNode) {
            TruffleObject thisJSObj = toObject(thisObj);
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
            return firstObj;
        }
    }

    public abstract static class JSArrayUnshiftNode extends JSArrayOperation {
        public JSArrayUnshiftNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final ValueProfile arrayTypeProfile = ValueProfile.createClassProfile();

        protected boolean isFastPath(Object thisObj) {
            return JSArray.isJSArray(thisObj) && !isArrayWithHoles((DynamicObject) thisObj, arrayTypeProfile);
        }

        @Specialization(guards = "isFastPath(thisObj)")
        protected double unshift(DynamicObject thisObj, Object[] args) {
            long len = getLength(thisObj);
            if (getContext().getEcmaScriptVersion() <= 5 || args.length > 0) {
                for (long i = len - 1; i >= 0; i--) {
                    write(thisObj, i + args.length, read(thisObj, i));
                }
                for (int i = 0; i < args.length; i++) {
                    write(thisObj, i, args[i]);
                }
            }
            setLength(thisObj, len + args.length);
            return len + args.length;
        }

        @Specialization(guards = "!isFastPath(thisObjParam)")
        protected double unshiftHoles(Object thisObjParam, Object[] args,
                        @Cached("create(THROW_ERROR, getContext())") DeletePropertyNode deletePropertyNode,
                        @Cached("create(getContext())") JSArrayLastElementIndexNode lastElementIndexNode,
                        @Cached("create(getContext())") JSArrayFirstElementIndexNode firstElementIndexNode) {
            TruffleObject thisObj = toObject(thisObjParam);
            long len = getLength(thisObj);

            if (getContext().getEcmaScriptVersion() <= 5 || args.length > 0) {
                if (args.length + len > JSRuntime.MAX_SAFE_INTEGER_LONG) {
                    errorBranch.enter();
                    throwLengthError();
                }
                long lastIdx = lastElementIndexNode.executeLong(thisObj, len);
                long firstIdx = firstElementIndexNode.executeLong(thisObj, len);

                for (long i = lastIdx; i >= firstIdx; i = previousElementIndex(thisObj, i)) {
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
            }
            setLength(thisObj, len + args.length);
            return len + args.length;
        }
    }

    public abstract static class JSArrayToStringNode extends BasicArrayOperation {
        @Child private PropertyNode joinPropertyNode;
        @Child private JSFunctionCallNode callNode;

        public JSArrayToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.joinPropertyNode = NodeFactory.getInstance(getContext()).createProperty(getContext(), null, "join");
        }

        private Object getJoinProperty(TruffleObject target) {
            return joinPropertyNode.executeWithTarget(target);
        }

        private Object callJoin(Object target, Object function) {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(JSFunctionCallNode.createCall());
            }
            return callNode.executeCall(JSArguments.createZeroArg(target, function));
        }

        @Specialization
        protected Object toString(Object thisObj) {
            if (JSTruffleOptions.NashornCompatibilityMode && JSArrayBufferView.isJSArrayBufferView(thisObj)) {
                // cf. e.g. NASHORN-377.js
                return JSObject.defaultToString((DynamicObject) thisObj);
            }
            TruffleObject arrayObj = toObject(thisObj);
            if (JSObject.isJSObject(arrayObj)) {
                Object join = getJoinProperty(arrayObj);
                if (isCallable(join)) {
                    return callJoin(arrayObj, join);
                } else {
                    return JSObject.defaultToString((DynamicObject) arrayObj);
                }
            } else {
                return "[object Foreign]";
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

        private final ConditionProfile isFirstSpreadable = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasFirstElements = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isSecondSpreadable = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasSecondElements = ConditionProfile.createBinaryProfile();
        private final ConditionProfile lengthErrorProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasMultipleArgs = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasOneArg = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isProxy = ConditionProfile.createBinaryProfile();
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
            TruffleObject thisJSObj = toObject(thisObj);
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
                DynamicObject elObj = (DynamicObject) el;
                long len2 = getLength(elObj);
                if (hasElements.profile(len2 > 0)) {
                    return concatSpreadable(retObj, n, elObj, len2, hasOneElement);
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

        private long concatSpreadable(DynamicObject retObj, long n, DynamicObject elObj, long len2, final ConditionProfile hasOneElement) {
            if (lengthErrorProfile.profile((n + len2) > JSRuntime.MAX_SAFE_INTEGER)) {
                errorBranch.enter();
                throwLengthError();
            }
            if (isProxy.profile(JSProxy.isProxy(elObj))) {
                // strictly to the standard implementation; traps could expose optimizations!
                for (long k = 0; k < len2; k++) {
                    String kStr = toString((int) k);
                    if (hasProperty(elObj, kStr)) {
                        writeOwn(retObj, toString((int) (n + k)), readAny(elObj, kStr));
                    }
                }
            } else if (hasOneElement.profile(len2 == 1)) {
                // fastpath for 1-element entries
                if (hasProperty(elObj, 0)) {
                    writeOwn(retObj, n, read(elObj, 0));
                }
            } else {
                long k = firstElementIndex(elObj, len2);
                long lastI = lastElementIndex(elObj, len2);
                for (; k <= lastI; k = nextElementIndex(elObj, k, len2)) {
                    writeOwn(retObj, n + k, read(elObj, k));
                }
            }
            return n + len2;
        }

        // ES2015, 22.1.3.1.1
        private boolean isConcatSpreadable(Object el) {
            if (!JSObject.isJSObject(el) || el == Undefined.instance || el == Null.instance) {
                return false;
            }
            DynamicObject obj = (DynamicObject) el;
            Object spreadable = getSpreadableProperty(obj);
            if (spreadable != Undefined.instance) {
                return toBoolean(spreadable);
            }
            return getArraySpeciesConstructorNode().isArray(obj);
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

        private final BranchProfile arrayWithContentBranch = BranchProfile.create();
        private final BranchProfile fromConversionBranch = BranchProfile.create();

        public JSArrayIndexOfNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation, boolean isForward) {
            super(context, builtin, isTypedArrayImplementation);
            this.isForward = isForward;
        }

        @Specialization
        protected Object indexOf(Object thisObj, Object[] args,
                        @Cached("create()") JSToIntegerSpecialNode toIntegerNode) {
            TruffleObject thisJSObject = toObject(thisObj);
            long len = getLength(thisJSObject);
            if (len == 0) {
                return -1;
            }
            arrayWithContentBranch.enter();
            Object searchElement = JSRuntime.getArgOrUndefined(args, 0);
            Object fromIndex = JSRuntime.getArgOrUndefined(args, 1);

            long fromIndexValue = isForward() ? calcFromIndexForward(args, len, fromIndex, toIntegerNode) : calcFromIndexBackward(args, len, fromIndex, toIntegerNode);
            if (fromIndexValue < 0) {
                return -1;
            }
            return forEachIndexCall(thisJSObject, Undefined.instance, searchElement, fromIndexValue, len, -1);
        }

        // for indexOf()
        private long calcFromIndexForward(Object[] args, long len, Object fromIndex, JSToIntegerSpecialNode toIntegerNode) {
            if (args.length <= 1) {
                return 0;
            } else {
                fromConversionBranch.enter();
                long fromIndexValue = toIntegerNode.executeLong(fromIndex);
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
        private long calcFromIndexBackward(Object[] args, long len, Object fromIndex, JSToIntegerSpecialNode toIntegerNode) {
            if (args.length <= 1) {
                return len - 1;
            } else {
                fromConversionBranch.enter();
                long fromIndexInt = toIntegerNode.executeLong(fromIndex);
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

        public JSArrayJoinNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.elementToStringNode = JSToStringNode.create();
        }

        @Specialization
        protected String join(Object thisObj, Object joinStr) {
            final TruffleObject thisJSObject = toObject(thisObj);
            final long length = getLength(thisJSObject);
            final String joinSeparator = joinStr == Undefined.instance ? "," : getSeparatorToString().executeString(joinStr);

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
        }

        private JSToStringNode getSeparatorToString() {
            if (separatorToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                separatorToStringNode = insert(JSToStringNode.create());
            }
            return separatorToStringNode;
        }

        private String joinOne(final TruffleObject thisObject) {
            Object value = read(thisObject, 0);
            return toStringOrEmpty(thisObject, value);
        }

        private String joinTwo(final TruffleObject thisObject, final String joinSeparator, final boolean appendSep) {
            String first = toStringOrEmpty(thisObject, read(thisObject, 0));
            String second = toStringOrEmpty(thisObject, read(thisObject, 1));

            long resultLength = first.length() + (appendSep ? joinSeparator.length() : 0) + second.length();
            if (resultLength > JSTruffleOptions.StringLengthLimit) {
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

        private String joinLoop(final TruffleObject thisJSObject, final long length, final String joinSeparator, final boolean appendSep) {
            final DelimitedStringBuilder res = new DelimitedStringBuilder();
            long i = 0;
            while (i < length) {
                if (appendSep && i != 0) {
                    res.append(joinSeparator);
                }
                Object value = read(thisJSObject, i);
                res.append(toStringOrEmpty(thisJSObject, value));

                if (appendSep) {
                    i++;
                } else {
                    i = nextElementIndex(thisJSObject, i, length);
                }
            }
            return res.toString();
        }

        private String toStringOrEmpty(final TruffleObject thisObject, Object value) {
            if (isValidEntry(thisObject, value)) {
                return elementToStringNode.executeString(value);
            } else {
                return "";
            }
        }

        private static boolean isValidEntry(final TruffleObject thisObject, Object value) {
            // the last check here is to avoid recursion
            return value != Undefined.instance && value != Null.instance && value != thisObject;
        }

        private String joinSparse(TruffleObject thisObject, long length, String joinSeparator, final boolean appendSep) {
            ArrayList<Object> converted = new ArrayList<>();
            long calculatedLength = 0;
            long i = 0;
            while (i < length) {
                Object value = read(thisObject, i);
                if (isValidEntry(thisObject, value)) {
                    String string = elementToStringNode.executeString(value);
                    int stringLength = string.length();
                    if (stringLength > 0) {
                        calculatedLength += stringLength;
                        Boundaries.listAdd(converted, i);
                        Boundaries.listAdd(converted, string);
                    }
                }
                i = nextElementIndex(thisObject, i, length);
            }
            if (appendSep) {
                calculatedLength += (length - 1) * joinSeparator.length();
            }
            if (calculatedLength > JSTruffleOptions.StringLengthLimit) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createRangeErrorInvalidStringLength();
            }
            assert calculatedLength <= Integer.MAX_VALUE;
            final DelimitedStringBuilder res = new DelimitedStringBuilder((int) calculatedLength);
            long lastIndex = 0;
            for (int j = 0; j < converted.size(); j += 2) {
                long index = (long) Boundaries.listGet(converted, j);
                String value = (String) Boundaries.listGet(converted, j + 1);
                if (appendSep) {
                    for (long k = lastIndex; k < index; k++) {
                        res.append(joinSeparator);
                    }
                }
                res.append(value);
                lastIndex = index;
            }
            if (appendSep) {
                for (long k = lastIndex; k < length - 1; k++) {
                    res.append(joinSeparator);
                }
            }
            assert res.length() == calculatedLength;
            return res.toString();
        }
    }

    public abstract static class JSArrayToLocaleStringNode extends JSArrayOperation {

        @Child private PropertyGetNode getToLocaleString;
        @Child private JSFunctionCallNode callToLocaleString;

        public JSArrayToLocaleStringNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected String toLocaleString(VirtualFrame frame, Object thisObj,
                        @Cached("create()") JSToStringNode toStringNode) {
            TruffleObject arrayObj = toObject(thisObj);
            long len = getLength(arrayObj);
            if (len == 0) {
                return "";
            }
            Object[] userArguments = JSArguments.extractUserArguments(frame.getArguments());
            long k = 0;
            DelimitedStringBuilder r = new DelimitedStringBuilder();
            while (k < len) {
                if (k > 0) {
                    r.append(',');
                }
                Object nextElement = read(arrayObj, k);
                if (nextElement != Null.instance && nextElement != Undefined.instance) {
                    Object result = callToLocaleString(nextElement, userArguments);
                    String executeString = toStringNode.executeString(result);
                    r.append(executeString);
                }
                k++;
            }
            return r.toString();
        }

        private Object callToLocaleString(Object nextElement, Object[] userArguments) {
            if (getToLocaleString == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getToLocaleString = insert(PropertyGetNode.create("toLocaleString", false, getContext()));
                callToLocaleString = insert(JSFunctionCallNode.createCall());
            }

            Object toLocaleString = getToLocaleString.getValue(nextElement);
            JSFunction.checkIsFunction(toLocaleString);
            return callToLocaleString.executeCall(JSArguments.create(nextElement, toLocaleString, userArguments));
        }
    }

    public abstract static class JSArraySpliceNode extends JSArrayOperationWithToInt {

        @Child private DeletePropertyNode deletePropertyNode; // DeletePropertyOrThrow
        private final BranchProfile branchA = BranchProfile.create();
        private final BranchProfile branchB = BranchProfile.create();
        private final BranchProfile branchDelete = BranchProfile.create();
        private final BranchProfile objectBranch = BranchProfile.create();
        private final ConditionProfile arrayElementwise = ConditionProfile.createBinaryProfile();
        private final ConditionProfile argsLengthProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile offsetProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile needMoveDeleteBranch = BranchProfile.create();
        private final BranchProfile needLoopDeleteBranch = BranchProfile.create();
        private final BranchProfile needFillBranch = BranchProfile.create();
        private final ValueProfile arrayTypeProfile = ValueProfile.createClassProfile();

        public JSArraySpliceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.deletePropertyNode = DeletePropertyNode.create(THROW_ERROR, context);
        }

        @Specialization
        protected DynamicObject splice(Object thisArg, Object[] args) {
            TruffleObject thisObj = toObject(thisArg);
            long len = getLength(thisObj);

            long actualStart = JSRuntime.getOffset(toIntegerSpecial(JSRuntime.getArgOrUndefined(args, 0)), len, offsetProfile);
            long actualDeleteCount = 0;
            if (argsLengthProfile.profile(args.length != 1)) {
                long deleteCount = toIntegerSpecial(JSRuntime.getArgOrUndefined(args, 1));
                actualDeleteCount = Math.min(Math.max(deleteCount, 0), len - actualStart);
            } else {
                // This is off-spec! all other major engines do it that way, though.
                // see testV8/array-splice.js:74 ff.
                actualDeleteCount = len - actualStart;
            }

            long itemCount = Math.max(0, args.length - 2);
            if (len + itemCount - actualDeleteCount > JSRuntime.MAX_SAFE_INTEGER_LONG) {
                errorBranch.enter();
                throwLengthError();
            }

            DynamicObject aObj = (DynamicObject) getArraySpeciesConstructorNode().createEmptyContainer(thisObj, actualDeleteCount);

            if (actualDeleteCount > 0) {
                branchDelete.enter();
                spliceRead(thisObj, actualStart, actualDeleteCount, aObj, len);
            }
            setLength(aObj, actualDeleteCount);

            boolean isJSArray = JSArray.isJSArray(thisObj);
            if (isJSArray) {
                DynamicObject dynObj = (DynamicObject) thisObj;
                ScriptArray arrayType = arrayTypeProfile.profile(arrayGetArrayType(dynObj, isJSArray));
                if (arrayElementwise.profile(mustUseElementwise(dynObj, arrayType))) {
                    spliceIntlArrayElementwise(dynObj, len, actualStart, actualDeleteCount, itemCount);
                } else {
                    spliceIntlArrayBlockwise(dynObj, actualStart, actualDeleteCount, itemCount, arrayType);
                }
            } else {
                objectBranch.enter();
                spliceIntlObj(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }

            if (itemCount > 0) {
                needFillBranch.enter();
                spliceFill(thisObj, actualStart, args);
            }

            long newLength = (len - actualDeleteCount + itemCount);
            if (newLength <= Integer.MAX_VALUE) {
                setLength(thisObj, (int) newLength);
            } else {
                setLength(thisObj, (double) newLength);
            }
            return aObj;
        }

        private boolean mustUseElementwise(DynamicObject obj, ScriptArray array) {
            return array instanceof SparseArray || array.isLengthNotWritable() || JSObject.getPrototype(obj) != getContext().getRealm().getArrayConstructor().getPrototype() ||
                            !getContext().getArrayPrototypeNoElementsAssumption().isValid() || (!getContext().getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(obj));
        }

        private void spliceRead(TruffleObject thisObj, long actualStart, long actualDeleteCount, DynamicObject aObj, long length) {
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

        private void spliceFill(TruffleObject thisObj, long actualStart, Object[] args) {
            for (int i = 2; i < args.length; i++) {
                write(thisObj, actualStart + i - 2, args[i]);
            }
        }

        private void spliceIntlObj(TruffleObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            if (itemCount < actualDeleteCount) {
                branchA.enter();
                spliceIntlObjShrink(thisObj, len, actualStart, actualDeleteCount, itemCount);
            } else if (itemCount > actualDeleteCount) {
                branchB.enter();
                spliceIntlObjMove(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }
        }

        private void spliceIntlObjMove(TruffleObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            long k = len - actualDeleteCount;
            while (k > actualStart) {
                spliceMoveValue(thisObj, (k + actualDeleteCount - 1), (k + itemCount - 1));
                k--;
            }
        }

        private void spliceIntlObjShrink(TruffleObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            long k = actualStart;
            while (k < len - actualDeleteCount) {
                spliceMoveValue(thisObj, (k + actualDeleteCount), (k + itemCount));
                k++;
            }
            k = len;
            if (k > len - actualDeleteCount + itemCount) {
                needLoopDeleteBranch.enter();
                while (k > len - actualDeleteCount + itemCount) {
                    deletePropertyNode.executeEvaluated(thisObj, k - 1);
                    k--;
                }
            }
        }

        private void spliceMoveValue(TruffleObject thisObj, long fromIndex, long toIndex) {
            if (hasProperty(thisObj, fromIndex)) {
                Object val = read(thisObj, fromIndex);
                write(thisObj, toIndex, val);
            } else {
                needMoveDeleteBranch.enter();
                deletePropertyNode.executeEvaluated(thisObj, toIndex);
            }
        }

        private void spliceIntlArrayElementwise(DynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
            assert JSArray.isJSArray(thisObj); // contract
            if (itemCount < actualDeleteCount) {
                branchA.enter();
                spliceIntlArrayElementwiseWalkUp(thisObj, len, actualStart, actualDeleteCount, itemCount);
            } else if (itemCount > actualDeleteCount) {
                branchB.enter();
                spliceIntlArrayElementwiseWalkDown(thisObj, len, actualStart, actualDeleteCount, itemCount);
            }
        }

        private void spliceIntlArrayElementwiseWalkDown(DynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
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

        private void spliceIntlArrayElementwiseWalkUp(DynamicObject thisObj, long len, long actualStart, long actualDeleteCount, long itemCount) {
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
            if (k >= len + delta) {
                needLoopDeleteBranch.enter();
                while (k >= len + delta) {
                    deletePropertyNode.executeEvaluated(thisObj, k);
                    k = previousElementIndex(thisObj, k);
                }
            }
        }

        private void spliceIntlArrayBlockwise(DynamicObject thisObj, long actualStart, long actualDeleteCount, long itemCount, ScriptArray array) {
            assert JSArray.isJSArray(thisObj); // contract
            if (itemCount < actualDeleteCount) {
                branchA.enter();
                arraySetArrayType(thisObj, array.removeRange(thisObj, actualStart + itemCount, actualStart + actualDeleteCount, errorBranch));
            } else if (itemCount > actualDeleteCount) {
                branchB.enter();
                arraySetArrayType(thisObj, array.addRange(thisObj, actualStart, (int) (itemCount - actualDeleteCount)));
            }
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
            public Object apply(long index, Object value, TruffleObject target, DynamicObject callback, Object callbackThisArg, Object currentResult) {
                return callNode.executeCall(JSArguments.create(callbackThisArg, callback, value, JSRuntime.boxIndex(index, indexInIntRangeCondition), target));
            }
        }

        @Child private ForEachIndexCallNode forEachIndexNode;

        protected final Object forEachIndexCall(TruffleObject arrayObj, DynamicObject callbackObj, Object thisArg, long fromIndex, long length, Object initialResult) {
            if (forEachIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                forEachIndexNode = insert(makeForEachIndexCallNode());
            }
            return forEachIndexNode.executeForEachIndex(arrayObj, callbackObj, thisArg, fromIndex, length, initialResult);
        }

        private ForEachIndexCallNode makeForEachIndexCallNode() {
            return ForEachIndexCallNode.create(getContext(), makeCallbackNode(), makeMaybeResultNode(), isForward());
        }

        protected boolean isForward() {
            return true;
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
            TruffleObject thisJSObj = toObject(thisObj);
            if (isTypedArrayImplementation) {
                validateTypedArray(thisJSObj);
            }
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);
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
            TruffleObject thisJSObj = toObject(thisObj);
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);

            DynamicObject resultArray;
            if (isTypedArrayImplementation) {
                resultArray = JSArray.createEmpty(getContext(), 0);
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
            if (!JSObject.isJSObject(obj)) {
                errorBranch.enter();
                throw Errors.createTypeErrorNotAnObject(obj);
            }
            DynamicObject typedResult = (DynamicObject) obj;
            TypedArray typedArray = arrayTypeProfile.profile(JSArrayBufferView.typedArrayGetArrayType(typedResult));
            ScriptArray array = resultArrayTypeProfile.profile(arrayGetArrayType(resultArray));
            for (long i = 0; i < resultLen; i++) {
                typedArray.setElement(typedResult, i, array.getElement(resultArray, i), true);
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
                @Child private WriteElementNode writeOwnNode = NodeFactory.getInstance(getContext()).createWriteElementNode(getContext(), true, true);

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
            TruffleObject thisJSObj = toObject(thisObj);
            if (isTypedArrayImplementation) {
                validateTypedArray(thisJSObj);
            }
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);
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
            TruffleObject thisJSObj = toObject(thisObj);
            if (isTypedArrayImplementation) {
                validateTypedArray(thisJSObj);
            }
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);
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
        protected TruffleObject map(Object thisObj, Object callback, Object thisArg) {
            TruffleObject thisJSObj = toObject(thisObj);
            if (isTypedArrayImplementation) {
                validateTypedArray(thisJSObj);
            }
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);

            Object resultArray = getArraySpeciesConstructorNode().createEmptyContainer(thisJSObj, length);
            return (TruffleObject) forEachIndexCall(thisJSObj, callbackFn, thisArg, 0, length, resultArray);
        }

        @Override
        protected MaybeResultNode makeMaybeResultNode() {
            return new ForEachIndexCallNode.MaybeResultNode() {
                @Child private WriteElementNode writeOwnNode = NodeFactory.getInstance(getContext()).createWriteElementNode(getContext(), true, true);

                @Override
                public MaybeResult<Object> apply(long index, Object value, Object callbackResult, Object currentResult) {
                    writeOwnNode.executeWithTargetAndIndexAndValue(currentResult, index, callbackResult);
                    return MaybeResult.continueResult(currentResult);
                }
            };
        }
    }

    public abstract static class JSArrayFindNode extends JSArrayOperation {
        @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();
        @Child private JSFunctionCallNode callNode = JSFunctionCallNode.createCall();

        public JSArrayFindNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        private Object callPredicate(DynamicObject function, Object target, Object value, double index, Object thisObj) {
            return callNode.executeCall(JSArguments.create(target, function, value, index, thisObj));
        }

        @Specialization
        protected Object find(Object thisObj, Object callback, Object thisArg) {
            TruffleObject thisJSObj = toObject(thisObj);
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);

            long k;
            for (k = 0; k < length; k++) {
                Object value = read(thisObj, k);
                Object callbackResult = callPredicate(callbackFn, thisArg, value, k, thisJSObj);
                boolean testResult = toBooleanNode.executeBoolean(callbackResult);
                if (testResult) {
                    return value;
                }
            }
            return Undefined.instance;
        }
    }

    public abstract static class JSArrayFindIndexNode extends JSArrayOperation {
        @Child private JSToBooleanNode toBooleanNode = JSToBooleanNode.create();
        @Child private JSFunctionCallNode callNode = JSFunctionCallNode.createCall();

        public JSArrayFindIndexNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        private Object callPredicate(DynamicObject function, Object target, Object value, double index, Object thisObj) {
            return callNode.executeCall(JSArguments.create(target, function, value, index, thisObj));
        }

        @Specialization
        protected Object findIndex(Object thisObj, Object callback, Object thisArg) {
            TruffleObject thisJSObj = toObject(thisObj);
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);

            long k;
            for (k = 0; k < length; k++) {
                Object value = read(thisObj, k);
                Object callbackResult = callPredicate(callbackFn, thisArg, value, k, thisJSObj);
                boolean testResult = toBooleanNode.executeBoolean(callbackResult);
                if (testResult) {
                    return JSRuntime.positiveLongToIntOrDouble(k);
                }
            }
            return -1;
        }
    }

    public abstract static class JSArraySortNode extends JSArrayOperation {

        @Child private DeletePropertyNode deletePropertyNode; // DeletePropertyOrThrow
        private final BranchProfile arrayIsSparseBranch = BranchProfile.create();
        private final BranchProfile arrayHasHolesBranch = BranchProfile.create();
        private final BranchProfile arrayIsDefaultBranch = BranchProfile.create();
        private final BranchProfile hasCompareFnBranch = BranchProfile.create();
        private final BranchProfile noCompareFnBranch = BranchProfile.create();

        public JSArraySortNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
            this.deletePropertyNode = DeletePropertyNode.create(THROW_ERROR, context);
        }

        @Specialization(guards = "isJSFastArray(thisObj)")
        protected DynamicObject sortArray(final DynamicObject thisObj, final Object compare, //
                        @Cached("create(getContext())") JSToObjectArrayNode arrayToObjectArrayNode,
                        @Cached("createClassProfile()") ValueProfile classProfile) {
            checkCompareFunction(compare);
            Object[] array;
            ScriptArray scriptArray = classProfile.profile(arrayGetArrayType(thisObj));
            long len = getLength(thisObj);

            if (scriptArray instanceof SparseArray) {
                arrayIsSparseBranch.enter();
                array = getArraySparse(thisObj, scriptArray, len);
            } else if (scriptArray.isHolesType() || scriptArray.hasHoles(thisObj)) {
                arrayHasHolesBranch.enter();
                if (JSObject.isFrozen(thisObj)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("cannot write to frozen object");
                }
                array = getArraySparse(thisObj, scriptArray, len);
            } else {
                arrayIsDefaultBranch.enter();
                array = arrayToObjectArrayNode.executeObjectArray(thisObj);
            }

            sortIntl(getComparator(thisObj, compare), array);
            for (int i = 0; i < array.length; i++) {
                write(thisObj, i, array[i]);
            }

            if (scriptArray instanceof SparseArray) {
                arrayIsSparseBranch.enter();
                deleteSparse(thisObj, array.length, len);
            } else if (scriptArray.isHolesType()) {
                arrayHasHolesBranch.enter();
                deleteSparse(thisObj, array.length, len);
            } else {
                arrayIsDefaultBranch.enter();
                for (int i = array.length; i < len; i++) {
                    deletePropertyNode.executeEvaluated(thisObj, i);
                }
            }
            return thisObj;
        }

        @Specialization
        protected DynamicObject sort(Object thisObj, final Object comparefn,
                        @Cached("create()") BranchProfile notAJSObjectBranch) {
            checkCompareFunction(comparefn);
            DynamicObject thisJSObj = JSRuntime.expectJSObject(toObject(thisObj), notAJSObjectBranch);
            if (JSObject.isFrozen(thisJSObj)) {
                errorBranch.enter();
                throw Errors.createTypeError("cannot write to frozen object");
            }
            long len = getLength(thisJSObj);
            Iterable<Object> keys = getKeys(thisJSObj);
            Object[] array = objectToArray(thisJSObj, len, keys);

            Comparator<Object> comparator = getComparator(thisJSObj, comparefn);
            sortIntl(comparator, array);

            for (int i = 0; i < array.length; i++) {
                write(thisJSObj, i, array[i]);
            }
            deleteGenericElements(thisJSObj, array.length, len, keys);
            return thisJSObj;
        }

        private void checkCompareFunction(Object compare) {
            if (!(JSRuntime.isCallable(compare) || JSRuntime.isForeignObject(compare) || getContext().isOptionV8CompatibilityMode() || compare == Undefined.instance)) {
                errorBranch.enter();
                throw Errors.createTypeError("illegal compare function");
            }
        }

        private Comparator<Object> getComparator(final DynamicObject thisObj, final Object compare) {
            if (JSRuntime.isCallable(compare) || JSRuntime.isForeignObject(compare)) {
                hasCompareFnBranch.enter();
                DynamicObject arrayBufferObj = isTypedArrayImplementation && JSArrayBufferView.isJSArrayBufferView(thisObj) ? JSArrayBufferView.getArrayBuffer(thisObj) : null;
                return new SortComparator(compare, arrayBufferObj);
            } else {
                noCompareFnBranch.enter();
                return getDefaultComparator(thisObj);
            }
        }

        @TruffleBoundary
        private Comparator<Object> getDefaultComparator(DynamicObject thisObj) {
            if (isTypedArrayImplementation) {
                return new JSArrayBufferView.DefaultJSArrayBufferViewComparator();
            } else {
                if (JSArray.isJSArray(thisObj)) {
                    ScriptArray array = arrayGetArrayType(thisObj);
                    if (array instanceof AbstractIntArray || array instanceof ConstantByteArray || array instanceof ConstantIntArray) {
                        return new JSArray.DefaultJSArrayIntegerComparator();
                    } else if (array instanceof AbstractDoubleArray || array instanceof ConstantDoubleArray) {
                        return new JSArray.DefaultJSArrayDoubleComparator();
                    }
                }
                return new JSArray.DefaultJSArrayComparator();
            }
        }

        /**
         * In a generic JSObject, this deletes all elements between the actual "size" (i.e., number
         * of non-empty elements) and the "length" (value of the property). I.e., it cleans up
         * garbage remaining after sorting all elements to lower indices (in case there are holes).
         */
        private void deleteGenericElements(DynamicObject obj, long fromIndex, long toIndex, Iterable<Object> keys) {
            for (Object key : keys) {
                long index = JSRuntime.propertyKeyToArrayIndex(key);
                if (fromIndex <= index && index < toIndex) {
                    deletePropertyNode.executeEvaluated(obj, key);
                }
            }
        }

        private void deleteSparse(DynamicObject thisObj, long start, long end) {
            long pos = start;
            while (pos < end) {
                deletePropertyNode.executeEvaluated(thisObj, pos);
                pos = nextElementIndex(thisObj, pos, end);
            }
        }

        private Object[] getArraySparse(DynamicObject thisObj, ScriptArray scriptArray, long len) {
            long pos = scriptArray.firstElementIndex(thisObj);
            ArrayList<Object> list = new ArrayList<>();
            while (pos <= scriptArray.lastElementIndex(thisObj)) {
                assert scriptArray.hasElement(thisObj, pos);
                Boundaries.listAdd(list, scriptArray.getElement(thisObj, pos));
                pos = nextElementIndex(thisObj, pos, len);
            }
            return list.toArray(new Object[list.size()]);
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
                if (isTypedArrayImplementation) {
                    if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(arrayBufferObj)) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorDetachedBuffer();
                    }
                }
                double d = JSRuntime.toDouble(retObj);
                return d == 0 ? 0 : (d < 0 ? -1 : 1);
            }
        }

        @TruffleBoundary
        private static Object[] objectToArray(DynamicObject thisObj, long len, Iterable<Object> keys) {
            ArrayList<Object> list = new ArrayList<>();
            for (Object key : keys) {
                long index = JSRuntime.propertyKeyToArrayIndex(key);
                if (0 <= index && index < len) {
                    Boundaries.listAdd(list, JSObject.get(thisObj, index));
                }
            }
            return list.toArray();
        }

        @TruffleBoundary
        private Iterable<Object> getKeys(DynamicObject thisObj) {
            if (getContext().isOptionArraySortInherited()) {
                Set<Object> keys = new LinkedHashSet<>();
                for (DynamicObject current = thisObj; current != Null.instance; current = JSObject.getPrototype(current)) {
                    for (Object key : JSObject.ownPropertyKeys(current)) {
                        if (key instanceof String && JSRuntime.isArrayIndex((String) key)) {
                            keys.add(key);
                        }
                    }
                }
                return keys;
            }
            return JSObject.ownPropertyKeys(thisObj);
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
            TruffleObject thisJSObj = toObject(thisObj);
            if (isTypedArrayImplementation) {
                validateTypedArray(thisJSObj);
            }
            long length = getLength(thisJSObj);
            DynamicObject callbackFn = checkCallbackIsFunction(callback);

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
        protected final Pair<Long, Object> findInitialValue(TruffleObject arrayObj, long fromIndex, long length) {
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
                }, isForward()));
            }
            return forEachIndexFindInitialNode;
        }

        @Override
        protected CallbackNode makeCallbackNode() {
            return new DefaultCallbackNode() {
                @Override
                public Object apply(long index, Object value, TruffleObject target, DynamicObject callback, Object callbackThisArg, Object currentResult) {
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
        protected TruffleObject fill(Object thisObj, Object value, Object start, Object end) {
            TruffleObject thisJSObj = toObject(thisObj);
            long len = getLength(thisJSObj);
            long lStart = JSRuntime.getOffset(toIntegerSpecial(start), len, offsetProfile1);
            long lEnd = end == Undefined.instance ? len : JSRuntime.getOffset(toIntegerSpecial(end), len, offsetProfile2);

            for (long idx = lStart; idx < lEnd; idx++) {
                write(thisJSObj, idx, value);
            }
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
        protected TruffleObject copyWithin(Object thisObj, Object target, Object start, Object end) {
            TruffleObject obj = toObject(thisObj);
            long len = getLength(obj);
            long to = JSRuntime.getOffset(toIntegerSpecial(target), len, offsetProfile1);
            long from = JSRuntime.getOffset(toIntegerSpecial(start), len, offsetProfile2);

            long finalIdx;
            if (end == Undefined.instance) {
                finalIdx = len;
            } else {
                finalIdx = JSRuntime.getOffset(toIntegerSpecial(end), len, offsetProfile3);
            }
            long count = Math.min(finalIdx - from, len - to);

            long direction;
            if (from < to && to < (from + count)) {
                direction = -1;
                from = from + count - 1;
                to = to + count - 1;
            } else {
                direction = 1;
            }

            while (count > 0) {
                if (hasProperty(obj, from)) {
                    Object fromVal = read(obj, from);
                    write(obj, to, fromVal);
                } else {
                    deletePropertyNode.executeEvaluated(obj, to);
                }
                from += direction;
                to += direction;
                count--;
            }

            return obj;
        }
    }

    public abstract static class JSArrayIncludesNode extends JSArrayOperationWithToInt {

        public JSArrayIncludesNode(JSContext context, JSBuiltin builtin, boolean isTypedArrayImplementation) {
            super(context, builtin, isTypedArrayImplementation);
        }

        @Specialization
        protected boolean includes(Object thisObj, Object searchElement, Object fromIndex,
                        @Cached("createSameValueZero()") JSIdenticalNode identicalNode) {
            TruffleObject thisJSObj = toObject(thisObj);
            long len = getLength(thisJSObj);
            if (len == 0) {
                return false;
            }

            long n = toIntegerSpecial(fromIndex);
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

            while (k < len) {
                Object currentElement = read(thisObj, k);

                if (identicalNode.executeBoolean(searchElement, currentElement)) {
                    return true;
                }
                k++;
            }
            return false;
        }
    }

    public abstract static class JSArrayReverseNode extends JSArrayOperation {
        @Child private DebugIsHolesArrayNode isHolesArrayNode;
        @Child private DeletePropertyNode deletePropertyNode;
        private final ConditionProfile hasHolesProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile bothExistProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile onlyUpperExistsProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile onlyLowerExistsProfile = ConditionProfile.createBinaryProfile();

        public JSArrayReverseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isHolesArrayNode = DebugIsHolesArrayNodeGen.create(context, null, null);
        }

        private boolean deleteProperty(TruffleObject array, long index) {
            if (deletePropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                deletePropertyNode = insert(DeletePropertyNode.create(true, getContext()));
            }
            return deletePropertyNode.executeEvaluated(array, index);
        }

        @Specialization
        protected Object reverse(Object thisObj) {
            final TruffleObject array = toObject(thisObj);
            final long length = getLength(array);
            long lower = 0;
            long upper = length - 1;
            boolean hasHoles = isHolesArrayNode.executeBoolean(array);

            while (lower <= upper) {
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

                if (hasHolesProfile.profile(hasHoles)) {
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
            }

            return array;
        }
    }

    public static class CreateArrayIteratorNode extends JavaScriptBaseNode {
        private final JSContext context;
        private final int iterationKind;
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private PropertySetNode setIterationKindNode;

        protected CreateArrayIteratorNode(JSContext context, int iterationKind) {
            this.context = context;
            this.iterationKind = iterationKind;
            this.createObjectNode = CreateObjectNode.createWithCachedPrototype(context, null);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.setIterationKindNode = PropertySetNode.createSetHidden(JSArray.ARRAY_ITERATION_KIND_ID, context);
        }

        public static CreateArrayIteratorNode create(JSContext context, int iterationKind) {
            return new CreateArrayIteratorNode(context, iterationKind);
        }

        public DynamicObject execute(VirtualFrame frame, TruffleObject array) {
            assert JSGuards.isJSObject(array) || JSGuards.isForeignObject(array);
            DynamicObject iterator = createObjectNode.executeDynamicObject(frame, context.getRealm().getArrayIteratorPrototype());
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
            return createArrayIteratorNode.execute(frame, toObjectNode.executeTruffleObject(thisObj));
        }
    }
}
