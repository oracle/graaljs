/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.array.ArrayAllocationSite;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.util.CompilableBiFunction;

public abstract class JSArrayFactory {
    protected final JSContext context;
    @CompilationFinal private DynamicObjectLibrary setProto;

    static JSArrayFactory create(JSContext context, Shape shape, PrototypeSupplier prototypeSupplier) {
        return new JSArrayFactory.WithShape(context, shape, prototypeSupplier);
    }

    static JSArrayFactory forArray(JSObjectFactory.IntrinsicBuilder builder) {
        return new JSArrayFactory.Intrinsic(builder.getContext(), JSArray.INSTANCE::makeInitialShape, builder.nextIndex());
    }

    static JSArrayFactory forArgumentsObject(JSObjectFactory.IntrinsicBuilder builder, boolean mapped) {
        return new JSArrayFactory.ArgumentsObject(builder.getContext(), builder.nextIndex(), mapped);
    }

    protected JSArrayFactory(JSContext context) {
        this.context = context;
    }

    public final DynamicObject createWithRealm(JSRealm realm,
                    ScriptArray arrayType, Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
        return createWithPrototype(realm, getPrototype(realm), arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    public final DynamicObject createWithPrototype(JSRealm realm, DynamicObject prototype,
                    ScriptArray arrayType, Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
        assert prototype != null;
        Shape shape = getShape(realm, prototype);
        if (isInObjectProto()) {
            DynamicObject obj = newInstance(shape, arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
            setPrototype(obj, prototype);
            return obj;
        }
        assert JSObjectFactory.verifyPrototype(shape, prototype);
        return newInstance(shape, arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    protected DynamicObject newInstance(Shape shape, ScriptArray arrayType, Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
        return JSArrayObject.create(shape, arrayType, array, site, length, usedLength, indexOffset, arrayOffset, holeCount);
    }

    protected abstract DynamicObject getPrototype(JSRealm realm);

    protected abstract Shape getShape(JSRealm realm, DynamicObject prototype);

    protected final void setPrototype(DynamicObject obj, DynamicObject prototype) {
        if (setProto == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setProto = context.adoptNode(JSObjectUtil.createCached(JSObject.HIDDEN_PROTO, obj));
        }
        setProto.put(obj, JSObject.HIDDEN_PROTO, prototype);
    }

    protected final boolean isInObjectProto() {
        return context.isMultiContext();
    }

    private static final class WithShape extends JSArrayFactory {
        private final Shape shape;
        private final PrototypeSupplier prototypeSupplier;

        protected WithShape(JSContext context, Shape shape, PrototypeSupplier prototypeSupplier) {
            super(context);
            this.shape = shape;
            this.prototypeSupplier = prototypeSupplier;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return prototypeSupplier.getIntrinsicDefaultProto(realm);
        }

        @Override
        protected Shape getShape(JSRealm realm, DynamicObject prototype) {
            return shape;
        }
    }

    private static class Intrinsic extends JSArrayFactory {
        private final int slot;
        private final CompilableBiFunction<JSContext, DynamicObject, Shape> shapeSupplier;
        @CompilationFinal private Shape shape;

        protected Intrinsic(JSContext context, CompilableBiFunction<JSContext, DynamicObject, Shape> shapeSupplier, int slot) {
            super(context);
            this.shapeSupplier = shapeSupplier;
            this.slot = slot;
        }

        @Override
        protected final Shape getShape(JSRealm realm, DynamicObject prototype) {
            if (context.isMultiContext()) {
                if (shape == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    shape = shapeSupplier.apply(context, null);
                    assert isInObjectProto() == JSObjectFactory.hasInObjectProto(shape);
                }
                return shape;
            } else {
                Shape realmShape = realm.getObjectFactories().shapes[slot];
                if (realmShape == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    Shape newShape = shapeSupplier.apply(context, prototype);
                    realmShape = realm.getObjectFactories().shapes[slot] = newShape;
                    assert isInObjectProto() == JSObjectFactory.hasInObjectProto(realmShape);
                }
                return realmShape;
            }
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return realm.getArrayPrototype();
        }
    }

    private static final class ArgumentsObject extends Intrinsic {
        private final boolean mapped;

        protected ArgumentsObject(JSContext context, int slot, boolean mapped) {
            super(context, JSObjectFactory.defaultShapeSupplier(JSArgumentsArray.INSTANCE), slot);
            this.mapped = mapped;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return realm.getObjectPrototype();
        }

        @Override
        protected DynamicObject newInstance(Shape shape, ScriptArray arrayType, Object array, ArrayAllocationSite site, long length, int usedLength, int indexOffset, int arrayOffset, int holeCount) {
            Object[] elements = (Object[]) array;
            if (mapped) {
                return JSArgumentsArray.createMapped(shape, elements);
            } else {
                return JSArgumentsArray.createUnmapped(shape, elements);
            }
        }
    }
}
