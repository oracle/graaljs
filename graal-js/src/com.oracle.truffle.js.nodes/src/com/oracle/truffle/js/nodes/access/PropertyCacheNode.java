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

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDictionaryObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.java.adapter.JavaSuperAdapter;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.util.DebugCounter;

/**
 * Common base class for {@link PropertyGetNode} and {@link PropertySetNode}. Unifies handling of
 * rewrites, obsolescence and shape checks.
 */
public abstract class PropertyCacheNode<T extends PropertyCacheNode<T>> extends JavaScriptBaseNode {
    /**
     * Checks the {@link Shape} of a {@link DynamicObject}.
     *
     * NB: always check notObsoletedAssumption before returning the result of the comparison.
     */
    protected abstract static class ReceiverCheckNode extends JavaScriptBaseNode {
        protected ReceiverCheckNode() {
        }

        /**
         * Check receiver shape, class, or instance.
         *
         * @return whether the object is supported by the associated property cache node.
         * @throws InvalidAssumptionException Shape has been invalidated or changed.
         */
        public abstract boolean accept(Object thisObj) throws InvalidAssumptionException;

        /**
         * @return the object that contains the property.
         */
        public abstract DynamicObject getStore(Object thisObj);

        public Shape getShape() {
            return null;
        }
    }

    /**
     * Checks the {@link Shape} of a {@link DynamicObject}.
     *
     * NB: always check notObsoletedAssumption before returning the result of the comparison.
     */
    protected abstract static class AbstractShapeCheckNode extends ReceiverCheckNode {

        private final Shape shape;

        protected AbstractShapeCheckNode(Shape shape) {
            this.shape = shape;
        }

        /**
         * @return the {@link DynamicObject} that contains the property.
         */
        @Override
        public abstract DynamicObject getStore(Object thisObj);

        @Override
        public final Shape getShape() {
            return shape;
        }

        public int getDepth() {
            return 0;
        }
    }

