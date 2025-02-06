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

import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.PromiseFunctionBuiltins;
import com.oracle.truffle.js.builtins.PromisePrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSPromise extends JSNonProxy implements JSConstructorFactory.Default.WithFunctionsAndSpecies, PrototypeSupplier {
    public static final TruffleString CLASS_NAME = Strings.constant("Promise");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Promise.prototype");

    public static final JSPromise INSTANCE = new JSPromise();

    public static final TruffleString RESOLVE = Strings.RESOLVE;
    public static final TruffleString THEN = Strings.THEN;

    // for Promise.prototype.finally
    public static final HiddenKey PROMISE_ON_FINALLY = new HiddenKey("OnFinally");
    public static final HiddenKey PROMISE_FINALLY_CONSTRUCTOR = new HiddenKey("Constructor");

    // Promise states
    public static final int PENDING = 0;
    public static final int FULFILLED = 1;
    public static final int REJECTED = 2;

    // HostPromiseRejectionTracker operations
    public static final int REJECTION_TRACKER_OPERATION_REJECT = 0;
    public static final int REJECTION_TRACKER_OPERATION_HANDLE = 1;
    // V8 extensions of HostPromiseRejectionTracker
    public static final int REJECTION_TRACKER_OPERATION_REJECT_AFTER_RESOLVED = 2;
    public static final int REJECTION_TRACKER_OPERATION_RESOLVE_AFTER_RESOLVED = 3;

    private JSPromise() {
    }

    public static JSPromiseObject create(JSContext context, JSRealm realm) {
        JSObjectFactory factory = context.getPromiseFactory();
        return create(factory, realm, factory.getPrototype(realm));
    }

    public static JSPromiseObject create(JSContext context, JSRealm realm, JSDynamicObject proto) {
        JSObjectFactory factory = context.getPromiseFactory();
        return create(factory, realm, proto);
    }

    private static JSPromiseObject create(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto) {
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSPromiseObject(shape, proto, PENDING), realm, proto);
        return factory.trackAllocation(newObj);
    }

    public static JSPromiseObject create(JSContext context, Shape shape, JSDynamicObject proto) {
        JSPromiseObject promise = JSPromiseObject.create(shape, proto, PENDING);
        return context.trackAllocation(promise);
    }

    public static JSPromiseObject createWithoutPrototype(JSContext context, JSDynamicObject proto) {
        Shape shape = context.getPromiseShapePrototypeInObject();
        JSPromiseObject obj = JSPromiseObject.create(shape, proto, PENDING);
        // prototype is set in caller
        return obj;
    }

    @Override
    public Shape makeInitialShape(JSContext context, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
    }

    public static boolean isJSPromise(Object obj) {
        return obj instanceof JSPromiseObject;
    }

    public static boolean isRejected(JSPromiseObject promise) {
        return REJECTED == getPromiseState(promise);
    }

    public static boolean isPending(JSPromiseObject promise) {
        return PENDING == getPromiseState(promise);
    }

    public static boolean isFulfilled(JSPromiseObject promise) {
        return FULFILLED == getPromiseState(promise);
    }

    public static int getPromiseState(JSPromiseObject promise) {
        return promise.getPromiseState();
    }

    public static void setPromiseState(JSPromiseObject promise, int promiseState) {
        assert isPending(promise) : getStatus(promise);
        promise.setPromiseState(promiseState);
    }

    public static TruffleString getStatus(JSPromiseObject obj) {
        if (isFulfilled(obj)) {
            return Strings.RESOLVED;
        } else if (isRejected(obj)) {
            return Strings.REJECTED;
        } else {
            assert isPending(obj);
            return Strings.PENDING;
        }
    }

    public static Object getPromiseResult(JSPromiseObject obj) {
        return JSRuntime.nullToUndefined(obj.getPromiseResult());
    }

    public static void throwIfRejected(JSPromiseObject promise, JSRealm realm) {
        if (JSPromise.isRejected(promise)) {
            if (!promise.isHandled()) {
                // Notify the promise rejection tracker that this promise has been handled.
                realm.getContext().notifyPromiseRejectionTracker(promise, JSPromise.REJECTION_TRACKER_OPERATION_HANDLE, Undefined.instance, realm.getAgent());
                promise.setIsHandled(true);
            }
            throw JSRuntime.getException(JSPromise.getPromiseResult(promise));
        }
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, constructor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, PromisePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, PromiseFunctionBuiltins.BUILTINS);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getPromisePrototype();
    }

}
