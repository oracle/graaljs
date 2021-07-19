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
package com.oracle.truffle.js.nodes.access;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.LanguageReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementTag;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedBigIntArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedFloatArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedIntArray;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint8ClampedArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractConstantArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractContiguousDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractContiguousIntArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractContiguousJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractContiguousObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractIntArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractWritableArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyArray;
import com.oracle.truffle.js.runtime.array.dyn.ContiguousIntArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesIntArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultIndicesArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSSlowArgumentsArray;
import com.oracle.truffle.js.runtime.builtins.JSSlowArray;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

public class WriteElementNode extends JSTargetableNode {
    @Child protected JavaScriptNode targetNode;
    @Child protected JavaScriptNode indexNode;
    @Child private ToArrayIndexNode toArrayIndexNode;
    @Child protected JavaScriptNode valueNode;
    @Child private WriteElementTypeCacheNode typeCacheNode;
    @Child private RequireObjectCoercibleNode requireObjectCoercibleNode;

    final JSContext context;
    final boolean isStrict;
    final boolean writeOwn;
    @CompilationFinal private byte indexState;
    private static final byte INDEX_INT = 1;
    private static final byte INDEX_OBJECT = 2;

    public static WriteElementNode create(JSContext context, boolean isStrict) {
        return create(null, null, null, context, isStrict, false);
    }

    public static WriteElementNode create(JSContext context, boolean isStrict, boolean writeOwn) {
        return create(null, null, null, context, isStrict, writeOwn);
    }

