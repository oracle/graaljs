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
package com.oracle.truffle.js.nodes.access;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.ArrayWriteElementCacheDispatchNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.BigIntWriteElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.BooleanWriteElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.ConstantArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.DoubleArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.ForeignObjectWriteElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.HolesDoubleArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.HolesIntArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.HolesJSObjectArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.HolesObjectArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.IntArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.JSObjectArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.LazyRegexResultArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.LazyRegexResultIndicesArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.NumberWriteElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.ObjectArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.StringWriteElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.SymbolWriteElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.TypedBigIntArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.TypedFloatArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.TypedIntArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.WritableArrayWriteElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.WriteElementNodeFactory.WriteElementTypeCacheDispatchNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNoToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementTag;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.ScriptArray.CreateWritableProfileAccess;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.AbstractUint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.AbstractUint8ClampedArray;
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
import com.oracle.truffle.js.runtime.array.dyn.AbstractWritableArray.SetSupportedProfileAccess;
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
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

public class WriteElementNode extends JSTargetableNode {
    @Child protected JavaScriptNode targetNode;
    @Child protected JavaScriptNode indexNode;
    @Child private ToArrayIndexNode toArrayIndexNode;
    @Child protected JavaScriptNode valueNode;
    @Child private WriteElementTypeCacheNode typeCacheNode;

    final JSContext context;
    final boolean isStrict;
    final boolean writeOwn;
    @CompilationFinal private byte indexState;
    private static final byte INDEX_INT = 1;
    private static final byte INDEX_OBJECT = 2;

    /** Exact cache limit unknown, but effectively bounded by the number of types. */
    static final int BOUNDED_BY_TYPES = Integer.MAX_VALUE;

    @NeverDefault
    public static WriteElementNode create(JSContext context, boolean isStrict) {
        return create(null, null, null, context, isStrict, false);
    }

