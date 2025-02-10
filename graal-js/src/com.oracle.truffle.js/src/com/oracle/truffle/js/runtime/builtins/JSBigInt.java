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

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.BigIntFunctionBuiltins;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltins;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public final class JSBigInt extends JSPrimitive implements JSConstructorFactory.Default.WithFunctions {

    public static final TruffleString TYPE_NAME = Strings.constant("bigint");
    public static final TruffleString CLASS_NAME = Strings.constant("BigInt");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("BigInt.prototype");

    public static final JSBigInt INSTANCE = new JSBigInt();

    private JSBigInt() {
    }

    @InliningCutoff
    public static JSBigIntObject create(JSContext context, JSRealm realm, BigInt value) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm), value);
    }

    @InliningCutoff
    public static JSBigIntObject create(JSContext context, JSRealm realm, JSDynamicObject proto, BigInt value) {
        JSObjectFactory factory = context.getBigIntFactory();
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSBigIntObject(shape, proto, value), realm, proto);
        return factory.trackAllocation(newObj);
    }

    private static BigInt getBigIntegerField(JSDynamicObject obj) {
        return ((JSBigIntObject) obj).getBigIntValue();
    }

    public static BigInt valueOf(JSDynamicObject obj) {
        return getBigIntegerField(obj);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSObject bigIntPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(bigIntPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, bigIntPrototype, BigIntPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(bigIntPrototype, CLASS_NAME);
        return bigIntPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, BigIntFunctionBuiltins.BUILTINS);
    }

    public static boolean isJSBigInt(Object obj) {
        return obj instanceof JSBigIntObject;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getBigIntPrototype();
    }
}