    public static WriteElementNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSContext context, boolean isStrict) {
        return create(targetNode, indexNode, valueNode, context, isStrict, false);
    }

    private static WriteElementNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSContext context, boolean isStrict, boolean writeOwn) {
        return new WriteElementNode(targetNode, indexNode, valueNode, context, isStrict, writeOwn);
    }

    protected WriteElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSContext context, boolean isStrict, boolean writeOwn) {
        // ToPropertyKey conversion should not be performed by indexNode
        // (we need to RequireObjectCoercible(target) before this conversion)
        assert !(indexNode instanceof JSToPropertyKeyWrapperNode);

        this.targetNode = targetNode;
        this.indexNode = indexNode;
        this.valueNode = valueNode;
        this.context = context;
        this.isStrict = isStrict;
        this.writeOwn = writeOwn;
        this.requireObjectCoercibleNode = RequireObjectCoercibleNode.create();
    }

    protected final Object toArrayIndex(Object index) {
        if (toArrayIndexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toArrayIndexNode = insert(ToArrayIndexNode.createNoStringToIndex());
        }
        return toArrayIndexNode.execute(index);
    }

    protected final void requireObjectCoercible(Object target, int index) {
        try {
            requireObjectCoercibleNode.executeVoid(target);
        } catch (JSException e) {
            throw Errors.createTypeErrorCannotSetProperty(JSRuntime.safeToString(index), target, this);
        }
    }

    protected final void requireObjectCoercible(Object target, Object index) {
        try {
            requireObjectCoercibleNode.executeVoid(target);
        } catch (JSException e) {
            throw Errors.createTypeErrorCannotSetProperty(JSRuntime.safeToString(index), target, this);
        }
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == WriteElementTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializationNeeded() && materializedTags.contains(WriteElementTag.class)) {
            JavaScriptNode clonedTarget = targetNode == null || targetNode.hasSourceSection() ? targetNode : JSTaggedExecutionNode.createForInput(targetNode, this, materializedTags);
            JavaScriptNode clonedIndex = indexNode == null || indexNode.hasSourceSection() ? indexNode : JSTaggedExecutionNode.createForInput(indexNode, this, materializedTags);
            JavaScriptNode clonedValue = valueNode == null || valueNode.hasSourceSection() ? valueNode : JSTaggedExecutionNode.createForInput(valueNode, this, materializedTags);
            if (clonedTarget == targetNode && clonedIndex == indexNode && clonedValue == valueNode) {
                return this;
            }
            if (clonedTarget == targetNode) {
                clonedTarget = cloneUninitialized(targetNode, materializedTags);
            }
            if (clonedIndex == indexNode) {
                clonedIndex = cloneUninitialized(indexNode, materializedTags);
            }
            if (clonedValue == valueNode) {
                clonedValue = cloneUninitialized(valueNode, materializedTags);
            }
            WriteElementNode cloned = createMaterialized(clonedTarget, clonedIndex, clonedValue);
            transferSourceSectionAndTags(this, cloned);
            return cloned;
        }
        return this;
    }

    private boolean materializationNeeded() {
        // Materialization is needed when source sections are missing.
        return (targetNode != null && !targetNode.hasSourceSection()) || (indexNode != null && !indexNode.hasSourceSection()) || (valueNode != null && !valueNode.hasSourceSection());
    }

    protected WriteElementNode createMaterialized(JavaScriptNode newTarget, JavaScriptNode newIndex, JavaScriptNode newValue) {
        return WriteElementNode.create(newTarget, newIndex, newValue, getContext(), isStrict(), writeOwn());
    }

    @Override
    public Object evaluateTarget(VirtualFrame frame) {
        return targetNode.execute(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object target = evaluateTarget(frame);
        return executeWithTarget(frame, target, evaluateReceiver(targetNode, frame, target));
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        Object target = evaluateTarget(frame);
        return executeWithTargetInt(frame, target, evaluateReceiver(targetNode, frame, target));
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        Object target = evaluateTarget(frame);
        return executeWithTargetDouble(frame, target, evaluateReceiver(targetNode, frame, target));
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        return executeWithTarget(frame, target, target);
    }

    public Object executeWithTarget(VirtualFrame frame, Object target, Object receiver) {
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            requireObjectCoercible(target, index);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndex(frame, target, (int) index, receiver);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(frame, target, toArrayIndex(index), receiver);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                requireObjectCoercible(target, e.getResult());
                return executeWithTargetAndIndex(frame, target, toArrayIndex(e.getResult()), receiver);
            }
            requireObjectCoercible(target, index);
            return executeWithTargetAndIndex(frame, target, index, receiver);
        } else {
            assert is == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            requireObjectCoercible(target, index);
            return executeWithTargetAndIndex(frame, target, toArrayIndex(index), receiver);
        }
    }

    public int executeWithTargetInt(VirtualFrame frame, Object target, Object receiver) throws UnexpectedResultException {
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            requireObjectCoercible(target, index);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexInt(frame, target, (int) index, receiver);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(frame, target, toArrayIndex(index), receiver);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                requireObjectCoercible(target, e.getResult());
                return executeWithTargetAndIndexInt(frame, target, toArrayIndex(e.getResult()), receiver);
            }
            requireObjectCoercible(target, index);
            return executeWithTargetAndIndexInt(frame, target, index, receiver);
        } else {
            assert is == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            requireObjectCoercible(target, index);
            return executeWithTargetAndIndexInt(frame, target, toArrayIndex(index), receiver);
        }
    }

    public double executeWithTargetDouble(VirtualFrame frame, Object target, Object receiver) throws UnexpectedResultException {
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            requireObjectCoercible(target, index);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexDouble(frame, target, (int) index, receiver);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(frame, target, toArrayIndex(index), receiver);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                requireObjectCoercible(target, e.getResult());
                return executeWithTargetAndIndexDouble(frame, target, toArrayIndex(e.getResult()), receiver);
            }
            requireObjectCoercible(target, index);
            return executeWithTargetAndIndexDouble(frame, target, index, receiver);
        } else {
            assert is == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            requireObjectCoercible(target, index);
            return executeWithTargetAndIndexDouble(frame, target, toArrayIndex(index), receiver);
        }
    }

    protected Object executeWithTargetAndIndex(VirtualFrame frame, Object target, Object index, Object receiver) {
        Object value = valueNode.execute(frame);
        executeWithTargetAndIndexAndValue(target, index, value, receiver);
        return value;
    }

    protected Object executeWithTargetAndIndex(VirtualFrame frame, Object target, int index, Object receiver) {
        Object value = valueNode.execute(frame);
        executeWithTargetAndIndexAndValue(target, index, value, receiver);
        return value;
    }

    protected int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, Object index, Object receiver) throws UnexpectedResultException {
        try {
            int value = valueNode.executeInt(frame);
            executeWithTargetAndIndexAndValue(target, index, value, receiver);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult(), receiver);
            throw e;
        }
    }

    protected int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, int index, Object receiver) throws UnexpectedResultException {
        try {
            int value = valueNode.executeInt(frame);
            executeWithTargetAndIndexAndValue(target, index, (Object) value, receiver);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult(), receiver);
            throw e;
        }
    }

    protected double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, Object index, Object receiver) throws UnexpectedResultException {
        try {
            double value = valueNode.executeDouble(frame);
            executeWithTargetAndIndexAndValue(target, index, value, receiver);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult(), receiver);
            throw e;
        }
    }

    protected double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, int index, Object receiver) throws UnexpectedResultException {
        try {
            double value = valueNode.executeDouble(frame);
            executeWithTargetAndIndexAndValue(target, index, (Object) value, receiver);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult(), receiver);
            throw e;
        }
    }

    public final void executeWithTargetAndIndexAndValue(Object target, Object index, Object value) {
        executeWithTargetAndIndexAndValue(target, index, value, target);
    }

    public final void executeWithTargetAndIndexAndValue(Object target, int index, Object value) {
        executeWithTargetAndIndexAndValue(target, index, value, target);
    }

    public final void executeWithTargetAndIndexAndValue(Object target, long index, Object value) {
        executeWithTargetAndIndexAndValue(target, index, value, target);
    }

    @ExplodeLoop
    public final void executeWithTargetAndIndexAndValue(Object target, Object index, Object value, Object receiver) {
        for (WriteElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                c.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
                return;
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        WriteElementTypeCacheNode specialization = specialize(target);
        specialization.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
    }

    @ExplodeLoop
    public final void executeWithTargetAndIndexAndValue(Object target, int index, Object value, Object receiver) {
        for (WriteElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                c.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
                return;
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        WriteElementTypeCacheNode specialization = specialize(target);
        specialization.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
    }

    @ExplodeLoop
    public final void executeWithTargetAndIndexAndValue(Object target, long index, Object value, Object receiver) {
        for (WriteElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                c.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
                return;
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        WriteElementTypeCacheNode specialization = specialize(target);
        specialization.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
    }

    private WriteElementTypeCacheNode specialize(Object target) {
        CompilerAsserts.neverPartOfCompilation();
        Lock lock = getLock();
        lock.lock();
        try {
            WriteElementTypeCacheNode currentHead = typeCacheNode;
            for (WriteElementTypeCacheNode c = currentHead; c != null; c = c.typeCacheNext) {
                if (c.guard(target)) {
                    return c;
                }
            }

            WriteElementTypeCacheNode newCacheNode = makeTypeCacheNode(target, currentHead);
            insert(newCacheNode);
            typeCacheNode = newCacheNode;
            if (!newCacheNode.guard(target)) {
                throw Errors.shouldNotReachHere();
            }
            return newCacheNode;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private static WriteElementTypeCacheNode makeTypeCacheNode(Object target, WriteElementTypeCacheNode next) {
        if (JSDynamicObject.isJSDynamicObject(target)) {
            return new JSObjectWriteElementTypeCacheNode(next);
        } else if (JSRuntime.isString(target)) {
            return new StringWriteElementTypeCacheNode(target.getClass(), next);
        } else if (target instanceof Boolean) {
            return new BooleanWriteElementTypeCacheNode(next);
        } else if (target instanceof Number) {
            return new NumberWriteElementTypeCacheNode(target.getClass(), next);
        } else if (target instanceof Symbol) {
            return new SymbolWriteElementTypeCacheNode(next);
        } else if (target instanceof BigInt) {
            return new BigIntWriteElementTypeCacheNode(next);
        } else if (target instanceof TruffleObject) {
            assert JSRuntime.isForeignObject(target);
            return new TruffleObjectWriteElementTypeCacheNode(target.getClass(), next);
        } else {
            assert JSRuntime.isJavaPrimitive(target);
            return new JavaObjectWriteElementTypeCacheNode(target.getClass(), next);
        }
    }

    abstract static class WriteElementTypeCacheNode extends JavaScriptBaseNode {
        @Child WriteElementTypeCacheNode typeCacheNext;

        protected WriteElementTypeCacheNode(WriteElementTypeCacheNode next) {
            this.typeCacheNext = next;
        }

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root);

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, int index, Object value, Object receiver, WriteElementNode root);

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root);

        public abstract boolean guard(Object target);
    }

    private static class JSObjectWriteElementTypeCacheNode extends WriteElementTypeCacheNode {
        @Child private IsArrayNode isArrayNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        @Child private ArrayWriteElementCacheNode arrayWriteElementNode;
        @Child private IsJSObjectNode isObjectNode;
        private final ConditionProfile intOrStringIndexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayProfile = ConditionProfile.createBinaryProfile();
        private final JSClassProfile jsclassProfile = JSClassProfile.create();
        @Child private CachedSetPropertyNode setPropertyCachedNode;

        JSObjectWriteElementTypeCacheNode(WriteElementTypeCacheNode next) {
            super(next);
            this.isArrayNode = IsArrayNode.createIsFastOrTypedArray();
            this.isObjectNode = IsJSObjectNode.createIncludeNullUndefined();
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
            JSDynamicObject targetObject = ((JSDynamicObject) target);
            if (arrayProfile.profile(isArrayNode.execute(targetObject))) {
                ScriptArray array = JSObject.getArray(targetObject);
                Object objIndex = toArrayIndex(index);

                if (intOrStringIndexProfile.profile(objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    if (!executeSetArray(targetObject, array, longIndex, value, root)) {
                        setPropertyGenericEvaluatedIndex(targetObject, longIndex, value, receiver, root);
                    }
                } else {
                    setPropertyGenericEvaluatedStringOrSymbol(targetObject, objIndex, value, receiver, root);
                }
            } else {
                setPropertyGeneric(targetObject, index, value, receiver, root);
            }
        }

        private Object toArrayIndex(Object index) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(index);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value, Object receiver, WriteElementNode root) {
            JSDynamicObject targetObject = ((JSDynamicObject) target);
            if (arrayProfile.profile(isArrayNode.execute(targetObject))) {
                ScriptArray array = JSObject.getArray(targetObject);

                if (intOrStringIndexProfile.profile(JSRuntime.isArrayIndex(index))) {
                    if (!executeSetArray(targetObject, array, index, value, root)) {
                        setPropertyGenericEvaluatedIndex(targetObject, index, value, receiver, root);
                    }
                } else {
                    setPropertyGenericEvaluatedStringOrSymbol(targetObject, Boundaries.stringValueOf(index), value, receiver, root);
                }
            } else {
                setPropertyGeneric(targetObject, index, value, receiver, root);
            }
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            JSDynamicObject targetObject = ((JSDynamicObject) target);
            if (arrayProfile.profile(isArrayNode.execute(targetObject))) {
                ScriptArray array = JSObject.getArray(targetObject);

                if (intOrStringIndexProfile.profile(JSRuntime.isArrayIndex(index))) {
                    if (!executeSetArray(targetObject, array, index, value, root)) {
                        setPropertyGenericEvaluatedIndex(targetObject, index, value, receiver, root);
                    }
                } else {
                    setPropertyGenericEvaluatedStringOrSymbol(targetObject, Boundaries.stringValueOf(index), value, receiver, root);
                }
            } else {
                setPropertyGeneric(targetObject, index, value, receiver, root);
            }
        }

        private void setPropertyGenericEvaluatedIndex(DynamicObject targetObject, long index, Object value, Object receiver, WriteElementNode root) {
            JSObject.setWithReceiver(targetObject, index, value, receiver, root.isStrict, jsclassProfile, root);
        }

        private void setPropertyGenericEvaluatedStringOrSymbol(DynamicObject targetObject, Object key, Object value, Object receiver, WriteElementNode root) {
            JSObject.setWithReceiver(targetObject, key, value, receiver, root.isStrict, jsclassProfile, root);
        }

        private void setPropertyGeneric(DynamicObject targetObject, Object index, Object value, Object receiver, WriteElementNode root) {
            setCachedProperty(targetObject, index, value, receiver, root);
        }

        private void setCachedProperty(DynamicObject targetObject, Object index, Object value, Object receiver, WriteElementNode root) {
            if (setPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setPropertyCachedNode = insert(CachedSetPropertyNode.create(root.context, root.isStrict, root.writeOwn, root.isSuperProperty()));
            }
            setPropertyCachedNode.execute(targetObject, index, value, receiver);
        }

        @Override
        public boolean guard(Object target) {
            return isObjectNode.executeBoolean(target);
        }

        @ExplodeLoop
        private boolean executeSetArray(DynamicObject targetObject, ScriptArray array, long index, Object value, WriteElementNode root) {
            for (ArrayWriteElementCacheNode c = arrayWriteElementNode; c != null; c = c.arrayCacheNext) {
                boolean guard = c.guard(targetObject, array);
                if (guard) {
                    return c.executeSetArray(targetObject, array, index, value, root);
                }
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayWriteElementCacheNode specialization = specialize(targetObject, array);
            return specialization.executeSetArray(targetObject, array, index, value, root);
        }

        private ArrayWriteElementCacheNode specialize(DynamicObject target, ScriptArray array) {
            CompilerAsserts.neverPartOfCompilation();
            Lock lock = getLock();
            lock.lock();
            try {
                ArrayWriteElementCacheNode currentHead = arrayWriteElementNode;
                for (ArrayWriteElementCacheNode c = currentHead; c != null; c = c.arrayCacheNext) {
                    if (c.guard(target, array)) {
                        return c;
                    }
                }

                currentHead = purgeStaleCacheEntries(currentHead, target);

                ArrayWriteElementCacheNode newCacheNode = makeArrayCacheNode(target, array, currentHead);
                insert(newCacheNode);
                arrayWriteElementNode = newCacheNode;
                if (currentHead != null && currentHead.arrayCacheNext != null && currentHead.arrayCacheNext.arrayCacheNext != null) {
                    reportPolymorphicSpecialize();
                }
                if (!newCacheNode.guard(target, array)) {
                    throw Errors.shouldNotReachHere();
                }
                return newCacheNode;
            } finally {
                lock.unlock();
            }
        }

        private static ArrayWriteElementCacheNode purgeStaleCacheEntries(ArrayWriteElementCacheNode head, DynamicObject target) {
            if (JSConfig.TrackArrayAllocationSites && head != null && JSArray.isJSArray(target)) {
                ArrayAllocationSite allocationSite = JSAbstractArray.arrayGetAllocationSite(target);
                if (allocationSite != null && allocationSite.getInitialArrayType() != null) {
                    for (ArrayWriteElementCacheNode c = head, prev = null; c != null; prev = c, c = c.arrayCacheNext) {
                        if (c instanceof ConstantArrayWriteElementCacheNode) {
                            ConstantArrayWriteElementCacheNode existingNode = (ConstantArrayWriteElementCacheNode) c;
                            ScriptArray initialArrayType = allocationSite.getInitialArrayType();
                            if (!(initialArrayType instanceof ConstantEmptyArray) && existingNode.getArrayType() instanceof ConstantEmptyArray) {
                                // allocation site has been patched to not create an empty array;
                                // purge existing empty array specialization in cache
                                if (JSConfig.TraceArrayTransitions) {
                                    System.out.println("purging " + existingNode + ": " + existingNode.getArrayType() + " => " + JSAbstractArray.arrayGetArrayType(target));
                                }
                                if (prev == null) {
                                    return existingNode.arrayCacheNext;
                                } else {
                                    prev.arrayCacheNext = existingNode.arrayCacheNext;
                                    return head;
                                }
                            }
                        }
                    }
                }
            }
            return head;
        }
    }

    private static class JavaObjectWriteElementTypeCacheNode extends WriteElementTypeCacheNode {
        protected final Class<?> targetClass;

        JavaObjectWriteElementTypeCacheNode(Class<?> targetClass, WriteElementTypeCacheNode next) {
            super(next);
            this.targetClass = targetClass;
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value, Object receiver, WriteElementNode root) {
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
        }

        @Override
        public final boolean guard(Object target) {
            return targetClass.isInstance(target);
        }
    }

    static ArrayWriteElementCacheNode makeArrayCacheNode(DynamicObject target, ScriptArray array, ArrayWriteElementCacheNode next) {
        if (JSSlowArray.isJSSlowArray(target) || JSSlowArgumentsArray.isJSSlowArgumentsObject(target)) {
            return new ExactArrayWriteElementCacheNode(array, next);
        }

        if (array.isLengthNotWritable() || !array.isExtensible()) {
            // TODO handle this case in the specializations below
            return new ExactArrayWriteElementCacheNode(array, next);
        }
        if (array instanceof LazyRegexResultArray) {
            return new LazyRegexResultArrayWriteElementCacheNode(array, next);
        } else if (array instanceof LazyRegexResultIndicesArray) {
            return new LazyRegexResultIndicesArrayWriteElementCacheNode(array, next);
        } else if (array instanceof AbstractConstantArray) {
            return new ConstantArrayWriteElementCacheNode(array, next);
        } else if (array instanceof HolesIntArray) {
            return new HolesIntArrayWriteElementCacheNode(array, next);
        } else if (array instanceof HolesDoubleArray) {
            return new HolesDoubleArrayWriteElementCacheNode(array, next);
        } else if (array instanceof HolesJSObjectArray) {
            return new HolesJSObjectArrayWriteElementCacheNode(array, next);
        } else if (array instanceof HolesObjectArray) {
            return new HolesObjectArrayWriteElementCacheNode(array, next);
        } else if (array instanceof AbstractIntArray) {
            return new IntArrayWriteElementCacheNode(array, next);
        } else if (array instanceof AbstractDoubleArray) {
            return new DoubleArrayWriteElementCacheNode(array, next);
        } else if (array instanceof AbstractObjectArray) {
            return new ObjectArrayWriteElementCacheNode(array, next);
        } else if (array instanceof AbstractJSObjectArray) {
            return new JSObjectArrayWriteElementCacheNode(array, next);
        } else if (array instanceof AbstractWritableArray) {
            return new WritableArrayWriteElementCacheNode(array, next);
        } else if (array instanceof TypedArray) {
            if (array instanceof TypedArray.AbstractUint32Array) {
                return new Uint32ArrayWriteElementCacheNode((TypedArray) array, next);
            } else if (array instanceof TypedArray.AbstractUint8ClampedArray) {
                return new Uint8ClampedArrayWriteElementCacheNode((TypedArray) array, next);
            } else if (array instanceof TypedIntArray) {
                return new TypedIntArrayWriteElementCacheNode((TypedArray) array, next);
            } else if (array instanceof TypedFloatArray) {
                return new TypedFloatArrayWriteElementCacheNode((TypedArray) array, next);
            } else if (array instanceof TypedBigIntArray) {
                return new TypedBigIntArrayWriteElementCacheNode((TypedArray) array, next);
            } else {
                throw Errors.shouldNotReachHere();
            }
        } else {
            return new ExactArrayWriteElementCacheNode(array, next);
        }
    }

    abstract static class ArrayWriteElementCacheNode extends JavaScriptBaseNode {
        @Child ArrayWriteElementCacheNode arrayCacheNext;

        ArrayWriteElementCacheNode(ArrayWriteElementCacheNode next) {
            this.arrayCacheNext = next;
        }

        protected abstract boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root);

        protected abstract boolean guard(Object target, ScriptArray array);
    }

    private abstract static class ArrayClassGuardCachedArrayWriteElementCacheNode extends ArrayWriteElementCacheNode {
        private final ScriptArray arrayType;

        ArrayClassGuardCachedArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayCacheNext);
            this.arrayType = arrayType;
        }

        @Override
        protected final boolean guard(Object target, ScriptArray array) {
            return arrayType.isInstance(array);
        }

        protected final ScriptArray cast(ScriptArray array) {
            return arrayType.cast(array);
        }

        protected final ScriptArray getArrayType() {
            return arrayType;
        }
    }

    private abstract static class RecursiveCachedArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        @Child private ArrayWriteElementCacheNode recursiveWrite;
        private final BranchProfile needPrototypeBranch = BranchProfile.create();

        RecursiveCachedArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        protected final boolean setArrayAndWrite(ScriptArray newArray, DynamicObject target, long index, Object value, WriteElementNode root) {
            arraySetArrayType(target, newArray);
            return executeRecursive(target, newArray, index, value, root);
        }

        protected final boolean nonHolesArrayNeedsSlowSet(DynamicObject target, AbstractWritableArray arrayType, long index, WriteElementNode root) {
            assert !arrayType.isHolesType();
            if (!root.context.getArrayPrototypeNoElementsAssumption().isValid() && !root.writeOwn) {
                if (!arrayType.hasElement(target, index)) {
                    needPrototypeBranch.enter();
                    return true;
                }
            }
            return false;
        }

        protected final boolean holesArrayNeedsSlowSet(DynamicObject target, AbstractWritableArray arrayType, long index, WriteElementNode root) {
            assert arrayType.isHolesType();
            if ((!root.context.getArrayPrototypeNoElementsAssumption().isValid() && !root.writeOwn) ||
                            (!root.context.getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(target)) ||
                            (!root.context.getFastArgumentsObjectAssumption().isValid() && JSSlowArgumentsArray.isJSSlowArgumentsObject(target))) {
                if (!arrayType.hasElement(target, index)) {
                    needPrototypeBranch.enter();
                    return true;
                }
            }
            return false;
        }

        @ExplodeLoop
        private boolean executeRecursive(DynamicObject targetObject, ScriptArray array, long index, Object value, WriteElementNode root) {
            for (ArrayWriteElementCacheNode c = recursiveWrite; c != null; c = c.arrayCacheNext) {
                boolean guard = c.guard(targetObject, array);
                if (guard) {
                    return c.executeSetArray(targetObject, array, index, value, root);
                }
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayWriteElementCacheNode specialization = specialize(targetObject, array);
            return specialization.executeSetArray(targetObject, array, index, value, root);
        }

        private ArrayWriteElementCacheNode specialize(DynamicObject target, ScriptArray array) {
            CompilerAsserts.neverPartOfCompilation();
            Lock lock = getLock();
            lock.lock();
            try {
                ArrayWriteElementCacheNode currentHead = recursiveWrite;
                for (ArrayWriteElementCacheNode c = currentHead; c != null; c = c.arrayCacheNext) {
                    if (c.guard(target, array)) {
                        return c;
                    }
                }

                ArrayWriteElementCacheNode newCacheNode = makeArrayCacheNode(target, array, currentHead);
                insert(newCacheNode);
                recursiveWrite = newCacheNode;
                if (!newCacheNode.guard(target, array)) {
                    throw Errors.shouldNotReachHere();
                }
                return newCacheNode;
            } finally {
                lock.unlock();
            }
        }
    }

    private static class ExactArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {

        ExactArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            return false;
        }
    }

    private static class LazyRegexResultArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        @Child private TRegexUtil.TRegexMaterializeResultNode materializeResultNode = TRegexUtil.TRegexMaterializeResultNode.create();

        LazyRegexResultArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            LazyRegexResultArray lazyRegexResultArray = (LazyRegexResultArray) cast(array);
            ScriptArray newArray = lazyRegexResultArray.createWritable(materializeResultNode, target, index, value);
            if (inBoundsProfile.profile(index >= 0 && index < 0x7fff_ffff)) {
                return setArrayAndWrite(newArray, target, index, value, root);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, newArray).setElement(target, index, value, root.isStrict));
                return true;
            }
        }
    }

    private static class LazyRegexResultIndicesArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        @Child private TRegexUtil.TRegexResultAccessor resultAccessor = TRegexUtil.TRegexResultAccessor.create();

        LazyRegexResultIndicesArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            LazyRegexResultIndicesArray lazyRegexResultIndicesArray = (LazyRegexResultIndicesArray) cast(array);
            ScriptArray newArray = lazyRegexResultIndicesArray.createWritable(root.context, resultAccessor, target, index, value);
            if (inBoundsProfile.profile(index >= 0 && index < 0x7fff_ffff)) {
                return setArrayAndWrite(newArray, target, index, value, root);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, newArray).setElement(target, index, value, root.isStrict));
                return true;
            }
        }
    }

    private static class ConstantArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile inBoundsIntBranch = BranchProfile.create();
        private final BranchProfile inBoundsDoubleBranch = BranchProfile.create();
        private final BranchProfile inBoundsJSObjectBranch = BranchProfile.create();
        private final BranchProfile inBoundsObjectBranch = BranchProfile.create();
        private final ScriptArray.ProfileHolder createWritableProfile = AbstractConstantArray.createCreateWritableProfile();

        ConstantArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            AbstractConstantArray constantArray = (AbstractConstantArray) cast(array);
            if (inBoundsProfile.profile(index >= 0 && index < 0x7fff_ffff)) {
                ScriptArray newArray;
                if (value instanceof Integer) {
                    inBoundsIntBranch.enter();
                    newArray = constantArray.createWriteableInt(target, index, (int) value, createWritableProfile);
                } else if (value instanceof Double) {
                    inBoundsDoubleBranch.enter();
                    newArray = constantArray.createWriteableDouble(target, index, (double) value, createWritableProfile);
                } else if (JSDynamicObject.isJSDynamicObject(value)) {
                    inBoundsJSObjectBranch.enter();
                    newArray = constantArray.createWriteableJSObject(target, index, (JSDynamicObject) value, createWritableProfile);
                } else {
                    inBoundsObjectBranch.enter();
                    newArray = constantArray.createWriteableObject(target, index, value, createWritableProfile);
                }
                return setArrayAndWrite(newArray, target, index, value, root);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, array).setElement(target, index, value, root.isStrict));
                return true;
            }
        }
    }

    private static class WritableArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        WritableArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBoundsProfile.profile(writableArray.isInBoundsFast(target, index))) {
                arraySetArrayType(target, writableArray.setElement(target, index, value, root.isStrict));
                return true;
            } else {
                return false;
            }
        }
    }

    private static class IntArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final BranchProfile intValueBranch = BranchProfile.create();
        private final BranchProfile toDoubleBranch = BranchProfile.create();
        private final BranchProfile toObjectBranch = BranchProfile.create();
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedNonZeroCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedZeroCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedContiguousCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedHolesCondition = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        IntArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            AbstractIntArray intArray = (AbstractIntArray) cast(array);
            if (value instanceof Integer) {
                intValueBranch.enter();
                return executeWithIntValueInner(target, intArray, index, (int) value, root);
            } else if (value instanceof Double) {
                toDoubleBranch.enter();
                double doubleValue = (double) value;
                return setArrayAndWrite(intArray.toDouble(target, index, doubleValue), target, index, doubleValue, root);
            } else {
                toObjectBranch.enter();
                return setArrayAndWrite(intArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean executeWithIntValueInner(DynamicObject target, AbstractIntArray intArray, long index, int intValue, WriteElementNode root) {
            assert !(intArray instanceof HolesIntArray);
            if (nonHolesArrayNeedsSlowSet(target, intArray, index, root)) {
                return false;
            }
            int iIndex = (int) index;
            if (inBoundsFastCondition.profile(intArray.isInBoundsFast(target, index) && !mightTransferToNonContiguous(intArray, target, index))) {
                intArray.setInBoundsFast(target, iIndex, intValue);
                return true;
            } else if (inBoundsCondition.profile(intArray.isInBounds(target, iIndex) && !mightTransferToNonContiguous(intArray, target, index))) {
                intArray.setInBounds(target, iIndex, intValue, profile);
                return true;
            } else if (supportedNonZeroCondition.profile(intArray.isSupported(target, index) && !mightTransferToNonContiguous(intArray, target, index))) {
                intArray.setSupported(target, iIndex, intValue, profile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedZeroCondition.profile(mightTransferToNonContiguous(intArray, target, index) && intArray.isSupported(target, index))) {
                    toArrayType = intArray.toNonContiguous(target, iIndex, intValue, profile);
                } else if (supportedContiguousCondition.profile(!(intArray instanceof AbstractContiguousIntArray) && intArray.isSupportedContiguous(target, index))) {
                    toArrayType = intArray.toContiguous(target, index, intValue);
                } else if (supportedHolesCondition.profile(intArray.isSupportedHoles(target, index))) {
                    toArrayType = intArray.toHoles(target, index, intValue);
                } else {
                    assert intArray.isSparse(target, index);
                    toArrayType = intArray.toSparse(target, index, intValue);
                }
                return setArrayAndWrite(toArrayType, target, index, intValue, root);
            }
        }

        private static boolean mightTransferToNonContiguous(AbstractIntArray intArray, DynamicObject target, long index) {
            return intArray instanceof ContiguousIntArray && index == 0 && intArray.firstElementIndex(target) == 1;
        }
    }

    private static class DoubleArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final BranchProfile intValueBranch = BranchProfile.create();
        private final BranchProfile doubleValueBranch = BranchProfile.create();
        private final BranchProfile toObjectBranch = BranchProfile.create();
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedContiguousCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedHolesCondition = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        DoubleArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            AbstractDoubleArray doubleArray = (AbstractDoubleArray) cast(array);
            double doubleValue;
            if (value instanceof Double) {
                doubleValueBranch.enter();
                doubleValue = (double) value;
            } else if (value instanceof Integer) {
                intValueBranch.enter();
                doubleValue = (int) value;
            } else {
                toObjectBranch.enter();
                return setArrayAndWrite(doubleArray.toObject(target, index, value), target, index, value, root);
            }
            return executeWithDoubleValueInner(target, doubleArray, index, doubleValue, root);
        }

        private boolean executeWithDoubleValueInner(DynamicObject target, AbstractDoubleArray doubleArray, long index, double doubleValue, WriteElementNode root) {
            assert !(doubleArray instanceof HolesDoubleArray);
            if (nonHolesArrayNeedsSlowSet(target, doubleArray, index, root)) {
                return false;
            }
            int iIndex = (int) index;
            if (inBoundsFastCondition.profile(doubleArray.isInBoundsFast(target, index))) {
                doubleArray.setInBoundsFast(target, iIndex, doubleValue);
                return true;
            } else if (inBoundsCondition.profile(doubleArray.isInBounds(target, iIndex))) {
                doubleArray.setInBounds(target, iIndex, doubleValue, profile);
                return true;
            } else if (supportedCondition.profile(doubleArray.isSupported(target, index))) {
                doubleArray.setSupported(target, iIndex, doubleValue, profile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedContiguousCondition.profile(!(doubleArray instanceof AbstractContiguousDoubleArray) && doubleArray.isSupportedContiguous(target, index))) {
                    toArrayType = doubleArray.toContiguous(target, index, doubleValue);
                } else if (supportedHolesCondition.profile(doubleArray.isSupportedHoles(target, index))) {
                    toArrayType = doubleArray.toHoles(target, index, doubleValue);
                } else {
                    assert doubleArray.isSparse(target, index);
                    toArrayType = doubleArray.toSparse(target, index, doubleValue);
                }
                return setArrayAndWrite(toArrayType, target, index, doubleValue, root);
            }
        }
    }

    private static class ObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedContiguousCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedHolesCondition = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        ObjectArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            AbstractObjectArray objectArray = (AbstractObjectArray) cast(array);
            assert !(objectArray instanceof HolesObjectArray);
            if (nonHolesArrayNeedsSlowSet(target, objectArray, index, root)) {
                return false;
            }
            int iIndex = (int) index;
            if (inBoundsFastCondition.profile(objectArray.isInBoundsFast(target, index))) {
                objectArray.setInBoundsFast(target, iIndex, value);
                return true;
            } else if (inBoundsCondition.profile(objectArray.isInBounds(target, iIndex))) {
                objectArray.setInBounds(target, iIndex, value, profile);
                return true;
            } else if (supportedCondition.profile(objectArray.isSupported(target, index))) {
                objectArray.setSupported(target, iIndex, value);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedContiguousCondition.profile(!(objectArray instanceof AbstractContiguousObjectArray) && objectArray.isSupportedContiguous(target, index))) {
                    toArrayType = objectArray.toContiguous(target, index, value);
                } else if (supportedHolesCondition.profile(objectArray.isSupportedHoles(target, index))) {
                    toArrayType = objectArray.toHoles(target, index, value);
                } else {
                    assert objectArray.isSparse(target, index);
                    toArrayType = objectArray.toSparse(target, index, value);
                }
                return setArrayAndWrite(toArrayType, target, index, value, root);
            }
        }
    }

    private static class JSObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final ConditionProfile objectType = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedContiguousCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedHolesCondition = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        JSObjectArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            AbstractJSObjectArray jsobjectArray = (AbstractJSObjectArray) cast(array);
            if (objectType.profile(JSDynamicObject.isJSDynamicObject(value))) {
                JSDynamicObject jsobjectValue = (JSDynamicObject) value;
                return executeWithJSObjectValueInner(target, jsobjectArray, index, jsobjectValue, root);
            } else {
                return setArrayAndWrite(jsobjectArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean executeWithJSObjectValueInner(DynamicObject target, AbstractJSObjectArray jsobjectArray, long index, JSDynamicObject jsobjectValue, WriteElementNode root) {
            assert !(jsobjectArray instanceof HolesJSObjectArray);
            int iIndex = (int) index;
            if (nonHolesArrayNeedsSlowSet(target, jsobjectArray, index, root)) {
                return false;
            }
            if (inBoundsFastCondition.profile(jsobjectArray.isInBoundsFast(target, index))) {
                jsobjectArray.setInBoundsFast(target, iIndex, jsobjectValue);
                return true;
            } else if (inBoundsCondition.profile(jsobjectArray.isInBounds(target, iIndex))) {
                jsobjectArray.setInBounds(target, iIndex, jsobjectValue, profile);
                return true;
            } else if (supportedCondition.profile(jsobjectArray.isSupported(target, index))) {
                jsobjectArray.setSupported(target, iIndex, jsobjectValue, profile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedContiguousCondition.profile(!(jsobjectArray instanceof AbstractContiguousJSObjectArray) && jsobjectArray.isSupportedContiguous(target, index))) {
                    toArrayType = jsobjectArray.toContiguous(target, index, jsobjectValue);
                } else if (supportedHolesCondition.profile(jsobjectArray.isSupportedHoles(target, index))) {
                    toArrayType = jsobjectArray.toHoles(target, index, jsobjectValue);
                } else {
                    assert jsobjectArray.isSparse(target, index);
                    toArrayType = jsobjectArray.toSparse(target, index, jsobjectValue);
                }
                return setArrayAndWrite(toArrayType, target, index, jsobjectValue, root);
            }
        }
    }

    private static class HolesIntArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final BranchProfile intValueBranch = BranchProfile.create();
        private final BranchProfile toDoubleBranch = BranchProfile.create();
        private final BranchProfile toObjectBranch = BranchProfile.create();
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsFastHoleCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedContainsHolesCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedNotContainsHolesCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasExplicitHolesProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile containsHolesProfile = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        HolesIntArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            HolesIntArray holesIntArray = (HolesIntArray) cast(array);
            if (value instanceof Integer) {
                intValueBranch.enter();
                int intValue = (int) value;
                return executeWithIntValueInner(target, holesIntArray, index, intValue, root);
            } else if (value instanceof Double) {
                toDoubleBranch.enter();
                double doubleValue = (double) value;
                return setArrayAndWrite(holesIntArray.toDouble(target, index, doubleValue), target, index, doubleValue, root);
            } else {
                toObjectBranch.enter();
                return setArrayAndWrite(holesIntArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean executeWithIntValueInner(DynamicObject target, HolesIntArray holesIntArray, long index, int intValue, WriteElementNode root) {
            if (holesArrayNeedsSlowSet(target, holesIntArray, index, root)) {
                return false;
            }
            int iIndex = (int) index;
            boolean containsHoles = containsHolesProfile.profile(containsHoles(target, holesIntArray, index));
            if (containsHoles && inBoundsFastCondition.profile(holesIntArray.isInBoundsFast(target, index) && !HolesIntArray.isHoleValue(intValue))) {
                if (inBoundsFastHoleCondition.profile(holesIntArray.isHoleFast(target, iIndex))) {
                    holesIntArray.setInBoundsFastHole(target, iIndex, intValue);
                } else {
                    holesIntArray.setInBoundsFastNonHole(target, iIndex, intValue);
                }
                return true;
            } else if (containsHoles && inBoundsCondition.profile(holesIntArray.isInBounds(target, iIndex) && !HolesIntArray.isHoleValue(intValue))) {
                holesIntArray.setInBounds(target, iIndex, intValue, profile);
                return true;
            } else if (containsHoles && supportedContainsHolesCondition.profile(holesIntArray.isSupported(target, index) && !HolesIntArray.isHoleValue(intValue))) {
                holesIntArray.setSupported(target, iIndex, intValue, profile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (!containsHoles && supportedNotContainsHolesCondition.profile(holesIntArray.isSupported(target, index))) {
                    toArrayType = holesIntArray.toNonHoles(target, index, intValue);
                } else {
                    assert holesIntArray.isSparse(target, index);
                    toArrayType = holesIntArray.toSparse(target, index, intValue);
                }
                return setArrayAndWrite(toArrayType, target, index, intValue, root);
            }
        }

        private boolean containsHoles(DynamicObject target, HolesIntArray holesIntArray, long index) {
            return hasExplicitHolesProfile.profile(JSArray.arrayGetHoleCount(target) > 0) || !holesIntArray.isInBoundsFast(target, index);
        }
    }

    private static class HolesDoubleArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final BranchProfile doubleValueBranch = BranchProfile.create();
        private final BranchProfile intValueBranch = BranchProfile.create();
        private final BranchProfile toObjectBranch = BranchProfile.create();
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsFastHoleCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedContainsHolesCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedNotContainsHolesCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasExplicitHolesProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile containsHolesProfile = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        HolesDoubleArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            HolesDoubleArray holesDoubleArray = (HolesDoubleArray) cast(array);
            double doubleValue;
            if (value instanceof Double) {
                doubleValueBranch.enter();
                doubleValue = (double) value;
            } else if (value instanceof Integer) {
                intValueBranch.enter();
                doubleValue = (int) value;
            } else {
                toObjectBranch.enter();
                return setArrayAndWrite(holesDoubleArray.toObject(target, index, value), target, index, value, root);
            }

            return executeWithDoubleValueInner(target, holesDoubleArray, index, doubleValue, root);
        }

        private boolean executeWithDoubleValueInner(DynamicObject target, HolesDoubleArray holesDoubleArray, long index, double doubleValue, WriteElementNode root) {
            if (holesArrayNeedsSlowSet(target, holesDoubleArray, index, root)) {
                return false;
            }
            int iIndex = (int) index;
            boolean containsHoles = containsHolesProfile.profile(containsHoles(target, holesDoubleArray, index));
            if (containsHoles && inBoundsFastCondition.profile(holesDoubleArray.isInBoundsFast(target, index) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                if (inBoundsFastHoleCondition.profile(holesDoubleArray.isHoleFast(target, iIndex))) {
                    holesDoubleArray.setInBoundsFastHole(target, iIndex, doubleValue);
                } else {
                    holesDoubleArray.setInBoundsFastNonHole(target, iIndex, doubleValue);
                }
                return true;
            } else if (containsHoles && inBoundsCondition.profile(holesDoubleArray.isInBounds(target, iIndex) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                holesDoubleArray.setInBounds(target, iIndex, doubleValue, profile);
                return true;
            } else if (containsHoles && supportedContainsHolesCondition.profile(holesDoubleArray.isSupported(target, index) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                holesDoubleArray.setSupported(target, iIndex, doubleValue, profile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (!containsHoles && supportedNotContainsHolesCondition.profile(holesDoubleArray.isSupported(target, index))) {
                    toArrayType = holesDoubleArray.toNonHoles(target, index, doubleValue);
                } else {
                    assert holesDoubleArray.isSparse(target, index);
                    toArrayType = holesDoubleArray.toSparse(target, index, doubleValue);
                }
                return setArrayAndWrite(toArrayType, target, index, doubleValue, root);
            }
        }

        private boolean containsHoles(DynamicObject target, HolesDoubleArray holesDoubleArray, long index) {
            return hasExplicitHolesProfile.profile(JSArray.arrayGetHoleCount(target) > 0) || !holesDoubleArray.isInBoundsFast(target, index);
        }
    }

    private static class HolesJSObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final ConditionProfile objectType = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsFastHoleCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedContainsHolesCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedNotContainsHolesCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile hasExplicitHolesProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile containsHolesProfile = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        HolesJSObjectArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
            assert arrayType.getClass() == HolesJSObjectArray.class;
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            HolesJSObjectArray holesArray = (HolesJSObjectArray) cast(array);
            if (objectType.profile(JSDynamicObject.isJSDynamicObject(value))) {
                return executeWithJSObjectValueInner(target, holesArray, index, (JSDynamicObject) value, root);
            } else {
                return setArrayAndWrite(holesArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean executeWithJSObjectValueInner(DynamicObject target, HolesJSObjectArray jsobjectArray, long index, JSDynamicObject value, WriteElementNode root) {
            if (holesArrayNeedsSlowSet(target, jsobjectArray, index, root)) {
                return false;
            }
            boolean containsHoles = containsHolesProfile.profile(containsHoles(target, jsobjectArray, index));
            if (containsHoles && inBoundsFastCondition.profile(jsobjectArray.isInBoundsFast(target, index))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                if (inBoundsFastHoleCondition.profile(jsobjectArray.isHoleFast(target, (int) index))) {
                    jsobjectArray.setInBoundsFastHole(target, (int) index, value);
                } else {
                    jsobjectArray.setInBoundsFastNonHole(target, (int) index, value);
                }
                return true;
            } else if (containsHoles && inBoundsCondition.profile(jsobjectArray.isInBounds(target, (int) index))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                jsobjectArray.setInBounds(target, (int) index, value, profile);
                return true;
            } else if (containsHoles && supportedContainsHolesCondition.profile(jsobjectArray.isSupported(target, index))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                jsobjectArray.setSupported(target, (int) index, value, profile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (!containsHoles && supportedNotContainsHolesCondition.profile(jsobjectArray.isSupported(target, index))) {
                    toArrayType = jsobjectArray.toNonHoles(target, index, value);
                } else {
                    assert jsobjectArray.isSparse(target, index);
                    toArrayType = jsobjectArray.toSparse(target, index, value);
                }
                return setArrayAndWrite(toArrayType, target, index, value, root);
            }
        }

        private boolean containsHoles(DynamicObject target, HolesJSObjectArray holesJSObjectArray, long index) {
            return hasExplicitHolesProfile.profile(JSArray.arrayGetHoleCount(target) > 0) || !holesJSObjectArray.isInBoundsFast(target, index);
        }
    }

    private static class HolesObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsFastHoleCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedCondition = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        HolesObjectArrayWriteElementCacheNode(ScriptArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
            assert arrayType.getClass() == HolesObjectArray.class;
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            HolesObjectArray objectArray = (HolesObjectArray) array;
            if (holesArrayNeedsSlowSet(target, objectArray, index, root)) {
                return false;
            }
            if (inBoundsFastCondition.profile(objectArray.isInBoundsFast(target, index))) {
                assert !HolesObjectArray.isHoleValue(value);
                if (inBoundsFastHoleCondition.profile(objectArray.isHoleFast(target, (int) index))) {
                    objectArray.setInBoundsFastHole(target, (int) index, value);
                } else {
                    objectArray.setInBoundsFastNonHole(target, (int) index, value);
                }
                return true;
            } else if (inBoundsCondition.profile(objectArray.isInBounds(target, (int) index))) {
                assert !HolesObjectArray.isHoleValue(value);
                objectArray.setInBounds(target, (int) index, value, profile);
                return true;
            } else if (supportedCondition.profile(objectArray.isSupported(target, index))) {
                assert !HolesObjectArray.isHoleValue(value);
                objectArray.setSupported(target, (int) index, value);
                return true;
            } else {
                assert objectArray.isSparse(target, index);
                return setArrayAndWrite(objectArray.toSparse(target, index, value), target, index, value, root);
            }
        }
    }

    private abstract static class AbstractTypedArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        @Child protected InteropLibrary interop;

        AbstractTypedArrayWriteElementCacheNode(TypedArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
            this.interop = arrayType.isInterop() ? InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit) : InteropLibrary.getUncached();
        }

        protected void checkDetachedArrayBuffer(DynamicObject target, WriteElementNode root) {
            if (JSArrayBufferView.hasDetachedBuffer(target, root.context)) {
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }
    }

    private abstract static class AbstractTypedIntArrayWriteElementCacheNode extends AbstractTypedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        AbstractTypedIntArrayWriteElementCacheNode(TypedArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected final boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            TypedIntArray typedArray = (TypedIntArray) cast(array);
            int iValue = toInt(value); // could throw
            checkDetachedArrayBuffer(target, root);
            if (inBoundsProfile.profile(typedArray.hasElement(target, index))) {
                typedArray.setInt(target, (int) index, iValue, interop);
            } else {
                // do nothing; cf. ES6 9.4.5.9 IntegerIndexedElementSet(O, index, value)
            }
            return true;
        }

        protected abstract int toInt(Object value);
    }

    private static class TypedIntArrayWriteElementCacheNode extends AbstractTypedIntArrayWriteElementCacheNode {
        @Child private JSToInt32Node toIntNode;

        TypedIntArrayWriteElementCacheNode(TypedArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
            this.toIntNode = JSToInt32Node.create();
        }

        @Override
        protected int toInt(Object value) {
            return toIntNode.executeInt(value);
        }
    }

    private static class TypedBigIntArrayWriteElementCacheNode extends AbstractTypedArrayWriteElementCacheNode {

        @Child private JSToBigIntNode toBigIntNode;
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        TypedBigIntArrayWriteElementCacheNode(TypedArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
            this.toBigIntNode = JSToBigIntNode.create();
        }

        @Override
        protected final boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            TypedBigIntArray typedArray = (TypedBigIntArray) cast(array);
            BigInt biValue = toBigIntNode.executeBigInteger(value); // could throw
            checkDetachedArrayBuffer(target, root);
            if (inBoundsProfile.profile(typedArray.hasElement(target, index))) {
                typedArray.setBigInt(target, (int) index, biValue, interop);
            }
            return true;
        }
    }

    private static class Uint8ClampedArrayWriteElementCacheNode extends AbstractTypedIntArrayWriteElementCacheNode {
        private final ConditionProfile toIntProfile = ConditionProfile.createBinaryProfile();
        @Child private JSToDoubleNode toDoubleNode;

        Uint8ClampedArrayWriteElementCacheNode(TypedArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected int toInt(Object value) {
            if (toIntProfile.profile(value instanceof Integer)) {
                return (int) value;
            } else {
                double doubleValue = toDouble(value);
                return Uint8ClampedArray.toInt(doubleValue);
            }
        }

        private double toDouble(Object value) {
            if (toDoubleNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toDoubleNode = insert(JSToDoubleNode.create());
            }
            return toDoubleNode.executeDouble(value);
        }
    }

    private static class Uint32ArrayWriteElementCacheNode extends AbstractTypedIntArrayWriteElementCacheNode {
        private final ConditionProfile toIntProfile = ConditionProfile.createBinaryProfile();
        @Child private JSToNumberNode toNumberNode;

        Uint32ArrayWriteElementCacheNode(TypedArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
        }

        @Override
        protected int toInt(Object value) {
            if (toIntProfile.profile(value instanceof Integer)) {
                return (int) value;
            } else {
                return (int) JSRuntime.toUInt32(toNumber(value));
            }
        }

        private Number toNumber(Object value) {
            if (toNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toNumberNode = insert(JSToNumberNode.create());
            }
            return toNumberNode.executeNumber(value);
        }
    }

    private static class TypedFloatArrayWriteElementCacheNode extends AbstractTypedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();
        @Child private JSToDoubleNode toDoubleNode;

        TypedFloatArrayWriteElementCacheNode(TypedArray arrayType, ArrayWriteElementCacheNode arrayCacheNext) {
            super(arrayType, arrayCacheNext);
            this.toDoubleNode = JSToDoubleNode.create();
        }

        @Override
        protected boolean executeSetArray(DynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            TypedFloatArray typedArray = (TypedFloatArray) cast(array);
            double dValue = toDouble(value); // could throw
            checkDetachedArrayBuffer(target, root);
            if (inBoundsProfile.profile(typedArray.hasElement(target, index))) {
                typedArray.setDouble(target, (int) index, dValue, interop);
            } else {
                // do nothing; cf. ES6 9.4.5.9 IntegerIndexedElementSet(O, index, value)
            }
            return true;
        }

        private double toDouble(Object value) {
            return toDoubleNode.executeDouble(value);
        }
    }

    private abstract static class ToPropertyKeyCachedWriteElementTypeCacheNode extends WriteElementTypeCacheNode {
        @Child private JSToPropertyKeyNode indexToPropertyKeyNode;
        protected final JSClassProfile classProfile = JSClassProfile.create();

        ToPropertyKeyCachedWriteElementTypeCacheNode(WriteElementTypeCacheNode next) {
            super(next);
        }

        protected final Object toPropertyKey(Object index) {
            if (indexToPropertyKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                indexToPropertyKeyNode = insert(JSToPropertyKeyNode.create());
            }
            return indexToPropertyKeyNode.execute(index);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value, Object receiver, WriteElementNode root) {
            executeWithTargetAndIndexUnguarded(target, (long) index, value, receiver, root);
        }
    }

    private static class StringWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        private final Class<?> stringClass;
        private final BranchProfile intIndexBranch = BranchProfile.create();
        private final BranchProfile stringIndexBranch = BranchProfile.create();
        private final ConditionProfile isImmutable = ConditionProfile.createBinaryProfile();
        @Child private ToArrayIndexNode toArrayIndexNode;

        StringWriteElementTypeCacheNode(Class<?> stringClass, WriteElementTypeCacheNode next) {
            super(next);
            this.stringClass = stringClass;
            this.toArrayIndexNode = ToArrayIndexNode.createNoToPropertyKey();
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (convertedIndex instanceof Long) {
                intIndexBranch.enter();
                long longIndex = (long) convertedIndex;
                if (isImmutable.profile(longIndex >= 0 && longIndex < JSRuntime.length(charSequence))) {
                    // cannot set characters of immutable strings
                    if (root.isStrict) {
                        throw Errors.createTypeErrorNotWritableProperty(Boundaries.stringValueOf(index), charSequence, this);
                    }
                    return;
                }
            }
            stringIndexBranch.enter();
            JSObject.setWithReceiver(JSString.create(root.context, charSequence), toPropertyKey(index), value, target, root.isStrict, classProfile, root);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            if (isImmutable.profile(index >= 0 && index < JSRuntime.length(charSequence))) {
                // cannot set characters of immutable strings
                if (root.isStrict) {
                    throw Errors.createTypeErrorNotWritableProperty(Boundaries.stringValueOf(index), charSequence, this);
                }
                return;
            } else {
                JSObject.setWithReceiver(JSString.create(root.context, charSequence), index, value, target, root.isStrict, classProfile, root);
            }
        }

        @Override
        public boolean guard(Object target) {
            return stringClass.isInstance(target);
        }
    }

    private static class NumberWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        private final Class<?> numberClass;

        NumberWriteElementTypeCacheNode(Class<?> numberClass, WriteElementTypeCacheNode next) {
            super(next);
            this.numberClass = numberClass;
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
            Number number = (Number) target;
            JSObject.setWithReceiver(JSNumber.create(root.context, number), toPropertyKey(index), value, target, root.isStrict, classProfile, root);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            Number number = (Number) target;
            JSObject.setWithReceiver(JSNumber.create(root.context, number), index, value, target, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return numberClass.isInstance(target);
        }
    }

    private static class BooleanWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        BooleanWriteElementTypeCacheNode(WriteElementTypeCacheNode next) {
            super(next);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
            Boolean bool = (Boolean) target;
            JSObject.setWithReceiver(JSBoolean.create(root.context, bool), toPropertyKey(index), value, target, root.isStrict, classProfile, root);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            Boolean bool = (Boolean) target;
            JSObject.setWithReceiver(JSBoolean.create(root.context, bool), index, value, target, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Boolean;
        }
    }

    private static class SymbolWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        SymbolWriteElementTypeCacheNode(WriteElementTypeCacheNode next) {
            super(next);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
            if (root.isStrict) {
                throw Errors.createTypeError("cannot set element on Symbol in strict mode", this);
            }
            Symbol symbol = (Symbol) target;
            JSObject.setWithReceiver(JSSymbol.create(root.context, symbol), toPropertyKey(index), value, receiver, root.isStrict, classProfile, root);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            if (root.isStrict) {
                throw Errors.createTypeError("cannot set element on Symbol in strict mode", this);
            }
            Symbol symbol = (Symbol) target;
            JSObject.setWithReceiver(JSSymbol.create(root.context, symbol), index, value, receiver, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Symbol;
        }
    }

    private static class BigIntWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        BigIntWriteElementTypeCacheNode(WriteElementTypeCacheNode next) {
            super(next);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
            BigInt bigInt = (BigInt) target;
            JSObject.setWithReceiver(JSBigInt.create(root.context, bigInt), toPropertyKey(index), value, target, root.isStrict, classProfile, root);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            BigInt bigInt = (BigInt) target;
            JSObject.setWithReceiver(JSBigInt.create(root.context, bigInt), index, value, target, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof BigInt;
        }
    }

    static class TruffleObjectWriteElementTypeCacheNode extends WriteElementTypeCacheNode {
        private final Class<?> targetClass;
        @Child private InteropLibrary interop;
        @Child private InteropLibrary keyInterop;
        @Child private InteropLibrary setterInterop;
        @Child private JSToPropertyKeyNode toPropertyKey;
        @Child private ExportValueNode exportValue;
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final BranchProfile errorBranch = BranchProfile.create();

        TruffleObjectWriteElementTypeCacheNode(Class<?> targetClass, WriteElementTypeCacheNode next) {
            super(next);
            this.targetClass = targetClass;
            this.toPropertyKey = JSToPropertyKeyNode.create();
            this.exportValue = ExportValueNode.create();
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
            this.keyInterop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root) {
            Object truffleObject = targetClass.cast(target);
            if (interop.isNull(truffleObject)) {
                throw Errors.createTypeErrorCannotSetProperty(index, truffleObject, this, root.getContext());
            }
            Object propertyKey;
            Object exportedValue = exportValue.execute(value);
            if (interop.hasArrayElements(truffleObject)) {
                Object indexOrPropertyKey = toArrayIndex(index);
                if (indexOrPropertyKey instanceof Long) {
                    try {
                        interop.writeArrayElement(truffleObject, (long) indexOrPropertyKey, exportedValue);
                        return;
                    } catch (InvalidArrayIndexException | UnsupportedTypeException | UnsupportedMessageException e) {
                        if (root.isStrict) {
                            errorBranch.enter();
                            throw Errors.createTypeErrorInteropException(truffleObject, e, "writeArrayElement", this);
                        } else {
                            return;
                        }
                    }
                } else {
                    propertyKey = indexOrPropertyKey;
                    assert JSRuntime.isPropertyKey(propertyKey);
                }
            } else {
                propertyKey = toPropertyKey.execute(index);
            }
            if (root.context.getContextOptions().hasForeignHashProperties() && interop.hasHashEntries(truffleObject)) {
                try {
                    interop.writeHashEntry(truffleObject, propertyKey, exportedValue);
                } catch (UnknownKeyException | UnsupportedMessageException | UnsupportedTypeException e) {
                    if (root.isStrict) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorInteropException(truffleObject, e, "writeHashEntry", this);
                    } else {
                        return;
                    }
                }
            }
            if (propertyKey instanceof Symbol) {
                return;
            }
            String stringKey = (String) propertyKey;
            if (root.context.isOptionNashornCompatibilityMode()) {
                if (tryInvokeSetter(truffleObject, stringKey, exportedValue, root.context)) {
                    return;
                }
            }
            try {
                interop.writeMember(truffleObject, stringKey, exportedValue);
            } catch (UnknownIdentifierException | UnsupportedTypeException | UnsupportedMessageException e) {
                if (root.isStrict) {
                    errorBranch.enter();
                    throw Errors.createTypeErrorInteropException(truffleObject, e, "writeMember", this);
                } else {
                    return;
                }
            }
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value, Object receiver, WriteElementNode root) {
            executeWithTargetAndIndexUnguarded(target, (Object) index, value, receiver, root);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            executeWithTargetAndIndexUnguarded(target, (Object) index, value, receiver, root);
        }

        @Override
        public boolean guard(Object target) {
            return targetClass.isInstance(target) && !JSDynamicObject.isJSDynamicObject(target);
        }

        private boolean tryInvokeSetter(Object thisObj, String key, Object value, JSContext context) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = context.getRealm().getEnv();
            if (env.isHostObject(thisObj)) {
                String setterKey = PropertyCacheNode.getAccessorKey("set", key);
                if (setterKey == null) {
                    return false;
                }
                if (setterInterop == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setterInterop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                if (!setterInterop.isMemberInvocable(thisObj, setterKey)) {
                    return false;
                }
                try {
                    setterInterop.invokeMember(thisObj, setterKey, value);
                    return true;
                } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                    // silently ignore
                }
            }
            return false;
        }

        private Object toArrayIndex(Object index) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(index);
        }
    }

    @Override
    public JavaScriptNode getTarget() {
        return targetNode;
    }

    public JavaScriptNode getElement() {
        return indexNode;
    }

    public JavaScriptNode getValue() {
        return valueNode;
    }

    public JSContext getContext() {
        return context;
    }

    public boolean isStrict() {
        return isStrict;
    }

    public boolean writeOwn() {
        return writeOwn;
    }

    boolean isSuperProperty() {
        return targetNode instanceof SuperPropertyReferenceNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(indexNode, materializedTags), cloneUninitialized(valueNode, materializedTags), getContext(), isStrict(),
                        writeOwn());
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return valueNode.isResultAlwaysOfType(clazz);
    }

    public static WriteElementNode createCachedInterop(LanguageReference<JavaScriptLanguage> languageRef) {
        return create(languageRef.get().getJSContext(), true);
    }
}
