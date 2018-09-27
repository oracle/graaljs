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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.JSConstructTypedArrayNodeGen.IntegerIndexedObjectCreateNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeFromConstructorNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSGetLengthNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * The %TypedArray% intrinsic constructor function object (ES6 22.2.1).
 */
@ImportStatic(JSArrayBuffer.class)
public abstract class JSConstructTypedArrayNode extends JSBuiltinNode {
    @Child private JSToIndexNode toIndexNode;
    // for TypedArray(factory)
    @Child private GetPrototypeFromConstructorNode getPrototypeFromConstructorViewNode;
    // for Buffer
    @Child private GetPrototypeFromConstructorNode getPrototypeFromConstructorBufferNode;
    @Child private IntegerIndexedObjectCreateNode integerIndexObjectCreateNode;
    @Child private ArraySpeciesConstructorNode arraySpeciesConstructorNode;
    private final BranchProfile errorBranch = BranchProfile.create();

    private final TypedArrayFactory factory;

    public JSConstructTypedArrayNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
        this.factory = findTypedArrayFactory(builtin.getName());
    }

    private static TypedArrayFactory findTypedArrayFactory(String name) {
        for (TypedArrayFactory typedArrayFactory : TypedArray.factories()) {
            if (typedArrayFactory.getName().equals(name)) {
                return typedArrayFactory;
            }
        }
        throw new NoSuchElementException(name);
    }

    private long toIndex(Object target) {
        if (toIndexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toIndexNode = insert(JSToIndexNode.create());
        }
        return toIndexNode.executeLong(target);
    }

    private DynamicObject getPrototypeFromConstructorView(DynamicObject newTarget) {
        if (getPrototypeFromConstructorViewNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getPrototypeFromConstructorViewNode = insert(GetPrototypeFromConstructorNode.create(getContext(), null, realm -> realm.getArrayBufferViewConstructor(factory).getPrototype()));
        }
        return getPrototypeFromConstructorViewNode.executeWithConstructor(newTarget);
    }

    private DynamicObject getPrototypeFromConstructorBuffer(DynamicObject newTarget) {
        if (getPrototypeFromConstructorBufferNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getPrototypeFromConstructorBufferNode = insert(GetPrototypeFromConstructorNode.create(getContext(), null, realm -> realm.getArrayBufferConstructor().getPrototype()));
        }
        return getPrototypeFromConstructorBufferNode.executeWithConstructor(newTarget);
    }

    private DynamicObject integerIndexObjectCreate(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto) {
        if (integerIndexObjectCreateNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            integerIndexObjectCreateNode = insert(IntegerIndexedObjectCreateNodeGen.create(getContext(), factory));
        }
        return integerIndexObjectCreateNode.execute(arrayBuffer, typedArray, offset, length, proto);
    }

    protected final ReadElementNode createReadNode() {
        return NodeFactory.getInstance(getContext()).createReadElementNode(getContext(), null, null);
    }

    private void checkDetachedBuffer(DynamicObject buffer) {
        if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(buffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
    }

    /**
     * %TypedArray%(buffer[, byteOffset[, length]]) (ES6 22.2.1.5).
     *
     * TypedArray(ArrayBuffer buffer, optional unsigned long byteOffset, optional unsigned long
     * length).
     *
     * Create a new TypedArray object using the passed ArrayBuffer for its storage. Optional
     * byteOffset and length can be used to limit the section of the buffer referenced. The
     * byteOffset indicates the offset in bytes from the start of the ArrayBuffer, and the length is
     * the count of elements from the offset that this TypedArray will reference. If both byteOffset
     * and length are omitted, the TypedArray spans the entire ArrayBuffer range. If the length is
     * omitted, the TypedArray extends from the given byteOffset until the end of the ArrayBuffer.
     *
     * The given byteOffset must be a multiple of the element size of the specific type, otherwise
     * an exception is raised.
     *
     * If a given byteOffset and length references an area beyond the end of the ArrayBuffer an
     * exception is raised.
     *
     * If length is not explicitly specified, the length of the ArrayBuffer minus the byteOffset
     * must be a multiple of the element size of the specific type, or an exception is raised.
     */
    @Specialization(guards = {"isJSFunction(newTarget)", "isJSHeapArrayBuffer(arrayBuffer)"})
    protected DynamicObject doArrayBuffer(DynamicObject newTarget, DynamicObject arrayBuffer, Object byteOffset0, Object length0,
                    @Cached("createBinaryProfile()") ConditionProfile lengthIsUndefined) {
        checkDetachedBuffer(arrayBuffer);
        byte[] byteArray = JSArrayBuffer.getByteArray(arrayBuffer);
        int arrayBufferLength = byteArray.length;
        return doArrayBufferImpl(arrayBuffer, byteOffset0, length0, newTarget, arrayBufferLength, false, lengthIsUndefined);
    }

    @Specialization(guards = {"isJSFunction(newTarget)", "isJSDirectArrayBuffer(arrayBuffer)"})
    protected DynamicObject doDirectArrayBuffer(DynamicObject newTarget, DynamicObject arrayBuffer, Object byteOffset0, Object length0,
                    @Cached("createBinaryProfile()") ConditionProfile lengthIsUndefined) {
        checkDetachedBuffer(arrayBuffer);
        ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        int arrayBufferLength = byteBuffer.limit();
        return doArrayBufferImpl(arrayBuffer, byteOffset0, length0, newTarget, arrayBufferLength, true, lengthIsUndefined);
    }

    private DynamicObject doArrayBufferImpl(DynamicObject arrayBuffer, Object byteOffset0, Object length0, DynamicObject newTarget, int bufferByteLength, boolean direct,
                    ConditionProfile lengthIsUndefinedProfile) {
        final int elementSize = factory.bytesPerElement();

        final long byteOffset = toIndex(byteOffset0);
        rangeCheckIsMultipleOfElementSize(byteOffset % elementSize == 0, "start offset", factory.getName(), elementSize);

        long length = 0;
        if (!lengthIsUndefinedProfile.profile(length0 == Undefined.instance)) {
            length = toIndex(length0);
            assert length >= 0;
        }

        checkDetachedBuffer(arrayBuffer);

        if (lengthIsUndefinedProfile.profile(length0 == Undefined.instance)) {
            rangeCheckIsMultipleOfElementSize(bufferByteLength % elementSize == 0, "buffer.byteLength", factory.getName(), elementSize);
            length = ((bufferByteLength - byteOffset) / elementSize);
            rangeCheck(length >= 0, "length < 0");
        }

        checkLengthLimit(length, elementSize);
        final int byteLength = toByteLength((int) length, elementSize);
        rangeCheck(byteOffset + byteLength <= bufferByteLength, "length exceeds buffer bounds");

        assert byteOffset <= Integer.MAX_VALUE && length <= Integer.MAX_VALUE;
        TypedArray typedArray = factory.createArrayType(direct, byteOffset != 0);
        return createTypedArray(arrayBuffer, typedArray, (int) byteOffset, (int) length, newTarget);
    }

    /**
     * TypedArray(SharedArrayBuffer buffer, optional unsigned long byteOffset, optional unsigned
     * long length).
     *
     * Create a new TypedArray object using the passed SharedArrayBuffer for its storage. As with
     * standard ArrayBuffer, optional parameters (byteOffset and length) can be used to limit the
     * section of the buffer referenced.
     */
    @Specialization(guards = {"isJSFunction(newTarget)", "isJSSharedArrayBuffer(arrayBuffer)"})
    protected DynamicObject doSharedArrayBuffer(DynamicObject newTarget, DynamicObject arrayBuffer, Object byteOffset0, Object length0,
                    @Cached("createBinaryProfile()") ConditionProfile lengthCondition) {
        return doDirectArrayBuffer(newTarget, arrayBuffer, byteOffset0, length0, lengthCondition);
    }

    /**
     * %TypedArray%(typedArray) (ES6 22.2.1.3).
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSFunction(newTarget)", "isJSArrayBufferView(arrayBufferView)"})
    protected DynamicObject doArrayBufferView(DynamicObject newTarget, DynamicObject arrayBufferView, Object byteOffset0, Object length0) {
        TypedArray sourceType = JSArrayBufferView.typedArrayGetArrayType(arrayBufferView);
        long length = sourceType.length(arrayBufferView);

        DynamicObject srcData = JSArrayBufferView.getArrayBuffer(arrayBufferView);
        DynamicObject defaultBufferConstructor = getContext().getRealm().getArrayBufferConstructor().getFunctionObject();
        DynamicObject bufferConstructor;
        if (JSSharedArrayBuffer.isJSSharedArrayBuffer(srcData)) {
            bufferConstructor = defaultBufferConstructor;
        } else {
            bufferConstructor = getArraySpeciesConstructorNode().speciesConstructor(srcData, defaultBufferConstructor);
        }
        DynamicObject arrayBuffer = createTypedArrayBuffer(length);
        JSObject.setPrototype(arrayBuffer, getPrototypeFromConstructorBuffer(bufferConstructor));

        checkDetachedBuffer(srcData);

        TypedArray typedArray = factory.createArrayType(getContext().isOptionDirectByteBuffer(), false);
        DynamicObject result = createTypedArray(arrayBuffer, typedArray, 0, (int) length, newTarget);

        assert typedArray == JSArrayBufferView.typedArrayGetArrayType(result);
        for (long i = 0; i < length; i++) {
            Object element = sourceType.getElement(arrayBufferView, i);
            typedArray.setElement(result, i, element, false);
        }
        return result;
    }

    protected ArraySpeciesConstructorNode getArraySpeciesConstructorNode() {
        if (arraySpeciesConstructorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            arraySpeciesConstructorNode = insert(ArraySpeciesConstructorNode.create(getContext(), true));
        }
        return arraySpeciesConstructorNode;
    }

    /**
     * %TypedArray%(object) (ES6 22.2.1.4).
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSFunction(newTarget)", "isJSArray(array)"})
    protected DynamicObject doArray(DynamicObject newTarget, DynamicObject array, Object byteOffset0, Object length0,
                    @Cached("createReadNode()") ReadElementNode readElementNode) {
        long length = JSAbstractArray.arrayGetArrayType(array).length(array);
        DynamicObject result = createTypedArrayWithLength(length, newTarget);
        assert length <= Integer.MAX_VALUE;

        TypedArray typedArray = factory.createArrayType(getContext().isOptionDirectByteBuffer(), false);
        assert typedArray == JSArrayBufferView.typedArrayGetArrayType(result);
        for (int i = 0; i < length; i++) {
            Object element = readElementNode.executeWithTargetAndIndex(array, i);
            typedArray.setElement(result, i, element, false);
        }

        return result;
    }

    /**
     * %TypedArray%() (ES6 22.2.1.1).
     *
     * This description applies only if the %TypedArray% function is called with no arguments.
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSFunction(newTarget)", "isUndefined(arg0)"})
    protected DynamicObject doEmpty(DynamicObject newTarget, DynamicObject arg0, Object byteOffset0, Object length0) {
        return createTypedArrayWithLength(0, newTarget);
    }

    /**
     * %TypedArray%(length) (ES6 22.2.1.2).
     *
     * This description applies only if the %TypedArray% function is called with at least one
     * argument and the Type of the first argument is not Object.
     */
    @Specialization(guards = {"isJSFunction(newTarget)", "!isJSObject(arg0)"})
    protected DynamicObject doLength(DynamicObject newTarget, Object arg0, @SuppressWarnings("unused") Object byteOffset0, @SuppressWarnings("unused") Object length0) {
        return createTypedArrayWithLength(toIndex(arg0), newTarget);
    }

    /**
     * %TypedArray%(object) (ES6 22.2.1.4).
     *
     * This description applies only if the %TypedArray% function is called with at least one
     * argument and the Type of the first argument is Object and that object does not have either a
     * [[TypedArrayName]] or an [[ArrayBufferData]] internal slot.
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"isJSFunction(newTarget)", "isJSObject(object)", "!isJSAbstractBuffer(object)", "!isJSArrayBufferView(object)", "!isJSArray(object)"})
    protected DynamicObject doObject(DynamicObject newTarget, DynamicObject object, Object byteOffset0, Object length0,
                    @Cached("createGetIteratorMethod()") GetMethodNode getIteratorMethodNode,
                    @Cached("createBinaryProfile()") ConditionProfile isIterableProfile,
                    @Cached("createWriteOwn()") WriteElementNode writeOwnNode,
                    @Cached("createCall()") JSFunctionCallNode iteratorCallNode,
                    @Cached("create()") IsObjectNode isObjectNode,
                    @Cached("create(getContext())") IteratorStepNode iteratorStepNode,
                    @Cached("create(getContext())") IteratorValueNode getIteratorValueNode,
                    @Cached("createGetLength()") JSGetLengthNode getLengthNode,
                    @Cached("create(getContext())") ReadElementNode readNode) {
        assert JSRuntime.isObject(object) && !JSArrayBufferView.isJSArrayBufferView(object) && !JSAbstractBuffer.isJSAbstractBuffer(object);

        DynamicObject proto = getPrototypeFromConstructorView(newTarget);
        assert JSRuntime.isObject(proto);

        Object usingIterator = getIteratorMethodNode.executeWithTarget(object);
        if (isIterableProfile.profile(usingIterator != Undefined.instance)) {
            ArrayList<Object> values = iterableToList(object, usingIterator, iteratorCallNode, isObjectNode, iteratorStepNode, getIteratorValueNode, this);

            int len = Boundaries.listSize(values);
            DynamicObject arrayBuffer = createTypedArrayBuffer(len);
            TypedArray typedArray = factory.createArrayType(getContext().isOptionDirectByteBuffer(), false);
            DynamicObject obj = integerIndexObjectCreate(arrayBuffer, typedArray, 0, len, proto);
            for (int k = 0; k < len; k++) {
                Object kValue = Boundaries.listGet(values, k);
                writeOwnNode.executeWithTargetAndIndexAndValue(obj, k, kValue);
            }
            return obj;
        }

        // NOTE: object is not an Iterable so assume it is already an array-like object.
        long len = getLengthNode.executeLong(object);
        DynamicObject arrayBuffer = createTypedArrayBuffer(len);
        assert len <= Integer.MAX_VALUE;
        TypedArray typedArray = factory.createArrayType(getContext().isOptionDirectByteBuffer(), false);
        DynamicObject obj = integerIndexObjectCreate(arrayBuffer, typedArray, 0, (int) len, proto);
        for (int k = 0; k < len; k++) {
            Object kValue = readNode.executeWithTargetAndIndex(object, k);
            writeOwnNode.executeWithTargetAndIndexAndValue(obj, k, kValue);
        }
        return obj;
    }

    private static ArrayList<Object> iterableToList(DynamicObject object, Object usingIterator, JSFunctionCallNode iteratorCallNode, IsObjectNode isObjectNode,
                    IteratorStepNode iteratorStepNode, IteratorValueNode getIteratorValueNode, JavaScriptBaseNode origin) {
        ArrayList<Object> values = new ArrayList<>();
        DynamicObject iterator = GetIteratorNode.getIterator(object, usingIterator, iteratorCallNode, isObjectNode, origin);
        while (true) {
            Object next = iteratorStepNode.execute(iterator);
            if (next instanceof Boolean && ((Boolean) next) == Boolean.FALSE) {
                break;
            }
            Object nextValue = getIteratorValueNode.execute((DynamicObject) next);
            Boundaries.listAdd(values, nextValue);
        }
        return values;
    }

    GetMethodNode createGetIteratorMethod() {
        return GetMethodNode.create(getContext(), null, Symbol.SYMBOL_ITERATOR);
    }

    WriteElementNode createWriteOwn() {
        return WriteElementNode.create(getContext(), true, true);
    }

    JSGetLengthNode createGetLength() {
        return JSGetLengthNode.create(getContext());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isJSFunction(newTarget)"})
    protected DynamicObject doUndefinedNewTarget(Object newTarget, Object arg0, Object byteOffset0, Object length0) {
        throw Errors.createTypeError("newTarget is not a function");
    }

    private DynamicObject createTypedArrayBuffer(long length) {
        assert length >= 0;
        int elementSize = factory.bytesPerElement();
        checkLengthLimit(length, elementSize);
        int byteLength = toByteLength((int) length, elementSize);
        assert length <= Integer.MAX_VALUE && byteLength >= 0 && byteLength <= Integer.MAX_VALUE;
        if (getContext().isOptionDirectByteBuffer()) {
            return JSArrayBuffer.createDirectArrayBuffer(getContext(), byteLength);
        } else {
            return JSArrayBuffer.createArrayBuffer(getContext(), byteLength);
        }
    }

    /**
     * TypedArray(unsigned long length).
     *
     * Create a new ArrayBuffer with enough bytes to hold length elements of this typed array, then
     * creates a typed array view referring to the full buffer. As with a directly constructed
     * ArrayBuffer, the contents are initialized to 0. If the requested number of bytes could not be
     * allocated an exception is raised.
     */
    private DynamicObject createTypedArrayWithLength(long length, DynamicObject newTarget) {
        DynamicObject arrayBuffer = createTypedArrayBuffer(length);
        TypedArray typedArray = factory.createArrayType(getContext().isOptionDirectByteBuffer(), false);
        return createTypedArray(arrayBuffer, typedArray, 0, (int) length, newTarget);
    }

    private DynamicObject createTypedArray(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject newTarget) {
        DynamicObject proto = getPrototypeFromConstructorView(newTarget);
        assert JSRuntime.isObject(proto);
        return integerIndexObjectCreate(arrayBuffer, typedArray, offset, length, proto);
    }

    private int checkLengthLimit(long length, int elementSize) {
        if (length > JSTruffleOptions.MaxTypedArrayLength / elementSize) {
            errorBranch.enter();
            throw throwInappropriateLengthError(length);
        }
        return (int) length;
    }

    private static int toByteLength(int length, int elementSize) {
        int byteLength = length * elementSize;
        assert byteLength >= 0 && byteLength / elementSize == length;
        return byteLength;
    }

    @TruffleBoundary
    private RuntimeException throwInappropriateLengthError(long length) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            throw Errors.createRangeError("inappropriate array buffer length: " + length);
        } else if (getContext().isOptionV8CompatibilityMode()) {
            throw Errors.createRangeError("Invalid array buffer length");
        } else {
            throw Errors.createRangeError("Invalid typed array length: " + length);
        }
    }

    private boolean rangeCheck(boolean condition, String message) {
        if (!condition) {
            errorBranch.enter();
            throw Errors.createRangeError(message);
        }
        return condition;
    }

    private boolean rangeCheckIsMultipleOfElementSize(boolean condition, String what, String name, int bytesPerElement) {
        if (!condition) {
            errorBranch.enter();
            throw createRangeErrorNotMultipleOfElementSize(what, name, bytesPerElement);
        }
        return condition;
    }

    @TruffleBoundary
    private static RuntimeException createRangeErrorNotMultipleOfElementSize(String what, String name, int bytesPerElement) {
        return Errors.createRangeError(String.format("%s of %s should be a multiple of %d", what, name, bytesPerElement));
    }

    abstract static class IntegerIndexedObjectCreateNode extends JavaScriptBaseNode {
        private final JSContext context;
        private final TypedArrayFactory factory;

        IntegerIndexedObjectCreateNode(JSContext context, TypedArrayFactory factory) {
            this.context = context;
            this.factory = factory;
        }

        abstract DynamicObject execute(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto);

        @Specialization(guards = "isDefaultPrototype(proto)")
        DynamicObject doDefaultProto(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, @SuppressWarnings("unused") DynamicObject proto) {
            assert !JSArrayBuffer.isDetachedBuffer(arrayBuffer);
            JSObjectFactory objectFactory = typedArray.isDirect() ? context.getDirectArrayBufferViewFactory(factory) : context.getArrayBufferViewFactory(factory);
            return JSArrayBufferView.createArrayBufferView(context, objectFactory, arrayBuffer, typedArray, offset, length);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isDefaultPrototype(proto)", "proto == cachedProto"}, limit = "1")
        DynamicObject doCachedProto(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto,
                        @Cached("proto") DynamicObject cachedProto,
                        @Cached("makeObjectFactory(cachedProto, typedArray)") JSObjectFactory objectFactory) {
            return JSArrayBufferView.createArrayBufferView(context, objectFactory, arrayBuffer, typedArray, offset, length);
        }

        @Specialization(guards = "!isDefaultPrototype(proto)", replaces = "doCachedProto")
        DynamicObject doUncachedProto(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto) {
            return JSArrayBufferView.createArrayBufferView(context, makeObjectFactory(proto, typedArray), arrayBuffer, typedArray, offset, length);
        }

        boolean isDefaultPrototype(DynamicObject proto) {
            return proto == context.getRealm().getArrayBufferViewConstructor(factory).getPrototype();
        }

        @TruffleBoundary
        JSObjectFactory makeObjectFactory(DynamicObject prototype, TypedArray typedArray) {
            return JSObjectFactory.createBound(context, prototype, JSArrayBufferView.makeInitialArrayBufferViewShape(context, prototype, typedArray.isDirect()).createFactory());
        }
    }
}
