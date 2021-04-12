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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;

public final class JSGlobal extends JSNonProxy {

    public static final String CLASS_NAME = "global";
    public static final String EVAL_NAME = "eval";

    public static final JSGlobal INSTANCE = new JSGlobal();

    private JSGlobal() {
    }

    public static DynamicObject create(JSRealm realm, DynamicObject objectPrototype) {
        CompilerAsserts.neverPartOfCompilation();
        JSContext context = realm.getContext();
        JSObjectFactory factory = context.getGlobalObjectFactory();
        DynamicObject global = new JSGlobalObject(factory.getShape(realm));
        factory.initProto(global, objectPrototype);

        JSObjectUtil.putToStringTag(global, CLASS_NAME);
        return global;
    }

    public static Shape makeGlobalObjectShape(JSContext context, DynamicObject objectPrototype) {
        // keep a separate shape tree for the global object in order not to pollute user objects
        boolean singleContext = !context.isMultiContext();
        Shape globalObjectShape = JSShape.newBuilder(context, JSGlobal.INSTANCE, singleContext ? objectPrototype : null).propertyAssumptions(singleContext).singleContextAssumption(
                        singleContext ? context.getSingleRealmAssumption() : null).build();
        if (singleContext) {
            globalObjectShape = Shape.newBuilder(globalObjectShape).addConstantProperty(JSObject.HIDDEN_PROTO, objectPrototype, 0).build();
        }
        return globalObjectShape;
    }

    public static DynamicObject createGlobalScope(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        return new JSGlobalObject(context.getGlobalScopeShape());
    }

    public static boolean isJSGlobalObject(Object obj) {
        return JSDynamicObject.isJSDynamicObject(obj) && isJSGlobalObject((DynamicObject) obj);
    }

    public static boolean isJSGlobalObject(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @TruffleBoundary
    @Override
    public boolean setPrototypeOf(DynamicObject thisObj, DynamicObject newPrototype) {
        if (JSObject.getPrototype(thisObj) == newPrototype) {
            return true;
        }
        JSObject.getJSContext(thisObj).getGlobalObjectPristineAssumption().invalidate();
        return super.setPrototypeOf(thisObj, newPrototype);
    }
}
