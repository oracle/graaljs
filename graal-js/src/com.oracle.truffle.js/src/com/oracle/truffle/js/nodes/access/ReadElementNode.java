/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.builtins.helper.ListGetNodeGen;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.ArrayReadElementCacheDispatchNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.BigIntReadElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.BooleanReadElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.ConstantArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.ConstantObjectArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.EmptyArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.ForeignObjectReadElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.HolesDoubleArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.HolesIntArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.HolesJSObjectArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.HolesObjectArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.JavaObjectReadElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.LazyArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.LazyRegexResultArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.LazyRegexResultIndicesArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.NumberReadElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.ReadElementTypeCacheDispatchNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.SymbolReadElementTypeCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.TypedBigIntArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.TypedFloatArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.TypedIntArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.Uint32ArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.access.ReadElementNodeFactory.WritableArrayReadElementCacheNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
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
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

public class ReadElementNode extends JSTargetableNode implements ReadNode {
    @Child private JavaScriptNode targetNode;
    @Child private JavaScriptNode indexNode;
    @Child private ReadElementTypeCacheDispatchNode typeCacheNode;
    protected final JSContext context;

    @CompilationFinal private byte indexState;
    private static final byte INDEX_INT = 1;
    private static final byte INDEX_OBJECT = 2;

    /** Exact cache limit unknown, but effectively bounded by the number of types. */
    static final int BOUNDED_BY_TYPES = Integer.MAX_VALUE;

    @NeverDefault
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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

