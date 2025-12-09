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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSAsyncGenerator;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSGenerator;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class SpecializedNewObjectNode extends JavaScriptBaseNode {
    protected final JSContext context;
    protected final boolean isBuiltin;
    protected final boolean isConstructor;
    protected final boolean isGenerator;
    protected final boolean isAsyncGenerator;
    @Child private JSTargetableNode getPrototypeNode;
    protected final JSNonProxy instanceLayout;

    public SpecializedNewObjectNode(JSContext context, boolean isBuiltin, boolean isConstructor, boolean isGenerator, boolean isAsyncGenerator, JSNonProxy instanceLayout) {
        this.context = context;
        this.isBuiltin = isBuiltin;
        this.isConstructor = isConstructor;
        this.isGenerator = isGenerator;
        this.isAsyncGenerator = isAsyncGenerator;
        this.instanceLayout = instanceLayout;
        this.getPrototypeNode = (!isBuiltin && isConstructor) ? PropertyNode.createProperty(context, null, JSObject.PROTOTYPE) : null;
    }

    @NeverDefault
    public static SpecializedNewObjectNode create(JSContext context, boolean isBuiltin, boolean isConstructor, boolean isGenerator, boolean isAsyncGenerator, JSNonProxy instanceLayout) {
        return SpecializedNewObjectNodeGen.create(context, isBuiltin, isConstructor, isGenerator, isAsyncGenerator, instanceLayout);
    }

    @NeverDefault
    public static SpecializedNewObjectNode create(JSFunctionData functionData, JSOrdinary instanceLayout) {
        return create(functionData.getContext(), functionData.isBuiltin(), functionData.isConstructor(), functionData.isGenerator(), functionData.isAsyncGenerator(), instanceLayout);
    }

    public final JSDynamicObject execute(VirtualFrame frame, JSDynamicObject newTarget) {
        Object prototype = getPrototypeNode != null ? getPrototypeNode.executeWithTarget(frame, newTarget) : Undefined.instance;
        return execute(newTarget, prototype);
    }

    protected abstract JSDynamicObject execute(JSDynamicObject newTarget, Object prototype);

    @NeverDefault
    protected Shape getShapeWithoutProto() {
        CompilerAsserts.neverPartOfCompilation();
        return JSObjectUtil.getProtoChildShape(null, instanceLayout, context);
    }

    @TruffleBoundary
    JSObjectFactory makeBoundObjectFactory(Object prototype) {
        if (prototype instanceof JSObject jsproto) {
            return JSObjectFactory.createBound(context, jsproto, JSObjectUtil.getProtoChildShape(jsproto, instanceLayout, context));
        }
        return null;
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "!context.isMultiContext()", "isJSObject(cachedPrototype)", "prototype == cachedPrototype"}, limit = "context.getPropertyCacheLimit()")
    public JSDynamicObject doCachedProto(@SuppressWarnings("unused") JSDynamicObject target, @SuppressWarnings("unused") Object prototype,
                    @Cached("prototype") @SuppressWarnings("unused") Object cachedPrototype,
                    @Cached("makeBoundObjectFactory(prototype)") JSObjectFactory factory) {
        JSRealm realm = getRealm();
        if (isAsyncGenerator) {
            return JSAsyncGenerator.create(factory, realm, (JSObject) cachedPrototype);
        } else if (isGenerator) {
            return JSGenerator.create(factory, realm, (JSObject) cachedPrototype);
        }
        return JSOrdinary.create(factory, realm, (JSObject) cachedPrototype);
    }

    /** Many different prototypes. */
    @ReportPolymorphism.Megamorphic
    @Specialization(guards = {"!isBuiltin", "isConstructor", "!context.isMultiContext()"}, replaces = "doCachedProto")
    public JSDynamicObject doUncachedProto(@SuppressWarnings("unused") JSDynamicObject target, JSObject prototype,
                    @Cached InlinedBranchProfile slowBranch) {
        if (isAsyncGenerator) {
            return JSAsyncGenerator.create(context, getRealm(), prototype);
        } else if (isGenerator) {
            return JSGenerator.create(context, getRealm(), prototype);
        }
        Shape shape = JSObjectUtil.getProtoChildShape(prototype, instanceLayout, context, this, slowBranch);
        return JSOrdinary.create(context, shape, prototype);
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "context.isMultiContext()", "prototypeClass != null", "prototypeClass.isInstance(prototype)"}, limit = "1")
    public JSDynamicObject createWithProtoCachedClass(@SuppressWarnings("unused") JSDynamicObject target, Object prototype,
                    @Cached @Shared DynamicObject.PutNode setProtoNode,
                    @Cached(value = "getClassIfJSObject(prototype)") Class<?> prototypeClass,
                    @Cached("getShapeWithoutProto()") @Shared Shape cachedShape) {
        return createWithProto(target, (JSObject) prototypeClass.cast(prototype), setProtoNode, cachedShape);
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "context.isMultiContext()"})
    public JSDynamicObject createWithProto(@SuppressWarnings("unused") JSDynamicObject target, JSObject prototype,
                    @Cached @Shared DynamicObject.PutNode setProtoNode,
                    @Cached("getShapeWithoutProto()") @Shared Shape cachedShape) {
        JSRealm realm = getRealm();
        if (isAsyncGenerator) {
            return JSAsyncGenerator.create(context, realm, prototype);
        } else if (isGenerator) {
            return JSGenerator.create(context, realm, prototype);
        }
        JSDynamicObject object = JSOrdinary.create(context, cachedShape, prototype);
        setProtoNode.execute(object, JSObject.HIDDEN_PROTO, prototype);
        return object;
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "!isJSObject(prototype)"})
    public JSDynamicObject createDefaultProto(JSDynamicObject target, @SuppressWarnings("unused") Object prototype) {
        // user-provided prototype is not an object
        JSRealm realm = JSRuntime.getFunctionRealm(target, getRealm());
        if (isAsyncGenerator) {
            return JSAsyncGenerator.create(context, realm);
        } else if (isGenerator) {
            return JSGenerator.create(context, realm);
        }
        return JSOrdinary.create(context, realm);
    }

    @Specialization(guards = {"isBuiltin", "isConstructor"})
    static JSDynamicObject builtinConstructor(@SuppressWarnings("unused") JSDynamicObject target, @SuppressWarnings("unused") Object proto) {
        return JSFunction.CONSTRUCT;
    }

    @Specialization(guards = {"!isConstructor"})
    public JSDynamicObject throwNotConstructorFunctionTypeError(JSDynamicObject target, @SuppressWarnings("unused") Object proto) {
        throw Errors.createTypeErrorNotAConstructor(target, context);
    }
}
