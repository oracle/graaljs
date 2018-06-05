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
package com.oracle.truffle.js.nodes.access;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetAllocationSite;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementExpressionTag;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.TypedArray;
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
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSlowArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSSlowArray;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.interop.Converters;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

public class WriteElementNode extends JSTargetableNode {
    @Child protected JavaScriptNode targetNode;
    @Child protected JavaScriptNode indexNode;
    @Child protected JavaScriptNode valueNode;
    @Child protected WriteElementTypeCacheNode typeCacheNode;

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
        this.targetNode = targetNode;
        this.indexNode = indexNode;
        this.valueNode = valueNode;
        this.typeCacheNode = new UninitWriteElementTypeCacheNode(context, isStrict, writeOwn);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == WriteElementExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadElementExpressionTag.class)) {
            JavaScriptNode clonedTarget = targetNode.hasSourceSection() ? targetNode : JSTaggedExecutionNode.createFor(targetNode, ExpressionTag.class);
            JavaScriptNode clonedIndex = indexNode.hasSourceSection() ? indexNode : JSTaggedExecutionNode.createFor(indexNode, ExpressionTag.class);
            JavaScriptNode clonedValue = valueNode.hasSourceSection() ? valueNode : JSTaggedExecutionNode.createFor(valueNode, ExpressionTag.class);
            JavaScriptNode cloned = WriteElementNode.create(clonedTarget, clonedIndex, clonedValue, getContext(), isStrict(), typeCacheNode.writeOwn);
            transferSourceSection(this, cloned);
            return cloned;
        }
        return this;
    }

    @Override
    public Object evaluateTarget(VirtualFrame frame) {
        return targetNode.execute(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeWithTarget(frame, evaluateTarget(frame));
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return executeWithTargetInt(frame, evaluateTarget(frame));
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return executeWithTargetDouble(frame, evaluateTarget(frame));
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        if (indexState == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndex(frame, target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(frame, target, index);
            }
        }
        if (indexState == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(frame, target, e.getResult());
            }
            return executeWithTargetAndIndex(frame, target, index);
        } else {
            assert indexState == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            return executeWithTargetAndIndex(frame, target, index);
        }
    }

    public int executeWithTargetInt(VirtualFrame frame, Object target) throws UnexpectedResultException {
        if (indexState == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexInt(frame, target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(frame, target, index);
            }
        }
        if (indexState == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(frame, target, e.getResult());
            }
            return executeWithTargetAndIndexInt(frame, target, index);
        } else {
            assert indexState == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            return executeWithTargetAndIndexInt(frame, target, index);
        }
    }

    public double executeWithTargetDouble(VirtualFrame frame, Object target) throws UnexpectedResultException {
        if (indexState == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexDouble(frame, target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(frame, target, index);
            }
        }
        if (indexState == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(frame, target, e.getResult());
            }
            return executeWithTargetAndIndexDouble(frame, target, index);
        } else {
            assert indexState == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            return executeWithTargetAndIndexDouble(frame, target, index);
        }
    }

    protected final Object executeWithTargetAndIndex(VirtualFrame frame, Object target, Object index) {
        Object value = valueNode.execute(frame);
        executeWithTargetAndIndexAndValue(target, index, value);
        return value;
    }

    protected final Object executeWithTargetAndIndex(VirtualFrame frame, Object target, int index) {
        Object value = valueNode.execute(frame);
        executeWithTargetAndIndexAndValue(target, index, value);
        return value;
    }

    protected final int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, Object index) throws UnexpectedResultException {
        try {
            int value = valueNode.executeInt(frame);
            executeWithTargetAndIndexAndValue(target, index, value);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult());
            throw e;
        }
    }

    protected final int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, int index) throws UnexpectedResultException {
        try {
            int value = valueNode.executeInt(frame);
            executeWithTargetAndIndexAndValue(target, index, (Object) value);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult());
            throw e;
        }
    }

    protected final double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, Object index) throws UnexpectedResultException {
        try {
            double value = valueNode.executeDouble(frame);
            executeWithTargetAndIndexAndValue(target, index, value);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult());
            throw e;
        }
    }

    protected final double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, int index) throws UnexpectedResultException {
        try {
            double value = valueNode.executeDouble(frame);
            executeWithTargetAndIndexAndValue(target, index, (Object) value);
            return value;
        } catch (UnexpectedResultException e) {
            executeWithTargetAndIndexAndValue(target, index, e.getResult());
            throw e;
        }
    }

    public final void executeWithTargetAndIndexAndValue(Object target, Object index, Object value) {
        typeCacheNode.executeWithTargetAndIndexAndValue(target, index, value);
    }

    public final void executeWithTargetAndIndexAndValue(Object target, int index, Object value) {
        typeCacheNode.executeWithTargetAndIndexAndValue(target, index, value);
    }

    protected abstract static class WriteElementCacheNode extends JavaScriptBaseNode {
        protected final JSContext context;
        protected final boolean isStrict;
        protected final boolean writeOwn;

        protected WriteElementCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            this.context = context;
            this.isStrict = isStrict;
            this.writeOwn = writeOwn;
        }
    }

    private abstract static class WriteElementTypeCacheNode extends WriteElementCacheNode {
        protected WriteElementTypeCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
        }

        public abstract void executeWithTargetAndIndexAndValue(Object target, Object index, Object value);

        public void executeWithTargetAndIndexAndValue(Object target, int index, Object value) {
            executeWithTargetAndIndexAndValue(target, (Object) index, value);
        }
    }

    private static class UninitWriteElementTypeCacheNode extends WriteElementTypeCacheNode {
        UninitWriteElementTypeCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
        }

        @Override
        public void executeWithTargetAndIndexAndValue(Object target, Object index, Object value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            CachedWriteElementTypeCacheNode specialized = makeTypeCacheNode(target);
            this.replace(specialized);
            specialized.executeWithTargetAndIndexAndValue(target, index, value);
        }

        @SuppressWarnings("unchecked")
        private CachedWriteElementTypeCacheNode makeTypeCacheNode(Object target) {
            if (JSProxy.isProxy(target)) {
                return new ProxyWriteElementNode(context, isStrict, writeOwn);
            } else if (JSObject.isJSObject(target)) {
                return new JSObjectWriteElementTypeCacheNode(context, isStrict, writeOwn);
            } else if (JSRuntime.isString(target)) {
                return new StringWriteElementTypeCacheNode(context, isStrict, target.getClass(), writeOwn);
            } else if (target instanceof Boolean) {
                return new BooleanWriteElementTypeCacheNode(context, isStrict, writeOwn);
            } else if (target instanceof Number) {
                return new NumberWriteElementTypeCacheNode(context, isStrict, target.getClass(), writeOwn);
            } else if (target instanceof Symbol && isStrict) {
                throw Errors.createTypeError("cannot set element on Symbol in strict mode", this);
            } else if (target instanceof TruffleObject && !(target instanceof Symbol)) {
                return new TruffleObjectWriteElementTypeCacheNode(context, isStrict, (Class<? extends TruffleObject>) target.getClass(), writeOwn);
            } else if (target instanceof Map) {
                return new MapWriteElementTypeCacheNode(context, isStrict, (Class<? extends Map<Object, Object>>) target.getClass(), writeOwn);
            } else if (target instanceof List) {
                return new ListWriteElementTypeCacheNode(context, isStrict, target.getClass(), writeOwn);
            } else if (JSGuards.isJavaArray(target)) {
                return new JavaArrayWriteElementTypeCacheNode(context, isStrict, target.getClass(), writeOwn);
            } else {
                return new ObjectWriteElementTypeCacheNode(context, isStrict, target.getClass(), writeOwn);
            }
        }
    }

    private static class ProxyWriteElementNode extends CachedWriteElementTypeCacheNode {
        @Child private JSProxyPropertySetNode proxySet;
        @Child private JSToPropertyKeyNode toPropertyKeyNode;

        protected ProxyWriteElementNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
            if (!writeOwn) {
                this.proxySet = JSProxyPropertySetNode.create(context, isStrict);
            }
        }

        private JSToPropertyKeyNode getToPropertyKeyNode() {
            if (toPropertyKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toPropertyKeyNode = insert(JSToPropertyKeyNode.create());
            }
            return toPropertyKeyNode;
        }

        @Override
        public boolean guard(Object target) {
            return JSProxy.isProxy(target);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            if (writeOwn) {
                createDataPropertyOrThrow(target, index, value);
            } else {
                proxySet.executeWithReceiverAndValue(target, target, value, getToPropertyKeyNode().execute(index), isStrict);
            }
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            if (writeOwn) {
                createDataPropertyOrThrow(target, index, value);
            } else {
                proxySet.executeWithReceiverAndValueIntKey(target, target, value, index, isStrict);
            }
        }

        protected void createDataPropertyOrThrow(Object o, Object i, Object v) {
            Object p = getToPropertyKeyNode().execute(i);
            PropertyDescriptor newDesc = PropertyDescriptor.createDataDefault(v);
            JSProxy.INSTANCE.defineOwnProperty((DynamicObject) o, p, newDesc, true);
        }
    }

    private abstract static class CachedWriteElementTypeCacheNode extends WriteElementTypeCacheNode {
        @Child private WriteElementTypeCacheNode typeCacheNext;

        CachedWriteElementTypeCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
        }

        @Override
        public void executeWithTargetAndIndexAndValue(Object target, Object index, Object value) {
            if (guard(target)) {
                executeWithTargetAndIndexUnguarded(target, index, value);
            } else {
                getNext().executeWithTargetAndIndexAndValue(target, index, value);
            }
        }

        @Override
        public void executeWithTargetAndIndexAndValue(Object target, int index, Object value) {
            if (guard(target)) {
                executeWithTargetAndIndexUnguarded(target, index, value);
            } else {
                getNext().executeWithTargetAndIndexAndValue(target, index, value);
            }
        }

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value);

        protected abstract void executeWithTargetAndIndexUnguarded(Object target, int index, Object value);

        public abstract boolean guard(Object target);

        private WriteElementTypeCacheNode getNext() {
            if (typeCacheNext == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                typeCacheNext = insert(new UninitWriteElementTypeCacheNode(context, isStrict, writeOwn));
            }
            return typeCacheNext;
        }
    }

    private static class JSObjectWriteElementTypeCacheNode extends CachedWriteElementTypeCacheNode {
        @Child private IsArrayNode isArrayNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        @Child private ArrayWriteElementCacheNode arrayWriteElementNode;
        private final ConditionProfile intOrStringIndexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayProfile = ConditionProfile.createBinaryProfile();
        private final JSClassProfile jsclassProfile = JSClassProfile.create();
        @Child private CachedSetPropertyNode setPropertyCachedNode;

        JSObjectWriteElementTypeCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
            this.isArrayNode = IsArrayNode.createIsFastOrTypedArray();
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            DynamicObject targetObject = JSObject.castJSObject(target);
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                Object objIndex = toArrayIndexNode.execute(index);

                if (intOrStringIndexProfile.profile(objIndex instanceof Long)) {
                    getArrayWriteElementNode().executeWithTargetAndArrayAndIndexAndValue(targetObject, array, (Long) objIndex, value, arrayCondition);
                } else {
                    setPropertyGenericEvaluatedStringOrSymbol(targetObject, objIndex, value);
                }
            } else {
                setPropertyGeneric(targetObject, index, value);
            }
        }

        private ArrayWriteElementCacheNode getArrayWriteElementNode() {
            if (arrayWriteElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayWriteElementNode = insert(new UninitArrayWriteElementCacheNode(context, isStrict, writeOwn, false));
            }
            return arrayWriteElementNode;
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            DynamicObject targetObject = JSObject.castJSObject(target);
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);

                if (intOrStringIndexProfile.profile(index >= 0)) {
                    getArrayWriteElementNode().executeWithTargetAndArrayAndIndexAndValue(targetObject, array, index, value, arrayCondition);
                } else {
                    setPropertyGenericEvaluatedStringOrSymbol(targetObject, Boundaries.stringValueOf(index), value);
                }
            } else {
                setPropertyGeneric(targetObject, index, value);
            }
        }

        private void setPropertyGenericEvaluatedStringOrSymbol(DynamicObject targetObject, Object key, Object value) {
            JSObject.set(targetObject, key, value, isStrict, jsclassProfile);
        }

        private void setPropertyGeneric(DynamicObject targetObject, Object index, Object value) {
            setCachedProperty(targetObject, index, value);
        }

        private void setCachedProperty(DynamicObject targetObject, Object index, Object value) {
            if (setPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setPropertyCachedNode = insert(CachedSetPropertyNode.create(context, isStrict));
            }
            setPropertyCachedNode.execute(targetObject, index, value);
        }

        @Override
        public boolean guard(Object target) {
            return JSObject.isDynamicObject(target);
        }
    }

    private static class ObjectWriteElementTypeCacheNode extends CachedWriteElementTypeCacheNode {
        protected final Class<?> targetClass;

        ObjectWriteElementTypeCacheNode(JSContext context, boolean isStrict, Class<?> targetClass, boolean writeOwn) {
            super(context, isStrict, writeOwn);
            this.targetClass = targetClass;
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            // throw new UnsupportedOperationException();
        }

        @Override
        public final boolean guard(Object target) {
            // return !(JSObject.isJSObject(target));
            return targetClass.isInstance(target);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            executeWithTargetAndIndexAndValue(target, (Object) index, value);
        }
    }

    private static class MapWriteElementTypeCacheNode extends CachedWriteElementTypeCacheNode {
        private final Class<? extends Map<Object, Object>> targetClass;
        private final Converters.Converter converter;

        MapWriteElementTypeCacheNode(JSContext context, boolean isStrict, Class<? extends Map<Object, Object>> targetClass, boolean writeOwn) {
            super(context, isStrict, writeOwn);
            this.targetClass = targetClass;
            this.converter = Converters.JS_TO_JAVA_CONVERTER;
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            Map<Object, Object> map = targetClass.cast(target);
            Boundaries.mapPut(map, converter.convert(index), converter.convert(value));
        }

        @Override
        public final boolean guard(Object target) {
            return targetClass.isInstance(target);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            executeWithTargetAndIndexAndValue(target, (Object) index, value);
        }
    }

    private static class ListWriteElementTypeCacheNode extends ObjectWriteElementTypeCacheNode {
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final ConditionProfile indexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile outOfBoundsProfile = ConditionProfile.createBinaryProfile();

        ListWriteElementTypeCacheNode(JSContext context, boolean isStrict, Class<?> targetClass, boolean writeOwn) {
            super(context, isStrict, targetClass, writeOwn);
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (indexProfile.profile(convertedIndex instanceof Long && ((Long) convertedIndex).intValue() >= 0)) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) targetClass.cast(target);
                Boundaries.listSet(list, ((Long) convertedIndex).intValue(), value);
            } else {
                if (outOfBoundsProfile.profile(index instanceof Double && Double.isInfinite(((Double) index).doubleValue()))) {
                    throwOutOfBoundsException(target, index);
                }
                super.executeWithTargetAndIndexUnguarded(target, index, value);
            }
        }

        @TruffleBoundary
        private void throwOutOfBoundsException(Object target, Object index) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) targetClass.cast(target);
            throw new IndexOutOfBoundsException("Index: " + (((Double) index).doubleValue() > 0 ? "" : "-") + "Infinity, Size: " + list.size());
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) targetClass.cast(target);
            Boundaries.listSet(list, index, value);
        }
    }

    private static class JavaArrayWriteElementTypeCacheNode extends ObjectWriteElementTypeCacheNode {
        @Child private ToArrayIndexNode toArrayIndexNode;
        @Child private JSToNumberNode valueToNumberNode;
        @Child private JSToStringNode valueToStringNode;
        @Child private JSToBooleanNode valueToBooleanNode;

        JavaArrayWriteElementTypeCacheNode(JSContext context, boolean isStrict, Class<?> targetClass, boolean writeOwn) {
            super(context, isStrict, targetClass, writeOwn);
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (convertedIndex instanceof Long && ((Long) convertedIndex).intValue() >= 0) {
                convertAndSetJavaArray(target, ((Long) convertedIndex).intValue(), value);
            } else {
                super.executeWithTargetAndIndexUnguarded(target, index, value);
            }
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            convertAndSetJavaArray(target, index, value);
        }

        private Number valueToNumber(Object target) {
            if (valueToNumberNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                valueToNumberNode = insert(JSToNumberNode.create());
            }
            return valueToNumberNode.executeNumber(target);
        }

        private String valueToString(Object target) {
            if (valueToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                valueToStringNode = insert(JSToStringNode.create());
            }
            return valueToStringNode.executeString(target);
        }

        private boolean valueToBoolean(Object target) {
            if (valueToBooleanNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                valueToBooleanNode = insert(JSToBooleanNode.create());
            }
            return valueToBooleanNode.executeBoolean(target);
        }

        private void convertAndSetJavaArray(Object array, int index, Object value) {
            Object valueConverted;
            valueConverted = convertIntl(array, value);
            setJavaArray(array, index, valueConverted);
        }

        private Object convertIntl(Object array, Object value) {
            if (array instanceof int[] || array instanceof Integer[]) {
                return JSRuntime.intValue(valueToNumber(value));
            } else if (array instanceof long[] || array instanceof Long[]) {
                return JSRuntime.longValue(valueToNumber(value));
            } else if (array instanceof byte[] || array instanceof Byte[]) {
                return valueToNumber(value).byteValue();
            } else if (array instanceof double[] || array instanceof Double[]) {
                return JSRuntime.doubleValue(valueToNumber(value));
            } else if (array instanceof boolean[] || array instanceof Boolean[]) {
                return valueToBoolean(value);
            } else if (array instanceof char[] || array instanceof Character[]) {
                if (value instanceof Number) {
                    int nr = ((Number) value).intValue();
                    if (nr >= 0 && nr <= 65535) {
                        return (char) nr;
                    } else {
                        throw Errors.createTypeError("Cannot convert number to character; it is out of 0-65535 range", this);
                    }
                } else {
                    String str = valueToString(value);
                    if (str.length() == 1) {
                        return str.charAt(0);
                    } else {
                        throw Errors.createTypeError("Cannot convert string to character; its length must be exactly 1", this);
                    }
                }
            } else if (array instanceof short[] || array instanceof Short[]) {
                return valueToNumber(value).shortValue();
            } else if (array instanceof float[] || array instanceof Float[]) {
                return JSRuntime.floatValue(valueToNumber(value));
            } else if (array instanceof String[]) {
                return valueToString(value);
            } else {
                return value == Null.instance ? null : (JSRuntime.isLazyString(value) ? Boundaries.javaToString(value) : value);
            }
        }

        private static void setJavaArray(Object array, int index, Object value) {
            // see GR-4172
            if (index >= 0 && index < Array.getLength(array)) {
                Array.set(array, index, value);
            }
        }
    }

    private abstract static class ArrayWriteElementCacheNode extends WriteElementCacheNode {
        ArrayWriteElementCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
        }

        protected abstract void executeWithTargetAndArrayAndIndexAndValue(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition);
    }

    private static class UninitArrayWriteElementCacheNode extends ArrayWriteElementCacheNode {
        private final boolean recursive;

        UninitArrayWriteElementCacheNode(JSContext context, boolean isStrict, boolean writeOwn, boolean recursive) {
            super(context, isStrict, writeOwn);
            this.recursive = recursive;
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValue(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayWriteElementCacheNode selection;
            if (!JSSlowArray.isJSSlowArray(target) && !JSSlowArgumentsObject.isJSSlowArgumentsObject(target)) {
                selection = getSelection(array);
            } else {
                selection = new ExactArrayWriteElementCacheNode(context, isStrict, array, writeOwn, this);
            }
            purgeStaleCacheEntries(target);
            this.replace(selection);
            selection.executeWithTargetAndArrayAndIndexAndValue(target, array, index, value, false);
        }

        private ArrayWriteElementCacheNode getSelection(ScriptArray array) {
            UninitArrayWriteElementCacheNode next = this;
            if (array.isLengthNotWritable() || !array.isExtensible()) {
                // TODO handle this case in the specializations below
                return new ExactArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            }
            if (array instanceof LazyRegexResultArray) {
                return new LazyRegexResultArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof AbstractConstantArray) {
                return new ConstantArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof HolesIntArray) {
                return new HolesIntArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof HolesDoubleArray) {
                return new HolesDoubleArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof HolesJSObjectArray) {
                return new HolesJSObjectArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof HolesObjectArray) {
                return new HolesObjectArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof AbstractIntArray) {
                return new IntArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof AbstractDoubleArray) {
                return new DoubleArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof AbstractObjectArray) {
                return new ObjectArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof AbstractJSObjectArray) {
                return new JSObjectArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof AbstractWritableArray) {
                return new WritableArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            } else if (array instanceof TypedArray) {
                if (array instanceof TypedArray.AbstractUint32Array) {
                    return new Uint32ArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
                } else if (array instanceof TypedArray.AbstractUint8ClampedArray) {
                    return new Uint8ClampedArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
                } else if (array instanceof TypedIntArray) {
                    return new TypedIntArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
                } else if (array instanceof TypedFloatArray) {
                    return new TypedFloatArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
                } else {
                    return new TypedArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
                }
            } else {
                return new ExactArrayWriteElementCacheNode(context, isStrict, array, writeOwn, next);
            }
        }

        private void purgeStaleCacheEntries(DynamicObject target) {
            if (JSTruffleOptions.TrackArrayAllocationSites && !recursive && this.getParent() instanceof ConstantArrayWriteElementCacheNode && JSArray.isJSArray(target)) {
                ArrayAllocationSite allocationSite = arrayGetAllocationSite(target);
                if (allocationSite != null && allocationSite.getInitialArrayType() != null) {
                    ScriptArray initialArrayType = allocationSite.getInitialArrayType();
                    ConstantArrayWriteElementCacheNode existingNode = (ConstantArrayWriteElementCacheNode) this.getParent();
                    if (!(initialArrayType instanceof ConstantEmptyArray) && existingNode.getArrayType() instanceof ConstantEmptyArray) {
                        // allocation site has been patched to not create an empty array;
                        // purge existing empty array specialization in cache
                        if (JSTruffleOptions.TraceArrayTransitions) {
                            System.out.println("purging " + existingNode + arrayGetArrayType(target));
                        }
                        existingNode.purge();
                    }
                }
            }
        }
    }

    private abstract static class CachedArrayWriteElementCacheNode extends ArrayWriteElementCacheNode {
        @Child private ArrayWriteElementCacheNode arrayCacheNext;

        CachedArrayWriteElementCacheNode(JSContext context, boolean isStrict, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, writeOwn);
            this.arrayCacheNext = arrayCacheNext;

        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValue(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            if (guard(target, array)) {
                executeWithTargetAndArrayAndIndexAndValueUnguarded(target, array, index, value, arrayCondition);
            } else {
                arrayCacheNext.executeWithTargetAndArrayAndIndexAndValue(target, array, index, value, arrayCondition);
            }
        }

        protected abstract void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition);

        protected abstract boolean guard(Object target, ScriptArray array);

        protected final void purge() {
            this.replace(arrayCacheNext);
        }
    }

    private abstract static class ArrayClassGuardCachedArrayWriteElementCacheNode extends CachedArrayWriteElementCacheNode {
        private final ScriptArray arrayType;

        ArrayClassGuardCachedArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, writeOwn, arrayCacheNext);
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

        protected void checkDetachedArrayBuffer(DynamicObject target) {
            if (JSArrayBufferView.hasDetachedBuffer(target, context)) {
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }
    }

    private abstract static class RecursiveCachedArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        @Child private ArrayWriteElementCacheNode recursiveWrite;
        private final BranchProfile needPrototypeBranch = BranchProfile.create();

        RecursiveCachedArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        protected final void setArrayAndWrite(ScriptArray newArray, DynamicObject target, long index, Object value, boolean arrayCondition) {
            arraySetArrayType(target, newArray);
            if (recursiveWrite == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.recursiveWrite = insert(new UninitArrayWriteElementCacheNode(context, isStrict, writeOwn, true));
            }
            recursiveWrite.executeWithTargetAndArrayAndIndexAndValue(target, newArray, index, value, arrayCondition);
        }

        protected final boolean nonHolesArrayNeedsSlowSet(DynamicObject target, AbstractWritableArray arrayType, long index, boolean arrayCondition) {
            assert !arrayType.isHolesType();
            if (!context.getArrayPrototypeNoElementsAssumption().isValid() && !writeOwn) {
                if (!arrayType.hasElement(target, index, arrayCondition) && JSObject.hasProperty(target, index)) {
                    needPrototypeBranch.enter();
                    return true;
                }
            }
            return false;
        }

        protected final boolean holesArrayNeedsSlowSet(DynamicObject target, AbstractWritableArray arrayType, long index, boolean arrayCondition) {
            assert arrayType.isHolesType();
            if ((!context.getArrayPrototypeNoElementsAssumption().isValid() && !writeOwn) ||
                            (!context.getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(target)) ||
                            (!context.getFastArgumentsObjectAssumption().isValid() && JSSlowArgumentsObject.isJSSlowArgumentsObject(target))) {
                if (!arrayType.hasElement(target, index, arrayCondition) && JSObject.hasProperty(target, index)) {
                    needPrototypeBranch.enter();
                    return true;
                }
            }
            return false;
        }
    }

    private static class ExactArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        protected final JSClassProfile classProfile = JSClassProfile.create();

        ExactArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            // target.setArray(arrayType.cast(array).setElement(index, value,
            // isStrict));
            JSObject.set(target, index, value, isStrict, classProfile);
        }
    }

    private static class LazyRegexResultArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {

        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        @Child private TRegexUtil.TRegexMaterializeResultNode materializeResultNode = TRegexUtil.TRegexMaterializeResultNode.create();

        LazyRegexResultArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            LazyRegexResultArray lazyRegexResultArray = (LazyRegexResultArray) cast(array);
            ScriptArray newArray = lazyRegexResultArray.createWritable(materializeResultNode, target, index, value);
            if (inBoundsProfile.profile(index >= 0 && index < 0x7fff_ffff)) {
                setArrayAndWrite(newArray, target, index, value, arrayCondition);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, newArray).setElement(target, index, value, isStrict, arrayCondition));
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

        ConstantArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            AbstractConstantArray constantArray = (AbstractConstantArray) cast(array);
            if (inBoundsProfile.profile(index >= 0 && index < 0x7fff_ffff)) {
                ScriptArray newArray;
                if (value instanceof Integer) {
                    inBoundsIntBranch.enter();
                    newArray = constantArray.createWriteableInt(target, index, (int) value, createWritableProfile);
                } else if (value instanceof Double) {
                    inBoundsDoubleBranch.enter();
                    newArray = constantArray.createWriteableDouble(target, index, (double) value, createWritableProfile);
                } else if (JSObject.isDynamicObject(value)) {
                    inBoundsJSObjectBranch.enter();
                    newArray = constantArray.createWriteableJSObject(target, index, (DynamicObject) value, createWritableProfile);
                } else {
                    inBoundsObjectBranch.enter();
                    newArray = constantArray.createWriteableObject(target, index, value, createWritableProfile);
                }
                setArrayAndWrite(newArray, target, index, value, arrayCondition);
            } else {
                arraySetArrayType(target, SparseArray.makeSparseArray(target, array).setElement(target, index, value, isStrict, arrayCondition));
            }
        }
    }

    private static class WritableArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();
        protected final JSClassProfile classProfile = JSClassProfile.create();

        WritableArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBoundsProfile.profile(writableArray.isInBoundsFast(target, index, arrayCondition))) {
                // TODO writableArray.setInBoundsFast(intIndex);
                arraySetArrayType(target, writableArray.setElement(target, index, value, isStrict, arrayCondition));
            } else {
                JSObject.set(target, index, value, isStrict, classProfile);
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

        IntArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            AbstractIntArray intArray = (AbstractIntArray) cast(array);
            if (value instanceof Integer) {
                intValueBranch.enter();
                executeWithTargetAndArrayAndIndexAndIntValueInner(target, intArray, index, (int) value, arrayCondition);
            } else if (value instanceof Double) {
                toDoubleBranch.enter();
                double doubleValue = (double) value;
                setArrayAndWrite(intArray.toDouble(target, index, doubleValue, arrayCondition), target, index, doubleValue, arrayCondition);
            } else {
                toObjectBranch.enter();
                setArrayAndWrite(intArray.toObject(target, index, value, arrayCondition), target, index, value, arrayCondition);
            }
        }

        private void executeWithTargetAndArrayAndIndexAndIntValueInner(DynamicObject target, AbstractIntArray intArray, long index, int intValue, boolean arrayCondition) {
            assert !(intArray instanceof HolesIntArray);
            if (nonHolesArrayNeedsSlowSet(target, intArray, index, arrayCondition)) {
                JSObject.set(target, index, (Object) intValue, isStrict);
                return;
            }
            int iIndex = (int) index;
            if (inBoundsFastCondition.profile(intArray.isInBoundsFast(target, index, arrayCondition) && !mightTransferToNonContiguous(intArray, index))) {
                intArray.setInBoundsFast(target, iIndex, intValue, arrayCondition);
            } else if (inBoundsCondition.profile(intArray.isInBounds(target, iIndex, arrayCondition) && !mightTransferToNonContiguous(intArray, index))) {
                intArray.setInBounds(target, iIndex, intValue, arrayCondition, profile);
            } else {
                if (supportedNonZeroCondition.profile(intArray.isSupported(target, index, arrayCondition) && !mightTransferToNonContiguous(intArray, index))) {
                    intArray.setSupported(target, iIndex, intValue, arrayCondition, profile);
                } else if (supportedZeroCondition.profile(mightTransferToNonContiguous(intArray, index) && intArray.isSupported(target, index, arrayCondition))) {
                    setArrayAndWrite(intArray.toNonContiguous(target, iIndex, intValue, arrayCondition, profile), target, index, intValue, arrayCondition);
                } else if (supportedContiguousCondition.profile(!(intArray instanceof AbstractContiguousIntArray) && intArray.isSupportedContiguous(target, index, arrayCondition))) {
                    setArrayAndWrite(intArray.toContiguous(target, index, intValue, arrayCondition), target, index, intValue, arrayCondition);
                } else if (supportedHolesCondition.profile(intArray.isSupportedHoles(target, index, arrayCondition))) {
                    setArrayAndWrite(intArray.toHoles(target, index, intValue, arrayCondition), target, index, intValue, arrayCondition);
                } else {
                    assert intArray.isSparse(target, index, arrayCondition);
                    setArrayAndWrite(intArray.toSparse(target, index, intValue), target, index, intValue, arrayCondition);
                }
            }
        }

        private static boolean mightTransferToNonContiguous(AbstractIntArray intArray, long index) {
            return intArray instanceof ContiguousIntArray && index == 0;
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

        DoubleArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            AbstractDoubleArray doubleArray = (AbstractDoubleArray) cast(array);
            double doubleValue = 0.0; // dummy
            if (value instanceof Double) {
                doubleValueBranch.enter();
                doubleValue = (double) value;
            } else if (value instanceof Integer) {
                intValueBranch.enter();
                doubleValue = (int) value;
            } else {
                toObjectBranch.enter();
                setArrayAndWrite(doubleArray.toObject(target, index, value, arrayCondition), target, index, value, arrayCondition);
                return;
            }
            executeWithTargetAndArrayAndIndexAndDoubleValueInner(target, doubleArray, index, doubleValue, arrayCondition);
        }

        private void executeWithTargetAndArrayAndIndexAndDoubleValueInner(DynamicObject target, AbstractDoubleArray doubleArray, long index, double doubleValue,
                        boolean arrayCondition) {
            assert !(doubleArray instanceof HolesDoubleArray);
            if (nonHolesArrayNeedsSlowSet(target, doubleArray, index, arrayCondition)) {
                JSObject.set(target, index, (Object) doubleValue, isStrict);
                return;
            }
            int iIndex = (int) index;
            if (inBoundsFastCondition.profile(doubleArray.isInBoundsFast(target, index, arrayCondition))) {
                doubleArray.setInBoundsFast(target, iIndex, doubleValue, arrayCondition);
            } else if (inBoundsCondition.profile(doubleArray.isInBounds(target, iIndex, arrayCondition))) {
                doubleArray.setInBounds(target, iIndex, doubleValue, arrayCondition, profile);
            } else {
                if (supportedCondition.profile(doubleArray.isSupported(target, index, arrayCondition))) {
                    doubleArray.setSupported(target, iIndex, doubleValue, arrayCondition, profile);
                } else if (supportedContiguousCondition.profile(!(doubleArray instanceof AbstractContiguousDoubleArray) && doubleArray.isSupportedContiguous(target, index, arrayCondition))) {
                    setArrayAndWrite(doubleArray.toContiguous(target, index, doubleValue, arrayCondition), target, index, doubleValue, arrayCondition);
                } else if (supportedHolesCondition.profile(doubleArray.isSupportedHoles(target, index, arrayCondition))) {
                    setArrayAndWrite(doubleArray.toHoles(target, index, doubleValue, arrayCondition), target, index, doubleValue, arrayCondition);
                } else {
                    assert doubleArray.isSparse(target, index, arrayCondition);
                    setArrayAndWrite(doubleArray.toSparse(target, index, doubleValue), target, index, doubleValue, arrayCondition);
                }
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

        ObjectArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            AbstractObjectArray objectArray = (AbstractObjectArray) cast(array);
            executeWithTargetAndArrayAndIndexAndValueInner(target, objectArray, index, value, arrayCondition);
        }

        private void executeWithTargetAndArrayAndIndexAndValueInner(DynamicObject target, AbstractObjectArray objectArray, long index, Object value, boolean arrayCondition) {
            assert !(objectArray instanceof HolesObjectArray);
            if (nonHolesArrayNeedsSlowSet(target, objectArray, index, arrayCondition)) {
                JSObject.set(target, index, value, isStrict);
                return;
            }
            int iIndex = (int) index;
            if (inBoundsFastCondition.profile(objectArray.isInBoundsFast(target, index, arrayCondition))) {
                objectArray.setInBoundsFast(target, iIndex, value, arrayCondition);
            } else if (inBoundsCondition.profile(objectArray.isInBounds(target, iIndex, arrayCondition))) {
                objectArray.setInBounds(target, iIndex, value, arrayCondition, profile);
            } else if (supportedCondition.profile(objectArray.isSupported(target, index, arrayCondition))) {
                objectArray.setSupported(target, iIndex, value, arrayCondition);
            } else if (supportedContiguousCondition.profile(!(objectArray instanceof AbstractContiguousObjectArray) && objectArray.isSupportedContiguous(target, index, arrayCondition))) {
                setArrayAndWrite(objectArray.toContiguous(target, index, value, arrayCondition), target, index, value, arrayCondition);
            } else if (supportedHolesCondition.profile(objectArray.isSupportedHoles(target, index, arrayCondition))) {
                setArrayAndWrite(objectArray.toHoles(target, index, value, arrayCondition), target, index, value, arrayCondition);
            } else {
                assert objectArray.isSparse(target, index, arrayCondition) : objectArray.getClass() + " " + objectArray.firstElementIndex(target) + "-" + objectArray.lastElementIndex(target) + " / " +
                                index;
                setArrayAndWrite(objectArray.toSparse(target, index, value), target, index, value, arrayCondition);
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

        JSObjectArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            AbstractJSObjectArray jsobjectArray = (AbstractJSObjectArray) cast(array);
            if (objectType.profile(JSObject.isDynamicObject(value))) {
                DynamicObject jsobjectValue = (DynamicObject) value;
                executeWithTargetAndArrayAndIndexAndJSObjectValueInner(target, jsobjectArray, index, jsobjectValue, arrayCondition);
            } else {
                setArrayAndWrite(jsobjectArray.toObject(target, index, value, arrayCondition), target, index, value, arrayCondition);
            }
        }

        private void executeWithTargetAndArrayAndIndexAndJSObjectValueInner(DynamicObject target, AbstractJSObjectArray jsobjectArray, long index, DynamicObject jsobjectValue,
                        boolean arrayCondition) {
            assert !(jsobjectArray instanceof HolesJSObjectArray);
            int iIndex = (int) index;
            if (nonHolesArrayNeedsSlowSet(target, jsobjectArray, index, arrayCondition)) {
                JSObject.set(target, index, jsobjectValue, isStrict);
                return;
            }
            if (inBoundsFastCondition.profile(jsobjectArray.isInBoundsFast(target, index, arrayCondition))) {
                jsobjectArray.setInBoundsFast(target, iIndex, jsobjectValue, arrayCondition);
            } else if (inBoundsCondition.profile(jsobjectArray.isInBounds(target, iIndex, arrayCondition))) {
                jsobjectArray.setInBounds(target, iIndex, jsobjectValue, arrayCondition, profile);
            } else if (supportedCondition.profile(jsobjectArray.isSupported(target, index, arrayCondition))) {
                jsobjectArray.setSupported(target, iIndex, jsobjectValue, arrayCondition, profile);
            } else if (supportedContiguousCondition.profile(!(jsobjectArray instanceof AbstractContiguousJSObjectArray) && jsobjectArray.isSupportedContiguous(target, index, arrayCondition))) {
                setArrayAndWrite(jsobjectArray.toContiguous(target, index, jsobjectValue, arrayCondition), target, index, jsobjectValue, arrayCondition);
            } else if (supportedHolesCondition.profile(jsobjectArray.isSupportedHoles(target, index, arrayCondition))) {
                setArrayAndWrite(jsobjectArray.toHoles(target, index, jsobjectValue, arrayCondition), target, index, jsobjectValue, arrayCondition);
            } else {
                assert jsobjectArray.isSparse(target, index, arrayCondition);
                setArrayAndWrite(jsobjectArray.toSparse(target, index, jsobjectValue), target, index, jsobjectValue, arrayCondition);
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

        HolesIntArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            HolesIntArray holesIntArray = (HolesIntArray) cast(array);
            if (value instanceof Integer) {
                intValueBranch.enter();
                int intValue = (int) value;
                executeWithTargetAndArrayAndIndexAndIntValueInner(target, holesIntArray, index, intValue, arrayCondition);
            } else if (value instanceof Double) {
                toDoubleBranch.enter();
                double doubleValue = (double) value;
                setArrayAndWrite(holesIntArray.toDouble(target, index, doubleValue, arrayCondition), target, index, doubleValue, arrayCondition);
            } else {
                toObjectBranch.enter();
                setArrayAndWrite(holesIntArray.toObject(target, index, value, arrayCondition), target, index, value, arrayCondition);
            }
        }

        private void executeWithTargetAndArrayAndIndexAndIntValueInner(DynamicObject target, HolesIntArray holesIntArray, long index, int intValue, boolean arrayCondition) {
            if (holesArrayNeedsSlowSet(target, holesIntArray, index, arrayCondition)) {
                JSObject.set(target, index, (Object) intValue, isStrict);
                return;
            }
            int iIndex = (int) index;
            boolean containsHoles = containsHolesProfile.profile(containsHoles(target, holesIntArray, index, arrayCondition));
            if (containsHoles && inBoundsFastCondition.profile(holesIntArray.isInBoundsFast(target, index, arrayCondition) && !HolesIntArray.isHoleValue(intValue))) {
                if (inBoundsFastHoleCondition.profile(holesIntArray.isHoleFast(target, iIndex, arrayCondition))) {
                    holesIntArray.setInBoundsFastHole(target, iIndex, intValue, arrayCondition);
                } else {
                    holesIntArray.setInBoundsFastNonHole(target, iIndex, intValue, arrayCondition);
                }
            } else if (containsHoles && inBoundsCondition.profile(holesIntArray.isInBounds(target, iIndex, arrayCondition) && !HolesIntArray.isHoleValue(intValue))) {
                holesIntArray.setInBounds(target, iIndex, intValue, arrayCondition, profile);
            } else if (containsHoles && supportedContainsHolesCondition.profile(holesIntArray.isSupported(target, index, arrayCondition) && !HolesIntArray.isHoleValue(intValue))) {
                holesIntArray.setSupported(target, iIndex, intValue, arrayCondition, profile);
            } else if (!containsHoles && supportedNotContainsHolesCondition.profile(holesIntArray.isSupported(target, index, arrayCondition))) {
                setArrayAndWrite(holesIntArray.toNonHoles(target, index, intValue, arrayCondition), target, index, intValue, arrayCondition);
            } else {
                assert holesIntArray.isSparse(target, index, arrayCondition);
                setArrayAndWrite(holesIntArray.toSparse(target, index, intValue), target, index, intValue, arrayCondition);
            }
        }

        private boolean containsHoles(DynamicObject target, HolesIntArray holesIntArray, long index, boolean condition) {
            return hasExplicitHolesProfile.profile(JSArray.arrayGetHoleCount(target, condition) > 0) || !holesIntArray.isInBoundsFast(target, index, condition);
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

        HolesDoubleArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            HolesDoubleArray holesDoubleArray = (HolesDoubleArray) cast(array);
            double doubleValue = 0.0; // dummy
            if (value instanceof Double) {
                doubleValueBranch.enter();
                doubleValue = (double) value;
            } else if (value instanceof Integer) {
                intValueBranch.enter();
                doubleValue = (int) value;
            } else {
                toObjectBranch.enter();
                setArrayAndWrite(holesDoubleArray.toObject(target, index, value, arrayCondition), target, index, value, arrayCondition);
                return;
            }

            executeWithTargetAndArrayAndIndexAndIntValueInner(target, holesDoubleArray, index, doubleValue, arrayCondition);
        }

        private void executeWithTargetAndArrayAndIndexAndIntValueInner(DynamicObject target, HolesDoubleArray holesDoubleArray, long index, double doubleValue,
                        boolean arrayCondition) {
            if (holesArrayNeedsSlowSet(target, holesDoubleArray, index, arrayCondition)) {
                JSObject.set(target, index, (Object) doubleValue, isStrict);
                return;
            }
            int iIndex = (int) index;
            boolean containsHoles = containsHolesProfile.profile(containsHoles(target, holesDoubleArray, index, arrayCondition));
            if (containsHoles && inBoundsFastCondition.profile(holesDoubleArray.isInBoundsFast(target, index, arrayCondition) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                if (inBoundsFastHoleCondition.profile(holesDoubleArray.isHoleFast(target, iIndex, arrayCondition))) {
                    holesDoubleArray.setInBoundsFastHole(target, iIndex, doubleValue, arrayCondition);
                } else {
                    holesDoubleArray.setInBoundsFastNonHole(target, iIndex, doubleValue, arrayCondition);
                }
            } else if (containsHoles && inBoundsCondition.profile(holesDoubleArray.isInBounds(target, iIndex, arrayCondition) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                holesDoubleArray.setInBounds(target, iIndex, doubleValue, arrayCondition, profile);
            } else if (containsHoles && supportedContainsHolesCondition.profile(holesDoubleArray.isSupported(target, index, arrayCondition) && !HolesDoubleArray.isHoleValue(doubleValue))) {
                holesDoubleArray.setSupported(target, iIndex, doubleValue, arrayCondition, profile);
            } else if (!containsHoles && supportedNotContainsHolesCondition.profile(holesDoubleArray.isSupported(target, index, arrayCondition))) {
                setArrayAndWrite(holesDoubleArray.toNonHoles(target, index, doubleValue, arrayCondition), target, index, doubleValue, arrayCondition);
            } else {
                assert holesDoubleArray.isSparse(target, index, arrayCondition);
                setArrayAndWrite(holesDoubleArray.toSparse(target, index, doubleValue), target, index, doubleValue, arrayCondition);
            }
        }

        private boolean containsHoles(DynamicObject target, HolesDoubleArray holesDoubleArray, long index, boolean condition) {
            return hasExplicitHolesProfile.profile(JSArray.arrayGetHoleCount(target, condition) > 0) || !holesDoubleArray.isInBoundsFast(target, index, condition);
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

        HolesJSObjectArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
            assert arrayType.getClass() == HolesJSObjectArray.class;
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            HolesJSObjectArray holesArray = (HolesJSObjectArray) cast(array);
            if (objectType.profile(JSObject.isDynamicObject(value))) {
                executeWithTargetAndArrayAndIndexAndJSObjectValueInner(target, holesArray, index, (DynamicObject) value, arrayCondition);
            } else {
                setArrayAndWrite(holesArray.toObject(target, index, value, arrayCondition), target, index, value, arrayCondition);
            }
        }

        private void executeWithTargetAndArrayAndIndexAndJSObjectValueInner(DynamicObject target, HolesJSObjectArray jsobjectArray, long index, DynamicObject value,
                        boolean arrayCondition) {
            if (holesArrayNeedsSlowSet(target, jsobjectArray, index, arrayCondition)) {
                JSObject.set(target, index, value, isStrict);
                return;
            }
            boolean containsHoles = containsHolesProfile.profile(containsHoles(target, jsobjectArray, index, arrayCondition));
            if (containsHoles && inBoundsFastCondition.profile(jsobjectArray.isInBoundsFast(target, index, arrayCondition))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                if (inBoundsFastHoleCondition.profile(jsobjectArray.isHoleFast(target, (int) index, arrayCondition))) {
                    jsobjectArray.setInBoundsFastHole(target, (int) index, value, arrayCondition);
                } else {
                    jsobjectArray.setInBoundsFastNonHole(target, (int) index, value, arrayCondition);
                }
            } else if (containsHoles && inBoundsCondition.profile(jsobjectArray.isInBounds(target, (int) index, arrayCondition))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                jsobjectArray.setInBounds(target, (int) index, value, arrayCondition, profile);
            } else if (containsHoles && supportedContainsHolesCondition.profile(jsobjectArray.isSupported(target, index, arrayCondition))) {
                assert !HolesJSObjectArray.isHoleValue(value);
                jsobjectArray.setSupported(target, (int) index, value, arrayCondition, profile);
            } else if (!containsHoles && supportedNotContainsHolesCondition.profile(jsobjectArray.isSupported(target, index, arrayCondition))) {
                setArrayAndWrite(jsobjectArray.toNonHoles(target, index, value, arrayCondition), target, index, value, arrayCondition);
            } else {
                assert jsobjectArray.isSparse(target, index, arrayCondition);
                setArrayAndWrite(jsobjectArray.toSparse(target, index, value), target, index, value, arrayCondition);
            }
        }

        private boolean containsHoles(DynamicObject target, HolesJSObjectArray holesJSObjectArray, long index, boolean condition) {
            return hasExplicitHolesProfile.profile(JSArray.arrayGetHoleCount(target, condition) > 0) || !holesJSObjectArray.isInBoundsFast(target, index, condition);
        }
    }

    private static class HolesObjectArrayWriteElementCacheNode extends RecursiveCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsFastCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsFastHoleCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile inBoundsCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile supportedCondition = ConditionProfile.createBinaryProfile();
        private final ScriptArray.ProfileHolder profile = AbstractWritableArray.createSetSupportedProfile();

        HolesObjectArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
            assert arrayType.getClass() == HolesObjectArray.class;
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            executeWithTargetAndArrayAndIndexAndValueInner(target, (HolesObjectArray) array, index, value, arrayCondition);
        }

        private void executeWithTargetAndArrayAndIndexAndValueInner(DynamicObject target, HolesObjectArray objectArray, long index, Object value, boolean arrayCondition) {
            if (holesArrayNeedsSlowSet(target, objectArray, index, arrayCondition)) {
                JSObject.set(target, index, value, isStrict);
                return;
            }
            if (inBoundsFastCondition.profile(objectArray.isInBoundsFast(target, index, arrayCondition))) {
                assert !HolesObjectArray.isHoleValue(value);
                if (inBoundsFastHoleCondition.profile(objectArray.isHoleFast(target, (int) index, arrayCondition))) {
                    objectArray.setInBoundsFastHole(target, (int) index, value, arrayCondition);
                } else {
                    objectArray.setInBoundsFastNonHole(target, (int) index, value, arrayCondition);
                }
            } else if (inBoundsCondition.profile(objectArray.isInBounds(target, (int) index, arrayCondition))) {
                assert !HolesObjectArray.isHoleValue(value);
                objectArray.setInBounds(target, (int) index, value, arrayCondition, profile);
            } else if (supportedCondition.profile(objectArray.isSupported(target, index, arrayCondition))) {
                assert !HolesObjectArray.isHoleValue(value);
                objectArray.setSupported(target, (int) index, value, arrayCondition);
            } else {
                assert objectArray.isSparse(target, index, arrayCondition);
                setArrayAndWrite(objectArray.toSparse(target, index, value), target, index, value, arrayCondition);
            }
        }
    }

    private static class TypedArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        TypedArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            checkDetachedArrayBuffer(target);
            TypedArray typedArray = (TypedArray) cast(array);
            if (inBoundsProfile.profile(typedArray.hasElement(target, index, arrayCondition))) {
                typedArray.setElement(target, index, value, isStrict, arrayCondition);
            } else {
                // do nothing; cf. ES6 9.4.5.9 IntegerIndexedElementSet(O, index, value)
            }
        }
    }

    private abstract static class AbstractTypedIntArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();

        AbstractTypedIntArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected final void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            TypedIntArray<?> typedArray = (TypedIntArray<?>) cast(array);
            int iValue = toInt(value); // could throw
            checkDetachedArrayBuffer(target);
            if (inBoundsProfile.profile(typedArray.hasElement(target, index, arrayCondition))) {
                typedArray.setInt(target, (int) index, iValue, arrayCondition);
            } else {
                // do nothing; cf. ES6 9.4.5.9 IntegerIndexedElementSet(O, index, value)
            }
        }

        protected abstract int toInt(Object value);
    }

    private static class TypedIntArrayWriteElementCacheNode extends AbstractTypedIntArrayWriteElementCacheNode {
        @Child private JSToInt32Node toIntNode;

        TypedIntArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
            this.toIntNode = JSToInt32Node.create();
        }

        @Override
        protected int toInt(Object value) {
            return toIntNode.executeInt(value);
        }
    }

    private static class Uint8ClampedArrayWriteElementCacheNode extends AbstractTypedIntArrayWriteElementCacheNode {
        private final ConditionProfile toInt = ConditionProfile.createBinaryProfile();
        @Child private JSToDoubleNode toDoubleNode;

        Uint8ClampedArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected int toInt(Object value) {
            if (toInt.profile(value instanceof Integer)) {
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
        private final ConditionProfile toInt = ConditionProfile.createBinaryProfile();
        @Child private JSToNumberNode toNumberNode;

        Uint32ArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
        }

        @Override
        protected int toInt(Object value) {
            if (toInt.profile(value instanceof Integer)) {
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

    private static class TypedFloatArrayWriteElementCacheNode extends ArrayClassGuardCachedArrayWriteElementCacheNode {
        private final ConditionProfile inBoundsProfile = ConditionProfile.createBinaryProfile();
        @Child private JSToDoubleNode toDoubleNode;

        TypedFloatArrayWriteElementCacheNode(JSContext context, boolean isStrict, ScriptArray arrayType, boolean writeOwn, ArrayWriteElementCacheNode arrayCacheNext) {
            super(context, isStrict, arrayType, writeOwn, arrayCacheNext);
            this.toDoubleNode = JSToDoubleNode.create();
        }

        @Override
        protected void executeWithTargetAndArrayAndIndexAndValueUnguarded(DynamicObject target, ScriptArray array, long index, Object value, boolean arrayCondition) {
            TypedFloatArray<?> typedArray = (TypedFloatArray<?>) cast(array);
            double dValue = toDouble(value); // could throw
            checkDetachedArrayBuffer(target);
            if (inBoundsProfile.profile(typedArray.hasElement(target, index, arrayCondition))) {
                typedArray.setDouble(target, (int) index, dValue, arrayCondition);
            } else {
                // do nothing; cf. ES6 9.4.5.9 IntegerIndexedElementSet(O, index, value)
            }
        }

        private double toDouble(Object value) {
            return toDoubleNode.executeDouble(value);
        }
    }

    private abstract static class IndexToStringCachedWriteElementTypeCacheNode extends CachedWriteElementTypeCacheNode {
        @Child private JSToStringNode indexToStringNode;
        protected final JSClassProfile classProfile = JSClassProfile.create();

        IndexToStringCachedWriteElementTypeCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
        }

        protected final String indexToString(Object index) {
            if (indexToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                indexToStringNode = insert(JSToStringNode.create());
            }
            return indexToStringNode.executeString(index);
        }
    }

    private static class StringWriteElementTypeCacheNode extends IndexToStringCachedWriteElementTypeCacheNode {
        private final Class<?> stringClass;
        private final BranchProfile intIndexBranch = BranchProfile.create();
        private final BranchProfile stringIndexBranch = BranchProfile.create();
        private final ConditionProfile isImmutable = ConditionProfile.createBinaryProfile();

        StringWriteElementTypeCacheNode(JSContext context, boolean isStrict, Class<?> stringClass, boolean writeOwn) {
            super(context, isStrict, writeOwn);
            this.stringClass = stringClass;
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            if (index instanceof Integer) {
                intIndexBranch.enter();
                int intIndex = (int) index;
                if (isImmutable.profile(intIndex >= 0 && intIndex < JSRuntime.length(charSequence))) {
                    // cannot set characters of immutable strings
                    return;
                }
            }
            stringIndexBranch.enter();
            JSObject.set(JSString.create(context, charSequence), indexToString(index), value, isStrict, classProfile);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            if (isImmutable.profile(index >= 0 && index < JSRuntime.length(charSequence))) {
                // cannot set characters of immutable strings
            } else {
                JSObject.set(JSString.create(context, charSequence), index, value, isStrict, classProfile);
            }
        }

        @Override
        public boolean guard(Object target) {
            return stringClass.isInstance(target);
        }
    }

    private static class NumberWriteElementTypeCacheNode extends IndexToStringCachedWriteElementTypeCacheNode {
        private final Class<?> numberClass;

        NumberWriteElementTypeCacheNode(JSContext context, boolean isStrict, Class<?> stringClass, boolean writeOwn) {
            super(context, isStrict, writeOwn);
            this.numberClass = stringClass;
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            Number charSequence = (Number) target;
            JSObject.set(JSNumber.create(context, charSequence), indexToString(index), value, isStrict, classProfile);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            Number charSequence = (Number) target;
            JSObject.set(JSNumber.create(context, charSequence), index, value, isStrict, classProfile);
        }

        @Override
        public boolean guard(Object target) {
            return numberClass.isInstance(target);
        }
    }

    private static class BooleanWriteElementTypeCacheNode extends IndexToStringCachedWriteElementTypeCacheNode {
        BooleanWriteElementTypeCacheNode(JSContext context, boolean isStrict, boolean writeOwn) {
            super(context, isStrict, writeOwn);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            Boolean bool = (Boolean) target;
            JSObject.set(JSBoolean.create(context, bool), indexToString(index), value, isStrict, classProfile);
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            Boolean bool = (Boolean) target;
            JSObject.set(JSBoolean.create(context, bool), index, value, isStrict, classProfile);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Boolean;
        }
    }

    private static class TruffleObjectWriteElementTypeCacheNode extends CachedWriteElementTypeCacheNode {
        private final Class<? extends TruffleObject> targetClass;
        @Child private Node foreignArrayAccess;

        TruffleObjectWriteElementTypeCacheNode(JSContext context, boolean isStrict, Class<? extends TruffleObject> targetClass, boolean writeOwn) {
            super(context, isStrict, writeOwn);
            this.targetClass = targetClass;
            this.foreignArrayAccess = Message.WRITE.createNode();
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, Object index, Object value) {
            try {
                ForeignAccess.sendWrite(foreignArrayAccess, targetClass.cast(target), index, value);
            } catch (UnknownIdentifierException | UnsupportedTypeException | UnsupportedMessageException e) {
                // do nothing
            }
        }

        @Override
        protected void executeWithTargetAndIndexUnguarded(Object target, int index, Object value) {
            executeWithTargetAndIndexUnguarded(target, (Object) index, value);
        }

        @Override
        public boolean guard(Object target) {
            return targetClass.isInstance(target);
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
        return typeCacheNode.context;
    }

    public boolean isStrict() {
        return typeCacheNode.isStrict;
    }

    public boolean writeOwn() {
        return typeCacheNode.writeOwn;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(targetNode), cloneUninitialized(indexNode), cloneUninitialized(valueNode), getContext(), isStrict(), writeOwn());
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return valueNode.isResultAlwaysOfType(clazz);
    }
}
