/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
