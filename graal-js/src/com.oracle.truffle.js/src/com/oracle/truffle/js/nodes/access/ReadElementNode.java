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

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage;
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
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractConstantArray;
import com.oracle.truffle.js.runtime.array.dyn.AbstractWritableArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesIntArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.LazyArray;
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
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

public class ReadElementNode extends JSTargetableNode implements ReadNode {
    @Child private JavaScriptNode targetNode;
    @Child private JavaScriptNode indexNode;
    @Child private ReadElementTypeCacheNode typeCacheNode;
    protected final JSContext context;

    @CompilationFinal private byte indexState;
    private static final byte INDEX_INT = 1;
    private static final byte INDEX_OBJECT = 2;

    public static ReadElementNode create(JSContext context) {
        return new ReadElementNode(null, null, context);
    }

    public static ReadElementNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JSContext context) {
        return new ReadElementNode(targetNode, indexNode, context);
    }

    protected ReadElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JSContext context) {
        this.targetNode = targetNode;
        this.indexNode = indexNode;
        this.context = context;
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadElementTag.class) && materializationNeeded()) {
            JavaScriptNode clonedTarget = targetNode == null || targetNode.hasSourceSection() ? targetNode : JSTaggedExecutionNode.createForInput(targetNode, this, materializedTags);
            JavaScriptNode clonedIndex = indexNode == null || indexNode.hasSourceSection() ? indexNode : JSTaggedExecutionNode.createForInput(indexNode, this, materializedTags);
            if (clonedTarget == targetNode && clonedIndex == indexNode) {
                return this;
            }
            if (clonedTarget == targetNode) {
                clonedTarget = cloneUninitialized(targetNode, materializedTags);
            }
            if (clonedIndex == indexNode) {
                clonedIndex = cloneUninitialized(indexNode, materializedTags);
            }
            JavaScriptNode cloned = ReadElementNode.create(clonedTarget, clonedIndex, getContext());
            transferSourceSectionAndTags(this, cloned);
            return cloned;
        }
        return this;
    }

    private boolean materializationNeeded() {
        // Materialization is needed when source sections are missing.
        return (targetNode != null && !targetNode.hasSourceSection()) || (indexNode != null && !indexNode.hasSourceSection());
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ReadElementTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
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
            Object index = getIndexNode().execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndex(target, (int) index, receiver);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(target, index, receiver);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = getIndexNode().executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(target, e.getResult(), receiver);
            }
            return executeWithTargetAndIndex(target, index);
        } else {
            assert is == INDEX_OBJECT;
            Object index = getIndexNode().execute(frame);
            return executeWithTargetAndIndex(target, index, receiver);
        }
    }

    public int executeWithTargetInt(VirtualFrame frame, Object target, Object receiver) throws UnexpectedResultException {
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = getIndexNode().execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexInt(target, (int) index, receiver);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(target, index, receiver);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = getIndexNode().executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(target, e.getResult(), receiver);
            }
            return executeWithTargetAndIndexInt(target, index, receiver);
        } else {
            assert is == INDEX_OBJECT;
            Object index = getIndexNode().execute(frame);
            return executeWithTargetAndIndexInt(target, index, receiver);
        }
    }

    public double executeWithTargetDouble(VirtualFrame frame, Object target, Object receiver) throws UnexpectedResultException {
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = getIndexNode().execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexDouble(target, (int) index, receiver);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(target, index, receiver);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = getIndexNode().executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(target, e.getResult(), receiver);
            }
            return executeWithTargetAndIndexDouble(target, index, receiver);
        } else {
            assert is == INDEX_OBJECT;
            Object index = getIndexNode().execute(frame);
            return executeWithTargetAndIndexDouble(target, index, receiver);
        }
    }

    public final Object executeWithTargetAndIndex(Object target, Object index) {
        return executeTypeDispatch(target, index, target, Undefined.instance);
    }

    public final Object executeWithTargetAndIndex(Object target, int index) {
        return executeTypeDispatch(target, index, target, Undefined.instance);
    }

    public final Object executeWithTargetAndIndex(Object target, long index) {
        return executeTypeDispatch(target, index, target, Undefined.instance);
    }

    public final Object executeWithTargetAndIndex(Object target, Object index, Object receiver) {
        return executeTypeDispatch(target, index, receiver, Undefined.instance);
    }

    public final Object executeWithTargetAndIndex(Object target, int index, Object receiver) {
        return executeTypeDispatch(target, index, receiver, Undefined.instance);
    }

    public final int executeWithTargetAndIndexInt(Object target, Object index, Object receiver) throws UnexpectedResultException {
        return executeTypeDispatchInt(target, index, receiver, Undefined.instance);
    }

    public final int executeWithTargetAndIndexInt(Object target, int index, Object receiver) throws UnexpectedResultException {
        return executeTypeDispatchInt(target, index, receiver, Undefined.instance);
    }

    public final double executeWithTargetAndIndexDouble(Object target, Object index, Object receiver) throws UnexpectedResultException {
        return executeTypeDispatchDouble(target, index, receiver, Undefined.instance);
    }

    public final double executeWithTargetAndIndexDouble(Object target, int index, Object receiver) throws UnexpectedResultException {
        return executeTypeDispatchDouble(target, index, receiver, Undefined.instance);
    }

    public final Object executeWithTargetAndIndexOrDefault(Object target, Object index, Object defaultValue) {
        return executeTypeDispatch(target, index, target, defaultValue);
    }

    @ExplodeLoop
    protected final Object executeTypeDispatch(Object target, Object index, Object receiver, Object defaultValue) {
        for (ReadElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                return c.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ReadElementTypeCacheNode specialization = specialize(target);
        return specialization.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
    }

    @ExplodeLoop
    protected final Object executeTypeDispatch(Object target, int index, Object receiver, Object defaultValue) {
        for (ReadElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                return c.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ReadElementTypeCacheNode specialization = specialize(target);
        return specialization.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
    }

    @ExplodeLoop
    protected final Object executeTypeDispatch(Object target, long index, Object receiver, Object defaultValue) {
        for (ReadElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                return c.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ReadElementTypeCacheNode specialization = specialize(target);
        return specialization.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
    }

    @ExplodeLoop
    protected final int executeTypeDispatchInt(Object target, Object index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        for (ReadElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                return c.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, this);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ReadElementTypeCacheNode specialization = specialize(target);
        return specialization.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, this);
    }

    @ExplodeLoop
    protected final int executeTypeDispatchInt(Object target, int index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        for (ReadElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                return c.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, this);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ReadElementTypeCacheNode specialization = specialize(target);
        return specialization.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, this);
    }

    @ExplodeLoop
    protected final double executeTypeDispatchDouble(Object target, Object index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        for (ReadElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                return c.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, this);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ReadElementTypeCacheNode specialization = specialize(target);
        return specialization.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, this);
    }

    @ExplodeLoop
    protected final double executeTypeDispatchDouble(Object target, int index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        for (ReadElementTypeCacheNode c = typeCacheNode; c != null; c = c.typeCacheNext) {
            boolean guard = c.guard(target);
            if (guard) {
                return c.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, this);
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        ReadElementTypeCacheNode specialization = specialize(target);
        return specialization.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, this);
    }

    private ReadElementTypeCacheNode specialize(Object target) {
        CompilerAsserts.neverPartOfCompilation();
        Lock lock = getLock();
        lock.lock();
        try {
            ReadElementTypeCacheNode currentHead = typeCacheNode;
            for (ReadElementTypeCacheNode c = currentHead; c != null; c = c.typeCacheNext) {
                if (c.guard(target)) {
                    return c;
                }
            }

            ReadElementTypeCacheNode newCacheNode = makeTypeCacheNode(target, currentHead);
            insert(newCacheNode);
            typeCacheNode = newCacheNode;
            if (currentHead != null && currentHead.typeCacheNext != null && currentHead.typeCacheNext.typeCacheNext != null) {
                reportPolymorphicSpecialize();
            }
            if (!newCacheNode.guard(target)) {
                throw Errors.shouldNotReachHere();
            }
            return newCacheNode;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private static ReadElementTypeCacheNode makeTypeCacheNode(Object target, ReadElementTypeCacheNode next) {
        if (JSDynamicObject.isJSDynamicObject(target)) {
            return new JSObjectReadElementTypeCacheNode(next);
        } else if (target instanceof JSLazyString) {
            return new LazyStringReadElementTypeCacheNode(next);
        } else if (JSRuntime.isString(target)) {
            return new StringReadElementTypeCacheNode(target.getClass(), next);
        } else if (target instanceof Boolean) {
            return new BooleanReadElementTypeCacheNode(next);
        } else if (target instanceof Number) {
            return new NumberReadElementTypeCacheNode(target.getClass(), next);
        } else if (target instanceof Symbol) {
            return new SymbolReadElementTypeCacheNode(next);
        } else if (target instanceof BigInt) {
            return new BigIntReadElementTypeCacheNode(next);
        } else if (target instanceof TruffleObject) {
            assert JSRuntime.isForeignObject(target);
            return new ForeignObjectReadElementTypeCacheNode(target.getClass(), next);
        } else {
            assert JSRuntime.isJavaPrimitive(target);
            return new JavaObjectReadElementTypeCacheNode(target.getClass(), next);
        }
    }

    abstract static class ReadElementTypeCacheNode extends JavaScriptBaseNode {
        @Child private ReadElementTypeCacheNode typeCacheNext;

        protected ReadElementTypeCacheNode(ReadElementTypeCacheNode next) {
            this.typeCacheNext = next;
        }

        public abstract boolean guard(Object target);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root);

        protected int executeWithTargetAndIndexUncheckedInt(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, root));
        }

        protected int executeWithTargetAndIndexUncheckedInt(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            return executeWithTargetAndIndexUncheckedInt(target, (Object) index, receiver, defaultValue, root);
        }

        protected double executeWithTargetAndIndexUncheckedDouble(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, root));
        }

        protected double executeWithTargetAndIndexUncheckedDouble(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            return executeWithTargetAndIndexUncheckedDouble(target, (Object) index, receiver, defaultValue, root);
        }
    }

    protected abstract static class ReadElementArrayDispatchNode extends ReadElementTypeCacheNode {
        @Child private ArrayReadElementCacheNode arrayReadElementNode;

        protected ReadElementArrayDispatchNode(ReadElementTypeCacheNode next) {
            super(next);
        }

        @ExplodeLoop
        protected final Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext root) {
            for (ArrayReadElementCacheNode c = arrayReadElementNode; c != null; c = c.arrayCacheNext) {
                boolean guard = c.guard(target, array);
                if (guard) {
                    return c.executeArrayGet(target, array, index, receiver, defaultValue, root);
                }
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayReadElementCacheNode specialization = specialize(target, array);
            return specialization.executeArrayGet(target, array, index, receiver, defaultValue, root);
        }

        @ExplodeLoop
        protected final int executeArrayGetInt(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext root)
                        throws UnexpectedResultException {
            for (ArrayReadElementCacheNode c = arrayReadElementNode; c != null; c = c.arrayCacheNext) {
                boolean guard = c.guard(target, array);
                if (guard) {
                    return c.executeArrayGetInt(target, array, index, receiver, defaultValue, root);
                }
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayReadElementCacheNode specialization = specialize(target, array);
            return specialization.executeArrayGetInt(target, array, index, receiver, defaultValue, root);
        }

        @ExplodeLoop
        protected final double executeArrayGetDouble(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext root)
                        throws UnexpectedResultException {
            for (ArrayReadElementCacheNode c = arrayReadElementNode; c != null; c = c.arrayCacheNext) {
                boolean guard = c.guard(target, array);
                if (guard) {
                    return c.executeArrayGetDouble(target, array, index, receiver, defaultValue, root);
                }
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayReadElementCacheNode specialization = specialize(target, array);
            return specialization.executeArrayGetDouble(target, array, index, receiver, defaultValue, root);
        }

        private ArrayReadElementCacheNode specialize(DynamicObject target, ScriptArray array) {
            CompilerAsserts.neverPartOfCompilation();
            Lock lock = getLock();
            lock.lock();
            try {
                ArrayReadElementCacheNode currentHead = arrayReadElementNode;
                for (ArrayReadElementCacheNode c = currentHead; c != null; c = c.arrayCacheNext) {
                    if (c.guard(target, array)) {
                        return c;
                    }
                }

                currentHead = purgeStaleCacheEntries(currentHead, target);

                ArrayReadElementCacheNode newCacheNode = makeArrayCacheNode(target, array, currentHead);
                insert(newCacheNode);
                arrayReadElementNode = newCacheNode;
                if (!newCacheNode.guard(target, array)) {
                    throw Errors.shouldNotReachHere();
                }
                return newCacheNode;
            } finally {
                lock.unlock();
            }
        }

        private static ArrayReadElementCacheNode purgeStaleCacheEntries(ArrayReadElementCacheNode head, DynamicObject target) {
            if (JSConfig.TrackArrayAllocationSites && head != null && JSArray.isJSArray(target)) {
                ArrayAllocationSite allocationSite = JSAbstractArray.arrayGetAllocationSite(target);
                if (allocationSite != null && allocationSite.getInitialArrayType() != null) {
                    for (ArrayReadElementCacheNode c = head, prev = null; c != null; prev = c, c = c.arrayCacheNext) {
                        if (c instanceof ConstantArrayReadElementCacheNode) {
                            ConstantArrayReadElementCacheNode existingNode = (ConstantArrayReadElementCacheNode) c;
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

        protected static ReadElementArrayDispatchNode create() {
            return new ReadElementArrayDispatchNode(null) {
                @Override
                public boolean guard(Object target) {
                    return true;
                }

                @Override
                protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
                    throw Errors.shouldNotReachHere();
                }

                @Override
                protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) {
                    throw Errors.shouldNotReachHere();
                }

                @Override
                protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
                    throw Errors.shouldNotReachHere();
                }
            };
        }
    }

    private static class JSObjectReadElementTypeCacheNode extends ReadElementArrayDispatchNode {
        @Child private IsArrayNode isArrayNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        @Child private JSObjectReadElementNonArrayTypeCacheNode nonArrayCaseNode;
        @Child private IsJSObjectNode isObjectNode;
        private final ConditionProfile arrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayIndexProfile = ConditionProfile.createBinaryProfile();
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        JSObjectReadElementTypeCacheNode(ReadElementTypeCacheNode next) {
            super(next);
            this.isArrayNode = IsArrayNode.createIsAnyArray();
            this.isObjectNode = IsJSObjectNode.createIncludeNullUndefined();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    return executeArrayGet(targetObject, array, longIndex, receiver, defaultValue, root.context);
                } else {
                    return getProperty(targetObject, objIndex, receiver, defaultValue);
                }
            } else {
                return readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root);
            }
        }

        private Object toArrayIndex(Object index) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(index);
        }

        private Object readNonArrayObjectIndex(DynamicObject targetObject, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            return getNonArrayNode().execute(targetObject, index, receiver, defaultValue, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject);
                if (arrayIndexProfile.profile(JSRuntime.isArrayIndex(index))) {
                    return executeArrayGet(targetObject, array, index, receiver, defaultValue, root.context);
                } else {
                    return getProperty(targetObject, Boundaries.stringValueOf(index), receiver, defaultValue);
                }
            } else {
                return readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root);
            }
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject);
                if (arrayIndexProfile.profile(JSRuntime.isArrayIndex(index))) {
                    return executeArrayGet(targetObject, array, index, receiver, defaultValue, root.context);
                } else {
                    return getProperty(targetObject, Boundaries.stringValueOf(index), receiver, defaultValue);
                }
            } else {
                return readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root);
            }
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    return executeArrayGetInt(targetObject, array, longIndex, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, objIndex, receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectInteger(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject);

                if (arrayIndexProfile.profile(JSRuntime.isArrayIndex(index))) {
                    return executeArrayGetInt(targetObject, array, index, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, Boundaries.stringValueOf(index), receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectInteger(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Override
        protected double executeWithTargetAndIndexUncheckedDouble(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    return executeArrayGetDouble(targetObject, array, longIndex, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, objIndex, receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectDouble(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Override
        protected double executeWithTargetAndIndexUncheckedDouble(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject);

                if (arrayIndexProfile.profile(JSRuntime.isArrayIndex(index))) {
                    return executeArrayGetDouble(targetObject, array, index, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, Boundaries.stringValueOf(index), receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectDouble(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Override
        public boolean guard(Object target) {
            return isObjectNode.executeBoolean(target);
        }

        private Object getProperty(DynamicObject targetObject, Object objIndex, Object receiver, Object defaultValue) {
            return JSObject.getOrDefault(targetObject, objIndex, receiver, defaultValue, jsclassProfile, this);
        }

        private JSObjectReadElementNonArrayTypeCacheNode getNonArrayNode() {
            if (nonArrayCaseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nonArrayCaseNode = insert(new JSObjectReadElementNonArrayTypeCacheNode());
            }
            return nonArrayCaseNode;
        }
    }

    private static class JSObjectReadElementNonArrayTypeCacheNode extends JavaScriptBaseNode {

        @Child private CachedGetPropertyNode getPropertyCachedNode;

        JSObjectReadElementNonArrayTypeCacheNode() {
        }

        public Object execute(DynamicObject targetObject, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            if (getPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getPropertyCachedNode = insert(CachedGetPropertyNode.create(root.context));
            }
            return getPropertyCachedNode.execute(targetObject, index, receiver, defaultValue);
        }
    }

    private static class JavaObjectReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        protected final Class<?> targetClass;

        JavaObjectReadElementTypeCacheNode(Class<?> targetClass, ReadElementTypeCacheNode next) {
            super(next);
            this.targetClass = targetClass;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            toPropertyKey(index);
            return Undefined.instance;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            return Undefined.instance;
        }

        @Override
        public final boolean guard(Object target) {
            return targetClass.isInstance(target);
        }
    }

    protected static ArrayReadElementCacheNode makeArrayCacheNode(@SuppressWarnings("unused") DynamicObject target, ScriptArray array, ArrayReadElementCacheNode next) {
        if (array instanceof ConstantEmptyArray) {
            return new EmptyArrayReadElementCacheNode(array, next);
        } else if (array instanceof ConstantObjectArray) {
            return new ConstantObjectArrayReadElementCacheNode(array, next);
        } else if (array instanceof LazyRegexResultArray) {
            return new LazyRegexResultArrayReadElementCacheNode(array, next);
        } else if (array instanceof LazyRegexResultIndicesArray) {
            return new LazyRegexResultIndicesArrayReadElementCacheNode(array, next);
        } else if (array instanceof LazyArray) {
            return new LazyArrayReadElementCacheNode(array, next);
        } else if (array instanceof AbstractConstantArray) {
            return new ConstantArrayReadElementCacheNode(array, next);
        } else if (array instanceof HolesIntArray) {
            return new HolesIntArrayReadElementCacheNode(array, next);
        } else if (array instanceof HolesDoubleArray) {
            return new HolesDoubleArrayReadElementCacheNode(array, next);
        } else if (array instanceof HolesJSObjectArray) {
            return new HolesJSObjectArrayReadElementCacheNode(array, next);
        } else if (array instanceof HolesObjectArray) {
            return new HolesObjectArrayReadElementCacheNode(array, next);
        } else if (array instanceof AbstractWritableArray) {
            return new WritableArrayReadElementCacheNode(array, next);
        } else if (array instanceof TypedArray) {
            if (array instanceof TypedArray.AbstractUint32Array) {
                return new Uint32ArrayReadElementCacheNode((TypedArray) array, next);
            } else if (array instanceof TypedArray.TypedIntArray) {
                return new TypedIntArrayReadElementCacheNode((TypedArray) array, next);
            } else if (array instanceof TypedArray.TypedFloatArray) {
                return new TypedFloatArrayReadElementCacheNode((TypedArray) array, next);
            } else if (array instanceof TypedArray.TypedBigIntArray) {
                return new TypedBigIntArrayReadElementCacheNode((TypedArray) array, next);
            } else {
                throw Errors.shouldNotReachHere();
            }
        } else {
            return new ExactArrayReadElementCacheNode(array, next);
        }
    }

    abstract static class ArrayReadElementCacheNode extends JavaScriptBaseNode {
        @Child ArrayReadElementCacheNode arrayCacheNext;

        protected ArrayReadElementCacheNode(ArrayReadElementCacheNode next) {
            this.arrayCacheNext = next;
        }

        protected abstract Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context);

        protected int executeArrayGetInt(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeArrayGet(target, array, index, receiver, defaultValue, context));
        }

        protected double executeArrayGetDouble(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeArrayGet(target, array, index, receiver, defaultValue, context));
        }

        protected abstract boolean guard(Object target, ScriptArray array);
    }

    private abstract static class ArrayClassGuardCachedArrayReadElementCacheNode extends ArrayReadElementCacheNode {
        private final ScriptArray arrayType;
        protected final ConditionProfile inBounds = ConditionProfile.createBinaryProfile();
        private final ConditionProfile needGetProperty = ConditionProfile.createBinaryProfile();
        private final JSClassProfile outOfBoundsClassProfile = JSClassProfile.create();

        ArrayClassGuardCachedArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(next);
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

        protected Object readOutOfBounds(DynamicObject target, long index, Object receiver, Object defaultValue, JSContext context) {
            if (needGetProperty.profile(needsSlowGet(target, context))) {
                return JSObject.getOrDefault(target, index, receiver, defaultValue, outOfBoundsClassProfile, this);
            } else {
                return defaultValue;
            }
        }

        private static boolean needsSlowGet(DynamicObject target, JSContext context) {
            return !context.getArrayPrototypeNoElementsAssumption().isValid() || (!context.getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(target)) ||
                            (!context.getFastArgumentsObjectAssumption().isValid() && JSSlowArgumentsArray.isJSSlowArgumentsObject(target));
        }
    }

    private static class ExactArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final JSClassProfile classProfile = JSClassProfile.create();

        ExactArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            return JSObject.getOrDefault(target, index, receiver, defaultValue, classProfile, this);
        }
    }

    private static class ConstantArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        ConstantArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            AbstractConstantArray constantArray = (AbstractConstantArray) cast(array);
            if (inBounds.profile(constantArray.hasElement(target, index))) {
                return constantArray.getElementInBounds(target, (int) index);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context);
            }
        }
    }

    private static class EmptyArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        EmptyArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
            assert arrayType.getClass() == ConstantEmptyArray.class;
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            return readOutOfBounds(target, index, receiver, defaultValue, context);
        }
    }

    private static class ConstantObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeArrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        ConstantObjectArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            ConstantObjectArray constantObjectArray = (ConstantObjectArray) cast(array);
            if (inBounds.profile(constantObjectArray.isInBoundsFast(target, index))) {
                Object value = ConstantObjectArray.getElementInBoundsDirect(target, (int) index);
                if (holeArrayProfile.profile(!constantObjectArray.hasHoles(target))) {
                    return value;
                } else {
                    if (holeProfile.profile(!HolesObjectArray.isHoleValue(value))) {
                        return value;
                    }
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context);
        }
    }

    private static class LazyRegexResultArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        @Child private TRegexUtil.TRegexMaterializeResultNode materializeResultNode;
        @Child private DynamicObjectLibrary lazyRegexResultNode;
        @Child private DynamicObjectLibrary lazyRegexResultOriginalInputNode;

        LazyRegexResultArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        private TRegexUtil.TRegexMaterializeResultNode getMaterializeResultNode() {
            if (materializeResultNode == null || lazyRegexResultNode == null || lazyRegexResultOriginalInputNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                materializeResultNode = insert(TRegexUtil.TRegexMaterializeResultNode.create());
                lazyRegexResultNode = insert(DynamicObjectLibrary.getFactory().createDispatched(JSConfig.PropertyCacheLimit));
                lazyRegexResultOriginalInputNode = insert(DynamicObjectLibrary.getFactory().createDispatched(JSConfig.PropertyCacheLimit));
            }
            return materializeResultNode;
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            LazyRegexResultArray lazyRegexResultArray = (LazyRegexResultArray) array;
            if (inBounds.profile(lazyRegexResultArray.hasElement(target, (int) index))) {
                return LazyRegexResultArray.materializeGroup(getMaterializeResultNode(), target, (int) index, lazyRegexResultNode, lazyRegexResultOriginalInputNode);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context);
            }
        }
    }

    private static class LazyRegexResultIndicesArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        @Child TRegexUtil.TRegexResultAccessor resultAccessor;

        LazyRegexResultIndicesArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        private TRegexUtil.TRegexResultAccessor getResultAccessor() {
            if (resultAccessor == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                resultAccessor = insert(TRegexUtil.TRegexResultAccessor.create());
            }
            return resultAccessor;
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            LazyRegexResultIndicesArray lazyRegexResultIndicesArray = (LazyRegexResultIndicesArray) array;
            if (inBounds.profile(lazyRegexResultIndicesArray.hasElement(target, (int) index))) {
                return LazyRegexResultIndicesArray.materializeGroup(context, getResultAccessor(), target, (int) index);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context);
            }
        }
    }

    private static class LazyArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        @Child private ListGetNode listGetNode;

        LazyArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
            this.listGetNode = ListGetNode.create();
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            LazyArray lazyRegexResultArray = (LazyArray) array;
            int intIndex = (int) index;
            if (inBounds.profile(lazyRegexResultArray.hasElement(target, intIndex))) {
                return lazyRegexResultArray.getElementInBounds(target, intIndex, listGetNode);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context);
            }
        }
    }

    private static class WritableArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        WritableArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index))) {
                return writableArray.getInBoundsFast(target, (int) index);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context);
            }
        }

        @Override
        protected int executeArrayGetInt(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index))) {
                return writableArray.getInBoundsFastInt(target, (int) index);
            } else {
                return JSTypesGen.expectInteger(readOutOfBounds(target, index, receiver, defaultValue, context));
            }
        }

        @Override
        protected double executeArrayGetDouble(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index))) {
                return writableArray.getInBoundsFastDouble(target, (int) index);
            } else {
                return JSTypesGen.expectDouble(readOutOfBounds(target, index, receiver, defaultValue, context));
            }
        }
    }

    private static class HolesIntArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesIntArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            HolesIntArray holesIntArray = (HolesIntArray) cast(array);
            if (inBounds.profile(holesIntArray.isInBoundsFast(target, index))) {
                int value = holesIntArray.getInBoundsFastInt(target, (int) index);
                if (holeProfile.profile(!HolesIntArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context);
        }
    }

    private static class HolesDoubleArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesDoubleArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            HolesDoubleArray holesDoubleArray = (HolesDoubleArray) cast(array);
            if (inBounds.profile(holesDoubleArray.isInBoundsFast(target, index))) {
                double value = holesDoubleArray.getInBoundsFastDouble(target, (int) index);
                if (holeProfile.profile(!HolesDoubleArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context);
        }
    }

    private static class HolesJSObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesJSObjectArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            HolesJSObjectArray holesArray = (HolesJSObjectArray) cast(array);
            if (inBounds.profile(holesArray.isInBoundsFast(target, index))) {
                DynamicObject value = holesArray.getInBoundsFastJSObject(target, (int) index);
                if (holeProfile.profile(!HolesJSObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context);
        }
    }

    private static class HolesObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesObjectArrayReadElementCacheNode(ScriptArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            HolesObjectArray holesArray = (HolesObjectArray) cast(array);
            if (inBounds.profile(holesArray.isInBoundsFast(target, index))) {
                Object value = holesArray.getInBoundsFastObject(target, (int) index);
                if (holeProfile.profile(!HolesObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context);
        }
    }

    private abstract static class AbstractTypedArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        @Child protected InteropLibrary interop;

        AbstractTypedArrayReadElementCacheNode(TypedArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
            this.interop = arrayType.isInterop() ? InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit) : InteropLibrary.getUncached();
        }

        protected void checkDetachedArrayBuffer(DynamicObject target, JSContext context) {
            if (JSArrayBufferView.hasDetachedBuffer(target, context)) {
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }
    }

    private static class TypedIntArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {

        TypedIntArrayReadElementCacheNode(TypedArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop);
            } else {
                return defaultValue;
            }
        }

        @Override
        protected int executeArrayGetInt(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop);
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Override
        protected double executeArrayGetDouble(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop);
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }
    }

    private static class Uint32ArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {
        private final ConditionProfile isSignedProfile = ConditionProfile.createBinaryProfile();

        Uint32ArrayReadElementCacheNode(TypedArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                int intValue = typedArray.getInt(target, (int) index, interop);
                if (isSignedProfile.profile(intValue >= 0)) {
                    return intValue;
                } else {
                    return (double) (intValue & 0xffff_ffffL);
                }
            } else {
                return defaultValue;
            }
        }

        @Override
        protected int executeArrayGetInt(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                int intValue = typedArray.getInt(target, (int) index, interop);
                if (isSignedProfile.profile(intValue >= 0)) {
                    return intValue;
                } else {
                    throw new UnexpectedResultException((double) (intValue & 0xffff_ffffL));
                }
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Override
        protected double executeArrayGetDouble(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop) & 0xffff_ffffL;
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }
    }

    private static class TypedFloatArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {

        TypedFloatArrayReadElementCacheNode(TypedArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedFloatArray typedArray = (TypedArray.TypedFloatArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                return typedArray.getDouble(target, (int) index, interop);
            } else {
                return defaultValue;
            }
        }

        @Override
        protected double executeArrayGetDouble(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedFloatArray typedArray = (TypedArray.TypedFloatArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                return typedArray.getDouble(target, (int) index, interop);
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }
    }

    private static class TypedBigIntArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {

        TypedBigIntArrayReadElementCacheNode(TypedArray arrayType, ArrayReadElementCacheNode next) {
            super(arrayType, next);
        }

        @Override
        protected Object executeArrayGet(DynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            checkDetachedArrayBuffer(target, context);
            TypedArray.TypedBigIntArray typedArray = (TypedArray.TypedBigIntArray) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index))) {
                return typedArray.getBigInt(target, (int) index, interop);
            } else {
                return defaultValue;
            }
        }
    }

    private abstract static class ToPropertyKeyCachedReadElementTypeCacheNode extends ReadElementTypeCacheNode {
        @Child private JSToPropertyKeyNode indexToPropertyKeyNode;
        protected final JSClassProfile jsclassProfile = JSClassProfile.create();

        ToPropertyKeyCachedReadElementTypeCacheNode(ReadElementTypeCacheNode next) {
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
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) {
            return executeWithTargetAndIndexUnchecked(target, (long) index, receiver, defaultValue, root);
        }
    }

    private static class StringReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> stringClass;
        private final ConditionProfile arrayIndexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile stringIndexInBounds = ConditionProfile.createBinaryProfile();
        @Child private ToArrayIndexNode toArrayIndexNode;

        StringReadElementTypeCacheNode(Class<?> stringClass, ReadElementTypeCacheNode next) {
            super(next);
            this.stringClass = stringClass;
            this.toArrayIndexNode = ToArrayIndexNode.createNoToPropertyKey();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (arrayIndexProfile.profile(convertedIndex instanceof Long)) {
                int intIndex = ((Long) convertedIndex).intValue();
                if (stringIndexInBounds.profile(intIndex >= 0 && intIndex < JSRuntime.length(charSequence))) {
                    // charAt needs boundary, SVM thinks it could be any type
                    return String.valueOf(JSRuntime.charAt(charSequence, intIndex));
                }
            }
            return JSObject.getOrDefault(JSString.create(root.context, charSequence), toPropertyKey(index), receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            if (stringIndexInBounds.profile(index >= 0 && index < JSRuntime.length(charSequence))) {
                return String.valueOf(JSRuntime.charAt(charSequence, index));
            } else {
                return JSObject.getOrDefault(JSString.create(root.context, charSequence), index, receiver, defaultValue, jsclassProfile, root);
            }
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            if (stringIndexInBounds.profile(index >= 0 && index < JSRuntime.length(charSequence))) {
                return String.valueOf(JSRuntime.charAt(charSequence, (int) index));
            } else {
                return JSObject.getOrDefault(JSString.create(root.context, charSequence), index, receiver, defaultValue, jsclassProfile, root);
            }
        }

        @Override
        public boolean guard(Object target) {
            return stringClass.isInstance(target);
        }
    }

    private static class LazyStringReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final ConditionProfile arrayIndexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile stringIndexInBounds = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isFlatProfile = ConditionProfile.createBinaryProfile();
        @Child private ToArrayIndexNode toArrayIndexNode;

        LazyStringReadElementTypeCacheNode(ReadElementTypeCacheNode next) {
            super(next);
            this.toArrayIndexNode = ToArrayIndexNode.createNoToPropertyKey();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            String str = ((JSLazyString) target).toString(isFlatProfile);
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (arrayIndexProfile.profile(convertedIndex instanceof Long)) {
                int intIndex = ((Long) convertedIndex).intValue();
                if (stringIndexInBounds.profile(intIndex >= 0 && intIndex < str.length())) {
                    return String.valueOf(str.charAt(intIndex));
                }
            }
            return JSObject.getOrDefault(JSString.create(root.context, str), toPropertyKey(index), receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) {
            String str = ((JSLazyString) target).toString(isFlatProfile);
            if (stringIndexInBounds.profile(index >= 0 && index < str.length())) {
                return String.valueOf(str.charAt(index));
            } else {
                return JSObject.getOrDefault(JSString.create(root.context, str), index, receiver, defaultValue, jsclassProfile, root);
            }
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            String str = ((JSLazyString) target).toString(isFlatProfile);
            if (stringIndexInBounds.profile(index >= 0 && index < str.length())) {
                return String.valueOf(str.charAt((int) index));
            } else {
                return JSObject.getOrDefault(JSString.create(root.context, str), index, receiver, defaultValue, jsclassProfile, root);
            }
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof JSLazyString;
        }
    }

    private static class NumberReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> numberClass;

        NumberReadElementTypeCacheNode(Class<?> stringClass, ReadElementTypeCacheNode next) {
            super(next);
            this.numberClass = stringClass;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            Number charSequence = (Number) target;
            return JSObject.getOrDefault(JSNumber.create(root.context, charSequence), toPropertyKey(index), receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            Number charSequence = (Number) target;
            return JSObject.getOrDefault(JSNumber.create(root.context, charSequence), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return numberClass.isInstance(target);
        }
    }

    private static class BooleanReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        BooleanReadElementTypeCacheNode(ReadElementTypeCacheNode next) {
            super(next);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            Boolean bool = (Boolean) target;
            return JSObject.getOrDefault(JSBoolean.create(root.context, bool), toPropertyKey(index), receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            Boolean bool = (Boolean) target;
            return JSObject.getOrDefault(JSBoolean.create(root.context, bool), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Boolean;
        }
    }

    private static class SymbolReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {

        SymbolReadElementTypeCacheNode(ReadElementTypeCacheNode next) {
            super(next);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            Symbol symbol = (Symbol) target;
            return JSObject.getOrDefault(JSSymbol.create(root.context, symbol), toPropertyKey(index), receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            Symbol symbol = (Symbol) target;
            return JSObject.getOrDefault(JSSymbol.create(root.context, symbol), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Symbol;
        }
    }

    private static class BigIntReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {

        BigIntReadElementTypeCacheNode(ReadElementTypeCacheNode next) {
            super(next);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            BigInt bigInt = (BigInt) target;
            return JSObject.getOrDefault(JSBigInt.create(root.context, bigInt), toPropertyKey(index), receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            BigInt bigInt = (BigInt) target;
            return JSObject.getOrDefault(JSBigInt.create(root.context, bigInt), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof BigInt;
        }
    }

    static class ForeignObjectReadElementTypeCacheNode extends ReadElementTypeCacheNode {
        private final Class<?> targetClass;

        @Child private InteropLibrary interop;
        @Child private JSToPropertyKeyNode toPropertyKey;
        @Child private ImportValueNode importValueNode;
        @Child private InteropLibrary getterInterop;
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;
        @Child private ReadElementNode readFromPrototypeNode;
        @Child private ToArrayIndexNode toArrayIndexNode;

        private final BranchProfile errorBranch = BranchProfile.create();
        @CompilationFinal private boolean optimistic = true;

        ForeignObjectReadElementTypeCacheNode(Class<?> targetClass, ReadElementTypeCacheNode next) {
            super(next);
            this.targetClass = targetClass;
            this.importValueNode = ImportValueNode.create();
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
            this.toPropertyKey = JSToPropertyKeyNode.create();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            Object truffleObject = targetClass.cast(target);
            if (interop.isNull(truffleObject)) {
                errorBranch.enter();
                throw Errors.createTypeErrorCannotGetProperty(root.getContext(), JSRuntime.safeToString(index), target, false, this);
            }
            Object foreignResult = getImpl(truffleObject, index, root);
            return importValueNode.executeWithTarget(foreignResult);
        }

        private Object getImpl(Object truffleObject, Object key, ReadElementNode root) {
            Object propertyKey;
            boolean hasArrayElements = interop.hasArrayElements(truffleObject);
            if (hasArrayElements) {
                try {
                    Object indexOrPropertyKey = toArrayIndex(key);
                    if (indexOrPropertyKey instanceof Long) {
                        return interop.readArrayElement(truffleObject, (long) indexOrPropertyKey);
                    } else {
                        propertyKey = indexOrPropertyKey;
                        assert JSRuntime.isPropertyKey(propertyKey);
                    }
                } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
                    return Undefined.instance;
                }
            } else {
                propertyKey = toPropertyKey.execute(key);
            }
            if (root.context.getContextOptions().hasForeignHashProperties() && interop.hasHashEntries(truffleObject)) {
                try {
                    return interop.readHashValue(truffleObject, propertyKey);
                } catch (UnknownKeyException e) {
                    // fall through: still need to try members
                } catch (UnsupportedMessageException e) {
                    return Undefined.instance;
                }
            }
            if (propertyKey instanceof Symbol) {
                return maybeReadFromPrototype(truffleObject, propertyKey, root.context);
            }
            String stringKey = (String) propertyKey;
            if (hasArrayElements && JSAbstractArray.LENGTH.equals(stringKey)) {
                return getSize(truffleObject);
            }
            if (root.context.isOptionNashornCompatibilityMode()) {
                Object result = tryGetters(truffleObject, stringKey, root.context);
                if (result != null) {
                    return result;
                }
            }
            if (optimistic) {
                try {
                    return interop.readMember(truffleObject, stringKey);
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    optimistic = false;
                    return maybeReadFromPrototype(truffleObject, stringKey, root.context);
                }
            } else {
                if (interop.isMemberReadable(truffleObject, stringKey)) {
                    try {
                        return interop.readMember(truffleObject, stringKey);
                    } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                        return Undefined.instance;
                    }
                } else {
                    return maybeReadFromPrototype(truffleObject, stringKey, root.context);
                }
            }
        }

        private Object tryGetters(Object thisObj, String key, JSContext context) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = context.getRealm().getEnv();
            if (env.isHostObject(thisObj)) {
                Object result = tryInvokeGetter(thisObj, "get", key);
                if (result != null) {
                    return result;
                }
                result = tryInvokeGetter(thisObj, "is", key);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        private Object tryInvokeGetter(Object thisObj, String prefix, String key) {
            String getterKey = PropertyCacheNode.getAccessorKey(prefix, key);
            if (getterKey == null) {
                return null;
            }
            if (getterInterop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getterInterop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            if (!getterInterop.isMemberInvocable(thisObj, getterKey)) {
                return null;
            }
            try {
                return getterInterop.invokeMember(thisObj, getterKey, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                return null; // try the next fallback
            }
        }

        private Object getSize(Object truffleObject) {
            try {
                return JSRuntime.longToIntOrDouble(interop.getArraySize(truffleObject));
            } catch (UnsupportedMessageException e) {
                errorBranch.enter();
                throw Errors.createTypeErrorInteropException(truffleObject, e, "getArraySize", this);
            }
        }

        private Object maybeReadFromPrototype(Object truffleObject, Object key, JSContext context) {
            assert JSRuntime.isPropertyKey(key);
            if (context.getContextOptions().hasForeignObjectPrototype() || key instanceof Symbol || JSInteropUtil.isBoxedPrimitive(truffleObject, interop)) {
                if (readFromPrototypeNode == null || foreignObjectPrototypeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    this.readFromPrototypeNode = insert(ReadElementNode.create(context));
                    this.foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
                }
                DynamicObject prototype = foreignObjectPrototypeNode.executeDynamicObject(truffleObject);
                return readFromPrototypeNode.executeWithTargetAndIndex(prototype, key);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object receiver, Object defaultValue, ReadElementNode root) {
            return executeWithTargetAndIndexUnchecked(target, (Object) index, receiver, defaultValue, root);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            return executeWithTargetAndIndexUnchecked(target, (Object) index, receiver, defaultValue, root);
        }

        @Override
        public boolean guard(Object target) {
            return targetClass.isInstance(target) && !JSDynamicObject.isJSDynamicObject(target);
        }

        private Object toArrayIndex(Object maybeIndex) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(maybeIndex);
        }
    }

    @Override
    public final JavaScriptNode getTarget() {
        return targetNode;
    }

    public final JavaScriptNode getElement() {
        return getIndexNode();
    }

    public final JSContext getContext() {
        return context;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(getIndexNode(), materializedTags), getContext());
    }

    @Override
    public String expressionToString() {
        if (targetNode != null && indexNode != null) {
            return Objects.toString(targetNode.expressionToString(), INTERMEDIATE_VALUE) + "[" + Objects.toString(indexNode.expressionToString(), INTERMEDIATE_VALUE) + "]";
        }
        return null;
    }

    public JavaScriptNode getIndexNode() {
        return indexNode;
    }
}
