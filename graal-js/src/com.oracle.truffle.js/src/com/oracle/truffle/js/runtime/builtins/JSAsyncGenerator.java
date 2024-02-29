/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.AsyncGeneratorPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSAsyncGenerator extends JSNonProxy implements JSConstructorFactory, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("AsyncGenerator");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("AsyncGenerator.prototype");
    public static final TruffleString TO_STRING_TAG = CLASS_NAME;

    public static final JSAsyncGenerator INSTANCE = new JSAsyncGenerator();

    private JSAsyncGenerator() {
    }

    public static JSAsyncGeneratorObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        return create(context.getAsyncGeneratorObjectFactory(), realm, proto);
    }

    public static JSAsyncGeneratorObject create(JSContext context, JSRealm realm) {
        return create(context.getAsyncGeneratorObjectFactory(), realm);
    }

    public static JSAsyncGeneratorObject create(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto) {
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSAsyncGeneratorObject(shape, proto), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static JSAsyncGeneratorObject create(JSObjectFactory factory, JSRealm realm) {
        return create(factory, realm, factory.getPrototype(realm));
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        // intrinsic object %AsyncGeneratorPrototype%
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, realm.getAsyncIteratorPrototype());
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, AsyncGeneratorPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putDataProperty(prototype, JSObject.CONSTRUCTOR, constructor, JSAttributes.configurableNotEnumerableNotWritable());
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getAsyncGeneratorObjectPrototype();
    }
}
