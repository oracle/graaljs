/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSTypesGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNodeGen;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
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
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSSlowArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSSlowArray;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyReference;
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
        if (materializedTags.contains(ReadElementExpressionTag.class) && !alreadyMaterialized()) {
            JavaScriptNode clonedTarget = targetNode == null || targetNode.hasSourceSection() ? targetNode : JSTaggedExecutionNode.createForInput(targetNode, this);
            JavaScriptNode clonedIndex = indexNode == null || indexNode.hasSourceSection() ? indexNode : JSTaggedExecutionNode.createForInput(indexNode, this);
            JavaScriptNode cloned = ReadElementNode.create(clonedTarget, clonedIndex, getContext());
            transferSourceSectionAndTags(this, cloned);
            return cloned;
        }
        return this;
    }

    private boolean alreadyMaterialized() {
        return targetNode instanceof JSTaggedExecutionNode || indexNode instanceof JSTaggedExecutionNode;
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
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = getIndexNode().execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndex(target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(target, index);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = getIndexNode().executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndex(target, e.getResult());
            }
            return executeWithTargetAndIndex(target, index);
        } else {
            assert is == INDEX_OBJECT;
            Object index = getIndexNode().execute(frame);
            return executeWithTargetAndIndex(target, index);
        }
    }

    public int executeWithTargetInt(VirtualFrame frame, Object target) throws UnexpectedResultException {
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = getIndexNode().execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexInt(target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(target, index);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = getIndexNode().executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexInt(target, e.getResult());
            }
            return executeWithTargetAndIndexInt(target, index);
        } else {
            assert is == INDEX_OBJECT;
            Object index = getIndexNode().execute(frame);
            return executeWithTargetAndIndexInt(target, index);
        }
    }

    public double executeWithTargetDouble(VirtualFrame frame, Object target) throws UnexpectedResultException {
        byte is = indexState;
        if (is == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object index = getIndexNode().execute(frame);
            if (index instanceof Integer) {
                indexState = INDEX_INT;
                return executeWithTargetAndIndexDouble(target, (int) index);
            } else {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(target, index);
            }
        } else if (is == INDEX_INT) {
            int index;
            try {
                index = getIndexNode().executeInt(frame);
            } catch (UnexpectedResultException e) {
                indexState = INDEX_OBJECT;
                return executeWithTargetAndIndexDouble(target, e.getResult());
            }
            return executeWithTargetAndIndexDouble(target, index);
        } else {
            assert is == INDEX_OBJECT;
            Object index = getIndexNode().execute(frame);
            return executeWithTargetAndIndexDouble(target, index);
        }
    }

    public final Object executeWithTargetAndIndex(Object target, Object index) {
        return getTypeCacheNode().executeWithTargetAndIndex(target, index, Undefined.instance);
    }

    public final Object executeWithTargetAndIndex(Object target, int index) {
        return getTypeCacheNode().executeWithTargetAndIndex(target, index, Undefined.instance);
    }

    public final int executeWithTargetAndIndexInt(Object target, Object index) throws UnexpectedResultException {
        return getTypeCacheNode().executeWithTargetAndIndexInt(target, index, Undefined.instance);
    }

    public final int executeWithTargetAndIndexInt(Object target, int index) throws UnexpectedResultException {
        return getTypeCacheNode().executeWithTargetAndIndexInt(target, index, Undefined.instance);
    }

    public final double executeWithTargetAndIndexDouble(Object target, Object index) throws UnexpectedResultException {
        return getTypeCacheNode().executeWithTargetAndIndexDouble(target, index, Undefined.instance);
    }

    public final double executeWithTargetAndIndexDouble(Object target, int index) throws UnexpectedResultException {
        return getTypeCacheNode().executeWithTargetAndIndexDouble(target, index, Undefined.instance);
    }

    public final Object executeWithTargetAndIndexOrDefault(Object target, Object index, Object defaultValue) {
        return getTypeCacheNode().executeWithTargetAndIndex(target, index, defaultValue);
    }

    private ReadElementTypeCacheNode getTypeCacheNode() {
        if (typeCacheNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeCacheNode = insert(new UninitReadElementTypeCacheNode(context));
        }
        return typeCacheNode;
    }

    abstract static class ReadElementCacheNode extends JavaScriptBaseNode {
        protected final JSContext context;

        protected ReadElementCacheNode(JSContext context) {
            this.context = context;
        }
    }

    abstract static class ReadElementTypeCacheNode extends ReadElementCacheNode {
        protected ReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        public abstract Object executeWithTargetAndIndex(Object target, Object index, Object defaultValue);

        public Object executeWithTargetAndIndex(Object target, int index, Object defaultValue) {
            return executeWithTargetAndIndex(target, (Object) index, defaultValue);
        }

        public int executeWithTargetAndIndexInt(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndIndex(target, index, defaultValue));
        }

        public int executeWithTargetAndIndexInt(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            return executeWithTargetAndIndexInt(target, (Object) index, defaultValue);
        }

        public double executeWithTargetAndIndexDouble(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndIndex(target, index, defaultValue));
        }

        public double executeWithTargetAndIndexDouble(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            return executeWithTargetAndIndexDouble(target, (Object) index, defaultValue);
        }
    }

    private static class UninitReadElementTypeCacheNode extends ReadElementTypeCacheNode {
        protected UninitReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        @Override
        public Object executeWithTargetAndIndex(Object target, Object index, Object defaultValue) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            CachedReadElementTypeCacheNode specialized = makeTypeCacheNode(target);
            checkForPolymorphicSpecialize();
            this.replace(specialized);
            return specialized.executeWithTargetAndIndex(target, index, defaultValue);
        }

        private void checkForPolymorphicSpecialize() {
            Node parent = getParent();
            if (parent != null && parent instanceof ReadElementCacheNode) {
                reportPolymorphicSpecialize();
            }
        }

        @SuppressWarnings("unchecked")
        private CachedReadElementTypeCacheNode makeTypeCacheNode(Object target) {
            if (JSObject.isJSObject(target)) {
                return new JSObjectReadElementTypeCacheNode(context);
            } else if (target instanceof JSLazyString) {
                return new LazyStringReadElementTypeCacheNode(context);
            } else if (JSRuntime.isString(target)) {
                return new StringReadElementTypeCacheNode(context, target.getClass());
            } else if (target instanceof Boolean) {
                return new BooleanReadElementTypeCacheNode(context);
            } else if (target instanceof Number) {
                return new NumberReadElementTypeCacheNode(context, target.getClass());
            } else if (target instanceof Symbol) {
                return new SymbolReadElementTypeCacheNode(context);
            } else if (target instanceof BigInt) {
                return new BigIntReadElementTypeCacheNode(context);
            } else if (target instanceof TruffleObject) {
                assert JSRuntime.isForeignObject(target);
                return new TruffleObjectReadElementTypeCacheNode(context, (Class<? extends TruffleObject>) target.getClass());
            } else {
                assert JSRuntime.isJavaPrimitive(target);
                return new JavaObjectReadElementTypeCacheNode(context, target.getClass());
            }
        }
    }

    abstract static class CachedReadElementTypeCacheNode extends ReadElementTypeCacheNode {
        @Child private ReadElementTypeCacheNode typeCacheNext;

        CachedReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        @Override
        public Object executeWithTargetAndIndex(Object target, Object index, Object defaultValue) {
            if (guard(target)) {
                return executeWithTargetAndIndexUnchecked(target, index, defaultValue);
            } else {
                return getNext().executeWithTargetAndIndex(target, index, defaultValue);
            }
        }

        @Override
        public Object executeWithTargetAndIndex(Object target, int index, Object defaultValue) {
            if (guard(target)) {
                return executeWithTargetAndIndexUnchecked(target, index, defaultValue);
            } else {
                return getNext().executeWithTargetAndIndex(target, index, defaultValue);
            }
        }

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue);

        protected abstract Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue);

        @Override
        public int executeWithTargetAndIndexInt(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedInt(target, index, defaultValue);
            } else {
                return getNext().executeWithTargetAndIndexInt(target, index, defaultValue);
            }
        }

        @Override
        public int executeWithTargetAndIndexInt(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedInt(target, index, defaultValue);
            } else {
                return getNext().executeWithTargetAndIndexInt(target, index, defaultValue);
            }
        }

        protected int executeWithTargetAndIndexUncheckedInt(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndIndexUnchecked(target, index, defaultValue));
        }

        protected int executeWithTargetAndIndexUncheckedInt(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            return executeWithTargetAndIndexUncheckedInt(target, (Object) index, defaultValue);
        }

        @Override
        public double executeWithTargetAndIndexDouble(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedDouble(target, index, defaultValue);
            } else {
                return getNext().executeWithTargetAndIndexDouble(target, index, defaultValue);
            }
        }

        @Override
        public double executeWithTargetAndIndexDouble(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            if (guard(target)) {
                return executeWithTargetAndIndexUncheckedDouble(target, index, defaultValue);
            } else {
                return getNext().executeWithTargetAndIndexDouble(target, index, defaultValue);
            }
        }

        protected double executeWithTargetAndIndexUncheckedDouble(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndIndexUnchecked(target, index, defaultValue));
        }

        protected double executeWithTargetAndIndexUncheckedDouble(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            return executeWithTargetAndIndexUncheckedDouble(target, (Object) index, defaultValue);
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
        @Child private IsJSObjectNode isObjectNode;
        private final ConditionProfile arrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile arrayIndexProfile = ConditionProfile.createBinaryProfile();
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        JSObjectReadElementTypeCacheNode(JSContext context) {
            super(context);
            this.isArrayNode = IsArrayNode.createIsAnyArray();
            this.isObjectNode = IsJSObjectNode.createIncludeNullUndefined();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndex(targetObject, array, (Long) objIndex, defaultValue, arrayCondition);
                } else {
                    return getProperty(targetObject, objIndex, defaultValue);
                }
            } else {
                return readNonArrayObjectIndex(targetObject, index, defaultValue);
            }
        }

        private Object toArrayIndex(Object index) {
            if (toArrayIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toArrayIndexNode = insert(ToArrayIndexNode.create());
            }
            return toArrayIndexNode.execute(index);
        }

        private ArrayReadElementCacheNode getArrayReadElementNode() {
            if (arrayReadElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayReadElementNode = insert(ArrayReadElementCacheNode.create(context));
            }
            return arrayReadElementNode;
        }

        private Object readNonArrayObjectIndex(DynamicObject targetObject, Object index, Object defaultValue) {
            return getNonArrayNode().execute(targetObject, index, defaultValue);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                if (arrayIndexProfile.profile(index >= 0)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndex(targetObject, array, index, defaultValue, arrayCondition);
                } else {
                    return getProperty(targetObject, Boundaries.stringValueOf(index), defaultValue);
                }
            } else {
                return getNonArrayNode().getPropertyGeneric(targetObject, index, defaultValue);
            }
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexInt(targetObject, array, (Long) objIndex, defaultValue, arrayCondition);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, objIndex, defaultValue));
                }
            } else {
                return JSTypesGen.expectInteger(readNonArrayObjectIndex(targetObject, index, defaultValue));
            }
        }

        @Override
        protected int executeWithTargetAndIndexUncheckedInt(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);

                if (arrayIndexProfile.profile(index >= 0)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexInt(targetObject, array, index, defaultValue, arrayCondition);
                } else {
                    return JSTypesGen.expectInteger(getProperty(targetObject, Boundaries.stringValueOf(index), defaultValue));
                }
            } else {
                return JSTypesGen.expectInteger(getNonArrayNode().getPropertyGeneric(targetObject, index, defaultValue));
            }
        }

        @Override
        protected double executeWithTargetAndIndexUncheckedDouble(Object target, Object index, Object defaultValue) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);
                Object objIndex = toArrayIndex(index);

                if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexDouble(targetObject, array, (Long) objIndex, defaultValue, arrayCondition);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, objIndex, defaultValue));
                }
            } else {
                return JSTypesGen.expectDouble(readNonArrayObjectIndex(targetObject, index, defaultValue));
            }
        }

        @Override
        protected double executeWithTargetAndIndexUncheckedDouble(Object target, int index, Object defaultValue) throws UnexpectedResultException {
            DynamicObject targetObject = (DynamicObject) target;
            boolean arrayCondition = isArrayNode.execute(targetObject);
            if (arrayProfile.profile(arrayCondition)) {
                ScriptArray array = JSObject.getArray(targetObject, arrayCondition);

                if (arrayIndexProfile.profile(index >= 0)) {
                    return getArrayReadElementNode().executeWithTargetAndArrayAndIndexDouble(targetObject, array, index, defaultValue, arrayCondition);
                } else {
                    return JSTypesGen.expectDouble(getProperty(targetObject, Boundaries.stringValueOf(index), defaultValue));
                }
            } else {
                return JSTypesGen.expectDouble(getNonArrayNode().getPropertyGeneric(targetObject, index, defaultValue));
            }
        }

        @Override
        public boolean guard(Object target) {
            return isObjectNode.executeBoolean(target);
        }

        private Object getProperty(DynamicObject targetObject, Object objIndex, Object defaultValue) {
            return JSObject.getOrDefault(targetObject, objIndex, defaultValue, jsclassProfile);
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

        public Object execute(DynamicObject targetObject, Object index, Object defaultValue) {
            if (propertyReferenceProfile.profile(
                            index instanceof PropertyReference && ((PropertyReference) index).getDepth() == 0 && ((PropertyReference) index).getShape() == targetObject.getShape())) {
                return readPropertyReference(index, targetObject);
            } else {
                return getPropertyGeneric(targetObject, index, defaultValue);
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

        public Object getPropertyGeneric(DynamicObject targetObject, Object index, Object defaultValue) {
            return getCachedProperty(targetObject, index, defaultValue);
        }

        private Object getCachedProperty(DynamicObject targetObject, Object index, Object defaultValue) {
            if (getPropertyCachedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getPropertyCachedNode = insert(CachedGetPropertyNode.create(context));
            }
            return getPropertyCachedNode.execute(targetObject, index, defaultValue);
        }
    }

    private static class JavaObjectReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        protected final Class<?> targetClass;

        JavaObjectReadElementTypeCacheNode(JSContext context, Class<?> targetClass) {
            super(context);
            this.targetClass = targetClass;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            toPropertyKey(index);
            return Undefined.instance;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            return Undefined.instance;
        }

        @Override
        public final boolean guard(Object target) {
            return targetClass.isInstance(target);
        }
    }

    abstract static class ArrayReadElementCacheNode extends ReadElementCacheNode {

        protected ArrayReadElementCacheNode(JSContext context) {
            super(context);
        }

        static ArrayReadElementCacheNode create(JSContext context) {
            return new UninitArrayReadElementCacheNode(context);
        }

        protected abstract Object executeWithTargetAndArrayAndIndex(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition);

        protected int executeWithTargetAndArrayAndIndexInt(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndArrayAndIndex(target, array, index, defaultValue, arrayCondition));
        }

        protected double executeWithTargetAndArrayAndIndexDouble(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndArrayAndIndex(target, array, index, defaultValue, arrayCondition));
        }
    }

    private static class UninitArrayReadElementCacheNode extends ArrayReadElementCacheNode {

        protected UninitArrayReadElementCacheNode(JSContext context) {
            super(context);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndex(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
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
            } else if (array instanceof TypedArray.TypedBigIntArray) {
                selection = new TypedBigIntArrayReadElementCacheNode(context, array);
            } else {
                selection = new ExactArrayReadElementCacheNode(context, array);
            }
            Lock lock = getLock();
            try {
                lock.lock();
                purgeStaleCacheEntries(target);
                this.replace(selection);
                Node parent = getParent();
                if (parent != null && parent instanceof CachedArrayReadElementCacheNode) {
                    reportPolymorphicSpecialize();
                }
            } finally {
                lock.unlock();
            }
            return selection.executeWithTargetAndArrayAndIndex(target, array, index, defaultValue, arrayCondition);
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
        protected Object executeWithTargetAndArrayAndIndex(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            if (guard(target, array)) {
                return executeWithTargetAndArrayAndIndexUnchecked(target, array, index, defaultValue, arrayCondition);
            } else {
                return getNext().executeWithTargetAndArrayAndIndex(target, array, index, defaultValue, arrayCondition);
            }
        }

        protected abstract Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition);

        @Override
        protected int executeWithTargetAndArrayAndIndexInt(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
            if (guard(target, array)) {
                return executeWithTargetAndArrayAndIndexUncheckedInt(target, array, index, defaultValue, arrayCondition);
            } else {
                return getNext().executeWithTargetAndArrayAndIndexInt(target, array, index, defaultValue, arrayCondition);
            }
        }

        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
            return JSTypesGen.expectInteger(executeWithTargetAndArrayAndIndexUnchecked(target, array, index, defaultValue, arrayCondition));
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexDouble(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
            if (guard(target, array)) {
                return executeWithTargetAndArrayAndIndexUncheckedDouble(target, array, index, defaultValue, arrayCondition);
            } else {
                return getNext().executeWithTargetAndArrayAndIndexDouble(target, array, index, defaultValue, arrayCondition);
            }
        }

        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition)
                        throws UnexpectedResultException {
            return JSTypesGen.expectDouble(executeWithTargetAndArrayAndIndexUnchecked(target, array, index, defaultValue, arrayCondition));
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

        protected Object readOutOfBounds(DynamicObject target, long index, Object defaultValue) {
            if (needGetProperty.profile(needsSlowGet(target))) {
                return JSObject.getOrDefault(target, index, defaultValue, outOfBoundsClassProfile);
            } else {
                return defaultValue;
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
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            return JSObject.getOrDefault(target, index, defaultValue, classProfile);
        }
    }

    private static class ConstantArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        ConstantArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            AbstractConstantArray constantArray = (AbstractConstantArray) cast(array);
            if (inBounds.profile(constantArray.hasElement(target, index, arrayCondition))) {
                return constantArray.getElementInBounds(target, (int) index, arrayCondition);
            } else {
                return readOutOfBounds(target, index, defaultValue);
            }
        }
    }

    private static class EmptyArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        EmptyArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
            assert arrayType.getClass() == ConstantEmptyArray.class;
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            return readOutOfBounds(target, index, defaultValue);
        }
    }

    private static class ConstantObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeArrayProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        ConstantObjectArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
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
            return readOutOfBounds(target, index, defaultValue);
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
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            LazyRegexResultArray lazyRegexResultArray = (LazyRegexResultArray) array;
            if (inBounds.profile(lazyRegexResultArray.hasElement(target, (int) index))) {
                return LazyRegexResultArray.materializeGroup(getMaterializeResultNode(), target, (int) index, arrayCondition && array instanceof LazyRegexResultArray);
            } else {
                return readOutOfBounds(target, index, defaultValue);
            }
        }
    }

    private static class WritableArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        WritableArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index, arrayCondition))) {
                return writableArray.getInBoundsFast(target, (int) index, arrayCondition);
            } else {
                return readOutOfBounds(target, index, defaultValue);
            }
        }

        @Override
        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index, arrayCondition))) {
                return writableArray.getInBoundsFastInt(target, (int) index, arrayCondition);
            } else {
                return JSTypesGen.expectInteger(readOutOfBounds(target, index, defaultValue));
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition)
                        throws UnexpectedResultException {
            AbstractWritableArray writableArray = (AbstractWritableArray) cast(array);
            if (inBounds.profile(writableArray.isInBoundsFast(target, index, arrayCondition))) {
                return writableArray.getInBoundsFastDouble(target, (int) index, arrayCondition);
            } else {
                return JSTypesGen.expectDouble(readOutOfBounds(target, index, defaultValue));
            }
        }
    }

    private static class HolesIntArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesIntArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            HolesIntArray holesIntArray = (HolesIntArray) cast(array);
            if (inBounds.profile(holesIntArray.isInBoundsFast(target, index, arrayCondition))) {
                int value = holesIntArray.getInBoundsFastInt(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesIntArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, defaultValue);
        }
    }

    private static class HolesDoubleArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesDoubleArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            HolesDoubleArray holesDoubleArray = (HolesDoubleArray) cast(array);
            if (inBounds.profile(holesDoubleArray.isInBoundsFast(target, index, arrayCondition))) {
                double value = holesDoubleArray.getInBoundsFastDouble(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesDoubleArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, defaultValue);
        }
    }

    private static class HolesJSObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesJSObjectArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            HolesJSObjectArray holesArray = (HolesJSObjectArray) cast(array);
            if (inBounds.profile(holesArray.isInBoundsFast(target, index, arrayCondition))) {
                DynamicObject value = holesArray.getInBoundsFastJSObject(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesJSObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, defaultValue);
        }
    }

    private static class HolesObjectArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile holeProfile = ConditionProfile.createBinaryProfile();

        HolesObjectArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            HolesObjectArray holesArray = (HolesObjectArray) cast(array);
            if (inBounds.profile(holesArray.isInBoundsFast(target, index, arrayCondition))) {
                Object value = holesArray.getInBoundsFastObject(target, (int) index, arrayCondition);
                if (holeProfile.profile(!HolesObjectArray.isHoleValue(value))) {
                    return value;
                }
            }
            return readOutOfBounds(target, index, defaultValue);
        }
    }

    private static class TypedIntArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        TypedIntArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition);
            } else {
                return defaultValue;
            }
        }

        @Override
        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition);
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition);
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }
    }

    private static class Uint32ArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {
        private final ConditionProfile isSignedProfile = ConditionProfile.createBinaryProfile();

        Uint32ArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
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
                return defaultValue;
            }
        }

        @Override
        protected int executeWithTargetAndArrayAndIndexUncheckedInt(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) throws UnexpectedResultException {
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
                throw new UnexpectedResultException(defaultValue);
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedIntArray<?> typedArray = (TypedArray.TypedIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getInt(target, (int) index, arrayCondition) & 0xffff_ffffL;
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }
    }

    private static class TypedFloatArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        TypedFloatArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedFloatArray<?> typedArray = (TypedArray.TypedFloatArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getDouble(target, (int) index, arrayCondition);
            } else {
                return defaultValue;
            }
        }

        @Override
        protected double executeWithTargetAndArrayAndIndexUncheckedDouble(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition)
                        throws UnexpectedResultException {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedFloatArray<?> typedArray = (TypedArray.TypedFloatArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getDouble(target, (int) index, arrayCondition);
            } else {
                throw new UnexpectedResultException(defaultValue);
            }
        }
    }

    private static class TypedBigIntArrayReadElementCacheNode extends ArrayClassGuardCachedArrayReadElementCacheNode {

        TypedBigIntArrayReadElementCacheNode(JSContext context, ScriptArray arrayType) {
            super(context, arrayType);
        }

        @Override
        protected Object executeWithTargetAndArrayAndIndexUnchecked(DynamicObject target, ScriptArray array, long index, Object defaultValue, boolean arrayCondition) {
            checkDetachedArrayBuffer(target);
            TypedArray.TypedBigIntArray<?> typedArray = (TypedArray.TypedBigIntArray<?>) cast(array);
            if (inBounds.profile(typedArray.hasElement(target, index, arrayCondition))) {
                return typedArray.getBigInt(target, (int) index, arrayCondition);
            } else {
                return defaultValue;
            }
        }
    }

    private abstract static class ToPropertyKeyCachedReadElementTypeCacheNode extends CachedReadElementTypeCacheNode {
        @Child private JSToPropertyKeyNode indexToPropertyKeyNode;
        protected final JSClassProfile jsclassProfile = JSClassProfile.create();

        ToPropertyKeyCachedReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        protected final Object toPropertyKey(Object index) {
            if (indexToPropertyKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                indexToPropertyKeyNode = insert(JSToPropertyKeyNode.create());
            }
            return indexToPropertyKeyNode.execute(index);
        }
    }

    private static class StringReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> stringClass;
        private final ConditionProfile arrayIndexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile stringIndexInBounds = ConditionProfile.createBinaryProfile();
        @Child private ToArrayIndexNode toArrayIndexNode;

        StringReadElementTypeCacheNode(JSContext context, Class<?> stringClass) {
            super(context);
            this.stringClass = stringClass;
            this.toArrayIndexNode = ToArrayIndexNode.createNoToPropertyKey();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (arrayIndexProfile.profile(convertedIndex instanceof Long)) {
                int intIndex = ((Long) convertedIndex).intValue();
                if (stringIndexInBounds.profile(intIndex >= 0 && intIndex < charSequence.length())) {
                    return String.valueOf(charSequence.charAt(intIndex));
                }
            }
            return JSObject.getOrDefault(JSString.create(context, charSequence), toPropertyKey(index), defaultValue, jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            CharSequence charSequence = (CharSequence) stringClass.cast(target);
            if (stringIndexInBounds.profile(index >= 0 && index < charSequence.length())) {
                return String.valueOf(charSequence.charAt(index));
            } else {
                return JSObject.getOrDefault(JSString.create(context, charSequence), index, defaultValue, jsclassProfile);
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

        LazyStringReadElementTypeCacheNode(JSContext context) {
            super(context);
            this.toArrayIndexNode = ToArrayIndexNode.createNoToPropertyKey();
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            String charSequence = ((JSLazyString) target).toString(isFlatProfile);
            Object convertedIndex = toArrayIndexNode.execute(index);
            if (arrayIndexProfile.profile(convertedIndex instanceof Long)) {
                int intIndex = ((Long) convertedIndex).intValue();
                if (stringIndexInBounds.profile(intIndex >= 0 && intIndex < charSequence.length())) {
                    return String.valueOf(charSequence.charAt(intIndex));
                }
            }
            return JSObject.getOrDefault(JSString.create(context, charSequence), toPropertyKey(index), defaultValue, jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            String charSequence = ((JSLazyString) target).toString(isFlatProfile);
            if (stringIndexInBounds.profile(index >= 0 && index < charSequence.length())) {
                return String.valueOf(charSequence.charAt(index));
            } else {
                return JSObject.getOrDefault(JSString.create(context, charSequence), index, defaultValue, jsclassProfile);
            }
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof JSLazyString;
        }
    }

    private static class NumberReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {
        private final Class<?> numberClass;

        NumberReadElementTypeCacheNode(JSContext context, Class<?> stringClass) {
            super(context);
            this.numberClass = stringClass;
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            Number charSequence = (Number) target;
            return JSObject.getOrDefault(JSNumber.create(context, charSequence), toPropertyKey(index), defaultValue, jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            Number charSequence = (Number) target;
            return JSObject.getOrDefault(JSNumber.create(context, charSequence), index, defaultValue, jsclassProfile);
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
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            Boolean bool = (Boolean) target;
            return JSObject.getOrDefault(JSBoolean.create(context, bool), toPropertyKey(index), defaultValue, jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            Boolean bool = (Boolean) target;
            return JSObject.getOrDefault(JSBoolean.create(context, bool), index, defaultValue, jsclassProfile);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Boolean;
        }
    }

    private static class SymbolReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {

        SymbolReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            Symbol symbol = (Symbol) target;
            return JSObject.getOrDefault(JSSymbol.create(context, symbol), toPropertyKey(index), defaultValue, jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            Symbol symbol = (Symbol) target;
            return JSObject.getOrDefault(JSSymbol.create(context, symbol), index, defaultValue, jsclassProfile);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof Symbol;
        }
    }

    private static class BigIntReadElementTypeCacheNode extends ToPropertyKeyCachedReadElementTypeCacheNode {

        BigIntReadElementTypeCacheNode(JSContext context) {
            super(context);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            BigInt bigInt = (BigInt) target;
            return JSObject.getOrDefault(JSBigInt.create(context, bigInt), toPropertyKey(index), defaultValue, jsclassProfile);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            BigInt bigInt = (BigInt) target;
            return JSObject.getOrDefault(JSBigInt.create(context, bigInt), index, defaultValue, jsclassProfile);
        }

        @Override
        public boolean guard(Object target) {
            return target instanceof BigInt;
        }
    }

    static class TruffleObjectReadElementTypeCacheNode extends CachedReadElementTypeCacheNode {
        private final Class<? extends TruffleObject> targetClass;

        @Child private InteropLibrary interop;
        @Child private InteropLibrary keyInterop;
        @Child private ExportValueNode exportKeyNode;
        @Child private JSForeignToJSTypeNode toJSTypeNode;
        @Child private InteropLibrary getterInterop;
        @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;
        @Child private ReadElementNode readFromPrototypeNode;

        TruffleObjectReadElementTypeCacheNode(JSContext context, Class<? extends TruffleObject> targetClass) {
            super(context);
            this.targetClass = targetClass;
            this.exportKeyNode = ExportValueNode.create();
            this.toJSTypeNode = JSForeignToJSTypeNodeGen.create();
            this.interop = InteropLibrary.getFactory().createDispatched(3);
            this.keyInterop = InteropLibrary.getFactory().createDispatched(3);
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, Object index, Object defaultValue) {
            TruffleObject truffleObject = targetClass.cast(target);
            if (interop.isNull(truffleObject)) {
                throw Errors.createTypeErrorCannotGetProperty(index, target, false, this);
            }
            Object exportedKey = exportKeyNode.execute(index);
            if (exportedKey instanceof Symbol) {
                return Undefined.instance;
            }
            Object foreignResult;
            if (keyInterop.isString(exportedKey)) {
                String stringKey;
                try {
                    stringKey = keyInterop.asString(exportedKey);
                } catch (UnsupportedMessageException e) {
                    throw Errors.createTypeErrorInteropException(truffleObject, e, "asString", this);
                }
                try {
                    foreignResult = interop.readMember(truffleObject, stringKey);
                } catch (UnknownIdentifierException e) {
                    if (JSAbstractArray.LENGTH.equals(stringKey) && interop.hasArrayElements(truffleObject)) {
                        foreignResult = getSize(truffleObject);
                    } else if (context.isOptionNashornCompatibilityMode()) {
                        foreignResult = tryInvokeGetter(truffleObject, stringKey);
                    } else {
                        return maybeReadFromPrototype(truffleObject, stringKey);
                    }
                } catch (UnsupportedMessageException e) {
                    return maybeReadFromPrototype(truffleObject, stringKey);
                }
            } else if (keyInterop.fitsInLong(exportedKey)) {
                try {
                    foreignResult = interop.readArrayElement(truffleObject, keyInterop.asLong(exportedKey));
                } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
                    return Undefined.instance;
                }
            } else {
                return Undefined.instance;
            }
            return toJSType(foreignResult);
        }

        private Object tryInvokeGetter(TruffleObject thisObj, String key) {
            assert context.isOptionNashornCompatibilityMode();
            TruffleLanguage.Env env = context.getRealm().getEnv();
            if (env.isHostObject(thisObj)) {
                Object result = tryGetResult(thisObj, "get", key);
                if (result != null) {
                    return result;
                }
                result = tryGetResult(thisObj, "is", key);
                if (result != null) {
                    return result;
                }
            }
            return maybeReadFromPrototype(thisObj, key);
        }

        private Object tryGetResult(TruffleObject thisObj, String prefix, String key) {
            String getterKey = PropertyCacheNode.getAccessorKey(prefix, key);
            if (getterKey == null) {
                return null;
            }
            if (getterInterop == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getterInterop = insert(InteropLibrary.getFactory().createDispatched(3));
            }
            if (!getterInterop.isMemberInvocable(thisObj, getterKey)) {
                return null;
            }
            try {
                return getterInterop.invokeMember(thisObj, getterKey, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            } catch (UnknownIdentifierException e) {
                return null;
            } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                return Undefined.instance;
            }
        }

        private Object toJSType(Object value) {
            if (toJSTypeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toJSTypeNode = insert(JSForeignToJSTypeNode.create());
            }
            return toJSTypeNode.executeWithTarget(value);
        }

        private Object getSize(TruffleObject truffleObject) {
            try {
                return JSRuntime.longToIntOrDouble(interop.getArraySize(truffleObject));
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(truffleObject, e, "getArraySize", this);
            }
        }

        private Object maybeReadFromPrototype(TruffleObject truffleObject, String index) {
            if (context.getContextOptions().hasForeignObjectPrototype()) {
                if (readFromPrototypeNode == null || foreignObjectPrototypeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    this.readFromPrototypeNode = insert(ReadElementNode.create(context));
                    this.foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
                }
                DynamicObject prototype = foreignObjectPrototypeNode.executeDynamicObject(truffleObject);
                return readFromPrototypeNode.executeWithTargetAndIndex(prototype, index);
            } else {
                return Undefined.instance;
            }
        }

        @Override
        protected Object executeWithTargetAndIndexUnchecked(Object target, int index, Object defaultValue) {
            return executeWithTargetAndIndexUnchecked(target, (Object) index, defaultValue);
        }

        @Override
        public boolean guard(Object target) {
            return targetClass.isInstance(target) && !JSObject.isJSObject(target);
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
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(targetNode), cloneUninitialized(getIndexNode()), getContext());
    }

    @Override
    public String expressionToString() {
        if (targetNode != null && getIndexNode() != null) {
            return Objects.toString(targetNode.expressionToString(), INTERMEDIATE_VALUE) + "[" + Objects.toString(getIndexNode().expressionToString(), INTERMEDIATE_VALUE) + "]";
        }
        return null;
    }

    public JavaScriptNode getIndexNode() {
        return indexNode;
    }

    public static ReadElementNode createCachedInterop(ContextReference<JSRealm> contextRef) {
        return create(contextRef.get().getContext());
    }
}
