/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.OperatorSet;

/**
 * @see JSOverloadedOperatorsObject
 */
public final class JSOverloadedOperators extends JSNonProxy {

    public static final JSOverloadedOperators INSTANCE = new JSOverloadedOperators();

    private JSOverloadedOperators() {
    }

    public static JSOverloadedOperatorsObject create(JSContext context, Shape shape, OperatorSet operatorSet) {
        JSOverloadedOperatorsObject object = JSOverloadedOperatorsObject.create(shape, operatorSet);
        return context.trackAllocation(object);
    }

    @Override
    @CompilerDirectives.TruffleBoundary
    public String getClassName(DynamicObject object) {
        JSContext context = JSObject.getJSContext(object);
        if (context.getEcmaScriptVersion() <= 5) {
            Object toStringTag = get(object, Symbol.SYMBOL_TO_STRING_TAG);
            if (JSRuntime.isString(toStringTag)) {
                return JSRuntime.toStringIsString(toStringTag);
            }
        }
        return JSOrdinary.CLASS_NAME;
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects, JSContext context) {
        if (context.isOptionNashornCompatibilityMode()) {
            return defaultToString(obj);
        } else {
            return JSRuntime.objectToConsoleString(obj, null, depth, allowSideEffects);
        }
    }

    @CompilerDirectives.TruffleBoundary
    @Override
    public Object get(DynamicObject thisObj, long index) {
        // convert index only once
        return get(thisObj, String.valueOf(index));
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject obj) {
        return true;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, context);
    }
}
