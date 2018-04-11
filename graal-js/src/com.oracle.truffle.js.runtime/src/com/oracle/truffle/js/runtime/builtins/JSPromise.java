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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSPromise extends JSBuiltinObject {
    public static final String CLASS_NAME = "Promise";

    public static final JSPromise INSTANCE = new JSPromise();

    public static final HiddenKey PROMISE_STATE = new HiddenKey("PromiseState");
    public static final HiddenKey PROMISE_RESULT = new HiddenKey("PromiseResult");
    public static final HiddenKey PROMISE_IS_HANDLED = new HiddenKey("PromiseIsHandled");

    // for Promise.prototype.finally
    public static final HiddenKey PROMISE_ON_FINALLY = new HiddenKey("OnFinally");
    public static final HiddenKey PROMISE_FINALLY_CONSTRUCTOR = new HiddenKey("Constructor");

    private static final Integer PENDING = 0;
    private static final Integer FULFILLED = 1;
    private static final Integer REJECTED = 2;

    private JSPromise() {
    }

    public static DynamicObject create(JSContext context) {
        return JSObject.create(context, context.getPromiseFactory());
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    public static Shape makeInitialShape(JSRealm realm) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(realm.getObjectPrototype(), INSTANCE, realm.getContext());
        return initialShape;
    }

    public static boolean isJSPromise(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSPromise((DynamicObject) obj);
    }

    public static boolean isJSPromise(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    public static boolean isRejected(DynamicObject promise) {
        assert isJSPromise(promise);
        return REJECTED.equals(promise.get(JSPromise.PROMISE_STATE));
    }

    public static boolean isPending(DynamicObject promise) {
        assert isJSPromise(promise);
        return PENDING.equals(promise.get(JSPromise.PROMISE_STATE));
    }

    public static boolean isFulfilled(DynamicObject promise) {
        assert isJSPromise(promise);
        return FULFILLED.equals(promise.get(JSPromise.PROMISE_STATE));
    }

    @Override
    public String safeToString(DynamicObject obj) {
        return JSRuntime.objectToConsoleString(obj, CLASS_NAME,
                        new String[]{"PromiseStatus", "PromiseValue"},
                        new Object[]{getStatus(obj), getValue(obj)});
    }

    private static String getStatus(DynamicObject obj) {
        if (isPending(obj)) {
            return "pending";
        } else if (isFulfilled(obj)) {
            return "resolved";
        } else {
            assert isRejected(obj);
            return "rejected";
        }
    }

    private static Object getValue(DynamicObject obj) {
        Object result = obj.get(PROMISE_RESULT);
        return (result == null) ? Undefined.instance : result;
    }

}
