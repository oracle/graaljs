/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.interop.InteropAsyncFunction;
import com.oracle.truffle.js.runtime.interop.InteropBoundFunction;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This node prepares the export of a value via Interop. It transforms values not allowed in Truffle
 * and binds Functions.
 *
 * @see JSRuntime#exportValue(Object)
 */
@ImportStatic({JSGuards.class, JSFunction.class})
@GenerateUncached
public abstract class ExportValueNode extends JavaScriptBaseNode {

    ExportValueNode() {
    }

    public final Object execute(Object value) {
        return execute(value, Undefined.instance, false);
    }

    public abstract Object execute(Object value, Object thiz, boolean bindMemberFunctions);

    @Idempotent
    protected final boolean isInteropCompletePromises() {
        return getLanguage().getJSContext().getLanguageOptions().isMLEMode();
    }

    @Specialization(guards = {"!bindFunctions", "!isInteropCompletePromises() || !isAsyncFunction(function)"})
    protected static JSDynamicObject doFunctionNoBind(JSFunctionObject function, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return function;
    }

    @Specialization(guards = {"bindFunctions", "isUndefined(thiz)", "!isInteropCompletePromises() || !isAsyncFunction(function)"})
    protected static JSDynamicObject doFunctionUndefinedThis(JSFunctionObject function, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return function;
    }

    @Specialization(guards = {"bindFunctions", "!isUndefined(thiz)", "!isInteropCompletePromises() || !isAsyncFunction(function)"})
    protected static TruffleObject doBindUnboundFunction(JSFunctionObject.Unbound function, Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return new InteropBoundFunction(function, thiz);
    }

    @Specialization(guards = {"bindFunctions", "!isInteropCompletePromises() || !isAsyncFunction(function)"})
    protected static JSDynamicObject doBoundFunction(JSFunctionObject.BoundOrWrapped function, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return function;
    }

    @Specialization(guards = {"isInteropCompletePromises()", "isAsyncFunction(function)"})
    protected static TruffleObject doAsyncFunction(JSFunctionObject function, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return new InteropAsyncFunction(function);
    }

    @Specialization
    protected static double doSafeInteger(SafeInteger value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value.doubleValue();
    }

    @Specialization(guards = {"!isJSFunction(value)"})
    protected static JSDynamicObject doObject(JSDynamicObject value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization
    protected static int doInt(int value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization
    protected static long doLong(long value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization
    protected static float doFloat(float value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization
    protected static double doDouble(double value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization
    protected static boolean doBoolean(boolean value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization
    protected static BigInt doBigInt(BigInt value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization
    protected static TruffleString doString(TruffleString value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @Specialization(guards = {"!isJSFunction(value)"}, replaces = "doObject")
    protected static TruffleObject doTruffleObject(TruffleObject value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"!isTruffleObject(value)", "!isString(value)", "!isBoolean(value)", "!isNumberDouble(value)", "!isNumberLong(value)", "!isNumberInteger(value)"})
    protected static Object doOther(Object value, @SuppressWarnings("unused") Object thiz, @SuppressWarnings("unused") boolean bindFunctions) {
        throw Errors.createTypeErrorFormat("Cannot convert to TruffleObject: %s", value == null ? null : value.getClass().getTypeName());
    }

    @NeverDefault
    public static ExportValueNode create() {
        return ExportValueNodeGen.create();
    }

    public static ExportValueNode getUncached() {
        return ExportValueNodeGen.getUncached();
    }
}
