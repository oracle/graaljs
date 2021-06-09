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
package com.oracle.truffle.js.builtins.temporal;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimeFunctionBuiltinsFactory.JSTemporalPlainDateTimeCompareNodeGen;
import com.oracle.truffle.js.builtins.temporal.TemporalPlainDateTimeFunctionBuiltinsFactory.JSTemporalPlainDateTimeFromNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.temporal.JSTemporalPlainDateTime;
import com.oracle.truffle.js.runtime.builtins.temporal.TemporalDateTime;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainDateTimeFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainDateTimeFunctionBuiltins.TemporalPlainDateTimeFunction> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainDateTimeFunctionBuiltins();

    protected TemporalPlainDateTimeFunctionBuiltins() {
        super(JSTemporalPlainDateTime.CLASS_NAME, TemporalPlainDateTimeFunction.class);
    }

    public enum TemporalPlainDateTimeFunction implements BuiltinEnum<TemporalPlainDateTimeFunction> {
        from(1),
        compare(2);

        private final int length;

        TemporalPlainDateTimeFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainDateTimeFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return JSTemporalPlainDateTimeFromNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case compare:
                return JSTemporalPlainDateTimeCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainDateTimeFromNode extends JSBuiltinNode {

        public JSTemporalPlainDateTimeFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object from(Object item, Object optParam,
                        @Cached("create()") IsObjectNode isObject,
                        @Cached("create()") JSToBooleanNode toBoolean,
                        @Cached("create()") JSToStringNode toString) {
            DynamicObject options = TemporalUtil.getOptionsObject(optParam, getContext());
            if (isObject.executeBoolean(item) && JSTemporalPlainDateTime.isJSTemporalPlainDateTime(item)) {
                TemporalDateTime dtItem = (TemporalDateTime) item;
                TemporalUtil.toTemporalOverflow(options, toBoolean, toString);
                return JSTemporalPlainDateTime.create(getContext(),
                                dtItem.getISOYear(), dtItem.getISOMonth(), dtItem.getISODay(),
                                dtItem.getHours(), dtItem.getMinutes(), dtItem.getSeconds(), dtItem.getMilliseconds(),
                                dtItem.getMicroseconds(), dtItem.getNanoseconds(), dtItem.getCalendar());
            }
            return TemporalUtil.toTemporalDateTime(item, options, getContext());
        }

    }

    public abstract static class JSTemporalPlainDateTimeCompareNode extends JSBuiltinNode {

        public JSTemporalPlainDateTimeCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int compare(Object obj1, Object obj2) {
            TemporalDateTime one = (TemporalDateTime) TemporalUtil.toTemporalDateTime(obj1, Undefined.instance, getContext());
            TemporalDateTime two = (TemporalDateTime) TemporalUtil.toTemporalDateTime(obj2, Undefined.instance, getContext());
            return JSTemporalPlainDateTime.compareISODateTime(
                            one.getISOYear(), one.getISOMonth(), one.getISODay(),
                            one.getHours(), one.getMinutes(), one.getSeconds(),
                            one.getMilliseconds(), one.getMicroseconds(), one.getNanoseconds(),
                            two.getISOYear(), two.getISOMonth(), two.getISODay(),
                            two.getHours(), two.getMinutes(), two.getSeconds(),
                            two.getMilliseconds(), two.getMicroseconds(), two.getNanoseconds());
        }
    }

}
