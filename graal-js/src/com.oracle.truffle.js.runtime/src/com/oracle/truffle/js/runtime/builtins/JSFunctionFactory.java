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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.util.CompilableFunction;

public abstract class JSFunctionFactory {
    protected final JSContext context;

    public static JSFunctionFactory create(JSContext context, DynamicObjectFactory factory) {
        return new JSFunctionFactory.Default(context, factory);
    }

    static JSFunctionFactory createIntrinsic(JSContext context, CompilableFunction<JSRealm, DynamicObject> intrinsicDefaultProto,
                    boolean isStrict, boolean isAnonymous, boolean isConstructor, boolean isGenerator, boolean isBound, int slot) {
        if (context.isMultiContext()) {
            return new JSFunctionFactory.IntrinsicMulti(context, intrinsicDefaultProto, isStrict, isAnonymous, isConstructor, isGenerator, isBound);
        } else {
            return new JSFunctionFactory.IntrinsicRealm(context, intrinsicDefaultProto, isStrict, isAnonymous, isConstructor, isGenerator, isBound, slot);
        }
    }

    static DynamicObjectFactory makeFactory(JSContext context, DynamicObject prototype, boolean isStrict, boolean isAnonymous, boolean isConstructor, boolean isGenerator, boolean isBound) {
        Shape initialShape;
        if (isBound) {
            initialShape = JSFunction.makeInitialBoundFunctionShape(context, prototype, isAnonymous);
        } else if (isGenerator) {
            initialShape = JSFunction.makeInitialGeneratorFunctionShape(context, prototype, isAnonymous);
        } else if (isConstructor) {
            initialShape = JSFunction.makeConstructorShape(JSFunction.makeInitialFunctionShape(context, prototype, isStrict, isAnonymous));
        } else {
            initialShape = JSFunction.makeInitialFunctionShape(context, prototype, isStrict, isAnonymous);
        }
        return initialShape.createFactory();
    }

    protected JSFunctionFactory(JSContext context) {
        this.context = context;
    }

    public final DynamicObject create(JSFunctionData functionData, MaterializedFrame enclosingFrame, Object classPrototype, JSRealm realm) {
        return createWithPrototype(functionData, enclosingFrame, classPrototype, realm, getPrototype(realm));
    }

    public final DynamicObject createWithPrototype(JSFunctionData functionData, MaterializedFrame enclosingFrame, Object classPrototype, JSRealm realm, DynamicObject prototype) {
        DynamicObjectFactory factory = getFactory(realm, prototype);
        assert functionData != null;
        assert enclosingFrame != null; // use JSFrameUtil.NULL_MATERIALIZED_FRAME instead
        assert factory.getShape().getObjectType() == JSFunction.INSTANCE;
        if (context.getEcmaScriptVersion() < 6 && functionData.hasStrictFunctionProperties()) {
            return createES5Strict(factory, functionData, enclosingFrame, classPrototype, realm, prototype);
        }
        if (context.isMultiContext()) {
            return JSObjectFactory.newInstance(factory, prototype, functionData, enclosingFrame, classPrototype, realm);
        }
        assert JSObjectFactory.verifyPrototype(factory, prototype);
        return JSObjectFactory.newInstance(factory, functionData, enclosingFrame, classPrototype, realm);
    }

    private DynamicObject createES5Strict(DynamicObjectFactory factory, JSFunctionData functionData, MaterializedFrame enclosingFrame, Object classPrototype, JSRealm realm, DynamicObject prototype) {
        if (context.isMultiContext()) {
            return JSObjectFactory.newInstance(factory, prototype, functionData, enclosingFrame, classPrototype, realm, realm.getThrowerAccessor(), realm.getThrowerAccessor());
        } else {
            assert JSObjectFactory.verifyPrototype(factory, prototype);
            return JSObjectFactory.newInstance(factory, functionData, enclosingFrame, classPrototype, realm, realm.getThrowerAccessor(), realm.getThrowerAccessor());
        }
    }

