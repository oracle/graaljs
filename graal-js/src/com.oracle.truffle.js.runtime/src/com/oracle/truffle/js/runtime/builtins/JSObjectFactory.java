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
package com.oracle.truffle.js.runtime.builtins;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.util.CompilableBiFunction;
import com.oracle.truffle.js.runtime.util.CompilableFunction;

public abstract class JSObjectFactory {
    protected final JSContext context;
    private final boolean inObjectProto;

    public static JSObjectFactory.UnboundProto createUnbound(JSContext context, DynamicObjectFactory factory) {
        return new JSObjectFactory.UnboundProto(context, factory);
    }

    public static JSObjectFactory.BoundProto createBound(JSContext context, DynamicObject prototype, DynamicObjectFactory factory) {
        return new JSObjectFactory.BoundProto(context, prototype, factory);
    }

    public static JSObjectFactory createDefault(JSContext context, PrototypeSupplier prototypeSupplier, DynamicObjectFactory factory) {
        return new JSObjectFactory.Eager(context, prototypeSupplier, factory);
    }

    static JSObjectFactory createIntrinsic(JSContext context, PrototypeSupplier prototypeSupplier, CompilableBiFunction<JSContext, DynamicObject, Shape> shapeSupplier, int slot) {
        return new JSObjectFactory.LazySupplier(context, prototypeSupplier, shapeSupplier, slot);
    }

    static <T extends JSClass & PrototypeSupplier> JSObjectFactory createIntrinsic(JSContext context, T jsclass, int slot) {
        return new JSObjectFactory.LazyJSClass<>(context, jsclass, slot);
    }

    public static final class IntrinsicBuilder {
        private final JSContext context;
        private int count;
        private boolean closed;

        public IntrinsicBuilder(JSContext context) {
            this.context = context;
        }

        public JSObjectFactory create(PrototypeSupplier prototypeSupplier, CompilableBiFunction<JSContext, DynamicObject, Shape> shapeSupplier) {
            assert !closed;
            int index = count++;
            return createIntrinsic(context, prototypeSupplier, shapeSupplier, index);
        }

        public <T extends JSClass & PrototypeSupplier> JSObjectFactory create(T jsclass) {
            assert !closed;
            int index = count++;
            return createIntrinsic(context, jsclass, index);
        }

        public JSFunctionFactory function(CompilableFunction<JSRealm, DynamicObject> intrinsicDefaultProto,
                        boolean isStrict, boolean isAnonymous, boolean isConstructor, boolean isGenerator, boolean isBound) {
            assert !closed;
            int index = count++;
            return JSFunctionFactory.createIntrinsic(context, intrinsicDefaultProto, isStrict, isAnonymous, isConstructor, isGenerator, isBound, index);
        }

        public int finish() {
            assert !closed;
            closed = true;
            return count;
        }
    }

    public static final class RealmData {
        @CompilationFinal(dimensions = 1) final DynamicObjectFactory[] factories;

        public RealmData(int count) {
            this.factories = new DynamicObjectFactory[count];
        }
    }

    protected JSObjectFactory(JSContext context, boolean inObjectProto) {
        this.context = context;
        this.inObjectProto = inObjectProto;
    }

    public final DynamicObject createWithPrototype(JSRealm realm, DynamicObject prototype, Object... values) {
        assert prototype != null;
        DynamicObjectFactory factory = getFactory(realm, prototype);
        if (isInObjectProto()) {
            Object[] protoAndValues = new Object[values.length + 1];
            protoAndValues[0] = prototype;
            System.arraycopy(values, 0, protoAndValues, 1, values.length);
            return newInstance(factory, protoAndValues);
        }
        assert verifyPrototype(factory, prototype);
        return newInstance(factory, values);
    }

    public final DynamicObject createWithPrototype(JSRealm realm, DynamicObject prototype, Object value) {
        assert prototype != null;
        DynamicObjectFactory factory = getFactory(realm, prototype);
        if (isInObjectProto()) {
            return newInstance(factory, prototype, value);
        }
        assert verifyPrototype(factory, prototype);
        return newInstance(factory, value);
    }

    static DynamicObject newInstance(DynamicObjectFactory factory, Object... initialValues) {
        if (CompilerDirectives.isPartialEvaluationConstant(factory)) {
            return factory.newInstance(initialValues);
        } else {
            // factory is not PE-constant in the case of multiple realms
            return newInstanceBoundary(factory, initialValues);
        }
    }

    @TruffleBoundary
    private static DynamicObject newInstanceBoundary(DynamicObjectFactory factory, Object[] initialValues) {
        return factory.newInstance(initialValues);
    }

    static boolean verifyPrototype(DynamicObjectFactory factory, DynamicObject prototype) {
        return JSShape.getPrototypeProperty(factory.getShape()).getLocation().isConstant() && JSShape.getPrototypeProperty(factory.getShape()).getLocation().get(null) == prototype;
    }

