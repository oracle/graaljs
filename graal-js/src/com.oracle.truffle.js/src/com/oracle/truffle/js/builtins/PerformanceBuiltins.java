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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class PerformanceBuiltins {

    public static final TruffleString TO_STRING_TAG = Strings.constant("Performance");
    public static final TruffleString FUNCTION_NAME = TO_STRING_TAG;
    public static final TruffleString OBJECT_NAME = Strings.constant("performance");
    public static final TruffleString TIME_ORIGIN = Strings.constant("timeOrigin");

    public static final JSBuiltinsContainer BUILTINS = JSBuiltinsContainer.fromEnum(FUNCTION_NAME, PerformancePrototype.class);

    public enum PerformancePrototype implements BuiltinEnum<PerformancePrototype> {
        now(0),
        timeOrigin(0),
        toJSON(0);

        private final int length;

        PerformancePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return this == timeOrigin;
        }

        @Override
        public boolean isEnumerable() {
            return true;
        }

        @Override
        public Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            return switch (this) {
                case now -> PerformanceBuiltinsFactory.TimeNodeGen.create(context, builtin, false, args().withThis().fixedArgs(0).createArgumentNodes(context));
                case timeOrigin -> PerformanceBuiltinsFactory.TimeNodeGen.create(context, builtin, true, args().withThis().fixedArgs(0).createArgumentNodes(context));
                case toJSON -> PerformanceBuiltinsFactory.ToJSONNodeGen.create(context, builtin, args().withThis().fixedArgs(0).createArgumentNodes(context));
            };
        }
    }

    public static JSObject createPerformanceObject(JSRealm realm) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSFunctionData functionData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.PerformanceConstructor,
                        PerformanceBuiltins::createPerformanceFunctionData);
        JSFunctionObject function = JSFunction.create(realm, functionData);
        JSObjectUtil.putConstructorPrototypeProperty(function, prototype);
        JSObjectUtil.putConstructorProperty(prototype, function);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, BUILTINS);
        JSObjectUtil.putToStringTag(prototype, TO_STRING_TAG);
        return JSOrdinary.createInit(realm, prototype);
    }

    private static JSFunctionData createPerformanceFunctionData(JSContext c) {
        return JSFunctionData.createCallOnly(c, c.getNotConstructibleCallTarget(), 0, TO_STRING_TAG);
    }

    /**
     * Implementation of {@code performance.now()} and {@code performance.timeOrigin}.
     */
    abstract static class TimeNode extends JSBuiltinNode {

        private final boolean timeOrigin;

        TimeNode(JSContext context, JSBuiltin builtin, boolean timeOrigin) {
            super(context, builtin);
            this.timeOrigin = timeOrigin;
        }

        @Specialization(guards = "thisObj == realm.getPerformanceObject()")
        protected double now(@SuppressWarnings("unused") Object thisObj,
                        @Bind("getRealm()") JSRealm realm) {
            long ns = timeOrigin
                            ? realm.nanoTimeOrigin()
                            : realm.nanoTime();
            return ns / (double) JSRealm.NANOSECONDS_PER_MILLISECOND;
        }

        @Fallback
        protected double typeError(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }

    /**
     * Implementation of {@code performance.toJSON()}.
     */
    abstract static class ToJSONNode extends JSBuiltinNode {

        ToJSONNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "thisObj == realm.getPerformanceObject()")
        protected Object toJSON(@SuppressWarnings("unused") Object thisObj,
                        @Bind("getRealm()") JSRealm realm) {
            var result = JSOrdinary.create(getContext(), realm);
            double timeOrigin = realm.nanoTimeOrigin() / (double) JSRealm.NANOSECONDS_PER_MILLISECOND;
            JSObjectUtil.putDataProperty(result, TIME_ORIGIN, timeOrigin);
            return result;
        }

        @Fallback
        protected Object typeError(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }
    }
}
