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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSDictionaryObject;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class CreateObjectNode extends JavaScriptBaseNode {
    protected final JSContext context;

    protected CreateObjectNode(JSContext context) {
        this.context = context;
    }

    public static CreateObjectNode create(JSContext context) {
        return new CreateOrdinaryObjectNode(context);
    }

    public static CreateObjectWithPrototypeNode createWithPrototype(JSContext context, JavaScriptNode prototypeExpression) {
        return createWithPrototype(context, prototypeExpression, JSUserObject.INSTANCE);
    }

    public static CreateObjectWithPrototypeNode createWithPrototype(JSContext context, JavaScriptNode prototypeExpression, JSClass jsclass) {
        return new CreateObjectWithUncachedPrototypeNode(context, prototypeExpression, jsclass);
    }

    public static CreateObjectWithPrototypeNode createWithCachedPrototype(JSContext context, JavaScriptNode prototypeExpression) {
        return createWithCachedPrototype(context, prototypeExpression, JSUserObject.INSTANCE);
    }

    public static CreateObjectWithPrototypeNode createWithCachedPrototype(JSContext context, JavaScriptNode prototypeExpression, JSClass jsclass) {
        return new CreateObjectWithCachedPrototypeNode(context, prototypeExpression, jsclass);
    }

    static CreateObjectNode createDictionary(JSContext context) {
        return new CreateDictionaryObjectNode(context);
    }

    public final DynamicObject execute(VirtualFrame frame) {
        return executeDynamicObject(frame);
    }

    public abstract DynamicObject executeDynamicObject(VirtualFrame frame);

    protected abstract CreateObjectNode copyUninitialized();

    final JSContext getContext() {
        return context;
    }

    private static class CreateOrdinaryObjectNode extends CreateObjectNode {
        protected CreateOrdinaryObjectNode(JSContext context) {
            super(context);
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame) {
            return JSUserObject.create(context);
        }

        @Override
        protected CreateObjectNode copyUninitialized() {
            return new CreateOrdinaryObjectNode(context);
        }
    }

    public abstract static class CreateObjectWithPrototypeNode extends CreateObjectNode {
        @Child protected JavaScriptNode prototypeExpression;
        private final ConditionProfile isNormalPrototype = ConditionProfile.createBinaryProfile();

        protected CreateObjectWithPrototypeNode(JSContext context, JavaScriptNode prototypeExpression) {
            super(context);
            this.prototypeExpression = prototypeExpression;
        }

        @Override
        public final DynamicObject executeDynamicObject(VirtualFrame frame) {
            Object prototype = prototypeExpression.execute(frame);
            if (isNormalPrototype.profile(JSObject.isDynamicObject(prototype) && prototype != Undefined.instance)) {
                if (prototype == Null.instance) {
                    prototype = null;
                }
                return executeDynamicObject(frame, (DynamicObject) prototype);
            } else {
                return JSUserObject.create(context);
            }
        }

        public abstract DynamicObject executeDynamicObject(VirtualFrame frame, DynamicObject prototype);

        abstract JSClass getJSClass();

        @Override
        protected CreateObjectWithPrototypeNode copyUninitialized() {
            return createWithCachedPrototype(context, JavaScriptNode.cloneUninitialized(prototypeExpression), getJSClass());
        }
    }

    private static class CreateObjectWithCachedPrototypeNode extends CreateObjectWithPrototypeNode {
        @CompilationFinal private DynamicObject cachedPrototype;
        @CompilationFinal private Shape protoChildShape;
        private final JSClass jsclass;

        protected CreateObjectWithCachedPrototypeNode(JSContext context, JavaScriptNode prototypeExpression, JSClass jsclass) {
            super(context, prototypeExpression);
            this.jsclass = jsclass;
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame, DynamicObject prototype) {
            if (cachedPrototype == null || protoChildShape == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedPrototype = prototype;
                protoChildShape = prototype == null ? context.getEmptyShape() : JSObjectUtil.getProtoChildShape(prototype, jsclass, context);
            }
            if (cachedPrototype == prototype) {
                return JSObject.create(context, protoChildShape);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                CreateObjectWithPrototypeNode uncached;
                if (jsclass == JSUserObject.INSTANCE) {
                    uncached = new CreateOrdinaryObjectWithPrototypeInObjectNode(context, prototypeExpression);
                } else {
                    uncached = new CreateObjectWithUncachedPrototypeNode(context, prototypeExpression, jsclass);
                }
                return this.replace(uncached).executeDynamicObject(frame, prototype);
            }
        }

        @Override
        JSClass getJSClass() {
            return jsclass;
        }
    }

    private static class CreateOrdinaryObjectWithPrototypeInObjectNode extends CreateObjectWithPrototypeNode {
        protected CreateOrdinaryObjectWithPrototypeInObjectNode(JSContext context, JavaScriptNode prototypeExpression) {
            super(context, prototypeExpression);
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame, DynamicObject prototype) {
            return JSUserObject.createWithPrototypeInObject(prototype, context);
        }

        @Override
        JSClass getJSClass() {
            return JSUserObject.INSTANCE;
        }
    }

    private static class CreateObjectWithUncachedPrototypeNode extends CreateObjectWithPrototypeNode {
        private final JSClass jsclass;

        protected CreateObjectWithUncachedPrototypeNode(JSContext context, JavaScriptNode prototypeExpression, JSClass jsclass) {
            super(context, prototypeExpression);
            this.jsclass = jsclass;
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame, DynamicObject prototype) {
            return JSObject.create(context, prototype, jsclass);
        }

        @Override
        JSClass getJSClass() {
            return jsclass;
        }
    }

    private static class CreateDictionaryObjectNode extends CreateObjectNode {
        protected CreateDictionaryObjectNode(JSContext context) {
            super(context);
        }

        @Override
        public DynamicObject executeDynamicObject(VirtualFrame frame) {
            return JSDictionaryObject.create(context);
        }

        @Override
        protected CreateObjectNode copyUninitialized() {
            return new CreateDictionaryObjectNode(context);
        }
    }
}