    public final DynamicObject createWithRealm(JSRealm realm, Object... values) {
        return createWithPrototype(realm, getPrototype(realm), values);
    }

    public final DynamicObject createWithRealm(JSRealm realm, Object value) {
        return createWithPrototype(realm, getPrototype(realm), value);
    }

    protected abstract DynamicObject getPrototype(JSRealm realm);

    static boolean hasInObjectProto(DynamicObjectFactory factory) {
        return !JSShape.getPrototypeProperty(factory.getShape()).getLocation().isConstant();
    }

    protected abstract DynamicObjectFactory getFactory(JSRealm realm, DynamicObject prototype);

    protected final boolean isInObjectProto() {
        return inObjectProto && context.isMultiContext();
    }

    public static final class UnboundProto extends JSObjectFactory {
        private final DynamicObjectFactory factory;

        protected UnboundProto(JSContext context, DynamicObjectFactory factory) {
            super(context, hasInObjectProto(factory));
            this.factory = factory;
        }

        @Override
        protected DynamicObjectFactory getFactory(JSRealm realm, DynamicObject proto) {
            return factory;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            throw Errors.shouldNotReachHere();
        }
    }

    public static final class BoundProto extends JSObjectFactory {
        private final DynamicObject prototype;
        private final DynamicObjectFactory factory;

        protected BoundProto(JSContext context, DynamicObject prototype, DynamicObjectFactory factory) {
            super(context, hasInObjectProto(factory));
            this.prototype = Objects.requireNonNull(prototype);
            this.factory = factory;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return prototype;
        }

        @Override
        protected DynamicObjectFactory getFactory(JSRealm realm, DynamicObject proto) {
            assert proto == this.prototype;
            return factory;
        }

        public DynamicObject createBound(Object... values) {
            return createWithPrototype(null, prototype, values);
        }

        public DynamicObject createBound(Object value) {
            return createWithPrototype(null, prototype, value);
        }
    }

    private static final class Eager extends JSObjectFactory {
        protected final PrototypeSupplier prototypeSupplier;
        protected final DynamicObjectFactory factory;

        protected Eager(JSContext context, PrototypeSupplier prototypeSupplier, DynamicObjectFactory factory) {
            super(context, hasInObjectProto(factory));
            this.prototypeSupplier = prototypeSupplier;
            this.factory = factory;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return prototypeSupplier.getIntrinsicDefaultProto(realm);
        }

        @Override
        protected DynamicObjectFactory getFactory(JSRealm realm, DynamicObject prototype) {
            return factory;
        }
    }

    private abstract static class Lazy extends JSObjectFactory {
        @CompilationFinal private DynamicObjectFactory factory;
        private final int slot;

        protected Lazy(JSContext context, int slot) {
            super(context, context.isMultiContext());
            this.slot = slot;
        }

        @Override
        protected final DynamicObjectFactory getFactory(JSRealm realm, DynamicObject prototype) {
            if (context.isMultiContext()) {
                if (factory == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    factory = makeInitialShape(isInObjectProto() ? null : prototype).createFactory();
                    assert isInObjectProto() == hasInObjectProto(factory);
                }
                return factory;
            } else {
                DynamicObjectFactory realmFactory = realm.getObjectFactories().factories[slot];
                if (realmFactory == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    DynamicObjectFactory newFactory = makeInitialShape(prototype).createFactory();
                    realmFactory = realm.getObjectFactories().factories[slot] = newFactory;
                    assert isInObjectProto() == hasInObjectProto(realmFactory);
                }
                return realmFactory;
            }
        }

        protected abstract Shape makeInitialShape(DynamicObject prototype);
    }

    private static final class LazySupplier extends Lazy {
        protected final PrototypeSupplier prototypeSupplier;
        protected final CompilableBiFunction<JSContext, DynamicObject, Shape> shapeSupplier;

        protected LazySupplier(JSContext context, PrototypeSupplier prototypeSupplier, CompilableBiFunction<JSContext, DynamicObject, Shape> shapeSupplier, int slot) {
            super(context, slot);
            this.prototypeSupplier = prototypeSupplier;
            this.shapeSupplier = shapeSupplier;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return prototypeSupplier.getIntrinsicDefaultProto(realm);
        }

        @Override
        protected Shape makeInitialShape(DynamicObject prototype) {
            return shapeSupplier.apply(context, prototype);
        }
    }

    private static final class LazyJSClass<T extends JSClass & PrototypeSupplier> extends Lazy {
        protected final T jsclass;

        protected LazyJSClass(JSContext context, T jsclass, int slot) {
            super(context, slot);
            this.jsclass = jsclass;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return jsclass.getIntrinsicDefaultProto(realm);
        }

        @Override
        protected Shape makeInitialShape(DynamicObject prototype) {
            return jsclass.makeInitialShape(context, prototype);
        }
    }
}
