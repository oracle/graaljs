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

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.CompilableFunction;

public class OrdinaryCreateFromConstructorNode extends JavaScriptNode {
    @Child private GetPrototypeFromConstructorNode getPrototypeFromConstructorNode;
    @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;

    protected OrdinaryCreateFromConstructorNode(JSContext context, JavaScriptNode constructorNode, CompilableFunction<JSRealm, JSDynamicObject> intrinsicDefaultProto, JSClass jsclass) {
        this.getPrototypeFromConstructorNode = GetPrototypeFromConstructorNode.create(context, constructorNode, intrinsicDefaultProto);
        this.createObjectNode = CreateObjectNode.createWithPrototype(context, jsclass);
    }

    private OrdinaryCreateFromConstructorNode(GetPrototypeFromConstructorNode getPrototypeFromConstructorNode, CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode) {
        this.getPrototypeFromConstructorNode = getPrototypeFromConstructorNode;
        this.createObjectNode = createObjectNode;
    }

    public static OrdinaryCreateFromConstructorNode create(JSContext context, JavaScriptNode constructorNode, CompilableFunction<JSRealm, JSDynamicObject> intrinsicDefaultProto, JSClass jsclass) {
        return new OrdinaryCreateFromConstructorNode(context, constructorNode, intrinsicDefaultProto, jsclass);
    }

    @Override
    public JSObject execute(VirtualFrame frame) {
        JSDynamicObject proto = getPrototypeFromConstructorNode.execute(frame);
        return executeWithPrototype(proto);
    }

    public JSObject executeWithConstructor(JSDynamicObject constructor) {
        JSDynamicObject proto = getPrototypeFromConstructorNode.executeWithConstructor(constructor);
        return executeWithPrototype(proto);
    }

    private JSObject executeWithPrototype(JSDynamicObject proto) {
        return createObjectNode.execute(proto);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == JSDynamicObject.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new OrdinaryCreateFromConstructorNode(cloneUninitialized(getPrototypeFromConstructorNode, materializedTags), createObjectNode);
    }
}
