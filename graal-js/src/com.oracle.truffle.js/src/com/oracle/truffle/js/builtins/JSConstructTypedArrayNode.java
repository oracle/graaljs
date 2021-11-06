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

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltins.ArraySpeciesConstructorNode;
import com.oracle.truffle.js.builtins.JSConstructTypedArrayNodeGen.IntegerIndexedObjectCreateNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeFromConstructorNode;
import com.oracle.truffle.js.nodes.access.IsJSObjectNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArrayFactory;
import com.oracle.truffle.js.runtime.builtins.JSAbstractBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * The %TypedArray% intrinsic constructor function object (ES6 22.2.1).
 */
@ImportStatic({JSArrayBuffer.class, JSRuntime.class, JSConfig.class})
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
            getPrototypeFromConstructorViewNode = insert(GetPrototypeFromConstructorNode.create(getContext(), null, realm -> realm.getArrayBufferViewPrototype(factory)));
        }
        return getPrototypeFromConstructorViewNode.executeWithConstructor(newTarget);
    }

    private DynamicObject getPrototypeFromConstructorBuffer(DynamicObject newTarget) {
        if (getPrototypeFromConstructorBufferNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getPrototypeFromConstructorBufferNode = insert(GetPrototypeFromConstructorNode.create(getContext(), null, JSRealm::getArrayBufferPrototype));
        }
        return getPrototypeFromConstructorBufferNode.executeWithConstructor(newTarget);
    }

    private DynamicObject integerIndexedObjectCreate(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto) {
        if (integerIndexObjectCreateNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            integerIndexObjectCreateNode = insert(IntegerIndexedObjectCreateNodeGen.create(getContext(), factory));
        }
        return integerIndexObjectCreateNode.execute(arrayBuffer, typedArray, offset, length, proto);
    }

    protected final ReadElementNode createReadNode() {
        return ReadElementNode.create(getContext());
    }

    private void checkDetachedBuffer(DynamicObject buffer) {
        if (!getContext().getTypedArrayNotDetachedAssumption().isValid() && JSArrayBuffer.isDetachedBuffer(buffer)) {
            throw Errors.createTypeErrorDetachedBuffer();
        }
    }

    /**
     * %TypedArray%(buffer[, byteOffset[, length]]).
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
    @Specialization(guards = {"!isUndefined(newTarget)", "isJSHeapArrayBuffer(arrayBuffer)"})
    protected DynamicObject doArrayBuffer(DynamicObject newTarget, DynamicObject arrayBuffer, Object byteOffset0, Object length0,
                    @Cached("createBinaryProfile()") @Shared("lengthIsUndefined") ConditionProfile lengthIsUndefined) {
        checkDetachedBuffer(arrayBuffer);
        byte[] byteArray = JSArrayBuffer.getByteArray(arrayBuffer);
        int arrayBufferLength = byteArray.length;
        return doArrayBufferImpl(arrayBuffer, byteOffset0, length0, newTarget, arrayBufferLength, false, false, lengthIsUndefined);
    }

    @Specialization(guards = {"!isUndefined(newTarget)", "isJSDirectArrayBuffer(arrayBuffer)"})
    protected DynamicObject doDirectArrayBuffer(DynamicObject newTarget, DynamicObject arrayBuffer, Object byteOffset0, Object length0,
                    @Cached("createBinaryProfile()") @Shared("lengthIsUndefined") ConditionProfile lengthIsUndefined) {
        checkDetachedBuffer(arrayBuffer);
        ByteBuffer byteBuffer = JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        int arrayBufferLength = byteBuffer.limit();
        return doArrayBufferImpl(arrayBuffer, byteOffset0, length0, newTarget, arrayBufferLength, true, false, lengthIsUndefined);
    }

    private DynamicObject doArrayBufferImpl(DynamicObject arrayBuffer, Object byteOffset0, Object length0, DynamicObject newTarget, long bufferByteLength,
                    boolean direct, boolean isInteropBuffer, ConditionProfile lengthIsUndefinedProfile) {
        final int elementSize = factory.getBytesPerElement();

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
        TypedArray typedArray = factory.createArrayType(direct, byteOffset != 0, isInteropBuffer);
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
    @Specialization(guards = {"!isUndefined(newTarget)", "isJSSharedArrayBuffer(arrayBuffer)"})
    protected DynamicObject doSharedArrayBuffer(DynamicObject newTarget, DynamicObject arrayBuffer, Object byteOffset0, Object length0,
                    @Cached("createBinaryProfile()") @Shared("lengthIsUndefined") ConditionProfile lengthIsUndefined) {
        return doDirectArrayBuffer(newTarget, arrayBuffer, byteOffset0, length0, lengthIsUndefined);
    }

    /**
     * TypedArray(ArrayBuffer buffer, optional unsigned long byteOffset, optional unsigned long
     * length).
     *
     * Create a new TypedArray object using the passed InteropArrayBuffer for its storage. As with
     * standard ArrayBuffer, optional parameters (byteOffset and length) can be used to limit the
     * section of the buffer referenced.
     */
    @Specialization(guards = {"!isUndefined(newTarget)", "isJSInteropArrayBuffer(arrayBuffer)"})
    protected DynamicObject doInteropArrayBuffer(DynamicObject newTarget, DynamicObject arrayBuffer, Object byteOffset0, Object length0,
                    @Cached("createBinaryProfile()") @Shared("lengthIsUndefined") ConditionProfile lengthIsUndefined,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        Object buffer = JSArrayBuffer.getInteropBuffer(arrayBuffer);
        long arrayBufferLength = getBufferSizeSafe(buffer, interop);
        return doArrayBufferImpl(arrayBuffer, byteOffset0, length0, newTarget, arrayBufferLength, false, true, lengthIsUndefined);
    }

    /**
     * %TypedArray%(typedArray).
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"!isUndefined(newTarget)", "isJSArrayBufferView(arrayBufferView)"})
    protected DynamicObject doArrayBufferView(DynamicObject newTarget, DynamicObject arrayBufferView, Object byteOffset0, Object length0) {
        TypedArray sourceType = JSArrayBufferView.typedArrayGetArrayType(arrayBufferView);
        long length = sourceType.length(arrayBufferView);

        DynamicObject srcData = JSArrayBufferView.getArrayBuffer(arrayBufferView);
        DynamicObject defaultBufferConstructor = getRealm().getArrayBufferConstructor();
        DynamicObject bufferConstructor;
        if (JSSharedArrayBuffer.isJSSharedArrayBuffer(srcData)) {
            bufferConstructor = defaultBufferConstructor;
        } else {
            bufferConstructor = getArraySpeciesConstructorNode().speciesConstructor(srcData, defaultBufferConstructor);
        }
        DynamicObject arrayBuffer = createTypedArrayBuffer(length);
        JSObject.setPrototype(arrayBuffer, getPrototypeFromConstructorBuffer(bufferConstructor));

        checkDetachedBuffer(srcData);

        boolean elementTypeIsBig = JSRuntime.isTypedArrayBigIntFactory(factory);
        boolean sourceTypeIsBig = sourceType instanceof TypedArray.TypedBigIntArray;
        if (elementTypeIsBig != sourceTypeIsBig) {
            throw Errors.createTypeErrorCannotMixBigIntWithOtherTypes(this);
        }

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
     * %TypedArray%().
     *
     * This description applies only if the %TypedArray% function is called with no arguments.
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"!isUndefined(newTarget)", "isUndefined(arg0)"})
    protected DynamicObject doEmpty(DynamicObject newTarget, DynamicObject arg0, Object byteOffset0, Object length0) {
        return createTypedArrayWithLength(0, newTarget);
    }

    /**
     * %TypedArray%(length).
     */
    @Specialization(guards = {"!isUndefined(newTarget)", "length >= 0"})
    protected DynamicObject doIntLength(DynamicObject newTarget, int length, @SuppressWarnings("unused") Object byteOffset0, @SuppressWarnings("unused") Object length0) {
        return createTypedArrayWithLength(length, newTarget);
    }

    /**
     * %TypedArray%(length).
     *
     * This description applies only if the %TypedArray% function is called with at least one
     * argument and the Type of the first argument is not Object.
     */
    @Specialization(guards = {"!isUndefined(newTarget)", "!isJSObject(arg0)", "!isForeignObject(arg0)"}, replaces = "doIntLength")
    protected DynamicObject doLength(DynamicObject newTarget, Object arg0, @SuppressWarnings("unused") Object byteOffset0, @SuppressWarnings("unused") Object length0) {
        return createTypedArrayWithLength(toIndex(arg0), newTarget);
    }

    /**
     * %TypedArray%(object).
     *
     * This description applies only if the %TypedArray% function is called with at least one
     * argument and the Type of the first argument is Object and that object does not have either a
     * [[TypedArrayName]] or an [[ArrayBufferData]] internal slot.
     */
    @SuppressWarnings("unused")
    @Specialization(guards = {"!isUndefined(newTarget)", "isJSObject(object)", "!isJSAbstractBuffer(object)", "!isJSArrayBufferView(object)"})
    protected DynamicObject doObject(DynamicObject newTarget, DynamicObject object, Object byteOffset0, Object length0,
                    @Cached("createGetIteratorMethod()") GetMethodNode getIteratorMethodNode,
                    @Cached("createBinaryProfile()") ConditionProfile isIterableProfile,
                    @Cached("createWriteOwn()") WriteElementNode writeOwnNode,
                    @Cached("createCall()") JSFunctionCallNode iteratorCallNode,
                    @Cached("create()") IsJSObjectNode isObjectNode,
                    @Cached("create(getContext())") IteratorStepNode iteratorStepNode,
                    @Cached("create(getContext())") IteratorValueNode getIteratorValueNode,
                    @Cached("createGetLength()") JSGetLengthNode getLengthNode,
                    @Cached("create(getContext())") ReadElementNode readNode,
                    @Cached("create(NEXT, getContext())") PropertyGetNode getNextMethodNode,
                    @Cached("create()") BranchProfile growProfile) {
        assert JSRuntime.isObject(object) && !JSArrayBufferView.isJSArrayBufferView(object) && !JSAbstractBuffer.isJSAbstractBuffer(object);

        DynamicObject proto = getPrototypeFromConstructorView(newTarget);
        assert JSRuntime.isObject(proto);

        Object usingIterator = getIteratorMethodNode.executeWithTarget(object);
        if (isIterableProfile.profile(usingIterator != Undefined.instance)) {
            SimpleArrayList<Object> values = GetIteratorNode.iterableToList(object, usingIterator, iteratorCallNode, isObjectNode, iteratorStepNode, getIteratorValueNode, getNextMethodNode, this,
                            growProfile);
            int len = values.size();
            DynamicObject arrayBuffer = createTypedArrayBuffer(len);
            TypedArray typedArray = factory.createArrayType(getContext().isOptionDirectByteBuffer(), false);
            DynamicObject obj = integerIndexedObjectCreate(arrayBuffer, typedArray, 0, len, proto);
            for (int k = 0; k < len; k++) {
                Object kValue = values.get(k);
                writeOwnNode.executeWithTargetAndIndexAndValue(obj, k, kValue);
            }
            return obj;
        }

        // NOTE: object is not an Iterable so assume it is already an array-like object.
        long len = getLengthNode.executeLong(object);
        DynamicObject arrayBuffer = createTypedArrayBuffer(len);
        assert len <= Integer.MAX_VALUE;
        TypedArray typedArray = factory.createArrayType(getContext().isOptionDirectByteBuffer(), false);
        DynamicObject obj = integerIndexedObjectCreate(arrayBuffer, typedArray, 0, (int) len, proto);
        for (int k = 0; k < len; k++) {
            Object kValue = readNode.executeWithTargetAndIndex(object, k);
            writeOwnNode.executeWithTargetAndIndexAndValue(obj, k, kValue);
        }
        return obj;
    }

    @Specialization(guards = {"!isUndefined(newTarget)", "isForeignObject(object)"}, limit = "InteropLibraryLimit")
    protected DynamicObject doForeignObject(DynamicObject newTarget, Object object, Object byteOffset0, Object length0,
                    @CachedLibrary("object") InteropLibrary interop,
                    @Cached("createWriteOwn()") WriteElementNode writeOwnNode,
                    @Cached ImportValueNode importValue,
                    @Cached("createBinaryProfile()") ConditionProfile lengthIsUndefined) {
        if (interop.hasBufferElements(object)) {
            DynamicObject arrayBuffer = JSArrayBuffer.createInteropArrayBuffer(getContext(), getRealm(), object);
            long bufferByteLength = getBufferSizeSafe(object, interop);
            return doArrayBufferImpl(arrayBuffer, byteOffset0, length0, newTarget, bufferByteLength, false, true, lengthIsUndefined);
        }

        long length;
        boolean fromArray = interop.hasArrayElements(object);
        if (fromArray) {
            length = toIndex(JSInteropUtil.getArraySize(object, interop, this));
        } else if (interop.fitsInInt(object)) {
            try {
                length = toIndex(interop.asInt(object));
            } catch (UnsupportedMessageException e) {
                length = 0;
            }
        } else {
            length = 0;
        }

        DynamicObject obj = createTypedArrayWithLength(length, newTarget);
        if (fromArray) {
            assert length <= Integer.MAX_VALUE;
            for (int k = 0; k < length; k++) {
                Object kValue = JSInteropUtil.readArrayElementOrDefault(object, k, 0, interop, importValue, this);
                writeOwnNode.executeWithTargetAndIndexAndValue(obj, k, kValue);
            }
        }
        return obj;
    }

    private static long getBufferSizeSafe(Object object, InteropLibrary interop) {
        try {
            return interop.getBufferSize(object);
        } catch (UnsupportedMessageException e) {
            return 0;
        }
    }

    GetMethodNode createGetIteratorMethod() {
        return GetMethodNode.create(getContext(), Symbol.SYMBOL_ITERATOR);
    }

    WriteElementNode createWriteOwn() {
        return WriteElementNode.create(getContext(), true, true);
    }

    JSGetLengthNode createGetLength() {
        return JSGetLengthNode.create(getContext());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isUndefined(newTarget)"})
    protected DynamicObject doUndefinedNewTarget(Object newTarget, Object arg0, Object byteOffset0, Object length0) {
        throw Errors.createTypeError("newTarget is not a function");
    }

    private DynamicObject createTypedArrayBuffer(long length) {
        assert length >= 0;
        int elementSize = factory.getBytesPerElement();
        checkLengthLimit(length, elementSize);
        int byteLength = toByteLength((int) length, elementSize);
        assert length <= Integer.MAX_VALUE && byteLength >= 0 && byteLength <= Integer.MAX_VALUE;
        JSRealm realm = getRealm();
        if (getContext().isOptionDirectByteBuffer()) {
            return JSArrayBuffer.createDirectArrayBuffer(getContext(), realm, byteLength);
        } else {
            return JSArrayBuffer.createArrayBuffer(getContext(), realm, byteLength);
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
        return integerIndexedObjectCreate(arrayBuffer, typedArray, offset, length, proto);
    }

    private int checkLengthLimit(long length, int elementSize) {
        if (length > getContext().getContextOptions().getMaxTypedArrayLength() / elementSize) {
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
        if (getContext().isOptionNashornCompatibilityMode()) {
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
        final JSContext context;
        final TypedArrayFactory factory;

        IntegerIndexedObjectCreateNode(JSContext context, TypedArrayFactory factory) {
            this.context = context;
            this.factory = factory;
        }

        abstract DynamicObject execute(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto);

        @Specialization(guards = "isDefaultPrototype(proto)")
        DynamicObject doDefaultProto(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, @SuppressWarnings("unused") DynamicObject proto) {
            JSObjectFactory objectFactory = context.getArrayBufferViewFactory(factory);
            return JSArrayBufferView.createArrayBufferView(context, getRealm(), objectFactory, arrayBuffer, typedArray, offset, length);
        }

        @Specialization(guards = {"!isDefaultPrototype(proto)", "context.isMultiContext()"})
        DynamicObject doMultiContext(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto) {
            JSObjectFactory objectFactory = context.getArrayBufferViewFactory(factory);
            return JSArrayBufferView.createArrayBufferViewWithProto(context, getRealm(), objectFactory, arrayBuffer, typedArray, offset, length, proto);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isDefaultPrototype(proto)", "!context.isMultiContext()", "proto == cachedProto"}, limit = "1")
        DynamicObject doCachedProto(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto,
                        @Cached("proto") DynamicObject cachedProto,
                        @Cached("makeObjectFactory(cachedProto)") JSObjectFactory objectFactory) {
            return JSArrayBufferView.createArrayBufferView(context, getRealm(), objectFactory, arrayBuffer, typedArray, offset, length);
        }

        @Specialization(guards = {"!isDefaultPrototype(proto)", "!context.isMultiContext()"}, replaces = "doCachedProto")
        DynamicObject doUncachedProto(DynamicObject arrayBuffer, TypedArray typedArray, int offset, int length, DynamicObject proto) {
            return JSArrayBufferView.createArrayBufferView(context, getRealm(), makeObjectFactory(proto), arrayBuffer, typedArray, offset, length);
        }

        boolean isDefaultPrototype(DynamicObject proto) {
            return proto == getRealm().getArrayBufferViewPrototype(factory);
        }

        @TruffleBoundary
        JSObjectFactory makeObjectFactory(DynamicObject prototype) {
            return JSObjectFactory.createBound(context, prototype, JSObjectUtil.getProtoChildShape(prototype, JSArrayBufferView.INSTANCE, context));
        }
    }
}
