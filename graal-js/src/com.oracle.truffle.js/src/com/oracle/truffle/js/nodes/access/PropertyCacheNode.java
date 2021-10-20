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

import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDictionary;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DebugCounter;

/**
 * Common base class for property cache nodes. Unifies the cache handling and receiver checks.
 *
 * @see PropertyGetNode
 * @see PropertySetNode
 * @see HasPropertyCacheNode
 */
public abstract class PropertyCacheNode<T extends PropertyCacheNode.CacheNode<T>> extends JavaScriptBaseNode {
    /**
     * Checks whether the receiver can be handled by the corresponding specialization.
     */
    protected abstract static class ReceiverCheckNode extends JavaScriptBaseNode {

        protected final Shape shape;

        protected ReceiverCheckNode(Shape shape) {
            this.shape = shape;
        }

        /**
         * Check receiver shape, class, or instance.
         *
         * @return whether the object is supported by the associated property cache node.
         */
        public abstract boolean accept(Object thisObj);

        /**
         * @return the object that contains the property.
         */
        public abstract DynamicObject getStore(Object thisObj);

        public final Shape getShape() {
            return shape;
        }

        /**
         * Checks if all required assumptions are valid.
         */
        public boolean isValid() {
            return true;
        }

        /**
         * @return true if a stable property assumption failed.
         */
        protected boolean isUnstable() {
            return false;
        }

        @Override
        public final NodeCost getCost() {
            return NodeCost.NONE;
        }
    }

    /**
     * Checks if the obj is an instance of the shape's layout class in compiled code and
     * <code>obj instanceof {@link JSDynamicObject}</code> in the interpreter.
     */
    protected static boolean isDynamicObject(Object obj, Shape shape) {
        if (CompilerDirectives.inCompiledCode()) {
            return shape.getLayoutClass().isInstance(obj);
        } else {
            return obj instanceof JSDynamicObject;
        }
    }

    /**
     * Casts the obj to the shape's layout class in compiled code and to {@link JSDynamicObject} in
     * the interpreter.
     */
    protected static DynamicObject castDynamicObject(Object obj, Shape shape) {
        if (CompilerDirectives.inCompiledCode()) {
            return shape.getLayoutClass().cast(obj);
        } else {
            return (JSDynamicObject) obj;
        }
    }

    /**
     * Checks whether the receiver is compatible with one of the two shapes. Assumes shapes with
     * compatible layout.
     */
    protected static class CombinedShapeCheckNode extends ReceiverCheckNode {

        private final Shape shape1;
        private final Shape shape2;

        CombinedShapeCheckNode(Shape shape1, Shape shape2) {
            super(null);
            assert shape1.getLayoutClass() == shape2.getLayoutClass();
            this.shape1 = shape1;
            this.shape2 = shape2;
        }

        @Override
        public boolean accept(Object thisObj) {
            if (isDynamicObject(thisObj, shape1)) {
                DynamicObject castObj = castDynamicObject(thisObj, shape1);
                return (shape1.check(castObj) || shape2.check(castObj));
            } else {
                return false;
            }
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return castDynamicObject(thisObj, shape1);
        }
    }

    /**
     * Checks the {@link Shape} of a {@link DynamicObject}.
     */
    protected abstract static class AbstractShapeCheckNode extends ReceiverCheckNode {

        protected AbstractShapeCheckNode(Shape shape) {
            super(shape);
        }

        /**
         * @return the {@link DynamicObject} that contains the property.
         */
        @Override
        public abstract DynamicObject getStore(Object thisObj);

        @Override
        public boolean accept(Object thisObj) {
            if (isDynamicObject(thisObj, shape)) {
                return shape.check(castDynamicObject(thisObj, shape));
            } else {
                return false;
            }
        }

        public int getDepth() {
            return 0;
        }

        @Override
        public abstract boolean isValid();
    }

    protected static final class NullCheckNode extends ReceiverCheckNode {

        protected NullCheckNode() {
            super(null);
        }

        @Override
        public boolean accept(Object thisObj) {
            return thisObj == null;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            throw Errors.shouldNotReachHere();
        }
    }

    protected static final class InstanceofCheckNode extends ReceiverCheckNode {
        protected final Class<?> type;

        protected InstanceofCheckNode(Class<?> type) {
            super(null);
            this.type = type;
        }

        @Override
        public boolean accept(Object thisObj) {
            return CompilerDirectives.isExact(thisObj, type);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            throw Errors.shouldNotReachHere();
        }
    }

    /**
     * Check the object shape by identity comparison.
     */
    protected static final class ShapeCheckNode extends AbstractShapeCheckNode {

        public ShapeCheckNode(Shape shape) {
            super(shape);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return castDynamicObject(thisObj, shape);
        }

        @Override
        public boolean isValid() {
            return shape.getValidAssumption().isValid();
        }
    }

    protected abstract static class AbstractSingleRealmShapeCheckNode extends AbstractShapeCheckNode {
        @CompilationFinal(dimensions = 1) protected final Assumption[] assumptions;

        protected AbstractSingleRealmShapeCheckNode(Shape shape, Assumption[] assumptions, JSContext context) {
            super(shape);
            this.assumptions = assumptions;
            assert !context.isMultiContext();
        }