    private ReadElementTypeCacheDispatchNode initTypeCacheDispatchNode() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return typeCacheNode = insert(ReadElementTypeCacheDispatchNodeGen.create());
    }

    protected final Object executeTypeDispatch(Object target, Object index, Object receiver, Object defaultValue) {
        ReadElementTypeCacheDispatchNode dispatch = typeCacheNode;
        if (dispatch == null) {
            dispatch = initTypeCacheDispatchNode();
        }
        return dispatch.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
    }

    protected final Object executeTypeDispatch(Object target, int index, Object receiver, Object defaultValue) {
        ReadElementTypeCacheDispatchNode dispatch = typeCacheNode;
        if (dispatch == null) {
            dispatch = initTypeCacheDispatchNode();
        }
        return dispatch.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
    }

    protected final Object executeTypeDispatch(Object target, long index, Object receiver, Object defaultValue) {
        ReadElementTypeCacheDispatchNode dispatch = typeCacheNode;
        if (dispatch == null) {
            dispatch = initTypeCacheDispatchNode();
        }
        return dispatch.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, this);
    }

    protected final int executeTypeDispatchInt(Object target, Object index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        ReadElementTypeCacheDispatchNode dispatch = typeCacheNode;
        if (dispatch == null) {
            dispatch = initTypeCacheDispatchNode();
        }
        return dispatch.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, this);
    }

    protected final int executeTypeDispatchInt(Object target, int index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        ReadElementTypeCacheDispatchNode dispatch = typeCacheNode;
        if (dispatch == null) {
            dispatch = initTypeCacheDispatchNode();
        }
        return dispatch.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, this);
    }

    protected final double executeTypeDispatchDouble(Object target, Object index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        ReadElementTypeCacheDispatchNode dispatch = typeCacheNode;
        if (dispatch == null) {
            dispatch = initTypeCacheDispatchNode();
        }
        return dispatch.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, this);
    }

    protected final double executeTypeDispatchDouble(Object target, int index, Object receiver, Object defaultValue) throws UnexpectedResultException {
        ReadElementTypeCacheDispatchNode dispatch = typeCacheNode;
        if (dispatch == null) {
            dispatch = initTypeCacheDispatchNode();
        }
        return dispatch.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, this);
    }

    abstract static class ReadElementTypeCacheNode extends JavaScriptBaseNode {

        protected ReadElementTypeCacheNode() {
        }

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root);

        protected abstract int executeWithTargetAndIndexUncheckedInt(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException;

        protected abstract int executeWithTargetAndIndexUncheckedInt(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException;

        protected abstract double executeWithTargetAndIndexUncheckedDouble(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException;

        protected abstract double executeWithTargetAndIndexUncheckedDouble(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) throws UnexpectedResultException;
    }

    abstract static class GuardedReadElementTypeCacheNode extends JavaScriptBaseNode {
        public abstract boolean guard(Object target);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root);
    }

    @ImportStatic(ReadElementNode.class)
    abstract static class ReadElementTypeCacheDispatchNode extends ReadElementTypeCacheNode {

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1", rewriteOn = UnexpectedResultException.class)
        protected static int doJSObjectLongIndexAsInt(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectReadElementTypeCacheNode objectHandler) throws UnexpectedResultException {
            return objectHandler.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, root);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1", rewriteOn = UnexpectedResultException.class, replaces = {"doJSObjectLongIndexAsInt"})
        protected static int doJSObjectAsInt(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectReadElementTypeCacheNode objectHandler) throws UnexpectedResultException {
            return objectHandler.executeWithTargetAndIndexUncheckedInt(target, index, receiver, defaultValue, root);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1", rewriteOn = UnexpectedResultException.class)
        protected static double doJSObjectLongIndexAsDouble(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectReadElementTypeCacheNode objectHandler) throws UnexpectedResultException {
            return objectHandler.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, root);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1", rewriteOn = UnexpectedResultException.class, replaces = {"doJSObjectLongIndexAsDouble"})
        protected static double doJSObjectAsDouble(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectReadElementTypeCacheNode objectHandler) throws UnexpectedResultException {
            return objectHandler.executeWithTargetAndIndexUncheckedDouble(target, index, receiver, defaultValue, root);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1")
        protected static Object doJSObjectLongIndex(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectReadElementTypeCacheNode objectHandler) {
            return objectHandler.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, root);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isObjectNode.executeBoolean(target)", limit = "1", replaces = {"doJSObjectLongIndex"})
        protected static Object doJSObject(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared IsJSDynamicObjectNode isObjectNode,
                        @Cached @Shared JSObjectReadElementTypeCacheNode objectHandler) {
            return objectHandler.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, root);
        }

        @Specialization
        protected static Object doStringLongIndex(TruffleString target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared StringReadElementTypeCacheNode stringHandler) {
            return stringHandler.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, root);
        }

        @Specialization(replaces = {"doStringLongIndex"})
        protected static Object doString(TruffleString target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared StringReadElementTypeCacheNode stringHandler) {
            return stringHandler.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, root);
        }

        @Specialization(guards = "otherHandler.guard(target)", limit = "BOUNDED_BY_TYPES")
        protected static Object doOther(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached("makeTypeCacheNode(target)") GuardedReadElementTypeCacheNode otherHandler) {
            return otherHandler.executeWithTargetAndIndexUnchecked(target, index, receiver, defaultValue, root);
        }

        protected static GuardedReadElementTypeCacheNode makeTypeCacheNode(Object target) {
            if (JSDynamicObject.isJSDynamicObject(target)) {
                // Handled by other specializations.
                throw CompilerDirectives.shouldNotReachHere("JSDynamicObject");
            } else if (Strings.isTString(target)) {
                // Handled by other specializations.
                throw CompilerDirectives.shouldNotReachHere("TruffleString");
            } else if (target instanceof Boolean) {
                return BooleanReadElementTypeCacheNodeGen.create();
            } else if (target instanceof Number) {
                return NumberReadElementTypeCacheNodeGen.create(target.getClass());
            } else if (target instanceof Symbol) {
                return SymbolReadElementTypeCacheNodeGen.create();
            } else if (target instanceof BigInt) {
                return BigIntReadElementTypeCacheNodeGen.create();
            } else if (target instanceof TruffleObject) {
                assert JSRuntime.isForeignObject(target);
                return ForeignObjectReadElementTypeCacheNodeGen.create();
            } else {
                assert JSRuntime.isJavaPrimitive(target) : target;
                return JavaObjectReadElementTypeCacheNodeGen.create(target.getClass());
            }
        }
    }

    abstract static class JSObjectReadElementTypeCacheNode extends ReadElementTypeCacheNode {
        @Child private IsArrayNode isArrayNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        @Child private JSObjectReadElementNonArrayTypeCacheNode nonArrayCaseNode;
        @Child private ArrayReadElementCacheDispatchNode arrayReadElementNode;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        JSObjectReadElementTypeCacheNode() {
            this.isArrayNode = IsArrayNode.createIsAnyArray();
        }

        private boolean isArray(JSDynamicObject targetObject, InlinedConditionProfile arrayIf) {
            return arrayIf.profile(this, isArrayNode.execute(targetObject));
        }

        private Object toArrayIndex(Object index) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(index);
        }

        private Object readNonArrayObjectIndex(JSDynamicObject targetObject, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            return getNonArrayNode().execute(targetObject, index, receiver, defaultValue, root);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected int doLongIndexAsInt(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile arrayIndexIf) throws UnexpectedResultException {
            JSDynamicObject targetObject = (JSDynamicObject) target;
            if (isArray(targetObject, arrayIf)) {
                ScriptArray array = JSObject.getArray(targetObject);

                if (arrayIndexIf.profile(this, JSRuntime.isArrayIndex(index))) {
                    return executeArrayGetInt(targetObject, array, index, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, Strings.fromLong(index), receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectInteger(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doLongIndexAsDouble(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile arrayIndexIf) throws UnexpectedResultException {
            JSDynamicObject targetObject = (JSDynamicObject) target;
            if (isArray(targetObject, arrayIf)) {
                ScriptArray array = JSObject.getArray(targetObject);

                if (arrayIndexIf.profile(this, JSRuntime.isArrayIndex(index))) {
                    return executeArrayGetDouble(targetObject, array, index, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, Strings.fromLong(index), receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectDouble(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Specialization
        protected Object doLongIndexAsObject(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile arrayIndexIf) {
            JSDynamicObject targetObject = (JSDynamicObject) target;
            if (isArray(targetObject, arrayIf)) {
                ScriptArray array = JSObject.getArray(targetObject);
                if (arrayIndexIf.profile(this, JSRuntime.isArrayIndex(index))) {
                    return executeArrayGet(targetObject, array, index, receiver, defaultValue, root.context);
                } else {
                    return getProperty(targetObject, Strings.fromLong(index), receiver, defaultValue);
                }
            } else {
                return readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root);
            }
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected int doObjectIndexAsInt(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile arrayIndexIf) throws UnexpectedResultException {
            JSDynamicObject targetObject = (JSDynamicObject) target;
            if (isArray(targetObject, arrayIf)) {
                ScriptArray array = JSObject.getArray(targetObject);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexIf.profile(this, objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    return executeArrayGetInt(targetObject, array, longIndex, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, objIndex, receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectInteger(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doObjectIndexAsDouble(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile arrayIndexIf) throws UnexpectedResultException {
            JSDynamicObject targetObject = (JSDynamicObject) target;
            if (isArray(targetObject, arrayIf)) {
                ScriptArray array = JSObject.getArray(targetObject);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexIf.profile(this, objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    return executeArrayGetDouble(targetObject, array, longIndex, receiver, defaultValue, root.context);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, objIndex, receiver, defaultValue));
                }
            } else {
                return JSTypesGen.expectDouble(readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root));
            }
        }

        @Specialization(replaces = {"doLongIndexAsObject"})
        protected Object doObjectIndexAsObject(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared InlinedConditionProfile arrayIf,
                        @Cached @Shared InlinedConditionProfile arrayIndexIf) {
            JSDynamicObject targetObject = (JSDynamicObject) target;
            if (isArray(targetObject, arrayIf)) {
                ScriptArray array = JSObject.getArray(targetObject);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexIf.profile(this, objIndex instanceof Long)) {
                    long longIndex = (Long) objIndex;
                    return executeArrayGet(targetObject, array, longIndex, receiver, defaultValue, root.context);
                } else {
                    return getProperty(targetObject, objIndex, receiver, defaultValue);
                }
            } else {
                return readNonArrayObjectIndex(targetObject, index, receiver, defaultValue, root);
            }
        }

        private Object getProperty(JSDynamicObject targetObject, Object objIndex, Object receiver, Object defaultValue) {
            return JSObject.getOrDefault(targetObject, objIndex, receiver, defaultValue, jsclassProfile, this);
        }

        private JSObjectReadElementNonArrayTypeCacheNode getNonArrayNode() {
            if (nonArrayCaseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nonArrayCaseNode = insert(new JSObjectReadElementNonArrayTypeCacheNode());
            }
            return nonArrayCaseNode;
        }

        private ArrayReadElementCacheDispatchNode initArrayReadElementNode() {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return arrayReadElementNode = insert(ArrayReadElementCacheDispatchNodeGen.create());
        }

        protected final int executeArrayGetInt(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext root)
                        throws UnexpectedResultException {
            ArrayReadElementCacheDispatchNode dispatch = arrayReadElementNode;
            if (dispatch == null) {
                dispatch = initArrayReadElementNode();
            }
            return dispatch.executeArrayGetInt(target, array, index, receiver, defaultValue, root);
        }

        protected final double executeArrayGetDouble(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext root)
                        throws UnexpectedResultException {
            ArrayReadElementCacheDispatchNode dispatch = arrayReadElementNode;
            if (dispatch == null) {
                dispatch = initArrayReadElementNode();
            }
            return dispatch.executeArrayGetDouble(target, array, index, receiver, defaultValue, root);
        }

        protected final Object executeArrayGet(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext root) {
            ArrayReadElementCacheDispatchNode dispatch = arrayReadElementNode;
            if (dispatch == null) {
                dispatch = initArrayReadElementNode();
            }
            return dispatch.executeArrayGet(target, array, index, receiver, defaultValue, root);
        }
    }

    private static class JSObjectReadElementNonArrayTypeCacheNode extends JavaScriptBaseNode {

        @Child private CachedGetPropertyNode getPropertyCachedNode;

        JSObjectReadElementNonArrayTypeCacheNode() {
        }

        public Object execute(JSDynamicObject targetObject, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            if (getPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getPropertyCachedNode = insert(CachedGetPropertyNode.create(root.context));
            }
            return getPropertyCachedNode.execute(targetObject, index, receiver, defaultValue);
        }
    }

    abstract static class JavaObjectReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        protected final Class<?> targetClass;

        JavaObjectReadElementTypeCacheNode(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doPrimitive(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doPrimitive(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            indexToPropertyKeyNode.execute(index);
            return Undefined.instance;
        }

        @Override
        public final boolean guard(Object target) {
            return CompilerDirectives.isExact(target, targetClass);
        }
    }

    protected static ArrayReadElementCacheNode makeArrayCacheNode(@SuppressWarnings("unused") JSDynamicObject target, ScriptArray array) {
        if (array instanceof ConstantEmptyArray) {
            return EmptyArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof ConstantObjectArray && array.isHolesType()) {
            return ConstantObjectArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof LazyRegexResultArray) {
            return LazyRegexResultArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof LazyRegexResultIndicesArray) {
            return LazyRegexResultIndicesArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof LazyArray) {
            return LazyArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof AbstractConstantArray) {
            return ConstantArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof HolesIntArray) {
            return HolesIntArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof HolesDoubleArray) {
            return HolesDoubleArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof HolesJSObjectArray) {
            return HolesJSObjectArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof HolesObjectArray) {
            return HolesObjectArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof AbstractWritableArray) {
            return WritableArrayReadElementCacheNodeGen.create(array);
        } else if (array instanceof TypedArray) {
            if (array instanceof TypedArray.AbstractUint32Array) {
                return Uint32ArrayReadElementCacheNodeGen.create((TypedArray) array);
            } else if (array instanceof TypedArray.TypedIntArray) {
                return TypedIntArrayReadElementCacheNodeGen.create((TypedArray) array);
            } else if (array instanceof TypedArray.TypedFloatArray) {
                return TypedFloatArrayReadElementCacheNodeGen.create((TypedArray) array);
            } else if (array instanceof TypedArray.TypedBigIntArray) {
                return TypedBigIntArrayReadElementCacheNodeGen.create((TypedArray) array);
            } else {
                throw Errors.shouldNotReachHere();
            }
        } else {
            return new ExactArrayReadElementCacheNode(array);
        }
    }

    abstract static class ArrayReadElementCacheNode extends JavaScriptBaseNode {

        protected ArrayReadElementCacheNode() {
        }

        protected abstract Object executeArrayGet(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context);

        protected int executeArrayGetInt(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeArrayGet(target, array, index, receiver, defaultValue, context));
        }

        protected double executeArrayGetDouble(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeArrayGet(target, array, index, receiver, defaultValue, context));
        }

        protected abstract boolean guard(Object target, ScriptArray array);
    }

    @ImportStatic(ReadElementNode.class)
    public abstract static class ArrayReadElementCacheDispatchNode extends JavaScriptBaseNode {

        protected ArrayReadElementCacheDispatchNode() {
        }

        public static ArrayReadElementCacheDispatchNode create() {
            return ArrayReadElementCacheDispatchNodeGen.create();
        }

        protected abstract Object executeArrayGet(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context);

        protected int executeArrayGetInt(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeArrayGet(target, array, index, receiver, defaultValue, context));
        }

        protected double executeArrayGetDouble(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context)
                        throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeArrayGet(target, array, index, receiver, defaultValue, context));
        }

        @Specialization(guards = "arrayType == cachedArrayType", limit = "BOUNDED_BY_TYPES", rewriteOn = UnexpectedResultException.class)
        protected int doDispatchInt(JSDynamicObject target, @SuppressWarnings("unused") ScriptArray arrayType, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached("arrayType") ScriptArray cachedArrayType,
                        @Cached("makeHandler(target, cachedArrayType)") ArrayReadElementCacheNode handler) throws UnexpectedResultException {
            return handler.executeArrayGetInt(target, cachedArrayType, index, receiver, defaultValue, context);
        }

        @Specialization(guards = "arrayType == cachedArrayType", limit = "BOUNDED_BY_TYPES", rewriteOn = UnexpectedResultException.class)
        protected double doDispatchDouble(JSDynamicObject target, @SuppressWarnings("unused") ScriptArray arrayType, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached("arrayType") ScriptArray cachedArrayType,
                        @Cached("makeHandler(target, cachedArrayType)") ArrayReadElementCacheNode handler) throws UnexpectedResultException {
            return handler.executeArrayGetDouble(target, cachedArrayType, index, receiver, defaultValue, context);
        }

        @Specialization(guards = "arrayType == cachedArrayType", limit = "BOUNDED_BY_TYPES")
        protected static Object doDispatch(JSDynamicObject target, @SuppressWarnings("unused") ScriptArray arrayType, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached("arrayType") ScriptArray cachedArrayType,
                        @Cached("makeHandler(target, cachedArrayType)") ArrayReadElementCacheNode handler) {
            return handler.executeArrayGet(target, cachedArrayType, index, receiver, defaultValue, context);
        }

        protected static ArrayReadElementCacheNode makeHandler(JSDynamicObject target, ScriptArray arrayType) {
            return makeArrayCacheNode(target, arrayType);
        }
    }

    abstract static class ArrayClassGuardCachedArrayReadElementCacheNode extends ArrayReadElementCacheNode {
        private final ScriptArray arrayType;
        private final JSClassProfile outOfBoundsClassProfile = JSClassProfile.create();

        ArrayClassGuardCachedArrayReadElementCacheNode(ScriptArray arrayType) {
            super();
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

        protected Object readOutOfBounds(JSDynamicObject target, long index, Object receiver, Object defaultValue, JSContext context, InlinedConditionProfile needGetProperty) {
            if (needGetProperty.profile(this, needsSlowGet(target, context))) {
                return JSObject.getOrDefault(target, index, receiver, defaultValue, outOfBoundsClassProfile, this);
            } else {
                return defaultValue;
            }
        }

        private static boolean needsSlowGet(JSDynamicObject target, JSContext context) {
            return !context.getArrayPrototypeNoElementsAssumption().isValid() || (!context.getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(target)) ||
                            (!context.getFastArgumentsObjectAssumption().isValid() && JSSlowArgumentsArray.isJSSlowArgumentsObject(target));
        }
    }

    private static class ExactArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final JSClassProfile classProfile = JSClassProfile.create();

        ExactArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Override
        protected Object executeArrayGet(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context) {
            return JSObject.getOrDefault(target, index, receiver, defaultValue, classProfile, this);
        }
    }

    abstract static class ConstantArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        ConstantArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doConstantArray(JSDynamicObject target, AbstractConstantArray constantArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile needGetProperty) {
            if (inBounds.profile(this, constantArray.hasElement(target, index))) {
                return constantArray.getElementInBounds(target, (int) index);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
            }
        }
    }

    abstract static class EmptyArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        EmptyArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doEmptyArray(JSDynamicObject target, @SuppressWarnings("unused") ConstantEmptyArray emptyArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile needGetProperty) {
            return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
        }
    }

    abstract static class ConstantObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        ConstantObjectArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doConstantObjectArray(JSDynamicObject target, ConstantObjectArray constantObjectArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile notHoleArray,
                        @Cached InlinedConditionProfile notHoleValue,
                        @Cached InlinedConditionProfile needGetProperty) {
            if (inBounds.profile(this, constantObjectArray.isInBoundsFast(target, index))) {
                Object value = ConstantObjectArray.getElementInBoundsDirect(target, (int) index);
                if (notHoleArray.profile(this, !constantObjectArray.hasHoles(target))) {
                    return value;
                } else {
                    if (notHoleValue.profile(this, !HolesObjectArray.isHoleValue(value))) {
                        return value;
                    }
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
        }
    }

    abstract static class LazyRegexResultArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        @Child private DynamicObjectLibrary lazyRegexResultNode = JSObjectUtil.createDispatched(JSAbstractArray.LAZY_REGEX_RESULT_ID);
        @Child private DynamicObjectLibrary lazyRegexResultOriginalInputNode = JSObjectUtil.createDispatched(JSAbstractArray.LAZY_REGEX_ORIGINAL_INPUT_ID);
        @Child private TruffleString.SubstringByteIndexNode substringNode = TruffleString.SubstringByteIndexNode.create();
        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getStartNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();
        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getEndNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();

        LazyRegexResultArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doLazyRegexResultArray(JSDynamicObject target, LazyRegexResultArray lazyRegexResultArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile needGetProperty) {
            int intIndex = (int) index;
            if (inBounds.profile(this, lazyRegexResultArray.hasElement(target, intIndex))) {
                return LazyRegexResultArray.materializeGroup(context, target, intIndex,
                                lazyRegexResultNode, lazyRegexResultOriginalInputNode, null, substringNode, getStartNode, getEndNode);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
            }
        }
    }

    abstract static class LazyRegexResultIndicesArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getStartNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();
        @Child private TRegexUtil.InvokeGetGroupBoundariesMethodNode getEndNode = TRegexUtil.InvokeGetGroupBoundariesMethodNode.create();

        LazyRegexResultIndicesArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doLazyRegexResultIndicesArray(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile needGetProperty) {
            LazyRegexResultIndicesArray lazyRegexResultIndicesArray = (LazyRegexResultIndicesArray) array;
            int intIndex = (int) index;
            if (inBounds.profile(this, lazyRegexResultIndicesArray.hasElement(target, intIndex))) {
                return LazyRegexResultIndicesArray.materializeGroup(context, target, intIndex,
                                null, getStartNode, getEndNode);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
            }
        }
    }

    abstract static class LazyArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        @Child private ListGetNode listGetNode;

        LazyArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
            this.listGetNode = ListGetNodeGen.create();
        }

        @Specialization
        protected Object doLazyArray(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile needGetProperty) {
            LazyArray lazyRegexResultArray = (LazyArray) array;
            int intIndex = (int) index;
            if (inBounds.profile(this, lazyRegexResultArray.hasElement(target, intIndex))) {
                return lazyRegexResultArray.getElementInBounds(target, intIndex, listGetNode);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
            }
        }
    }

    abstract static class WritableArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        WritableArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected int doWritableArrayInt(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds,
                        @Cached @Shared InlinedConditionProfile needGetProperty)
                        throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(this, writableArray.isInBoundsFast(target, index))) {
                return writableArray.getInBoundsFastInt(target, (int) index);
            } else {
                return JSTypesGen.expectInteger(readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty));
            }
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doWritableArrayDouble(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds,
                        @Cached @Shared InlinedConditionProfile needGetProperty)
                        throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(this, writableArray.isInBoundsFast(target, index))) {
                return writableArray.getInBoundsFastDouble(target, (int) index);
            } else {
                return JSTypesGen.expectDouble(readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty));
            }
        }

        @Specialization
        protected Object doWritableArray(JSDynamicObject target, ScriptArray array, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds,
                        @Cached @Shared InlinedConditionProfile needGetProperty) {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(this, writableArray.isInBoundsFast(target, index))) {
                return writableArray.getInBoundsFast(target, (int) index);
            } else {
                return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
            }
        }
    }

    abstract static class HolesIntArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        HolesIntArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doHolesIntArray(JSDynamicObject target, HolesIntArray holesIntArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile notHoleValue,
                        @Cached InlinedConditionProfile needGetProperty) {
            if (inBounds.profile(this, holesIntArray.isInBoundsFast(target, index))) {
                int value = holesIntArray.getInBoundsFastInt(target, (int) index);
                if (notHoleValue.profile(this, !HolesIntArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
        }
    }

    abstract static class HolesDoubleArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        HolesDoubleArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doHolesDoubleArray(JSDynamicObject target, HolesDoubleArray holesDoubleArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile notHoleValue,
                        @Cached InlinedConditionProfile needGetProperty) {
            if (inBounds.profile(this, holesDoubleArray.isInBoundsFast(target, index))) {
                double value = holesDoubleArray.getInBoundsFastDouble(target, (int) index);
                if (notHoleValue.profile(this, !HolesDoubleArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
        }
    }

    abstract static class HolesJSObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        HolesJSObjectArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doHolesJSObjectArray(JSDynamicObject target, HolesJSObjectArray holesArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile notHoleValue,
                        @Cached InlinedConditionProfile needGetProperty) {
            if (inBounds.profile(this, holesArray.isInBoundsFast(target, index))) {
                JSDynamicObject value = holesArray.getInBoundsFastJSObject(target, (int) index);
                if (notHoleValue.profile(this, !HolesJSObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
        }
    }

    abstract static class HolesObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        HolesObjectArrayReadElementCacheNode(ScriptArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doHolesObjectArray(JSDynamicObject target, HolesObjectArray holesArray, long index, Object receiver, Object defaultValue, JSContext context,
                        @Cached InlinedConditionProfile inBounds,
                        @Cached InlinedConditionProfile notHoleValue,
                        @Cached InlinedConditionProfile needGetProperty) {
            if (inBounds.profile(this, holesArray.isInBoundsFast(target, index))) {
                Object value = holesArray.getInBoundsFastObject(target, (int) index);
                if (notHoleValue.profile(this, !HolesObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, receiver, defaultValue, context, needGetProperty);
        }
    }

    private abstract static class AbstractTypedArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        @Child protected InteropLibrary interop;

        AbstractTypedArrayReadElementCacheNode(TypedArray arrayType) {
            super(arrayType);
            this.interop = arrayType.isInterop() ? InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit) : InteropLibrary.getUncached();
        }

    }

    abstract static class TypedIntArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {

        TypedIntArrayReadElementCacheNode(TypedArray arrayType) {
            super(arrayType);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected int doTypedIntArrayInt(JSDynamicObject target, TypedArray.TypedIntArray typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue, JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds) throws UnexpectedResultException {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doTypedIntArrayDouble(JSDynamicObject target, TypedArray.TypedIntArray typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue,
                        JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds) throws UnexpectedResultException {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Specialization
        protected Object doTypedIntArray(JSDynamicObject target, TypedArray.TypedIntArray typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue, JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds) {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop);
            } else {
                return defaultValue;
            }
        }
    }

    abstract static class Uint32ArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {

        Uint32ArrayReadElementCacheNode(TypedArray arrayType) {
            super(arrayType);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected int doTypedUint32Array(JSDynamicObject target, TypedArray.AbstractUint32Array typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue,
                        JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds,
                        @Cached @Shared InlinedConditionProfile notNegative)
                        throws UnexpectedResultException {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                int intValue = typedArray.getInt(target, (int) index, interop);
                if (notNegative.profile(this, intValue >= 0)) {
                    return intValue;
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnexpectedResultException((double) (intValue & 0xffff_ffffL));
                }
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doTypedUint32ArrayDouble(JSDynamicObject target, TypedArray.AbstractUint32Array typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue,
                        JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds)
                        throws UnexpectedResultException {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                return typedArray.getInt(target, (int) index, interop) & 0xffff_ffffL;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Specialization
        protected Object doTypedUint32ArrayGet(JSDynamicObject target, TypedArray.AbstractUint32Array typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue,
                        JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds,
                        @Cached @Shared InlinedConditionProfile notNegative) {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                int intValue = typedArray.getInt(target, (int) index, interop);
                if (notNegative.profile(this, intValue >= 0)) {
                    return intValue;
                } else {
                    return (double) (intValue & 0xffff_ffffL);
                }
            } else {
                return defaultValue;
            }
        }
    }

    abstract static class TypedFloatArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {

        TypedFloatArrayReadElementCacheNode(TypedArray arrayType) {
            super(arrayType);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doTypedFloatArrayDouble(JSDynamicObject target, TypedArray.TypedFloatArray typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue,
                        JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds)
                        throws UnexpectedResultException {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                return typedArray.getDouble(target, (int) index, interop);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Specialization
        protected Object doTypedFloatArray(JSDynamicObject target, TypedArray.TypedFloatArray typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue,
                        JSContext context,
                        @Cached @Shared InlinedConditionProfile inBounds) {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                return typedArray.getDouble(target, (int) index, interop);
            } else {
                return defaultValue;
            }
        }

    }

    abstract static class TypedBigIntArrayReadElementCacheNode extends AbstractTypedArrayReadElementCacheNode {

        TypedBigIntArrayReadElementCacheNode(TypedArray arrayType) {
            super(arrayType);
        }

        @Specialization
        protected Object doTypedBigIntArray(JSDynamicObject target, TypedArray.TypedBigIntArray typedArray, long index, @SuppressWarnings("unused") Object receiver, Object defaultValue,
                        JSContext context,
                        @Cached InlinedConditionProfile inBounds) {
            if (!JSArrayBufferView.hasDetachedBuffer(target, context) && inBounds.profile(this, typedArray.hasElement(target, index))) {
                return typedArray.getBigInt(target, (int) index, interop);
            } else {
                return defaultValue;
            }
        }
    }

    private abstract static class ToPropertyKeyCachedReadElementTypeCacheNode extends GuardedReadElementTypeCacheNode {
        @Child private CachedGetPropertyNode getPropertyCachedNode;
        protected final JSClassProfile jsclassProfile = JSClassProfile.create();

        ToPropertyKeyCachedReadElementTypeCacheNode() {
            super();
        }

        public Object readFromWrapper(JSDynamicObject wrapper, Object index, Object receiver, Object defaultValue, ReadElementNode root) {
            if (getPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getPropertyCachedNode = insert(CachedGetPropertyNode.create(root.context));
            }
            return getPropertyCachedNode.execute(wrapper, index, receiver, defaultValue);
        }
    }

    abstract static class StringReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {

        @Child private ToArrayIndexNode toArrayIndexNode;
        @Child private TruffleString.SubstringByteIndexNode substringByteIndexNode;

        StringReadElementTypeCacheNode() {
            this.toArrayIndexNode = ToArrayIndexNode.createNoToPropertyKey();
            this.substringByteIndexNode = TruffleString.SubstringByteIndexNode.create();
        }

        @Specialization
        protected Object doStringLongIndex(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Shared InlinedConditionProfile stringIndexInBounds) {
            TruffleString string = (TruffleString) target;
            if (stringIndexInBounds.profile(this, index >= 0 && index < Strings.length(string))) {
                return Strings.substring(root.context, substringByteIndexNode, string, (int) index, 1);
            } else {
                return JSObject.getOrDefault(JSString.create(root.context, getRealm(), string), index, receiver, defaultValue, jsclassProfile, root);
            }
        }

        @Specialization
        protected Object doString(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached @Exclusive InlinedConditionProfile arrayIndexIf,
                        @Cached @Shared InlinedConditionProfile stringIndexInBounds,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            TruffleString string = (TruffleString) target;
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (arrayIndexIf.profile(this, convertedIndex instanceof Long)) {
                int intIndex = ((Long) convertedIndex).intValue();
                if (stringIndexInBounds.profile(this, intIndex >= 0 && intIndex < Strings.length(string))) {
                    return Strings.substring(root.context, substringByteIndexNode, string, intIndex, 1);
                }
            }
            return readFromWrapper(JSString.create(root.context, getRealm(), string), indexToPropertyKeyNode.execute(index), receiver, defaultValue, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof TruffleString;
        }
    }

    abstract static class NumberReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> numberClass;

        NumberReadElementTypeCacheNode(Class<?> stringClass) {
            this.numberClass = stringClass;
        }

        @Specialization
        protected Object doNumber(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            Number number = (Number) CompilerDirectives.castExact(target, numberClass);
            return JSObject.getOrDefault(JSNumber.create(root.context, getRealm(), number), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Specialization
        protected Object doNumber(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            Number number = (Number) CompilerDirectives.castExact(target, numberClass);
            return readFromWrapper(JSNumber.create(root.context, getRealm(), number), indexToPropertyKeyNode.execute(index), receiver, defaultValue, root);
        }

        @Override
        public boolean guard(Object target) {
            return CompilerDirectives.isExact(target, numberClass);
        }
    }

    abstract static class BooleanReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        BooleanReadElementTypeCacheNode() {
        }

        @Specialization
        protected Object doBoolean(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            Boolean bool = (Boolean) target;
            return JSObject.getOrDefault(JSBoolean.create(root.context, getRealm(), bool), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Specialization
        protected Object doBoolean(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            Boolean bool = (Boolean) target;
            return readFromWrapper(JSBoolean.create(root.context, getRealm(), bool), indexToPropertyKeyNode.execute(index), receiver, defaultValue, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Boolean;
        }
    }

    abstract static class SymbolReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {

        SymbolReadElementTypeCacheNode() {
        }

        @Specialization
        protected Object doSymbol(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            Symbol symbol = (Symbol) target;
            return JSObject.getOrDefault(JSSymbol.create(root.context, getRealm(), symbol), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Specialization
        protected Object doSymbol(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            Symbol symbol = (Symbol) target;
            return readFromWrapper(JSSymbol.create(root.context, getRealm(), symbol), indexToPropertyKeyNode.execute(index), receiver, defaultValue, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Symbol;
        }
    }

    abstract static class BigIntReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {

        BigIntReadElementTypeCacheNode() {
            super();
        }

        @Specialization
        protected Object doBigInt(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            BigInt bigInt = (BigInt) target;
            return JSObject.getOrDefault(JSBigInt.create(root.context, getRealm(), bigInt), index, receiver, defaultValue, jsclassProfile, root);
        }

        @Specialization
        protected Object doBigInt(Object target, Object index, Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached JSToPropertyKeyNode indexToPropertyKeyNode) {
            BigInt bigInt = (BigInt) target;
            return readFromWrapper(JSBigInt.create(root.context, getRealm(), bigInt), indexToPropertyKeyNode.execute(index), receiver, defaultValue, root);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof BigInt;
        }
    }

    abstract static class ForeignObjectReadElementTypeCacheNode extends GuardedReadElementTypeCacheNode {

        @Child private InteropLibrary interop;
        @Child private JSToPropertyKeyNode toPropertyKeyNode;
        @Child private ImportValueNode importValueNode;
        @Child private InteropLibrary getterInterop;
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;
        @Child private ReadElementNode readFromPrototypeNode;
        @Child private ToArrayIndexNode toArrayIndexNode;

        @CompilationFinal private boolean optimistic = true;

        ForeignObjectReadElementTypeCacheNode() {
            super();
            this.importValueNode = ImportValueNode.create();
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Specialization
        protected Object doForeignObject(Object target, Object index, @SuppressWarnings("unused") Object receiver, Object defaultValue, ReadElementNode root,
                        @Cached InlinedExactClassProfile classProfile,
                        @Cached InlinedBranchProfile errorBranch) {
            Object truffleObject = classProfile.profile(this, target);
            if (interop.isNull(truffleObject)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorCannotGetProperty(root.getContext(), JSRuntime.safeToString(index), target, false, this);
            }
            Object foreignResult = getImpl(truffleObject, index, defaultValue, root, errorBranch);
            if (foreignResult == defaultValue) {
                return foreignResult;
            }
            return importValueNode.executeWithTarget(foreignResult);
        }

        private Object getImpl(Object truffleObject, Object key, Object defaultValue, ReadElementNode root,
                        @Cached InlinedBranchProfile errorBranch) {
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
                    return defaultValue;
                }
            } else {
                propertyKey = toPropertyKey(key);
            }
            if (root.context.getContextOptions().hasForeignHashProperties() && interop.hasHashEntries(truffleObject)) {
                try {
                    return interop.readHashValue(truffleObject, propertyKey);
                } catch (UnknownKeyException e) {
                    // fall through: still need to try members
                } catch (UnsupportedMessageException e) {
                    return defaultValue;
                }
            }
            if (propertyKey instanceof Symbol) {
                return maybeReadFromPrototype(truffleObject, propertyKey, root.context);
            }
            TruffleString exportedKeyStr = (TruffleString) propertyKey;
            if (hasArrayElements && Strings.equals(JSAbstractArray.LENGTH, exportedKeyStr)) {
                return getSize(truffleObject, errorBranch);
            }
            if (root.context.isOptionNashornCompatibilityMode()) {
                Object result = tryGetters(truffleObject, exportedKeyStr, root.context);
                if (result != null) {
                    return result;
                }
            }
            String stringKey = Strings.toJavaString(exportedKeyStr);
            if (optimistic) {
                try {
                    return interop.readMember(truffleObject, stringKey);
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    optimistic = false;
                    return maybeReadFromPrototype(truffleObject, exportedKeyStr, root.context);
                }
            } else {
                if (interop.isMemberReadable(truffleObject, stringKey)) {
                    try {
                        return interop.readMember(truffleObject, stringKey);
                    } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                        return defaultValue;
                    }
                } else {
                    return maybeReadFromPrototype(truffleObject, exportedKeyStr, root.context);
                }
            }
        }

        private Object tryGetters(Object thisObj, TruffleString key, JSContext context) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = getRealm().getEnv();
            if (env.isHostObject(thisObj)) {
                Object result = tryInvokeGetter(thisObj, Strings.GET, key);
                if (result != null) {
                    return result;
                }
                result = tryInvokeGetter(thisObj, Strings.IS, key);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        private Object tryInvokeGetter(Object thisObj, TruffleString prefix, TruffleString key) {
            TruffleString getterKey = PropertyCacheNode.getAccessorKey(prefix, key);
            if (getterKey == null) {
                return null;
            }
            if (getterInterop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getterInterop = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
            }
            if (!getterInterop.isMemberInvocable(thisObj, Strings.toJavaString(getterKey))) {
                return null;
            }
            try {
                return getterInterop.invokeMember(thisObj, Strings.toJavaString(getterKey), JSArguments.EMPTY_ARGUMENTS_ARRAY);
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                return null; // try the next fallback
            }
        }

        private Object getSize(Object truffleObject, InlinedBranchProfile errorBranch) {
            try {
                return JSRuntime.longToIntOrDouble(interop.getArraySize(truffleObject));
            } catch (UnsupportedMessageException e) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorInteropException(truffleObject, e, "getArraySize", this);
            }
        }

        @InliningCutoff
        private Object maybeReadFromPrototype(Object truffleObject, Object key, JSContext context) {
            assert JSRuntime.isPropertyKey(key);
            if (context.getContextOptions().hasForeignObjectPrototype() || key instanceof Symbol || JSInteropUtil.isBoxedPrimitive(truffleObject, interop)) {
                if (readFromPrototypeNode == null || foreignObjectPrototypeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    this.readFromPrototypeNode = insert(ReadElementNode.create(context));
                    this.foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
                }
                JSDynamicObject prototype = foreignObjectPrototypeNode.execute(truffleObject);
                return readFromPrototypeNode.executeWithTargetAndIndex(prototype, key);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, long index, Object receiver, Object defaultValue, ReadElementNode root) {
            return executeWithTargetAndIndexUnchecked(target, (Object) index, receiver, defaultValue, root);
        }

        @Override
        public boolean guard(Object target) {
            return JSRuntime.isForeignObject(target);
        }

        private Object toArrayIndex(Object maybeIndex) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(maybeIndex);
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
