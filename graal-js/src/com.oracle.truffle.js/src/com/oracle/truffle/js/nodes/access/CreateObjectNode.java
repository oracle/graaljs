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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDictionary;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;

public abstract class CreateObjectNode extends JavaScriptBaseNode {
    protected final JSContext context;

    protected CreateObjectNode(JSContext context) {
        this.context = context;
    }

    @NeverDefault
    public static CreateObjectNode create(JSContext context) {
        return new CreateOrdinaryObjectNode(context);
    }

    @NeverDefault
    public static CreateObjectWithPrototypeNode createOrdinaryWithPrototype(JSContext context) {
        return createWithPrototype(context, JSOrdinary.INSTANCE);
    }

    @NeverDefault
    public static CreateObjectWithPrototypeNode createWithPrototype(JSContext context, JSClass jsclass) {
        return CreateObjectWithPrototypeNode.create(context, jsclass);
    }

    @NeverDefault
    static CreateObjectNode createDictionary(JSContext context) {
        return new CreateDictionaryObjectNode(context);
    }

    public final JSObject execute(JSRealm realm) {
        return executeWithPrototype(realm, realm.getObjectPrototype());
    }

    public abstract JSObject executeWithPrototype(JSRealm realm, Object proto);

    public boolean seenArrayPrototype() {
        return false;
    }

    private static class CreateOrdinaryObjectNode extends CreateObjectNode {
        protected CreateOrdinaryObjectNode(JSContext context) {
            super(context);
        }

        @Override
        public JSObject executeWithPrototype(JSRealm realm, Object proto) {
            assert proto == realm.getObjectPrototype() : proto;
            return JSOrdinary.create(context, realm);
        }
    }

    public abstract static class CreateObjectWithPrototypeNode extends CreateObjectNode {
        protected final JSClass jsclass;

        @Child private DynamicObject.GetShapeFlagsNode getShapeFlagsNode;
        @Child private DynamicObject.SetShapeFlagsNode setShapeFlagsNode;
        @CompilationFinal private boolean seenArrayPrototype;

        protected CreateObjectWithPrototypeNode(JSContext context, JSClass jsclass) {
            super(context);
            this.jsclass = jsclass;
            assert isOrdinaryObject() || isPromiseObject();
        }

        public abstract JSObject execute(Object prototype);

        @Override
        public final JSObject executeWithPrototype(JSRealm realm, Object proto) {
            return execute(proto);
        }

        protected static CreateObjectWithPrototypeNode create(JSContext context, JSClass jsclass) {
            return CreateObjectNodeFactory.CreateObjectWithPrototypeNodeGen.create(context, jsclass);
        }

        @Specialization(guards = {"!context.isMultiContext()", "isValidPrototype(cachedPrototype)", "prototype == cachedPrototype"}, limit = "1")
        final JSObject doCachedPrototype(@SuppressWarnings("unused") JSDynamicObject prototype,
                        @Cached("prototype") @SuppressWarnings("unused") JSDynamicObject cachedPrototype,
                        @Cached("getProtoChildShape(cachedPrototype)") Shape protoChildShape) {
            if (isPromiseObject()) {
                return JSPromise.create(context, protoChildShape, cachedPrototype);
            } else if (isOrdinaryObject()) {
                return JSOrdinary.create(context, protoChildShape, cachedPrototype);
            } else {
                throw Errors.unsupported("unsupported object type");
            }
        }

        @Specialization(guards = {"isOrdinaryObject()", "isValidPrototype(prototype)"}, replaces = "doCachedPrototype")
        final JSObject doOrdinaryInstancePrototype(JSDynamicObject prototype,
                        @Cached @Shared DynamicObject.PutNode setProtoNode) {
            JSObject object = JSOrdinary.createWithoutPrototype(context, prototype);
            Properties.put(setProtoNode, object, JSObject.HIDDEN_PROTO, prototype);
            handleArrayPrototype(object, prototype);
            return object;
        }

        @Specialization(guards = {"isPromiseObject()", "isValidPrototype(prototype)"}, replaces = "doCachedPrototype")
        final JSObject doPromiseInstancePrototype(JSDynamicObject prototype,
                        @Cached @Shared DynamicObject.PutNode setProtoNode) {
            JSObject object = JSPromise.createWithoutPrototype(context, prototype);
            Properties.put(setProtoNode, object, JSObject.HIDDEN_PROTO, prototype);
            return object;
        }

        @Specialization(guards = {"isOrdinaryObject() || isPromiseObject()", "!isValidPrototype(prototype)"})
        final JSObject doNotJSObjectOrNull(@SuppressWarnings("unused") Object prototype) {
            return JSOrdinary.create(context, getRealm());
        }

        @NeverDefault
        final Shape getProtoChildShape(JSDynamicObject prototype) {
            if (prototype == Null.instance) {
                return context.getEmptyShapeNullPrototype();
            } else {
                Shape derivedShape = JSObjectUtil.getProtoChildShape(prototype, jsclass, context);
                if (isOrdinaryObject() && JSShape.isArrayPrototypeOrDerivative(prototype)) {
                    seenArrayPrototype = true;
                    derivedShape = Shape.newBuilder(derivedShape).shapeFlags(derivedShape.getFlags() | JSShape.ARRAY_PROTOTYPE_FLAG).build();
                }
                return derivedShape;
            }
        }

        @Idempotent
        final boolean isOrdinaryObject() {
            return jsclass == JSOrdinary.INSTANCE;
        }

        @Idempotent
        final boolean isPromiseObject() {
            return jsclass == JSPromise.INSTANCE;
        }

        /**
         * Check if the prototype is (derived from) %Array.prototype%, and if so, mark the new
         * object as a potential array prototype tracked by the no-prototype-elements assumption.
         */
        private void handleArrayPrototype(JSObject object, JSDynamicObject proto) {
            if (JSShape.isArrayPrototypeOrDerivative(proto)) {
                markAsArrayPrototype(object);
            }
        }

        private void markAsArrayPrototype(JSObject object) {
            if (getShapeFlagsNode == null || setShapeFlagsNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // Only used by member nodes that are guaranteed to be in the same compilation unit,
                // so a transferToInterpreterAndInvalidate() should be sufficient for this flag.
                seenArrayPrototype = true;
                getShapeFlagsNode = insert(DynamicObject.GetShapeFlagsNode.create());
                setShapeFlagsNode = insert(DynamicObject.SetShapeFlagsNode.create());
            }
            assert JSOrdinary.isJSOrdinaryObject(object) : object;
            setShapeFlagsNode.execute(object, getShapeFlagsNode.execute(object) | JSShape.ARRAY_PROTOTYPE_FLAG);
        }

        @Override
        public boolean seenArrayPrototype() {
            return seenArrayPrototype;
        }
    }

    private static class CreateDictionaryObjectNode extends CreateObjectNode {
        protected CreateDictionaryObjectNode(JSContext context) {
            super(context);
        }

        @Override
        public JSObject executeWithPrototype(JSRealm realm, Object proto) {
            assert proto == realm.getObjectPrototype() : proto;
            return JSDictionary.create(context, realm);
        }
    }
}