        @ExplodeLoop
        @Override
        public final boolean isValid() {
            for (Assumption assumption : assumptions) {
                if (!assumption.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    protected abstract static class AbstractFinalPrototypeShapeCheckNode extends AbstractSingleRealmShapeCheckNode {
        private final DynamicObject prototype;

        protected AbstractFinalPrototypeShapeCheckNode(Shape shape, Assumption[] assumptions, DynamicObject prototype, JSContext context) {
            super(shape, assumptions, context);
            this.prototype = prototype;
        }

        @Override
        public final DynamicObject getStore(Object thisObj) {
            return prototype;
        }
    }

    /**
     * Check the shape of the object by identity and the shape of its immediate prototype by
     * assumption (valid and unchanged).
     */
    protected static final class PrototypeShapeCheckNode extends AbstractFinalPrototypeShapeCheckNode {

        private PrototypeShapeCheckNode(Shape shape, Assumption[] assumptions, DynamicObject prototype, JSContext context) {
            super(shape, assumptions, prototype, context);

        }

        static AbstractShapeCheckNode create(Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            assert depth == 1; // This specialization must only be used for prototype depth 1.
            Assumption[] ass = new Assumption[3];
            int pos = 0;
            ass[pos++] = shape.getValidAssumption();
            DynamicObject finalProto = JSObject.getPrototype(thisObj);
            Shape protoShape = finalProto.getShape();
            ass[pos++] = protoShape.getValidAssumption();
            ass[pos++] = JSShape.getPropertyAssumption(protoShape, key, true);
            assert pos == ass.length;
            return new PrototypeShapeCheckNode(shape, ass, finalProto, context);
        }

        @Override
        public int getDepth() {
            return 1;
        }
    }

    /**
     * Checks the top shape by identity and the shapes of the prototype chain up to the given depth
     * using assumptions only (valid and property unchanged).
     */
    protected static final class PrototypeChainShapeCheckNode extends AbstractFinalPrototypeShapeCheckNode {

        private PrototypeChainShapeCheckNode(Shape shape, Assumption[] assumptions, DynamicObject prototype, JSContext context) {
            super(shape, assumptions, prototype, context);
        }

        static AbstractShapeCheckNode create(Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            Assumption[] ass = new Assumption[1 + (depth == 0 ? 0 : depth * 3 - 1)];
            int pos = 0;
            ass[pos++] = shape.getValidAssumption();
            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                Assumption stablePrototypeAssumption = i == 0 ? null : JSShape.getPrototypeAssumption(depthShape);
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                ass[pos++] = depthShape.getValidAssumption();
                ass[pos++] = JSShape.getPropertyAssumption(depthShape, key, true);
                if (stablePrototypeAssumption != null) {
                    ass[pos++] = stablePrototypeAssumption;
                }
            }
            assert pos == ass.length;
            return new PrototypeChainShapeCheckNode(shape, ass, depthProto, context);
        }

        @Override
        public int getDepth() {
            return assumptions.length / 3;
        }
    }

    /**
     * Check that the given shape is valid and the property is unchanged using assumptions only.
     *
     * Requires that the object is constant. Used e.g. for stable global object property accesses.
     *
     * @see JSConfig#SkipFinalShapeCheck
     * @see JSConfig#SkipGlobalShapeCheck
     */
    protected static final class ConstantObjectAssumptionShapeCheckNode extends AbstractSingleRealmShapeCheckNode {

        private ConstantObjectAssumptionShapeCheckNode(Shape shape, Assumption[] assumptions, JSContext context) {
            super(shape, assumptions, context);
        }

        static AbstractShapeCheckNode create(Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            assert thisObj != null;
            assert depth == 0;
            Assumption[] ass = new Assumption[2];
            int pos = 0;
            ass[pos++] = shape.getValidAssumption();
            ass[pos++] = JSShape.getPropertyAssumption(shape, key);
            assert pos == ass.length;
            return new ConstantObjectAssumptionShapeCheckNode(shape, ass, context);
        }

        @Override
        public boolean accept(Object thisObj) {
            return true;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return ((JSDynamicObject) thisObj);
        }

        @Override
        protected boolean isUnstable() {
            return shape.isValid() && !assumptions[1].isValid();
        }
    }

    /**
     * Checks that the given shape and all prototype shapes up to depth are valid, that the property
     * is unchanged in these shapes, and that all prototypes are stable, all using assumptions only.
     *
     * @see JSConfig#SkipFinalShapeCheck
     * @see JSConfig#SkipPrototypeShapeCheck
     */
    protected static final class ConstantObjectPrototypeChainShapeCheckNode extends AbstractSingleRealmShapeCheckNode {

        private final WeakReference<DynamicObject> prototype;

        private ConstantObjectPrototypeChainShapeCheckNode(Shape shape, Assumption[] assumptions, DynamicObject prototype, JSContext context) {
            super(shape, assumptions, context);
            this.prototype = new WeakReference<>(prototype);
        }

        static AbstractShapeCheckNode create(Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            Assumption[] ass = new Assumption[2 + depth * 3];
            int pos = 0;
            ass[pos++] = shape.getValidAssumption();
            ass[pos++] = JSShape.getPropertyAssumption(shape, key);
            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                Assumption stablePrototypeAssumption = JSShape.getPrototypeAssumption(depthShape);
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                ass[pos++] = depthShape.getValidAssumption();
                ass[pos++] = JSShape.getPropertyAssumption(depthShape, key, true);
                ass[pos++] = stablePrototypeAssumption;
            }
            assert pos == ass.length;
            return new ConstantObjectPrototypeChainShapeCheckNode(shape, ass, depthProto, context);
        }

        @Override
        public boolean accept(Object thisObj) {
            assert this.prototype.get() != null;
            return true;
        }

        /**
         * Returns a constant reference to the resolved prototype.
         *
         * The prototype is held in as a weak reference that is guaranteed to be non-null if the
         * associated constant object specialization matches the expected object.
         */
        @Override
        public DynamicObject getStore(Object thisObj) {
            return prototype.get();
        }

        @Override
        public int getDepth() {
            return assumptions.length / 3;
        }

        @Override
        protected boolean isUnstable() {
            return shape.isValid() && !assumptions[1].isValid();
        }
    }

    /**
     * Check the shapes of the prototype chain up to the given depth.
     *
     * This class actually traverses the prototype chain and checks each prototype shape's identity.
     *
     * @see JSConfig#SkipPrototypeShapeCheck
     */
    protected static final class TraversePrototypeChainShapeCheckNode extends AbstractShapeCheckNode {

        @CompilationFinal(dimensions = 1) private final Shape[] protoShapes;
        @Children private final GetPrototypeNode[] getPrototypeNodes;

        public TraversePrototypeChainShapeCheckNode(Shape shape, DynamicObject thisObj, int depth) {
            super(shape);
            this.protoShapes = new Shape[depth];
            this.getPrototypeNodes = new GetPrototypeNode[depth];

            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                protoShapes[i] = depthShape;
                getPrototypeNodes[i] = GetPrototypeNode.create();
            }
        }

        @ExplodeLoop
        @Override
        public boolean accept(Object thisObj) {
            if (!isDynamicObject(thisObj, shape)) {
                return false;
            }
            DynamicObject current = castDynamicObject(thisObj, shape);
            boolean result = getShape().check(current);
            if (!result) {
                return false;
            }
            Shape[] shapeArray = protoShapes;
            GetPrototypeNode[] getPrototypeArray = getPrototypeNodes;
            for (int i = 0; i < shapeArray.length; i++) {
                current = getPrototypeArray[i].execute(current);
                result = shapeArray[i].check(current);
                if (!result) {
                    return false;
                }
            }
            // Return the shape check of the prototype we're going to access.
            return result;
        }

        @ExplodeLoop
        @Override
        public DynamicObject getStore(Object thisObj) {
            DynamicObject proto = (JSDynamicObject) thisObj;
            GetPrototypeNode[] getPrototypeArray = getPrototypeNodes;
            for (int i = 0; i < getPrototypeArray.length; i++) {
                proto = getPrototypeArray[i].execute(proto);
            }
            return proto;
        }

        @Override
        public int getDepth() {
            return protoShapes.length;
        }

        @ExplodeLoop
        @Override
        public boolean isValid() {
            if (!shape.getValidAssumption().isValid()) {
                return false;
            }
            for (Shape protoShape : protoShapes) {
                if (!protoShape.getValidAssumption().isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Check the shapes of the object and its immediate prototype.
     *
     * This class actually reads the prototype and checks the prototype shape's identity.
     *
     * @see JSConfig#SkipPrototypeShapeCheck
     */
    protected static final class TraversePrototypeShapeCheckNode extends AbstractShapeCheckNode {

        private final Shape protoShape;
        @Child private GetPrototypeNode getPrototypeNode;

        public TraversePrototypeShapeCheckNode(Shape shape, DynamicObject thisObj) {
            super(shape);
            this.protoShape = JSObject.getPrototype(thisObj).getShape();
            this.getPrototypeNode = GetPrototypeNode.create();
        }

        @Override
        public boolean accept(Object thisObj) {
            if (isDynamicObject(thisObj, shape)) {
                DynamicObject jsobj = castDynamicObject(thisObj, shape);
                if (shape.check(jsobj)) {
                    // Return the shape check of the prototype we're going to access.
                    return protoShape.check(getPrototypeNode.execute(jsobj));
                }
            }
            return false;
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return getPrototypeNode.execute(thisObj);
        }

        @Override
        public int getDepth() {
            return 1;
        }

        @Override
        public boolean isValid() {
            if (!shape.getValidAssumption().isValid()) {
                return false;
            } else if (!protoShape.getValidAssumption().isValid()) {
                return false;
            }
            return true;
        }
    }

    /**
     * Checks the shapes of the prototype chain up to the given depth using assumptions only (valid
     * and property unchanged).
     */
    protected static final class ValuePrototypeChainCheckNode extends AbstractFinalPrototypeShapeCheckNode {

        private final Class<?> valueClass;

        private ValuePrototypeChainCheckNode(Class<?> valueClass, Shape shape, Assumption[] assumptions, DynamicObject prototype, JSContext context) {
            super(shape, assumptions, prototype, context);
            this.valueClass = valueClass;
        }

        static AbstractShapeCheckNode create(Class<?> valueClass, Shape shape, DynamicObject thisObj, Object key, int depth, JSContext context) {
            assert depth >= 1;
            Assumption[] ass = new Assumption[Math.max(0, depth * 3 - 1)];
            int pos = 0;
            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                Assumption stablePrototypeAssumption = i == 0 ? null : JSShape.getPrototypeAssumption(depthShape);
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                ass[pos++] = depthShape.getValidAssumption();
                ass[pos++] = JSShape.getPropertyAssumption(depthShape, key, true);
                if (stablePrototypeAssumption != null) {
                    ass[pos++] = stablePrototypeAssumption;
                }
            }
            assert pos == ass.length;
            return new ValuePrototypeChainCheckNode(valueClass, shape, ass, depthProto, context);
        }

        @Override
        public boolean accept(Object thisObj) {
            return CompilerDirectives.isExact(thisObj, valueClass);
        }

        @Override
        public int getDepth() {
            return (assumptions.length + 1) / 3;
        }
    }

    /**
     * Check the shapes of the prototype chain up to the given depth.
     *
     * This class actually traverses the prototype chain and checks each prototype shape's identity.
     *
     * @see JSConfig#SkipPrototypeShapeCheck
     */
    protected static final class TraverseValuePrototypeChainCheckNode extends AbstractShapeCheckNode {

        private final Class<?> valueClass;
        private final PrototypeSupplier jsclass;
        @CompilationFinal(dimensions = 1) private final Shape[] protoShapes;
        @Children private final GetPrototypeNode[] getPrototypeNodes;

        public TraverseValuePrototypeChainCheckNode(Class<?> valueClass, Shape shape, DynamicObject thisObj, int depth, JSClass jsclass) {
            super(shape);
            assert depth >= 1;
            this.valueClass = valueClass;
            this.jsclass = (PrototypeSupplier) jsclass;
            this.protoShapes = new Shape[depth];
            this.getPrototypeNodes = new GetPrototypeNode[depth - 1];

            Shape depthShape = shape;
            DynamicObject depthProto = thisObj;
            for (int i = 0; i < depth; i++) {
                depthProto = JSObject.getPrototype(depthProto);
                depthShape = depthProto.getShape();
                protoShapes[i] = depthShape;
                if (i < depth - 1) {
                    getPrototypeNodes[i] = GetPrototypeNode.create();
                }
            }
        }

        @ExplodeLoop
        @Override
        public boolean accept(Object thisObj) {
            if (!CompilerDirectives.isExact(thisObj, valueClass)) {
                return false;
            }
            DynamicObject current = jsclass.getIntrinsicDefaultProto(getRealm());
            boolean result = true;
            Shape[] shapeArray = protoShapes;
            GetPrototypeNode[] getPrototypeArray = getPrototypeNodes;
            for (int i = 0; i < shapeArray.length; i++) {
                result = shapeArray[i].check(current);
                if (!result) {
                    return false;
                }
                if (i < shapeArray.length - 1) {
                    current = getPrototypeArray[i].execute(current);
                }
            }
            // Return the shape check of the prototype we're going to access.
            return result;
        }

        @ExplodeLoop
        @Override
        public DynamicObject getStore(Object thisObj) {
            DynamicObject proto = jsclass.getIntrinsicDefaultProto(getRealm());
            GetPrototypeNode[] getPrototypeArray = getPrototypeNodes;
            for (int i = 0; i < getPrototypeArray.length; i++) {
                proto = getPrototypeArray[i].execute(proto);
            }
            return proto;
        }

        @Override
        public int getDepth() {
            return protoShapes.length;
        }

        @ExplodeLoop
        @Override
        public boolean isValid() {
            for (Shape protoShape : protoShapes) {
                if (!protoShape.getValidAssumption().isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    protected static final class JSClassCheckNode extends ReceiverCheckNode {
        private final JSClass jsclass;

        protected JSClassCheckNode(JSClass jsclass) {
            super(null);
            this.jsclass = jsclass;
        }

        @Override
        public boolean accept(Object thisObj) {
            return JSClass.isInstance(thisObj, jsclass);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            return (JSDynamicObject) thisObj;
        }
    }

    protected static final class ForeignLanguageCheckNode extends ReceiverCheckNode {

        protected ForeignLanguageCheckNode() {
            super(null);
        }

        @Override
        public boolean accept(Object thisObj) {
            return JSRuntime.isForeignObject(thisObj);
        }

        @Override
        public DynamicObject getStore(Object thisObj) {
            throw Errors.shouldNotReachHere();
        }
    }

    // ---

    public abstract static class CacheNode<T extends CacheNode<T>> extends JavaScriptBaseNode {
        protected static final int IS_SINGLE_REALM = 1 << 0;
        protected static final int IS_FINAL = 1 << 1;
        protected static final int IS_FINAL_CONSTANT_OBJECT = 1 << 2;
        protected static final int IS_SIMPLE_SHAPE_CHECK = 1 << 3;

        private final int specializationFlags;
        @Child protected ReceiverCheckNode receiverCheck;

        protected CacheNode(ReceiverCheckNode receiverCheck) {
            this(receiverCheck, 0);
        }

        protected CacheNode(ReceiverCheckNode receiverCheck, int specializationFlags) {
            this.receiverCheck = receiverCheck;
            this.specializationFlags = specializationFlags |
                            (receiverCheck instanceof AbstractSingleRealmShapeCheckNode ? IS_SINGLE_REALM : 0) |
                            (receiverCheck instanceof ShapeCheckNode ? IS_SIMPLE_SHAPE_CHECK : 0);
        }

        protected abstract T getNext();

        protected abstract void setNext(T next);

        @SuppressWarnings("unchecked")
        protected T withNext(T newNext) {
            T copy = (T) copy();
            copy.setNext(newNext);
            return copy;
        }

        protected final boolean isGeneric() {
            return receiverCheck == null;
        }

        protected final boolean accepts(Object thisObj) {
            return receiverCheck == null || receiverCheck.accept(thisObj);
        }

        protected final boolean isValid() {
            return receiverCheck == null || receiverCheck.isValid();
        }

        protected final boolean isSingleRealm() {
            return (specializationFlags & IS_SINGLE_REALM) != 0;
        }

        protected boolean acceptsValue(Object value) {
            assert value == null;
            return true;
        }

        protected boolean sweep() {
            return false;
        }

        protected final boolean isFinalSpecialization() {
            return (specializationFlags & IS_FINAL) != 0;
        }

        protected final boolean isConstantObjectSpecialization() {
            return (specializationFlags & IS_FINAL_CONSTANT_OBJECT) != 0;
        }

        protected final boolean isSimpleShapeCheck() {
            return (specializationFlags & IS_SIMPLE_SHAPE_CHECK) != 0;
        }

        protected boolean isValidFinalAssumption() {
            return true;
        }

        protected JSDynamicObject getExpectedObject() {
            return null;
        }

        protected void clearExpectedObject() {
        }

        protected String debugString() {
            CompilerAsserts.neverPartOfCompilation();
            if (receiverCheck != null) {
                return getClass().getSimpleName() + "<check=" + receiverCheck + ", shape=" + receiverCheck.getShape() + ">\n" + ((getNext() == null) ? "" : getNext().debugString());
            }
            return null;
        }

        @Override
        public final NodeCost getCost() {
            return NodeCost.NONE;
        }
    }

    protected final Object key;
    protected final JSContext context;
    @CompilationFinal private Assumption invalidationAssumption;

    public PropertyCacheNode(Object key, JSContext context) {
        this.key = key;
        this.context = context;
        assert JSRuntime.isPropertyKey(key) || key instanceof HiddenKey;
    }

    public final Object getKey() {
        return key;
    }

    protected abstract T getCacheNode();

    protected abstract void setCacheNode(T cache);

    protected abstract T createGenericPropertyNode();

    protected abstract T createCachedPropertyNode(Property entry, Object thisObj, int depth, Object value, T currentHead);

    protected abstract T createUndefinedPropertyNode(Object thisObj, Object store, int depth, Object value);

    protected abstract T createJavaPropertyNodeMaybe(Object thisObj, int depth);

    protected abstract T createTruffleObjectPropertyNode();

    protected abstract boolean canCombineShapeCheck(Shape parentShape, Shape cacheShape, Object thisObj, int depth, Object value, Property property);

    protected abstract T createCombinedIcPropertyNode(Shape parentShape, Shape cacheShape, Object thisObj, int depth, Object value, Property property);

    @TruffleBoundary
    protected T specialize(Object thisObj) {
        return specialize(thisObj, null);
    }

    @TruffleBoundary
    protected T specialize(Object thisObj, Object value) {
        T res;
        Lock lock = getLock();
        lock.lock();
        try {
            T currentHead = getCacheNode();
            do {
                assert currentHead == getCacheNode();
                int cachedCount = 0;
                boolean invalid = false;
                boolean generic = false;
                res = null;

                for (T c = currentHead; c != null; c = c.getNext()) {
                    if (c.isGeneric()) {
                        generic = true;
                        res = c;
                        assert c.getNext() == null;
                        break;
                    } else {
                        cachedCount++;
                        if (!c.isValid() ||
                                        (c.isSingleRealm() && !this.context.getSingleRealmAssumption().isValid()) ||
                                        (c.isFinalSpecialization() && !c.isValidFinalAssumption())) {
                            invalid = true;
                            break;
                        } else {
                            c.sweep();
                            if (res == null && c.accepts(thisObj) && c.acceptsValue(value)) {
                                res = c;
                                // continue checking for invalid cache entries
                            }
                            if (isUnexpectedConstantObject(c, thisObj)) {
                                invalid = true;
                                break;
                            }
                        }
                    }
                }
                if (invalid) {
                    checkForUnstableAssumption(currentHead, thisObj);
                    currentHead = rewriteCached(currentHead, filterValid(currentHead));
                    traceAssumptionInvalidated();
                    res = null;
                    continue; // restart
                }
                if (res == null) {
                    assert !generic;
                    T newNode = createSpecialization(thisObj, currentHead, cachedCount, value);
                    if (newNode == null) {
                        currentHead = this.getCacheNode();
                        continue; // restart
                    }
                    res = newNode;
                    assert res.getParent() != null;
                }
            } while (res == null);
        } finally {
            lock.unlock();
        }
        if (!(res.isGeneric() || (res.accepts(thisObj) && res.acceptsValue(value)))) {
            throw Errors.shouldNotReachHere();
        }
        return res;
    }

    protected T createSpecialization(Object thisObj, T currentHead, int cachedCount, Object value) {
        int depth = 0;
        T specialized = null;

        DynamicObject store = null;
        if (JSDynamicObject.isJSDynamicObject(thisObj)) {
            if ((!JSAdapter.isJSAdapter(thisObj) && !JSProxy.isJSProxy(thisObj)) || key instanceof HiddenKey) {
                store = (JSDynamicObject) thisObj;
            }
        } else if (JSRuntime.isForeignObject(thisObj)) {
            assert !JSDynamicObject.isJSDynamicObject(thisObj);
            specialized = createTruffleObjectPropertyNode();
        } else {
            store = wrapPrimitive(thisObj, context);
        }

        while (store != null) {
            // check for obsolete shape
            if (DynamicObjectLibrary.getUncached().updateShape(store)) {
                return retryCache();
            }

            Shape cacheShape = store.getShape();

            if (JSConfig.DictionaryObject && JSDictionary.isJSDictionaryObject(store)) {
                // TODO: could probably specialize on shape as well.
                return rewriteToGeneric(currentHead, cachedCount, "dictionary object");
            }

            if (JSConfig.MergeShapes && cachedCount > 0) {
                // check if we're creating unnecessary polymorphism due to compatible types
                if (tryMergeShapes(cacheShape, currentHead)) {
                    DynamicObjectLibrary.getUncached().updateShape(store);
                    return retryCache();
                }
            }

            Property property = cacheShape.getProperty(key);

            if (JSConfig.MergeCompatibleLocations && cachedCount == 1 && depth == 0) {
                if (!(currentHead.receiverCheck instanceof CombinedShapeCheckNode)) {
                    Shape existingShape = currentHead.receiverCheck.getShape();
                    if (existingShape != null && property != null && shapesHaveCommonLayoutForKey(existingShape, cacheShape) &&
                                    canCombineShapeCheck(existingShape, cacheShape, thisObj, depth, value, property)) {
                        return rewriteToCombinedIC(existingShape, cacheShape, thisObj, depth, value, property);
                    }
                }
            }

            if (property != null) {
                specialized = createCachedPropertyNode(property, thisObj, depth, value, currentHead);
                if (specialized == null) {
                    return null;
                }
                break;
            } else if (alwaysUseStore(store, key)) {
                specialized = createUndefinedPropertyNode(thisObj, store, depth, value);
                break;
            } else if (isOwnProperty()) {
                break;
            }

            store = (DynamicObject) JSRuntime.toJavaNull(JSObject.getPrototype(store));
            if (store != null) {
                depth++;
            }
        }

        if (cachedCount >= context.getPropertyCacheLimit() || (specialized != null && specialized.isGeneric())) {
            return rewriteToGeneric(currentHead, cachedCount, "cache limit reached");
        }

        if (specialized == null) {
            specialized = createUndefinedPropertyNode(thisObj, thisObj, depth, value);
        }

        return insertCached(specialized, currentHead, cachedCount);
    }

    private T rewriteToCombinedIC(Shape parentShape, Shape cacheShape, Object thisObj, int depth, Object value, Property property) {
        // replace the entire cache with a combined IC case
        assert shapesHaveCommonLayoutForKey(parentShape, cacheShape);
        T newNode = createCombinedIcPropertyNode(parentShape, cacheShape, thisObj, depth, value, property);
        assert newNode != null;
        invalidateCache();
        insert(newNode);
        this.setCacheNode(newNode);
        return newNode;
    }

    protected final boolean shapesHaveCommonLayoutForKey(Shape shape1, Shape shape2) {
        Class<? extends DynamicObject> cachedType = shape1.getLayoutClass();
        Class<? extends DynamicObject> incomingType = shape2.getLayoutClass();
        if (cachedType == incomingType) {
            Property cachedProperty = shape1.getProperty(key);
            Property incomingProperty = shape2.getProperty(key);
            if (incomingProperty != null && incomingProperty.equals(cachedProperty)) {
                Location cachedLocation = cachedProperty.getLocation();
                Location incomingLocation = incomingProperty.getLocation();
                // We need to compare locations by identity; locations that are equal are not
                // necessarily interchangeable.
                return incomingLocation == cachedLocation;
            }
        }
        return false;
    }

    protected static boolean alwaysUseStore(DynamicObject store, Object key) {
        return JSProxy.isJSProxy(store) || (JSArrayBufferView.isJSArrayBufferView(store) && isNonIntegerIndex(key)) || key instanceof HiddenKey;
    }

    protected final void deoptimize(CacheNode<?> stop) {
        if (CompilerDirectives.inCompiledCode() && stop != null) {
            /*
             * If the invalidation assumption is initialized, we have previously retried the cache.
             * Do not invalidate code here, since we might just re-shape the object (which does not
             * need a modification of the AST). The assumption will be used to invalidate the code.
             */
            if (Assumption.isValidAssumption(invalidationAssumption)) {
                return;
            }
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
    }

    protected T retryCache() {
        if (invalidationAssumption == null) {
            invalidationAssumption = Truffle.getRuntime().createAssumption("PropertyCacheNode");
            cacheAssumptionInitializedCount.inc();
            // This could be removed eventually GR-25874
            reportPolymorphicSpecialize();
        }
        return null;
    }

    protected void invalidateCache() {
        if (invalidationAssumption != null) {
            invalidationAssumption.invalidate("PropertyCacheNode invalidation");
            invalidationAssumption = Truffle.getRuntime().createAssumption("PropertyCacheNode");
            cacheAssumptionInvalidatedCount.inc();
        }
    }

    protected T insertCached(T specialized, T currentHead, int cachedCount) {
        assert currentHead == this.getCacheNode();
        // insert specialization at the front
        invalidateCache();
        insert(specialized);
        specialized.setNext(currentHead);
        this.setCacheNode(specialized);

        if (cachedCount > 0) {
            polymorphicCount.inc();
        }
        traceRewriteInsert(specialized, cachedCount);
        if (JSConfig.TracePolymorphicPropertyAccess && cachedCount > 0) {
            System.out.printf("POLYMORPHIC PROPERTY ACCESS key='%s' %s\n%s\n---\n", key, getEncapsulatingSourceSection(), specialized.debugString());
        }
        return specialized;
    }

    protected T rewriteToGeneric(T currentHead, int cachedCount, String reason) {
        assert currentHead == this.getCacheNode();
        // replace the entire cache with the generic case
        T newNode = createGenericPropertyNode();
        invalidateCache();
        insert(newNode);
        this.setCacheNode(newNode);

        if (cachedCount > 0 && cachedCount >= context.getPropertyCacheLimit()) {
            megamorphicCount.inc();
            reportPolymorphicSpecialize();
        }
        traceRewriteMegamorphic(newNode, reason);
        if (JSConfig.TraceMegamorphicPropertyAccess) {
            System.out.printf("MEGAMORPHIC PROPERTY ACCESS key='%s' %s\n%s\n---\n", key, getEncapsulatingSourceSection(), currentHead.debugString());
        }
        return newNode;
    }

    protected T rewriteCached(T currentHead, T newHead) {
        assert currentHead == this.getCacheNode();
        invalidateCache();
        this.setCacheNode(newHead);
        return newHead;
    }

    /**
     * Does the given map relate to any of the cached maps by upcasting? If so, obsolete the
     * downcast map.
     *
     * @param cacheShape The new map to check against
     * @return true if a map was obsoleted
     */
    protected static <T extends CacheNode<T>> boolean tryMergeShapes(Shape cacheShape, T head) {
        assert cacheShape.isValid();
        boolean result = false;

        for (T cur = head; cur != null; cur = cur.getNext()) {
            if (cur.receiverCheck == null) {
                continue;
            }
            Shape other = cur.receiverCheck.getShape();
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

    protected void checkForUnstableAssumption(T head, Object thisObj) {
        for (T cur = head; cur != null; cur = cur.getNext()) {
            ReceiverCheckNode check = cur.receiverCheck;
            if (check == null) {
                continue;
            }
            if (check.isUnstable()) {
                setPropertyAssumptionCheckEnabled(false);
                propertyAssumptionCheckFailedCount.inc();
            }
            if (isUnexpectedConstantObject(cur, thisObj)) {
                // constant object is null or another object
                cur.clearExpectedObject();
                setPropertyAssumptionCheckEnabled(false);
                constantObjectCheckFailedCount.inc();
                traceRewriteEvictFinal(cur);
            }
        }
    }

    private boolean isUnexpectedConstantObject(T cache, Object thisObj) {
        return cache.isConstantObjectSpecialization() && cache.getExpectedObject() != thisObj;
    }

    protected T filterValid(T cache) {
        if (cache == null) {
            return null;
        }
        T filteredNext = filterValid(cache.getNext());
        if (cache.isValid() && (!cache.isSingleRealm() || this.context.getSingleRealmAssumption().isValid()) &&
                        (!cache.isFinalSpecialization() || cache.isValidFinalAssumption()) &&
                        (!cache.isConstantObjectSpecialization() || cache.getExpectedObject() != null)) {
            if (filteredNext == cache.getNext()) {
                return cache;
            } else {
                return cache.withNext(filteredNext);
            }
        } else {
            return filteredNext;
        }
    }

    protected static final DynamicObject wrapPrimitive(Object thisObject, JSContext context) {
        // wrap primitives for lookup
        Object wrapper = JSRuntime.toObjectFromPrimitive(context, thisObject, false);
        return JSDynamicObject.isJSDynamicObject(wrapper) ? ((JSDynamicObject) wrapper) : null;
    }

    protected final AbstractShapeCheckNode createShapeCheckNode(Shape shape, JSDynamicObject thisObj, int depth, boolean isConstantObjectFinal, boolean isDefine) {
        if (depth == 0) {
            return createShapeCheckNodeDepth0(shape, thisObj, isConstantObjectFinal, isDefine);
        } else if (depth == 1) {
            return createShapeCheckNodeDepth1(shape, thisObj, depth, isConstantObjectFinal);
        } else {
            return createShapeCheckNodeDeeper(shape, thisObj, depth, isConstantObjectFinal);
        }
    }

    private AbstractShapeCheckNode createShapeCheckNodeDepth0(Shape shape, JSDynamicObject thisObj, boolean isConstantObjectFinal, boolean isDefine) {
        assert thisObj.getShape() == shape;
        // if isDefine is true, shape change is imminent, so don't use assumption
        if (!isDefine && (isConstantObjectFinal || (isGlobal() && JSConfig.SkipGlobalShapeCheck)) &&
                        isPropertyAssumptionCheckEnabled() && JSShape.getPropertyAssumption(shape, key).isValid()) {
            return ConstantObjectAssumptionShapeCheckNode.create(shape, thisObj, key, 0, getContext());
        } else {
            return new ShapeCheckNode(shape);
        }
    }

    private AbstractShapeCheckNode createShapeCheckNodeDepth1(Shape shape, JSDynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        assert depth == 1;
        if (JSConfig.SkipPrototypeShapeCheck && prototypesInShape(thisObj, depth) && propertyAssumptionsValid(thisObj, depth, isConstantObjectFinal)) {
            return isConstantObjectFinal
                            ? ConstantObjectPrototypeChainShapeCheckNode.create(shape, thisObj, key, depth, getContext())
                            : PrototypeShapeCheckNode.create(shape, thisObj, key, depth, getContext());
        } else {
            traversePrototypeShapeCheckCount.inc();
            return new TraversePrototypeShapeCheckNode(shape, thisObj);
        }
    }

    private AbstractShapeCheckNode createShapeCheckNodeDeeper(Shape shape, JSDynamicObject thisObj, int depth, boolean isConstantObjectFinal) {
        assert depth > 1;
        if (JSConfig.SkipPrototypeShapeCheck && prototypesInShape(thisObj, depth) && propertyAssumptionsValid(thisObj, depth, isConstantObjectFinal)) {
            return isConstantObjectFinal
                            ? ConstantObjectPrototypeChainShapeCheckNode.create(shape, thisObj, key, depth, getContext())
                            : PrototypeChainShapeCheckNode.create(shape, thisObj, key, depth, getContext());
        } else {
            traversePrototypeChainShapeCheckCount.inc();
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
        Shape depthShape = depthObject.getShape();
        if (checkDepth0 && !JSShape.getPropertyAssumption(depthShape, key).isValid()) {
            return false;
        }
        for (int i = 0; i < depth; i++) {
            if ((depth != 0 || checkDepth0) && !JSShape.getPrototypeAssumption(depthShape).isValid()) {
                return false;
            }
            depthObject = JSObject.getPrototype(depthObject);
            depthShape = depthObject.getShape();
            if (!JSShape.getPropertyAssumption(depthShape, key, true).isValid()) {
                return false;
            }
        }
        return true;
    }

    protected final ReceiverCheckNode createPrimitiveReceiverCheck(Object thisObj, int depth) {
        Class<?> valueClass = thisObj.getClass();
        if (depth == 0) {
            return new InstanceofCheckNode(valueClass);
        } else {
            assert JSRuntime.isJSPrimitive(thisObj);
            DynamicObject wrapped = wrapPrimitive(thisObj, context);
            if (JSConfig.SkipPrototypeShapeCheck && prototypesInShape(wrapped, depth) && propertyAssumptionsValid(wrapped, depth, false)) {
                return ValuePrototypeChainCheckNode.create(valueClass, wrapped.getShape(), wrapped, key, depth, context);
            } else {
                return new TraverseValuePrototypeChainCheckNode(valueClass, wrapped.getShape(), wrapped, depth, JSObject.getJSClass(wrapped));
            }
        }
    }

    protected abstract boolean isGlobal();

    protected abstract boolean isOwnProperty();

    public final JSContext getContext() {
        return context;
    }

    protected abstract boolean isPropertyAssumptionCheckEnabled();

    protected abstract void setPropertyAssumptionCheckEnabled(boolean value);

    @Override
    public NodeCost getCost() {
        T cacheNode = getCacheNode();
        if (cacheNode == null) {
            return NodeCost.UNINITIALIZED;
        } else if (cacheNode.isGeneric()) {
            return NodeCost.MEGAMORPHIC;
        } else if (cacheNode.getNext() == null) {
            return NodeCost.MONOMORPHIC;
        } else {
            return NodeCost.POLYMORPHIC;
        }
    }

    protected static boolean isArrayLengthProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSArray.ArrayLengthProxyProperty;
    }

    protected static boolean isFunctionLengthProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSFunction.FunctionLengthPropertyProxy;
    }

    protected static boolean isFunctionNameProperty(Property property) {
        return JSProperty.isProxy(property) && JSProperty.getConstantProxy(property) instanceof JSFunction.FunctionNamePropertyProxy;
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

    protected static boolean isNonIntegerIndex(Object key) {
        assert !(key instanceof String) || JSRuntime.INFINITY_STRING.equals(key) || (JSRuntime.canonicalNumericIndexString((String) key) == Undefined.instance);
        return JSRuntime.INFINITY_STRING.equals(key);
    }

    private void traceRewriteInsert(Node newNode, int cacheDepth) {
        if (TruffleOptions.TraceRewrites) {
            PrintStream out = System.out;
            out.printf("[truffle]   rewrite %-50s |Property %s |Node %s (%d/%d)%n", this, key, newNode, cacheDepth, getContext().getPropertyCacheLimit());
        }
    }

    private void traceRewriteMegamorphic(Node newNode, String reason) {
        if (TruffleOptions.TraceRewrites) {
            PrintStream out = System.out;
            out.printf("[truffle]   rewrite %-50s |Property %s |Node %s |Reason %s (limit %d)%n", this, key, newNode, reason, getContext().getPropertyCacheLimit());
        }
    }

    protected void traceRewriteEvictFinal(Node evicted) {
        if (TruffleOptions.TraceRewrites) {
            PrintStream out = System.out;
            out.printf("[truffle]   rewrite %-50s |Property %s |Node %s |Reason evict final%n", this, key, evicted);
        }
    }

    private void traceAssumptionInvalidated() {
        if (TruffleOptions.TraceRewrites) {
            PrintStream out = System.out;
            out.printf("[truffle]   rewrite %-50s |Property %s |Reason assumption invalidated%n", this, key);
        }
    }

    protected String getAccessorKey(String getset) {
        return getAccessorKey(getset, getKey());
    }

    @TruffleBoundary
    protected static String getAccessorKey(String getset, Object key) {
        assert JSRuntime.isString(key);
        String origKey = key instanceof String ? (String) key : ((JSLazyString) key).toString();
        if (origKey.length() > 0 && Character.isLetter(origKey.charAt(0))) {
            return getset + origKey.substring(0, 1).toUpperCase() + origKey.substring(1);
        }
        return null;
    }

    protected static DynamicObjectLibrary createCachedAccess(Object key, ReceiverCheckNode receiverCheck, DynamicObject store) {
        assert key != null;
        if (receiverCheck instanceof AbstractSingleRealmShapeCheckNode) {
            return DynamicObjectLibrary.getFactory().create(store);
        } else if (receiverCheck instanceof AbstractShapeCheckNode && !(receiverCheck instanceof AbstractSingleRealmShapeCheckNode)) {
            return DynamicObjectLibrary.getFactory().create(store);
        } else {
            return DynamicObjectLibrary.getFactory().createDispatched(JSConfig.PropertyCacheLimit);
        }
    }

    private static final DebugCounter polymorphicCount = DebugCounter.create("Polymorphic property cache count");
    private static final DebugCounter megamorphicCount = DebugCounter.create("Megamorphic property cache count");
    private static final DebugCounter cacheAssumptionInitializedCount = DebugCounter.create("Property cache assumptions initialized");
    private static final DebugCounter cacheAssumptionInvalidatedCount = DebugCounter.create("Property cache assumptions invalidated");
    private static final DebugCounter propertyAssumptionCheckFailedCount = DebugCounter.create("Property assumption checks failed");
    private static final DebugCounter constantObjectCheckFailedCount = DebugCounter.create("Constant object checks failed");
    private static final DebugCounter traversePrototypeShapeCheckCount = DebugCounter.create("TraversePrototypeShapeCheckNode count");
    private static final DebugCounter traversePrototypeChainShapeCheckCount = DebugCounter.create("TraversePrototypeChainShapeCheckNode count");
}