    public final DynamicObject createBound(JSFunctionData functionData, Object classPrototype, JSRealm realm, DynamicObject boundTargetFunction, Object boundThis, Object[] boundArguments) {
        DynamicObject prototype = getPrototype(realm);
        DynamicObjectFactory factory = getFactory(realm, prototype);
        assert functionData != null;
        assert factory.getShape().getObjectType() == JSFunction.INSTANCE;
        assert functionData.hasStrictFunctionProperties();
        if (context.getEcmaScriptVersion() < 6) {
            return createBoundES5(factory, functionData, classPrototype, realm, prototype, boundTargetFunction, boundThis, boundArguments);
        }
        if (context.isMultiContext()) {
            return JSObjectFactory.newInstance(factory, prototype, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, classPrototype, realm, boundTargetFunction, boundThis, boundArguments);
        }
        assert JSObjectFactory.verifyPrototype(factory, prototype);
        return JSObjectFactory.newInstance(factory, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, classPrototype, realm, boundTargetFunction, boundThis, boundArguments);
    }

    private DynamicObject createBoundES5(DynamicObjectFactory factory, JSFunctionData functionData, Object classPrototype, JSRealm realm, DynamicObject prototype,
                    DynamicObject boundTargetFunction, Object boundThis, Object[] boundArguments) {
        if (context.isMultiContext()) {
            return JSObjectFactory.newInstance(factory, prototype, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, classPrototype, realm, realm.getThrowerAccessor(), realm.getThrowerAccessor(),
                            boundTargetFunction, boundThis, boundArguments);
        } else {
            assert JSObjectFactory.verifyPrototype(factory, prototype);
            return JSObjectFactory.newInstance(factory, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, classPrototype, realm, realm.getThrowerAccessor(), realm.getThrowerAccessor(),
                            boundTargetFunction, boundThis, boundArguments);
        }
    }

    protected abstract DynamicObject getPrototype(JSRealm realm);

    protected abstract DynamicObjectFactory getFactory(JSRealm realm, DynamicObject prototype);

    private static final class Default extends JSFunctionFactory {
        private final DynamicObjectFactory factory;

        protected Default(JSContext context, DynamicObjectFactory factory) {
            super(context);
            this.factory = factory;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return realm.getFunctionPrototype();
        }

        @Override
        protected DynamicObjectFactory getFactory(JSRealm realm, DynamicObject prototype) {
            return factory;
        }
    }

    private static final class IntrinsicMulti extends JSFunctionFactory {
        private final CompilableFunction<JSRealm, DynamicObject> getProto;
        private final DynamicObjectFactory factory;

        protected IntrinsicMulti(JSContext context, CompilableFunction<JSRealm, DynamicObject> getProto, boolean isStrict, boolean isAnonymous, boolean isConstructor, boolean isGenerator,
                        boolean isBound) {
            super(context);
            this.factory = makeFactory(context, null, isStrict, isAnonymous, isConstructor, isGenerator, isBound);
            this.getProto = getProto;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return getProto.apply(realm);
        }

        @Override
        protected DynamicObjectFactory getFactory(JSRealm realm, DynamicObject prototype) {
            return factory;
        }
    }

    private static final class IntrinsicRealm extends JSFunctionFactory {
        private final CompilableFunction<JSRealm, DynamicObject> getProto;
        private final boolean isStrict;
        private final boolean isAnonymous;
        private final boolean isConstructor;
        private final boolean isGenerator;
        private final boolean isBound;
        private final int slot;

        protected IntrinsicRealm(JSContext context, CompilableFunction<JSRealm, DynamicObject> getProto, boolean isStrict, boolean isAnonymous, boolean isConstructor, boolean isGenerator,
                        boolean isBound, int slot) {
            super(context);
            this.getProto = getProto;
            this.isStrict = isStrict;
            this.isAnonymous = isAnonymous;
            this.isConstructor = isConstructor;
            this.isGenerator = isGenerator;
            this.isBound = isBound;
            this.slot = slot;
        }

        @Override
        protected DynamicObject getPrototype(JSRealm realm) {
            return getProto.apply(realm);
        }

        @Override
        protected DynamicObjectFactory getFactory(JSRealm realm, DynamicObject prototype) {
            DynamicObjectFactory realmFactory = realm.getObjectFactories().factories[slot];
            if (realmFactory == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                DynamicObjectFactory newFactory = makeFactory(context, prototype, isStrict, isAnonymous, isConstructor, isGenerator, isBound);
                realmFactory = realm.getObjectFactories().factories[slot] = newFactory;
            }
            return realmFactory;
        }
    }
}
