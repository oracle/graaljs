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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
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
    protected final JSOrdinary instanceLayout;

    public SpecializedNewObjectNode(JSContext context, boolean isBuiltin, boolean isConstructor, boolean isGenerator, boolean isAsyncGenerator, JSOrdinary instanceLayout) {
        this.context = context;
        this.isBuiltin = isBuiltin;
        this.isConstructor = isConstructor;
        this.isGenerator = isGenerator;
        this.isAsyncGenerator = isAsyncGenerator;
        this.instanceLayout = instanceLayout;
        this.getPrototypeNode = (!isBuiltin && isConstructor) ? PropertyNode.createProperty(context, null, JSObject.PROTOTYPE) : null;
    }

    public static SpecializedNewObjectNode create(JSContext context, boolean isBuiltin, boolean isConstructor, boolean isGenerator, boolean isAsyncGenerator, JSOrdinary instanceLayout) {
        return SpecializedNewObjectNodeGen.create(context, isBuiltin, isConstructor, isGenerator, isAsyncGenerator, instanceLayout);
    }

    public static SpecializedNewObjectNode create(JSContext context, boolean isBuiltin, boolean isConstructor, boolean isGenerator, boolean isAsyncGenerator) {
        return create(context, isBuiltin, isConstructor, isGenerator, isAsyncGenerator, JSOrdinary.INSTANCE);
    }

    public static SpecializedNewObjectNode create(JSFunctionData functionData, JSOrdinary instanceLayout) {
        return create(functionData.getContext(), functionData.isBuiltin(), functionData.isConstructor(), functionData.isGenerator(), functionData.isAsyncGenerator(), instanceLayout);
    }

    public final DynamicObject execute(VirtualFrame frame, DynamicObject newTarget) {
        Object prototype = getPrototypeNode != null ? getPrototypeNode.executeWithTarget(frame, newTarget) : Undefined.instance;
        return execute(newTarget, prototype);
    }

    protected abstract DynamicObject execute(DynamicObject newTarget, Object prototype);

    protected Shape getProtoChildShape(Object prototype) {
        CompilerAsserts.neverPartOfCompilation();
        if (JSGuards.isJSObject(prototype)) {
            JSObject jsproto = (JSObject) prototype;
            return JSObjectUtil.getProtoChildShape(jsproto, instanceLayout, context);
        }
        return null;
    }

    protected Shape getShapeWithoutProto() {
        CompilerAsserts.neverPartOfCompilation();
        return JSObjectUtil.getProtoChildShape(null, instanceLayout, context);
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "!context.isMultiContext()", "isJSObject(cachedPrototype)", "prototype == cachedPrototype"}, limit = "context.getPropertyCacheLimit()")
    public DynamicObject doCachedProto(@SuppressWarnings("unused") DynamicObject target, @SuppressWarnings("unused") Object prototype,
                    @Cached("prototype") @SuppressWarnings("unused") Object cachedPrototype,
                    @Cached("getProtoChildShape(prototype)") Shape shape) {
        return JSOrdinary.create(context, shape);
    }

    /** Many different prototypes. */
    @ReportPolymorphism.Megamorphic
    @Specialization(guards = {"!isBuiltin", "isConstructor", "!context.isMultiContext()", "isJSObject(prototype)"}, replaces = "doCachedProto")
    public DynamicObject doUncachedProto(@SuppressWarnings("unused") DynamicObject target, DynamicObject prototype,
                    @Cached("create()") BranchProfile slowBranch) {
        Shape shape = JSObjectUtil.getProtoChildShape(prototype, instanceLayout, context, slowBranch);
        return JSOrdinary.create(context, shape);
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "context.isMultiContext()", "prototypeClass != null", "prototypeClass.isInstance(prototype)"}, limit = "1")
    public DynamicObject createWithProtoCachedClass(@SuppressWarnings("unused") DynamicObject target, Object prototype,
                    @CachedLibrary(limit = "3") @Shared("setProtoNode") DynamicObjectLibrary setProtoNode,
                    @Cached("getClassIfJSObject(prototype)") Class<?> prototypeClass,
                    @Cached("getShapeWithoutProto()") Shape cachedShape) {
        return createWithProto(target, (DynamicObject) prototypeClass.cast(prototype), setProtoNode, cachedShape);
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "context.isMultiContext()", "isJSObject(prototype)"})
    public DynamicObject createWithProto(@SuppressWarnings("unused") DynamicObject target, DynamicObject prototype,
                    @CachedLibrary(limit = "3") @Shared("setProtoNode") DynamicObjectLibrary setProtoNode,
                    @Cached("getShapeWithoutProto()") Shape cachedShape) {
        DynamicObject object = JSOrdinary.create(context, cachedShape);
        setProtoNode.put(object, JSObject.HIDDEN_PROTO, prototype);
        return object;
    }

    @Specialization(guards = {"!isBuiltin", "isConstructor", "!isJSObject(prototype)"})
    public DynamicObject createDefaultProto(DynamicObject target, @SuppressWarnings("unused") Object prototype) {
        // user-provided prototype is not an object
        JSRealm realm = JSRuntime.getFunctionRealm(target, getRealm());
        if (isAsyncGenerator) {
            return JSOrdinary.createWithRealm(context, context.getAsyncGeneratorObjectFactory(), realm);
        } else if (isGenerator) {
            return JSOrdinary.createWithRealm(context, context.getGeneratorObjectFactory(), realm);
        }
        return JSOrdinary.create(context, realm);
    }

    @Specialization(guards = {"isBuiltin", "isConstructor"})
    static DynamicObject builtinConstructor(@SuppressWarnings("unused") DynamicObject target, @SuppressWarnings("unused") Object proto) {
        return JSFunction.CONSTRUCT;
    }

    @Specialization(guards = {"!isConstructor"})
    public DynamicObject throwNotConstructorFunctionTypeError(DynamicObject target, @SuppressWarnings("unused") Object proto) {
        throw Errors.createTypeErrorNotAConstructor(target, context);
    }
}
