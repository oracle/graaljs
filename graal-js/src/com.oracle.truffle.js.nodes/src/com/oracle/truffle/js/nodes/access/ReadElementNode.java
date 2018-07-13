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

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNodeGen;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
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
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultArray;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSlowArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSSlowArray;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyReference;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.TRegexUtil;

public class ReadElementNode extends JSTargetableNode implements ReadNode {
    @Child protected JavaScriptNode targetNode;
    @Child protected JavaScriptNode indexNode;
    @Child protected ReadElementTypeCacheNode typeCacheNode;

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
        this.typeCacheNode = new UninitReadElementTypeCacheNode(context);
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ReadElementExpressionTag.class) && materializationNeeded()) {
            JavaScriptNode clonedTarget = targetNode.hasSourceSection() ? cloneUninitialized(targetNode) : JSTaggedExecutionNode.createFor(targetNode, ExpressionTag.class);
            JavaScriptNode clonedIndex = indexNode.hasSourceSection() ? cloneUninitialized(indexNode) : JSTaggedExecutionNode.createFor(indexNode, ExpressionTag.class);
            JavaScriptNode cloned = ReadElementNode.create(clonedTarget, clonedIndex, getContext());
            transferSourceSectionAndTags(this, cloned);
            transferSourceSectionAddExpressionTag(this, clonedTarget);
            transferSourceSectionAddExpressionTag(this, clonedIndex);
            return cloned;
        }
        return this;
    }

    private boolean materializationNeeded() {
        // Materialization is needed only if we don't have source sections.
        return !(targetNode.hasSourceSection() && indexNode.hasSourceSection());
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ReadElementExpressionTag.class) {
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
                return executeWithTargetAndIndex(target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(target, index);
            }
        }
        if (indexState == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(target, e.getResult());
            }
            return executeWithTargetAndIndex(target, index);
        } else {
            assert indexState == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            return executeWithTargetAndIndex(target, index);
        }
    }

    public int executeWithTargetInt(VirtualFrame frame, Object target) throws UnexpectedResultException {
        if (indexState == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexInt(target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(target, index);
            }
        }
        if (indexState == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(target, e.getResult());
            }
            return executeWithTargetAndIndexInt(target, index);
        } else {
            assert indexState == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            return executeWithTargetAndIndexInt(target, index);
        }
    }

    public double executeWithTargetDouble(VirtualFrame frame, Object target) throws UnexpectedResultException {
        if (indexState == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = indexNode.execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexDouble(target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(target, index);
            }
        }
        if (indexState == INDEX_INT) {
            int index;
            try {
                index = indexNode.executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(target, e.getResult());
            }
            return executeWithTargetAndIndexDouble(target, index);
        } else {
            assert indexState == INDEX_OBJECT;
            Object index = indexNode.execute(frame);
            return executeWithTargetAndIndexDouble(target, index);
        }
    }

    public final Object executeWithTargetAndIndex(Object target, Object index) {
        return typeCacheNode.executeWithTargetAndIndex(target, index);
    }

    public final Object executeWithTargetAndIndex(Object target, int index) {
        return typeCacheNode.executeWithTargetAndIndex(target, index);
    }

    public final int executeWithTargetAndIndexInt(Object target, Object index) throws UnexpectedResultException {
        return typeCacheNode.executeWithTargetAndIndexInt(target, index);
    }

    public final int executeWithTargetAndIndexInt(Object target, int index) throws UnexpectedResultException {
        return typeCacheNode.executeWithTargetAndIndexInt(target, index);
    }

    public final double executeWithTargetAndIndexDouble(Object target, Object index) throws UnexpectedResultException {
        return typeCacheNode.executeWithTargetAndIndexDouble(target, index);
    }

    public final double executeWithTargetAndIndexDouble(Object target, int index) throws UnexpectedResultException {
        return typeCacheNode.executeWithTargetAndIndexDouble(target, index);
    }

    private abstract static class ReadElementCacheNode extends JavaScriptBaseNode {
        protected final JSContext context;

        protected ReadElementCacheNode(JSContext context) {
            this.context = context;
        }
    }

    private abstract static class ReadElementTypeCacheNode extends ReadElementCacheNode {
        protected ReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        public abstract Object executeWithTargetAndIndex(Object target, Object index);

        public Object executeWithTargetAndIndex(Object target, int index) {
            return executeWithTargetAndIndex(target, (Object) index);
        }

        public int executeWithTargetAndIndexInt(Object target, Object index) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndIndex(target, index));
        }

        public int executeWithTargetAndIndexInt(Object target, int index) throws UnexpectedResultException {
            return executeWithTargetAndIndexInt(target, (Object) index);
        }

        public double executeWithTargetAndIndexDouble(Object target, Object index) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndIndex(target, index));
        }

        public double executeWithTargetAndIndexDouble(Object target, int index) throws UnexpectedResultException {
            return executeWithTargetAndIndexDouble(target, (Object) index);
        }
    }

    private static class UninitReadElementTypeCacheNode extends ReadElementTypeCacheNode {
        protected UninitReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        @Override
        public Object executeWithTargetAndIndex(Object target, Object index) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            CachedReadElementTypeCacheNode specialized = makeTypeCacheNode(target);

            return this.replace(specialized).executeWithTargetAndIndex(target, index);
        }

        @SuppressWarnings("unchecked")
        private CachedReadElementTypeCacheNode makeTypeCacheNode(Object target) {
            if (JSProxy.isProxy(target)) {
                return new ProxyReadElementNode(context);
            } else if (JSObject.isJSObject(target)) {
                return new JSObjectReadElementTypeCacheNode(context);
            } else if (JSRuntime.isString(target)) {
                return new StringReadElementTypeCacheNode(context, target.getClass());
            } else if (target instanceof Boolean) {
                return new BooleanReadElementTypeCacheNode(context);
            } else if (target instanceof Number) {
                return new NumberReadElementTypeCacheNode(context, target.getClass());
            } else if (target instanceof Symbol) {
                return new SymbolReadElementTypeCacheNode(context, target.getClass());
            } else if (target instanceof TruffleObject) {
                assert !(target instanceof Symbol);
                return new TruffleObjectReadElementTypeCacheNode(context, (Class<? extends TruffleObject>) target.getClass());
            } else if (target instanceof Map) {
                return new MapReadElementTypeCacheNode(context, (Class<? extends Map<?, ?>>) target.getClass());
            } else if (target instanceof List) {
                return new ListReadElementTypeCacheNode(context, (Class<? extends List<?>>) target.getClass());
            } else if (JSGuards.isJavaArray(target)) {
                return new JavaArrayReadElementTypeCacheNode(context, target.getClass());
            } else {
                return new ObjectReadElementTypeCacheNode(context, target.getClass());
            }
        }
    }

    private static class ProxyReadElementNode extends CachedReadElementTypeCacheNode {

        @Child private JSProxyPropertyGetNode proxyGet;

        protected ProxyReadElementNode(JSContext context) {
            super(context);
            this.proxyGet = JSProxyPropertyGetNode.create(context);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            return proxyGet.executeWithReceiver(target, target, true, index);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            return proxyGet.executeWithReceiverInt(target, target, true, index);
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, int index) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(proxyGet.executeWithReceiverInt(target, target, true, index));
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, Object index) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(proxyGet.executeWithReceiver(target, target, true, index));
        }

        @Override
        public boolean guard(Object target) {
            return JSProxy.isProxy(target);
        }

    }

    private abstract static class CachedReadElementTypeCacheNode extends ReadElementTypeCacheNode {
        @Child private ReadElementTypeCacheNode typeCacheNext;

        CachedReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        @Override
        public Object executeWithTargetAndIndex(Object target, Object index) {
            if (guard(target)) {
                return executeWithTargetAndIndexUnchecked(target, index);
            } else {
                return getNext().executeWithTargetAndIndex(target, index);
            }
        }

        @Override
        public Object executeWithTargetAndIndex(Object target, int index) {
            if (guard(target)) {
                return executeWithTargetAndIndexUnchecked(target, index);
            } else {
                return getNext().executeWithTargetAndIndex(target, index);
            }
        }

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, Object index);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, int index);

        @Override
        public int executeWithTargetAndIndexInt(Object target, Object index) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedInt(target, index);
            } else {
                return getNext().executeWithTargetAndIndexInt(target, index);
            }
        }

        @Override
        public int executeWithTargetAndIndexInt(Object target, int index) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedInt(target, index);
            } else {
                return getNext().executeWithTargetAndIndexInt(target, index);
            }
        }

        protected int executeWithTargetAndIndexUncheckedInt(Object target, Object index) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndIndexUnchecked(target, index));
        }

        protected int executeWithTargetAndIndexUncheckedInt(Object target, int index) throws UnexpectedResultException {
            return executeWithTargetAndIndexUncheckedInt(target, (Object) index);
        }

        @Override
        public double executeWithTargetAndIndexDouble(Object target, Object index) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedDouble(target, index);
            } else {
                return getNext().executeWithTargetAndIndexDouble(target, index);
            }
        }

        @Override
        public double executeWithTargetAndIndexDouble(Object target, int index) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedDouble(target, index);
            } else {
                return getNext().executeWithTargetAndIndexDouble(target, index);
            }
        }

        protected double executeWithTargetAndIndexUncheckedDouble(Object target, Object index) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndIndexUnchecked(target, index));
        }

        protected double executeWithTargetAndIndexUncheckedDouble(Object target, int index) throws UnexpectedResultException {
            return executeWithTargetAndIndexUncheckedDouble(target, (Object) index);
        }

        public abstract boolean guard(Object target);

        private ReadElementTypeCacheNode getNext() {
            if (typeCacheNext == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                typeCacheNext = insert(new UninitReadElementTypeCacheNode(context));
            }
            return typeCacheNext;
        }
    }

    private static class JSObjectReadElementTypeCacheNode extends CachedReadElementTypeCacheNode {
        @Child private IsArrayNode isArrayNode;
        @Child private ToArrayIndexNode toArrayIndexNode;
        @Child private ArrayReadElementCacheNode arrayReadElementNode;
        @Child private JSObjectReadElementNonArrayTypeCacheNode nonArrayCaseNode;
        @Child private IsObjectNode isObjectNode;
        private final ConditionProfile arrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayIndexProfile = ConditionProfile.createBinaryProfile();
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        JSObjectReadElementTypeCacheNode(JSContext context) {
            super(context);
            this.isArrayNode = IsArrayNode.createIsAnyArray();
            this.toArrayIndexNode = ToArrayIndexNode.create();
            this.isObjectNode = IsObjectNode.createIncludeNullUndefined();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition && !(index instanceof HiddenKey))) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                Object objIndex = toArrayIndexNode.execute(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndex(targetObject, array, (Long) objIndex, arrayCondition);
                } else {
                    return getProperty(targetObject, objIndex);
                }
            } else {
                return readNonArrayObjectIndex(targetObject, index);
            }
        }

        private ArrayReadElementCacheNode getArrayReadElementNode() {
            if (arrayReadElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayReadElementNode = insert(ArrayReadElementCacheNode.create(context));
            }
            return arrayReadElementNode;
        }

        private Object readNonArrayObjectIndex(DynamicObject targetObject, Object index) {
            return getNonArrayNode().execute(targetObject, index);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                if (arrayIndexProfile.profile(index >= 0)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndex(targetObject, array, index, arrayCondition);
                } else {
                    return getProperty(targetObject, Boundaries.stringValueOf(index));
                }
            } else {
                return getNonArrayNode().getPropertyGeneric(targetObject, index);
            }
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, Object index) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition && !(index instanceof HiddenKey))) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                Object objIndex = toArrayIndexNode.execute(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexInt(targetObject, array, (Long) objIndex, arrayCondition);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, objIndex));
                }
            } else {
                return JSTypesGen.expectInteger(readNonArrayObjectIndex(targetObject, index));
            }
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, int index) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);

                if (arrayIndexProfile.profile(index >= 0)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexInt(targetObject, array, index, arrayCondition);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, Boundaries.stringValueOf(index)));
                }
            } else {
                return JSTypesGen.expectInteger(getNonArrayNode().getPropertyGeneric(targetObject, index));
            }
        }

        @Override
        protected double executeWithTargetAndIndexUncheckedDouble(Object target, Object index) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition && !(index instanceof HiddenKey))) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                Object objIndex = toArrayIndexNode.execute(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexDouble(targetObject, array, (Long) objIndex, arrayCondition);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, objIndex));
                }
            } else {
                return JSTypesGen.expectDouble(readNonArrayObjectIndex(targetObject, index));
            }
        }

        @Override
        protected double executeWithTargetAndIndexUncheckedDouble(Object target, int index) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);

                if (arrayIndexProfile.profile(index >= 0)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexDouble(targetObject, array, index, arrayCondition);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, Boundaries.stringValueOf(index)));
                }
            } else {
                return JSTypesGen.expectDouble(getNonArrayNode().getPropertyGeneric(targetObject, index));
            }
        }

        @Override
        public boolean guard(Object target) {
            return isObjectNode.executeBoolean(target);
        }

        private Object getProperty(DynamicObject targetObject, Object objIndex) {
            return JSObject.get(targetObject, objIndex, jsclassProfile);
        }

        private JSObjectReadElementNonArrayTypeCacheNode getNonArrayNode() {
            if (nonArrayCaseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nonArrayCaseNode = insert(new JSObjectReadElementNonArrayTypeCacheNode(context));
            }
            return nonArrayCaseNode;
        }
    }

    private static class JSObjectReadElementNonArrayTypeCacheNode extends JavaScriptBaseNode {

        private final ConditionProfile propertyReferenceProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isDataPropertyBranch = ConditionProfile.createBinaryProfile();
        @Child private CachedGetPropertyNode getPropertyCachedNode;
        private final JSContext context;

        JSObjectReadElementNonArrayTypeCacheNode(JSContext context) {
            super();
            this.context = context;
        }

        public Object execute(DynamicObject targetObject, Object index) {
            if (propertyReferenceProfile.profile(
                            index instanceof PropertyReference && ((PropertyReference) index).getDepth() == 0 && ((PropertyReference) index).getShape() == targetObject.getShape())) {
                return readPropertyReference(index, targetObject);
            } else {
                return getPropertyGeneric(targetObject, index);
            }
        }

        private Object readPropertyReference(Object index, DynamicObject targetObject) {
            Property property = ((PropertyReference) index).getProperty();
            if (isDataPropertyBranch.profile(JSProperty.isData(property))) {
                // TODO PIC for location class
                // return locationClassProfile.profileClass(property.getLocation())
                return property.getLocation().get(targetObject, false);
            } else {
                return JSProperty.getValue(property, targetObject, targetObject, false);
            }
        }

        public Object getPropertyGeneric(DynamicObject targetObject, Object index) {
            return getCachedProperty(targetObject, index);
        }

        private Object getCachedProperty(DynamicObject targetObject, Object index) {
            if (getPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getPropertyCachedNode = insert(CachedGetPropertyNode.create(context));
            }
            return getPropertyCachedNode.execute(targetObject, index);
        }
    }

    private static class ObjectReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        protected final Class<?> targetClass;

        ObjectReadElementTypeCacheNode(JSContext context, Class<?> targetClass) {
            super(context);
            this.targetClass = targetClass;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            return JSObject.get(JSJavaWrapper.create(context, target), toPropertyKey(index));
        }

        @Override
        public final boolean guard(Object target) {
            // return !(JSObject.isJSObject(target));
            return targetClass.isInstance(target);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            return executeWithTargetAndIndex(target, (Object) index);
        }
    }

    private static class MapReadElementTypeCacheNode extends ObjectReadElementTypeCacheNode {
        MapReadElementTypeCacheNode(JSContext context, Class<? extends Map<?, ?>> targetClass) {
            super(context, targetClass);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            Map<?, ?> map = (Map<?, ?>) targetClass.cast(target);
            Object key = JSRuntime.toJavaNull(index);
            Object value = Boundaries.mapGet(map, key);
            if (value == null && key instanceof CharSequence) {
                // TODO optimize this
                return super.executeWithTargetAndIndexUnchecked(target, index);
            }
            return JSRuntime.toJSNull(value);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            return executeWithTargetAndIndex(target, (Object) index);
        }
    }

    private static class ListReadElementTypeCacheNode extends ObjectReadElementTypeCacheNode {
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final ConditionProfile indexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile outOfBoundsProfile = ConditionProfile.createBinaryProfile();

        ListReadElementTypeCacheNode(JSContext context, Class<? extends List<?>> targetClass) {
            super(context, targetClass);
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (indexProfile.profile(convertedIndex instanceof Long && ((Long) convertedIndex).intValue() >= 0)) {
                List<?> list = ((List<?>) targetClass.cast(target));
                Object value = Boundaries.listGet(list, ((Long) convertedIndex).intValue());
                return JSRuntime.toJSNull(value);
            } else {
                if (outOfBoundsProfile.profile(index instanceof Double && Double.isInfinite(((Double) index).doubleValue()))) {
                    indexOutOfBoundsException(target, index);
                }
                return super.executeWithTargetAndIndexUnchecked(target, index);
            }
        }

        @TruffleBoundary
        private void indexOutOfBoundsException(Object target, Object index) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) targetClass.cast(target);
            throw new IndexOutOfBoundsException("Index: " + (((Double) index).doubleValue() > 0 ? "" : "-") + "Infinity, Size: " + list.size());
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            Object value = Boundaries.listGet(((List<?>) targetClass.cast(target)), index);
            return JSRuntime.toJSNull(value);
        }
    }

    private static class JavaArrayReadElementTypeCacheNode extends ObjectReadElementTypeCacheNode {
        @Child private ToArrayIndexNode toArrayIndexNode;
        private final ConditionProfile indexProfile = ConditionProfile.createBinaryProfile();

        JavaArrayReadElementTypeCacheNode(JSContext context, Class<?> targetClass) {
            super(context, targetClass);
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (indexProfile.profile(convertedIndex instanceof Long && ((Long) convertedIndex).intValue() >= 0)) {
                return arrayGet(target, ((Long) convertedIndex).intValue());
            } else {
                return super.executeWithTargetAndIndexUnchecked(target, index);
            }
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            return arrayGet(target, index);
        }

        private static Object arrayGet(Object array, int index) {
            if (index >= 0 && index < Array.getLength(array)) {
                Object value = Array.get(array, index);
                return JSRuntime.toJSNull(value);
            } else {
                // see GR-4172
                return Undefined.instance;
            }
        }

    }

    abstract static class ArrayReadElementCacheNode extends ReadElementCacheNode {

        protected ArrayReadElementCacheNode(JSContext context) {
            super(context);
        }

        static ArrayReadElementCacheNode create(JSContext context) {
            return new UninitArrayReadElementCacheNode(context);
        }

        protected abstract Object executeWithTargetAndArrayAndIndex(DynamicObject target, ScriptArray array, long index, boolean arrayCondition);

        protected int executeWithTargetAndArrayAndIndexInt(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndArrayAndIndex(target, array, index, arrayCondition));
        }

        protected double executeWithTargetAndArrayAndIndexDouble(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndArrayAndIndex(target, array, index, arrayCondition));
        }
    }

    private static class UninitArrayReadElementCacheNode extends ArrayReadElementCacheNode {

        protected UninitArrayReadElementCacheNode(JSContext context) {
            super(context);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndex(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArrayReadElementCacheNode selection = null;
            if (array instanceof ConstantEmptyArray) {
                selection = new EmptyArrayReadElementCacheNode(context, array);
            } else if (array instanceof ConstantObjectArray) {
                selection = new ConstantObjectArrayReadElementCacheNode(context, array);
            } else if (array instanceof LazyRegexResultArray) {
                selection = new LazyRegexResultArrayReadElementCacheNode(context, array);
            } else if (array instanceof AbstractConstantArray) {
                selection = new ConstantArrayReadElementCacheNode(context, array);
            } else if (array instanceof HolesIntArray) {
                selection = new HolesIntArrayReadElementCacheNode(context, array);
            } else if (array instanceof HolesDoubleArray) {
                selection = new HolesDoubleArrayReadElementCacheNode(context, array);
            } else if (array instanceof HolesJSObjectArray) {
                selection = new HolesJSObjectArrayReadElementCacheNode(context, array);
            } else if (array instanceof HolesObjectArray) {
                selection = new HolesObjectArrayReadElementCacheNode(context, array);
            } else if (array instanceof AbstractWritableArray) {
                selection = new WritableArrayReadElementCacheNode(context, array);
            } else if (array instanceof TypedArray.AbstractUint32Array) {
                selection = new Uint32ArrayReadElementCacheNode(context, array);
            } else if (array instanceof TypedArray.TypedIntArray) {
                selection = new TypedIntArrayReadElementCacheNode(context, array);
            } else if (array instanceof TypedArray.TypedFloatArray) {
                selection = new TypedFloatArrayReadElementCacheNode(context, array);
            } else {
                selection = new ExactArrayReadElementCacheNode(context, array);
            }
            purgeStaleCacheEntries(target);
            this.replace(selection);
            return selection.executeWithTargetAndArrayAndIndex(target, array, index, arrayCondition);
        }

        private void purgeStaleCacheEntries(DynamicObject target) {
            if (JSTruffleOptions.TrackArrayAllocationSites && this.getParent() instanceof ConstantArrayReadElementCacheNode && JSArray.isJSArray(target)) {
                ArrayAllocationSite allocationSite = JSAbstractArray.arrayGetAllocationSite(target);
                if (allocationSite != null && allocationSite.getInitialArrayType() != null) {
                    ScriptArray initialArrayType = allocationSite.getInitialArrayType();
                    ConstantArrayReadElementCacheNode existingNode = (ConstantArrayReadElementCacheNode) this.getParent();
                    if (!(initialArrayType instanceof ConstantEmptyArray) && existingNode.getArrayType() instanceof ConstantEmptyArray) {
                        // allocation site has been patched to not create an empty array;
                        // purge existing empty array specialization in cache
                        if (JSTruffleOptions.TraceArrayTransitions) {
                            System.out.println("purging " + existingNode);
                        }
                        existingNode.purge();
                    }
                }
            }
        }
    }

    private abstract static class CachedArrayReadElementCacheNode extends ArrayReadElementCacheNode {
        @Child private ArrayReadElementCacheNode arrayCacheNext;

        CachedArrayReadElementCacheNode(JSContext context) {
            super(context);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndex(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            if (guard(target, array)) {
                return executeWithTargetAndArrayAndIndexUnchecked(target, array, index, arrayCondition);
            } else {
                return getNext().executeWithTargetAndArrayAndIndex(target, array, index, arrayCondition);
            }
        }

        protected abstract Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition);

        @Override
        protected int executeWithTargetAndArrayAndIndexInt(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            if (guard(target, array)) {
                return executeWithTargetAndArrayAndIndexUncheckedInt(target, array, index, arrayCondition);
            } else {
                return getNext().executeWithTargetAndArrayAndIndexInt(target, array, index, arrayCondition);
            }
        }

        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndArrayAndIndexUnchecked(target, array, index, arrayCondition));
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexDouble(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            if (guard(target, array)) {
                return executeWithTargetAndArrayAndIndexUncheckedDouble(target, array, index, arrayCondition);
            } else {
                return getNext().executeWithTargetAndArrayAndIndexDouble(target, array, index, arrayCondition);
            }
        }

        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, boolean arrayCondition)
                        throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndArrayAndIndexUnchecked(target, array, index, arrayCondition));
        }

        protected abstract boolean guard(Object target, ScriptArray array);

        private ArrayReadElementCacheNode getNext() {
            if (arrayCacheNext == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayCacheNext = insert(ArrayReadElementCacheNode.create(context));
            }
            return arrayCacheNext;
        }

        protected final void purge() {
            this.replace(getNext());
        }
    }

    private abstract static class ArrayClassGuardCachedArrayReadElementCacheNode extends CachedArrayReadElementCacheNode {
        private final ScriptArray arrayType;
        protected final ConditionProfile inBounds = ConditionProfile.createBinaryProfile();
        private final ConditionProfile needGetProperty = ConditionProfile.createBinaryProfile();
        private final JSClassProfile outOfBoundsClassProfile = JSClassProfile.create();

        ArrayClassGuardCachedArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context);
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

        protected Object readOutOfBounds(DynamicObject target, long index) {
            if (needGetProperty.profile(needsSlowGet(target))) {
                return JSObject.get(target, index, outOfBoundsClassProfile);
            } else {
                return Undefined.instance;
            }
        }

        private boolean needsSlowGet(DynamicObject target) {
            return !context.getArrayPrototypeNoElementsAssumption().isValid() || (!context.getFastArrayAssumption().isValid() && JSSlowArray.isJSSlowArray(target)) ||
                            (!context.getFastArgumentsObjectAssumption().isValid() && JSSlowArgumentsObject.isJSSlowArgumentsObject(target));
        }

        protected void checkDetachedArrayBuffer(DynamicObject target) {
            if (JSArrayBufferView.hasDetachedBuffer(target, context)) {
                throw Errors.createTypeErrorDetachedBuffer();
            }
        }
    }

    private static class ExactArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final JSClassProfile classProfile = JSClassProfile.create();

        ExactArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            return JSObject.get(target, index, classProfile);
        }
    }

    private static class ConstantArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        ConstantArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            AbstractConstantArray constantArray = (AbstractConstantArray) cast(array);
            if (inBounds.profile(constantArray.hasElement(target, index, arrayCondition))) {
                return constantArray.getElementInBounds(target, (int) index, arrayCondition);
            } else {
                return readOutOfBounds(target, index);
            }
        }
    }

    private static class EmptyArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        EmptyArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
            assert arrayType.getClass() == ConstantEmptyArray.class;
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            return readOutOfBounds(target, index);
        }
    }

    private static class ConstantObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeArrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        ConstantObjectArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            ConstantObjectArray constantObjectArray = (ConstantObjectArray) cast(array);
            if (inBounds.profile(constantObjectArray.isInBoundsFast(target, index, arrayCondition))) {
                Object value = ConstantObjectArray.getElementInBoundsDirect(target, (int) index, arrayCondition);
                if (holeArrayProfile.profile(!constantObjectArray.hasHoles(target, arrayCondition))) {
                    return value;
                } else {
                    if (holeProfile.profile(!HolesObjectArray.isHoleValue(value))) {
                        return value;
                    }
                }
            }
            return readOutOfBounds(target, index);
        }
    }

    private static class LazyRegexResultArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        @Child TRegexUtil.TRegexMaterializeResultNode materializeResultNode;

        LazyRegexResultArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        private TRegexUtil.TRegexMaterializeResultNode getMaterializeResultNode() {
            if (materializeResultNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                materializeResultNode = insert(TRegexUtil.TRegexMaterializeResultNode.create());
            }
            return materializeResultNode;
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            LazyRegexResultArray lazyRegexResultArray = (LazyRegexResultArray) array;
            if (inBounds.profile(lazyRegexResultArray.hasElement(target, (int) index))) {
                return LazyRegexResultArray.materializeGroup(getMaterializeResultNode(), target, (int) index);
            } else {
                return readOutOfBounds(target, index);
            }
        }
    }

    private static class WritableArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        WritableArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index, arrayCondition))) {
                return writableArray.getInBoundsFast(target, (int) index, arrayCondition);
            } else {
                return readOutOfBounds(target, index);
            }
        }

        @Override
        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index, arrayCondition))) {
                return writableArray.getInBoundsFastInt(target, (int) index, arrayCondition);
            } else {
                return JSTypesGen.expectInteger(readOutOfBounds(target, index));
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, boolean arrayCondition)
                        throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index, arrayCondition))) {
                return writableArray.getInBoundsFastDouble(target, (int) index, arrayCondition);
            } else {
                return JSTypesGen.expectDouble(readOutOfBounds(target, index));
            }
        }
    }

    private static class HolesIntArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesIntArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            HolesIntArray holesIntArray = (HolesIntArray) cast(array);
            if (inBounds.profile(holesIntArray.isInBoundsFast(target, index, arrayCondition))) {
                int value = holesIntArray.getInBoundsFastInt(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesIntArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index);
        }
    }

    private static class HolesDoubleArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesDoubleArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            HolesDoubleArray holesDoubleArray = (HolesDoubleArray) cast(array);
            if (inBounds.profile(holesDoubleArray.isInBoundsFast(target, index, arrayCondition))) {
                double value = holesDoubleArray.getInBoundsFastDouble(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesDoubleArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index);
        }
    }

    private static class HolesJSObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesJSObjectArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            HolesJSObjectArray holesArray = (HolesJSObjectArray) cast(array);
            if (inBounds.profile(holesArray.isInBoundsFast(target, index, arrayCondition))) {
                DynamicObject value = holesArray.getInBoundsFastJSObject(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesJSObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index);
        }
    }

    private static class HolesObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesObjectArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            HolesObjectArray holesArray = (HolesObjectArray) cast(array);
            if (inBounds.profile(holesArray.isInBoundsFast(target, index, arrayCondition))) {
                Object value = holesArray.getInBoundsFastObject(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index);
        }
    }

    private static class TypedIntArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        TypedIntArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition);
            } else {
                throw new UnexpectedResultException(Undefined.instance);
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, boolean arrayCondition)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition);
            } else {
                throw new UnexpectedResultException(Undefined.instance);
            }
        }
    }

    private static class Uint32ArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile isSignedProfile = ConditionProfile.createBinaryProfile();

        Uint32ArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                int intValue = typedArray.getInt(target, (int) index, arrayCondition);
                if (isSignedProfile.profile(intValue >= 0)) {
                    return intValue;
                } else {
                    return (double) (intValue & 0xffff_ffffL);
                }
            } else {
                return Undefined.instance;
            }
        }

        @Override
        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                int intValue = typedArray.getInt(target, (int) index, arrayCondition);
                if (isSignedProfile.profile(intValue >= 0)) {
                    return intValue;
                } else {
                    throw new UnexpectedResultException((double) (intValue & 0xffff_ffffL));
                }
            } else {
                throw new UnexpectedResultException(Undefined.instance);
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, boolean arrayCondition)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition) & 0xffff_ffffL;
            } else {
                throw new UnexpectedResultException(Undefined.instance);
            }
        }
    }

    private static class TypedFloatArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        TypedFloatArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, boolean arrayCondition) {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedFloatArray<?> typedArray = (TypedArray.TypedFloatArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getDouble(target, (int) index, arrayCondition);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, boolean arrayCondition)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedFloatArray<?> typedArray = (TypedArray.TypedFloatArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getDouble(target, (int) index, arrayCondition);
            } else {
                throw new UnexpectedResultException(Undefined.instance);
            }
        }
    }

    private abstract static class ToPropertyKeyCachedReadElementTypeCacheNode extends CachedReadElementTypeCacheNode {
        @Child private JSToStringNode indexToStringNode;
        protected final JSClassProfile jsclassProfile = JSClassProfile.create();

        ToPropertyKeyCachedReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        protected final Object toPropertyKey(Object index) {
            if (index instanceof Symbol) {
                return index;
            }
            if (indexToStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                indexToStringNode = insert(JSToStringNode.create());
            }
            return indexToStringNode.executeString(index);
        }
    }

    private static class StringReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> stringClass;
        private final BranchProfile intIndexBranch = BranchProfile.create();
        private final BranchProfile intIndexInBoundsBranch = BranchProfile.create();
        private final BranchProfile stringIndexBranch = BranchProfile.create();
        @Child private ToArrayIndexNode toArrayIndexNode;

        StringReadElementTypeCacheNode(JSContext context, Class<?> stringClass) {
            super(context);
            this.stringClass = stringClass;
            this.toArrayIndexNode = ToArrayIndexNode.create();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (convertedIndex instanceof Long) {
                intIndexBranch.enter();
                int intIndex = ((Long) convertedIndex).intValue();
                if (intIndex >= 0 && intIndex < JSRuntime.length(charSequence)) {
                    intIndexInBoundsBranch.enter();
                    return String.valueOf(Boundaries.charAt(charSequence, intIndex));
                }
            }
            stringIndexBranch.enter();
            return JSObject.get(JSString.create(context, charSequence), toPropertyKey(convertedIndex), jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            if (index >= 0 && index < charSequence.length()) {
                intIndexInBoundsBranch.enter();
                return String.valueOf(Boundaries.charAt(charSequence, index));
            } else {
                stringIndexBranch.enter();
                return JSObject.get(JSString.create(context, charSequence), index, jsclassProfile);
            }
        }

        @Override
        public boolean guard(Object target) {
            return stringClass.isInstance(target);
        }
    }

    private static class NumberReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> numberClass;

        NumberReadElementTypeCacheNode(JSContext context, Class<?> stringClass) {
            super(context);
            this.numberClass = stringClass;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            Number charSequence = (Number) target;
            return JSObject.get(JSNumber.create(context, charSequence), toPropertyKey(index), jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            Number charSequence = (Number) target;
            return JSObject.get(JSNumber.create(context, charSequence), index, jsclassProfile);
        }

        @Override
        public boolean guard(Object target) {
            return numberClass.isInstance(target);
        }
    }

    private static class BooleanReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        BooleanReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            Boolean bool = (Boolean) target;
            return JSObject.get(JSBoolean.create(context, bool), toPropertyKey(index), jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            Boolean bool = (Boolean) target;
            return JSObject.get(JSBoolean.create(context, bool), index, jsclassProfile);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Boolean;
        }
    }

    private static class SymbolReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> numberClass;

        SymbolReadElementTypeCacheNode(JSContext context, Class<?> stringClass) {
            super(context);
            this.numberClass = stringClass;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            Symbol symbol = (Symbol) target;
            return JSObject.get(JSSymbol.create(context, symbol), toPropertyKey(index), jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            Symbol symbol = (Symbol) target;
            return JSObject.get(JSSymbol.create(context, symbol), index, jsclassProfile);
        }

        @Override
        public boolean guard(Object target) {
            return numberClass.isInstance(target);
        }
    }

    private static class TruffleObjectReadElementTypeCacheNode extends CachedReadElementTypeCacheNode {
        private final Class<? extends TruffleObject> targetClass;

        @Child private Node foreignIsNull;
        @Child private Node foreignArrayAccess;
        @Child private ExportValueNode convert;
        @Child private JSForeignToJSTypeNode foreignConvertNode;

        TruffleObjectReadElementTypeCacheNode(JSContext context, Class<? extends TruffleObject> targetClass) {
            super(context);
            this.targetClass = targetClass;
            this.convert = ExportValueNodeGen.create(context);
            this.foreignIsNull = Message.IS_NULL.createNode();
            this.foreignArrayAccess = Message.READ.createNode();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index) {
            TruffleObject truffleObject = targetClass.cast(target);
            if (ForeignAccess.sendIsNull(foreignIsNull, truffleObject)) {
                throw Errors.createTypeErrorCannotGetProperty(index, target, false, this);
            }
            try {
                Object converted = convert.executeWithTarget(index, Undefined.instance);
                if (converted instanceof Symbol) {
                    return Undefined.instance;
                }
                return toJSType(ForeignAccess.sendRead(foreignArrayAccess, truffleObject, converted));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                return Undefined.instance;
            }
        }

        private Object toJSType(Object value) {
            if (foreignConvertNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignConvertNode = insert(JSForeignToJSTypeNode.create());
            }
            return foreignConvertNode.executeWithTarget(value);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index) {
            return executeWithTargetAndIndexUnchecked(target, (Object) index);
        }

        @Override
        public boolean guard(Object target) {
            return targetClass.isInstance(target);
        }
    }

    @Override
    public final JavaScriptNode getTarget() {
        return targetNode;
    }

    public final JavaScriptNode getElement() {
        return indexNode;
    }

    public final JSContext getContext() {
        return typeCacheNode.context;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(targetNode), cloneUninitialized(indexNode), getContext());
    }

    @Override
    public String expressionToString() {
        if (targetNode != null && indexNode != null) {
            return Objects.toString(targetNode.expressionToString(), INTERMEDIATE_VALUE) + "[" + Objects.toString(indexNode.expressionToString(), INTERMEDIATE_VALUE) + "]";
        }
        return null;
    }

}