    @NeverDefault
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
    }

    protected final Object toArrayIndex(Object index) {
        if (toArrayIndexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toArrayIndexNode = insert(ToArrayIndexNode.createNoStringToIndex());
        }
        return toArrayIndexNode.execute(index);
    }

    @SuppressWarnings("unused")
    protected void requireObjectCoercible(Object target, int index) {
    }

    @SuppressWarnings("unused")
    protected void requireObjectCoercible(Object target, Object index) {
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
            CompilerDirectives.transferToInterpreterAndInvalidate();
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

    public final void executeWithTargetAndIndexAndValue(Object target, Object index, Object value, Object receiver) {
        if (typeCacheNode == null) {
            initTypeCacheNode();
        }
        typeCacheNode.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
    }

    public final void executeWithTargetAndIndexAndValue(Object target, int index, Object value, Object receiver) {
        if (typeCacheNode == null) {
            initTypeCacheNode();
        }
        typeCacheNode.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
    }

    public final void executeWithTargetAndIndexAndValue(Object target, long index, Object value, Object receiver) {
        if (typeCacheNode == null) {
            initTypeCacheNode();
        }
        typeCacheNode.executeWithTargetAndIndexUnguarded(target, index, value, receiver, this);
    }

    private void initTypeCacheNode() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        typeCacheNode = insert(WriteElementTypeCacheDispatchNodeGen.create());
    }

    abstract static class WriteElementTypeCacheNode extends JavaScriptBaseNode {
        protected WriteElementTypeCacheNode() {
        }

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root);

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, long index, Object value, Object receiver, WriteElementNode root);

    }

    abstract static class GuardedWriteElementTypeCacheNode extends JavaScriptBaseNode {
        protected GuardedWriteElementTypeCacheNode() {
        }

        public abstract boolean guard(Object target);

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value, Object receiver, WriteElementNode root);
    }

    @ImportStatic(WriteElementNode.class)
    abstract static class WriteElementTypeCacheDispatchNode extends WriteElementTypeCacheNode {

        protected WriteElementTypeCacheDispatchNode() {
            super();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1")
        protected static void doJSObjectLongIndex(Object target, long index, Object value, Object receiver, WriteElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectWriteElementTypeCacheNode objectHandler) {
            objectHandler.executeWithTargetAndIndexUnguarded(target, index, value, receiver, root);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1", replaces = {"doJSObjectLongIndex"})
        protected static void doJSObject(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectWriteElementTypeCacheNode objectHandler) {
            objectHandler.executeWithTargetAndIndexUnguarded(target, index, value, receiver, root);
        }

        @Specialization(guards = "otherHandler.guard(target)", limit = "BOUNDED_BY_TYPES")
        protected static void doOther(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached("makeHandler(target)") GuardedWriteElementTypeCacheNode otherHandler) {
            otherHandler.executeWithTargetAndIndexUnguarded(target, index, value, receiver, root);
        }

        protected static GuardedWriteElementTypeCacheNode makeHandler(Object target) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                // Handled by other specializations.
                throw Errors.shouldNotReachHere("JSDynamicObject");
            } else if (Strings.isTString(target)) {
                return StringWriteElementTypeCacheNodeGen.create();
            } else if (target instanceof Boolean) {
                return BooleanWriteElementTypeCacheNodeGen.create();
            } else if (target instanceof Number) {
                return NumberWriteElementTypeCacheNodeGen.create(target.getClass());
            } else if (target instanceof Symbol) {
                return SymbolWriteElementTypeCacheNodeGen.create();
            } else if (target instanceof BigInt) {
                return BigIntWriteElementTypeCacheNodeGen.create();
            } else {
                assert JSRuntime.isForeignObject(target) : target.getClass();
                return ForeignObjectWriteElementTypeCacheNodeGen.create();
            }
        }
    }

    abstract static class JSObjectWriteElementTypeCacheNode extends WriteElementTypeCacheNode {
        @Child private IsArrayNode isArrayNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();
        @Child private CachedSetPropertyNode setPropertyCachedNode;

        JSObjectWriteElementTypeCacheNode() {
            super();
            this.isArrayNode = IsArrayNode.createIsFastOrTypedArray();
        }

        @Specialization
        protected void doJSObjectIntegerIndex(Object target, long index, Object value, Object receiver, WriteElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile intOrStringIndexIf,
                        @Cached(inline = true) @Shared ArrayWriteElementCacheDispatchNode arrayDispatch) {
            JSDynamicObject targetObject = ((JSDynamicObject) target);
            if (arrayIf.profile(this, isArrayNode.execute(targetObject))) {
                ScriptArray array = JSObject.getArray(targetObject);

                if (intOrStringIndexIf.profile(this, JSRuntime.isArrayIndex(index))) {
                    if (!arrayDispatch.executeSetArray(this, targetObject, array, index, value, root)) {
                        setPropertyGenericEvaluatedIndex(targetObject, index, value, receiver, root);
                    }
                } else {
                    setPropertyGenericEvaluatedStringOrSymbol(targetObject, Strings.fromLong(index), value, receiver, root);
                }
            } else {
                setPropertyGeneric(targetObject, index, value, receiver, root);
            }
        }

        @Specialization
        protected void doJSObject(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile intOrStringIndexIf,
                        @Cached(inline = true) @Shared ArrayWriteElementCacheDispatchNode arrayDispatch) {
            JSDynamicObject targetObject = ((JSDynamicObject) target);
            if (arrayIf.profile(this, isArrayNode.execute(targetObject))) {
                Object objIndex = toArrayIndex(index);
                ScriptArray array = JSObject.getArray(targetObject);

                if (intOrStringIndexIf.profile(this, objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    if (!arrayDispatch.executeSetArray(this, targetObject, array, longIndex, value, root)) {
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
            ToArrayIndexNode toArrayIndex = toArrayIndexNode;
            if (toArrayIndex == null) {
                toArrayIndex = initToArrayIndexNode();
            }
            return toArrayIndex.execute(index);
        }

        private ToArrayIndexNode initToArrayIndexNode() {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return toArrayIndexNode = insert(ToArrayIndexNode.create());
        }

        private void setPropertyGenericEvaluatedIndex(JSDynamicObject targetObject, long index, Object value, Object receiver, WriteElementNode root) {
            JSObject.setWithReceiver(targetObject, index, value, receiver, root.isStrict, jsclassProfile, root);
        }

        private void setPropertyGenericEvaluatedStringOrSymbol(JSDynamicObject targetObject, Object key, Object value, Object receiver, WriteElementNode root) {
            JSObject.setWithReceiver(targetObject, key, value, receiver, root.isStrict, jsclassProfile, root);
        }

        private void setPropertyGeneric(JSDynamicObject targetObject, Object index, Object value, Object receiver, WriteElementNode root) {
            setCachedProperty(targetObject, index, value, receiver, root);
        }

        @InliningCutoff
        private void setCachedProperty(JSDynamicObject targetObject, Object index, Object value, Object receiver, WriteElementNode root) {
            if (setPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setPropertyCachedNode = insert(CachedSetPropertyNode.create(root.context, root.isStrict, root.writeOwn, root.isSuperProperty()));
            }
            setPropertyCachedNode.execute(targetObject, index, value, receiver);
        }
    }

    static ArrayWriteElementCacheNode makeArrayCacheNode(JSDynamicObject target, ScriptArray array) {
        if (JSSlowArray.isJSSlowArray(target) || JSSlowArgumentsArray.isJSSlowArgumentsObject(target)) {
            return new ExactArrayWriteElementCacheNode();
        }

        if (array.isLengthNotWritable() || !array.isExtensible()) {
            // TODO handle this case in the specializations below
            return new ExactArrayWriteElementCacheNode();
        }
        if (array instanceof LazyRegexResultArray) {
            return LazyRegexResultArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof LazyRegexResultIndicesArray) {
            return LazyRegexResultIndicesArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof AbstractConstantArray) {
            return ConstantArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof HolesIntArray) {
            return HolesIntArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof HolesDoubleArray) {
            return HolesDoubleArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof HolesJSObjectArray) {
            return HolesJSObjectArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof HolesObjectArray) {
            return HolesObjectArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof AbstractIntArray) {
            return IntArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof AbstractDoubleArray) {
            return DoubleArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof AbstractObjectArray) {
            return ObjectArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof AbstractJSObjectArray) {
            return JSObjectArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof AbstractWritableArray) {
            return WritableArrayWriteElementCacheNodeGen.create();
        } else if (array instanceof TypedArray) {
            if (array instanceof TypedIntArray) {
                return TypedIntArrayWriteElementCacheNodeGen.create((TypedArray) array);
            } else if (array instanceof TypedFloatArray) {
                return TypedFloatArrayWriteElementCacheNodeGen.create((TypedArray) array);
            } else if (array instanceof TypedBigIntArray) {
                return TypedBigIntArrayWriteElementCacheNodeGen.create((TypedArray) array);
            } else {
                throw Errors.shouldNotReachHere();
            }
        } else {
            return new ExactArrayWriteElementCacheNode();
        }
    }

    abstract static class ArrayWriteElementCacheNode extends JavaScriptBaseNode {

        ArrayWriteElementCacheNode() {
        }

        protected abstract boolean executeSetArray(JSDynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root);
    }

    @SuppressWarnings("truffle-inlining")
    @GenerateInline
    @GenerateCached(true)
    @ImportStatic(WriteElementNode.class)
    abstract static class ArrayWriteElementCacheDispatchNode extends JavaScriptBaseNode {

        ArrayWriteElementCacheDispatchNode() {
        }

        protected abstract boolean executeSetArray(Node node, JSDynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root);

        @Specialization(guards = "arrayType == cachedArrayType", limit = "BOUNDED_BY_TYPES")
        protected static boolean doDispatch(JSDynamicObject target, @SuppressWarnings("unused") ScriptArray arrayType, long index, Object value, WriteElementNode root,
                        @Cached("arrayType") ScriptArray cachedArrayType,
                        @Cached("makeHandler(target, cachedArrayType)") ArrayWriteElementCacheNode handler) {
            return handler.executeSetArray(target, cachedArrayType, index, value, root);
        }

        protected static ArrayWriteElementCacheNode makeHandler(JSDynamicObject target, ScriptArray arrayType) {
            return makeArrayCacheNode(target, arrayType);
        }
    }

    private abstract static class RecursiveCachedArrayWriteElementCacheNode extends ArrayWriteElementCacheNode {

        @Child private ArrayWriteElementCacheDispatchNode recursiveWrite;

        RecursiveCachedArrayWriteElementCacheNode() {
            super();
        }

        protected final boolean setArrayAndWrite(ScriptArray newArray, JSDynamicObject target, long index, Object value, WriteElementNode root) {
            arraySetArrayType(target, newArray);
            return executeRecursive(target, newArray, index, value, root);
        }

        protected static boolean nonHolesArrayNeedsSlowSet(JSDynamicObject target, AbstractWritableArray arrayType, long index, WriteElementNode root) {
            assert !arrayType.isHolesType();
            if (!root.writeOwn && !root.context.getArrayPrototypeNoElementsAssumption().isValid()) {
                if (!arrayType.hasElement(target, index)) {
                    return true;
                }
            }
            return false;
        }

        protected static boolean holesArrayNeedsSlowSet(JSDynamicObject target, AbstractWritableArray arrayType, long index, WriteElementNode root) {
            assert arrayType.isHolesType();
            if ((!root.writeOwn && !root.context.getArrayPrototypeNoElementsAssumption().isValid()) ||
                            (!root.context.getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(target)) ||
                            (!root.context.getFastArgumentsObjectAssumption().isValid() && JSSlowArgumentsArray.isJSSlowArgumentsObject(target))) {
                if (!arrayType.hasElement(target, index)) {
                    return true;
                }
            }
            return false;
        }

        private boolean executeRecursive(JSDynamicObject targetObject, ScriptArray array, long index, Object value, WriteElementNode root) {
            if (recursiveWrite == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursiveWrite = insert(ArrayWriteElementCacheDispatchNodeGen.create());
            }
            return recursiveWrite.executeSetArray(null, targetObject, array, index, value, root);
        }
    }

    private static class ExactArrayWriteElementCacheNode extends ArrayWriteElementCacheNode {

        ExactArrayWriteElementCacheNode() {
            super();
        }

        @Override
        protected boolean executeSetArray(JSDynamicObject target, ScriptArray array, long index, Object value, WriteElementNode root) {
            return false;
        }
    }

    abstract static class LazyRegexResultArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        @Child private DynamicObjectLibrary lazyRegexResultNode = JSObjectUtil.createDispatched(JSAbstractArray.LAZY_REGEX_RESULT_ID);
        @Child private DynamicObjectLibrary lazyRegexResultOriginalInputNode = JSObjectUtil.createDispatched(JSAbstractArray.LAZY_REGEX_ORIGINAL_INPUT_ID);
        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();
        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getStartNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();
        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getEndNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();

        LazyRegexResultArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doLazyRegexResultArray(JSDynamicObject target, LazyRegexResultArray lazyRegexResultArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsIf) {
            ScriptArray newArray = lazyRegexResultArray.createWritable(root.context, target, index, value,
                            lazyRegexResultNode, lazyRegexResultOriginalInputNode, null, substringNode, getStartNode, getEndNode);
            if (inBoundsIf.profile(this, index >= 0 && index < 0x7fff_ffff)) {
                return setArrayAndWrite(newArray, target, index, value, root);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, newArray).setElement(target, index, value, root.isStrict));
                return true;
            }
        }
    }

    abstract static class LazyRegexResultIndicesArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getStartNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();
        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getEndNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();

        LazyRegexResultIndicesArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doLazyRegexREsultIndicesArray(JSDynamicObject target, LazyRegexResultIndicesArray lazyRegexResultIndicesArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsIf) {
            ScriptArray newArray = lazyRegexResultIndicesArray.createWritable(root.context, target, index, value,
                            null, getStartNode, getEndNode);
            if (inBoundsIf.profile(this, index >= 0 && index < 0x7fff_ffff)) {
                return setArrayAndWrite(newArray, target, index, value, root);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, newArray).setElement(target, index, value, root.isStrict));
                return true;
            }
        }
    }

    abstract static class ConstantArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        ConstantArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doConstantArray(JSDynamicObject target, AbstractConstantArray constantArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedBranchProfile intValueBranch,
                        @Cached InlinedBranchProfile doubleValueBranch,
                        @Cached InlinedBranchProfile jsObjectValueBranch,
                        @Cached InlinedBranchProfile objectValueBranch,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached CreateWritableProfileAccess createWritableProfile) {
            if (inBoundsIf.profile(this, index >= 0 && index < 0x7fff_ffff)) {
                ScriptArray newArray;
                if (value instanceof Integer) {
                    intValueBranch.enter(this);
                    newArray = constantArray.createWriteableInt(target, index, (int) value, this, createWritableProfile);
                } else if (value instanceof Double) {
                    doubleValueBranch.enter(this);
                    newArray = constantArray.createWriteableDouble(target, index, (double) value, this, createWritableProfile);
                } else if (JSDynamicObject.isJSDynamicObject(value)) {
                    jsObjectValueBranch.enter(this);
                    newArray = constantArray.createWriteableJSObject(target, index, (JSDynamicObject) value, this, createWritableProfile);
                } else {
                    objectValueBranch.enter(this);
                    newArray = constantArray.createWriteableObject(target, index, value, this, createWritableProfile);
                }
                return setArrayAndWrite(newArray, target, index, value, root);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, constantArray).setElement(target, index, value, root.isStrict));
                return true;
            }
        }
    }

    abstract static class WritableArrayWriteElementCacheNode extends ArrayWriteElementCacheNode {

        WritableArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected final boolean doWritableArray(JSDynamicObject target, AbstractWritableArray writableArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsIf) {
            if (inBoundsIf.profile(this, writableArray.isInBoundsFast(target, index))) {
                arraySetArrayType(target, writableArray.setElement(target, index, value, root.isStrict));
                return true;
            } else {
                return false;
            }
        }
    }

    abstract static class IntArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        IntArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doIntArray(JSDynamicObject target, AbstractIntArray intArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedBranchProfile intValueBranch,
                        @Cached InlinedBranchProfile doubleValueBranch,
                        @Cached InlinedBranchProfile objectValueBranch,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile supportedNonZeroIf,
                        @Cached InlinedConditionProfile supportedZeroIf,
                        @Cached InlinedConditionProfile supportedContiguousIf,
                        @Cached InlinedConditionProfile supportedHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            if (value instanceof Integer) {
                intValueBranch.enter(this);
                return doIntArrayWithIntValue(target, intArray, index, (int) value, root,
                                inBoundsFastIf,
                                inBoundsIf,
                                supportedNonZeroIf,
                                supportedZeroIf,
                                supportedContiguousIf,
                                supportedHolesIf,
                                needPrototypeBranch,
                                setSupportedProfile);
            } else if (value instanceof Double) {
                doubleValueBranch.enter(this);
                double doubleValue = (double) value;
                return setArrayAndWrite(intArray.toDouble(target, index, doubleValue), target, index, doubleValue, root);
            } else {
                objectValueBranch.enter(this);
                return setArrayAndWrite(intArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean doIntArrayWithIntValue(JSDynamicObject target, AbstractIntArray intArray, long index, int intValue, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile supportedNonZeroIf,
                        @Cached InlinedConditionProfile supportedZeroIf,
                        @Cached InlinedConditionProfile supportedContiguousIf,
                        @Cached InlinedConditionProfile supportedHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            assert !(intArray instanceof HolesIntArray);
            if (nonHolesArrayNeedsSlowSet(target, intArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            int iIndex = (int) index;
            if (inBoundsFastIf.profile(this, intArray.isInBoundsFast(target, index) && !mightTransferToNonContiguous(intArray, target, index))) {
                intArray.setInBoundsFast(target, iIndex, intValue);
                return true;
            } else if (inBoundsIf.profile(this, intArray.isInBounds(target, iIndex) && !mightTransferToNonContiguous(intArray, target, index))) {
                intArray.setInBounds(target, iIndex, intValue, this, setSupportedProfile);
                return true;
            } else if (supportedNonZeroIf.profile(this, intArray.isSupported(target, index) && !mightTransferToNonContiguous(intArray, target, index))) {
                intArray.setSupported(target, iIndex, intValue, this, setSupportedProfile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedZeroIf.profile(this, mightTransferToNonContiguous(intArray, target, index) && intArray.isSupported(target, index))) {
                    toArrayType = intArray.toNonContiguous(target, iIndex, intValue, this, setSupportedProfile);
                } else if (supportedContiguousIf.profile(this, !(intArray instanceof AbstractContiguousIntArray) && intArray.isSupportedContiguous(target, index))) {
                    toArrayType = intArray.toContiguous(target, index, intValue);
                } else if (supportedHolesIf.profile(this, intArray.isSupportedHoles(target, index))) {
                    toArrayType = intArray.toHoles(target, index, intValue);
                } else {
                    assert intArray.isSparse(target, index);
                    toArrayType = intArray.toSparse(target, index, intValue);
                }
                return setArrayAndWrite(toArrayType, target, index, intValue, root);
            }
        }

        private static boolean mightTransferToNonContiguous(AbstractIntArray intArray, JSDynamicObject target, long index) {
            return intArray instanceof ContiguousIntArray && index == 0 && intArray.firstElementIndex(target) == 1;
        }
    }

    abstract static class DoubleArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        DoubleArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doDoubleArray(JSDynamicObject target, AbstractDoubleArray doubleArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedBranchProfile intValueBranch,
                        @Cached InlinedBranchProfile doubleValueBranch,
                        @Cached InlinedBranchProfile objectValueBranch,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile supportedIf,
                        @Cached InlinedConditionProfile supportedContiguousIf,
                        @Cached InlinedConditionProfile supportedHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            double doubleValue;
            if (value instanceof Double) {
                doubleValueBranch.enter(this);
                doubleValue = (double) value;
            } else if (value instanceof Integer) {
                intValueBranch.enter(this);
                doubleValue = (int) value;
            } else {
                objectValueBranch.enter(this);
                return setArrayAndWrite(doubleArray.toObject(target, index, value), target, index, value, root);
            }
            return executeWithDoubleValueInner(target, doubleArray, index, doubleValue, root,
                            inBoundsFastIf,
                            inBoundsIf,
                            supportedIf,
                            supportedContiguousIf,
                            supportedHolesIf,
                            needPrototypeBranch,
                            setSupportedProfile);
        }

        private boolean executeWithDoubleValueInner(JSDynamicObject target, AbstractDoubleArray doubleArray, long index, double doubleValue, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile supportedIf,
                        @Cached InlinedConditionProfile supportedContiguousIf,
                        @Cached InlinedConditionProfile supportedHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            assert !(doubleArray instanceof HolesDoubleArray);
            if (nonHolesArrayNeedsSlowSet(target, doubleArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            int iIndex = (int) index;
            if (inBoundsFastIf.profile(this, doubleArray.isInBoundsFast(target, index))) {
                doubleArray.setInBoundsFast(target, iIndex, doubleValue);
                return true;
            } else if (inBoundsIf.profile(this, doubleArray.isInBounds(target, iIndex))) {
                doubleArray.setInBounds(target, iIndex, doubleValue, this, setSupportedProfile);
                return true;
            } else if (supportedIf.profile(this, doubleArray.isSupported(target, index))) {
                doubleArray.setSupported(target, iIndex, doubleValue, this, setSupportedProfile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedContiguousIf.profile(this, !(doubleArray instanceof AbstractContiguousDoubleArray) && doubleArray.isSupportedContiguous(target, index))) {
                    toArrayType = doubleArray.toContiguous(target, index, doubleValue);
                } else if (supportedHolesIf.profile(this, doubleArray.isSupportedHoles(target, index))) {
                    toArrayType = doubleArray.toHoles(target, index, doubleValue);
                } else {
                    assert doubleArray.isSparse(target, index);
                    toArrayType = doubleArray.toSparse(target, index, doubleValue);
                }
                return setArrayAndWrite(toArrayType, target, index, doubleValue, root);
            }
        }
    }

    abstract static class ObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        ObjectArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doObjectArray(JSDynamicObject target, AbstractObjectArray objectArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile supportedIf,
                        @Cached InlinedConditionProfile supportedContiguousIf,
                        @Cached InlinedConditionProfile supportedHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            assert !(objectArray instanceof HolesObjectArray);
            if (nonHolesArrayNeedsSlowSet(target, objectArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            int iIndex = (int) index;
            if (inBoundsFastIf.profile(this, objectArray.isInBoundsFast(target, index))) {
                objectArray.setInBoundsFast(target, iIndex, value);
                return true;
            } else if (inBoundsIf.profile(this, objectArray.isInBounds(target, iIndex))) {
                objectArray.setInBounds(target, iIndex, value, this, setSupportedProfile);
                return true;
            } else if (supportedIf.profile(this, objectArray.isSupported(target, index))) {
                objectArray.setSupported(target, iIndex, value, this, setSupportedProfile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedContiguousIf.profile(this, !(objectArray instanceof AbstractContiguousObjectArray) && objectArray.isSupportedContiguous(target, index))) {
                    toArrayType = objectArray.toContiguous(target, index, value);
                } else if (supportedHolesIf.profile(this, objectArray.isSupportedHoles(target, index))) {
                    toArrayType = objectArray.toHoles(target, index, value);
                } else {
                    assert objectArray.isSparse(target, index);
                    toArrayType = objectArray.toSparse(target, index, value);
                }
                return setArrayAndWrite(toArrayType, target, index, value, root);
            }
        }
    }

    abstract static class JSObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        JSObjectArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doJSObjectArray(JSDynamicObject target, AbstractJSObjectArray jsobjectArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedBranchProfile jsObjectValueBranch,
                        @Cached InlinedBranchProfile objectValueBranch,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile supportedIf,
                        @Cached InlinedConditionProfile supportedContiguousIf,
                        @Cached InlinedConditionProfile supportedHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            if (JSDynamicObject.isJSDynamicObject(value)) {
                jsObjectValueBranch.enter(this);
                JSDynamicObject jsobjectValue = (JSDynamicObject) value;
                return executeWithJSObjectValueInner(target, jsobjectArray, index, jsobjectValue, root,
                                inBoundsFastIf,
                                inBoundsIf,
                                supportedIf,
                                supportedContiguousIf,
                                supportedHolesIf,
                                needPrototypeBranch,
                                setSupportedProfile);
            } else {
                objectValueBranch.enter(this);
                return setArrayAndWrite(jsobjectArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean executeWithJSObjectValueInner(JSDynamicObject target, AbstractJSObjectArray jsobjectArray, long index, JSDynamicObject jsobjectValue, WriteElementNode root,
                        InlinedConditionProfile inBoundsFastIf,
                        InlinedConditionProfile inBoundsIf,
                        InlinedConditionProfile supportedIf,
                        InlinedConditionProfile supportedContiguousIf,
                        InlinedConditionProfile supportedHolesIf,
                        InlinedBranchProfile needPrototypeBranch,
                        SetSupportedProfileAccess setSupportedProfile) {
            assert !(jsobjectArray instanceof HolesJSObjectArray);
            int iIndex = (int) index;
            if (nonHolesArrayNeedsSlowSet(target, jsobjectArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            if (inBoundsFastIf.profile(this, jsobjectArray.isInBoundsFast(target, index))) {
                jsobjectArray.setInBoundsFast(target, iIndex, jsobjectValue);
                return true;
            } else if (inBoundsIf.profile(this, jsobjectArray.isInBounds(target, iIndex))) {
                jsobjectArray.setInBounds(target, iIndex, jsobjectValue, this, setSupportedProfile);
                return true;
            } else if (supportedIf.profile(this, jsobjectArray.isSupported(target, index))) {
                jsobjectArray.setSupported(target, iIndex, jsobjectValue, this, setSupportedProfile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (supportedContiguousIf.profile(this, !(jsobjectArray instanceof AbstractContiguousJSObjectArray) && jsobjectArray.isSupportedContiguous(target, index))) {
                    toArrayType = jsobjectArray.toContiguous(target, index, jsobjectValue);
                } else if (supportedHolesIf.profile(this, jsobjectArray.isSupportedHoles(target, index))) {
                    toArrayType = jsobjectArray.toHoles(target, index, jsobjectValue);
                } else {
                    assert jsobjectArray.isSparse(target, index);
                    toArrayType = jsobjectArray.toSparse(target, index, jsobjectValue);
                }
                return setArrayAndWrite(toArrayType, target, index, jsobjectValue, root);
            }
        }
    }

    abstract static class HolesIntArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        HolesIntArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doHolesIntArray(JSDynamicObject target, HolesIntArray holesIntArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedBranchProfile intValueBranch,
                        @Cached InlinedBranchProfile doubleValueBranch,
                        @Cached InlinedBranchProfile objectValueBranch,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile containsHolesIf,
                        @Cached InlinedConditionProfile inBoundsFastHoleIf,
                        @Cached InlinedConditionProfile supportedContainsHolesIf,
                        @Cached InlinedConditionProfile supportedNotContainsHolesIf,
                        @Cached InlinedConditionProfile hasExplicitHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            if (value instanceof Integer) {
                intValueBranch.enter(this);
                int intValue = (int) value;
                return executeWithIntValueInner(target, holesIntArray, index, intValue, root,
                                inBoundsFastIf,
                                inBoundsIf,
                                containsHolesIf,
                                inBoundsFastHoleIf,
                                supportedContainsHolesIf,
                                supportedNotContainsHolesIf,
                                hasExplicitHolesIf,
                                needPrototypeBranch,
                                setSupportedProfile);
            } else if (value instanceof Double) {
                doubleValueBranch.enter(this);
                double doubleValue = (double) value;
                return setArrayAndWrite(holesIntArray.toDouble(target, index, doubleValue), target, index, doubleValue, root);
            } else {
                objectValueBranch.enter(this);
                return setArrayAndWrite(holesIntArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean executeWithIntValueInner(JSDynamicObject target, HolesIntArray holesIntArray, long index, int intValue, WriteElementNode root,
                        InlinedConditionProfile inBoundsFastIf,
                        InlinedConditionProfile inBoundsIf,
                        InlinedConditionProfile containsHolesIf,
                        InlinedConditionProfile inBoundsFastHoleIf,
                        InlinedConditionProfile supportedContainsHolesIf,
                        InlinedConditionProfile supportedNotContainsHolesIf,
                        InlinedConditionProfile hasExplicitHolesIf,
                        InlinedBranchProfile needPrototypeBranch,
                        SetSupportedProfileAccess setSupportedProfile) {
            if (holesArrayNeedsSlowSet(target, holesIntArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            int iIndex = (int) index;
            boolean containsHoles = containsHolesIf.profile(this, containsHoles(target, holesIntArray, index, hasExplicitHolesIf));
            if (containsHoles && inBoundsFastIf.profile(this, holesIntArray.isInBoundsFast(target, index) && !HolesIntArray.isHoleValue(intValue))) {
                if (inBoundsFastHoleIf.profile(this, holesIntArray.isHoleFast(target, iIndex))) {
                    holesIntArray.setInBoundsFastHole(target, iIndex, intValue);
                } else {
                    holesIntArray.setInBoundsFastNonHole(target, iIndex, intValue);
                }
                return true;
            } else if (containsHoles && inBoundsIf.profile(this, holesIntArray.isInBounds(target, iIndex) && !HolesIntArray.isHoleValue(intValue))) {
                holesIntArray.setInBounds(target, iIndex, intValue, this, setSupportedProfile);
                return true;
            } else if (containsHoles && supportedContainsHolesIf.profile(this, holesIntArray.isSupported(target, index) && !HolesIntArray.isHoleValue(intValue))) {
                holesIntArray.setSupported(target, iIndex, intValue, this, setSupportedProfile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (!containsHoles && supportedNotContainsHolesIf.profile(this, holesIntArray.isSupported(target, index))) {
                    toArrayType = holesIntArray.toNonHoles(target, index, intValue);
                } else {
                    assert holesIntArray.isSparse(target, index) || HolesIntArray.isHoleValue(intValue);
                    toArrayType = holesIntArray.toSparse(target, index, intValue);
                }
                return setArrayAndWrite(toArrayType, target, index, intValue, root);
            }
        }

        private boolean containsHoles(JSDynamicObject target, HolesIntArray holesIntArray, long index, InlinedConditionProfile hasExplicitHolesIf) {
            return hasExplicitHolesIf.profile(this, JSArray.arrayGetHoleCount(target) > 0) || !holesIntArray.isInBoundsFast(target, index);
        }
    }

    abstract static class HolesDoubleArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        HolesDoubleArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doHolesDoubleArray(JSDynamicObject target, HolesDoubleArray holesDoubleArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedBranchProfile intValueBranch,
                        @Cached InlinedBranchProfile doubleValueBranch,
                        @Cached InlinedBranchProfile objectValueBranch,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile containsHolesIf,
                        @Cached InlinedConditionProfile inBoundsFastHoleIf,
                        @Cached InlinedConditionProfile supportedContainsHolesIf,
                        @Cached InlinedConditionProfile supportedNotContainsHolesIf,
                        @Cached InlinedConditionProfile hasExplicitHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            double doubleValue;
            if (value instanceof Double) {
                doubleValueBranch.enter(this);
                doubleValue = (double) value;
            } else if (value instanceof Integer) {
                intValueBranch.enter(this);
                doubleValue = (int) value;
            } else {
                objectValueBranch.enter(this);
                return setArrayAndWrite(holesDoubleArray.toObject(target, index, value), target, index, value, root);
            }

            return executeWithDoubleValueInner(target, holesDoubleArray, index, doubleValue, root,
                            inBoundsFastIf,
                            inBoundsIf,
                            containsHolesIf,
                            inBoundsFastHoleIf,
                            supportedContainsHolesIf,
                            supportedNotContainsHolesIf,
                            hasExplicitHolesIf,
                            needPrototypeBranch,
                            setSupportedProfile);
        }

        private boolean executeWithDoubleValueInner(JSDynamicObject target, HolesDoubleArray holesDoubleArray, long index, double doubleValue, WriteElementNode root,
                        InlinedConditionProfile inBoundsFastIf,
                        InlinedConditionProfile inBoundsIf,
                        InlinedConditionProfile containsHolesIf,
                        InlinedConditionProfile inBoundsFastHoleIf,
                        InlinedConditionProfile supportedContainsHolesIf,
                        InlinedConditionProfile supportedNotContainsHolesIf,
                        InlinedConditionProfile hasExplicitHolesIf,
                        InlinedBranchProfile needPrototypeBranch,
                        SetSupportedProfileAccess setSupportedProfile) {
            if (holesArrayNeedsSlowSet(target, holesDoubleArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            int iIndex = (int) index;
            boolean containsHoles = containsHolesIf.profile(this, containsHoles(target, holesDoubleArray, index, hasExplicitHolesIf));
            if (containsHoles && inBoundsFastIf.profile(this, holesDoubleArray.isInBoundsFast(target, index) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                if (inBoundsFastHoleIf.profile(this, holesDoubleArray.isHoleFast(target, iIndex))) {
                    holesDoubleArray.setInBoundsFastHole(target, iIndex, doubleValue);
                } else {
                    holesDoubleArray.setInBoundsFastNonHole(target, iIndex, doubleValue);
                }
                return true;
            } else if (containsHoles && inBoundsIf.profile(this, holesDoubleArray.isInBounds(target, iIndex) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                holesDoubleArray.setInBounds(target, iIndex, doubleValue, this, setSupportedProfile);
                return true;
            } else if (containsHoles && supportedContainsHolesIf.profile(this, holesDoubleArray.isSupported(target, index) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                holesDoubleArray.setSupported(target, iIndex, doubleValue, this, setSupportedProfile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (!containsHoles && supportedNotContainsHolesIf.profile(this, holesDoubleArray.isSupported(target, index))) {
                    toArrayType = holesDoubleArray.toNonHoles(target, index, doubleValue);
                } else {
                    assert holesDoubleArray.isSparse(target, index) || HolesDoubleArray.isHoleValue(doubleValue);
                    toArrayType = holesDoubleArray.toSparse(target, index, doubleValue);
                }
                return setArrayAndWrite(toArrayType, target, index, doubleValue, root);
            }
        }

        private boolean containsHoles(JSDynamicObject target, HolesDoubleArray holesDoubleArray, long index, InlinedConditionProfile hasExplicitHolesIf) {
            return hasExplicitHolesIf.profile(this, JSArray.arrayGetHoleCount(target) > 0) || !holesDoubleArray.isInBoundsFast(target, index);
        }
    }

    abstract static class HolesJSObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        HolesJSObjectArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doJSObjectArray(JSDynamicObject target, HolesJSObjectArray holesArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedBranchProfile jsObjectValueBranch,
                        @Cached InlinedBranchProfile objectValueBranch,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile containsHolesIf,
                        @Cached InlinedConditionProfile inBoundsFastHoleIf,
                        @Cached InlinedConditionProfile supportedContainsHolesIf,
                        @Cached InlinedConditionProfile supportedNotContainsHolesIf,
                        @Cached InlinedConditionProfile hasExplicitHolesIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            if (JSDynamicObject.isJSDynamicObject(value)) {
                jsObjectValueBranch.enter(this);
                return executeWithJSObjectValueInner(target, holesArray, index, (JSDynamicObject) value, root,
                                inBoundsFastIf,
                                inBoundsIf,
                                containsHolesIf,
                                inBoundsFastHoleIf,
                                supportedContainsHolesIf,
                                supportedNotContainsHolesIf,
                                hasExplicitHolesIf,
                                needPrototypeBranch,
                                setSupportedProfile);
            } else {
                objectValueBranch.enter(this);
                return setArrayAndWrite(holesArray.toObject(target, index, value), target, index, value, root);
            }
        }

        private boolean executeWithJSObjectValueInner(JSDynamicObject target, HolesJSObjectArray jsobjectArray, long index, JSDynamicObject value, WriteElementNode root,
                        InlinedConditionProfile inBoundsFastIf,
                        InlinedConditionProfile inBoundsIf,
                        InlinedConditionProfile containsHolesIf,
                        InlinedConditionProfile inBoundsFastHoleIf,
                        InlinedConditionProfile supportedContainsHolesIf,
                        InlinedConditionProfile supportedNotContainsHolesIf,
                        InlinedConditionProfile hasExplicitHolesIf,
                        InlinedBranchProfile needPrototypeBranch,
                        SetSupportedProfileAccess setSupportedProfile) {
            if (holesArrayNeedsSlowSet(target, jsobjectArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            boolean containsHoles = containsHolesIf.profile(this, containsHoles(target, jsobjectArray, index, hasExplicitHolesIf));
            if (containsHoles && inBoundsFastIf.profile(this, jsobjectArray.isInBoundsFast(target, index))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                if (inBoundsFastHoleIf.profile(this, jsobjectArray.isHoleFast(target, (int) index))) {
                    jsobjectArray.setInBoundsFastHole(target, (int) index, value);
                } else {
                    jsobjectArray.setInBoundsFastNonHole(target, (int) index, value);
                }
                return true;
            } else if (containsHoles && inBoundsIf.profile(this, jsobjectArray.isInBounds(target, (int) index))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                jsobjectArray.setInBounds(target, (int) index, value, this, setSupportedProfile);
                return true;
            } else if (containsHoles && supportedContainsHolesIf.profile(this, jsobjectArray.isSupported(target, index))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                jsobjectArray.setSupported(target, (int) index, value, this, setSupportedProfile);
                return true;
            } else {
                ScriptArray toArrayType;
                if (!containsHoles && supportedNotContainsHolesIf.profile(this, jsobjectArray.isSupported(target, index))) {
                    toArrayType = jsobjectArray.toNonHoles(target, index, value);
                } else {
                    assert jsobjectArray.isSparse(target, index);
                    toArrayType = jsobjectArray.toSparse(target, index, value);
                }
                return setArrayAndWrite(toArrayType, target, index, value, root);
            }
        }

        private boolean containsHoles(JSDynamicObject target, HolesJSObjectArray holesJSObjectArray, long index, InlinedConditionProfile hasExplicitHolesIf) {
            return hasExplicitHolesIf.profile(this, JSArray.arrayGetHoleCount(target) > 0) || !holesJSObjectArray.isInBoundsFast(target, index);
        }
    }

    abstract static class HolesObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        HolesObjectArrayWriteElementCacheNode() {
            super();
        }

        @Specialization
        protected boolean doHolesObjectArray(JSDynamicObject target, HolesObjectArray objectArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsFastIf,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached InlinedConditionProfile inBoundsFastHoleIf,
                        @Cached InlinedConditionProfile supportedIf,
                        @Cached InlinedBranchProfile needPrototypeBranch,
                        @Cached SetSupportedProfileAccess setSupportedProfile) {
            if (holesArrayNeedsSlowSet(target, objectArray, index, root)) {
                needPrototypeBranch.enter(this);
                return false;
            }
            if (inBoundsFastIf.profile(this, objectArray.isInBoundsFast(target, index))) {
                assert !HolesObjectArray.isHoleValue(value);
                if (inBoundsFastHoleIf.profile(this, objectArray.isHoleFast(target, (int) index))) {
                    objectArray.setInBoundsFastHole(target, (int) index, value);
                } else {
                    objectArray.setInBoundsFastNonHole(target, (int) index, value);
                }
                return true;
            } else if (inBoundsIf.profile(this, objectArray.isInBounds(target, (int) index))) {
                assert !HolesObjectArray.isHoleValue(value);
                objectArray.setInBounds(target, (int) index, value, this, setSupportedProfile);
                return true;
            } else if (supportedIf.profile(this, objectArray.isSupported(target, index))) {
                assert !HolesObjectArray.isHoleValue(value);
                objectArray.setSupported(target, (int) index, value, this, setSupportedProfile);
                return true;
            } else {
                assert objectArray.isSparse(target, index);
                return setArrayAndWrite(objectArray.toSparse(target, index, value), target, index, value, root);
            }
        }
    }

    private abstract static class AbstractTypedArrayWriteElementCacheNode extends ArrayWriteElementCacheNode {

        @Child protected InteropLibrary interop;

        AbstractTypedArrayWriteElementCacheNode(TypedArray arrayType) {
            super();
            this.interop = arrayType.isInterop() ? InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit) : InteropLibrary.getUncached();
        }

    }

    abstract static class TypedIntArrayWriteElementCacheNode extends AbstractTypedArrayWriteElementCacheNode {

        TypedIntArrayWriteElementCacheNode(TypedArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected final boolean doTypedIntArrayIntValue(JSDynamicObject target, TypedIntArray typedArray, long index, int iValue, WriteElementNode root,
                        @Cached @Shared InlinedConditionProfile inBoundsIf) {
            if (!JSArrayBufferView.hasDetachedBuffer(target, root.context) && inBoundsIf.profile(this, typedArray.hasElement(target, index))) {
                typedArray.setInt(target, (int) index, iValue, interop);
            } else {
                // do nothing; cf. ES6 9.4.5.9 IntegerIndexedElementSet(O, index, value)
            }
            return true;
        }

        protected static boolean isSpecial(TypedIntArray typedArray) {
            return typedArray instanceof AbstractUint32Array || typedArray instanceof AbstractUint8ClampedArray;
        }

        @Specialization(guards = "!isSpecial(typedArray)", replaces = "doTypedIntArrayIntValue")
        protected final boolean doTypedIntArray(JSDynamicObject target, TypedIntArray typedArray, long index, Object value, WriteElementNode root,
                        @Cached JSToInt32Node toIntNode,
                        @Cached @Shared InlinedConditionProfile inBoundsIf) {
            int iValue = toIntNode.executeInt(value); // could throw
            return doTypedIntArrayIntValue(target, typedArray, index, iValue, root, inBoundsIf);
        }

        @Specialization(replaces = "doTypedIntArrayIntValue")
        protected final boolean doTypedIntArray(JSDynamicObject target, AbstractUint32Array typedArray, long index, Object value, WriteElementNode root,
                        @Cached JSToNumberNode toNumberNode,
                        @Cached @Shared InlinedConditionProfile inBoundsIf) {
            int iValue = (int) JSRuntime.toUInt32(toNumberNode.executeNumber(value));
            return doTypedIntArrayIntValue(target, typedArray, index, iValue, root, inBoundsIf);
        }

        @Specialization(replaces = "doTypedIntArrayIntValue")
        protected final boolean doTypedIntArray(JSDynamicObject target, AbstractUint8ClampedArray typedArray, long index, Object value, WriteElementNode root,
                        @Cached JSToDoubleNode toDoubleNode,
                        @Cached @Shared InlinedConditionProfile inBoundsIf) {
            double doubleValue = toDoubleNode.executeDouble(value);
            int iValue = Uint8ClampedArray.toInt(doubleValue);
            return doTypedIntArrayIntValue(target, typedArray, index, iValue, root, inBoundsIf);
        }
    }

    abstract static class TypedBigIntArrayWriteElementCacheNode extends AbstractTypedArrayWriteElementCacheNode {

        @Child private JSToBigIntNode toBigIntNode;

        TypedBigIntArrayWriteElementCacheNode(TypedArray arrayType) {
            super(arrayType);
            this.toBigIntNode = JSToBigIntNode.create();
        }

        @Specialization
        protected final boolean doBigIntArray(JSDynamicObject target, TypedBigIntArray typedArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsIf) {
            BigInt biValue = toBigIntNode.executeBigInteger(value); // could throw
            if (!JSArrayBufferView.hasDetachedBuffer(target, root.context) && inBoundsIf.profile(this, typedArray.hasElement(target, index))) {
                typedArray.setBigInt(target, (int) index, biValue, interop);
            }
            return true;
        }
    }

    abstract static class TypedFloatArrayWriteElementCacheNode extends AbstractTypedArrayWriteElementCacheNode {

        TypedFloatArrayWriteElementCacheNode(TypedArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected boolean doTypedFloatArray(JSDynamicObject target, TypedFloatArray typedArray, long index, Object value, WriteElementNode root,
                        @Cached InlinedConditionProfile inBoundsIf,
                        @Cached JSToDoubleNode toDouble) {
            double dValue = toDouble.executeDouble(value); // could throw
            if (!JSArrayBufferView.hasDetachedBuffer(target, root.context) && inBoundsIf.profile(this, typedArray.hasElement(target, index))) {
                typedArray.setDouble(target, (int) index, dValue, interop);
            } else {
                // do nothing; cf. ES6 9.4.5.9 IntegerIndexedElementSet(O, index, value)
            }
            return true;
        }
    }

    private abstract static class ToPropertyKeyCachedWriteElementTypeCacheNode extends GuardedWriteElementTypeCacheNode {
        protected final JSClassProfile classProfile = JSClassProfile.create();

        ToPropertyKeyCachedWriteElementTypeCacheNode() {
            super();
        }
    }

    abstract static class StringWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {

        StringWriteElementTypeCacheNode() {
            super();
        }

        @Specialization
        protected void doStringIntegerIndex(Object target, long index, Object value, Object receiver, WriteElementNode root,
                        @Cached @Shared InlinedConditionProfile isImmutable) {
            TruffleString string = (TruffleString) target;
            if (isImmutable.profile(this, index >= 0 && index < Strings.length(string))) {
                // cannot set characters of immutable strings
                if (root.isStrict) {
                    throw Errors.createTypeErrorNotWritableIndex(index, string, this);
                }
                return;
            } else {
                JSObject.setWithReceiver(JSString.create(root.context, getRealm(), string), index, value, receiver, root.isStrict, classProfile, root);
            }
        }

        @Specialization(replaces = {"doStringIntegerIndex"})
        protected void doString(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached @Shared InlinedConditionProfile isImmutable,
                        @Cached ToArrayIndexNoToPropertyKeyNode toArrayIndexNode,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            TruffleString string = (TruffleString) target;
            long longIndex = toArrayIndexNode.executeLong(this, index);
            if (isImmutable.profile(this, longIndex >= 0 && longIndex < Strings.length(string))) {
                // cannot set characters of immutable strings
                if (root.isStrict) {
                    throw Errors.createTypeErrorNotWritableIndex(longIndex, string, this);
                }
                return;
            }
            Object propertyKey = indexToPropertyKeyNode.execute(index);
            JSObject.setWithReceiver(JSString.create(root.context, getRealm(), string), propertyKey, value, receiver, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof TruffleString;
        }
    }

    abstract static class NumberWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        private final Class<?> numberClass;

        NumberWriteElementTypeCacheNode(Class<?> numberClass) {
            super();
            this.numberClass = numberClass;
        }

        @Specialization
        protected void doNumber(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            Number number = (Number) CompilerDirectives.castExact(target, numberClass);
            JSObject.setWithReceiver(JSNumber.create(root.context, getRealm(), number), index, value, receiver, root.isStrict, classProfile, root);
        }

        @Specialization
        protected void doNumber(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            Number number = (Number) CompilerDirectives.castExact(target, numberClass);
            JSObject.setWithReceiver(JSNumber.create(root.context, getRealm(), number), indexToPropertyKeyNode.execute(index), value, receiver, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return CompilerDirectives.isExact(target, numberClass);
        }
    }

    abstract static class BooleanWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        BooleanWriteElementTypeCacheNode() {
            super();
        }

        @Specialization
        protected void doBoolean(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            Boolean bool = (Boolean) target;
            JSObject.setWithReceiver(JSBoolean.create(root.context, getRealm(), bool), index, value, receiver, root.isStrict, classProfile, root);
        }

        @Specialization
        protected void doBoolean(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            Boolean bool = (Boolean) target;
            JSObject.setWithReceiver(JSBoolean.create(root.context, getRealm(), bool), indexToPropertyKeyNode.execute(index), value, receiver, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Boolean;
        }
    }

    abstract static class SymbolWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        SymbolWriteElementTypeCacheNode() {
            super();
        }

        @Specialization
        protected void doSymbol(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            if (root.isStrict) {
                throw Errors.createTypeError("cannot set element on Symbol in strict mode", this);
            }
            Symbol symbol = (Symbol) target;
            JSObject.setWithReceiver(JSSymbol.create(root.context, getRealm(), symbol), index, value, receiver, root.isStrict, classProfile, root);
        }

        @Specialization
        protected void doSymbol(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            if (root.isStrict) {
                throw Errors.createTypeError("cannot set element on Symbol in strict mode", this);
            }
            Symbol symbol = (Symbol) target;
            JSObject.setWithReceiver(JSSymbol.create(root.context, getRealm(), symbol), indexToPropertyKeyNode.execute(index), value, receiver, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Symbol;
        }
    }

    abstract static class BigIntWriteElementTypeCacheNode extends ToPropertyKeyCachedWriteElementTypeCacheNode {
        BigIntWriteElementTypeCacheNode() {
            super();
        }

        @Specialization
        protected void doBigIntIntegerIndex(Object target, long index, Object value, Object receiver, WriteElementNode root) {
            BigInt bigInt = (BigInt) target;
            JSContext context = root.context;
            JSObject.setWithReceiver(JSBigInt.create(context, getRealm(), bigInt), index, value, receiver, root.isStrict, classProfile, root);
        }

        @Specialization
        protected void doBigInt(Object target, Object index, Object value, Object receiver, WriteElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            BigInt bigInt = (BigInt) target;
            JSContext context = root.context;
            JSObject.setWithReceiver(JSBigInt.create(context, getRealm(), bigInt), indexToPropertyKeyNode.execute(index), value, receiver, root.isStrict, classProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof BigInt;
        }
    }

    abstract static class ForeignObjectWriteElementTypeCacheNode extends GuardedWriteElementTypeCacheNode {
        @Child private InteropLibrary interop;
        @Child private InteropLibrary keyInterop;
        @Child private InteropLibrary setterInterop;
        @Child private JSToPropertyKeyNode toPropertyKeyNode;
        @Child private ExportValueNode exportValue;
        @Child private ToArrayIndexNode toArrayIndexNode;

        ForeignObjectWriteElementTypeCacheNode() {
            super();
            this.exportValue = ExportValueNode.create();
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
            this.keyInterop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected void doForeignObject(Object target, Object index, Object value, @SuppressWarnings("unused") Object receiver, WriteElementNode root,
                        @Cached InlinedExactClassProfile classProfile,
                        @Cached InlinedBranchProfile errorBranch) {
            Object truffleObject = classProfile.profile(this, target);
            if (interop.isNull(truffleObject)) {
                throw Errors.createTypeErrorCannotSetProperty(index, truffleObject, this, root.getContext());
            }
            Object propertyKey;
            Object exportedValue = exportValue.execute(value);
            boolean hasArrayElements = interop.hasArrayElements(truffleObject);
            if (hasArrayElements) {
                Object indexOrPropertyKey = toArrayIndex(index);
                if (indexOrPropertyKey instanceof Long) {
                    try {
                        interop.writeArrayElement(truffleObject, (long) indexOrPropertyKey, exportedValue);
                        return;
                    } catch (InvalidArrayIndexException | UnsupportedTypeException | UnsupportedMessageException e) {
                        if (root.isStrict) {
                            errorBranch.enter(this);
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
                propertyKey = toPropertyKey(index);
            }
            if (root.context.getLanguageOptions().hasForeignHashProperties() && interop.hasHashEntries(truffleObject)) {
                try {
                    interop.writeHashEntry(truffleObject, propertyKey, exportedValue);
                    return;
                } catch (UnknownKeyException | UnsupportedMessageException | UnsupportedTypeException e) {
                    if (root.isStrict) {
                        errorBranch.enter(this);
                        throw Errors.createTypeErrorInteropException(truffleObject, e, "writeHashEntry", this);
                    } else {
                        return;
                    }
                }
            }
            if (propertyKey instanceof Symbol) {
                return;
            }
            TruffleString stringKey = (TruffleString) propertyKey;
            if (hasArrayElements && Strings.equals(JSAbstractArray.LENGTH, stringKey)) {
                JSInteropUtil.setArraySize(truffleObject, value, root.isStrict, interop, this, null);
            }
            if (root.context.isOptionNashornCompatibilityMode()) {
                if (tryInvokeSetter(truffleObject, stringKey, exportedValue, root.context)) {
                    return;
                }
            }
            try {
                String javaPropertyKey = Strings.toJavaString(stringKey);
                interop.writeMember(truffleObject, javaPropertyKey, exportedValue);
            } catch (UnknownIdentifierException | UnsupportedTypeException | UnsupportedMessageException e) {
                if (root.isStrict) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorInteropException(truffleObject, e, "writeMember", this);
                } else {
                    return;
                }
            }
        }

        @Override
        public boolean guard(Object target) {
            return JSRuntime.isForeignObject(target);
        }

        private boolean tryInvokeSetter(Object thisObj, TruffleString key, Object value, JSContext context) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = getRealm().getEnv();
            if (env.isHostObject(thisObj)) {
                TruffleString setterKey = PropertyCacheNode.getAccessorKey(Strings.SET, key);
                if (setterKey == null) {
                    return false;
                }
                if (setterInterop == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setterInterop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                if (!setterInterop.isMemberInvocable(thisObj, Strings.toJavaString(setterKey))) {
                    return false;
                }
                try {
                    setterInterop.invokeMember(thisObj, Strings.toJavaString(setterKey), value);
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

        private Object toPropertyKey(Object index) {
            if (toPropertyKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toPropertyKeyNode = insert(JSToPropertyKeyNode.create());
            }
            return toPropertyKeyNode.execute(index);
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

    @NeverDefault
    public static WriteElementNode createCachedInterop() {
        return create(JavaScriptLanguage.get(null).getJSContext(), true);
    }
}