    protected static final class NullCheckNode extends ReceiverCheckNode {
        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            return thisObj == null;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            throw new UnsupportedOperationException();
        }
    }

    protected static final class InstanceofCheckNode extends ReceiverCheckNode {
        private final Class<?> type;
        @Child private JSToObjectNode toObject;

        protected InstanceofCheckNode(Class<?> type, JSContext context) {
            this.type = type;
            this.toObject = JSToObjectNode.createToObjectNoCheckNoForeign(context);
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            return type.isInstance(thisObj);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return (DynamicObject) toObject.executeTruffleObject(thisObj);
        }
    }

    protected static final class PrimitiveReceiverCheckNode extends ReceiverCheckNode {
        private final Class<?> type;
        @Child private AbstractShapeCheckNode prototypeShapeCheck;

        protected PrimitiveReceiverCheckNode(Class<?> type, AbstractShapeCheckNode prototypeShapeCheck) {
            this.type = type;
            this.prototypeShapeCheck = prototypeShapeCheck;
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            if (type.isInstance(thisObj)) {
                return prototypeShapeCheck.accept(thisObj);
            } else {
                return false;
            }
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return prototypeShapeCheck.getStore(thisObj);
        }
    }

    protected static final class JavaSuperAdapterCheckNode extends ReceiverCheckNode {
        private final Class<?> type;

        protected JavaSuperAdapterCheckNode(JavaSuperAdapter adapter) {
            this.type = adapter.getAdapter().getClass();
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            return thisObj instanceof JavaSuperAdapter && ((JavaSuperAdapter) thisObj).getAdapter().getClass() == type;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Check the object shape by identity comparison.
     */
    protected static final class ShapeCheckNode extends AbstractShapeCheckNode {

        private final Assumption shapeValidAssumption;

        public ShapeCheckNode(Shape shape) {
            super(shape);
            this.shapeValidAssumption = shape.getValidAssumption();
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            shapeValidAssumption.check();
            return JSObject.isDynamicObject(thisObj) && getShape().check((DynamicObject) thisObj);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return JSObject.castJSObject(thisObj);
        }
    }

    protected abstract static class AbstractAssumptionShapeCheckNode extends AbstractShapeCheckNode {
        protected final JSContext context;

        protected AbstractAssumptionShapeCheckNode(Shape shape, JSContext context) {
            super(shape);
            this.context = context;
        }

        protected final void onShallowPropertyAssumptionFailed() {
            propertyAssumptionCheckFailedCount.inc();
            getPropertyCacheNode().getUninitializedNode().setPropertyAssumptionCheckEnabled(false);
        }

        private PropertyCacheNode<?> getPropertyCacheNode() {
            Node parent = this.getParent();
            while (parent instanceof ReceiverCheckNode) {
                parent = parent.getParent();
            }
            PropertyCacheNode<?> propertyCacheNode = (PropertyCacheNode<?>) parent;
            return propertyCacheNode;
        }

        protected final void assumeSingleRealm() throws InvalidAssumptionException {
            context.assumeSingleRealm();
        }
    }

    /**
     * Check that the given shape is valid and unchanged. Requires that the object is constant.
     *
     * For global object and prototype chain checks only.
     */
    protected static final class AssumptionShapeCheckNode extends AbstractAssumptionShapeCheckNode {

        private final Assumption shapeValidAssumption;
        private final Assumption unchangedShapeAssumption;

        public AssumptionShapeCheckNode(Shape shape, Object key, JSContext context) {
            super(shape, context);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.unchangedShapeAssumption = JSShape.getPropertyAssumption(shape, key);
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            assumeSingleRealm();
            shapeValidAssumption.check();
            try {
                unchangedShapeAssumption.check();
            } catch (InvalidAssumptionException e) {
                onShallowPropertyAssumptionFailed();
                throw e;
            }
            assert thisObj == null || !(JSObject.isDynamicObject(thisObj)) || ((DynamicObject) thisObj).getShape().isRelated(getShape()) : "shapes are not related";
            return true;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return JSObject.castJSObject(thisObj);
        }
    }

    /**
     * Check the shape of the object by identity and the shape of its immediate prototype by
     * assumption (valid and unchanged).
     */
    protected static final class PrototypeShapeCheckNode extends AbstractAssumptionShapeCheckNode {

        private final Assumption notObsoletedAssumption;
        private final Assumption protoNotObsoletedAssumption;
        private final Assumption protoUnchangedAssumption;
        private final DynamicObject prototype;

        public PrototypeShapeCheckNode(Shape shape, DynamicObject thisObj, Object key, JSContext context) {
            super(shape, context);
            this.notObsoletedAssumption = shape.getValidAssumption();

            DynamicObject finalProto = JSObject.getPrototype(thisObj);
            Shape protoShape = finalProto.getShape();
            this.protoNotObsoletedAssumption = protoShape.getValidAssumption();
            this.protoUnchangedAssumption = JSShape.getPropertyAssumption(protoShape, key);
            this.prototype = finalProto;
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            assumeSingleRealm();
            notObsoletedAssumption.check();
            if (JSObject.isDynamicObject(thisObj) && getShape().check((DynamicObject) thisObj)) {
                protoNotObsoletedAssumption.check();
                protoUnchangedAssumption.check();
                return true;
            }
            return false;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return prototype;
        }

        @Override
        public int getDepth() {
            return 1;
        }
    }

    /**
     * Checks the top shape by identity and the shapes of the prototype chain up to the given depth
     * by assumption ({@link AssumptionShapeCheckNode}).
     */
    protected static final class PrototypeChainShapeCheckNode extends AbstractAssumptionShapeCheckNode {

        private final Assumption shapeValidAssumption;
        private final DynamicObject prototype;
        @Children private final AssumptionShapeCheckNode[] shapeCheckNodes;

        public PrototypeChainShapeCheckNode(Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            super(shape, context);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.shapeCheckNodes = new AssumptionShapeCheckNode[depth];

            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                shapeCheckNodes[i] = new AssumptionShapeCheckNode(depthShape, key, context);
            }
            this.prototype = depthProto;
        }

        @ExplodeLoop
        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            assumeSingleRealm();
            shapeValidAssumption.check();
            if (!(JSObject.isDynamicObject(thisObj) && getShape().check((DynamicObject) thisObj))) {
                return false;
            }
            for (int i = 0; i < shapeCheckNodes.length; i++) {
                if (!shapeCheckNodes[i].accept((Object) null)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return prototype;
        }

        @Override
        public int getDepth() {
            return shapeCheckNodes.length;
        }
    }

    /**
     * Checks that the object is constant and the shape by comparison.
     */
    protected static final class ConstantObjectShapeCheckNode extends AbstractShapeCheckNode {
        private final Assumption shapeValidAssumption;
        private final WeakReference<DynamicObject> expectedObjectRef;

        public ConstantObjectShapeCheckNode(Shape shape, DynamicObject thisObj) {
            super(shape);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.expectedObjectRef = new WeakReference<>(thisObj);
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            shapeValidAssumption.check();
            DynamicObject expectedObj = this.expectedObjectRef.get();
            if (thisObj != expectedObj) {
                return false;
            }
            if (expectedObj == null) {
                throw new InvalidAssumptionException();
            }
            return JSObject.isDynamicObject(thisObj) && getShape().check((DynamicObject) thisObj);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return JSObject.castJSObject(thisObj);
        }
    }

    /**
     * Checks that the object is constant and the shape by assumption (valid and unchanged).
     *
     * @see JSTruffleOptions#SkipFinalShapeCheck
     */
    protected static final class ConstantObjectAssumptionShapeCheckNode extends AbstractAssumptionShapeCheckNode {

        private final Assumption shapeValidAssumption;
        private final Assumption unchangedAssumption;
        private final WeakReference<DynamicObject> expectedObjectRef;

        public ConstantObjectAssumptionShapeCheckNode(Shape shape, DynamicObject thisObj, Object key, JSContext context) {
            super(shape, context);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.unchangedAssumption = JSShape.getPropertyAssumption(shape, key);
            this.expectedObjectRef = new WeakReference<>(thisObj);
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            assumeSingleRealm();
            shapeValidAssumption.check();
            DynamicObject expectedObj = this.expectedObjectRef.get();
            if (thisObj != expectedObj) {
                return false;
            }
            if (expectedObj == null) {
                throw new InvalidAssumptionException();
            }
            try {
                unchangedAssumption.check();
            } catch (InvalidAssumptionException e) {
                onShallowPropertyAssumptionFailed();
                throw e;
            }
            return true;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return JSObject.castJSObject(thisObj);
        }
    }

    /**
     * Checks that the object is constant and all the shapes of the prototype chain up to the given
     * depth by assumption (valid and unchanged).
     *
     * @see JSTruffleOptions#SkipFinalShapeCheck
     */
    protected static final class ConstantObjectPrototypeChainShapeCheckNode extends AbstractAssumptionShapeCheckNode {

        private final Assumption shapeValidAssumption;
        private final Assumption shapeUnchangedAssumption;
        private final WeakReference<DynamicObject> expectedObjectRef;
        private final WeakReference<DynamicObject> prototype;
        @Children private final AssumptionShapeCheckNode[] shapeCheckNodes;

        public ConstantObjectPrototypeChainShapeCheckNode(Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            super(shape, context);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.shapeUnchangedAssumption = JSShape.getPropertyAssumption(shape, key);
            this.expectedObjectRef = new WeakReference<>(thisObj);
            this.shapeCheckNodes = new AssumptionShapeCheckNode[depth];

            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                shapeCheckNodes[i] = new AssumptionShapeCheckNode(depthShape, key, context);
            }
            this.prototype = new WeakReference<>(depthProto);
        }

        @ExplodeLoop
        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            assumeSingleRealm();
            shapeValidAssumption.check();
            DynamicObject expectedObj = this.expectedObjectRef.get();
            if (thisObj != expectedObj) {
                return false;
            }
            if (expectedObj == null) {
                throw new InvalidAssumptionException();
            }
            assert this.prototype.get() != null;
            try {
                shapeUnchangedAssumption.check();
            } catch (InvalidAssumptionException e) {
                onShallowPropertyAssumptionFailed();
                throw e;
            }
            for (int i = 0; i < shapeCheckNodes.length; i++) {
                if (!shapeCheckNodes[i].accept((Object) null)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return prototype.get();
        }

        @Override
        public int getDepth() {
            return shapeCheckNodes.length;
        }
    }

    /**
     * Checks that the object is constant and the shape of the object and its immediate prototype by
     * assumption (valid and unchanged).
     *
     * @see JSTruffleOptions#SkipFinalShapeCheck
     */
    protected static final class ConstantObjectPrototypeShapeCheckNode extends AbstractAssumptionShapeCheckNode {

        private final Assumption shapeValidAssumption;
        private final Assumption unchangedAssumption;
        private final Assumption protoShapeValidAssumption;
        private final Assumption protoUnchangedAssumption;
        private final WeakReference<DynamicObject> expectedObjectRef;
        private final WeakReference<DynamicObject> prototype;

        public ConstantObjectPrototypeShapeCheckNode(Shape shape, DynamicObject thisObj, Object key, JSContext context) {
            super(shape, context);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.unchangedAssumption = JSShape.getPropertyAssumption(shape, key);

            DynamicObject finalProto = JSObject.getPrototype(thisObj);
            Shape protoShape = finalProto.getShape();
            this.protoShapeValidAssumption = protoShape.getValidAssumption();
            this.protoUnchangedAssumption = JSShape.getPropertyAssumption(protoShape, key);
            this.expectedObjectRef = new WeakReference<>(thisObj);
            this.prototype = new WeakReference<>(finalProto);
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            assumeSingleRealm();
            shapeValidAssumption.check();
            DynamicObject expectedObj = this.expectedObjectRef.get();
            if (thisObj != expectedObj) {
                return false;
            }
            if (expectedObj == null) {
                throw new InvalidAssumptionException();
            }
            assert this.prototype.get() != null;
            try {
                unchangedAssumption.check();
            } catch (InvalidAssumptionException e) {
                onShallowPropertyAssumptionFailed();
                throw e;
            }
            protoShapeValidAssumption.check();
            protoUnchangedAssumption.check();
            return true;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return prototype.get();
        }

        @Override
        public int getDepth() {
            return 1;
        }
    }

    /**
     * Check the shapes of the prototype chain up to the given depth.
     *
     * This class actually traverses the prototype chain and checks each prototype shape's identity.
     *
     * @see JSTruffleOptions#SkipPrototypeShapeCheck
     */
    protected static final class TraversePrototypeChainShapeCheckNode extends AbstractShapeCheckNode {

        private final Assumption shapeValidAssumption;
        @Children private final ShapeCheckNode[] shapeCheckNodes;
        @Children private final GetPrototypeNode[] getPrototypeNodes;

        public TraversePrototypeChainShapeCheckNode(Shape shape, DynamicObject thisObj, int depth) {
            super(shape);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.shapeCheckNodes = new ShapeCheckNode[depth];
            this.getPrototypeNodes = new GetPrototypeNode[depth];

            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                shapeCheckNodes[i] = new ShapeCheckNode(depthShape);
                getPrototypeNodes[i] = GetPrototypeNode.create();
            }
        }

        @ExplodeLoop
        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            shapeValidAssumption.check();
            if (!(JSObject.isDynamicObject(thisObj) && getShape().check((DynamicObject) thisObj))) {
                return false;
            }
            DynamicObject current = (DynamicObject) thisObj;
            for (int i = 0; i < shapeCheckNodes.length; i++) {
                current = getPrototypeNodes[i].executeDynamicObject(current);
                if (!shapeCheckNodes[i].accept(current)) {
                    return false;
                }
            }
            return true;
        }

        @ExplodeLoop
        @Override
        public DynamicObject getStore(Object thisObj) {
            DynamicObject proto = (DynamicObject) thisObj;
            for (int i = 0; i < shapeCheckNodes.length; i++) {
                proto = getPrototypeNodes[i].executeDynamicObject(proto);
            }
            return proto;
        }

        @Override
        public int getDepth() {
            return shapeCheckNodes.length;
        }
    }

    /**
     * Check the shapes of the object and its immediate prototype.
     *
     * This class actually reads the prototype and checks the prototype shape's identity.
     *
     * @see JSTruffleOptions#SkipPrototypeShapeCheck
     */
    protected static final class TraversePrototypeShapeCheckNode extends AbstractShapeCheckNode {

        private final Assumption shapeValidAssumption;
        @Child private ShapeCheckNode protoShapeCheck;
        @Child private GetPrototypeNode getPrototypeNode;

        public TraversePrototypeShapeCheckNode(Shape shape, DynamicObject thisObj) {
            super(shape);
            this.shapeValidAssumption = shape.getValidAssumption();
            this.protoShapeCheck = new ShapeCheckNode(JSObject.getPrototype(thisObj).getShape());
            this.getPrototypeNode = GetPrototypeNode.create();
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            shapeValidAssumption.check();
            return JSObject.isDynamicObject(thisObj) && getShape().check((DynamicObject) thisObj) && protoShapeCheck.accept(getPrototypeNode.executeDynamicObject((DynamicObject) thisObj));
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return JSObject.getPrototype((DynamicObject) thisObj);
        }

        @Override
        public int getDepth() {
            return 1;
        }
    }

    /**
     * Checks the shapes of the prototype chain up to the given depth by assumption (
     * {@link AssumptionShapeCheckNode}).
     */
    protected static final class PrototypeChainCheckNode extends AbstractAssumptionShapeCheckNode {
        private final DynamicObject prototype;
        @Children private final AssumptionShapeCheckNode[] shapeCheckNodes;

        public PrototypeChainCheckNode(Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            super(shape, context);
            assert depth >= 1;
            this.shapeCheckNodes = new AssumptionShapeCheckNode[depth];

            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                shapeCheckNodes[i] = new AssumptionShapeCheckNode(depthShape, key, context);
            }
            this.prototype = depthProto;
        }

        @ExplodeLoop
        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            assumeSingleRealm();
            for (int i = 0; i < shapeCheckNodes.length; i++) {
                if (!shapeCheckNodes[i].accept((Object) null)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return prototype;
        }

        @Override
        public int getDepth() {
            return shapeCheckNodes.length;
        }
    }

    /**
     * Check the shapes of the prototype chain up to the given depth.
     *
     * This class actually traverses the prototype chain and checks each prototype shape's identity.
     *
     * @see JSTruffleOptions#SkipPrototypeShapeCheck
     */
    protected static final class TraversePrototypeChainCheckNode extends AbstractShapeCheckNode {
        private final JSContext context;
        private final JSClass jsclass;
        @Children private final ShapeCheckNode[] shapeCheckNodes;
        @Children private final GetPrototypeNode[] getPrototypeNodes;

        public TraversePrototypeChainCheckNode(Shape shape, DynamicObject thisObj, int depth, JSClass jsclass, JSContext context) {
            super(shape);
            assert depth >= 1;
            this.context = context;
            this.jsclass = jsclass;
            this.shapeCheckNodes = new ShapeCheckNode[depth];
            this.getPrototypeNodes = new GetPrototypeNode[depth - 1];

            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                shapeCheckNodes[i] = new ShapeCheckNode(depthShape);
                if (i < depth - 1) {
                    getPrototypeNodes[i] = GetPrototypeNode.create();
                }
            }
        }

        @ExplodeLoop
        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            DynamicObject current = jsclass.getIntrinsicDefaultProto(context.getRealm());
            for (int i = 0; i < shapeCheckNodes.length; i++) {
                if (!shapeCheckNodes[i].accept(current)) {
                    return false;
                }
                if (i < shapeCheckNodes.length - 1) {
                    current = getPrototypeNodes[i].executeDynamicObject(current);
                }
            }
            return true;
        }

        @ExplodeLoop
        @Override
        public DynamicObject getStore(Object thisObj) {
            DynamicObject proto = jsclass.getIntrinsicDefaultProto(context.getRealm());
            for (int i = 0; i < getPrototypeNodes.length; i++) {
                proto = getPrototypeNodes[i].executeDynamicObject(proto);
            }
            return proto;
        }

        @Override
        public int getDepth() {
            return shapeCheckNodes.length;
        }
    }

    protected static final class JSClassCheckNode extends ReceiverCheckNode {
        private final JSClass jsclass;

        protected JSClassCheckNode(JSClass jsclass) {
            this.jsclass = jsclass;
        }

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            return JSClass.isInstance(thisObj, jsclass);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return (DynamicObject) thisObj;
        }
    }

    protected static final class ForeignLanguageCheckNode extends ReceiverCheckNode {

        @Override
        public boolean accept(Object thisObj) throws InvalidAssumptionException {
            return JSRuntime.isForeignObject(thisObj);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            throw new UnsupportedOperationException();
        }
    }

    // ---

    protected final Object key;
    @CompilationFinal private Assumption invalidationAssumption;

    public PropertyCacheNode(Object key) {
        this.key = key;
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey;
    }

    @TruffleBoundary
    public String debugString() {
        return getClass().getSimpleName() + "<property=" + key + ">";
    }

    public final Object getKey() {
        return key;
    }

    protected abstract T getNext();

    protected abstract void setNext(T next);

    protected abstract Shape getShape();

    protected abstract T createGenericPropertyNode(JSContext context);

    protected abstract T createCachedPropertyNode(Property entry, Object thisObj, int depth, JSContext context, Object value);

    protected abstract T createUndefinedPropertyNode(Object thisObject, Object store, int depth, JSContext context, Object value);

    protected abstract T createJavaPropertyNodeMaybe(Object thisObj, int depth, JSContext context);

    protected abstract T createTruffleObjectPropertyNode(TruffleObject thisObj, JSContext context);

    /**
     * Rewrite this, presumably to a cached version, with the given target.
     *
     * @param thisObject The target object
     *
     * @return The replacement node
     */
    protected final T rewrite(JSContext context, Object thisObject, Object value) {
        if (invalidationAssumption == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
        } else {
            if (CompilerDirectives.inCompiledCode()) {
                try {
                    invalidationAssumption.check();
                } catch (InvalidAssumptionException e) {
                }
            }
            /*
             * Do not invalidate code here, since we might just re-shape the object (which does not
             * need a modification of the AST).
             */
            CompilerDirectives.transferToInterpreter();
        }

        Lock lock = getLock();
        lock.lock();
        try {
            int depth = 0;
            T specialized = null;

            DynamicObject store = null;
            if (JSObject.isJSObject(thisObject)) {
                if (!JSAdapter.isJSAdapter(thisObject) && !JSProxy.isProxy(thisObject)) {
                    store = (DynamicObject) thisObject;
                }
            } else if (JSRuntime.isForeignObject(thisObject)) {
                assert !JSObject.isJSObject(thisObject);
                specialized = createTruffleObjectPropertyNode((TruffleObject) thisObject, context);
            } else {
                store = wrapPrimitive(thisObject, context);
            }

            while (store != null) {
                // check for obsolete shape
                if (store.updateShape()) {
                    return retryCache();
                }

                // otherwise, we need to specialize
                Shape cacheShape = store.getShape();

                if (JSTruffleOptions.DictionaryObject && JSDictionaryObject.isJSDictionaryObject(store)) {
                    // TODO: could probably specialize on shape as well.
                    // replace the entire cache with the generic case
                    T top = getTopCache();
                    invalidateCache();
                    return top.replace(createGenericPropertyNode(context), "dictionary object");
                }

                if (JSTruffleOptions.MergeShapes) {
                    // check if we're creating unnecessary polymorphism due to compatible types
                    synchronized (store.getShape().getMutex()) {
                        if (hasUpcastShapes(cacheShape)) {
                            store.updateShape();
                            return retryCache();
                        }
                    }
                }

                // specialize on what we got
                Property property = cacheShape.getProperty(key);
                if (property != null) {
                    specialized = createCachedPropertyNode(property, thisObject, depth, context, value);
                    break;
                } else if (JSProxy.isProxy(store)) {
                    specialized = createUndefinedPropertyNode(thisObject, store, depth, context, value);
                    break;
                } else if (isOwnProperty()) {
                    break;
                }

                store = (DynamicObject) JSRuntime.toJavaNull(JSObject.getPrototype(store));
                if (store != null) {
                    depth++;
                }
            }

            // check if we're at the generic limit
            int poly = getCachePolymorphism();

            if (poly >= JSTruffleOptions.PropertyCacheLimit) {
                return rewriteToGeneric(context);
            }

            // maybe this is totally undefined
            if (specialized == null) {
                specialized = createUndefinedPropertyNode(thisObject, thisObject, depth, context, value);
            }

            // specialize with the given node
            T topCache = getTopCache();
            specialized.setNext(topCache);
            invalidateCache();
            topCache.replace(specialized, reasonResolved(key, poly));

            if (poly > 0) {
                polymorphicCount.inc();
                if (JSTruffleOptions.TracePolymorphicPropertyAccess) {
                    System.out.printf("POLYMORPHIC PROPERTY ACCESS %s\n%s\n---\n", getEncapsulatingSourceSection(), getTopCache().debugString());
                }
            }

            return specialized;
        } finally {
            lock.unlock();
        }
    }

    private T retryCache() {
        if (invalidationAssumption == null) {
            invalidationAssumption = Truffle.getRuntime().createAssumption("PropertyCacheNode");
            cacheAssumptionInitializedCount.inc();
        }
        return getTopCache();
    }

    private void invalidateCache() {
        if (invalidationAssumption != null) {
            invalidationAssumption.invalidate();
            invalidationAssumption = Truffle.getRuntime().createAssumption("PropertyCacheNode");
            cacheAssumptionInvalidatedCount.inc();
        }
    }

    private T rewriteToGeneric(JSContext context) {
        megamorphicCount.inc();
        if (JSTruffleOptions.TraceMegamorphicPropertyAccess) {
            System.out.printf("MEGAMORPHIC PROPERTY ACCESS %s\n%s\n---\n", getEncapsulatingSourceSection(), getTopCache().debugString());
        }

        // replace the entire cache with the generic case
        T top = getTopCache();
        invalidateCache();
        return top.replace(createGenericPropertyNode(context), reasonCacheLimit(key));
    }

    protected static final DynamicObject wrapPrimitive(Object thisObject, JSContext context) {
        // wrap primitives for lookup
        return JSRuntime.toObjectFromPrimitive(context, thisObject, false);
    }

    /**
     * Get the top-level cache node for this.
     */
    private T getTopCache() {
        Class<T> base = getBaseClass();
        T cur = base.cast(this);
        while (base.isInstance(cur.getParent()) && base.cast(cur.getParent()).getNext() == cur) {
            cur = base.cast(cur.getParent());
        }
        return cur;
    }

    /**
     * Get the degree of polymorphism for this inline cache.
     *
     * @return The degree of polymorphism
     */
    private int getCachePolymorphism() {
        Class<T> base = getBaseClass();
        int poly = 0;
        T cur = base.cast(this);
        while (base.isInstance(cur.getParent()) && base.cast(cur.getParent()).getNext() == cur) {
            cur = getBaseClass().cast(cur.getParent());
            poly++;
        }
        return poly;
    }

    protected abstract Class<T> getBaseClass();

    protected abstract Class<? extends T> getUninitializedNodeClass();

    protected final T getUninitializedNode() {
        PropertyCacheNode<T> uninitialized = this;
        while (!getUninitializedNodeClass().isInstance(uninitialized)) {
            uninitialized = uninitialized.getNext();
        }
        return getUninitializedNodeClass().cast(uninitialized);
    }

    protected boolean isPropertyAssumptionCheckEnabled() {
        throw new UnsupportedOperationException();
    }

    protected void setPropertyAssumptionCheckEnabled(@SuppressWarnings("unused") boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Does the given map relate to any of the cached maps by upcasting? If so, obsolete the
     * downcast map.
     *
     * @param cacheShape The new map to check against
     * @return true if a map was obsoleted
     */
    private boolean hasUpcastShapes(Shape cacheShape) {
        assert cacheShape.isValid();
        boolean result = false;
        T cur = getBaseClass().cast(this);
        while (getBaseClass().isInstance(cur.getParent())) {
            cur = getBaseClass().cast(cur.getParent());
            Shape other = cur.getShape();
            if (cacheShape != other && other != null && other.isValid()) {
                assert cacheShape.isValid();
                result |= cacheShape.tryMerge(other) != null;
                if (!cacheShape.isValid()) {
                    break;
                }
            }
        }
        return result;
    }

    protected final AbstractShapeCheckNode createShapeCheckNode(Shape shape, DynamicObject thisObj, int depth, boolean isConstantObjectFinal, boolean isDefine) {
        if (depth == 0) {
            return createShapeCheckNodeDepth0(shape, thisObj, isConstantObjectFinal, isDefine);
        } else if (depth == 1) {
            return createShapeCheckNodeDepth1(shape, thisObj, depth, isConstantObjectFinal);
        } else {
            return createShapeCheckNodeDeeper(shape, thisObj, depth, isConstantObjectFinal);
        }
    }

    private AbstractShapeCheckNode createShapeCheckNodeDepth0(Shape shape, DynamicObject thisObj, boolean isConstantObjectFinal, boolean isDefine) {
        // if isDefine is true, shape change is imminent, so don't use assumption
        if (isGlobal() && JSTruffleOptions.SkipGlobalShapeCheck && !isDefine && isPropertyAssumptionCheckEnabled() && JSShape.getPropertyAssumption(shape, key).isValid()) {
            return new AssumptionShapeCheckNode(shape, key, getContext());
        } else if (isConstantObjectFinal) {
            assert !isDefine;
            if (isPropertyAssumptionCheckEnabled() && JSShape.getPropertyAssumption(shape, key).isValid()) {
                return new ConstantObjectAssumptionShapeCheckNode(shape, thisObj, key, getContext());
            } else {
                return new ConstantObjectShapeCheckNode(shape, thisObj);
            }
        } else {
            assert !isConstantObjectFinal;
            return new ShapeCheckNode(shape);
        }
    }

    private AbstractShapeCheckNode createShapeCheckNodeDepth1(Shape shape, DynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        assert depth == 1;
        if (JSTruffleOptions.SkipPrototypeShapeCheck && prototypesInShape(thisObj, depth) && propertyAssumptionsValid(thisObj, depth, isConstantObjectFinal)) {
            return isConstantObjectFinal
                            ? new ConstantObjectPrototypeShapeCheckNode(shape, thisObj, key, getContext())
                            : new PrototypeShapeCheckNode(shape, thisObj, key, getContext());
        } else {
            return new TraversePrototypeShapeCheckNode(shape, thisObj);
        }
    }

    private AbstractShapeCheckNode createShapeCheckNodeDeeper(Shape shape, DynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        assert depth > 1;
        if (JSTruffleOptions.SkipPrototypeShapeCheck && prototypesInShape(thisObj, depth) && propertyAssumptionsValid(thisObj, depth, isConstantObjectFinal)) {
            return isConstantObjectFinal
                            ? new ConstantObjectPrototypeChainShapeCheckNode(shape, thisObj, key, depth, getContext())
                            : new PrototypeChainShapeCheckNode(shape, thisObj, key, depth, getContext());
        } else {
            return new TraversePrototypeChainShapeCheckNode(shape, thisObj, depth);
        }
    }

    protected static boolean prototypesInShape(DynamicObject thisObj, int depth) {
        DynamicObject depthObject = thisObj;
        for (int i = 0; i < depth; i++) {
            if (!JSShape.isPrototypeInShape(depthObject.getShape())) {
                return false;
            }
            depthObject = JSObject.getPrototype(depthObject);
        }
        return true;
    }

    protected final boolean propertyAssumptionsValid(DynamicObject thisObj, int depth, boolean checkDepth0) {
        if (!getContext().isSingleRealm()) {
            return false;
        }
        DynamicObject depthObject = thisObj;
        if (checkDepth0 && !JSShape.getPropertyAssumption(depthObject.getShape(), key).isValid()) {
            return false;
        }
        for (int i = 0; i < depth; i++) {
            depthObject = JSObject.getPrototype(depthObject);
            if (!JSShape.getPropertyAssumption(depthObject.getShape(), key).isValid()) {
                return false;
            }
        }
        return true;
    }

    protected final ReceiverCheckNode createPrimitiveReceiverCheck(Object thisObj, int depth, JSContext context) {
        if (depth == 0) {
            return new InstanceofCheckNode(thisObj.getClass(), context);
        } else {
            assert JSRuntime.isJSPrimitive(thisObj);
            DynamicObject wrapped = wrapPrimitive(thisObj, context);
            AbstractShapeCheckNode prototypeShapeCheck;
            if (JSTruffleOptions.SkipPrototypeShapeCheck && prototypesInShape(wrapped, depth) && propertyAssumptionsValid(wrapped, depth, false)) {
                prototypeShapeCheck = new PrototypeChainCheckNode(wrapped.getShape(), wrapped, key, depth, context);
            } else {
                prototypeShapeCheck = new TraversePrototypeChainCheckNode(wrapped.getShape(), wrapped, depth, JSObject.getJSClass(wrapped), context);
            }
            return new PrimitiveReceiverCheckNode(thisObj.getClass(), prototypeShapeCheck);
        }
    }

    protected abstract boolean isGlobal();

    protected abstract boolean isOwnProperty();

    public abstract JSContext getContext();

    protected static boolean isArrayLengthProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSArray.ArrayLengthProxyProperty;
    }

    protected static boolean isClassPrototypeProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSFunction.ClassPrototypeProxyProperty;
    }

    protected static boolean isStringLengthProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSString.StringLengthProxyProperty;
    }

    protected static boolean isLazyRegexResultIndexProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSRegExp.LazyRegexResultIndexProxyProperty;
    }

    protected static boolean isLazyNamedCaptureGroupProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSRegExp.LazyNamedCaptureGroupProperty;
    }

    static CharSequence reasonShapeAssumptionInvalidated(Object key) {
        CompilerAsserts.neverPartOfCompilation();
        if (TruffleOptions.TraceRewrites) {
            return String.format("shape assumption invalidated (property %s)", key);
        }
        return "shape assumption invalidated";
    }

    static CharSequence reasonFinalAssumptionInvalidated(Object key) {
        CompilerAsserts.neverPartOfCompilation();
        if (TruffleOptions.TraceRewrites) {
            return String.format("final assumption invalidated (property %s)", key);
        }
        return "final assumption invalidated";
    }

    static CharSequence reasonResolved(Object key, int cacheDepth) {
        CompilerAsserts.neverPartOfCompilation();
        if (TruffleOptions.TraceRewrites) {
            return String.format("resolved property %s (%d/%d)", key, cacheDepth, JSTruffleOptions.PropertyCacheLimit);
        }
        return "resolved property";
    }

    static CharSequence reasonCacheLimit(Object key) {
        CompilerAsserts.neverPartOfCompilation();
        if (TruffleOptions.TraceRewrites) {
            return String.format("reached cache limit (property %s, limit %d)", key, JSTruffleOptions.PropertyCacheLimit);
        }
        return "reached cache limit";
    }

    @TruffleBoundary
    protected String getAccessorKey(String getset) {
        assert JSRuntime.isString(getKey());
        String origKey = getKey() instanceof String ? (String) getKey() : ((JSLazyString) getKey()).toString();
        if (origKey.length() > 0 && Character.isLetter(origKey.charAt(0))) {
            String accessorKey = getset + origKey.substring(0, 1).toUpperCase() + origKey.substring(1);
            return accessorKey;
        }
        return null;
    }

    private static final DebugCounter polymorphicCount = DebugCounter.create("Polymorphic property cache count");
    private static final DebugCounter megamorphicCount = DebugCounter.create("Megamorphic property cache count");
    private static final DebugCounter cacheAssumptionInitializedCount = DebugCounter.create("Property cache assumptions initialized");
    private static final DebugCounter cacheAssumptionInvalidatedCount = DebugCounter.create("Property cache assumptions invalidated");
    private static final DebugCounter propertyAssumptionCheckFailedCount = DebugCounter.create("Property assumption checks failed");
}
