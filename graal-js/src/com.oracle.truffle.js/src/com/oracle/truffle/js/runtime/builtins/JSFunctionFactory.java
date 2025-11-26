/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.function.InitFunctionNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;

public abstract class JSFunctionFactory {
    protected final JSContext context;
    protected final JSObjectFactory objectFactory;

    public static JSFunctionFactory create(JSContext context, JSDynamicObject prototype) {
        Shape initialShape = JSFunction.makeFunctionShape(context, prototype, false, false);
        JSObjectFactory factory = prototype == null ? JSObjectFactory.createUnbound(context, initialShape) : JSObjectFactory.createBound(context, prototype, initialShape);
        return new JSFunctionFactory.Default(context, factory);
    }

    static JSFunctionFactory createIntrinsic(JSContext context, JSObjectFactory objectFactory,
                    boolean isStrict, boolean isConstructor, boolean isGenerator, boolean isBound, boolean isAsync) {
        return new JSFunctionFactory.Intrinsic(context, objectFactory, isStrict, isConstructor, isGenerator, isBound, isAsync);
    }

    @SuppressWarnings("unused")
    static Shape makeShape(JSContext context, JSDynamicObject prototype,
                    boolean isStrict, boolean isAnonymous, boolean isConstructor, boolean isGenerator, boolean isBound, boolean isAsync) {
        return JSFunction.makeFunctionShape(context, prototype, isGenerator, isAsync);
    }

    protected JSFunctionFactory(JSContext context, JSObjectFactory objectFactory) {
        this.context = context;
        this.objectFactory = objectFactory;
    }

    public final JSFunctionObject create(JSFunctionData functionData, MaterializedFrame enclosingFrame, Object classPrototype, JSRealm realm) {
        return createWithPrototype(functionData, enclosingFrame, classPrototype, realm, getPrototype(realm));
    }

    public final JSFunctionObject createWithPrototype(JSFunctionData functionData, MaterializedFrame enclosingFrame, Object classPrototype, JSRealm realm, JSDynamicObject prototype) {
        Shape shape = getShape(realm, prototype);
        assert functionData != null;
        assert enclosingFrame != null; // use JSFrameUtil.NULL_MATERIALIZED_FRAME instead
        assert shape.getDynamicType() == JSFunction.INSTANCE;
        JSFunctionObject obj = JSFunctionObject.create(shape, prototype, functionData, enclosingFrame, realm, classPrototype);
        objectFactory.initProto(obj, realm, prototype);
        initProperties(obj, functionData);
        if (context.getEcmaScriptVersion() < 6 && functionData.hasStrictFunctionProperties()) {
            initES5StrictProperties(obj, realm);
        }
        return obj;
    }

    protected abstract void initProperties(JSFunctionObject obj, JSFunctionData functionData);

    public final JSFunctionObject createBound(JSFunctionData functionData, Object classPrototype, JSRealm realm, Object boundTargetFunction, Object boundThis, Object[] boundArguments) {
        Shape shape = objectFactory.getShape(realm);
        assert shape.getDynamicType() == JSFunction.INSTANCE;
        assert functionData.hasStrictFunctionProperties();
        if (context.getEcmaScriptVersion() < 6) {
            return createBoundES5(shape, functionData, classPrototype, realm, boundTargetFunction, boundThis, boundArguments);
        }
        JSDynamicObject proto = objectFactory.getPrototype(realm);
        JSFunctionObject obj = JSFunctionObject.createBound(shape, proto, functionData, realm, classPrototype, boundTargetFunction, boundThis, boundArguments);
        objectFactory.initProto(obj, realm, proto);
        initProperties(obj, functionData);
        return obj;
    }

    @InliningCutoff
    private JSFunctionObject createBoundES5(Shape shape, JSFunctionData functionData, Object classPrototype, JSRealm realm,
                    Object boundTargetFunction, Object boundThis, Object[] boundArguments) {
        JSDynamicObject proto = objectFactory.getPrototype(realm);
        JSFunctionObject obj = JSFunctionObject.createBound(shape, proto, functionData, realm, classPrototype, boundTargetFunction, boundThis, boundArguments);
        objectFactory.initProto(obj, realm, proto);
        initProperties(obj, functionData);
        initES5StrictProperties(obj, realm);
        return obj;
    }

    @TruffleBoundary
    private static void initES5StrictProperties(JSObject obj, JSRealm realm) {
        int propertyFlags = JSAttributes.notConfigurableNotEnumerable() | JSProperty.ACCESSOR;
        Accessor throwerAccessor = realm.getThrowerAccessor();
        Properties.putWithFlagsUncached(obj, JSFunction.ARGUMENTS, throwerAccessor, propertyFlags);
        Properties.putWithFlagsUncached(obj, JSFunction.CALLER, throwerAccessor, propertyFlags);
    }

    public final JSFunctionObject createWrapped(JSFunctionData functionData, JSRealm realm, Object wrappedTargetFunction) {
        Shape shape = objectFactory.getShape(realm);
        assert shape.getDynamicType() == JSFunction.INSTANCE;
        assert functionData.hasStrictFunctionProperties();
        JSDynamicObject proto = objectFactory.getPrototype(realm);
        JSFunctionObject obj = JSFunctionObject.createWrapped(shape, proto, functionData, realm, wrappedTargetFunction);
        objectFactory.initProto(obj, realm, proto);
        initProperties(obj, functionData);
        return obj;
    }

    protected abstract JSDynamicObject getPrototype(JSRealm realm);

    protected abstract Shape getShape(JSRealm realm, JSDynamicObject prototype);

    private static final class Default extends JSFunctionFactory {

        protected Default(JSContext context, JSObjectFactory objectFactory) {
            super(context, objectFactory);
        }

        @Override
        protected JSDynamicObject getPrototype(JSRealm realm) {
            return realm.getFunctionPrototype();
        }

        @Override
        protected Shape getShape(JSRealm realm, JSDynamicObject prototype) {
            return objectFactory.getShape(realm, prototype);
        }

        @Override
        protected void initProperties(JSFunctionObject obj, JSFunctionData functionData) {
        }
    }

    private static final class Intrinsic extends JSFunctionFactory {
        private final InitFunctionNode initFunctionNode;

        protected Intrinsic(JSContext context, JSObjectFactory objectFactory, boolean isStrict, boolean isConstructor, boolean isGenerator,
                        boolean isBound, @SuppressWarnings("unused") boolean isAsync) {
            super(context, objectFactory);
            this.initFunctionNode = context.adoptNode(InitFunctionNode.create(context, isStrict, isConstructor, isBound, isGenerator, false));
        }

        @Override
        protected JSDynamicObject getPrototype(JSRealm realm) {
            return objectFactory.getPrototype(realm);
        }

        @Override
        protected Shape getShape(JSRealm realm, JSDynamicObject prototype) {
            return objectFactory.getShape(realm, prototype);
        }

        @Override
        protected void initProperties(JSFunctionObject obj, JSFunctionData functionData) {
            initFunctionNode.execute(obj, functionData);
        }
    }
}
