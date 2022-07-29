/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.builtins.WrapForAsyncIteratorPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSWrapForAsyncIterator extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final JSWrapForAsyncIterator INSTANCE = new JSWrapForAsyncIterator();

    private JSWrapForAsyncIterator() {
    }

    public static JSWrapForAsyncIteratorObject create(JSContext context, JSRealm realm, IteratorRecord iteratorRecord) {
        JSObjectFactory factory = context.getWrapForAsyncIteratorFactory();
        JSWrapForAsyncIteratorObject obj = factory.initProto(new JSWrapForAsyncIteratorObject(factory.getShape(realm), iteratorRecord), realm);
        return context.trackAllocation(obj);
    }

    public static boolean isWrapForAsyncIterator(Object obj) {
        return obj instanceof JSWrapForAsyncIteratorObject;
    }

    public static JSWrapForAsyncIteratorObject asWrapForIterator(Object obj) {
        assert isWrapForAsyncIterator(obj);
        return (JSWrapForAsyncIteratorObject) obj;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSDynamicObject iteratorPrototype = realm.getAsyncIteratorPrototype();
        JSDynamicObject wrapForIteratorPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, iteratorPrototype);
        JSObjectUtil.putFunctionsFromContainer(realm, wrapForIteratorPrototype, WrapForAsyncIteratorPrototypeBuiltins.BUILTINS);
        return wrapForIteratorPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        return initialShape;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getWrapForAsyncIteratorPrototype();
    }

    @Override
    public TruffleString getClassName() {
        return JSIterator.CLASS_NAME;
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return getClassName();
    }

}
